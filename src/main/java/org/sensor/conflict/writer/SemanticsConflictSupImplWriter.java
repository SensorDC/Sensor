package org.sensor.conflict.writer;

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
import java.util.Map;

public class SemanticsConflictSupImplWriter {
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
                            printer.println("classPath:" + DepJars.i().getUsedJarPathsStr());
                            printer.println("pomPath:" + MavenUtil.i().getBaseDir());
                            for (Record4path record : dis2records.flat()) {
                                printer.println("\n" + "conflict:" + conflict.toString());
                                printer.println("risk method name:" + record.getRiskMethod());
                                printer.println("form conflict:" + depJarRisk.getConflictDepJar().toString());
                                printer.println("pathLen:" + record.getPathlen() + "\n" + addJarPath(record.getPathStr()));
                            }
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
