package neu.lab.conflict.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import neu.lab.conflict.container.Conflicts;
import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.graph.GraphForMethodName;
import neu.lab.conflict.risk.jar.DepJarJRisk;
import neu.lab.conflict.soot.SootJRiskCg;
import neu.lab.conflict.soot.tf.JRiskObjectCgTf;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.vo.Conflict;
import neu.lab.conflict.vo.DepJar;

public class CountProjectWriter {

	private List<Conflict> conflicts;

	public CountProjectWriter() {
		conflicts = Conflicts.i().getConflicts();
	}

	public void writeTofileForSourceObjectCount(String outPath) {
		PrintWriter printer = null;
		try {
			printer = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "ObjectCount.txt", true)));
			writeSourceInfo(printer);
			printer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeToFileForCountInfo(String outPath) {
		PrintWriter printer = null;
		try {
			printer = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "ConflictCount.txt", true)));
			writeConflictSig(printer);
			printer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeForRiskMethodInProject(String outPath) {
		PrintWriter printer = null;
		try {
			printer = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "RiskMethodCount.txt", true)));
			writeRiskMethodCountInProject(printer);
			printer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeConflictSig(PrintWriter printer) {
		for (Conflict conflict : conflicts) {
			printer.println(conflict.getSig());
		}
	}

	private void writeRiskMethodCountInProject(PrintWriter printer) {
		printer.println("projectInfo=" + MavenUtil.i().getProjectCor());
		for (Conflict conflict : conflicts) {
			printer.println("conflictSig=" + conflict.getSig());
			for (DepJarJRisk depJarRisk : conflict.getJarRisks()) {
				printer.println("conflictVersion=" + depJarRisk.toString());
//				Graph4path pathGraph = 
				depJarRisk.getAllSemantemeMethodForDifferences();
//				Set<String> hostNodes = pathGraph.getHostNodes();
//				Map<String, IBook> pathBooks = new Dog(pathGraph).findRlt(hostNodes, Conf.DOG_DEP_FOR_PATH,
//						Strategy.NOT_RESET_BOOK);
//				MySortedMap<Integer, Record4path> dis2records = new MySortedMap<Integer, Record4path>();
//				for (String topMthd : pathBooks.keySet()) {
//					if (hostNodes.contains(topMthd)) {
//						Book4path book = (Book4path) (pathBooks.get(topMthd));
//						for (IRecord iRecord : book.getRecords()) {
//							Record4path record = (Record4path) iRecord;
//							dis2records.add(record.getPathlen(), record);
//						}
//					}
//				}
				Map<String, List<Integer>> semantemeMethodForReturn = depJarRisk.getSemantemeMethodForDifferences();
				for (String method : semantemeMethodForReturn.keySet()) {
					printer.println("riskMethod=" + method);
					printer.println("difference=" + semantemeMethodForReturn.get(method).get(0));
				}

//				if (dis2records.size() > 0) {
//					Set<String> hasWriterRiskMethodPath = new HashSet<String>();
//					for (Record4path record : dis2records.flat()) {
//						if (!hasWriterRiskMethodPath.contains(record.getRiskMthd())) {
//							int difference = semantemeMethodForDifferences.get(record.getRiskMthd());
//							printer.println("riskMethod=" + record.getRiskMthd() + " " + "difference=" + difference);
//							hasWriterRiskMethodPath.add(record.getRiskMthd());
//						}
//					}
//				}
			}
		}
		printer.println();
	}

	/**
	 * 得到Source文件的对象多样化信息
	 */
	private void writeSourceInfo(PrintWriter printer) {
		printer.println("projectInfo=" + MavenUtil.i().getProjectCor());
		DepJar hostDepJar = DepJars.i().getHostDepJar();
		int sum = 0;
		GraphForMethodName graphForMethodName = (GraphForMethodName) SootJRiskCg.i()
				.getGraph(hostDepJar.getJarFilePaths(true).toArray(new String[0]), new JRiskObjectCgTf());
		HashMap<String, ArrayList<String>> accessibleMethods = graphForMethodName.getAccessibleMethod();
		for (String accessibleMethod : accessibleMethods.keySet()) {
			printer.println("可访问的类方法：" + accessibleMethod);
			int size = accessibleMethods.get(accessibleMethod).size();
			sum += size;
			printer.println("\\+数目：" + size);
			for (String method : accessibleMethods.get(accessibleMethod)) {
				printer.println("\\+返回值为对象的方法：" + method);
			}
		}
		printer.println("总数目：" + sum);
	}
}
