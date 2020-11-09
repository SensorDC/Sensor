package neu.lab.evosuiteshell;

import java.io.File;

import neu.lab.conflict.util.MavenUtil;

public class Config {
	private static String MAVEN_PATH = "";
	public static String SENSOR_DIR = "sensor_testcase";
	public static String EVOSUITE_NAME = "evosuite-runtime-1.0.6.jar";

	/**
	 * get mvn.bat or mvn.cmd path
	 * 
	 * @return
	 */
	public static String getMaven() {
		if (!MAVEN_PATH.equals("")) {
			return MAVEN_PATH;
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
		if (new File(mavenPath + "\\mvn.bat").exists())
			MAVEN_PATH = mavenPath + "\\mvn.bat";
		else if (new File(mavenPath + "\\mvn.cmd").exists())
			MAVEN_PATH = mavenPath + "\\mvn.cmd";
		if (MAVEN_PATH.equals("")) {
			MavenUtil.i().getLog().error("Please check or set maven home path!" + "MAVEN_PATH=" + MAVEN_PATH);
		}
		return MAVEN_PATH + " ";
	}

	public static void setMaven(String mavenPath) {
		MAVEN_PATH = mavenPath;
	}

	public static void main(String[] args) {
		System.out.println(getMaven());
	}
}
