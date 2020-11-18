package org.sensor.conflict;

import fj.Hash;
import gumtree.spoon.diff.operations.Operation;
import org.sensor.conflict.container.Conflicts;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.graph.*;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.util.Conf;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.MySortedMap;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.Conflict;
import org.sensor.conflict.vo.DepJar;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wangchao
 * 用来打印输出API对的详细信息，用于Issue Report数据补充
 */
@Mojo(name = "printAPIDataInfo", defaultPhase = LifecyclePhase.VALIDATE)
public class PrintAPIDataInfoMojo extends ConflictMojo {
    @Override
    public void run() {
        writeSemanticsPath(Conf.outDir);
    }

    /**
     * 输出到文件中
     *
     * @param outPath
     */
    public void writeSemanticsRiskToFile(String outPath) {
        PrintWriter printer = null;
        try {

            String fileName = MavenUtil.i().getProjectGroupId() + ":" + MavenUtil.i().getProjectArtifactId() + ":"
                    + MavenUtil.i().getProjectVersion();
            printer = new PrintWriter(new BufferedWriter(
                    new FileWriter(outPath + "path_" + fileName.replace('.', '_').replace(':', '_') + ".txt", true)));

            for (Conflict conflict : Conflicts.i().getConflicts()) {
                for (DepJarJRisk depJarRisk : conflict.getJarRisks()) {
                    Graph4path pathGraph = depJarRisk.getMethodPathGraphForSemanteme();
                    Set<String> hostNodes = pathGraph.getHostNodes();
                    Map<String, String> methodMappingASMMethod = pathGraph.getMethodMappingASMMethod();
                    Map<String, List<Operation>> allRiskMethodDiffsMap = depJarRisk.getRiskMethodDiffsMap();
                    Map<String, IBook> pathBooks = new Dog(pathGraph).findRlt(hostNodes, Conf.DOG_DEP_FOR_PATH,
                            Dog.Strategy.NOT_RESET_BOOK);
                    MySortedMap<Integer, Record4path> dis2records = new MySortedMap<Integer, Record4path>();
                    for (String topMthd : pathBooks.keySet()) {
                        if (hostNodes.contains(topMthd)) {
                            Book4path book = (Book4path) (pathBooks.get(topMthd));
                            for (IRecord iRecord : book.getRecords()) {
                                Record4path record = (Record4path) iRecord;
                                dis2records.add(record.getPathlen(), record);
                            }
                        }
                    }
                    if (dis2records.size() > 0) {
                        HashSet<String> has = new HashSet<>();
                        for (Record4path record : dis2records.flat()) {
                            if (!has.contains(record.getRiskMethod())) {
                                int differenceAndSame;
                                try {
                                    differenceAndSame = allRiskMethodDiffsMap.get((methodMappingASMMethod.get(record.getRiskMethod()))).size();
                                } catch (Exception e) {
                                    MavenUtil.i().getLog().error(e + "\n" + record.getRiskMethod());
                                    continue;
                                }
                                printer.println("\n" + "Conflicting library : " + conflict.toString());
                                printer.println("Conflicting API pair : " + record.getRiskMethod());
                                printer.println("====================");
                                printer.println("Diff size : " + differenceAndSame);
                                printer.println("Diff : ");
                                for (Operation operation : allRiskMethodDiffsMap.get((methodMappingASMMethod.get(record.getRiskMethod())))) {
                                    printer.print(operation.toString());
                                }
                                printer.println("====================");
                                printer.println("Path length : " + record.getPathlen());
                                printer.println("Invocation path : " + "\n" + addJarPath(record.getPathStr()));
                                printer.println("====================");
                                printer.println();
                                has.add(record.getRiskMethod());
                            }
                        }
                        printer.println("\n" + has.size());
                    }
                }
            }

        } catch (Exception e) {
            MavenUtil.i().getLog().error(e);
        } finally {
            printer.close();
        }
    }

    public void writeSemanticsPath(String outPath) {
        PrintWriter printer = null;
        try {
            String fileName = MavenUtil.i().getProjectGroupId() + ":" + MavenUtil.i().getProjectArtifactId() + ":"
                    + MavenUtil.i().getProjectVersion();
            printer = new PrintWriter(new BufferedWriter(
                    new FileWriter(outPath + "supImplSemantics_" + fileName.replace('.', '_').replace(':', '_') + ".txt", true)));

            for (Conflict conflict : Conflicts.i().getConflicts()) {
                if (Conf.targetJar == null || "".equals(Conf.targetJar) || conflict.getSig().contains(Conf.targetJar)) {
                    for (DepJarJRisk depJarRisk : conflict.getJarRisks()) {
                        Graph4path graph4path = depJarRisk.getGraph4mthdPath();
                        Map<String, IBook> pathBooks = new Dog(graph4path).findRlt(graph4path.getAllNode(), Conf.DOG_DEP_FOR_PATH,
                                Dog.Strategy.NOT_RESET_BOOK);
                        MySortedMap<Integer, Record4path> dis2records = new MySortedMap<Integer, Record4path>();
                        for (String topMethod : pathBooks.keySet()) {
                            if (graph4path.getHostNodes().contains(topMethod)) {
                                Book4path book = (Book4path) (pathBooks.get(topMethod));
                                for (IRecord iRecord : book.getRecords()) {
                                    Record4path record = (Record4path) iRecord;
                                    dis2records.add(record.getPathlen(), record);
                                }
                            }
                        }
                        if (dis2records.size() > 0) {
                            HashSet<String> has = new HashSet<>();
                            for (Record4path record : dis2records.flat()) {
                                if (!has.contains(record.getRiskMethod())) {
                                    printer.println("\n" + "Conflicting library : " + conflict.toString());
                                    printer.println("Conflicting API pair : " + record.getRiskMethod());
                                    printer.println("====================");
                                    printer.println("Path length : " + record.getPathlen());
                                    printer.println("Invocation path : " + "\n" + addJarPath(record.getPathStr()));
                                    printer.println("====================");
                                    printer.println();
                                    has.add(record.getRiskMethod());
                                }
                            }
                            printer.println("\n" + has.size());
                        }
                    }
                }
            }
        } catch (Exception e) {
            MavenUtil.i().getLog().error(e.getMessage());
        } finally {
            printer.close();
        }
    }

    private String addJarPath(String mthdCallPath) {
        StringBuilder sb = new StringBuilder();
        String[] mthds = mthdCallPath.split("\\n");
        for (int i = 0; i < mthds.length - 1; i++) {
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
}