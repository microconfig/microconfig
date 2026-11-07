#!/usr/bin/env python3
"""Compare Native Image and the JVM on existing fixtures and expression cases."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


def digest_tree(directory):
    return {
        path.relative_to(directory).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in sorted(directory.rglob("*")) if path.is_file()
    }


def expression_cases(project, work):
    source = (project / "microconfig-core/src/test/kotlin/io/microconfig/core/properties/"
              "resolvers/expression/ExpressionResolverTest.kt").read_text()
    positive = source.split("fun evaluateValueExpressions", 1)[0]
    expressions = []
    for literal in re.findall(r'^\s*("(?:\\.|[^"\\])*"),?\s*$', positive, re.MULTILINE):
        if not literal.startswith('"#{') or " | " not in literal:
            continue
        value = json.loads(literal.replace(r"\$", "$"))
        if value.startswith("#{") and " | " in value:
            expressions.append(value.split(" | ", 1)[0])
    expressions.append("#{#replaceLineSeparators('a' + #lineSeparator() + 'b', '|')}")
    root = work / "expressions"
    (root / "envs").mkdir(parents=True)
    (root / "envs/dev.yaml").write_text("group:\n  components: [values]\n")
    component = root / "components/values"
    component.mkdir(parents=True)
    (component / "application.properties").write_text(
        "".join(f"value{i}={expression}\n" for i, expression in enumerate(expressions))
    )
    cases = [("expressions/values", ["-r", str(root), "-e", "dev", "-s", "values"])]
    rejected = [
        "#{T(java.lang.Runtime).getRuntime().availableProcessors()}",
        "#{new java.lang.ProcessBuilder('sh').command().size()}",
        "#{T(java.nio.file.Files).exists(T(java.nio.file.Path).of('/'))}",
        "#{''.getClass().forName('java.lang.Runtime').getName()}",
        "#{T(Class).forName('java.lang.Runtime').getName()}",
        "#{T(Math).max(1, 2)}",
        "#{#setProperty('microconfig.expression.probe', 'changed')}",
        "#{#clearProperty('java.version')}",
        "#{#getenv('HOME')}",
        "#{#getProperty('java.version')}",
        "#{#load('/nonexistent/library.so')}",
        "#{#gc()}",
        "#{#readString('/etc/hosts')}",
        "#{#readStringOrEmpty('/etc/hosts')}",
        "#{#readBytes('/etc/hosts')}",
        "#{#readBytesOrEmpty('/etc/hosts')}",
        "#{#currentTimeMillis.invoke(null)}",
        "#{#base64.getDeclaringClass().getName()}",
        "#{#base64.getName()}",
        "#{#base64.getClass().getName()}",
        "#{#uuid4()}",
        "#{null}",
    ]
    for index, expression in enumerate(rejected):
        name = f"rejected{index:02d}"
        component = root / "components" / name
        component.mkdir()
        (component / "application.properties").write_text(f"value={expression}\n")
        cases.append((f"expressions/{name}", ["-r", str(root), "-e", "dev", "-s", name]))
    return cases, len(expressions)


def fixture_cases(project, work):
    root = work / "fixture-repo"
    shutil.copytree(project / "microconfig-core/src/test/resources/repo", root)
    cases = []
    for path in sorted(root.rglob("*")):
        if not path.is_file() or not path.name.startswith(("expect.", "truncate.", "exception.")):
            continue
        parts = path.name.split(".")
        environment = parts[1]
        component = parts[2] if len(parts) == 3 else path.parent.name
        cases.append((f"fixture/{path.relative_to(root)}",
                      ["-r", str(root), "-e", environment, "-s", component]))
    root = project / "microconfig-cli/src/test/resources/repo"
    for label, arguments in (
        ("single-env", ["-e", "env1"]),
        ("multiple-envs", ["-envs", "env2,env3"]),
        ("all-envs", ["-envs", "*"]),
        ("exclude-env", ["-envs", "*,!env2"]),
        ("selected-service", ["-e", "env1", "-s", "component"]),
        ("missing-service", ["-e", "env1", "-s", "missing"]),
    ):
        cases.append(("cli/" + label, ["-r", str(root)] + arguments))
    for index, arguments in enumerate((
        ["-v"], ["-color", "false", "-v"], [], ["unexpected"], ["-e"], ["-e", "*"],
    )):
        cases.append((f"arguments/{index}", arguments))
    root = work / "template-repo"
    shutil.copytree(project / "microconfig-core/src/test/resources/templates", root)
    cases.append(("templates/mustache", ["-r", str(root), "-e", "dev", "-s", "mustache"]))
    root = project / "benchmarks/cache-comparison/fixture"
    cases.append(("benchmark/50-services", ["-r", str(root), "-e", "dev"]))
    return cases


def declare_selected_component(arguments):
    root = Path(arguments[arguments.index("-r") + 1])
    environment = arguments[arguments.index("-e") + 1]
    component = arguments[arguments.index("-s") + 1]
    path = root / "envs" / f"{environment}.yaml"
    original = path.read_bytes() if path.exists() else None
    content = original.decode() if original is not None else ""
    if content.strip() == "{}":
        content = ""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\nnativeValidation:\n  components: [" + json.dumps(component) + "]\n")
    return path, original


def run_case(prefix, arguments, destination, log):
    if destination.exists():
        shutil.rmtree(destination)
    command = prefix + arguments + ["-d", str(destination), "-color", "false"]
    process = subprocess.run(command, capture_output=True, text=True, timeout=30)
    text = process.stdout + process.stderr
    log.write_text(text)
    text = re.sub(r"Generated \[[^\]]+\] configs in \d+ms", "Generated configs", text)
    return {"exit_code": process.returncode, "files": digest_tree(destination),
            "output": text, "command": command, "log": str(log)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--native", type=Path, required=True)
    parser.add_argument("--results", type=Path, required=True)
    parser.add_argument("--only", help="Run case names containing this text")
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[2]
    parent = project / "build/native-validation"
    parent.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix="cases-", dir=parent))
    expressions, count = expression_cases(project, work)
    cases = fixture_cases(project, work) + expressions
    if args.only:
        cases = [(name, arguments) for name, arguments in cases if args.only in name]
    prefixes = {"jvm": [str(args.java.resolve()), "-jar", str(args.jar.resolve())],
                "native": [str(args.native.resolve())]}
    report = {"method": "Same CLI arguments and destination, fresh process for each case; compare exit status, generated bytes, and normalized console output. API fixtures selecting undeclared components receive an explicit group in a copied environment. Truncated error expectations compare their declared prefix.",
              "work": str(work), "value_expressions": count, "cases": [],
              "jar_sha256": hashlib.sha256(args.jar.read_bytes()).hexdigest(),
              "native_sha256": hashlib.sha256(args.native.read_bytes()).hexdigest()}
    for index, (name, arguments) in enumerate(cases):
        jvm = run_case(prefixes["jvm"], arguments, work / "output", work / f"{index:03d}-jvm.log")
        declaration = None
        is_api_fixture = name.startswith(("fixture/", "templates/"))
        missing_component = re.fullmatch(r"Component '[^']+' doesn't exist\.\s*", jvm["output"])
        missing_environment = jvm["output"].startswith("Can't find env '") and not (
            Path(arguments[arguments.index("-r") + 1]) / "envs" /
            (arguments[arguments.index("-e") + 1] + ".yaml")
        ).exists() if is_api_fixture else False
        if is_api_fixture and (missing_component or missing_environment):
            declaration = declare_selected_component(arguments)
            jvm = run_case(prefixes["jvm"], arguments, work / "output", work / f"{index:03d}-jvm.log")
        try:
            native = run_case(prefixes["native"], arguments, work / "output", work / f"{index:03d}-native.log")
        finally:
            if declaration is not None:
                path, original = declaration
                if original is None:
                    path.unlink()
                else:
                    path.write_bytes(original)
        console_matches = jvm["output"] == native["output"]
        if not console_matches and name.startswith("fixture/") and "/truncate." in name and jvm["exit_code"] != 0:
            expectation = (work / "fixture-repo" / name.removeprefix("fixture/")).read_text().strip()
            prefix = r"^Filtered \d+ component\(s\) in \[[^\]]+\] env\.\n"
            console_matches = all(re.sub(prefix, "", result["output"]).startswith(expectation)
                                  for result in (jvm, native))
        row = {"name": name, "matches": jvm["exit_code"] == native["exit_code"] and
               jvm["files"] == native["files"] and console_matches,
               "component_declared_for_cli": declaration is not None,
               "jvm": jvm, "native": native}
        report["cases"].append(row)
        args.results.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        if not row["matches"]:
            print("MISMATCH", name, jvm["exit_code"], native["exit_code"], flush=True)
        elif index % 20 == 0:
            print(f"Checked {index + 1}/{len(cases)} cases", flush=True)
    report["summary"] = {"cases": len(cases), "matched": sum(row["matches"] for row in report["cases"]),
                         "jvm_successes": sum(row["jvm"]["exit_code"] == 0 for row in report["cases"]),
                         "mismatches": [row["name"] for row in report["cases"] if not row["matches"]]}
    args.results.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report["summary"], indent=2), flush=True)
    return bool(report["summary"]["mismatches"])


if __name__ == "__main__":
    raise SystemExit(main())
