package org.sensor.conflict.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.evosuite.Properties;
import org.evosuite.TestSuiteGenerator;
import org.evosuite.Properties.Criterion;
import org.evosuite.Properties.SecondaryObjective;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.coverage.mutation.MutationPool;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.seeding.ConstantPoolManager;
import org.evosuite.seeding.ObjectPool;
import org.evosuite.seeding.ObjectPoolManager;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.factories.RandomLengthTestFactory;
import org.evosuite.testcase.statements.StringPrimitiveStatement;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.evosuite.utils.LoggingUtils;
import org.sensor.conflict.SemanticsConflictMojo;
import org.sensor.evosuiteshell.junit.ExecuteJunit;
import org.sensor.evosuiteshell.search.SearchConstantPool;
import org.sensor.evosuiteshell.search.SearchPrimitiveManager;

import com.google.common.io.Files;

import org.sensor.conflict.container.Conflicts;
import org.sensor.conflict.graph.Book4path;
import org.sensor.conflict.graph.Dog;
import org.sensor.conflict.graph.Graph4path;
import org.sensor.conflict.graph.IBook;
import org.sensor.conflict.graph.IRecord;
import org.sensor.conflict.graph.Record4path;
import org.sensor.conflict.graph.Dog.Strategy;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.util.Conf;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.MySortedMap;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.Conflict;
import org.sensor.conflict.vo.DependencyInfo;
import org.sensor.evosuiteshell.Command;
import org.sensor.evosuiteshell.Config;
import org.sensor.evosuiteshell.ExecuteCommand;
import org.sensor.evosuiteshell.ReadXML;
import org.sensor.evosuiteshell.TestCaseUtil;

public class SemanticsConflictWriter {
	public void writeSemanticsConflict(String outPath) {
		PrintWriter printer = null;
		try {
			printer = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "SemeanticsConflict.txt", false)));
			runEvosuite(printer);
			printer.close();
//			System.out.println(TestCaseUtil
//					.removeFileDir(new File(System.getProperty("user.dir") + "\\" + Config.SENSOR_DIR + "\\")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}// PrintWriter printer

	/**
	 * 得到依赖jar包的路径
	 */
	public String getDependencyCP(Conflict conflict) {
		copyDependency();
		StringBuffer CP = new StringBuffer(System.getProperty("user.dir") + "\\target\\classes;"
				+ System.getProperty("user.dir") + "\\" + Config.EVOSUITE_NAME);
		String dependencyConflictJarDir = System.getProperty("user.dir") + "\\" + Config.SENSOR_DIR + "\\"
				+ "defependencyConflictJar\\";
		String dependencyJar = System.getProperty("user.dir") + "\\" + Config.SENSOR_DIR + "\\" + "dependencyJar\\";
		if (conflict == null) {
			for (String dependency : dependencyJarsPath) {
				CP.append(";" + dependencyJar + dependency);
			}
		} else {
			copyConflictDependency();
			for (String dependency : dependencyJarsPath) {
				if (dependency.contains(conflict.getArtifactId()))
					continue;
				CP.append(";" + dependencyJar + dependency);
			}
			for (String dependency : dependencyConflictJarsPath) {
				if (dependency.contains(conflict.getArtifactId()))
					CP.append(";" + dependencyConflictJarDir + dependency);
			}
		}
		return CP.toString();
	}

	public void runEvosuite(PrintWriter printer) {
		for (Conflict conflict : Conflicts.i().getConflicts()) {
			riskMethodPair(conflict);
			System.setProperty("org.slf4j.simpleLogger.log.org.evosuite", "debug");
			for (String method : methodToHost.keySet()) {
				String testDir = createMethodDir(method);
				String CP = getDependencyCP(null);
				String ConflictCP = getDependencyCP(conflict);
				String testClassName = "";
				HashSet<String> riskMethodClassHosts = methodToHost.get(method);
				for (String riskMethodClassHost : riskMethodClassHosts) {
					printer.println(conflict.toString());
					printer.println(riskMethodClassHost + "===>" + method);
					testClassName = riskMethodClassHost + "_ESTest";
					startEvolution(CP, testDir, riskMethodClassHost);
					compileJunit(testDir, testClassName, CP);
					ArrayList<String> results = executeJunit(testDir, testClassName, ConflictCP);
					printer.println(handleResult(results));
				}
				testClassName = SootUtil.mthdSig2cls(method);
				printer.println("target class ===> conflict class " + testClassName);
				startEvolution(CP, testDir, testClassName);
				compileJunit(testDir, testClassName, CP);
				ArrayList<String> results = executeJunit(testDir, testClassName, ConflictCP);
				printer.println(handleResult(results));
			}
		}
	}

	public void seedingConstant(String targetClass) {
		SearchPrimitiveManager.getInstance();
		int num = targetClass.lastIndexOf(".");
		String name = targetClass.substring(num + 1, targetClass.length());
		HashSet<String> constants = SearchConstantPool.getInstance().getPoolValues(name);
		if (constants != null) {
			for (String constant : constants) {
				ConstantPoolManager.getInstance().addSUTConstant(constant);
				ConstantPoolManager.getInstance().addDynamicConstant(constant);
				ConstantPoolManager.getInstance().addNonSUTConstant(constant);
			}
		}
	}

	public void startEvolution(String CP, String testDir, String targetClass) {
		TestSuiteGenerator testSuiteGenerator = new TestSuiteGenerator();
		seedingConstant(targetClass);
//		Properties.MINIMIZE = false;
		Properties.MIN_INITIAL_TESTS = 10;
		Properties.NUM_TESTS = 10;
		Properties.TEST_DIR = testDir;
		Properties.JUNIT_CHECK = false;
		Properties.CP = CP + ";" + System.getProperty("user.dir") + "\\" + Config.EVOSUITE_NAME;
		Properties.TARGET_CLASS = targetClass;
//		Properties.TARGET_CLASS = SootUtil.mthdSig2cls(targetClass);
		Properties.TARGET_METHOD = "onStart";
		testSuiteGenerator.generateTestSuite();
	}

	public String handleResult(ArrayList<String> results) {
		double run = 0;
		double failures = 0;
		String handle = "";
		double percent = 1;
		for (String result : results) {
			if (result.contains("Tests")) {
				String[] lines = result.split(",");
				run = Double.parseDouble(lines[0].split(": ")[1]);
				failures = Double.parseDouble(lines[1].split(": ")[1]);
				percent = failures / run;
				handle = "test case nums : " + run + " * failures nums : " + failures + " * failures accounted for "
						+ percent * 100 + "%";
				break;
			}
			if (result.contains("OK")) {
				String line = result.substring(result.indexOf("(") + 1, result.indexOf(")"));
				handle = "test case nums : " + line.split(" ")[0] + " * failures accounted for 0%";
				break;
			}
		}
		return handle;
	}

	public void compileJunit(String testDir, String testClassName, String CP) {
		String fileName = testClassName.substring(testClassName.lastIndexOf(".") + 1);
		String packageName = testClassName.replace(fileName, "");
		String fileDir = testDir + packageName.replace(".", "\\");
		StringBuffer cmd = new StringBuffer("cd " + fileDir + "\n");
		cmd.append(Command.JAVAC);
		cmd.append(Command.CLASSPATH);
		cmd.append(CP + " ");
		cmd.append("*.java");
//		System.out.println(cmd + "\n" + fileDir);
		ExecuteCommand.exeBatAndGetResult(ExecuteJunit.creatBat(cmd.toString(), fileDir));
	}

	public ArrayList<String> executeJunit(String testDir, String testClassName, String CP) {
		String fileName = testClassName.substring(testClassName.lastIndexOf(".") + 1);
		String packageName = testClassName.replace(fileName, "");
		String fileDir = testDir + packageName.replace(".", "\\");
		StringBuffer cmd = new StringBuffer("cd " + fileDir + "\n");
		cmd.append(Command.JAVA);
		cmd.append(Command.CLASSPATH);
		cmd.append(CP + ";" + testDir);
		cmd.append(Command.JUNIT_CORE);
		cmd.append(testClassName);
//		System.out.println(cmd + "\n" + fileDir);
		return ExecuteCommand.exeBatAndGetResult(ExecuteJunit.creatBat(cmd.toString(), fileDir));
	}

	public Set<String> readFiles(String path) {
		Set<String> paths = new HashSet<String>();
		File folder = new File(path);
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			paths.add(files[i].getPath());
		}
		return paths;
	}

	HashMap<String, HashSet<String>> methodToHost = new HashMap<String, HashSet<String>>();

	/**
	 * get Risk method pair to methodToHost
	 */
	public void riskMethodPair(Conflict conflict) {
		for (DepJarJRisk depJarRisk : conflict.getJarRisks()) {
			Graph4path pathGraph = depJarRisk.getMethodPathGraphForSemanteme();
			Set<String> hostNodes = pathGraph.getHostNodes();
			Map<String, IBook> pathBooks = new Dog(pathGraph).findRlt(hostNodes, Conf.DOG_DEP_FOR_PATH,
					Strategy.NOT_RESET_BOOK);
			for (String topMthd : pathBooks.keySet()) {
				if (hostNodes.contains(topMthd)) {
					Book4path book = (Book4path) (pathBooks.get(topMthd));
					for (IRecord iRecord : book.getRecords()) {
						Record4path record = (Record4path) iRecord;
						HashSet<String> host = methodToHost.get(record.getRiskMthd());
						if (host == null) {
							host = new HashSet<String>();
							methodToHost.put(record.getRiskMthd(), host);
						}
						// 修改后 存的是host节点的class名
						host.add(SootUtil.mthdSig2cls(topMthd));
					}
				}
			}
		}
	}

	/**
	 * 创建测试方法目录
	 */
	public String createMethodDir(String riskMethod) {
		String workPath = System.getProperty("user.dir") + "\\" + Config.SENSOR_DIR + "\\test_method\\";
		riskMethod = SootUtil.mthdSig2methodName(riskMethod);
		workPath = workPath + riskMethod + "\\";
		File dir = new File(workPath);
		if (!dir.exists())
			dir.mkdirs();
		return workPath;
	}

	private String[] dependencyJarsPath;

	/**
	 * 复制项目自身的依赖到指定文件夹中
	 */
	public void copyDependency() {
		String workPath = System.getProperty("user.dir") + "\\" + Config.SENSOR_DIR + "\\";
		String dependencyJarDir = workPath + "dependencyJar\\";
		String targetPom = MavenUtil.i().getProjectPom();
		if (!(new File(dependencyJarDir)).exists()) {
			new File(dependencyJarDir).mkdirs();
		}
		String mvnCmd = Config.getMaven() + Command.MVN_POM + targetPom + Command.MVN_COPY + dependencyJarDir;
		try {
			ExecuteCommand.exeCmd(mvnCmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File file = new File(dependencyJarDir);
		boolean existJunit = false;
		for (String dependency : file.list()) {
			if (dependency.contains("junit")) {
				existJunit = true;
				break;
			}
		}
		if (!existJunit) {
			copyJunitFormMaven(dependencyJarDir);
		}
		dependencyJarsPath = file.list();
	}

	/**
	 * 依赖中没有junit包，则手动导入Junit4-12的包依赖
	 */
	public void copyJunitFormMaven(String dir) {
		String xmlFileName = ReadXML.copyPom(ReadXML.COPY_JUNIT);
		ReadXML.executeMavenCopy(xmlFileName, dir);
	}

	private String[] dependencyConflictJarsPath;

	/**
	 * 复制项目中自身冲突的依赖到指定文件夹中 升级更新：不复制到指定文件夹，直接定位到仓库位置
	 */
	public void copyConflictDependency() {
		String dependencyConflictJarDir = System.getProperty("user.dir") + "\\" + Config.SENSOR_DIR + "\\"
				+ "defependencyConflictJar\\";
		if (!(new File(dependencyConflictJarDir)).exists()) {
			new File(dependencyConflictJarDir).mkdirs();
		}
		String conflictDependencyJar = MavenUtil.i().getMvnRep();
		for (Conflict conflict : Conflicts.i().getConflicts()) {
			for (DepJarJRisk depJarJRisk : conflict.getJarRisks()) {
				conflictDependencyJar += conflict.getGroupId().replace(".", "\\") + "\\"
						+ conflict.getArtifactId().replace(".", "\\") + "\\" + depJarJRisk.getVersion() + "\\"
						+ conflict.getArtifactId() + "-" + depJarJRisk.getVersion() + ".jar";
				if (new File(conflictDependencyJar).exists()) {
					try {
						Files.copy(new File(conflictDependencyJar), new File(dependencyConflictJarDir + "\\"
								+ conflict.getArtifactId() + "-" + depJarJRisk.getVersion() + ".jar"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					DependencyInfo dependencyInfo = new DependencyInfo(conflict.getGroupId(), conflict.getArtifactId(),
							depJarJRisk.getVersion());
					copyConflictFromMaven(dependencyInfo, dependencyConflictJarDir);
				}
			}
		}
		dependencyConflictJarsPath = new File(dependencyConflictJarDir).list();
	}

	public void copyConflictFromMaven(DependencyInfo dependencyInfo, String dir) {
		String xmlFileName = ReadXML.copyPom(ReadXML.COPY_CONFLICT);
		ReadXML.setCopyDependency(dependencyInfo, xmlFileName);
		ReadXML.executeMavenCopy(xmlFileName, dir);
	}
}