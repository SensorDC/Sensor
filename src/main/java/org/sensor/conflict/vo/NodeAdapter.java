package org.sensor.conflict.vo;

import java.util.*;

import org.sensor.conflict.util.Conf;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.container.NodeAdapters;
import org.sensor.conflict.util.ClassifierUtil;
import org.sensor.conflict.util.MavenUtil;

/**
 * @author w
 *
 */
public class NodeAdapter {
	protected DependencyNode node;
	protected DepJar depJar;
	protected List<String> filePaths; //文件路径

	public NodeAdapter(DependencyNode node) {
		this.node = node;
		if (node != null)
			resolve();
	}

	public Element getPathElement() {
		Element pathElement = new DefaultElement("path");
		pathElement.addText(getWholePath());
		return pathElement;
	}

	private void resolve() {
		try {
			if (!isInnerProject()) {// inner project is target/classes 内部项目是target/classes
				if (null == node.getPremanagedVersion()) {
					// artifact version of node is the version declared in pom. 节点的构件版本是POM中声明的版本。
					if (!node.getArtifact().isResolved())
						MavenUtil.i().resolve(node.getArtifact());
				} else {
					Artifact artifact = MavenUtil.i().getArtifact(getGroupId(), getArtifactId(), getVersion(),
							getType(), getClassifier(), getScope());
					if (!artifact.isResolved())
						MavenUtil.i().resolve(artifact); // 解析这个构件
				}
			}
		} catch (ArtifactResolutionException e) {
			MavenUtil.i().getLog().warn("cant resolve " + this.toString());
		} catch (ArtifactNotFoundException e) {
			MavenUtil.i().getLog().warn("cant resolve " + this.toString());
		}
	}

	public String getSelectedNodeWholeSig(){
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}

	public String getGroupId() {
		return node.getArtifact().getGroupId();
	}

	public String getScope() {
		return node.getArtifact().getScope();
	}

	public String getArtifactId() {
		return node.getArtifact().getArtifactId();
	}

	public String getVersion() {
		if (null != node.getPremanagedVersion()) {
			return node.getPremanagedVersion();
		} else {
			return node.getArtifact().getVersion();
		}
	}

	/**
	 * version changes because of dependency management 被dependency management更改过版本
	 * 
	 * @return
	 */
	public boolean isVersionChanged() {
		return null != node.getPremanagedVersion();
	}

	protected String getType() {
		return node.getArtifact().getType();
	}

	public String getClassifier() {
		return ClassifierUtil.transformClf(node.getArtifact().getClassifier());
	}

	/**
	 * used version is select from this node,if version was from management ,this
	 * node will return false. 这个版本的node是否被使用，如果被management更改过版本，将返回false
	 * 
	 * @return
	 */
	public boolean isNodeSelected() {
		if (isVersionChanged())
			return false;
		return node.getState() == DependencyNode.INCLUDED;
	}

//	/**
//	 * used version is select from this node
//	 * 
//	 * @return
//	 */
//	public boolean isVersionSelected() {
//		return getDepJar().isSelected();
//	}

	public String getManagedVersion() {
		return node.getArtifact().getVersion();
	}


	/**
	 * @param includeSelf :whether includes self
	 * @return ancestors(from down to top) 从下至上
	 */
	public LinkedList<NodeAdapter> getAncestors(boolean includeSelf) {
		LinkedList<NodeAdapter> ancestors = new LinkedList<NodeAdapter>();
		if (includeSelf)
			ancestors.add(this);
		NodeAdapter father = getParent();
		while (null != father) {
			ancestors.add(father);
			father = father.getParent();
		}
		return ancestors;
	}

	/**
	 * jarClasspaths
	 * 得到所有祖先节点的JarClassPath
	 * @param includeSelf
	 * @return
	 */
	public Collection<String> getAncestorJarCps(boolean includeSelf) {
		List<String> jarCps = new ArrayList<String>();
		if (includeSelf)
			jarCps.addAll(this.getFilePath());
		NodeAdapter father = getParent();
		while (null != father) {
			jarCps.addAll(father.getFilePath());
			father = father.getParent();
		}
		return jarCps;
	}

	/**
	 * get immediate ancestor jar class paths, don't contain cousins
	 * @param includeSelf : whether includes self
	 * @return List<String> jarCps
	 */
	public Collection<String> getImmediateAncestorJarCps(boolean includeSelf){
		Set<NodeAdapter> loadedNodes = new HashSet<>();
		if (includeSelf) {
			loadedNodes.add(NodeAdapters.i().getNodeAdapter(node));
		}
		List<String> jarCps = new ArrayList<String>();
		NodeAdapter father = getParent();
		while (null != father) {
			loadedNodes.add(father);
			father = father.getParent();
		}
		//first level
		Map<String, NodeAdapter> loadedNodesMap = initLoadedNodesMap(loadedNodes);
		List<NodeAdapter> needAddNodes = addExcludeNodes(loadedNodesMap);

		Conf.needAddNodeList.addAll(needAddNodes);
		for(NodeAdapter needAddNode : needAddNodes){
			loadedNodes.add(needAddNode);
			NodeAdapter needAddFather = needAddNode.getParent();
			while (null != needAddFather) {
				loadedNodes.add(needAddFather);
				needAddFather = needAddFather.getParent();
			}
		}


		//second level
		Map<String, NodeAdapter> firstLevelNeedAddNodesMap = initLoadedNodesMap(needAddNodes);
		List<NodeAdapter> firstLevelNeedAddNodes = addExcludeNodes(firstLevelNeedAddNodesMap);
		Conf.firstLevelNeedAddNodeList.addAll(firstLevelNeedAddNodes);
		for(NodeAdapter firstLevelNeedAddNode : firstLevelNeedAddNodes){
			loadedNodes.add(firstLevelNeedAddNode);
			NodeAdapter needAddFather = firstLevelNeedAddNode.getParent();
			while (null != needAddFather) {
				loadedNodes.add(needAddFather);
				needAddFather = needAddFather.getParent();
			}
		}

		loadedNodes.remove(NodeAdapters.i().getNodeAdapter(node));

		for(NodeAdapter loadedNode : loadedNodes){
			jarCps.addAll(loadedNode.getFilePath());
		}

		return jarCps;
	}

	/**
	 * init loaded nodes, used to search excluded nodes
	 * @param loadedNodes
	 * @return Map<String, NodeAdapter> loadedNodesMap
	 */
	private Map<String, NodeAdapter> initLoadedNodesMap(Set<NodeAdapter> loadedNodes){
		Map<String, NodeAdapter> loadedNodesMap = new HashMap<>();
		for(NodeAdapter loadedNode : loadedNodes){
			String sig = loadedNode.getOnlySelectedNodeSig();
			loadedNodesMap.put(sig, loadedNode);
		}
		return loadedNodesMap;
	}

	/**
	 * init loaded nodes, used to search excluded nodes
	 * @param needAddNodes
	 * @return Map<String, NodeAdapter> loadedNodesMap
	 */
	private Map<String, NodeAdapter> initLoadedNodesMap(List<NodeAdapter> needAddNodes){
		Map<String, NodeAdapter> loadedNodesMap = new HashMap<>();
		for(NodeAdapter loadedNode : needAddNodes){
			String sig = loadedNode.getOnlySelectedNodeSig();
			loadedNodesMap.put(sig, loadedNode);
		}
		return loadedNodesMap;
	}


	public String getOnlySelectedNodeSig(){
		return getGroupId() + ":" + getArtifactId();
	}

	/**
	 * if the node is excluded, add it
	 * @param loadedNodesMap
	 * @return List<NodeAdapter> needAddNodes
	 */
	private List<NodeAdapter> addExcludeNodes(Map<String, NodeAdapter> loadedNodesMap){
		List<NodeAdapter> needAddNodes = new ArrayList<>();
		for(Map.Entry<String, List<NodeAdapter>> entry : Conf.dependencyMap.entrySet()){
			if (loadedNodesMap.containsKey(entry.getKey())){
				needAddNodes.addAll(entry.getValue());
			}
		}
		return needAddNodes;
	}
	
	/**
	 * 得到父节点的jar classpath
	 * 只得到一层
	 * @param includeSelf
	 */
public Set<String> getParentJarClassPath(boolean includeSelf) {
		Set<String> jarClassPath = new HashSet<String>();
		if (includeSelf)
			jarClassPath.addAll(this.getFilePath());
		NodeAdapter father = getParent();
		jarClassPath.addAll(father.getFilePath());
		return jarClassPath;
	}
	/**
	 * 得到父节点
	 * 
	 * @return
	 */
	public NodeAdapter getParent() {
		if (null == node.getParent())
			return null;
		return NodeAdapters.i().getNodeAdapter(node.getParent());
	}

	
	/**
	 * 得到文件路径
	 * 
	 * @return
	 */
	public List<String> getFilePath() {
		if (filePaths == null) {
			filePaths = new ArrayList<String>();
			if (isInnerProject()) {// inner project is target/classes
				filePaths.add(MavenUtil.i().getMavenProject(this).getBuild().getOutputDirectory());
				// filePaths = UtilGetter.i().getSrcPaths();
			} else {// dependency is repository address

				try {
					if (null == node.getPremanagedVersion()) {
						filePaths.add(node.getArtifact().getFile().getAbsolutePath());
					} else {
						Artifact artifact = MavenUtil.i().getArtifact(getGroupId(), getArtifactId(), getVersion(),
								getType(), getClassifier(), getScope());
						if (!artifact.isResolved())
							MavenUtil.i().resolve(artifact);
						filePaths.add(artifact.getFile().getAbsolutePath());
					}
				} catch (ArtifactResolutionException e) {
					MavenUtil.i().getLog().warn("cant resolve " + this.toString());
				} catch (ArtifactNotFoundException e) {
					MavenUtil.i().getLog().warn("cant resolve " + this.toString());
				}

			}
		}
		MavenUtil.i().getLog().debug("node filepath for " + toString() + " : " + filePaths);
		return filePaths;

	}

	public boolean isInnerProject() {
		return MavenUtil.i().isInner(this);
	}

	public boolean isSelf(DependencyNode node2) {
		return node.equals(node2);
	}

	public boolean isSelf(MavenProject mavenProject) {
		return getGroupId().equals(mavenProject.getGroupId()) && getArtifactId().equals(mavenProject.getArtifactId())
				&& getVersion().equals(mavenProject.getVersion())
				&& getClassifier().equals(ClassifierUtil.transformClf(mavenProject.getArtifact().getClassifier()));
	}

	public boolean isSelf(NodeAdapter entryNodeAdapter) {
		return getGroupId().equals(entryNodeAdapter.getGroupId())
				&& getArtifactId().equals(entryNodeAdapter.getArtifactId())
				&& getVersion().equals(entryNodeAdapter.getVersion())
				&& getClassifier().equals(entryNodeAdapter.getClassifier());
	}

	public MavenProject getSelfMavenProject() {
		return MavenUtil.i().getMavenProject(this);
	}

	public DepJar getDepJar() {
		if (depJar == null)
			depJar = DepJars.i().getDep(this);
		return depJar;
	}

	@Override
	public String toString() {
		String scope = getScope();
		if (null == scope)
			scope = "";
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion() + ":" + getClassifier() + ":" + scope;
	}

	public String getWholePath() {
		StringBuilder sb = new StringBuilder(toString());
		NodeAdapter father = getParent();
		while (null != father) {
			sb.insert(0, father.toString() + " + ");
			father = father.getParent();
		}
		return sb.toString();
	}

	public int getNodeDepth() {
		int depth = 1;
		NodeAdapter father = getParent();
		while (null != father) {
			depth++;
			father = father.getParent();
		}
		return depth;
	}
}
