package neu.lab.conflict.risk.jar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.GlobalVar;
import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.container.SemantemeMethods;
import neu.lab.conflict.graph.Graph4distance;
import neu.lab.conflict.graph.Graph4path;
import neu.lab.conflict.graph.GraphForMethodOutPath;
import neu.lab.conflict.graph.IBook;
import neu.lab.conflict.graph.IGraph;
import neu.lab.conflict.graph.IRecord;
import neu.lab.conflict.graph.Node4distance;
import neu.lab.conflict.graph.Node4path;
import neu.lab.conflict.graph.Record4distance;
import neu.lab.conflict.graph.Record4path;
import neu.lab.conflict.soot.SootJRiskCg;
import neu.lab.conflict.soot.SootRiskMthdFilter;
import neu.lab.conflict.soot.SootRiskMthdFilter2;
import neu.lab.conflict.soot.tf.JRiskDistanceCgTf;
import neu.lab.conflict.soot.tf.JRiskMethodOutPathCgTf;
import neu.lab.conflict.soot.tf.JRiskMthdPathCgTf;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.vo.DepJar;
import neu.lab.conflict.vo.MethodCall;

/**
 * 依赖风险jar
 * 
 * @author wangchao
 *
 */
public class DepJarJRisk {
	private DepJar depJar; // 依赖jar
	private DepJar usedDepJar; // 依赖jar
	private Set<String> thrownMthds; // 抛弃的方法
	private Set<String> semantemeRiskMethods; // 语义风险方法集合
	// private Set<String> rchedMthds;
	private Graph4distance graph4distance; // 图
//	private Map<String, IBook> books; // book记录用

	/*
	 * 构造函数
	 */
	public DepJarJRisk(DepJar depJar, DepJar usedDepJar) {
		this.depJar = depJar;
		this.usedDepJar = usedDepJar;
		// calculate thrownMthd
		// calculate call-graph
	}

	/*
	 * 得到版本
	 */
	public String getVersion() {
		return depJar.getVersion();
	}

	public DepJar getUsedDepJar() {
		return usedDepJar;
	}

	public void setUsedDepJar(DepJar usedDepJar) {
		this.usedDepJar = usedDepJar;
	}

	public Set<String> getThrownClasses() {
		Set<String> thrownClasses = usedDepJar.getRiskClasses(depJar.getAllCls(false));
		return thrownClasses;
	}

	/**
	 * 得到抛弃的方法
	 * 
	 * @return
	 * 
	 */
	public Set<String> getThrownMthds() {
		// e.g.:"<neu.lab.plug.testcase.homemade.host.prob.ProbBottom: void m()>"
		thrownMthds = usedDepJar.getRiskMthds(depJar.getallMethods());
		MavenUtil.i().getLog().info("riskMethod size before filter: " + thrownMthds.size());
		if (thrownMthds.size() > 0)
			new SootRiskMthdFilter().filterRiskMthds(thrownMthds);
		MavenUtil.i().getLog().info("riskMethod size after filter1: " + thrownMthds.size());
		if (thrownMthds.size() > 0)
			new SootRiskMthdFilter2().filterRiskMthds(this, thrownMthds);
		MavenUtil.i().getLog().info("riskMethod size after filter2: " + thrownMthds.size());
		return thrownMthds;
	}

	/**
	 * 用传入的depJar去得到抛弃的方法
	 * 
	 * @param depJar
	 * @return
	 */
	public Set<String> getThrownMthds(DepJar enterDepJar) {
		thrownMthds = usedDepJar.getRiskMthds(depJar.getallMethods());
		MavenUtil.i().getLog().info("riskMethod size before filter: " + thrownMthds.size());
		if (thrownMthds.size() > 0)
			new SootRiskMthdFilter().filterRiskMthds(thrownMthds, enterDepJar);
		MavenUtil.i().getLog().info("riskMethod size after filter1: " + thrownMthds.size());
		if (thrownMthds.size() > 0)
			new SootRiskMthdFilter2().filterRiskMthds(this, thrownMthds);
		MavenUtil.i().getLog().info("riskMethod size after filter2: " + thrownMthds.size());
		return thrownMthds;
	}

	/**
	 * 语义冲突得到相关方法
	 * 
	 * @return
	 */
	public Set<String> getSemantemeRiskMethods() {
		semantemeRiskMethods = usedDepJar.getCommonMethods(depJar.getallMethods());
		MavenUtil.i().getLog().info("semantemeRiskMethods size for common methods: " + semantemeRiskMethods.size());
		return semantemeRiskMethods;
	}

	public Set<String> getMethodBottom(Map<String, IBook> books) {
		Set<String> bottomMethods = new HashSet<String>();
		for (IBook book : books.values()) {
			for (IRecord iRecord : book.getRecords()) {
				Record4distance record = (Record4distance) iRecord;
				bottomMethods.add(record.getName());
			}
		}
		return bottomMethods;
	}

	public Set<String> getMethodBottomForPath(Map<String, IBook> books) {
		Set<String> bottomMethods = new HashSet<String>();
		for (IBook book : books.values()) {
			for (IRecord iRecord : book.getRecords()) {
				Record4path record = (Record4path) iRecord;
				bottomMethods.add(record.getName());
			}
		}
		return bottomMethods;
	}

	public Collection<String> getPrcDirPaths() throws Exception {
		List<String> classpaths;
		if (GlobalVar.useAllJar) { // default:true
			classpaths = depJar.getRepalceClassPath();
		} else {
			MavenUtil.i().getLog().info("not add all jar to process");
			classpaths = new ArrayList<String>();
			// keep first is self
			classpaths.addAll(this.depJar.getJarFilePaths(true));
			classpaths.addAll(this.depJar.getFatherJarClassPaths(false));

		}
		return classpaths;

	}

	public DepJar getEntryDepJar() {
		return DepJars.i().getHostDepJar();
	}

	public DepJar getConflictDepJar() {
		return depJar;
	}

	/**
	 * 得到距离图
	 * 
	 * @return
	 */
	public Graph4distance getGraph4distance() {
		if (graph4distance == null) {
			Set<String> thrownmethods = getThrownMthds();
			if (thrownmethods.size() > 0) {
				IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskDistanceCgTf(this, thrownmethods));
				if (iGraph != null) {
					graph4distance = (Graph4distance) iGraph;
				} else {
					graph4distance = new Graph4distance(new HashMap<String, Node4distance>(),
							new ArrayList<MethodCall>());
				}
			} else {
				graph4distance = new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
			}
		}
		return graph4distance;
	}

	/**
	 * 得到距离图 多态
	 * 
	 * @return
	 */
	public Graph4distance getGraph4distance(DepJar useDepJar) {
		Set<String> thrownmethods = getThrownMthds(useDepJar);
		if (thrownmethods.size() > 0) {
			IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskDistanceCgTf(this, thrownmethods));
			if (iGraph != null) {
				return (Graph4distance) iGraph;
			} else {
				return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
			}
		} else {
			return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
		}
	}

	public Graph4path getGraph4mthdPath() {
		Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
		if (semantemeRiskMethods.size() > 0) {
			IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskMthdPathCgTf(this, semantemeRiskMethods));
			if (iGraph != null) {
				return (Graph4path) iGraph;
			} else {
				return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
			}
		} else {
			return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
		}
//		if (getThrownMthds().size() > 0) {
//			IGraph iGraph = SootJRiskCg.i().getGraph4branch(this,new JRiskMthdPathCgTf(this));
//			if(iGraph!=null)
//				return (Graph4path)iGraph;
//		}
//		return new Graph4path(new HashMap<String, Node4path>(), new ArrayList<MethodCall>());
	}

	Map<String, List<Integer>> semantemeMethodForDifferences; // 语义方法的差异集合

	public Map<String, List<Integer>> getSemantemeMethodForDifferences() {
		return semantemeMethodForDifferences;
	}

	public void getAllSemantemeMethodForDifferences() {
		Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
//		Set<String> riskMethods = new HashSet<String>();
		if (semantemeRiskMethods.size() > 0) {
			GraphForMethodOutPath depJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i().getGraph(depJar,
					new JRiskMethodOutPathCgTf(semantemeRiskMethods));

			GraphForMethodOutPath usedDepJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i()
					.getGraph(usedDepJar, new JRiskMethodOutPathCgTf(semantemeRiskMethods));

			SemantemeMethods semantemeMethods = new SemantemeMethods(depJarGraphForMethodOutPath.getSemantemeMethods(),
					usedDepJarGraphForMethodOutPath.getSemantemeMethods());

			semantemeMethods.CalculationDifference(); // 计算差异

			semantemeMethodForDifferences = semantemeMethods.getSemantemeMethodForReturn();
		}
	}

	// 得到语义冲突的路径图
	public Graph4distance getMethodPathGraphForSemanteme() {

		Set<String> semantemeRiskMethods = getSemantemeRiskMethods();
		Set<String> riskMethods = new HashSet<String>();
		if (semantemeRiskMethods.size() > 0) {
			GraphForMethodOutPath depJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i().getGraph(depJar,
					new JRiskMethodOutPathCgTf(semantemeRiskMethods));

			GraphForMethodOutPath usedDepJarGraphForMethodOutPath = (GraphForMethodOutPath) SootJRiskCg.i()
					.getGraph(usedDepJar, new JRiskMethodOutPathCgTf(semantemeRiskMethods));

			SemantemeMethods semantemeMethods = new SemantemeMethods(depJarGraphForMethodOutPath.getSemantemeMethods(),
					usedDepJarGraphForMethodOutPath.getSemantemeMethods());

			semantemeMethods.CalculationDifference(); // 计算差异

			semantemeMethodForDifferences = semantemeMethods.getSemantemeMethodForReturn();

			riskMethods = semantemeMethods.sortMap(100);

			depJarGraphForMethodOutPath = null;
			usedDepJarGraphForMethodOutPath = null;

			if (riskMethods.size() > 0) {
				IGraph iGraph = SootJRiskCg.i().getGraph(this, new JRiskDistanceCgTf(this, riskMethods));
				if (iGraph != null) {
					return (Graph4distance) iGraph;
				} else {
					return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
				}
			} else {
				return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
			}
		} else {
			return new Graph4distance(new HashMap<String, Node4distance>(), new ArrayList<MethodCall>());
		}
	}
//	private Map<String, IBook> getBooks4distance() {
//		if (this.books == null) {
//			if (getThrownMthds().size() > 0) {
//				// calculate distance
//
//				books = new Dog(getGraph4distance()).findRlt(getGraph4distance().getHostNds(), Conf.DOG_DEP_FOR_DIS,
//						Dog.Strategy.NOT_RESET_BOOK);
//
////				GraphPrinter.printGraph(graph4branch, "d:\\graph_distance.txt",getGraph4branch().getHostNds());
//			} else {
//				books = new HashMap<String, IBook>();
//			}
//		}
//		return books;
//	}

	@Override
	public String toString() {
		return depJar.toString() + " in conflict " + usedDepJar.toString();
	}

}
