package deployment.mgmt.atrifacts.changes;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.factory.MgmtFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;

public class ClasspathDiffBuilderImplTestIT {
    @Test
    public void compare() {
        MgmtFactory mgmtFactory = new MgmtFactory();

        List<String> services = mgmtFactory.getComponentGroupService().getServices();
        ClasspathService classpathService = mgmtFactory.getClasspathService();

        long time = System.currentTimeMillis();
        services.stream().forEach(service -> {
            List<File> files = classpathService.classpathFor(service).current().asFiles();
            if (files.isEmpty()) return;

            long t = currentTimeMillis();
            ClasspathDiff diff = ClasspathDiff.builder("s1").indexPreviousClasspath(files).indexCurrentClasspath(emptyList()).compare();
            System.out.println(currentTimeMillis() - t);
        });
        System.out.println(System.currentTimeMillis() - time);
    }

    public static void main(String[] args) {
        ClasspathDiff diff = ClasspathDiff.builder("zuul")
                .indexPreviousClasspath(List.of(new File("C:/Users/amatorin/Documents/hash/1/zuul-RP-18.21-SNAPSHOT.jar")))
                .indexCurrentClasspath(List.of(new File("C:/Users/amatorin/Documents/hash/2/zuul-RP-18.21-SNAPSHOT.jar")))
                .compare();

        System.out.println();
    }
}