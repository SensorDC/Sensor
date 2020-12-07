package org.sensor.evosuiteshell;

import java.io.File;

import org.sensor.conflict.util.MavenUtil;

public class Config {
    private static String MAVEN_PATH = "";
    public static String FILE_SEPARATOR = File.separator;
    public static String SENSOR_DIR = "sensor_testcase";
    public static String EVOSUITE_NAME = "evosuite-runtime-1.0.6.jar";

    public static String CLASSPATH_SEPARATOR = ";";

    public static String osName = System.getProperty("os.name");

    static {
        if (!System.getProperty("os.name").contains("Win")) {
            CLASSPATH_SEPARATOR = ":";
            MAVEN_PATH = "mvn";
        }
    }

    //项目junit测试用例目录
    public static String PROJECT_TESTCASE_DIR = System.getProperty("user.dir") + FILE_SEPARATOR + "src" + FILE_SEPARATOR + "test" + FILE_SEPARATOR + "java" + FILE_SEPARATOR;

    /**
     * get mvn.bat or mvn.cmd path
     *
     * @return
     */
    public static String getMaven() {
        if (!MAVEN_PATH.equals("")) {
            return MAVEN_PATH + " ";
        }
        String properties = System.getProperty("java.library.path");
        String mavenPath = "";
        String[] paths = properties.split(";");
        for (String path : paths) {
            if (path.contains("maven")) {
                mavenPath = path;
                break;
            }
        }
        if (new File(mavenPath + FILE_SEPARATOR + "+mvn.bat").exists())
            MAVEN_PATH = mavenPath + FILE_SEPARATOR + "mvn.bat";
        else if (new File(mavenPath + FILE_SEPARATOR + "mvn.cmd").exists())
            MAVEN_PATH = mavenPath + FILE_SEPARATOR + "mvn.cmd";
        if (MAVEN_PATH.equals("")) {
            MavenUtil.i().getLog().error("Please check or set maven home path!" + "MAVEN_PATH=" + MAVEN_PATH);
        }
        return MAVEN_PATH + " ";
    }

    public static void setMaven(String mavenPath) {
        MAVEN_PATH = mavenPath;
    }

//    public static void main(String[] args) {
//        System.out.println(System.getProperty("os.name"));
//        System.out.println(getMaven());
//        System.out.println(CLASSPATH_SEPARATOR);
//    }
}
