# CPU profile analysis: current 5.0 only

Source: existing three cold CLI JFR recordings, JAR SHA-256 `7453400022b70a1aa5921265ffa44cfdb9ccc03f17ed0f75387514d7ac06ce36`. No application/build reruns.

## Denominators and limitations

- Main Java execution: 1,531 `jdk.ExecutionSample` events total; runs 483 / 498 / 550. Percentages below are sample shares, not wall-clock percentages or exact CPU milliseconds.
- Main native: 1,211 `jdk.NativeMethodSample` events total; runs 397 / 407 / 407. Native samples include executing or waiting, and are analyzed separately.
- Closest app caller and phase/work buckets are disjoint. Inclusive methods below overlap; do not sum them. Each inclusive method is counted at most once per event, including recursive calls.
- Class loading is an orthogonal marker, not an extra phase to add to the phase percentages. JFR startup before the recording starts is not represented by these Java samples.
- Compiler durations overlap across C2/C1 threads and with main; their sums are not elapsed time or exact CPU.
- The three profiles establish hot work in 5.0. They do not by themselves attribute the entire 4.4x 4.9.7-to-5.0 difference to individual changes.

## Main Java phase, disjoint

| Phase | Aggregate | Run1 | Run2 | Run3 |
|---|---:|---:|---:|---:|
|read_sources_and_includes|39.84%|40.17%|38.96%|40.36%|
|resolve|30.70%|28.78%|31.53%|31.64%|
|save_prepare_write_cleanup|24.89%|26.92%|24.30%|23.64%|
|startup_environment_and_other|4.57%|4.14%|5.22%|4.36%|

Source/read includes exclude nested source lookups during resolution: the higher-priority resolve phase owns those. Save includes preparation, output validation, writing and cleanup.

## Main Java work, disjoint

| Work | Aggregate | Run1 | Run2 | Run3 |
|---|---:|---:|---:|---:|
|yaml_engine_construction|27.30%|27.12%|24.30%|30.18%|
|yaml_source_mask_events_restore|16.39%|16.15%|16.27%|16.73%|
|yaml_compose_nodes|12.67%|11.59%|14.66%|11.82%|
|yaml_emit|8.95%|9.32%|8.84%|8.73%|
|yaml_scalar_key_helpers|4.25%|5.59%|3.21%|4.00%|
|spel_expression|2.61%|2.28%|3.21%|2.36%|
|other|27.82%|27.95%|29.52%|26.18%|

YAML engine construction is any stack with `org.yaml.snakeyaml.Yaml.<init>`, taking priority over other work categories. Parser/emitter factory setup dominates this category. Constructors create resolver/constructor/representer maps repeatedly.

Closest application callers, exclusive:

- 332/1531 = 21.69%: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml$parser$1.<init>`
- 186/1531 = 12.15%: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml$parser$1.compose`
- 130/1531 = 8.49%: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml.serialize`
- 87/1531 = 5.68%: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml.emitter`
- 66/1531 = 4.31%: `io.microconfig.core.properties.io.yaml.impl.YamlSource.restore`
- 60/1531 = 3.92%: `io.microconfig.core.properties.io.yaml.impl.YamlSource.events_delegate$lambda$0`
- 48/1531 = 3.14%: `io.microconfig.core.properties.io.yaml.YamlScalars.indexProperties$microconfig_core`
- 35/1531 = 2.29%: `io.microconfig.core.properties.io.yaml.impl.YamlSource.statementPattern`
- 28/1531 = 1.83%: `io.microconfig.core.properties.resolvers.expression.impl.ExpressionEvaluator.evaluate`
- 22/1531 = 1.44%: `io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders$Companion.find`
- 19/1531 = 1.24%: `io.microconfig.core.properties.io.yaml.impl.YamlSource.<init>`
- 15/1531 = 0.98%: `io.microconfig.core.properties.repository.Include$Companion.parse`

Useful inclusive methods (do not add):

- 435/1531 = 28.41%: `io.microconfig.core.properties.io.yaml.impl.YamlValueResolver.resolveYamlProperty`
- 320/1531 = 20.90%: `io.microconfig.core.properties.io.yaml.impl.YamlSource.compose`
- 282/1531 = 18.42%: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml.parse`
- 217/1531 = 14.17%: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml.serialize`
- 57/1531 = 3.72%: `io.microconfig.core.properties.io.yaml.YamlScalars.quotedTextOrNull$microconfig_core`
- 48/1531 = 3.14%: `io.microconfig.core.properties.io.yaml.YamlScalars.indexProperties$microconfig_core`
- 28/1531 = 1.83%: `io.microconfig.core.properties.resolvers.expression.impl.ExpressionEvaluator.evaluate`

Class loading occurs on 50/1531 = 3.27% of main Java stacks (run1/2/3: 16/15/19). It is not the dominant sampled Java work.

## Native path checks

`FilePropertySerializer.resolveLinks` is closest application caller in 931/1,211 = 76.88% of native main samples; per-run 74.81% / 79.12% / 76.66%. Native save/prepare/write/cleanup phase is 1,006/1,211 = 83.07%.

Native leafs:

- 722/1211 = 59.62%: `sun.nio.fs.UnixNativeDispatcher.realpath0`
- 175/1211 = 14.45%: `sun.nio.fs.UnixNativeDispatcher.lstat0`
- 75/1211 = 6.19%: `java.io.FileInputStream.open0`
- 58/1211 = 4.79%: `sun.nio.fs.UnixNativeDispatcher.access0`
- 55/1211 = 4.54%: `java.io.UnixFileSystem.getBooleanAttributes0`
- 37/1211 = 3.06%: `java.util.zip.Inflater.inflateBytesBytes`

931 resolveLinks native samples break down by next caller: 566 `isProtected`, 227 `preparedSerializer`, 138 `output`. The path is `outputPath -> resolveLinks -> Path.toRealPath -> UnixNativeDispatcher.realpath0` (plus `Files.isSymbolicLink` / `Files.exists` checks).

Production locations: `FilePropertySerializer.kt:86-109`; repeated calls at `:168`, `:177`, `:187`, `:233`, `:266`, `:268`, `:275`. Optimization must preserve symlink aliases, output ownership, collision detection and failure handling.

The chosen destination is deep under the workspace. `resolveLinks` iterates every path component and `toRealPath` itself resolves a path; cost depends on output path depth. This profile does not establish the same path-check cost for a short `-d /tmp/out`.

## Compilers, separate

| Run | C2 events / summed duration | C1 events / summed duration |
|---|---:|---:|
|1|694 / 3054.4ms|2025 / 304.0ms|
|2|696 / 2976.2ms|2112 / 300.2ms|
|3|689 / 3343.3ms|2081 / 335.7ms|

Three C2 threads are active concurrently; a little over 1s of recorded compilation work on each. Largest summed compile targets across all3 runs:

- 496.7ms: `io.microconfig.core.properties.io.yaml.impl.YamlSource.maskStatements`
- 363.7ms: `org.yaml.snakeyaml.resolver.Resolver.resolve`
- 363.4ms: `io.microconfig.core.properties.io.yaml.impl.YamlSource.mask`
- 344.2ms: `org.yaml.snakeyaml.serializer.Serializer.serializeNode`
- 342.0ms: `org.yaml.snakeyaml.scanner.ScannerImpl.fetchMoreTokens`
- 238.5ms: `org.yaml.snakeyaml.scanner.ScannerImpl.scanPlain`
- 234.0ms: `org.yaml.snakeyaml.composer.Composer.composeNode`
- 228.8ms: `org.yaml.snakeyaml.Yaml.serialize`
- 201.2ms: `io.microconfig.core.properties.io.yaml.impl.SnakeYaml.serialize`
- 200.9ms: `org.yaml.snakeyaml.representer.SafeRepresenter.<init>`
- 148.3ms: `kotlin.collections.CollectionsKt___CollectionsKt.toCollection`
- 140.4ms: `org.yaml.snakeyaml.parser.ParserImpl.parseNode`

## Best validation targets

1. Repeated YAML engine construction: parsers/emitter creation is already 24–30% of sampled main Java work, and also drives constructor maps/allocations. Validate narrow reuse or avoidance of redundant scalar parses. Do not share mutable SnakeYaml state unsafely across threads or change options/anchor reset semantics.
2. Repeated path canonicalization in output preparation and cleanup: 75–79% of main native samples. Validate reuse of known prepared paths or a fast path preserving symlink/ownership correctness, with unprofiled cold CLI measurements.
3. YAML source masking/event traversal and repeated source/key/value parse/emit passes are the next Java work. A static scalar still goes through `YamlValueResolver.resolveYamlProperty` / `YamlSource.compose` before the resolver discovers there is no statement. Any shortcut must preserve explicit tags, quoted/literal strings, anchors/merges and type handling.
4. SpEL/placeholder micro-optimizations are lower priority for this fixture; `ExpressionEvaluator.evaluate` is only 1.83% inclusive of Java samples.

All artifacts in this analysis were generated from existing recordings only.
