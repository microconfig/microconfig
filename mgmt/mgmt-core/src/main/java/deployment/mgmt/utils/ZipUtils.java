package deployment.mgmt.utils;

import io.microconfig.utils.FileUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry.PERMISSION;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static io.microconfig.utils.FileUtils.createDir;
import static io.microconfig.utils.FileUtils.delete;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.attribute.PosixFilePermission.*;
import static java.util.Objects.requireNonNull;
import static java.util.Set.of;
import static java.util.stream.Collectors.toSet;
import static mgmt.utils.OsUtil.isWindows;

public class ZipUtils {
    public static byte[] readInnerFile(File zipArchive, String innerFile) {
        try (ZipInputStream in = newZipStream(zipArchive);) {
            ZipInputStream is = requireNonNull(findInnerFile(in, innerFile), () -> "Can't find " + innerFile + " inside " + zipArchive);
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsInnerFile(File zipArchive, String innerFile) {
        try (ZipInputStream in = newZipStream(zipArchive);) {
            return findInnerFile(in, innerFile) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static ZipInputStream findInnerFile(ZipInputStream in, String innerFile) throws IOException {
        ZipEntry nextEntry;
        while ((nextEntry = in.getNextEntry()) != null) {
            if (nextEntry.getName().equals(innerFile)) {
                return in;
            }
        }

        return null;
    }

    public static void forEachInnerFiles(File zipArchive, BiConsumer<ZipEntry, ZipInputStream> consumer) {
        try (ZipInputStream in = newZipStream(zipArchive)) {
            ZipEntry nextEntry;
            while ((nextEntry = in.getNextEntry()) != null) {
                consumer.accept(nextEntry, in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void zip(File[] sources, File destinationZip) {
        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(destinationZip))) {
            for (File source : sources) {
                zipStream.putNextEntry(new ZipEntry(source.getName()));
                try (InputStream stream = new FileInputStream(source)) {
                    IOUtils.copy(stream, zipStream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Stream.of(sources).forEach(FileUtils::delete);
    }

    public static void unzip(File archive, File destination) {
        try (ArchiveInputStream i = createArchiveInputStream(archive)) {
            ArchiveEntry entry;
            while ((entry = i.getNextEntry()) != null) {
                File f = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    delete(f);
                    createDirWithPermissions(f);
                } else {
                    createDirWithPermissions(f.getParentFile());
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                        copyPermissions(entry, f);
                    }
                }

            }
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException(e);
        }
    }

    public static void unzip(File archive) {
        unzip(archive, archive.getParentFile());
    }

    private static void createDirWithPermissions(File dir) {
        List<File> notExist = new LinkedList<>();
        while (!dir.exists()) {
            notExist.add(0, dir);
            dir = dir.getParentFile();
        }

        for (File d : notExist) {
            createDir(d);
            d.setWritable(true, false);
            d.setReadable(true, false);
            d.setExecutable(true, false);
        }
    }

    private static void copyPermissions(ArchiveEntry entry, File f) {
        if (isWindows()) return;

        IntSupplier permissionsMode = () -> {
            if (entry instanceof TarArchiveEntry) return ((TarArchiveEntry) entry).getMode();
            if (entry instanceof ZipArchiveEntry) return ((ZipArchiveEntry) entry).getUnixMode();
            return -1;
        };

        int mode = permissionsMode.getAsInt();

        Map<PERMISSION, Set<PosixFilePermission>> map = Map.of(
                PERMISSION.USER_READ, of(OWNER_READ, GROUP_READ),
                PERMISSION.USER_WRITE, of(OWNER_WRITE, GROUP_WRITE),
                PERMISSION.USER_EXEC, of(OWNER_EXECUTE, GROUP_EXECUTE),
                PERMISSION.GROUP_READ, of(GROUP_READ),
                PERMISSION.GROUP_WRITE, of(GROUP_WRITE),
                PERMISSION.GROUP_EXEC, of(GROUP_EXECUTE),
                PERMISSION.WORLD_READ, of(OTHERS_READ),
                PERMISSION.WORLD_WRITE, of(OWNER_WRITE),
                PERMISSION.WORLD_EXEC, of(OTHERS_EXECUTE)
        );

        try {
            setPosixFilePermissions(f.toPath(), getPermissions(mode, map));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private static Set<PosixFilePermission> getPermissions(int mode, Map<PERMISSION, Set<PosixFilePermission>> map) {
        if (mode <= 0) return Set.of(
                OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                GROUP_READ, GROUP_WRITE, GROUP_EXECUTE
        );

        return PERMISSION.find(mode).stream()
                .map(map::get)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    private static ArchiveInputStream createArchiveInputStream(File archive) throws IOException, ArchiveException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(archive));
        return archive.getName().endsWith(".gz") ? new TarArchiveInputStream(new GzipCompressorInputStream(inputStream))
                : new ArchiveStreamFactory().createArchiveInputStream(inputStream);
    }

    private static ZipInputStream newZipStream(File zipArchive) throws FileNotFoundException {
        return new ZipInputStream(new FileInputStream(zipArchive));
    }
}