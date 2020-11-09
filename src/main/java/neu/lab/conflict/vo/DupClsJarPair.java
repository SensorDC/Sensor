package neu.lab.conflict.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.container.SemantemeMethods;
import neu.lab.conflict.graph.Graph4path;
import neu.lab.conflict.graph.GraphForMethodOutPath;
import neu.lab.conflict.graph.IGraph;
import neu.lab.conflict.graph.Node4path;
import neu.lab.conflict.soot.SootJRiskCg;
import neu.lab.conflict.soot.tf.JRiskMethodOutPathCgTf;
import neu.lab.conflict.soot.tf.JRiskMthdPathCgTf;
import neu.lab.conflict.util.SootUtil;
import neu.lab.conflict.vo.ClassVO;
import neu.lab.conflict.vo.DepJar;
import neu.lab.conflict.vo.MethodVO;

/**
 * two jar that have different name and same class.
 * 
 * @author asus
 *
 */
public class DupClsJarPair {
	private DepJar jar1;
	private DepJar jar2;
	private Set<String> clsSigs;
	private Set<String> thrownMethods;

	public void addThrownMethods(String method) {
		if (thrownMethods == null) {
			thrownMethods = new HashSet<String>();
		}
		this.thrownMethods.add(method);
	}

	public DupClsJarPair(DepJar jarA, DepJar jarB) {
		jar1 = jarA;
		jar2 = jarB;
		clsSigs = new HashSet<String>();
	}

	public boolean isInDupCls(String rhcedMthd) {
		return clsSigs.contains(SootUtil.mthdSig2cls(rhcedMthd));
	}

	public void addClass(String clsSig) {
		clsSigs.add(clsSig);
	}

	public boolean isSelf(DepJar jarA, DepJar jarB) {
		return (jar1.equals(jarA) && jar2.equals(jarB)) || (jar1.equals(jarB) && jar2.equals(jarA));
	}

	public DepJar getJar1() {
		return jar1;
	}

	public DepJar getJar2() {
		return jar2;
	}

	public String getRiskString() {
		StringBuilder sb = new StringBuilder("classConflict:");
		sb.append("<" + jar1.toString() + ">");
		sb.append("<" + jar2.toString() + ">\n");
		sb.append(getJarString(jar1, jar2));
		sb.append(getJarString(jar2, jar1));
		return sb.toString();
	}

	private String getJarString(DepJar total, DepJar some) {
		StringBuilder sb = new StringBuilder();
		List<String> onlyMthds = getOnlyMethod(total, some);
		sb.append("   methods that only exist in " + total.getValidDepPath() + "\n");
		if (onlyMthds.size() > 0) {
			for (String onlyMthd : onlyMthds) {
				sb.append(onlyMthd + "\n");
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "DupClsJarPair [jar1=" + jar1 + ", jar2=" + jar2 + ", clsSigs=" + clsSigs
				+ ", semantemeMethodForDifferences=" + semantemeMethodForDifferences + "]";
	}

	private List<String> getOnlyMethod(DepJar total, DepJar some) {
		List<String> onlyMthds = new ArrayList<String>();
		for (String clsSig : clsSigs) {
			ClassVO classVO = total.getClassVO(clsSig);
			if (classVO != null) {
				for (MethodVO mthd : classVO.getMethods()) {
					if (!some.getClassVO(clsSig).hasMethod(mthd.getMthdSig()))
						onlyMthds.add(mthd.getMthdSig());
				}
			}
		}
		return onlyMthds;
	}

	public String getCommonMethodsString() {
		StringBuilder sb = new StringBuilder("\n");
		DepJar depJar;
		if (jar1.isSelected()) {
			depJar = jar2.getUsedDepJar();
			sb.append("<" + jar1.toString() + ">  used:" + jar1.isSelected() + " scope:" + jar1.getScope() + "\n");
			sb.append("<" + jar2.toString() + ">  used:" + jar2.isSelected() + " scope:" + jar2.getScope() + "\n");
			if (!depJar.isSelf(jar2)) {
				sb.append("<" + depJar.toString() + ">  used:" + depJar.isSelected() + " scope:" + jar2.getScope()
						+ "\n");
			}
		} else if (jar2.isSelected()) {
			depJar = jar1.getUsedDepJar();
			sb.append("<" + jar1.toString() + ">  used:" + jar1.isSelected() + " scope:" + jar1.getScope() + "\n");
			sb.append("<" + jar2.toString() + ">  used:" + jar2.isSelected() + " scope:" + jar2.getScope() + "\n");
			if (!depJar.isSelf(jar1)) {
				sb.append("<" + depJar.toString() + ">  used:" + depJar.isSelected() + " scope:" + jar1.getScope()
						+ "\n");
			}
		}
		return sb.toString();
	}

	public Set<String> getCommonMethods() {
		Set<String> commonMethods = new HashSet<String>();
		for (String clsSig : clsSigs) {
			ClassVO classVO = jar1.getClassVO(clsSig);
			if (classVO != null) {
				for (MethodVO mthd : classVO.getMethods()) {
					if (jar2.getClassVO(clsSig).hasMethod(mthd.getMthdSig()))
						commonMethods.add(mthd.getMthdSig());
				}
			}
		}
		return commonMethods;
	}

	Map<String, List<Integer>> semantemeMethodForDifferences; // 语义方法的差异集合

	public Map<String, List<Integer>> getSemantemeMethodForDifferences() {
		return semantemeMethodForDifferences;
	}

	public Graph4path getMethoPathGraphForSemanteme() {
		Set<String> commonMethods = getCommonMethods();
//		System.out.println("commonMethods size"+ commonMethods.size());
		Set<String> riskMethods = new HashSet<String>();
		if (commonMethods.size() > 0) {
			GraphForMethodOutPath depJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i().getGraph(jar1,
					new JRiskMethodOutPathCgTf(commonMethods));

			GraphForMethodOutPath usedDepJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i()
					.getGraph(jar2, new JRiskMethodOutPathCgTf(commonMethods));

			SemantemeMethods semantemeMethods = new SemantemeMethods(depJarGraphForMethodOutPath.getSemantemeMethods(),
					usedDepJarGraphForMethodOutPath.getSemantemeMethods());

			semantemeMethods.CalculationDifference(); // 计算差异

			semantemeMethodForDifferences = semantemeMethods.getSemantemeMethodForReturn();
//			System.out.println("semantemeMethodForDifferences size"+ semantemeMethodForDifferences.size());
			riskMethods = semantemeMethods.sortMap(100);
			if (riskMethods == null) {
				return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
			}
			depJarGraphForMethodOutPath = null;
			usedDepJarGraphForMethodOutPath = null;

			if (riskMethods.size() > 0) {
				IGraph iGraph = SootJRiskCg.i().getGraph(DepJars.i().getUsedJarPaths().toArray(new String[0]),
						new JRiskMthdPathCgTf(riskMethods));
				if (iGraph != null) {
					return (Graph4path) iGraph;
				} else {
					return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
				}
			} else {
				return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
			}
		} else {
			return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
		}
	}
// 
//	public ClsDupJarPairRisk getPairRisk(DepJarNRisks jarCgs) {
//		return new ClsDupJarPairRisk(this, jarCgs.getDepJarCg(getJar1()), jarCgs.getDepJarCg(getJar2()));
//	}

	// @Override
	// public int hashCode() {
	// return jar1.hashCode() + jar2.hashCode();
	// }
	//
	// @Override
	// public boolean equals(Object obj) {
	// if (obj instanceof JarCmp) {
	// JarCmp other = (JarCmp) obj;
	// return (jar1.equals(other.getJar1()) && jar2.equals(other.getJar2()))
	// || (jar1.equals(other.getJar2()) && jar2.equals(other.getJar1()));
	// }
	// return false;
	// }
}
