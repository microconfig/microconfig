#!/usr/bin/env python3
"""Compare real CLI invocations in fresh JVMs; never reuse Microconfig or output."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import statistics
import subprocess
import sys
import tempfile
import time


def digest_tree(directory):
    return {
        path.relative_to(directory).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in sorted(directory.rglob("*"))
        if path.is_file()
    }


def run_cli(java, jar, fixture, destination, log_path, existing_output=None):
    if existing_output is None:
        destination.mkdir()
    else:
        shutil.copytree(existing_output, destination)
    command = [str(java), "-jar", str(jar), "-r", str(fixture), "-e", "dev", "-d", str(destination)]
    with log_path.open("w") as log:
        started = time.perf_counter_ns()
        process = subprocess.Popen(command, stdout=log, stderr=subprocess.STDOUT)
        _, status, usage = os.wait4(process.pid, 0)
        process.returncode = os.waitstatus_to_exitcode(status)
        elapsed_ms = (time.perf_counter_ns() - started) / 1_000_000
    if process.returncode:
        raise RuntimeError(f"CLI exited {process.returncode}; see {log_path}")
    rss_bytes = usage.ru_maxrss if sys.platform == "darwin" else usage.ru_maxrss * 1024
    log_text = re.sub(r"\x1b\[[0-9;]*m", "", log_path.read_text())
    reported = re.search(r"Generated \[dev\] configs in (\d+)ms", log_text)
    return {
        "wall_ms": elapsed_ms,
        "cli_reported_ms": int(reported.group(1)) if reported else None,
        "user_ms": usage.ru_utime * 1000,
        "system_ms": usage.ru_stime * 1000,
        "peak_rss_bytes": rss_bytes,
        "command": command,
        "log": str(log_path),
        "destination": str(destination),
    }


def summarize(rows):
    wall = [row["wall_ms"] for row in rows]
    rss = [row["peak_rss_bytes"] for row in rows]
    summary = {
        "runs": len(rows),
        "median_wall_ms": statistics.median(wall),
        "min_wall_ms": min(wall),
        "max_wall_ms": max(wall),
        "median_peak_rss_bytes": statistics.median(rss),
        "max_peak_rss_bytes": max(rss),
    }
    reported = [row["cli_reported_ms"] for row in rows if row["cli_reported_ms"] is not None]
    if len(reported) == len(rows):
        summary.update({"median_cli_reported_ms": statistics.median(reported),
                        "min_cli_reported_ms": min(reported), "max_cli_reported_ms": max(reported)})
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--old-jar", type=Path, required=True)
    parser.add_argument("--new-jar", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--fixture", type=Path, default=Path(__file__).parent / "fixture")
    parser.add_argument("--existing-output", type=Path,
                        help="Copy these previous output files before each measured JVM; no warm-up run")
    parser.add_argument("--runs", type=int, default=7)
    parser.add_argument("--work", type=Path, default=Path(__file__).parent / "work")
    parser.add_argument("--results", type=Path, default=Path(__file__).parent / "results.json")
    args = parser.parse_args()
    if args.runs < 1:
        parser.error("--runs must be positive")
    java = args.java.resolve()
    jars = {"old": args.old_jar.resolve(), "new": args.new_jar.resolve()}
    fixture = args.fixture.resolve()
    existing_output = args.existing_output.resolve() if args.existing_output else None
    if existing_output is not None and not existing_output.is_dir():
        parser.error("--existing-output must be a directory")
    args.work.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix="cold-", dir=args.work.resolve()))
    version = subprocess.run([str(java), "-version"], capture_output=True, text=True, check=True)
    report = {
        "method": "Fresh JVM for each actual java -jar invocation; no warm-up, no JVM flags, default logging; separate destination; alternating order",
        "output_mode": "empty" if existing_output is None else "existing",
        "existing_output": str(existing_output) if existing_output else None,
        "existing_output_sha256": digest_tree(existing_output) if existing_output else None,
        "filesystem_cache": "OS page cache is not flushed",
        "machine": platform.platform(),
        "java": str(java),
        "java_version": (version.stdout + version.stderr).strip(),
        "fixture": str(fixture),
        "fixture_manifest": json.loads((fixture / "manifest.json").read_text()),
        "jar_sha256": {key: hashlib.sha256(path.read_bytes()).hexdigest() for key, path in jars.items()},
        "jar_paths": {key: str(path) for key, path in jars.items()},
        "work": str(work),
        "samples": {"old": [], "new": []},
        "output_validation": [],
    }

    def save():
        args.results.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")

    for round_index in range(args.runs):
        order = ["old", "new"] if round_index % 2 == 0 else ["new", "old"]
        outputs = {}
        for label in order:
            destination = work / f"{round_index:02d}-{label}"
            result = run_cli(java, jars[label], fixture, destination, work / f"{round_index:02d}-{label}.log",
                             existing_output)
            result.update({"round": round_index + 1, "order": order.index(label) + 1})
            report["samples"][label].append(result)
            save()
            outputs[label] = digest_tree(destination)
            if len(outputs[label]) != 100:
                raise AssertionError(f"Expected 100 output files for {label}, got {len(outputs[label])}")
        if outputs["old"] != outputs["new"]:
            differing = sorted(key for key in outputs["old"].keys() | outputs["new"].keys()
                               if outputs["old"].get(key) != outputs["new"].get(key))
            report["output_mismatches"] = differing
            save()
            raise AssertionError(f"Outputs differ: {differing[:10]}; inspect {work}")
        report["output_validation"].append({"round": round_index + 1, "files": 100, "byte_identical": True})
        save()
        print(f"Round {round_index + 1}/{args.runs}: " + ", ".join(
            f"{label} {report['samples'][label][-1]['wall_ms']:.1f} ms" for label in ["old", "new"]), flush=True)

    report["summary"] = {key: summarize(rows) for key, rows in report["samples"].items()}
    report["new_to_old_wall_ratio"] = report["summary"]["new"]["median_wall_ms"] / report["summary"]["old"]["median_wall_ms"]
    save()
    print(json.dumps(report["summary"], indent=2), flush=True)


if __name__ == "__main__":
    main()
