#!/usr/bin/env python3
"""Analyze compact JFR JSONL allocation byte weights and recorded I/O/GC events."""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path

CATEGORIES = {
    'sourceConfigFileParse': ('repository.ConfigFile.parseUsing',),
    'sourceReaderScalarKeys': ('yaml.impl.SnakeYamlReader.keyPart',),
    'yamlReaderAll': ('yaml.impl.SnakeYamlReader.',),
    'yamlSourceCompose': ('yaml.impl.YamlSource.compose',),
    'yamlParserConstruction': ('yaml.impl.SnakeYaml.parser',),
    'yamlEmitterConstruction': ('yaml.impl.SnakeYaml.emitter',),
    'yamlTreeBuild': ('yaml.impl.SnakeYamlTree$PropertyTree.',),
    'yamlScalarReadBack': ('yaml.YamlScalars.readBack',),
    'yamlQuotedText': ('yaml.YamlScalars.quotedTextOrNull',),
    'propertyIndexing': ('yaml.YamlScalars.indexProperties',),
    'typedPropertiesConstruction': ('properties.impl.TypedPropertiesImpl.<init>',),
    'yamlValueResolution': ('yaml.impl.YamlValueResolver.',),
    'outputSerializer': ('serializers.impl.FilePropertySerializer.',),
    'configDiff': ('serializers.impl.ConfigDiff.',),
    'snakeYamlLibrary': ('org.yaml.snakeyaml.',),
    'springLibrary': ('org.springframework.',),
}


def new_allocation_summary():
    return {
        'weightBytes': 0, 'observedSamples': 0, 'truncatedWeightBytes': 0,
        'missingStackWeightBytes': 0, 'objectClasses': Counter(), 'leafMethods': Counter(),
        'nearestAppMethods': Counter(), 'inclusiveAppMethods': Counter(),
        'inclusiveCategories': Counter(), 'phases': Counter(),
    }


def add_allocation(summary, event):
    weight = int(event['weight'])
    frames = [frame.rsplit(':', 1)[0] for frame in event.get('frames', [])]
    summary['weightBytes'] += weight
    summary['observedSamples'] += 1
    summary['objectClasses'][event['object_class']] += weight
    if event.get('truncated'):
        summary['truncatedWeightBytes'] += weight
    if not frames:
        summary['missingStackWeightBytes'] += weight
        return
    summary['leafMethods'][frames[0]] += weight
    app = [frame for frame in frames if frame.startswith('io.microconfig.')]
    if app:
        summary['nearestAppMethods'][app[0]] += weight
    for frame in set(app):
        summary['inclusiveAppMethods'][frame] += weight
    for label, patterns in CATEGORIES.items():
        if any(pattern in frame for pattern in patterns for frame in frames):
            summary['inclusiveCategories'][label] += weight
    if any('repository.ConfigFile.parseUsing' in frame for frame in frames):
        phase = 'sourceConfigFileParse'
    elif any('serializers.impl.FilePropertySerializer.' in frame for frame in frames):
        phase = 'outputPreparationOrWriting'
    elif any('yaml.impl.YamlValueResolver.' in frame for frame in frames):
        phase = 'yamlValueResolution'
    elif any('properties.impl.TypedPropertiesImpl.<init>' in frame for frame in frames):
        phase = 'typedPropertiesConstructionOutsideParsing'
    elif app:
        phase = 'otherApplication'
    else:
        phase = 'noApplicationFrame'
    summary['phases'][phase] += weight


def describe(summary):
    result = {key: value for key, value in summary.items() if not isinstance(value, Counter)}
    result['estimatedMiB'] = round(result['weightBytes'] / 2**20, 3)
    for name, values in summary.items():
        if isinstance(values, Counter):
            result[name] = [
                {'name': key, 'weightBytes': value, 'MiB': round(value / 2**20, 3),
                 'percentOfThreadWeight': round(100 * value / summary['weightBytes'], 3)}
                for key, value in sorted(values.items(), key=lambda item: (-item[1], str(item[0])))[:30]
            ]
    return result


def analyze(path):
    output_prefix = str(path.resolve().parent) + "/output-"
    threads = defaultdict(new_allocation_summary)
    event_counts = Counter()
    gc = []
    heap = defaultdict(list)
    allocation_statistics = defaultdict(list)
    common_reads = Counter()
    io = defaultdict(lambda: {'recordedEvents': 0, 'positiveBytesEvents': 0, 'bytes': 0, 'durationMs': 0.0})
    with path.open() as handle:
        for line in handle:
            event = json.loads(line)
            kind = event['type']
            event_counts[kind] += 1
            thread = event.get('thread') or '<none>'
            if kind == 'jdk.ObjectAllocationSample':
                add_allocation(threads[thread], event)
            elif kind == 'jdk.GarbageCollection':
                gc.append({'pauseMs': event.get('pause_ms', 0), 'durationMs': event['duration_ms'], 'cause': event['cause']})
            elif kind == 'jdk.GCHeapSummary':
                heap[event['when']].append(event['heap_used'])
            elif kind == 'jdk.ThreadAllocationStatistics':
                allocation_statistics[thread].append(event['allocated'])
            elif kind in ('jdk.FileRead', 'jdk.FileWrite'):
                target = event.get('path') or ''
                label = 'other'
                if '/fixture/components/common-l' in target:
                    label = 'fixtureSharedIncludes'
                elif '/fixture/components/svc' in target:
                    label = 'fixtureServices'
                elif '/fixture/envs/' in target:
                    label = 'fixtureEnvironments'
                elif target.startswith(output_prefix):
                    label = 'outputs'
                elif target.endswith('.jar'):
                    label = 'jar'
                record = io[kind + ':' + label]
                record['recordedEvents'] += 1
                record['durationMs'] += event['duration_ms']
                count = max(0, int(event.get('bytes', 0)))
                record['bytes'] += count
                if count:
                    record['positiveBytesEvents'] += 1
                    if kind == 'jdk.FileRead' and label == 'fixtureSharedIncludes':
                        common_reads[Path(target).parent.name] += 1
    result = {
        'input': str(path), 'eventCounts': dict(event_counts),
        'threads': {name: describe(value) for name, value in sorted(threads.items(), key=lambda pair: -pair[1]['weightBytes'])},
        'gc': {'events': len(gc), 'pauseMs': sum(row['pauseMs'] for row in gc),
               'durationMs': sum(row['durationMs'] for row in gc),
               'causes': dict(Counter(row['cause'] for row in gc)), 'individual': gc},
        'heapSummaryBytes': {name: {'min': min(values), 'max': max(values), 'observations': len(values)} for name, values in heap.items()},
        'threadAllocationStatistics': {name: {'minObservedBytes': min(values), 'maxObservedBytes': max(values), 'observedDeltaBytes': max(values)-min(values), 'observations': len(values)} for name, values in allocation_statistics.items()},
        'io': dict(io), 'positiveSharedIncludeReadEventsByComponent': dict(sorted(common_reads.items())),
    }
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inputs', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    result = {
        'interpretation': 'ObjectAllocationSample.weight estimates allocated byte volume; observedSamples are JFR event counts, not allocated object counts. Inclusive rows overlap. I/O event counts are recorded read/write calls, not file-open counts. ThreadAllocationStatistics snapshots need not include process termination.',
        'runs': [analyze(path) for path in args.inputs],
    }
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    for run in result['runs']:
        main = run['threads'].get('main', {})
        print(Path(run['input']).name, 'main estimatedMiB=', main.get('estimatedMiB'), 'samples=', main.get('observedSamples'), 'GC pauseMs=', run['gc']['pauseMs'])
        for row in main.get('inclusiveCategories', []):
            print(' ',row['name'], row['MiB'], str(row['percentOfThreadWeight'])+'%')
        print(' phases',main.get('phases'))
        print(' shared reads',run['io'].get('jdk.FileRead:fixtureSharedIncludes'))


if __name__ == '__main__':
    main()
