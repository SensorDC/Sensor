package neu.lab.conflict.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import neu.lab.conflict.container.Conflicts;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.vo.Conflict;

public class RepairWriter {
	private String safeJar;

	public void write(String outPath) {
		String projectName = MavenUtil.i().getProjectGroupId() + MavenUtil.i().getProjectArtifactId()
				+ MavenUtil.i().getProjectVersion();
		String projectRisk = outPath + "projectRisk" + ".txt";
		String hasRiskProject = outPath + "hasRiskProject.txt";
		PrintWriter printer = null;
		PrintWriter printerRisk = null;
		try {
			printer = new PrintWriter(new BufferedWriter(new FileWriter(projectRisk, true)));
			printerRisk = new PrintWriter(new BufferedWriter(new FileWriter(hasRiskProject, true)));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		printer.println("项目>>>>" + projectName);
		for (Conflict conflict : Conflicts.i().getConflicts()) {
			int riskLevel = 0;
			Map<Integer, String> result = new HashMap<Integer, String>();
			result = conflict.getRiskLevel();
			riskLevel = result.keySet().iterator().next();
			safeJar = result.get(riskLevel);
			printer.println("冲突>>>>" + conflict.toString());
			printer.println("冲突风险等级>>>>" + riskLevel);
			printer.println("安全jar包>>>>" + safeJar);
			printer.println("========================");
			printer.println();
			printer.close();
			if (riskLevel == 3 || riskLevel == 4) {
				printerRisk.println("module:" + MavenUtil.i().getProjectPom());
				printerRisk.println("conflict:" + conflict.getSig());
			}
			//writeXML(outPath);
		}
	}

	public void writeXML(String outPath) {
		String pomPath = MavenUtil.i().getProjectPom();
		String pomRepairPath = outPath + MavenUtil.i().getProjectGroupId() + MavenUtil.i().getProjectArtifactId()
				+ MavenUtil.i().getProjectVersion() + "pomRepaired.xml";
		File pomFile = new File(pomPath);
		SAXReader reader = new SAXReader();
		try {
			Document doc = reader.read(pomFile);
			Element root = doc.getRootElement();
			Element dependencies = root.element("dependencies");
			dependencies = deleteDupJar(dependencies);
			Element dependency = dependencies.addElement("dependency");
			dependency = addRightJarInPom(dependency);
			Writer fileWriter = new FileWriter(pomRepairPath);
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setNewlines(true);
			XMLWriter xmlWriter = new XMLWriter(fileWriter, format);
			xmlWriter.write(doc);
			xmlWriter.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public Element deleteDupJar(Element dependencies) {
		String[] jarSig = safeJar.split(":");
		for (Iterator it = dependencies.elements("dependency").iterator(); it.hasNext();) {
			Element element = (Element) it.next();
			if (element.element("groupId").getText().equals(jarSig[0])&&element.element("artifactId").getText().equals(jarSig[1])) {
				dependencies.remove(element);
			}
		}
		return dependencies;
	}
	
	public Element addRightJarInPom(Element dependency) {
		Element groupId = dependency.addElement("groupId");
		Element artifactId = dependency.addElement("artifactId");
		Element version = dependency.addElement("version");
		String[] jarSig = safeJar.split(":");
		groupId.addText(jarSig[0]);
		artifactId.addText(jarSig[1]);
		version.addText(jarSig[2]);
		
		return dependency;
	}
}
