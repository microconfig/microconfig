package mgmt.utils;

import java.io.*;

import static io.microconfig.utils.FileUtils.createFile;

public class FileLogger implements AutoCloseable {
    private final PrintWriter writer;

    public FileLogger(File file) {
        try {
            this.writer = new PrintWriter(file == null ? new StringWriter() : new BufferedWriter(new FileWriter(createFile(file))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void info(String log) {
        writer.println(log);
    }

    public void error(Exception e) {
        e.printStackTrace(writer);
    }

    @Override
    public void close() {
        writer.close();
    }
}