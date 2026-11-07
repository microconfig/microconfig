#!/usr/bin/env python3
"""Generate the fixed, deterministic shared-include cache benchmark fixture."""

import argparse
import hashlib
import json
import shutil
from pathlib import Path

MARKER = ".microconfig-cache-benchmark-fixture"
SERVICE_COUNT = 50
BRANCHES = 3


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def build_sources():
    files = {}
    shared_keys = []
    names_by_level = {1: [], 2: [], 3: []}
    for level in range(1, 4):
        for ordinal in range(BRANCHES ** level):
            digits = ""
            remainder = ordinal
            for _ in range(level):
                digits = str(remainder % BRANCHES) + digits
                remainder //= BRANCHES
            name = f"common-l{level}-{digits}"
            names_by_level[level].append(name)
            prefix = f"shared.l{level}.branch{digits}"
            keys = [prefix + ".text", prefix + ".number", prefix + ".enabled"]
            shared_keys.extend(keys)
            children = [] if level == 3 else [
                f"common-l{level + 1}-{digits}{child}" for child in range(BRANCHES)
            ]
            lines = (["#include " + ", ".join(children)] if children else []) + [
                f"{keys[0]}: text-l{level}-{digits}",
                f"{keys[1]}: {level * 100 + ordinal}",
                f"{keys[2]}: {'true' if ordinal % 2 == 0 else 'false'}",
            ]
            files[f"components/{name}/application.yaml"] = "\n".join(lines) + "\n"

    services = [f"svc{index:02d}" for index in range(SERVICE_COUNT)]
    for index, name in enumerate(services):
        lines = [
            "#include " + ", ".join(names_by_level[1]),
            f"service.name: {name}",
            f"service.ordinal: {index}",
        ]
        for reference in range(50):
            target = shared_keys[(index * 17 + reference) % len(shared_keys)]
            lines.append(f"resolved.ref{reference:02d}: ${{this@{target}}}")
        lines += [
            "calculated.answer: #{6 * 7}",
            "calculated.product: #{7 * 8}",
            "calculated.offset: #{100 + 23}",
        ]
        files[f"components/{name}/application.yaml"] = "\n".join(lines) + "\n"
        files[f"components/{name}/config.proc"] = (
            f"runtime.service={name}\n"
            "runtime.heap=256m\n"
            f"runtime.port={9000 + index}\n"
            "runtime.enabled=true\n"
            f"runtime.label=benchmark-{name}\n"
        )
    files["envs/dev.yaml"] = (
        "benchmark:\n  components:\n" + "".join(f"    - {name}\n" for name in services)
    )
    return files


def check_sources(files):
    applications = {}
    includes = {}
    for path, content in files.items():
        if not path.startswith("components/") or not path.endswith("/application.yaml"):
            continue
        component = Path(path).parent.name
        properties = {}
        children = []
        for line in content.splitlines():
            if line.startswith("#include "):
                children.extend(part.strip() for part in line.removeprefix("#include ").split(","))
            else:
                key, separator, value = line.partition(": ")
                assert separator and key not in properties, (path, line)
                properties[key] = value
        applications[component] = properties
        includes[component] = children

    services = [line.strip().removeprefix("- ") for line in files["envs/dev.yaml"].splitlines()
                if line.strip().startswith("- ")]
    assert services == [f"svc{index:02d}" for index in range(50)]
    assert len(applications) == 89
    assert set(applications) - set(services) == {
        name for name in applications if name.startswith("common-l")
    }
    assert sum(len(children) for children in includes.values()) == 186
    assert all(len(children) in (0, 3) for children in includes.values())
    completed = {}

    def collect(component, visiting):
        assert component in applications, component
        assert component not in visiting, f"Cycle: {visiting + [component]}"
        if component in completed:
            return completed[component]
        values = {}
        depth = 0
        for child in includes[component]:
            child_values, child_depth = collect(child, visiting + [component])
            assert not (values.keys() & child_values.keys()), component
            values.update(child_values)
            depth = max(depth, child_depth + 1)
        assert not (values.keys() & applications[component].keys()), component
        values.update(applications[component])
        completed[component] = (values, depth)
        return values, depth

    for service in services:
        values, depth = collect(service, [])
        assert depth == 3 and len(values) == 172, (service, depth, len(values))
        included_keys = values.keys() - applications[service].keys()
        assert len(included_keys) == 117
        references = [value for value in applications[service].values()
                      if value.startswith("${this@")]
        assert len(references) == 50
        assert all(value.removeprefix("${this@").removesuffix("}") in included_keys
                   for value in references)
        process = files[f"components/{service}/config.proc"].splitlines()
        assert len(process) == 5 and all("=" in line and not line.startswith("#") for line in process)
    assert len(files) == 140
    return {
        "buildComponents": 50,
        "includeOnlyComponents": 39,
        "componentDirectories": 89,
        "applicationFiles": 89,
        "processFiles": 50,
        "environmentFiles": 1,
        "sourceFiles": 140,
        "includeDirectiveLines": 62,
        "includeEdges": 186,
        "maxIncludeDepthEdges": 3,
        "branchingFactor": 3,
        "sharedPropertiesPerService": 117,
        "rootScalarReferencesPerService": 50,
        "rootExpressionsPerService": 3,
        "applicationPropertiesPerService": 172,
        "processPropertiesPerService": 5,
        "totalPropertiesPerService": 177,
        "expectedOutputFiles": 100,
        "expectedOutputProperties": 8850,
    }


def generate(output):
    files = build_sources()
    counts = check_sources(files)
    source_digest = hashlib.sha256()
    records = []
    for name, text in sorted(files.items()):
        data = text.encode("utf-8")
        digest = sha256(data)
        source_digest.update(name.encode("utf-8") + b"\0" + data + b"\0")
        records.append({"path": name, "bytes": len(data), "sha256": digest})
    digest = source_digest.hexdigest()
    manifest = {
        "schemaVersion": 1,
        "generatorSha256": sha256(Path(__file__).read_bytes()),
        "fixtureSourcesSha256": digest,
        "sourceHashEncoding": "sorted relative POSIX path + NUL + UTF-8 content + NUL",
        "environment": "dev",
        "builtComponentNames": [f"svc{index:02d}" for index in range(50)],
        "includeLevels": {"l1": 3, "l2": 9, "l3": 27},
        "counts": counts,
        "sources": records,
    }
    readme = f"""# Shared-include cache benchmark fixture

Generated by `../generator.py`; rerun with `python3 generator.py --output fixture`.
The generator validates counts, references, maximum depth and absence of cycles before writing.
All source text is UTF-8 with LF endings. There are no random values, timestamps, IP lookups,
external dependencies or machine-specific absolute paths in the fixture.

Build environment `dev`, group `benchmark`. It contains exactly `svc00` through `svc49`.
Only these 50 services are built; the 39 `common-l*` components are included, not built separately.
There are no profiles, overrides, custom configuration types, tags or YAML aliases.

Each service includes the same three L1 components. Every L1 includes three distinct L2
components, and every L2 includes three distinct L3 components. The 39-node include tree
is shared across all services, so the repository is a DAG. Maximum root-to-leaf depth is
**3 include edges**. Nodes at a level have unique keys; include precedence cannot change results.

| Item | Count |
| --- | ---: |
| Built services | 50 |
| Shared L1 / L2 / L3 components | 3 / 9 / 27 |
| Total component directories | 89 |
| Application YAML / process Properties / environment YAML files | 89 / 50 / 1 |
| Total configuration source files | 140 |
| Include directive lines / declared include edges | 62 / 186 |
| Shared application properties reached by each service | 117 |
| Per-service own application properties | 2 |
| Per-service scalar references / deterministic expressions | 50 / 3 |
| Final application / process properties per service | 172 / 5 |
| Total final properties per service | 177 |
| Expected generated config files / total properties | 100 / 8850 |

Each shared node declares text, integer and boolean values with unique dotted string keys.
All 50 `${{this@...}}` references in a service target existing shared keys. Reference selection
rotates by service, avoiding identical service output. Expressions are `#{{6 * 7}}`,
`#{{7 * 8}}` and `#{{100 + 23}}` (42, 56 and 123). Each service also has five plain
Properties values in `config.proc`; process files have no includes. Expected output names
are `application.yaml` and `process.properties` in each service directory.

`manifest.json` records the generator hash, counts and SHA-256/byte size for every source.
`fixtureSourcesSha256` hashes sorted relative POSIX path, NUL, UTF-8 source content, NUL.
It excludes this README, the ownership marker and the manifest itself.

Fixture source SHA-256: `{digest}`
"""
    if output.exists():
        if any(output.iterdir()) and not (output / MARKER).is_file():
            raise SystemExit(f"Refusing to replace an unmanaged non-empty directory: {output}")
        shutil.rmtree(output)
    output.mkdir(parents=True)
    for name, content in files.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content.encode("utf-8"))
    (output / MARKER).write_bytes(b"Generated deterministic cache benchmark fixture, schema 1.\n")
    (output / "README.md").write_bytes(readme.encode("utf-8"))
    (output / "manifest.json").write_bytes((json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode("utf-8"))
    assert {name: (output / name).read_text(encoding="utf-8") for name in files} == files
    return {"output": str(output), "fixtureSourcesSha256": digest, "counts": counts}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parent / "fixture")
    args = parser.parse_args()
    print(json.dumps(generate(args.output.resolve()), indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
