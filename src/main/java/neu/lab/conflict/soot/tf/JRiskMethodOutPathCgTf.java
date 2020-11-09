package neu.lab.conflict.soot.tf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.graph.GraphForMethodOutPath;
import neu.lab.conflict.risk.jar.DepJarJRisk;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.vo.DepJar;
import neu.lab.conflict.vo.SemantemeMethod;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class JRiskMethodOutPathCgTf extends JRiskCgTf{

	public JRiskMethodOutPathCgTf(DepJarJRisk depJarJRisk) {
		super(depJarJRisk);
	}
	public JRiskMethodOutPathCgTf(Set<String> entryMethods) {
		super(entryMethods);
	}
	public JRiskMethodOutPathCgTf(Set<DepJar> parentDepJars, Set<String> entryMethods) {
		super(parentDepJars, entryMethods);
	}
	@Override
	protected void formGraph() {
		if (graph == null) {
			MavenUtil.i().getLog().info("start form graph...");
			
			Map<String, SemantemeMethod> semantemeMethods = new HashMap<String, SemantemeMethod>();
			
			SemantemeMethod semantemeMethod;
			
			Iterator<SootMethod> sootMethodIterator = Scene.v().getMethodNumberer().iterator();
			
			while (sootMethodIterator.hasNext()) {
				SootMethod sootMethod = sootMethodIterator.next();
				String methodName = sootMethod.getSignature();
				if (riskMthds.contains(methodName) && sootMethod.hasActiveBody()) {
					semantemeMethod = new SemantemeMethod(methodName);
					semantemeMethod.setUnits(sootMethod.getActiveBody().getAllUnitBoxes());
					semantemeMethod.setValues(sootMethod.getActiveBody().getUseAndDefBoxes());
					semantemeMethods.put(methodName, semantemeMethod);
				}
			}
			
			graph = new GraphForMethodOutPath(semantemeMethods);
			
			MavenUtil.i().getLog().info("end form graph.");
		}
	}

	/**
	 * 保留来自冲突jar父类的路径，去掉来自usedDepJar的路径
	 * @param cg
	 */
	public void reservedFromConflictParentJarMethod(CallGraph cg) {
		
		Set<String> reservedMethod = new HashSet<String>();
		
		Iterator<Edge> ite = cg.iterator();
		
		while (ite.hasNext()) {
			Edge edge = ite.next();
			if (edge.src().isJavaLibraryMethod() || !edge.src().isConcrete()) {
			}
			else {
			String srcClassName = edge.src().getDeclaringClass().getName();
			String tgtMethodName = edge.tgt().getSignature();
			if (parentDepJarClasses.contains(srcClassName) && riskMthds.contains(tgtMethodName)) {
				reservedMethod.add(tgtMethodName);
			}
			else {
				String srcMethodName = edge.src().getSignature();
				if (riskMthds.contains(srcMethodName) && !reservedMethod.contains(srcMethodName)) {
					reservedMethod.add(srcMethodName);
				}
			}
		}
		}
		riskMthds = null;
		riskMthds = reservedMethod;
	}
	@Override
	protected void initMthd2branch() {
		
	}

}
