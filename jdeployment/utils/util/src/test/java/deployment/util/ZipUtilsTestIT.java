package deployment.util;

import java.io.File;

import static deployment.util.ZipUtils.unzip;

public class ZipUtilsTestIT {
    public static void main(String[] args) {
//        unzip(new File("C:/Users/amatorin/Downloads/demo.zip"));
        unzip(new File("C:/Users/amatorin/Downloads/demo.zip"), new File("C:/Temp/config-repo"));
    }
}