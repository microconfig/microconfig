package deployment.mgmt.stat.monitoring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Consumer;

import static deployment.mgmt.utils.JsonUtil.toJson;
import static io.microconfig.utils.ConsoleColor.green;
import static io.microconfig.utils.Logger.align;
import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class MemoryUsage {
    private final String name;
    private final String description;
    private final int valueInMb;
    private final List<MemoryUsage> children;

    public static MemoryUsage statFor(String type, String description, List<MemoryUsage> children) {
        return new MemoryUsage(type, description, children.stream().mapToInt(MemoryUsage::getValueInMb).sum(), children);
    }

    public static MemoryUsage statFor(String type, String description, int valueInMb) {
        return new MemoryUsage(type, description, valueInMb, emptyList());
    }

    public void outputTo(Consumer<String> writeTo) {
        int align = 1 + children.stream()
                .map(MemoryUsage::getName)
                .mapToInt(String::length)
                .max()
                .orElse(0);

        children.forEach(s -> writeTo.accept(green(align(s.getName(), align)) + "-> " + s.getValueInMb() + " mb"));
        writeTo.accept(green(align("Summary", align) + "-> " + valueInMb + " mb"));
    }

    public void outputAsJsonTo(Consumer<String> writeTo, boolean pretty) {
        String json = toJson(this, pretty)
                .replaceAll(",\\s*\"children\":\\s*\\[]", "");

        writeTo.accept(json);
    }

    @Override
    public String toString() {
        return name + " " + description;
    }
}