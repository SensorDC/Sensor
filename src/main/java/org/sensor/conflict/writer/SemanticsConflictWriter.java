package org.sensor.conflict.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import gumtree.spoon.diff.operations.Operation;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.graph.*;
import org.sensor.conflict.util.MySortedMap;
import org.sensor.conflict.vo.DepJar;
import org.sensor.evosuiteshell.*;
import org.sensor.evosuiteshell.generate.GenericObjectSet;
import org.sensor.evosuiteshell.generate.GenericPoolFromTestCase;
import org.sensor.evosuiteshell.search.*;
import org.evosuite.Properties;
import org.evosuite.TestSuiteGenerator;
import org.evosuite.Properties.Criterion;
import org.evosuite.coverage.method.designation.GlobalVar;
import org.evosuite.coverage.method.designation.NodeProbDistance;
import org.evosuite.seeding.ConstantPoolManager;
import com.google.common.io.Files;

import org.sensor.conflict.container.Conflicts;
import org.sensor.conflict.distance.MethodProbDistances;
import org.sensor.conflict.graph.Dog.Strategy;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.util.Conf;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.Conflict;
import org.sensor.conflict.vo.DependencyInfo;
import org.sensor.evosuiteshell.junit.ExecuteJunit;
import org.evosuite.utils.LoggingUtils;

public class SemanticsConflictWriter {
    private Map<String, IBook> pathBooks;

    private int runTime = 0;
    private boolean hasNecessaryRunNextTime = true;
    private String outDir;

    public void writeSemanticsConflict(String outDir) {
        //删除原有的sensor_testcase文件夹，避免产生干扰
        if (new File((System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR)).exists()) {
            TestCaseUtil.delete(System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR);
        }

        this.outDir = outDir;
        copyDependency();
        copyConflictDependency();
        try {
            for (int time = 0; time < Conf.runTime; time++) {
                runTime++;
                runEvosuite();
                if (!hasNecessaryRunNextTime) {
                    MavenUtil.i().getLog().info("does not have necessary to run next time, exit!");
                    break;
                }
            }
        } catch (Exception e) {
            MavenUtil.i().getLog().error(e.getMessage());
        }
    }

    /**
     * 得到依赖jar包的路径
     */
    private String getDependencyCP(Conflict conflict) {
        StringBuffer CP = new StringBuffer();
        if (new File(System.getProperty("user.dir") + Config.FILE_SEPARATOR + "target" + Config.FILE_SEPARATOR + "test-classes").exists()) {
            CP = new StringBuffer(System.getProperty("user.dir") + Config.FILE_SEPARATOR + "target" + Config.FILE_SEPARATOR + "classes" + Config.CLASSPATH_SEPARATOR + System.getProperty("user.dir") + Config.FILE_SEPARATOR + "target" + Config.FILE_SEPARATOR + "test-classes");
        } else {
            CP = new StringBuffer(System.getProperty("user.dir") + Config.FILE_SEPARATOR + "target" + Config.FILE_SEPARATOR + "classes");
        }
        CP.append(addEvosuiteRuntimeDependency());
        String dependencyConflictJarDir = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR
                + "dependencyConflictJar" + Config.FILE_SEPARATOR;
        String dependencyJar = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR + "dependencyJar" + Config.FILE_SEPARATOR;
        if (conflict == null) {
            for (String dependency : dependencyJarsPath) {
                CP.append(Config.CLASSPATH_SEPARATOR + dependencyJar + dependency);
            }
        } else {
            for (String dependency : dependencyJarsPath) {
                if (dependency.contains(conflict.getArtifactId()))
                    continue;
                CP.append(Config.CLASSPATH_SEPARATOR + dependencyJar + dependency);
            }
            for (String dependency : dependencyConflictJarsPath) {
                if (dependency.contains(conflict.getArtifactId()))
                    CP.append(Config.CLASSPATH_SEPARATOR + dependencyConflictJarDir + dependency);
            }
        }
        return CP.toString();
    }

    private void runEvosuite() throws Exception {
        HashSet<String> hasDetectConflict = new HashSet<>();
        for (Conflict conflict : Conflicts.i().getConflicts()) {
            if (hasDetectConflict.contains(conflict.getUsedDepJar().toString())) {
                continue;
            }
            hasDetectConflict.add(conflict.getUsedDepJar().toString());
//            new File(Config.SENSOR_DIR + Config.FILE_SEPARATOR + "SemeanticsConflict_" + conflict.getSig().replaceAll("\\p{Punct}", "") + ".txt").createNewFile();
//            PrintWriter printer = new PrintWriter(new BufferedWriter(new FileWriter(Config.SENSOR_DIR + Config.FILE_SEPARATOR + "SemeanticsConflict_" + conflict.getSig().replaceAll("\\p{Punct}", "") + runTime + ".txt", Conf.append)));
            //单独运行
//            PrintWriter printer = new PrintWriter(new BufferedWriter(new FileWriter(Config.SENSOR_DIR + Config.FILE_SEPARATOR + "SemeanticsConflict_" + conflict.getSig().replaceAll("\\p{Punct}", "") + runTime + ".txt", Conf.append)));
            //服务器批量运行
            new File(outDir + Config.FILE_SEPARATOR + "sensor" + Config.FILE_SEPARATOR + MavenUtil.i().getProjectCor().replaceAll("\\p{Punct}", "") + Config.FILE_SEPARATOR).mkdirs();
            PrintWriter printer = new PrintWriter(new BufferedWriter(new FileWriter(outDir + Config.FILE_SEPARATOR + "sensor" + Config.FILE_SEPARATOR + MavenUtil.i().getProjectCor().replaceAll("\\p{Punct}", "") + Config.FILE_SEPARATOR + "SemeanticsConflict_" + conflict.getSig().replaceAll("\\p{Punct}", "") + runTime + ".txt", Conf.append)));
            printer.println(conflict.toString());
            riskMethodPair(conflict);
            printRiskMetodDiff(printer);
            System.setProperty("org.slf4j.simpleLogger.log.org.evosuite", "off");
            for (String method : methodToHost.keySet()) {
//                initObjectPool(SootUtil.mthdSig2cls(method));
//                System.out.println(method);
                String testDir = createMethodDir(method);
                String CP = getDependencyCP(null);
                String ConflictCP = getDependencyCP(conflict);
                String testClassName = "";
                HashSet<String> riskMethodHosts = methodToHost.get(method);
                for (String riskMethodHost : riskMethodHosts) {
                    String riskMethodClassHost = SootUtil.mthdSig2cls(riskMethodHost);
//                    System.out.println(riskMethodHost);
                    printer.println("target class : " + riskMethodClassHost);
                    printer.println("host method : " + riskMethodHost);
                    printer.println("risk method : " + method);
                    testClassName = riskMethodClassHost + "_ESTest";
                    startEvolution(CP, testDir, riskMethodClassHost, method, riskMethodHost);
                    compileJunit(testDir, testClassName, CP);
                    ArrayList<String> results = executeJunit(testDir, testClassName, CP);
                    ArrayList<String> conflictResults = executeJunit(testDir, testClassName, ConflictCP);
                    printer.println(handleResult(results, conflictResults));
                    printer.println();
                }
                testClassName = SootUtil.mthdSig2cls(method);
                printer.println("risk target class ===> " + testClassName);
                printer.println("risk target method ===> " + method);
                startEvolution(CP, testDir, testClassName, method, method);
                compileJunit(testDir, testClassName + "_ESTest", CP);
                ArrayList<String> results = executeJunit(testDir, testClassName + "_ESTest", CP);
                ArrayList<String> conflictResults = executeJunit(testDir, testClassName + "_ESTest", ConflictCP);
                printer.println(handleResult(results, conflictResults));
                printer.println("risk method can be called, method path:");
                printer.println(methodPath.get(method));
            }
            printer.println();
            printer.close();
        }
        if (methodToHost.keySet().size() == 0) {
            hasNecessaryRunNextTime = false;
        }
    }

//    public void initObjectPool(String className) {
//        /*
//        测试 getPoolFromJUnit
//         */
////        final org.evosuite.testcarver.extraction.CarvingClassLoader classLoader = new org.evosuite.testcarver.extraction.CarvingClassLoader();
////        InstrumentingClassLoader instrumentingClassLoader = new InstrumentingClassLoader();
////        try {
////            System.out.println(1111);
////            Class<?> Host = instrumentingClassLoader.loadClass(className);
//////            Class<?> Host = instrumentingClassLoader.loadClassFromFile("org.sensor.Host.Host", "/Users/wangchao/eclipse-workspace/Host/target/classes/neu/lab/Host/Host.class");
////            Class<?> Hosttest = instrumentingClassLoader.loadClassFromFile("org.sensor.Host.HostTest", "/Users/wangchao/eclipse-workspace/Host/target/test-classes/neu/lab/Host/HostTest.class");
////            ObjectPool.getPoolFromJUnit(new GenericClass(Host), Hosttest);
////        } catch (ClassNotFoundException e) {
////            e.printStackTrace();
////        }
////Properties.WRITE_POOL="aaa.txt";
//
//        String hostJarPath = DepJars.i().getHostDepJar().getJarFilePaths(true).toArray(new String[]{})[0];
//        if (!(hostJarPath.endsWith(".jar"))) {
//            for (File file : new File(hostJarPath).getParentFile().listFiles()) {
//                if (file.getAbsolutePath().endsWith(".jar")) {
//                    hostJarPath = file.getAbsolutePath();
//                    break;
//                }
//            }
//        }
//        new SootExe().initProjectInfo(new String[]{hostJarPath});
//        ProjectInfo.i().setEntryCls(className);
//        MyClassLoader.jarLoader(new File(hostJarPath));
////    System.out.println(ProjectInfo.i().getClassInfo("org.sensor.A.Principal"));
//        GenericObjectSet.getInstance().generateGenericObject(className);
//    }

    private void seedingConstant(String targetClass) {
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

    private void startEvolution(String CP, String testDir, String targetClass, String riskMethod, String byteRiskMethod) {
        Properties.getInstance();
        Properties.CP = CP;
        TestSuiteGenerator testSuiteGenerator = new TestSuiteGenerator();
//        setNodeProbDistance(pathBooks, riskMethod);
//        Properties.SEED_TYPES = false;
        seedingConstant(targetClass);// String 参数种植
//        System.out.println(targetClass);
        GenericPoolFromTestCase.receiveTargetClass(targetClass);

        Properties.ASSERTION_STRATEGY = Properties.AssertionStrategy.ALL;

        Properties.ASSERTION_MINIMIZATION_FALLBACK = 1;
        Properties.ASSERTION_MINIMIZATION_FALLBACK_TIME = 1;

        Properties.MINIMIZE = false;
        Properties.MINIMIZE_VALUES = true;
        Properties.P_OBJECT_POOL = 1;
//        System.out.println(riskMethod);
        Properties.RISK_METHOD = riskMethod;
        Properties.TARGET_METHOD = SootUtil.bytemthdSig2fullymethodName(byteRiskMethod);
//        System.out.println(Properties.TARGET_METHOD);
        Properties.MIN_INITIAL_TESTS = 10;
        Properties.CRITERION = new Criterion[]{Criterion.METHOD, Criterion.BRANCH, Criterion.LINE};
        Properties.NUM_TESTS = 10;
        Properties.TEST_DIR = testDir;
        Properties.JUNIT_CHECK = false;
        Properties.MOCK_IF_NO_GENERATOR = false;
        Properties.FUNCTIONAL_MOCKING_PERCENT = 0;
//        CP = CP.replace(";", ":");// + ":" + System.getProperty("user.dir") + Config.FILE_SEPARATOR + "evosuite-shaded-1.0.6.jar";
//        CP = CP.replaceAll("/Users/wangchao/eclipse-workspace/Host/", "");

//        CP.replaceAll(";",":");
//        System.out.println(CP);
//        System.out.println(targetClass);
        Properties.TARGET_CLASS = targetClass;

        GenericObjectSet.getInstance().generateObject(targetClass);

//        initObjectPool(targetClass);
//		Properties.TARGET_CLASS = SootUtil.mthdSig2cls(targetClass);
//		Properties.TARGET_METHOD = "onStart";
        testSuiteGenerator.generateTestSuite();
    }

    private void setNodeProbDistance(Map<String, IBook> pathGraph, String riskMethod) {
        MethodProbDistances methodProbabilityDistances = getMethodProDistances(pathGraph);
        NodeProbDistance nodeProbDistance = methodProbabilityDistances.getEvosuiteProbability(riskMethod);
        GlobalVar.i().setNodeProbDistance(nodeProbDistance);
    }

    private MethodProbDistances getMethodProDistances(Map<String, IBook> books) {
        MethodProbDistances distances = new MethodProbDistances();
        for (IBook book : books.values()) {
            // MavenUtil.i().getLog().info("book:"+book.getNodeName());
            for (IRecord iRecord : book.getRecords()) {

                Record4distance record = (Record4distance) iRecord;
                // MavenUtil.i().getLog().info("record:"+record.getName());
                distances.addDistance(record.getName(), book.getNodeName(), record.getDistance());
                distances.addProb(record.getName(), book.getNodeName(), record.getBranch());
            }
        }
        return distances;
    }

    private String handleResult(ArrayList<String> results, ArrayList<String> conflictResults) {
//        double run = 0;
        double failures = 0;
        double conflictRun = 0;
        double conflictFailures = 0;
        String handle;
        double percent;
        for (String result : results) {
            if (result.contains("Tests")) {
                String[] lines = result.split(",");
//                run = Double.parseDouble(lines[0].split(": ")[1]);
                failures = Double.parseDouble(lines[1].split(": ")[1]);
                break;
            }
            if (result.contains("OK")) {
                break;
            }
        }
        for (String conflictResult : conflictResults) {
            if (conflictResult.contains("Tests")) {
                String[] lines = conflictResult.split(",");
                conflictRun = Double.parseDouble(lines[0].split(": ")[1]);
                conflictFailures = Double.parseDouble(lines[1].split(": ")[1]);
                break;
            }
            if (conflictResult.contains("OK")) {
                conflictRun = Double.parseDouble(conflictResult.split("\\(")[1].split(" ")[0]);
                break;
            }
        }
        if (conflictRun == 0) {
            percent = 0;
        } else {
            percent = (conflictFailures - failures) / conflictRun;
        }
        handle = "test case nums : " + conflictRun + " * failures nums : " + (conflictFailures - failures) + " * failures accounted for "
                + String.format("%.2f", percent * 100) + "%";
        return handle;
    }

    private void compileJunit(String testDir, String testClassName, String CP) {
        MavenUtil.i().getLog().info("start to compile test case");
        if (!CP.contains("junit")) {
            CP += addJunitDependency();
        }
        String fileName = testClassName.substring(testClassName.lastIndexOf(".") + 1);
        String packageName = testClassName.replace(fileName, "");
        String fileDir = testDir + packageName.replace(".", Config.FILE_SEPARATOR);
        StringBuffer cmd = new StringBuffer("cd " + fileDir + "\n");
        cmd.append(Command.JAVAC);
        cmd.append(Command.CLASSPATH);
        cmd.append(CP + " ");
        cmd.append("*.java");
        cmd.append(" >log.txt 2>&1");
//		System.out.println(cmd + "\n" + fileDir);
        if (Config.osName.contains("Windows")) {
            ExecuteCommand.exeBatAndGetResult(ExecuteJunit.creatBat("c" + fileName, cmd.toString(), fileDir));
        } else {
            ExecuteCommand.exeBatAndGetResult(ExecuteJunit.creatShellScript("c" + fileName, cmd.toString(), fileDir));
        }
    }

    private ArrayList<String> executeJunit(String testDir, String testClassName, String CP) {
        MavenUtil.i().getLog().info("start to execute test case");
        if (!CP.contains("junit")) {
            CP += addJunitDependency();
        }
        String fileName = testClassName.substring(testClassName.lastIndexOf(".") + 1);
        String packageName = testClassName.replace(fileName, "");
        String fileDir = testDir + packageName.replace(".", Config.FILE_SEPARATOR);
        StringBuffer cmd = new StringBuffer("cd " + fileDir + "\n");
        cmd.append(Command.JAVA);
        cmd.append(Command.CLASSPATH);
        cmd.append(CP + Config.CLASSPATH_SEPARATOR + testDir);
        cmd.append(Command.JUNIT_CORE);
        cmd.append(testClassName);
        cmd.append(" >log.txt 2>&1");
//		System.out.println(cmd + "\n" + fileDir);
        if (Config.osName.contains("Windows")) {
            MavenUtil.i().getLog().info("create bat in windows");
            return ExecuteCommand.exeBatAndGetResult(ExecuteJunit.creatBat("e" + fileName, cmd.toString(), fileDir));
        } else {
            MavenUtil.i().getLog().info("create sh");
            return ExecuteCommand.exeBatAndGetResult(ExecuteJunit.creatShellScript("e" + fileName, cmd.toString(), fileDir));
        }
    }

    private String addJunitDependency() {
        StringBuffer stringBuffer = new StringBuffer("");
        for (String jarPath : junitJarsPath) {
            stringBuffer.append(Config.CLASSPATH_SEPARATOR);
            stringBuffer.append(jarPath);
        }
        return stringBuffer.toString();
    }

    private String addEvosuiteRuntimeDependency() {
        StringBuffer stringBuffer = new StringBuffer("");
        for (String jarPath : evosuiteRuntimeJarsPath) {
            stringBuffer.append(Config.CLASSPATH_SEPARATOR);
            stringBuffer.append(jarPath);
        }
        return stringBuffer.toString();
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

    HashMap<String, String> methodPath = new HashMap<>();

    Map<String, List<Operation>> riskMethodDiffsMap = new HashMap<>();

    private void printRiskMetodDiff(PrintWriter printer) {
//        Set<Map.Entry<String, List<Operation>>> entrys = riskMethodDiffsMap.entrySet();
        for (String riskMethod : riskMethodDiffsMap.keySet()) {
            List<Operation> operations = riskMethodDiffsMap.get(riskMethod);
            printer.println(riskMethod + " ===> diff count : " + operations.size() + "\n used compare shield diff:");
            if (Conf.printDiff) {
                for (Operation operation : operations) {
                    try {
                        printer.println(operation.toString());
                    } catch (Exception e) {
//                                    e.printStackTrace();
                        break;
                    }
                }
            }
        }

    }

    /**
     * get Risk method pair to methodToHost
     */
    private void riskMethodPair(Conflict conflict) {
        for (DepJarJRisk depJarRisk : conflict.getJarRisks()) {
            Graph4path pathGraph = depJarRisk.getMethodPathGraphForSemanteme();
            Map<String, String> methodMappingASMMethod = pathGraph.getMethodMappingASMMethod();
            Map<String, List<Operation>> allRiskMethodDiffsMap = depJarRisk.getRiskMethodDiffsMap();
            Set<String> hostNodes = pathGraph.getHostNodes();
            /**
             * 可以在这里修改寻找深度，改成10层
             */
            pathBooks = new Dog(pathGraph).findRlt(hostNodes, Conf.DOG_DEP_FOR_PATH, Strategy.NOT_RESET_BOOK);
//			MethodProbDistances methodProbabilityDistances = getMethodProDistances(pathBooks);
//			setNodeProbDistance(methodProbabilityDistances);
//			System.out.println(getMethodProDistances(pathBooks));
//            System.out.println(pathBooks.keySet());
            MySortedMap<Integer, Record4path> dis2records = new MySortedMap<>();

            for (String topMethod : pathBooks.keySet()) {
                // 不保存内部类
                if (SootUtil.mthdSig2cls(topMethod).contains("$")) {
                    continue;
                }
                if (hostNodes.contains(topMethod)) {
                    Book4path book = (Book4path) pathBooks.get(topMethod);
                    for (IRecord iRecord : book.getRecords()) {
                        Record4path record = (Record4path) iRecord;
                        dis2records.add(record.getPathlen(), record);
                        HashSet<String> host = methodToHost.get(record.getRiskMethod());
                        if (host == null) {
                            host = new HashSet<>();
                            methodToHost.put(record.getRiskMethod(), host);
                            riskMethodDiffsMap.put(record.getRiskMethod(), allRiskMethodDiffsMap.get(methodMappingASMMethod.get(record.getRiskMethod())));
                        }
                        // 修改后 存的是host节点的完全名
//                        host.add(SootUtil.mthdSig2cls(topMthd));
                        host.add(topMethod);
                    }
//                    Map<String, IBook> books4path = new Dog(pathGraph).findRlt(hostNodes, Conf.DOG_DEP_FOR_PATH,
//                            Strategy.NOT_RESET_BOOK);
//                    Book4path book4path = (Book4path) books4path.get(topMthd);
//                    for (IRecord iRecord : book4path.getRecords()) {
//                        Record4path record4path = (Record4path) iRecord;
//                        dis2records.add(record4path.getPathlen(), record4path);
//                    }
                }
            }
            MavenUtil.i().getLog().info("risk method from host can reach size : " + methodToHost.keySet().size());
            if (dis2records.size() > 0) {
                Record4path record = dis2records.flat().get(0);
                // 只存最短一条
                methodPath.put(record.getRiskMethod(), addJarPath(record.getPathStr()));
            }
        }
    }

    private String addJarPath(String mthdCallPath) {
        StringBuilder sb = new StringBuilder();
        String[] mthds = mthdCallPath.split("\\n");
        for (int i = 0; i < mthds.length - 1; i++) {
            // last method is risk method,don't need calculate.
            String mthd = mthds[i];
            String cls = SootUtil.mthdSig2cls(mthd);
            DepJar depJar = DepJars.i().getClassJar(cls);
            String jarPath = "";
            if (depJar != null)
                jarPath = depJar.getJarFilePaths(true).get(0);
            sb.append(mthd + " " + jarPath + "\n");
        }
        sb.append(mthds[mthds.length - 1]);
        return sb.toString();
    }

    /**
     * 创建测试方法目录
     */
    private String createMethodDir(String riskMethod) {
        String workPath = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR + "test_method" + runTime + Config.FILE_SEPARATOR;
        riskMethod = SootUtil.bytemthdSig2methodName(riskMethod);
        workPath = workPath + riskMethod + Config.FILE_SEPARATOR;
        File dir = new File(workPath);
        if (!dir.exists())
            dir.mkdirs();
        return workPath;
    }

    private String[] dependencyJarsPath;
    private String[] junitJarsPath;
    private String[] evosuiteRuntimeJarsPath;

    /**
     * 复制项目自身的依赖到指定文件夹中
     */
    private void copyDependency() {
        String workPath = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR;
        String dependencyJarDir = workPath + "dependencyJar" + Config.FILE_SEPARATOR;
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
            copyJunitFormMaven();
        }
        copyEvosuiteRuntimeFormMaven();
        dependencyJarsPath = file.list();
    }

    /**
     * 依赖中没有junit包，则手动导入Junit4-12的包依赖
     */
    private void copyJunitFormMaven() {
        String workPath = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR + "junit" + Config.FILE_SEPARATOR;
        if (!(new File(workPath)).exists()) {
            new File(workPath).mkdirs();
        }
        String xmlFileName = ReadXML.copyPom(ReadXML.COPY_JUNIT);
        ReadXML.executeMavenCopy(xmlFileName, workPath);
        junitJarsPath = new File(workPath).list();
        if (junitJarsPath == null) return;
        for (int i = 0; i < junitJarsPath.length; i++) {
            junitJarsPath[i] = workPath + junitJarsPath[i];
        }
    }

    /**
     * 手动导入EvosuiteRuntime的包依赖
     */
    private void copyEvosuiteRuntimeFormMaven() {
        String workPath = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR + "EvosuiteRuntime" + Config.FILE_SEPARATOR;
        if (!(new File(workPath)).exists()) {
            new File(workPath).mkdirs();
        }
        String xmlFileName = ReadXML.copyPom(ReadXML.COYT_EVOSUITE);
        ReadXML.executeMavenCopy(xmlFileName, workPath);
        evosuiteRuntimeJarsPath = new File(workPath).list();
        if (evosuiteRuntimeJarsPath == null) return;
        for (int i = 0; i < evosuiteRuntimeJarsPath.length; i++) {
            evosuiteRuntimeJarsPath[i] = workPath + evosuiteRuntimeJarsPath[i];
        }
    }

    private String[] dependencyConflictJarsPath;

    /**
     * 复制项目中自身冲突的依赖到指定文件夹中 升级更新：不复制到指定文件夹，直接定位到仓库位置
     */
    private void copyConflictDependency() {
        String dependencyConflictJarDir = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR
                + "dependencyConflictJar" + Config.FILE_SEPARATOR;
        if (!(new File(dependencyConflictJarDir)).exists()) {
            new File(dependencyConflictJarDir).mkdirs();
        }
        String conflictDependencyJar = MavenUtil.i().getMvnRep();
        for (Conflict conflict : Conflicts.i().getConflicts()) {
            try {
                for (DepJarJRisk depJarJRisk : conflict.getJarRisks()) {
                    conflictDependencyJar += conflict.getGroupId().replace(".", Config.FILE_SEPARATOR) + Config.FILE_SEPARATOR
                            + conflict.getArtifactId().replace(".", Config.FILE_SEPARATOR) + Config.FILE_SEPARATOR + depJarJRisk.getVersion() + Config.FILE_SEPARATOR
                            + conflict.getArtifactId() + "-" + depJarJRisk.getVersion() + ".jar";
                    if (new File(conflictDependencyJar).exists()) {
                        try {
                            Files.copy(new File(conflictDependencyJar), new File(dependencyConflictJarDir + Config.FILE_SEPARATOR
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
            } catch (Exception e) {
                MavenUtil.i().getLog().error(e.getMessage());
            }
        }
        dependencyConflictJarsPath = new File(dependencyConflictJarDir).list();
    }

    private void copyConflictFromMaven(DependencyInfo dependencyInfo, String dir) {
        String xmlFileName = ReadXML.copyPom(ReadXML.COPY_CONFLICT);
        ReadXML.setCopyDependency(dependencyInfo, xmlFileName);
        ReadXML.executeMavenCopy(xmlFileName, dir);
    }
}