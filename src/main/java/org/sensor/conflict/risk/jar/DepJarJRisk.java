package org.sensor.conflict.risk.jar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.sensor.conflict.CountProjectMojo;
import org.sensor.conflict.GlobalVar;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.container.SemantemeMethods;
import org.sensor.conflict.graph.Graph4distance;
import org.sensor.conflict.graph.Graph4path;
import org.sensor.conflict.graph.GraphForMethodOutPath;
import org.sensor.conflict.graph.IBook;
import org.sensor.conflict.graph.IGraph;
import org.sensor.conflict.graph.IRecord;
import org.sensor.conflict.graph.Node4distance;
import org.sensor.conflict.graph.Node4path;
import org.sensor.conflict.graph.Record4distance;
import org.sensor.conflict.graph.Record4path;
import org.sensor.conflict.soot.SootJRiskCg;
import org.sensor.conflict.soot.SootRiskMthdFilter;
import org.sensor.conflict.soot.SootRiskMthdFilter2;
import org.sensor.conflict.soot.tf.JRiskDistanceCgTf;
import org.sensor.conflict.soot.tf.JRiskMethodOutPathCgTf;
import org.sensor.conflict.soot.tf.JRiskMthdPathCgTf;
import org.sensor.conflict.util.*;
import org.sensor.conflict.vo.DepJar;
import org.sensor.conflict.vo.MethodCall;
import org.sensor.evosuiteshell.Config;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import spoon.reflect.declaration.CtMethod;

/**
 * 依赖风险jar
 *
 * @author wangchao
 */
public class DepJarJRisk {
    private DepJar depJar; // 依赖jar
    private DepJar usedDepJar; // 依赖jar
    private Set<String> thrownMthds; // 抛弃的方法
    //    private Set<String> semantemeRiskMethods; // 语义风险方法集合
    // private Set<String> rchedMthds;
    private Graph4distance graph4distance; // 图
//	private Map<String, IBook> books; // book记录用

    /*
     * 构造函数
     */
    public DepJarJRisk(DepJar depJar, DepJar usedDepJar) {
        this.depJar = depJar;
        this.usedDepJar = usedDepJar;
        // calculate thrownMthd
        // calculate call-graph
    }

    /*
     * 得到版本
     */
    public String getVersion() {
        return depJar.getVersion();
    }

    public DepJar getUsedDepJar() {
        return usedDepJar;
    }

    public void setUsedDepJar(DepJar usedDepJar) {
        this.usedDepJar = usedDepJar;
    }

    public Set<String> getThrownClasses() {
        Set<String> thrownClasses = usedDepJar.getRiskClasses(depJar.getAllCls(false));
        return thrownClasses;
    }

    /**
     * 得到抛弃的方法
     *
     * @return
     */
    public Set<String> getThrownMthds() {
        // e.g.:"<org.sensor.plug.testcase.homemade.host.prob.ProbBottom: void m()>"
        thrownMthds = usedDepJar.getRiskMthds(depJar.getallMethods());
        MavenUtil.i().getLog().info("riskMethod size before filter: " + thrownMthds.size());
        if (thrownMthds.size() > 0)
            new SootRiskMthdFilter().filterRiskMthds(thrownMthds);
        MavenUtil.i().getLog().info("riskMethod size after filter1: " + thrownMthds.size());
        if (thrownMthds.size() > 0)
            new SootRiskMthdFilter2().filterRiskMthds(this, thrownMthds);
        MavenUtil.i().getLog().info("riskMethod size after filter2: " + thrownMthds.size());
        return thrownMthds;
    }

    /**
     * 用传入的depJar去得到抛弃的方法
     *
     * @param enterDepJar
     * @return
     */
    public Set<String> getThrownMthds(DepJar enterDepJar) {
        thrownMthds = usedDepJar.getRiskMthds(depJar.getallMethods());
        MavenUtil.i().getLog().info("riskMethod size before filter: " + thrownMthds.size());
        if (thrownMthds.size() > 0)
            new SootRiskMthdFilter().filterRiskMthds(thrownMthds, enterDepJar);
        MavenUtil.i().getLog().info("riskMethod size after filter1: " + thrownMthds.size());
        if (thrownMthds.size() > 0)
            new SootRiskMthdFilter2().filterRiskMthds(this, thrownMthds);
        MavenUtil.i().getLog().info("riskMethod size after filter2: " + thrownMthds.size());
        return thrownMthds;
    }

    /**
     * 语义冲突得到相关方法
     *
     * @return
     */
    public Set<String> getSemantemeRiskMethods() {
        Set<String> semantemeRiskMethods = usedDepJar.getCommonMethods(depJar.getallMethods());
        MavenUtil.i().getLog().info("semantemeRiskMethods size for common methods: " + semantemeRiskMethods.size());
        return semantemeRiskMethods;
    }

    public Set<String> getMethodBottom(Map<String, IBook> books) {
        Set<String> bottomMethods = new HashSet<String>();
        for (IBook book : books.values()) {
            for (IRecord iRecord : book.getRecords()) {
                Record4distance record = (Record4distance) iRecord;
                bottomMethods.add(record.getName());
            }
        }
        return bottomMethods;
    }

    public Set<String> getMethodBottomForPath(Map<String, IBook> books) {
        Set<String> bottomMethods = new HashSet<String>();
        for (IBook book : books.values()) {
            for (IRecord iRecord : book.getRecords()) {
                Record4path record = (Record4path) iRecord;
                bottomMethods.add(record.getName());
            }
        }
        return bottomMethods;
    }

    public Collection<String> getPrcDirPaths() throws Exception {
        List<String> classpaths;
        if (GlobalVar.useAllJar) { // default:true
            classpaths = depJar.getRepalceClassPath();
        } else {
            MavenUtil.i().getLog().info("not add all jar to process");
            classpaths = new ArrayList<String>();
            // keep first is self
            classpaths.addAll(this.depJar.getJarFilePaths(true));
            classpaths.addAll(this.depJar.getFatherJarClassPaths(false));

        }
        return classpaths;

    }

    public DepJar getEntryDepJar() {
        return DepJars.i().getHostDepJar();
    }

    public DepJar getConflictDepJar() {
        return depJar;
    }

    /**
     * 得到距离图
     *
     * @return
     */
    public Graph4distance getGraph4distance() {
        if (graph4distance == null) {
            Set<String> thrownmethods = getThrownMthds();
            if (thrownmethods.size() > 0) {
                IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskDistanceCgTf(this, thrownmethods));
                if (iGraph != null) {
                    graph4distance = (Graph4distance) iGraph;
                } else {
                    graph4distance = new Graph4distance(new HashMap<String, Node4distance>(),
                            new ArrayList<MethodCall>());
                }
            } else {
                graph4distance = new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
            }
        }
        return graph4distance;
    }

    /**
     * 得到距离图 多态
     *
     * @return
     */
    public Graph4distance getGraph4distance(DepJar useDepJar) {
        Set<String> thrownmethods = getThrownMthds(useDepJar);
        if (thrownmethods.size() > 0) {
            IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskDistanceCgTf(this, thrownmethods));
            if (iGraph != null) {
                return (Graph4distance) iGraph;
            } else {
                return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
            }
        } else {
            return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
        }
    }

    public Graph4path getGraph4mthdPath() {
        Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
        if (semantemeRiskMethods.size() > 0) {
            IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskMthdPathCgTf(this, semantemeRiskMethods));
            if (iGraph != null) {
                return (Graph4path) iGraph;
            } else {
                return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
            }
        } else {
            return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
        }
//		if (getThrownMthds().size() > 0) {
//			IGraph iGraph = SootJRiskCg.i().getGraph4branch(this,new JRiskMthdPathCgTf(this));
//			if(iGraph!=null)
//				return (Graph4path)iGraph;
//		}
//		return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
    }

    private Map<String, List<Integer>> semantemeMethodForDifferences; // 语义方法的差异集合

    public Map<String, List<Integer>> getSemantemeMethodForDifferences() {
        return semantemeMethodForDifferences;
    }

    public Map<String, List<Operation>> getAllSemantemeMethodForDifferences() {

        Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
        if (semantemeRiskMethods.size() > 0) {
//            GraphForMethodOutPath depJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i().getGraph(depJar,
//                    new JRiskMethodOutPathCgTf(semantemeRiskMethods));
//
//            GraphForMethodOutPath usedDepJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i()
//                    .getGraph(usedDepJar, new JRiskMethodOutPathCgTf(semantemeRiskMethods));
//
//            SemantemeMethods semantemeMethods = new SemantemeMethods(depJarGraphForMethodOutPath.getSemantemeMethods(),
//                    usedDepJarGraphForMethodOutPath.getSemantemeMethods());
//
//            semantemeMethods.CalculationDifference(); // 计算差异
//
//            semantemeMethodForDifferences = semantemeMethods.getSemantemeMethodForReturn();
            decompile();

            calculationDifference(semantemeRiskMethods);
        }
        return riskMethodDiffsMap;
    }

    // depJar 的反编译文件路径
    private String depJarDecompressionPath;
    // used depJar 的反编译文件路径
    private String usedDepJarDecompressionPath;

    // 得到语义冲突的路径图
    public Graph4distance getMethodDistanceGraphForSemanteme() {

        Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
        Set<String> riskMethods;

        if (semantemeRiskMethods.size() > 0) {

            decompile();

            riskMethods = calculationDifference(semantemeRiskMethods);

            if (riskMethods.size() > 0) {
                IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskDistanceCgTf(this, riskMethods));
                if (iGraph != null) {
                    return (Graph4distance) iGraph;
                } else {
                    return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
                }
            } else {
                return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
            }
        } else {
            return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
        }
    }

    public Graph4path getMethodPathGraphForSemanteme() {

        Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
        Set<String> riskMethods;

        if (semantemeRiskMethods.size() > 0) {

            GraphForMethodOutPath depJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i().getGraph(depJar,
                    new JRiskMethodOutPathCgTf(semantemeRiskMethods));

            GraphForMethodOutPath usedDepJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i()
                    .getGraph(usedDepJar, new JRiskMethodOutPathCgTf(semantemeRiskMethods));

            SemantemeMethods semantemeMethods = new SemantemeMethods(depJarGraphForMethodOutPath.getSemantemeMethods(),
                    usedDepJarGraphForMethodOutPath.getSemantemeMethods());

            semantemeMethods.CalculationDifference(); // 计算差异

            //内部类会报错
            semantemeMethods.deleteInnerClass();

            semantemeRiskMethods = semantemeMethods.sortMap(5000);

            if (semantemeRiskMethods == null) {
                return new Graph4path(new HashMap<>(), new ArrayList<>());
            }

            MavenUtil.i().getLog().info("filter risk method, after size : " + semantemeRiskMethods.size());

            decompile();
            try {
                riskMethods = calculationDifference(semantemeRiskMethods);
            } catch (Exception e) {
                MavenUtil.i().getLog().error(e.getMessage());
                return new Graph4path(new HashMap<>(), new ArrayList<>());
            }

            MavenUtil.i().getLog().info("risk method size : " + riskMethods.size());

            if (riskMethods.size() > 0) {
                IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskMthdPathCgTf(this, riskMethods));
                if (iGraph != null) {
                    return (Graph4path) iGraph;
                } else {
                    return new Graph4path(new HashMap<>(), new ArrayList<>());
                }
            } else {
                return new Graph4path(new HashMap<>(), new ArrayList<>());
            }
        } else {
            return new Graph4path(new HashMap<>(), new ArrayList<>());
        }
    }

    /**
     * 解压冲突jar包
     */
    public void decompile() {

        depJarDecompressionPath = JARDecompressionTool.decompressionPath + depJar.getDepJarName() + Config.FILE_SEPARATOR;

        usedDepJarDecompressionPath = JARDecompressionTool.decompressionPath + usedDepJar.getDepJarName() + Config.FILE_SEPARATOR;

        JARDecompressionTool.decompress(depJar.getJarFilePaths(true).get(0), depJarDecompressionPath);

        JARDecompressionTool.decompress(usedDepJar.getJarFilePaths(true).get(0), usedDepJarDecompressionPath);
    }


//    public static PrintWriter printer;

    public Map<String, List<Operation>> getRiskMethodDiffsMap() {
        return riskMethodDiffsMap;
    }

    private Map<String, List<Operation>> riskMethodDiffsMap = new HashMap<>();

    /**
     * 计算共有方法对的ast tree的差异集合
     *
     * @param semantemeRiskMethods 两个jar包共有的方法集合，即有可能存在语义冲突的方法集合
     * @return 返回有差异的方法对，默认返回排序后的前100个
     */
    private Set<String> calculationDifference(Set<String> semantemeRiskMethods) {

        Map<String, Integer> semantemeMethodForDifferences = new HashMap<>(); // 语义方法的差异集合

        Loader loaderDepJar = new JDCoreLoader(new File(depJarDecompressionPath));
        Loader loaderUsedDepJar = new JDCoreLoader(new File(usedDepJarDecompressionPath));
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();

        AstComparator astComparator = new AstComparator();

        // key class
        // value methods
        Map<String, Set<String>> methodsFromClass = new HashMap<>();
        try {
            for (String method : semantemeRiskMethods) {
                Set<String> methods = methodsFromClass.get(SootUtil.mthdSig2cls(method));
                if (methods == null) {
                    methods = new HashSet<>();
                }
                methods.add(method);
                methodsFromClass.put(SootUtil.mthdSig2cls(method), methods);
            }

            MavenUtil.i().getLog().info("decompiler......");
            MavenUtil.i().getLog().info("use thread num: " + Conf.nThreads);
            ExecutorService executor = Executors.newFixedThreadPool(Conf.nThreads);
//            ThreadPoolExecutor executor = new ThreadPoolExecutor(Conf.nThreads+1, Conf.nThreads + 1, 1L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadPoolExecutor.CallerRunsPolicy());
            for (String methodClassSig : methodsFromClass.keySet()) {
                executor.execute(new Thread(new Runnable() {
                    /**
                     * When an object implementing interface <code>Runnable</code> is used
                     * to create a thread, starting the thread causes the object's
                     * <code>run</code> method to be called in that separately executing
                     * thread.
                     * <p>
                     * The general contract of the method <code>run</code> is that it may
                     * take any action whatsoever.
                     *
                     * @see Thread#run()
                     */
                    @Override
                    public void run() {

                        Printer printerDepJar = new JDCorePrinter();
                        Printer printerUsedDepJar = new JDCorePrinter();
                        try {
                            decompiler.decompile(loaderDepJar, printerDepJar, methodClassSig.replace(".", File.separator));
                            decompiler.decompile(loaderUsedDepJar, printerUsedDepJar, methodClassSig.replace(".", File.separator));
                        } catch (Exception e) {
                            MavenUtil.i().getLog().error(e);
                            return;
                        }
//                        MavenUtil.i().getLog().info("decompiler success");

                        String depJarContent = printerDepJar.toString();
                        String usedDepJarContent = printerUsedDepJar.toString();

                        for (String method : methodsFromClass.get(methodClassSig)) {
                            try {

                                List<CtMethod<?>> depJarCtMethods = astComparator.getCtType(depJarContent).getMethodsByName(SootUtil.mthdSig2methodName(method));

                                List<CtMethod<?>> usedDepJarCtMethods = astComparator.getCtType(usedDepJarContent).getMethodsByName(SootUtil.mthdSig2methodName(method));

                                CtMethod ctMethodFromDepJar = getCtMethod(depJarCtMethods, SootUtil.mthdSig2param(method));

                                CtMethod ctMethodFromUsedDepJar = getCtMethod(usedDepJarCtMethods, SootUtil.mthdSig2param(method));

                                if (ctMethodFromDepJar == null || ctMethodFromUsedDepJar == null) {
                                    continue;
                                }
                                Diff diff = astComparator.compare(ctMethodFromDepJar, ctMethodFromUsedDepJar);

                                int differentSize = diff.getRootOperations().size();
                                if (differentSize > 0) {
                                    riskMethodDiffsMap.put(method, diff.getRootOperations());
                                    //输出差异
//                            printer.println(method + " ===> diff count : " + differentSize + "\n used compare shield diff:");
//                            if (Conf.printDiff) {
//                                for (Operation operation : diff.getRootOperations()) {
//                                    try {
//                                        printer.println(operation.toString());
//                                    } catch (Exception e) {
////                                    e.printStackTrace();
//                                        break;
//                                    }
//                                }
//                            }
//                            System.out.println(method + differentSize);
                                    semantemeMethodForDifferences.put(method, differentSize);
                                }
                            } catch (Throwable e) {
                                break;
                            }
                        }
                    }
                }));
            }
            executor.shutdown();

            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//                while (!executor.isTerminated()) {
//                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            MavenUtil.i().getLog().error(e.toString());
//            e.printStackTrace();
        }
        MavenUtil.i().getLog().info("decompiler complete");
        return sortMap(semantemeMethodForDifferences, Conf.MAX_RISK_METHOD_NUM);
    }

    /**
     * 通过输入的参数列表找到特定的方法
     *
     * @param ctMethods
     * @param sootMethodParams
     * @return
     */
    private CtMethod<?> getCtMethod(List<CtMethod<?>> ctMethods, String sootMethodParams) {

        if (sootMethodParams.length() == 0) {
            for (CtMethod<?> ctMethod : ctMethods) {
                if (ctMethod.getParameters().size() == 0) {
                    return ctMethod;
                }
            }
        } else if (!sootMethodParams.contains(",")) {
            for (CtMethod<?> ctMethod : ctMethods) {
                if (ctMethod.getParameters().size() == 1 && (ctMethod.getParameters().get(0)).getType().getQualifiedName().equals(sootMethodParams)) {
                    return ctMethod;
                }
            }
        } else {
            String[] params = sootMethodParams.split(",");
            for (CtMethod<?> ctMethod : ctMethods) {
                int flag = 0;
                if (ctMethod.getParameters().size() == params.length) {
                    for (int i = 0; i < params.length; i++) {
                        if (ctMethod.getParameters().get(i).getType().getQualifiedName().equals(params[i])) {
                            flag++;
                        } else {
                            break;
                        }
                    }
                    if (flag == params.length) {
                        return ctMethod;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 对Map排序后，输出前N个Intger最大的method 降序
     *
     * @param entrySize 大小限制，输出多少个排序后数组
     * @return
     */
    private Set<String> sortMap(Map<String, Integer> semantemeMethodForDifferences, int entrySize) {
        if (semantemeMethodForDifferences.size() == 0) {
            return null;
        }
        LinkedHashSet<String> afterSortMethods = new LinkedHashSet<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(
                semantemeMethodForDifferences.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> obj1, Map.Entry<String, Integer> obj2) {
                return obj2.getValue() - obj1.getValue();
            }
        });
        int size = 0;
        if (semantemeMethodForDifferences.size() > entrySize) {
            size = entrySize;
        } else {
            size = semantemeMethodForDifferences.size();
        }
        for (int i = 0; i < size; i++) {
            afterSortMethods.add(entries.get(i).getKey());
        }
        return afterSortMethods;
    }

    @Override
    public String toString() {
        return depJar.toString() + " in conflict " + usedDepJar.toString();
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        MavenUtil.i().setMojo(new CountProjectMojo());
        List<String> JarFilePath = new ArrayList<>();
        JarFilePath.add(getRepositoryJarPath("org.reactivestreams", "reactive-streams", "1.0.2"));
        DepJar usedDepJar = new DepJar("org.reactivestreams", "reactive-streams", "1.0.2", "", JarFilePath);
        List<String> conflictJarFilePath = new ArrayList<>();
        conflictJarFilePath.add(getRepositoryJarPath("org.reactivestreams", "reactive-streams", "0.4.0.M2"));
        DepJar conflictDepJar = new DepJar("org.reactivestreams", "reactive-streams", "0.4.0.M2", "", conflictJarFilePath);
        DepJarJRisk depJarJRisk = new DepJarJRisk(conflictDepJar, usedDepJar);
        Map<String, List<Operation>> riskMethodDiffsMap = depJarJRisk.getAllSemantemeMethodForDifferences();
        System.out.println("risk method size : " + riskMethodDiffsMap.keySet().size());
        FileWriter fileWriter = new FileWriter("sensor_testcase/commonMethods.txt", false);
        for (String method : riskMethodDiffsMap.keySet()) {
            fileWriter.write(method + "@@" + riskMethodDiffsMap.get(method).size() + "\n");
        }
        fileWriter.close();
        long endTime = System.currentTimeMillis();
        System.out.println("运行时间 : " + (endTime - startTime) + "ms");
    }

    public static String getRepositoryJarPath(String groupId, String artifactId, String version) {
        return "/Users/wangchao/.m2/repository/" +
                groupId.replace(".", "/") + File.separator +
                artifactId.replace(".", "/") + File.separator +
                version + File.separator +
                artifactId + "-" + version + ".jar";
    }
}
