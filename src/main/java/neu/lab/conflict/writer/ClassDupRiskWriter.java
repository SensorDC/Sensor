package neu.lab.conflict.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.container.ClassDups;
import neu.lab.conflict.container.Conflicts;
import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.container.DupClsJarPairs;
import neu.lab.conflict.graph.Book4path;
import neu.lab.conflict.graph.Dog;
import neu.lab.conflict.graph.Graph4path;
import neu.lab.conflict.graph.IBook;
import neu.lab.conflict.graph.IRecord;
import neu.lab.conflict.graph.Record4path;
import neu.lab.conflict.risk.jar.DepJarJRisk;
import neu.lab.conflict.graph.Dog.Strategy;
import neu.lab.conflict.util.Conf;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.util.MySortedMap;
import neu.lab.conflict.util.SootUtil;
import neu.lab.conflict.vo.ClassDup;
import neu.lab.conflict.vo.Conflict;
import neu.lab.conflict.vo.DepJar;
import neu.lab.conflict.vo.DupClsJarPair;

public class ClassDupRiskWriter {

	private ClassDups classDups;

	private DupClsJarPairs jarPairs;

	public ClassDupRiskWriter() {
		classDups = new ClassDups(DepJars.i());
	}

	public void writeByClass() {
		try {
			PrintWriter printer = new PrintWriter(
					new BufferedWriter(new FileWriter(new File(Conf.outDir + "classDupRisk.txt"), true)));
			printer.println("projectPath->" + MavenUtil.i().getProjectInfo());

			printer.println("class duplicate:");
			for (ClassDup classDup : classDups.getAllClsDup()) {
				printer.println(classDup.getRiskString());
			}
			printer.println("\n\n\n\n");
			printer.close();
		} catch (Exception e) {
			MavenUtil.i().getLog().error("can't write classDupByClass:", e);
		}
	}

	public void writeByJarNoClassMissing(String outPath) {
		try {
			String fileName = MavenUtil.i().getProjectGroupId() + "_" + MavenUtil.i().getProjectArtifactId() + "_"
					+ MavenUtil.i().getProjectVersion();
			PrintWriter printer = new PrintWriter(new BufferedWriter(
					new FileWriter(new File(outPath + fileName.replace('.', '_') + "DupByJar.txt"), true)));

			for (DupClsJarPair jarPair : getJarPairs().getAllJarPair()) {
				printer.println("projectPath->" + MavenUtil.i().getProjectInfo());
				write(jarPair, printer);
//				printer.println(jarPair.getJar1().getJarFilePaths(false));
			}
			printer.close();
		} catch (Exception e) {
			MavenUtil.i().getLog().error("can't write classDupByJar:", e);
		}
	}

	public void write(DupClsJarPair jarPair, PrintWriter printer) {
//		printer.println(jarPair.getCommonMethods());
		Graph4path pathGraph = jarPair.getMethoPathGraphForSemanteme();
		Set<String> hostNodes = pathGraph.getHostNodes();
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
		Map<String, List<Integer>> semantemeMethodForDifferences = jarPair.getSemantemeMethodForDifferences();
		if (dis2records.size() > 0) {
//			Set<String> hasWriterRiskMethodPath = new HashSet<String>();
			printer.println("classPath:" + DepJars.i().getUsedJarPathsStr());
			printer.println("pomPath:" + MavenUtil.i().getBaseDir());
			
			for (Record4path record : dis2records.flat()) {
//				if (!hasWriterRiskMethodPath.contains(record.getRiskMthd())) {
//				if(addJarPath(record.getPathStr()).contains(conflictDepJarVersion)) {
				List<Integer> differenceAndSame = semantemeMethodForDifferences.get(record.getRiskMthd());
				printer.println("\n" + "conflict:" + jarPair.getCommonMethodsString());
				printer.println("risk method name:" + record.getRiskMthd());
				printer.println("差异:" + differenceAndSame.get(0));
				printer.println("相同:" + differenceAndSame.get(1));
				printer.println("pathLen:" + record.getPathlen() + "\n" + addJarPath(record.getPathStr()));
//					hasWriterRiskMethodPath.add(record.getRiskMthd());
//				}
//				}
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
