package deployment.mgmt.configs.service.properties;

import deployment.mgmt.atrifacts.Artifact;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static deployment.mgmt.atrifacts.Artifact.fromMavenString;
import static deployment.mgmt.atrifacts.ArtifactType.JAR;
import static java.util.List.of;
import static org.junit.Assert.*;

public class ArtifactTest {
    @Test
    public void testFromStringJava() {
        Artifact artifact = fromMavenString("com.oracle:openjdk-linux:gz:tar:x64_bin:11.0.1");
        assertEquals("com.oracle", artifact.getGroupId());
        assertEquals("openjdk-linux", artifact.getArtifactId());
        assertEquals("gz:tar:x64_bin", artifact.getClassifier());
        assertEquals("11.0.1", artifact.getVersion());
        assertEquals("com/oracle/openjdk-linux/11.0.1/openjdk-linux-11.0.1-x64_bin.tar.gz", artifact.toUrlPath(JAR));
    }

    @Test
    public void testFromString() {
        Artifact artifact = cassandraArtifact();
        assertEquals("org.apache.cassandra", artifact.getGroupId());
        assertEquals("apache-cassandra", artifact.getArtifactId());
        assertEquals("tar.gz:bin", artifact.getClassifier());
        assertEquals("3.0.10", artifact.getVersion());
        assertEquals("org/apache/cassandra/apache-cassandra/3.0.10/apache-cassandra-3.0.10-bin.tar.gz", artifact.toUrlPath(JAR));

        Artifact simple = simpleArtifact();
        assertEquals("a", simple.getGroupId());
        assertEquals("b", simple.getArtifactId());
        assertNull(simple.getClassifier());
        assertEquals("2", simple.getVersion());
    }

    @Test
    public void testNewVersion() {
        Artifact artifact = cassandraArtifact().withNewVersion("10.1");
        assertEquals("org.apache.cassandra", artifact.getGroupId());
        assertEquals("apache-cassandra", artifact.getArtifactId());
        assertEquals("tar.gz:bin", artifact.getClassifier());
        assertEquals("10.1", artifact.getVersion());

        assertEquals("org.apache.cassandra:apache-cassandra:tar.gz:bin:10.1", artifact.getMavenFormatString());
        assertEquals("org.apache.cassandra:apache-cassandra:10.1", artifact.withoutClassifier().getMavenFormatString());

        assertEquals("a:b:zip:2", Artifact.fromMavenString("a:b:zip:${version}").withNewVersion("2").getMavenFormatString());
    }

    @Test
    public void testPackaging() {
        Artifact artifact = fromMavenString("org.apache:apache-activemq:tar.gz:bin:1.2");
        assertEquals("tar.gz:bin", artifact.getClassifier());
        assertEquals("org/apache/apache-activemq/1.2/apache-activemq-1.2-bin.tar.gz", artifact.toUrlPath(JAR));

        Artifact jdkArtifact = fromMavenString("com.oracle:jdk-linux:gz:10.0.2");
        assertEquals("gz", jdkArtifact.getClassifier());
        assertEquals("com/oracle/jdk-linux/10.0.2/jdk-linux-10.0.2.gz", jdkArtifact.toUrlPath(JAR));
    }

    @Test
    public void fromFile() {
        Consumer<String> test = path -> {
            File file = new File(path);
            Artifact artifact = Artifact.fromFile(file);
            assertEquals("org.apache.cassandra", artifact.getGroupId());
            assertEquals("apache-cassandra", artifact.getArtifactId());
            assertNull(artifact.getClassifier());
            assertEquals("3.0.10", artifact.getVersion());
        };

        String path = "org/apache/cassandra/apache-cassandra/3.0.10/apache-cassandra-3.0.10.jar";
        test.accept("maven-repo/" + path);
        test.accept("/maven-repo/" + path);
        test.accept("/home/user/maven-repo/" + path);
        test.accept("..\\..\\home/user/maven-repo/" + path);
        test.accept("/" + path);
        test.accept(path);
    }

    @Test
    public void testJarPath() {
        assertEquals("org/apache/cassandra/apache-cassandra/3.0.10/apache-cassandra-3.0.10-bin.tar.gz", cassandraArtifact().toUrlPath(JAR));
        assertEquals("a/b/2/b-2.jar", simpleArtifact().toUrlPath(JAR));
    }

    private Artifact simpleArtifact() {
        return fromMavenString("a:b:2");
    }

    private Artifact cassandraArtifact() {
        return fromMavenString("org.apache.cassandra:apache-cassandra:tar.gz:bin:3.0.10");
    }
}