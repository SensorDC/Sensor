package neu.lab.evosuiteshell.junit;

import java.io.FileWriter;
import java.io.IOException;

public class ExecuteJunit {

	/**
	 * 创建bat文件去执行cmd命令
	 * 
	 * @param cmd
	 * @param url 目录
	 * @return fileName 带绝对路径
	 *         C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-tests\\B\\B\\executeCMD.bat
	 */
	public static String creatBat(String cmd, String dir) {
		String fileName = dir + "executeCMD.bat";
		try {
			FileWriter fileWriter = new FileWriter(fileName);
			fileWriter.write(cmd);
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileName;
	}

	public static void main(String[] args) throws IOException {
//		String cmd = "cd C:\\Users\\Flipped\\eclipse-workspace\\Host\\sensor_testcase\\test_method\\getSize\\neu\\lab\\Host\njavac -classpath C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\classes;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\junit-4.12.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\A-1.0.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-runtime-1.0.6.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-tests;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\hamcrest-core-1.3.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\B-1.0.jar Host_ESTest.java";
//		String url = "C:\\Users\\Flipped\\eclipse-workspace\\Host\\sensor_testcase\\test_method\\getSize\\neu\\lab\\Host\\";
//		ArrayList<String> result = ExecuteCommand.exeCmdAndGetResult(creatBat(cmd, url));
//		System.out.println(result);
//		String cmd1 = "cd C:\\Users\\Flipped\\eclipse-workspace\\Host\\sensor_testcase\\test_method\\getSize\\neu\\lab\\Host\njava -classpath C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\classes;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\junit-4.12.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\A-1.0.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-runtime-1.0.6.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-tests;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\hamcrest-core-1.3.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\B-1.0.jar org.junit.runner.JUnitCore neu.lab.Host.Host_ESTest";
//		String url1 = "C:\\Users\\Flipped\\eclipse-workspace\\Host\\sensor_testcase\\test_method\\getSize\\neu\\lab\\Host\\";
//		ArrayList<String> result1 = ExecuteCommand.exeCmdAndGetResult(creatBat(cmd1, url1));
//		System.out.println(result1);
//		System.out.println(System.getProperty("java.home"));
//		System.out.println("Java编译器：" + System.getProperty("java.compiler")); // Java编译器
//		System.out.println("Java执行路径：" + System.getProperty("java.ext.dirs")); // Java执行路径
//
//		System.setProperty("user.dir", "C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-tests\\B\\B");
//		ArrayList<String> result = compileTestCaseJava(
//				"C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\classes;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\junit-4.12.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\A-1.0.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-runtime-1.0.6.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\evosuite-tests;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\hamcrest-core-1.3.jar;C:\\Users\\Flipped\\eclipse-workspace\\Host\\target\\dependency\\B-1.0.jar",
//				" ServicesConfig_ESTest.java");
//		System.out.println(result);
	}
}
