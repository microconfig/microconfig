package deployment.mgmt.atrifacts.changes;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static io.microconfig.utils.IoUtils.readChunked;
import static java.util.Comparator.comparing;

@RequiredArgsConstructor
class Hasher {
    private final String name;
    private final Checksum c1 = new CRC32();
    private final Checksum c2 = new Adler32();

    public Hasher hash(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return hash(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Hasher hash(InputStream inputStream) {
        readChunked(inputStream, (bytes, length) -> {
            c1.update(bytes, 0, length);
            c2.update(bytes, 0, length);
        });

        return this;
    }

    public String value() {
        return c1.getValue() + "|" + c2.getValue();
    }

    public static String reduce(List<Hasher> hashers) {
        ToLongFunction<Function<Hasher, Checksum>> reduce = checksumGetter -> hashers.stream()
                .sorted(comparing(h -> h.name))
                .map(checksumGetter)
                .mapToLong(Checksum::getValue)
                .filter(v -> v != 0)
                .reduce(hashers.size(), (v1, v2) -> 31 * v1 + v2);

        long c1 = reduce.applyAsLong(h -> h.c1);
        long c2 = reduce.applyAsLong(h -> h.c2);

        return c1 + "|" + c2;
    }
}