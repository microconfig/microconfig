# Профиль Microconfig 5.0.0

Профилировалась только текущая версия 5.0.0 из исходного сравнения:
SHA-256 JAR `7453400022b70a1aa5921265ffa44cfdb9ccc03f17ed0f75387514d7ac06ce36`.
Версия 4.9.7 повторно не запускалась и не профилировалась.

Тот же fixture: 50 собираемых сервисов, общий include-граф глубиной 3,
100 результирующих файлов и 8850 свойств. Все профилируемые запуски дали побайтово
одинаковый результат с исходным бенчмарком. Production-код не менялся.

## Основные выводы

### 1. Большая часть Java-работы связана с YAML

В трёх запусках набрано 1531 Java execution sample основного потока.
Непересекающиеся категории:

| Работа | Доля Java samples |
| --- | ---: |
| Создание YAML parser/emitter | 27,3% |
| Подготовка YAML source: masking, события, восстановление | 16,4% |
| YAML compose | 12,7% |
| YAML serialize | 8,9% |
| Scalar/key helpers | 4,3% |

Объекты `Yaml` создаются многократно для отдельных ключей и значений.
Особенно заметны `SnakeYamlTree.keyNode`, `SnakeYamlReader.keyPart`,
`SnakeYamlWriter.serializedValue`, `YamlSource.compose/events` и
`YamlScalars.quotedTextOrNull`.

Например, при обработке ключа `SnakeYamlTree.put` и `find` повторно вызывают
`keyNode`, который запускает полноценный parser. При чтении обычного строкового
ключа `SnakeYamlReader.keyPart` создаёт emitter для scalar dump.

`YamlValueResolver.resolveYamlProperty` разбирает значение через `YamlSource`,
а последующий `resolveScalar` отдельно разбирает его для проверки кавычек —
даже для многих значений без выражений и ссылок.

### 2. Большой объём временных объектов

Сумма весов allocation samples основного потока оценивает около **2,00 GiB
аллокаций на одну сборку**. Это суммарно выделенные байты, не RSS и не живой heap.

| Фаза по стеку аллокации | Доля |
| --- | ---: |
| Подготовка и запись output | 51,4–51,5% |
| Разрешение YAML-значений | 32,2–32,4% |
| Разбор исходных ConfigFile | 12,7–13,6% |

Около 596–643 MiB allocation weight проходит через `SnakeYamlTree.keyNode`
(inclusive). Преобладают массивы, узлы HashMap, списки и `ResolverTuple`.
Это указывает на лишнее создание инфраструктуры parser/emitter и повторные
parse/serialize-проходы. Семь GC за запуск дали 19–21 мс пауз; наблюдаемый heap
после GC составлял 5–13 MiB. Эти данные показывают churn, а не доказывают утечку.

### 3. Повторная нормализация выходных путей

`FilePropertySerializer.resolveLinks` присутствует в **76,9% native samples**
основного потока, преимущественно в `realpath0` и `lstat0`.

В отдельном инструментированном запуске:

| Метод | Вызовы | Среднее | Приблизительная сумма |
| --- | ---: | ---: | ---: |
| `MicroconfigRunner.buildProperties` | 1 | 855 мс | 855 мс |
| `PropertiesImpl.save` | 1 | 780 мс | 780 мс |
| `ConfigFile.parseUsing` | 2050 | 0,184 мс | 377 мс |
| `FilePropertySerializer.outputPath` | 2850 | 0,151 мс | 430 мс |
| `FilePropertySerializer.resolveLinks` | 2850 | 0,151 мс | 430 мс |
| `TypedPropertiesImpl.<init>` | 4300 | 0,00369 мс | 16 мс |

Вложенные строки таблицы пересекаются: `outputPath` включает `resolveLinks`,
а `save` включает обе. Складывать все суммы нельзя. Длительности MethodTiming
приблизительные и получены под инструментированием, не из обычного бенчмарка.

Пути проверяются повторно в `output`, `isProtected`, `preparedSerializer`,
подготовке уведомлений и cleanup. Планируется 450 outputs по типам конфигурации,
хотя реально записываются 100 файлов. В `resolveLinks` для каждого сегмента
пути выполняются проверки symlink/exists и `toRealPath`.

Контрольный тест без JFR менял только глубину output directory, не код:
три пары холодных запусков дали медианы 1671 мс для глубокого и 1651 мс для
короткого пути, с заметным разбросом. Убедительного ускорения от одного лишь
укорочения пути не получено; эффект устранения повторных проверок пока не измерен.

### 4. Includes повторно разбираются, но чтение данных с диска мало

На каждый из 39 общих include-файлов приходится 50 положительных FileRead events:
1950 таких чтений за сборку. Их суммарная записанная длительность — 3,8–5,0 мс.
Эта метрика не включает стоимость всех open/stat и прочих операций.

2050 `ConfigFile.parseUsing` включают 2000 application-файловых разборов
(50 корней + 50 × 39 общих узлов) и 50 process-файлов. Работа состоит главным
образом из повторного разбора и подготовки данных, а не передачи байтов с диска.

### Что оказалось второстепенным на этом fixture

- `indexProperties`: около 2,45% allocation weight, 3,14% Java samples inclusive.
  Все 4300 конструкторов TypedProperties заняли примерно 16 мс в MethodTiming.
- `ExpressionEvaluator.evaluate`: 1,83% Java samples inclusive.
- Загрузка классов встречается в 3,27% записанных Java-стеков main. Это не
  измерение всей стоимости запуска JVM до начала записи.
- Scalar `readBack`: менее 0,2% allocation weight.

Возврат прежнего универсального кэша сам по себе не устраняет выявленные
повторные YAML-проходы и проверки output paths. Следующие эксперименты стоит
начать с повторного создания YAML engines, повторного разбора ключей/значений,
переиспользования уже подготовленных путей и разобранных общих файлов.
Проверки типов, тегов, aliases, symlinks и принадлежности output должны сохраняться.

## Методика и артефакты

- Три независимых холодных запуска `java -jar`, OpenJDK 25.0.2, без прогрева.
- Java/native sampling 1 ms, allocation sampling до 1000 событий/с,
  stack depth 128, FileRead/FileWrite threshold 0.
- Дополнительно один холодный запуск с JFR MethodTiming только для указанных
  методов; application bytecode исходного JAR не менялся.
- JFR добавляет overhead. Проценты samples не являются долями wall time;
  Java и native samples анализируются отдельно. Allocation weight — оценка.
- Длительности параллельных JIT-компиляций нельзя складывать с main wall time.

Файлы:

- `recordings.json`: команды, пути к трём `.jfr`, логам и компактным JSONL.
- `profile.jfc`, `method-timing.jfc`: настройки записи.
- `method-timing.txt`: исходный вывод JFR MethodTiming.
- `cpu-summary.json`, `cpu-details.md`: CPU/native/JIT сводки.
- `allocation-summary.json`, `allocation-targeted.json`: аллокации, I/O и GC.
- `path-depth-results.json`: контроль глубины пути без JFR, только 5.0.0.
- `JfrExtract.java`: потоковый экспорт нужных полей JFR в JSONL.
- `analyze_cpu.py`, `analyze_allocations.py`: расчёт сводок из JSONL.

Raw recordings и JSONL находятся под игнорируемым `../work/profiles-*/`.
Открыть `.jfr` можно в JDK Mission Control или другом совместимом просмотрщике.

Запуск из корня проекта, с новым пустым output directory:

```sh
java -XX:FlightRecorderOptions=stackdepth=128 \
  -XX:StartFlightRecording=settings=benchmarks/cache-comparison/profiling/profile.jfc,filename=benchmarks/cache-comparison/work/profile.jfr,dumponexit=true \
  -jar /path/to/microconfig-cli-5.0.0.jar \
  -r benchmarks/cache-comparison/fixture -e dev -d /path/to/empty-output
jfr view --width 160 hot-methods benchmarks/cache-comparison/work/profile.jfr
```
