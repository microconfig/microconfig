# Microconfig 4.9.7 versus the current working version

This fixture and runner compare complete, real CLI builds in **fresh JVM processes**.
There are no JVM warm-up runs or wrapper entry points.

**Latest concurrency review and cold-launch comparison after removing serializer locks:**
see [lock-review.md](lock-review.md),
[single-write-current-empty-results.json](single-write-current-empty-results.json), and
[single-write-current-existing-results.json](single-write-current-existing-results.json).

**Preceding comparison with parallel component resolution and existing-output builds:**
see [parallel-optimization.md](parallel-optimization.md),
[parallel-results.json](parallel-results.json), and
[parallel-existing-results.json](parallel-existing-results.json).
The preceding optimization stage is preserved in [optimization.md](optimization.md)
and [optimized-results.json](optimized-results.json).
The initial measurements below are preserved as historical results; their timings
must not be compared directly with the updated results because the power mode changed.

## Fixture

[`fixture/`](fixture/) is a Microconfig repository containing:

- 50 services (`svc00` through `svc49`) built in environment `dev`.
- 39 shared include components: 3 at level 1, 9 at level 2, and 27 at level 3.
- Three include branches per service; maximum depth is three include edges.
- 186 declared include edges and 140 configuration source files.
- 172 application properties and 5 process properties per service, including
  50 scalar references and three deterministic expressions.

Each build produces **100 files with 8850 scalar properties**. The include graph,
source file hashes, and generation checks are described in
[`fixture/README.md`](fixture/README.md) and [`fixture/manifest.json`](fixture/manifest.json).

Regenerate the fixture:

```sh
python3 benchmarks/cache-comparison/generator.py --output benchmarks/cache-comparison/fixture
```

## Versions and environment

Measured on 2026-11-07, macOS / arm64, using OpenJDK **25.0.2+10-69** for both versions.
The current Kotlin code targets JVM 17; the execution runtime for this comparison was 25.

- **Old:** the unmodified published
  [Microconfig CLI 4.9.7 JAR](https://repo1.maven.org/maven2/io/microconfig/microconfig-cli/4.9.7/microconfig-cli-4.9.7.jar).
  Its Maven SHA-1 sidecar was verified. SHA-256:
  `b080d82d1184e3231fc49dde7fdf22c30c2655b9d5135e60c9f693640f87866f`.
- **New:** a frozen copy of the current working sources, including uncommitted
  changes, built offline with `:microconfig-cli:shadowJar`; CLI version `5.0.0`.
  Git HEAD was `43194fe9d63ae3bbe95d27f1be0ecafee1c7d617`.
  JAR SHA-256:
  `7453400022b70a1aa5921265ffa44cfdb9ccc03f17ed0f75387514d7ac06ce36`.

[`old-release.json`](old-release.json), [`new-state.json`](new-state.json), and
[`new-source-manifest.json`](new-source-manifest.json) record provenance.
An earlier historical Git build was prepared but **was not used in these measurements**.

## Method

Seven measured runs of each version, with alternating execution order:
old/new, new/old, old/new, and so on. Every invocation uses a new JVM and an empty,
separate destination directory:

```sh
/path/to/java -jar VERSION.jar -r fixture -e dev -d EMPTY_DESTINATION
```

No heap, GC, color, or logging flags are added. Normal CLI logging remains enabled
and goes to a separate log file for each run. The wall timer includes process/JVM
startup, configuration generation, file writing, logging, and process shutdown.
Creation of the empty destination and output validation are outside the timer.

The OS filesystem cache is **not flushed**. Thus these are cold JVM launches, not
cold-disk measurements. Peak RSS is obtained for each child process using `wait4`;
the table reports the median of those per-process peaks, not Java heap size.

## Results

| Metric | Published 4.9.7 | Current working 5.0.0 |
| --- | ---: | ---: |
| Median wall time | 404.4 ms | 1774.5 ms |
| Minimum–maximum wall time | 388.0–410.5 ms | 1690.9–1884.1 ms |
| Median peak process RSS | 218.25 MiB | 471.48 MiB |

On this workload, the current version is **4.39 times slower** and its median peak
process RSS is **2.16 times higher**. All 14 builds produced the expected file and
property counts. All generated files were byte-identical between versions in all
seven pairs; independent validation also compared all 14 output trees.

This compares complete releases/working versions. It does not isolate the effect
of the caching strategy, Kotlin migration, dependency changes, or individual fixes.

Raw samples, exact commands, JAR hashes, and validation results are in
[`results.json`](results.json). Per-run logs and output trees are under `work/`
and are intentionally ignored by Git.

## Repeat

Download the published baseline and build the current project:

```sh
mkdir -p benchmarks/cache-comparison/work
curl --fail --location \
  https://repo1.maven.org/maven2/io/microconfig/microconfig-cli/4.9.7/microconfig-cli-4.9.7.jar \
  --output benchmarks/cache-comparison/work/microconfig-cli-4.9.7.jar
./gradlew --offline :microconfig-cli:shadowJar
python3 benchmarks/cache-comparison/run.py \
  --old-jar benchmarks/cache-comparison/work/microconfig-cli-4.9.7.jar \
  --new-jar microconfig-cli/build/libs/microconfig-cli-5.0.0.jar \
  --java /path/to/jdk/bin/java \
  --runs 7
```

The runner uses `wait4` and supports macOS and Linux. Use the same Java executable
for both versions. It stops on a CLI error, unexpected output count, or differing
output bytes. Each repeat creates a new owned subdirectory under `work/`.

To measure rebuilding an existing output tree, add `--existing-output /path/to/previous/output`.
The runner copies that tree into each separate destination outside the timer; it
does not launch an extra seed JVM. Reports distinguish full process wall time
from the CLI's own `Generated [dev] configs in ...ms` message.
