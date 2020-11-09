package neu.lab.conflict.soot.tf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.graph.IGraph;
import neu.lab.conflict.risk.jar.DepJarJRisk;
import neu.lab.conflict.util.LibCopyInfo;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.vo.DepJar;
import soot.SceneTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;

/**
 * to get call-graph. 得到call-graph
 * 
 * @author asus
 *
 */
public abstract class JRiskCgTf extends SceneTransformer {

	// private DepJarJRisk depJarJRisk;
	protected Set<String> entryClses; // 入口类集合
	protected Set<String> conflictJarClses; // 冲突jar类集合
	protected Set<String> usedJarClses; // 使用的jar类集合
	protected Set<String> riskMthds; // 风险方法集合
	protected Set<String> rchMthds;
	protected IGraph graph;
	protected Map<String, Integer> mthd2branch;
	protected Set<String> parentDepJarClasses;

	public JRiskCgTf() {
		super();
		entryClses = DepJars.i().getHostDepJar().getAllCls(true);
	}

	public JRiskCgTf(DepJarJRisk depJarJRisk) {
		super();
		// this.depJarJRisk = depJarJRisk;
		entryClses = depJarJRisk.getEntryDepJar().getAllCls(true);
		conflictJarClses = depJarJRisk.getConflictDepJar().getAllCls(true);
		riskMthds = depJarJRisk.getThrownMthds();
		rchMthds = new HashSet<String>();

	}

	/**
	 * 重构函数
	 * 
	 * @param depJarJRisk
	 * @param thrownMethods
	 */
	public JRiskCgTf(DepJarJRisk depJarJRisk, Set<String> thrownMethods) {
		super();
		// this.depJarJRisk = depJarJRisk;
		entryClses = depJarJRisk.getEntryDepJar().getAllCls(true);
		conflictJarClses = depJarJRisk.getConflictDepJar().getAllCls(true);
		riskMthds = thrownMethods;
		rchMthds = new HashSet<String>();
	}

	public JRiskCgTf(DepJarJRisk depJarJRisk, Set<DepJar> allParentDepJar, Set<String> thrownMethods) {
		super();
		// this.depJarJRisk = depJarJRisk;
		entryClses = depJarJRisk.getEntryDepJar().getAllCls(true);
		conflictJarClses = depJarJRisk.getConflictDepJar().getAllCls(true);
		riskMthds = thrownMethods;
		rchMthds = new HashSet<String>();
		usedJarClses = new HashSet<String>();
		for (DepJar parentDepJar : allParentDepJar) {
			usedJarClses.addAll(parentDepJar.getAllCls(true));
		}
	}

	public JRiskCgTf(Set<String> thrownMethods) {
		super();
		// this.depJarJRisk = depJarJRisk;
		entryClses = DepJars.i().getHostDepJar().getAllCls(true);
		riskMthds = thrownMethods;
	}

	public JRiskCgTf(Set<DepJar> parentDepJars, Set<String> thrownMethods) {
		super();
		parentDepJarClasses = new HashSet<String>();
		// this.depJarJRisk = depJarJRisk;
		for (DepJar depJar : parentDepJars) {
			parentDepJarClasses.addAll(depJar.getAllCls(true));
			System.out.println("has add depJar all classes" + depJar.toString());
		}
		riskMthds = thrownMethods;
	}

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {

		MavenUtil.i().getLog().info("JRiskCgTf start..");
		Map<String, String> cgMap = new HashMap<String, String>();

		cgMap.put("enabled", "true");
		cgMap.put("apponly", "true");
		cgMap.put("all-reachable", "true");

		initMthd2branch();

		CHATransformer.v().transform("wjtp", cgMap);

		formGraph();

		MavenUtil.i().getLog().info("JRiskCgTf end..");
	}

	protected abstract void initMthd2branch();

	protected abstract void formGraph();

	protected boolean isHostClass(String clsName) {
		return entryClses.contains(clsName) && !LibCopyInfo.isLibCopy(MavenUtil.i().getProjectCor(), clsName);
	}

//	protected boolean isHostClassNoSameJar(String clsName) {
//		return !LibCopyInfo.isLibCopy(MavenUtil.i().getProjectCor(), clsName);
//	}

	public IGraph getGraph() {
		return graph;
	}

}