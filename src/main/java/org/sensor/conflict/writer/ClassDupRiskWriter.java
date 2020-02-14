package org.sensor.conflict.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import gumtree.spoon.diff.operations.Operation;
import org.sensor.conflict.container.ClassDups;
import org.sensor.conflict.container.Conflicts;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.container.DupClsJarPairs;
import org.sensor.conflict.graph.Book4path;
import org.sensor.conflict.graph.Dog;
import org.sensor.conflict.graph.Graph4path;
import org.sensor.conflict.graph.IBook;
import org.sensor.conflict.graph.IRecord;
import org.sensor.conflict.graph.Record4path;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.graph.Dog.Strategy;
import org.sensor.conflict.util.Conf;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.MySortedMap;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.ClassDup;
import org.sensor.conflict.vo.Conflict;
import org.sensor.conflict.vo.DepJar;
import org.sensor.conflict.vo.DupClsJarPair;

public class ClassDupRiskWriter {

    private ClassDups classDups;

    private DupClsJarPairs jarPairs;

    public ClassDupRiskWriter() {
        classDups = new ClassDups(DepJars.i());
    }

    public void writeByClass() {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(Conf.outDir + "classDupRisk.txt"), true)));
            printer.println("projectPath->" + MavenUtil.i().getProjectInfo());

            printer.println("class duplicate:");
            for (ClassDup classDup : classDups.getAllClsDup()) {
                printer.println(classDup.getRiskString());
            }
            printer.println("\n\n\n\n");
        } catch (Exception e) {
            MavenUtil.i().getLog().error("can't write classDupByClass:", e);
        } finally {
            printer.close();
        }
    }

    public void writeByJarNoClassMissing(String outPath) {
        PrintWriter printer = null;
        try {
            String fileName = MavenUtil.i().getProjectGroupId() + "_" + MavenUtil.i().getProjectArtifactId() + "_"
                    + MavenUtil.i().getProjectVersion();
            printer = new PrintWriter(new BufferedWriter(
                    new FileWriter(new File(outPath + fileName.replace('.', '_') + "DupByJar.txt"), true)));

            for (DupClsJarPair jarPair : getJarPairs().getAllJarPair()) {
//				printer.println("projectPath->" + MavenUtil.i().getProjectInfo());
                if (Conf.targetJar == null || "".equals(Conf.targetJar) || jarPair.getSig().contains(Conf.targetJar)) {
                    write(jarPair, printer);
                }
//				printer.println(jarPair.getJar1().getJarFilePaths(false));
            }
        } catch (Exception e) {
            MavenUtil.i().getLog().error("can't write classDupByJar:", e);
        } finally {
            printer.close();
        }
    }

    public void write(DupClsJarPair jarPair, PrintWriter printer) {
//		printer.println(jarPair.getCommonMethods());
        Graph4path pathGraph = jarPair.getMethoPathGraphForSemanteme();
        Set<String> hostNodes = pathGraph.getHostNodes();
        Map<String, String> methodMappingASMMethod = pathGraph.getMethodMappingASMMethod();
        Map<String, List<Operation>> allRiskMethodDiffsMap = jarPair.getRiskMethodDiffsMap();
//		System.out.println(hostNodes.toString());
        Map<String, IBook> pathBooks = new Dog(pathGraph).findRlt(hostNodes, Conf.DOG_DEP_FOR_PATH,
                Strategy.NOT_RESET_BOOK);
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
//		Map<String, List<Integer>> semantemeMethodForDifferences = jarPair.getSemantemeMethodForDifferences();
        int num = 0;
        if (dis2records.size() > 0) {
            Map<String, Integer> sortMap = new TreeMap<>();
            Set<String> hasWriterRiskMethodPath = new HashSet<>();
            printer.println("conflict : " + jarPair.toString());
//            printer.println("classPath:" + DepJars.i().getUsedJarPathsStr());
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

    //
    public void writeByJarHasClassMissing(String outPath) {
        try {
            String fileName = MavenUtil.i().getProjectGroupId() + "_" + MavenUtil.i().getProjectArtifactId() + "_"
                    + MavenUtil.i().getProjectVersion();
            PrintWriter printer = new PrintWriter(new BufferedWriter(
                    new FileWriter(new File(outPath + fileName.replace('.', '_') + "DupByJar.txt"), true)));

            for (Conflict conflict : Conflicts.i().getConflicts()) {
                printer.println("projectPath->" + MavenUtil.i().getProjectInfo());
                for (DepJarJRisk depJarJRisk : conflict.getJarRisks()) {
//					System.out.println(depJarJRisk.toString());
                    ClassDups newClassDups = new ClassDups(DepJars.i(), depJarJRisk);
//					System.out.println(newClassDups.getAllClsDup().size());
                    DupClsJarPairs newJarPairs = new DupClsJarPairs(newClassDups);
//					System.out.println(newJarPairs.getAllJarPair().size());
                    for (DupClsJarPair jarPair : newJarPairs.getAllJarPair()) {
//						printer.println(jarPair.toString());
                        write(jarPair, printer);
                    }
                }
            }
            printer.close();
        } catch (Exception e) {
            MavenUtil.i().getLog().error("can't write classDupByJar:", e);
        }
    }
//	public void writeRchNum(String outPath, boolean append) {
//		try {
//			CSVPrinter printer = new CSVPrinter(new FileWriter(outPath, append), CSVFormat.DEFAULT);
//			DepJarNRisks jarCgs = new DepJarNRisks();
//			for (DupClsJarPair jarPair : getJarPairs().getAllJarPair()) {
//				FourRow fourRow = jarPair.getPairRisk(jarCgs).getFourRow();
//				printer.printRecord(fourRow.mthdRow);
//				printer.printRecord(fourRow.mthdNameRow);
//				printer.printRecord(fourRow.serviceRow);
//				printer.printRecord(fourRow.serviceNameRow);
//			}
//			printer.close();
//		} catch (Exception e) {
//			MavenUtil.i().getLog().error("can't write reach class number:", e);
//		}
//	}

    private DupClsJarPairs getJarPairs() {
        if (jarPairs == null)
            jarPairs = new DupClsJarPairs(classDups);
        return jarPairs;
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
