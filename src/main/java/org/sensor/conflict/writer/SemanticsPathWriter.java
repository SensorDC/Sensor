package org.sensor.conflict.writer;

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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class SemanticsPathWriter {
    public void writeSemanticsPath(String outPath) {
        PrintWriter printer = null;
        try {

            String fileName = MavenUtil.i().getProjectGroupId() + ":" + MavenUtil.i().getProjectArtifactId() + ":"
                    + MavenUtil.i().getProjectVersion();
            printer = new PrintWriter(new BufferedWriter(
                    new FileWriter(outPath + "path_" + fileName.replace('.', '_').replace(':', '_') + ".txt", true)));

            for (Conflict conflict : Conflicts.i().getConflicts()) {
                if (Conf.targetJar == null || "".equals(Conf.targetJar) || conflict.getSig().contains(Conf.targetJar)) {
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
                        int num = 0;
                        if (dis2records.size() > 0) {
                            Map<String, Integer> sortMap = new TreeMap<>();
                            Set<String> hasWriterRiskMethodPath = new HashSet<>();
                            printer.println("conflict:" + conflict.toString());
                            printer.println("classPath:" + DepJars.i().getUsedJarPathsStr());
                            for (Record4path record : dis2records.flat()) {
                                if (!hasWriterRiskMethodPath.contains(record.getRiskMethod())) {
                                    int differenceAndSame = allRiskMethodDiffsMap.get((methodMappingASMMethod.get(record.getRiskMethod()))).size();
                                    StringBuffer stringBuffer = new StringBuffer();

                                    List<Operation> operationList = allRiskMethodDiffsMap.get((methodMappingASMMethod.get(record.getRiskMethod())));
                                    stringBuffer.append("risk method name : " + record.getRiskMethod() + "\n");
                                    stringBuffer.append("diff size : " + differenceAndSame + "\n");
                                    stringBuffer.append("diff :\n");
                                    for (Operation operation : operationList) {
                                        stringBuffer.append(operation.toString());
                                    }
                                    stringBuffer.append("path length : " + record.getPathlen() + "\npath :\n" + addJarPath(record.getPathStr()) + "\n\n\n");
                                    sortMap.put(stringBuffer.toString(), differenceAndSame);
                                    hasWriterRiskMethodPath.add(record.getRiskMethod());
//                                    num++;
                                }
//								}
                            }

                            List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(sortMap.entrySet());

                            Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
                                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                                    return -o1.getValue().compareTo(o2.getValue());
                                }
                            });
                            for (Map.Entry<String, Integer> e : list) {
                                if (num < Conf.semanticsPrintNum) {
                                    printer.println(e.getKey());
                                    num++;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            MavenUtil.i().getLog().error(e);
        } finally {
            printer.close();
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

}
