package neu.lab.conflict.container;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.shared.dependency.tree.DependencyNode;

import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.util.NodeAdapterCollector;
import neu.lab.conflict.vo.DepJar;
import neu.lab.conflict.vo.ManageNodeAdapter;
import neu.lab.conflict.vo.NodeAdapter;

/**
 * @author asus
 *
 */
public class NodeAdapters {
	private static NodeAdapters instance;

	public static NodeAdapters i() {
		return instance;
	}

	public static void init(DependencyNode root) {
		// if (instance == null) {
		instance = new NodeAdapters();
		// add node in dependency tree
		NodeAdapterCollector visitor = new NodeAdapterCollector(instance);
		root.accept(visitor);
		// add management node
		List<NodeAdapter> manageNds = new ArrayList<NodeAdapter>();
		for (NodeAdapter nodeAdapter : instance.container) {
			if (nodeAdapter.isVersionChanged()) {// this node have management
				if (null == instance.getNodeAdapter(nodeAdapter)) {
					// this managed-version doesnt have used node,we should new a virtual node to
					// find conflict
					NodeAdapter manageNd = null;
					for (NodeAdapter existManageNd : manageNds) {// find if manageNd exists
						if (existManageNd.isSelf(nodeAdapter)) {
							manageNd = existManageNd;
							break;
						}
					}
					if (null == manageNd) {// dont exist manageNd,should new and add
						manageNd = new ManageNodeAdapter(nodeAdapter);
						manageNds.add(manageNd);
					}
				}
			}
		}
		for (NodeAdapter manageNd : manageNds) {
			instance.addNodeAapter(manageNd);
		}
		// }
	}

	private List<NodeAdapter> container;

	private NodeAdapters() {
		container = new ArrayList<NodeAdapter>();
	}

	public void addNodeAapter(NodeAdapter nodeAdapter) {
		container.add(nodeAdapter);
	}

	/**
	 * 根据node获得对应的adapter
	 * 
	 * @param node
	 */
	public NodeAdapter getNodeAdapter(DependencyNode node) {
		for (NodeAdapter nodeAdapter : container) {
			if (nodeAdapter.isSelf(node))
				return nodeAdapter;
		}
		MavenUtil.i().getLog().warn("cant find nodeAdapter for node:" + node.toNodeString());
		return null;
	}

	public NodeAdapter getNodeAdapter(NodeAdapter entryNodeAdapter) {
		for (NodeAdapter nodeAdapter : container) {
			if (nodeAdapter.isSelf(entryNodeAdapter))
				return nodeAdapter;
		}
		MavenUtil.i().getLog().warn("can not find nodeAdapter for management node:" + entryNodeAdapter.toString());
		return null;
	}

	/**
	 * 得到使用depJar的所有NodeAdapters
	 * @param depJar
	 * @return
	 */
	public Set<NodeAdapter> getNodeAdapters(DepJar depJar) {
		Set<NodeAdapter> result = new HashSet<NodeAdapter>();
		for (NodeAdapter nodeAdapter : container) {
			if (nodeAdapter.getDepJar() == depJar) {
				result.add(nodeAdapter);
			}
		}
		if (result.size() == 0)
			MavenUtil.i().getLog().warn("cant find nodeAdapter for depJar:" + depJar.toString());
		return result;
	}

	public List<NodeAdapter> getAllNodeAdapter() {
		return container;
	}

}
