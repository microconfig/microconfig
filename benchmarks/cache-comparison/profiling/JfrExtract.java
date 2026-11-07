import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/** Streams relevant JFR events into compact JSON lines without retaining the recording. */
public class JfrExtract {
    private static final Set<String> STACK_EVENTS = Set.of(
        "jdk.ExecutionSample", "jdk.NativeMethodSample", "jdk.ObjectAllocationSample");
    private static final Set<String> OTHER_EVENTS = Set.of(
        "jdk.GarbageCollection", "jdk.GCHeapSummary", "jdk.Compilation",
        "jdk.ThreadCPULoad", "jdk.CPULoad", "jdk.FileRead", "jdk.FileWrite",
        "jdk.ThreadAllocationStatistics");

    public static void main(String[] args) throws Exception {
        try (var recording = new RecordingFile(Path.of(args[0]));
             var output = Files.newBufferedWriter(Path.of(args[1]))) {
            while (recording.hasMoreEvents()) {
                RecordedEvent event = recording.readEvent();
                String type = event.getEventType().getName();
                if (!STACK_EVENTS.contains(type) && !OTHER_EVENTS.contains(type)) continue;
                var line = new StringBuilder("{\"type\":");
                quoted(line, type);
                string(line, "time", event.getStartTime().toString());
                number(line, "duration_ms", event.getDuration().toNanos() / 1_000_000.0);
                RecordedThread thread = type.equals("jdk.ExecutionSample") || type.equals("jdk.NativeMethodSample")
                    ? event.getThread("sampledThread")
                    : type.equals("jdk.ThreadAllocationStatistics") ? event.getThread("thread") : event.getThread();
                string(line, "thread", thread == null ? "" : thread.getJavaName());
                if (STACK_EVENTS.contains(type)) {
                    line.append(",\"frames\":[");
                    var stack = event.getStackTrace();
                    boolean first = true;
                    if (stack != null) for (var frame : stack.getFrames()) {
                        if (!first) line.append(',');
                        first = false;
                        quoted(line, method(frame.getMethod()) + ":" + frame.getLineNumber());
                    }
                    line.append("],\"truncated\":").append(stack != null && stack.isTruncated());
                }
                switch (type) {
                    case "jdk.ObjectAllocationSample" -> {
                        number(line, "weight", event.getLong("weight"));
                        string(line, "object_class", event.getClass("objectClass").getName());
                    }
                    case "jdk.FileRead", "jdk.FileWrite" -> {
                        string(line, "path", event.getString("path"));
                        number(line, "bytes", event.getLong(type.equals("jdk.FileRead") ? "bytesRead" : "bytesWritten"));
                    }
                    case "jdk.GarbageCollection" -> {
                        number(line, "pause_ms", event.getDuration("sumOfPauses").toNanos() / 1_000_000.0);
                        string(line, "cause", event.getString("cause"));
                    }
                    case "jdk.GCHeapSummary" -> {
                        number(line, "heap_used", event.getLong("heapUsed"));
                        string(line, "when", event.getString("when"));
                    }
                    case "jdk.ThreadAllocationStatistics" -> number(line, "allocated", event.getLong("allocated"));
                    case "jdk.ThreadCPULoad" -> {
                        number(line, "user", event.getFloat("user"));
                        number(line, "system", event.getFloat("system"));
                    }
                    case "jdk.Compilation" -> {
                        string(line, "method", method(event.getValue("method")));
                        string(line, "compiler", event.getString("compiler"));
                    }
                }
                output.write(line.append("}\n").toString());
            }
        }
    }

    private static String method(RecordedMethod method) {
        return method.getType().getName().replace('/', '.') + "." + method.getName();
    }

    private static void string(StringBuilder out, String key, String value) {
        out.append(','); quoted(out, key); out.append(':'); quoted(out, value);
    }

    private static void number(StringBuilder out, String key, double value) {
        out.append(','); quoted(out, key); out.append(':').append(value);
    }

    private static void quoted(StringBuilder out, String value) {
        if (value == null) { out.append("null"); return; }
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        out.append('"');
    }
}
