package org.sensor.conflict.vo;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.sensor.conflict.container.SemantemeMethods;
import org.sensor.conflict.graph.Graph4path;
import org.sensor.conflict.graph.GraphForMethodOutPath;
import org.sensor.conflict.graph.IGraph;
import org.sensor.conflict.soot.SootJRiskCg;
import org.sensor.conflict.soot.tf.JRiskMethodOutPathCgTf;
import org.sensor.conflict.soot.tf.JRiskMthdPathCgTf;
import org.sensor.conflict.util.*;
import org.sensor.evosuiteshell.Config;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import spoon.reflect.declaration.CtMethod;

/**
 * two jar that have different name and same class.
 *
 * @author asus
 */
public class DupClsJarPair {
    private DepJar jar1;
    private DepJar jar2;
    private Set<String> clsSigs;
    private Set<String> thrownMethods;

    public void addThrownMethods(String method) {
        if (thrownMethods == null) {
            thrownMethods = new HashSet<String>();
        }
        this.thrownMethods.add(method);
    }

    public DupClsJarPair(DepJar jarA, DepJar jarB) {
        jar1 = jarA;
        jar2 = jarB;
        clsSigs = new HashSet<String>();
    }

    public String getSig() {
        return jar1.toString() + "-" + jar2.toString();
    }

    public boolean isInDupCls(String rhcedMthd) {
        return clsSigs.contains(SootUtil.mthdSig2cls(rhcedMthd));
    }

    public void addClass(String clsSig) {
        clsSigs.add(clsSig);
    }

    public boolean isSelf(DepJar jarA, DepJar jarB) {
        return (jar1.equals(jarA) && jar2.equals(jarB)) || (jar1.equals(jarB) && jar2.equals(jarA));
    }

    public DepJar getJar1() {
        return jar1;
    }

    public DepJar getJar2() {
        return jar2;
    }

    public String getRiskString() {
        StringBuilder sb = new StringBuilder("classConflict:");
        sb.append("<" + jar1.toString() + ">");
        sb.append("<" + jar2.toString() + ">\n");
        sb.append(getJarString(jar1, jar2));
        sb.append(getJarString(jar2, jar1));
        return sb.toString();
    }

    private String getJarString(DepJar total, DepJar some) {
        StringBuilder sb = new StringBuilder();
        List<String> onlyMthds = getOnlyMethod(total, some);
        sb.append("   methods that only exist in " + total.getValidDepPath() + "\n");
        if (onlyMthds.size() > 0) {
            for (String onlyMthd : onlyMthds) {
                sb.append(onlyMthd + "\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "jar1=" + jar1 + ", jar2=" + jar2;
    }

    private List<String> getOnlyMethod(DepJar total, DepJar some) {
        List<String> onlyMthds = new ArrayList<String>();
        for (String clsSig : clsSigs) {
            ClassVO classVO = total.getClassVO(clsSig);
            if (classVO != null) {
                for (MethodVO mthd : classVO.getMethods()) {
                    if (!some.getClassVO(clsSig).hasMethod(mthd.getMthdSig()))
                        onlyMthds.add(mthd.getMthdSig());
                }
            }
        }
        return onlyMthds;
    }

    public Collection<String> getPrcDirPaths() throws Exception {
        List<String> classpaths;
        classpaths = jar1.getRepalceClassPath();
        return classpaths;
    }

    public String getCommonMethodsString() {
        StringBuilder sb = new StringBuilder("\n");
        DepJar depJar;
        if (jar1.isSelected()) {
            depJar = jar2.getUsedDepJar();
            sb.append("<" + jar1.toString() + ">  used:" + jar1.isSelected() + " scope:" + jar1.getScope() + "\n");
            sb.append("<" + jar2.toString() + ">  used:" + jar2.isSelected() + " scope:" + jar2.getScope() + "\n");
            if (!depJar.isSelf(jar2)) {
                sb.append("<" + depJar.toString() + ">  used:" + depJar.isSelected() + " scope:" + jar2.getScope()
                        + "\n");
            }
        } else if (jar2.isSelected()) {
            depJar = jar1.getUsedDepJar();
            sb.append("<" + jar1.toString() + ">  used:" + jar1.isSelected() + " scope:" + jar1.getScope() + "\n");
            sb.append("<" + jar2.toString() + ">  used:" + jar2.isSelected() + " scope:" + jar2.getScope() + "\n");
            if (!depJar.isSelf(jar1)) {
                sb.append("<" + depJar.toString() + ">  used:" + depJar.isSelected() + " scope:" + jar1.getScope()
                        + "\n");
            }
        }
        return sb.toString();
    }

    public Set<String> getCommonMethods() {
        Set<String> commonMethods = new HashSet<String>();
        for (String clsSig : clsSigs) {
            ClassVO classVO = jar1.getClassVO(clsSig);
            if (classVO != null) {
                for (MethodVO mthd : classVO.getMethods()) {
                    if (jar2.getClassVO(clsSig).hasMethod(mthd.getMthdSig()))
                        commonMethods.add(mthd.getMthdSig());
                }
            }
        }
        return commonMethods;
    }

    Map<String, List<Integer>> semantemeMethodForDifferences; // 语义方法的差异集合

    public Map<String, List<Integer>> getSemantemeMethodForDifferences() {
        return semantemeMethodForDifferences;
    }

    public Graph4path getMethoPathGraphForSemanteme() {

        Set<String> semantemeRiskMethods = getCommonMethods();
        Set<String> riskMethods;

        if (semantemeRiskMethods.size() > 0) {

            GraphForMethodOutPath depJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i().getGraph(jar1,
                    new JRiskMethodOutPathCgTf(semantemeRiskMethods));

            GraphForMethodOutPath usedDepJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i()
                    .getGraph(jar2, new JRiskMethodOutPathCgTf(semantemeRiskMethods));

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

    // depJar 的反编译文件路径
    private String depJarDecompressionPath;
    // used depJar 的反编译文件路径
    private String usedDepJarDecompressionPath;

    /**
     * 解压冲突jar包
     */
    public void decompile() {

        depJarDecompressionPath = JARDecompressionTool.decompressionPath + jar1.getDepJarName() + Config.FILE_SEPARATOR;

        usedDepJarDecompressionPath = JARDecompressionTool.decompressionPath + jar2.getDepJarName() + Config.FILE_SEPARATOR;

        JARDecompressionTool.decompress(jar1.getJarFilePaths(true).get(0), depJarDecompressionPath);

        JARDecompressionTool.decompress(jar2.getJarFilePaths(true).get(0), usedDepJarDecompressionPath);
    }

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
// 
//	public ClsDupJarPairRisk getPairRisk(DepJarNRisks jarCgs) {
//		return new ClsDupJarPairRisk(this, jarCgs.getDepJarCg(getJar1()), jarCgs.getDepJarCg(getJar2()));
//	}

    // @Override
    // public int hashCode() {
    // return jar1.hashCode() + jar2.hashCode();
    // }
    //
    // @Override
    // public boolean equals(Object obj) {
    // if (obj instanceof JarCmp) {
    // JarCmp other = (JarCmp) obj;
    // return (jar1.equals(other.getJar1()) && jar2.equals(other.getJar2()))
    // || (jar1.equals(other.getJar2()) && jar2.equals(other.getJar1()));
    // }
    // return false;
    // }
}
