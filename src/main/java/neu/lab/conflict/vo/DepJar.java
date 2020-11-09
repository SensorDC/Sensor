package neu.lab.conflict.vo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.container.AllCls;
import neu.lab.conflict.container.AllRefedCls;
import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.container.NodeAdapters;
import neu.lab.conflict.soot.JarAna;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.util.SootUtil;

/**
 * @author wangchao
 *
 */
public class DepJar {
	private String groupId;
	private String artifactId;// artifactId
	private String version;// version
	private String classifier; // 附属构件
	private List<String> jarFilePaths;// host project may have multiple source.
	private Map<String, ClassVO> allClass;// all class in jar
	private Set<NodeAdapter> nodeAdapters;// all
	private Set<String> allMethods;

	public DepJar(String groupId, String artifactId, String version, String classifier, List<String> jarFilePaths) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.classifier = classifier;
		this.jarFilePaths = jarFilePaths;
	}

	/**
	 * get jar may have risk thinking same class in different dependency,selected
	 * jar may have risk; Not thinking same class in different dependency,selected
	 * jar is safe jar可能存在不同依赖项中考虑同一类的风险，所选的jar可能存在风险；如果不以不同的依赖关系考虑相同的类，则选择的jar是安全的。
	 * 
	 * @return
	 */
	public boolean isRisk() {
		return !this.isSelected();
	}

	/**
	 * all class in jar中是不是包含某一class
	 *
	 */
	public boolean containClass(String classSig) {
		return this.getAllClass().containsKey(classSig);
	}

//	public Element getRchNumEle() {
//		Element nodeEle = new DefaultElement("version");
//		nodeEle.addAttribute("versionId", getVersion());
//		nodeEle.addAttribute("loaded", "" + isSelected());
//		for (NodeAdapter node : this.getNodeAdapters()) {
//			nodeEle.add(node.getPathElement());
//		}
//		return nodeEle;
//	}
	public DepJar getUsedDepJar() {
		for (DepJar depJar : DepJars.i().getAllDepJar()) {
			if (isSameLib(depJar) && depJar.isSelected()) {
				return depJar;
			}
		}
		return this;
	}
//	public Element getClsConflictEle(int num) {
//		Element nodeEle = new DefaultElement("jar-" + num);
//		nodeEle.addAttribute("id", toString());
//		for (NodeAdapter node : this.getNodeAdapters()) {
//			nodeEle.add(node.getPathElement());
//		}
//		return nodeEle;
//	}

	public Set<NodeAdapter> getNodeAdapters() {
		if (nodeAdapters == null)
			nodeAdapters = NodeAdapters.i().getNodeAdapters(this);
		return nodeAdapters;
	}

	public String getScope() {
		String scope = null;
		for (NodeAdapter node : nodeAdapters) {
			scope = node.getScope();
			if (scope != null) {
				break;
			}
		}
		return scope;
	}

	public String getAllDepPath() {
		StringBuilder sb = new StringBuilder(toString() + ":");
		for (NodeAdapter node : getNodeAdapters()) {
			sb.append("  [");
			sb.append(node.getWholePath());
			sb.append("]");
		}
		return sb.toString();

	}

	/**
	 * @return the import path of depJar.
	 */
	public String getValidDepPath() {
		StringBuilder sb = new StringBuilder(toString() + ":");
		for (NodeAdapter node : getNodeAdapters()) {
			if (node.isNodeSelected()) {
				sb.append("  [");
				sb.append(node.getWholePath());
				sb.append("]");
			}
		}
		return sb.toString();
	}

//	public NodeAdapter getSelectedNode() {
//		for (NodeAdapter node : getNodeAdapters()) {
//			if (node.isNodeSelected()) {
//				return node;
//			}
//		}
//		return null;
//	}

//	public boolean isProvided() {
//		for (NodeAdapter node : getNodeAdapters()) {
//			if (node.isNodeSelected()) {
//				return "provided".equals(node.getScope());
//			}
//		}
//		return false;
//	}

	public boolean isSelected() {
		for (NodeAdapter nodeAdapter : getNodeAdapters()) {
			if (nodeAdapter.isNodeSelected())
				return true;
		}
		return false;
	}

	/**
	 * 得到这个jar所有类的集合
	 * 
	 * @return
	 */
	public Map<String, ClassVO> getAllClass() {
		if (allClass == null) {
			if (null == this.getJarFilePaths(true)) {
				// no file
				allClass = new HashMap<String, ClassVO>();
				MavenUtil.i().getLog().warn("can't find jarFile for:" + toString());
			} else {
				allClass = JarAna.i().deconstruct(this.getJarFilePaths(true));
				if (allClass.size() == 0) {
					MavenUtil.i().getLog().warn("get empty allClass for " + toString());
				}
				for (ClassVO clsVO : allClass.values()) {
					clsVO.setDepJar(this);
				}
			}
		}
		return allClass;
	}

	public ClassVO getClassVO(String clsSig) {
		return getAllClass().get(clsSig);
	}

	/**
	 * 得到这个jar的所有方法
	 * 
	 * @return
	 */
	public Set<String> getallMethods() {
		if (allMethods == null) {
			allMethods = new HashSet<String>();
			for (ClassVO cls : getAllClass().values()) {
				for (MethodVO mthd : cls.getMethods()) {
					allMethods.add(mthd.getMthdSig());
				}
			}
		}
		return allMethods;
	}

	public boolean containMethod(String mthd) {
		return getallMethods().contains(mthd);
	}

//	/**
//	 * 得到本depjar独有的cls
//	 * @param otherJar
//	 * @return
//	 */
//	public Set<String> getOnlyClses(DepJar otherJar) {
//		Set<String> onlyCls = new HashSet<String>();
//		Set<String> otherAll = otherJar.getAllCls(true);
//		for (String clsSig : getAllCls(true)) {
//			if (!otherAll.contains(clsSig)) {
//				onlyCls.add(clsSig);
//			}
//		}
//		return onlyCls;
//	}

//	/**
//	 * 得到本depjar独有的mthds
//	 * @param otherJar
//	 * @return
//	 */
//	public Set<String> getOnlyMthds(DepJar otherJar) {
//		Set<String> onlyMthds = new HashSet<String>();
//		for (String clsSig : getAllClass().keySet()) {
//			ClassVO otherCls = otherJar.getClassVO(clsSig);
//			if (otherCls != null) {
//				ClassVO cls = getClassVO(clsSig);
//				for (MethodVO mthd : cls.getMethods()) {
//					if (!otherCls.hasMethod(mthd.getMthdSig())) {
//						onlyMthds.add(mthd.getMthdSig());
//					}
//				}
//			}
//		}
//		return onlyMthds;
//	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DepJar) {
			return isSelf((DepJar) obj);

		}
		return false;
	}

	@Override
	public int hashCode() {
		return groupId.hashCode() * 31 * 31 + artifactId.hashCode() * 31 + version.hashCode()
				+ classifier.hashCode() * 31 * 31 * 31;
	}

	@Override
	public String toString() {
		return groupId + ":" + artifactId + ":" + version + ":" + classifier;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getClassifier() {
		return classifier;
	}

	/**
	 * 是否为同一个
	 * 
	 * @param dep
	 * @return
	 */
	public boolean isSelf(DepJar depJar) {
		return isSame(depJar.getGroupId(), depJar.getArtifactId(), depJar.getVersion(), depJar.getClassifier());
	}

	public boolean isSame(String groupId2, String artifactId2, String version2, String classifier2) {
		return groupId.equals(groupId2) && artifactId.equals(artifactId2) && version.equals(version2)
				&& classifier.equals(classifier2);
	}

	/**
	 * 没有比较版本
	 * 
	 * @param depJar
	 * @return
	 */
	public boolean isSameLib(DepJar depJar) {
		return getGroupId().equals(depJar.getGroupId()) && getArtifactId().equals(depJar.getArtifactId());
	}

	public void setallClass(Map<String, ClassVO> allClass) {
		this.allClass = allClass;
	}

//	public boolean hasallClass() {
//		return null != this.allClass;
//	}

//	/**
//	 * 得到testMthds中哪些mthds存在于本jar
//	 * @param testMthds
//	 * @return
//	 */
//	public List<String> getInnerMthds(Collection<String> testMthds) {
//		Set<String> jarMthds = getallMethods();
//		List<String> innerMthds = new ArrayList<String>();
//		for (String mthd : testMthds) {
//			if (jarMthds.contains(mthd))
//				innerMthds.add(mthd);
//		}
//		return innerMthds;
//	}

	/**
	 * note:from the view of usedJar. e.g.
	 * getReplaceJar().getRiskMthds(getRchedMthds());
	 * 
	 * @param entryMethods -depJar.getallMethods()
	 * @return
	 */
	public Set<String> getRiskMthds(Collection<String> entryMethods) {
		Set<String> riskMthds = new HashSet<String>();
		for (String testMethod : entryMethods) {
			if (!this.containMethod(testMethod) && AllRefedCls.i().contains(SootUtil.mthdSig2cls(testMethod))) {
				// don't have method,and class is used. 使用这个类，但是没有方法
				if (this.containClass(SootUtil.mthdSig2cls(testMethod))) {
					// has class.don't have method. 有这个类，没有方法
					riskMthds.add(testMethod);
				} else if (!AllCls.i().contains(SootUtil.mthdSig2cls(testMethod))) {
					// This jar don't have class,and all jar don't have class.
					// 这个jar没有这个class，所有的jar都没有
					riskMthds.add(testMethod);
				}
			}
		}
		// if (diffMthd.contains("<init>") || diffMthd.contains("<clinit>")) {
		return riskMthds;
	}

	public Set<String> getRiskMthdsNoAllClass(Collection<String> entryMethods) {
		Set<String> riskMthds = new HashSet<String>();
		for (String testMethod : entryMethods) {
			if (!this.containMethod(testMethod) && AllRefedCls.i().contains(SootUtil.mthdSig2cls(testMethod))) {
				// don't have method,and class is used. 使用这个类，但是没有方法
				riskMthds.add(testMethod);
			}
		}
		// if (diffMthd.contains("<init>") || diffMthd.contains("<clinit>")) {
		return riskMthds;
	}

	public Set<String> getRiskClasses(Collection<String> entryClasses) {
		Set<String> riskClasses = new HashSet<String>();
		for (String cls : entryClasses) {
			if (!this.containClass(cls)) {
				riskClasses.add(cls);
			}
		}
		return riskClasses;
	}

	public Set<String> getCommonMethods(Collection<String> entryMethods) {
		Set<String> commonMethods = new HashSet<String>();
		for (String testMethod : entryMethods) {
			if (this.containMethod(testMethod)) {
				commonMethods.add(testMethod);
			}
		}
		return commonMethods;
	}
//	public Set<String> getRiskMthds(Collection<String> testMthds, DepJar depJar) {
//		Set<String> riskMthds = new HashSet<String>();
//		for (String testMthd : testMthds) {
//			if (!this.containMethod(testMthd) && AllRefedCls.i(depJar).contains(SootUtil.mthdSig2cls(testMthd))) {
//				// don't have method,and class is used. 使用这个类，但是没有方法
//				if (this.containClass(SootUtil.mthdSig2cls(testMthd))) {
//					// has class.don't have method.	有这个类，没有方法
//					riskMthds.add(testMthd);
//				} else if (!AllCls.i().contains(SootUtil.mthdSig2cls(testMthd))) {
//					// This jar don't have class,and all jar don't have class.	这个jar没有这个class，所有的jar都没有
//					riskMthds.add(testMthd);
//				}
//			}
//		}
//		// if (diffMthd.contains("<init>") || diffMthd.contains("<clinit>")) {
//		return riskMthds;
//	}
//	/**
//	 * 暂时不明白用途
//	 * @param usedJar
//	 * @return
//	 */
//	public Set<String> getThrownMthds(DepJar entryDepJar) {
//		Set<String> thrownMthds = new HashSet<String>();
//		Set<String> usedMthds = new HashSet<String>();
//		for (String mthd : this.getallMethods()) {
//			if (!usedMthds.contains(mthd)) {
//				thrownMthds.add(mthd);
//			}
//		}
//		return thrownMthds;
//	}
//
//	/**
//	 * methods that this jar don't have.
//	 * 
//	 * @param testMthds
//	 * @return
//	 */
//	public Set<String> getOutMthds(Collection<String> testMthds) {
//		Set<String> jarMthds = getallMethods();
//		Set<String> outMthds = new HashSet<String>();
//		for (String mthd : testMthds) {
//			if (!jarMthds.contains(mthd))
//				outMthds.add(mthd);
//		}
//		return outMthds;
//	}
//	

	public Set<String> getAllCls(boolean useTarget) {
		return SootUtil.getJarsClasses(this.getJarFilePaths(useTarget));
	}

	/**
	 * @param useTarget: host-class-name can get from source directory(false) or
	 *        target directory(true). using source directory: advantage: get class
	 *        before maven-package disadvantage:class can't deconstruct by soot;miss
	 *        class that generated.
	 *        主机类名称可以从源目录(False)或目标目录(True)获得.使用源目录。优点：获取类之前maven包的缺点：类不能被soot解构，错过类生成。
	 *        true:[C:\Users\Flipped\.m2\repository\neu\lab\testcase\TA\1.0\TA-1.0.jar]
	 * @return
	 */
	public List<String> getJarFilePaths(boolean useTarget) {
		if (!useTarget && isHost()) {// use source directory
			// if node is inner project,will return source directory(using source directory
			// can get classes before maven-package)
			return MavenUtil.i().getSrcPaths();
		}
		return jarFilePaths;
	}

	public boolean isHost() {
		if (getNodeAdapters().size() == 1) {
			NodeAdapter node = getNodeAdapters().iterator().next();
			if (MavenUtil.i().isInner(node))
				return true;
		}
		return false;
	}

	/**
	 * use this jar replace version of used-version ,then return path of
	 * all-used-jar 使用这个jar替代了旧版本，然后返回所有的旧jar的路径
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<String> getRepalceClassPath() throws Exception {
		List<String> paths = new ArrayList<String>();
		paths.addAll(this.getJarFilePaths(true));
		boolean hasRepalce = false;
		for (DepJar usedDepJar : DepJars.i().getUsedDepJars()) {
			if (this.isSameLib(usedDepJar)) {// used depJar instead of usedDepJar.
				if (hasRepalce) {
					MavenUtil.i().getLog().warn("when cg, find multiple usedLib for " + toString()); // 有重复的使用路径
					throw new Exception("when cg, find multiple usedLib for " + toString());
				}
				hasRepalce = true;
			} else {
				for (String path : usedDepJar.getJarFilePaths(true)) {
					paths.add(path);
				}
				// paths.addAll(usedDepJar.getJarFilePaths(true));
			}
		}
		if (!hasRepalce) {
			MavenUtil.i().getLog().warn("when cg,can't find mutiple usedLib for " + toString());
			throw new Exception("when cg,can't find mutiple usedLib for " + toString());
		}
		return paths;
	}

	/**
	 * @return include self
	 */
	public Set<String> getFatherJarClassPaths(boolean includeSelf) {
		Set<String> fatherJarCps = new HashSet<String>();
		for (NodeAdapter node : this.nodeAdapters) {
			fatherJarCps.addAll(node.getAncestorJarCps(includeSelf));
		}
		return fatherJarCps;
	}

	/**
	 * 得到所有这个jar包节点的一层父节点的jar class path
	 * 
	 * @param includeSelf
	 * @return
	 */
	public Set<String> getAllParentJarClassPaths(boolean includeSelf) {
		Set<String> parentJarClassPaths = new HashSet<String>();
		for (NodeAdapter node : this.nodeAdapters) {
			parentJarClassPaths.addAll(node.getParentJarClassPath(includeSelf));
		}
		return parentJarClassPaths;
	}

	public Set<DepJar> getAllParentDepJar() {
		Set<DepJar> parentDepJar = new HashSet<DepJar>();
		for (NodeAdapter node : this.nodeAdapters) {
			parentDepJar.add(node.getParent().getDepJar());
		}
		return parentDepJar;
	}
}
