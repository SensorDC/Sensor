package neu.lab.evosuiteshell;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

public class ExecuteCommand {
	public static void exeCmd(String mvnCmd) throws ExecuteException, IOException {
		exeCmd(mvnCmd, 0, null);
	}

	public static void exeCmd(String mvnCmd, long timeout, String logPath) throws ExecuteException, IOException {
		CommandLine cmdLine = CommandLine.parse(mvnCmd);
		DefaultExecutor executor = new DefaultExecutor();
		if (timeout != 0) {
			ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
			executor.setWatchdog(watchdog);
		}
		if (logPath != null) {
			executor.setStreamHandler(new PumpStreamHandler(new FileOutputStream(logPath)));
		}
		executor.execute(cmdLine);
	}

	public static ArrayList<String> exeBatAndGetResult(String batFilePath) {
		BufferedReader br = null;
//		StringBuilder stringBuilder = new StringBuilder();
		ArrayList<String> lines = new ArrayList<String>();
		try {
			Process p = Runtime.getRuntime().exec(batFilePath);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
//				stringBuilder.append(line + "\n");
				lines.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
//			new File(batFilePath).delete();
		}
		return lines;
	}

	public static void main(String[] args) throws ExecuteException, IOException {
//		String commandStr = Config.getMaven() + " -version";
//		ArrayList<String> results = ExecuteCommand.exeCmdAndGetResult(commandStr);
//		for (String line : results) {
//			if (line.contains("3.6.0"))
//				System.out.println(line);
//		}
//		String sensor_dir = "C:\\Users\\Flipped\\eclipse-workspace\\Host\\" + Config.SENSOR_DIR + "\\";
//		String targetFile = ReadXML.copyPom(sensor_dir);
//		List<DependencyInfo> DependencyInfos = new ArrayList<DependencyInfo>();
//		DependencyInfo dependencyInfo = new DependencyInfo();
//		dependencyInfo.setArtifactId("B");
//		dependencyInfo.setGroupId("neu.lab");
//		dependencyInfo.setVersion("1.0");
//		DependencyInfos.add(dependencyInfo);
//		ReadXML.setCopyDependency(DependencyInfos, targetFile);
//		String mvnCmd = Config.getMaven() + Command.MVN_POM + targetFile + Command.MVN_COPY + sensor_dir + "jar\\";
//		exeCmd(mvnCmd);
	}
}
