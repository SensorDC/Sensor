package org.sensor.conflict.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensor.conflict.container.Conflicts;
import org.sensor.conflict.container.DepJars;
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
import org.sensor.conflict.vo.DepJar;

public class SemanticsRiskWriter {

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
					Map<String, List<Integer>> semantemeMethodForDifferences = depJarRisk
							.getSemantemeMethodForDifferences();
					if (dis2records.size() > 0) {
//							Set<String> hasWriterRiskMethodPath = new HashSet<String>();
						printer.println("classPath:" + DepJars.i().getUsedJarPathsStr());
						printer.println("pomPath:" + MavenUtil.i().getBaseDir());
						for (Record4path record : dis2records.flat()) {
//								if (!hasWriterRiskMethodPath.contains(record.getRiskMthd())) {
//								if(addJarPath(record.getPathStr()).contains(conflictDepJarVersion)) {
							List<Integer> differenceAndSame = semantemeMethodForDifferences.get(record.getRiskMthd());
							printer.println("\n" + "conflict:" + conflict.toString());
							printer.println("risk method name:" + record.getRiskMthd());
							printer.println("来自冲突版本:" + depJarRisk.getConflictDepJar().toString());
							printer.println("差异:" + differenceAndSame.get(0));
							printer.println("相同:" + differenceAndSame.get(1));
							printer.println("pathLen:" + record.getPathlen() + "\n" + addJarPath(record.getPathStr()));
//									hasWriterRiskMethodPath.add(record.getRiskMthd());
//								}
//								}
						}
					}
				}
			}

		} catch (Exception e) {
			MavenUtil.i().getLog().error("can't write jar duplicate risk:", e);
		}
		printer.close();
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
