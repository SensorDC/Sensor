package neu.lab.conflict.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import neu.lab.conflict.container.AllCls;
import neu.lab.conflict.container.AllRefedCls;
import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.graph.Dog;
import neu.lab.conflict.graph.Graph4distance;
import neu.lab.conflict.graph.IBook;
import neu.lab.conflict.graph.Dog.Strategy;
import neu.lab.conflict.risk.jar.DepJarJRisk;
import neu.lab.conflict.util.Conf;
import neu.lab.conflict.util.MavenUtil;

public class Conflict {
	private String groupId;
	private String artifactId;
	//一个conflict中存在n个depJar n>1 一个depJar对应1:n个node
	private Set<NodeAdapter> nodeAdapters;
	private Set<DepJar> depJars;
	private DepJar usedDepJar;
	// private ConflictRiskAna riskAna;
	private List<DepJarJRisk> jarRisks; // 依赖风险jar集合

	
	public Conflict(String groupId, String artifactId) {
		nodeAdapters = new HashSet<NodeAdapter>();
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	/**
	 * 得到使用的DepJar
	 * 
	 * @return
	 */
	public DepJar getUsedDepJar() {
		if (null == usedDepJar) {
			for (DepJar depJar : depJars) {
				if (depJar.isSelected()) {
					usedDepJar = depJar;
				}
			}
		}
		return usedDepJar;

	}

	/**
	 * 设置usedDepJar
	 */
	public void setUsedDepJar(DepJar depJar) {
		usedDepJar = depJar;
	}

	/**
	 * 得到除了被选中的jar以外的其他被依赖的jar包
	 * 
	 * @return
	 */
	public Set<DepJar> getOtherDepJarExceptSelect() {
		Set<DepJar> usedDepJars = new HashSet<DepJar>();
		for (DepJar depJar : depJars) {
			if (!depJar.isSelected()) {
				usedDepJars.add(depJar);
			}
		}
		return usedDepJars;
	}

	public void addNodeAdapter(NodeAdapter nodeAdapter) {
		nodeAdapters.add(nodeAdapter);
	}

	/**
	 * 同一个构件
	 * 
	 * @param groupId2
	 * @param artifactId2
	 * @return
	 */
	public boolean sameArtifact(String groupId2, String artifactId2) {
		return groupId.equals(groupId2) && artifactId.equals(artifactId2);
	}

	public Set<DepJar> getDepJars() {
		if (depJars == null) {
			depJars = new HashSet<DepJar>();
			for (NodeAdapter nodeAdapter : nodeAdapters) {
				depJars.add(nodeAdapter.getDepJar());
			}
		}
		return depJars;
	}

	public Set<NodeAdapter> getNodeAdapters() {
		return this.nodeAdapters;
	}

	public boolean isConflict() {
		return getDepJars().size() > 1;
	}

	@Override
	public String toString() {
		String str = groupId + ":" + artifactId + " conflict version:";
		for (DepJar depJar : depJars) {
			str = str + depJar.getVersion() + ":" + depJar.getClassifier() + "-";
		}
		str = str + "---used jar:" + getUsedDepJar().getVersion() + ":" + getUsedDepJar().getClassifier();
		return str;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getSig() {
		return getGroupId() + ":" + getArtifactId();
	}

	/**
	 * @return first version is the used version 第一个版本是正在使用的版本
	 */
	public List<String> getVersions() {
		List<String> versions = new ArrayList<String>();
		versions.add(getUsedDepJar().getVersion());
		for (DepJar depJar : depJars) {
			String version = depJar.getVersion();
			if (!versions.contains(version)) {
//				versions.add("/" + version);
				versions.add(version);
			}
		}
		return versions;
	}
	


	public List<DepJarJRisk> getJarRisks() {
		if(jarRisks == null) {
			jarRisks = new ArrayList<DepJarJRisk>();
			getUsedDepJar();
			for (DepJar depJar : getDepJars()) {
				if (usedDepJar.isSelf(depJar)) {
				}else {
					jarRisks.add(new DepJarJRisk(depJar, usedDepJar));
				}
				
			}
		}
			return jarRisks;
	}


	/**
	 * 可以细分等级1, 2，3，4 method:得到风险等级 name:wangchao time:2018-9-29 16:27:21
	 */
	public Map<Integer, String> getRiskLevel() {
		boolean isUsedDepJar = false; // 记录参与运算的depJar是不是本项目被使用的usedDepJar
		DepJar usedDepJar = getUsedDepJar(); // 记录usedJar
		Set<DepJar> depJars = getDepJars();
		Set<String> usedDepJarSet = new HashSet<String>(); // 被使用的usedDepJar风险方法集合
		Map<String, Map<String, Set<String>>> isNotUsedDepJarMap = new HashMap<String, Map<String, Set<String>>>(); // 未被使用的usedDepJars风险方法集合
		Map<String, Set<String>> nowUsedDepJarMethod = null; // 当前DepJar风险方法集合
		Set<String> bottomMethods = null;
		Map<Integer, String> result = new HashMap<Integer, String>();
		for (DepJar depJar : depJars) {
			nowUsedDepJarMethod = new HashMap<String, Set<String>>();
			// 初始化
			this.setUsedDepJar(depJar);
			AllCls.init(DepJars.i(), depJar);
			AllRefedCls.init(depJar);

			for (DepJarJRisk depJarJRisk : getJarRisks()) {
				bottomMethods = new HashSet<String>();
				if (depJarJRisk.getConflictDepJar() != getUsedDepJar()) {
					isUsedDepJar = false;
					if (depJar.isSelf(usedDepJar)) {
						isUsedDepJar = true;
					}

					Graph4distance distanceGraph = depJarJRisk.getGraph4distance(depJar);

					if (distanceGraph.getAllNode().isEmpty()) {
						MavenUtil.i().getLog().info("distanceGraph is empty");
						nowUsedDepJarMethod.put(depJarJRisk.getConflictDepJar().toString(), bottomMethods);
						break;
					}

					Map<String, IBook> distanceBooks = new Dog(distanceGraph).findRlt(distanceGraph.getHostNodes(),
							Conf.DOG_DEP_FOR_DIS, Strategy.NOT_RESET_BOOK);
					bottomMethods = depJarJRisk.getMethodBottom(distanceBooks);
					if (isUsedDepJar) {
						usedDepJarSet.addAll(bottomMethods);
					} else {
						nowUsedDepJarMethod.put(depJarJRisk.getConflictDepJar().toString(), bottomMethods);
					}
				}

			}
			if (!nowUsedDepJarMethod.isEmpty()) {
				isNotUsedDepJarMap.put(getUsedDepJar().toString(), nowUsedDepJarMethod);
			}
		}
		/*
		 * 使用的集合是不是为空 不使用的集合是不是为空
		 */
		boolean useSet = false;
		boolean noUseSet = false;
		int isNotUsedDepJarMapSize = isNotUsedDepJarMap.entrySet().size();
		int noUseSetNum = 0;
		String jarSig = null;
		if (usedDepJarSet.isEmpty()) {
			useSet = true;
		}
		for (Entry<String, Map<String, Set<String>>> entrys : isNotUsedDepJarMap.entrySet()) {
			Map<String, Set<String>> mapEntry = entrys.getValue();
			boolean isnot = true;
			for (Entry<String, Set<String>> entry : mapEntry.entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				} else {
					isnot = false;
				}
			}
			if (isnot) {
				noUseSetNum++;
				jarSig = entrys.getKey();
			}
		}
		if (noUseSetNum > 0) {
			noUseSet = true;
		}
		
		if (useSet) {
			jarSig = usedDepJar.toString();
		}
		int riskLevel = 0;
		/*
		 * 风险1：项目使用的jar包和不使用的jar包的风险方法集合都为空 风险2：项目使用的jar包风险方法集合为空，不使用的jar包都有风险方法
		 * 风险3：项目使用的jar包风险方法集合不为空，不使用的jar包中有风险方法集合为空的jar包
		 * 风险4：项目使用的jar包和不使用的jar包的风险方法集合都为空
		 */
		if (useSet && noUseSet && noUseSetNum == isNotUsedDepJarMapSize) {
			riskLevel = 1;
		} else if (useSet) {
			riskLevel = 2;
		} else if (!useSet && noUseSet) {
			riskLevel = 3;
		} else if (!useSet && !noUseSet) {
			riskLevel = 4;
		}
		// 重置
		this.setUsedDepJar(usedDepJar);
		AllCls.init(DepJars.i(), usedDepJar);
		AllRefedCls.init(usedDepJar);
		result.put(riskLevel, jarSig);
		return result;
	}

	/**
	 * 得到Conflict的等级 分为1-3两个等级 记录3等级Conflict（1等级的Conflict忽略不计算，可大幅减少运算时间）
	 * 继续计算3等级，分为3-4两个等级
	 * 
	 * @return
	 */
//	public Set<String> getConflictLevel() {
//		Set<String> usedRiskMethods = new HashSet<String>(); // 被使用的usedDepJar风险方法集合
//		for (DepJarJRisk depJarJRisk : getJarRisks()) {
//			Graph4distance distanceGraph = depJarJRisk.getGraph4distance();
//			Map<String, IBook> distanceBooks = new Dog(distanceGraph).findRlt(distanceGraph.getHostNds(),
//					Conf.DOG_DEP_FOR_DIS, Strategy.NOT_RESET_BOOK);
//			Set<String> bottomMethods = depJarJRisk.getMethodBottom(distanceBooks);
//			usedRiskMethods.addAll(bottomMethods);
//		}
//		return usedRiskMethods;
//	}
	public Set<String> getConflictLevel() {
		Set<String> usedRiskMethods = new HashSet<String>(); // 被使用的usedDepJar风险方法集合
		for (DepJarJRisk depJarJRisk : getJarRisks()) {
			usedRiskMethods.addAll(depJarJRisk.getThrownMthds());
//			 Graph4path pathGraph = depJarJRisk.getMethodPathGraphForSemanteme();
//			Map<String, IBook> pathBooks = new Dog(pathGraph).findRlt(pathGraph.getHostNodes(),
//					Conf.DOG_DEP_FOR_DIS, Strategy.NOT_RESET_BOOK);
//			Set<String> bottomMethods = depJarJRisk.getMethodBottomForPath(pathBooks);
//			usedRiskMethods.addAll(bottomMethods);
		}
		return usedRiskMethods;
	}
}
