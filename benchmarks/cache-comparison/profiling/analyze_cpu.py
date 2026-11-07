from collections import Counter,defaultdict
from pathlib import Path
import argparse,json
parser=argparse.ArgumentParser()
parser.add_argument('--recordings',type=Path,default=Path(__file__).with_name('recordings.json'))
parser.add_argument('--output',type=Path,default=Path(__file__).with_name('cpu-summary.json'))
args=parser.parse_args()
META=args.recordings
APP='io.microconfig.'
def method(f): return f.rsplit(':',1)[0]
def phase(fs):
 if any(f.startswith('io.microconfig.core.properties.impl.PropertiesImpl.save') for f in fs): return 'save_prepare_write_cleanup'
 if any(f.startswith('io.microconfig.core.properties.impl.PropertiesImpl.resolveBy') for f in fs): return 'resolve'
 if any(f.startswith(('io.microconfig.core.environments.impl.ComponentsImpl.getPropertiesFor','io.microconfig.core.properties.impl.PropertiesFactoryImpl.getPropertiesOf')) for f in fs): return 'read_sources_and_includes'
 return 'startup_environment_and_other'
def work(fs):
 if any(f.startswith('org.yaml.snakeyaml.Yaml.<init>') for f in fs): return 'yaml_engine_construction'
 if any(f.startswith('io.microconfig.core.properties.io.yaml.impl.SnakeYaml$parser$1.compose') for f in fs): return 'yaml_compose_nodes'
 if any(f.startswith('io.microconfig.core.properties.io.yaml.impl.SnakeYaml.serialize') for f in fs): return 'yaml_emit'
 a=next((f for f in fs if f.startswith(APP)),'')
 if a.startswith('io.microconfig.core.properties.io.yaml.impl.YamlSource.'): return 'yaml_source_mask_events_restore'
 if a.startswith('io.microconfig.core.properties.io.yaml.YamlScalars.'): return 'yaml_scalar_key_helpers'
 if a.startswith('io.microconfig.core.properties.resolvers.expression.'): return 'spel_expression'
 return 'other'
def classloading(fs):
 return any((f.startswith(('java.lang.ClassLoader.','jdk.internal.loader.')) and any(k in f for k in ['loadClass','findClass','defineClass'])) or f.startswith(('java.lang.Class.forName','jdk.internal.misc.Unsafe.ensureClassInitialized')) for f in fs)
def rows(c,total=None,n=35):
 return [{'name':str(k),'count':v,**({'percent':round(100*v/total,4)} if total else {})} for k,v in sorted(c.items(),key=lambda item:(-item[1],str(item[0])))[:n]]
def analyze(events):
 ts=Counter(); execthreads=Counter(); native_threads=Counter(); leaf=Counter(); first=Counter(); inc=Counter(); incapp=Counter(); ph=Counter(); workc=Counter(); workphase=defaultdict(Counter); nph=Counter(); nfirst=Counter(); nleaf=Counter(); ninc=Counter(); npath=Counter(); compcount=Counter(); comps=Counter(); compmethods=Counter(); compt=Counter(); cpu=defaultdict(list); iofiles=Counter(); io_ms=Counter(); classloads=0; samplestack=Counter();native_total=0;ex_total=0;maxtail=0;norm=[]
 for e in events:
  ty=e['type'];ts[ty]+=1; fs=[method(f) for f in e.get('frames',[])]
  if ty=='jdk.ExecutionSample':
   execthreads[e.get('thread')]+=1
   if e.get('thread')!='main':continue
   ex_total+=1
   if classloading(fs):classloads+=1
   if fs: leaf[fs[0]]+=1
   for f in set(fs):
    inc[f]+=1
    if f.startswith(APP):incapp[f]+=1
   first[next((f for f in fs if f.startswith(APP)),'(no application frame)')]+=1
   p=phase(fs);ph[p]+=1; w=work(fs);workc[w]+=1;workphase[w][p]+=1
   if len(fs)>=128:maxtail+=1
  elif ty=='jdk.NativeMethodSample':
   native_threads[e.get('thread')]+=1
   if e.get('thread')!='main':continue
   native_total+=1
   nph[phase(fs)]+=1
   a=next((f for f in fs if f.startswith(APP)),'(no application frame)');nfirst[a]+=1
   if fs:nleaf[fs[0]]+=1
   for f in set(fs): ninc[f]+=1
   if 'io.microconfig.core.properties.serializers.impl.FilePropertySerializer.resolveLinks' in fs:
    idx=fs.index('io.microconfig.core.properties.serializers.impl.FilePropertySerializer.resolveLinks'); npath[' -> '.join(fs[idx:idx+5])]+=1
  elif ty=='jdk.Compilation':
   compcount[e['compiler']]+=1;comps[e['compiler']]+=e['duration_ms'];compmethods[e['method']]+=e['duration_ms'];compt[e.get('thread')]+=e['duration_ms']
  elif ty=='jdk.ThreadCPULoad':cpu[e.get('thread')].append({'time':e['time'],'user':e['user'],'system':e['system']})
  elif ty=='jdk.FileRead':iofiles[e.get('path')]+=1;io_ms[e.get('path')]+=e['duration_ms']
 return {'event_counts':dict(ts),'execution_sample_threads':dict(execthreads),'native_sample_threads':dict(native_threads),'main_java_samples':ex_total,'main_native_samples':native_total,'main_java_first_app_caller':rows(first,ex_total,80),'main_java_leaf':rows(leaf,ex_total,50),'main_java_inclusive':rows(inc,ex_total,100),'main_java_inclusive_app':rows(incapp,ex_total,110),'main_java_phase_disjoint':rows(ph,ex_total,10),'main_java_work_disjoint':rows(workc,ex_total,10),'main_java_work_phase':dict(workphase),'classloading_main_java_samples':classloads,'frames_at_depth_limit':maxtail,'main_native_phase_disjoint':rows(nph,native_total,10),'main_native_first_app_caller':rows(nfirst,native_total,40),'main_native_leaf':rows(nleaf,native_total,25),'main_native_inclusive':rows(ninc,native_total,40),'resolve_links_native_callers':rows(npath,native_total,20),'compilation_counts':dict(compcount),'compilation_summed_duration_ms':dict(comps),'compilation_summed_duration_by_thread_ms':dict(compt),'compilation_top_methods_by_summed_duration_ms':rows(compmethods,None,40),'thread_cpu_load_samples':dict(cpu),'file_read_top_count':rows(iofiles,None,15),'file_read_top_summed_duration_ms':rows(io_ms,None,15)}
meta=json.loads(META.read_text());runs=[];all_events=[]
for r in meta['runs']:
 ev=[json.loads(line) for line in Path(r['compact_jsonl']).open()];all_events.extend(ev); a=analyze(ev);a.update({'round':r['round'],'profiled_wall_ms':r['profiled_wall_ms']});runs.append(a)
agg=analyze(all_events)
report={'methodology':{'cpu_denominator':'Main-thread jdk.ExecutionSample counts (not wall-clock or absolute CPU milliseconds). First-app and phases/work categories are disjoint; inclusive method counts overlap and cannot be summed.','native_denominator':'Main-thread jdk.NativeMethodSample counts, separate from Java execution; may include waiting.','compiler_duration':'Sum of jdk.Compilation durations across concurrent compiler threads, not elapsed time or exact CPU time.','phase_priority':['PropertiesImpl.save => save/prepare/write/cleanup','PropertiesImpl.resolveBy => resolution, including nested property lookup','ComponentsImpl.getPropertiesFor or PropertiesFactoryImpl.getPropertiesOf => source read/includes','otherwise startup/environment/other'],'work_priority':['Yaml.<init>','SnakeYaml parser compose','SnakeYaml.serialize','closest app YamlSource','closest app YamlScalars','closest app expression','other'],'classloading':'orthogonal main Java stack occurrence of ClassLoader load/find/defineClass, internal loader equivalents, Class.forName, or Unsafe.ensureClassInitialized'},'runs':runs,'aggregate':agg}
out=args.output;out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
for a in runs+[dict(agg,round='ALL')]:
 print('\nRUN',a['round'],'JAVA',a['main_java_samples'],'NATIVE',a['main_native_samples'],'classload',a['classloading_main_java_samples'])
 for key in ['main_java_phase_disjoint','main_java_work_disjoint','main_java_first_app_caller','main_java_leaf','main_native_phase_disjoint','main_native_first_app_caller']:
  print(key)
  for row in a[key][:14]: print(row)
 print('compile',a['compilation_counts'],a['compilation_summed_duration_ms'])
 print('work_phase',a['main_java_work_phase'])
