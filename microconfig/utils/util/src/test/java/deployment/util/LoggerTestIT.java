package deployment.util;

public class LoggerTestIT {
    public static void main(String[] args) {
        LoggerUtils.oneLineInfo("1111");
        LoggerUtils.oneLineInfo("2222222222222");
        LoggerUtils.oneLineInfo("33333333333333333333");
        LoggerUtils.oneLineInfo("4444");
    }
}