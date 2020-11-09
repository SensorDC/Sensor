package neu.lab.conflict.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import neu.lab.conflict.vo.NodeAdapter;
import neu.lab.conflict.util.Conf;
import neu.lab.conflict.vo.Conflict;

public class Conflicts {
	private static Conflicts instance;	//实例

	/**
	 * 通过节点初始化冲突，如果有两个相同节点
	 * @param nodeAdapters
	 */
	public static void init(NodeAdapters nodeAdapters) {
		//if (instance == null) {   不使用单例模式
			instance = new Conflicts(nodeAdapters);
		//}
	}

	public static Conflicts i() {
		return instance;
	}

	private List<Conflict> container; 	//冲突容器，存储多个冲突

	/**
	 * must initial NodeAdapters before this construct
	 * 必须在这个构造函数前初始化NodeAdapters
	 */
	private Conflicts(NodeAdapters nodeAdapters) {
		container = new ArrayList<Conflict>();
		for (NodeAdapter node : nodeAdapters.getAllNodeAdapter()) {
				addNodeAdapter(node);
		}
		// delete conflict if there is only one version 如果只有一个版本就删除冲突
		Iterator<Conflict> ite = container.iterator();
		while (ite.hasNext()) {
			Conflict conflict = ite.next();
			if (!conflict.isConflict()||!wantCal(conflict)) {	//如果这个方法不是需要的冲突
				ite.remove();
			}
		}
	}
	
	/**this method use to debug.
	 * 这个方法用于DEBUG，可以设置参数-DcallConflict指向存在的某一个冲突
	 * @param conflict
	 * @return
	 */
	private boolean wantCal(Conflict conflict) {

		if(Conf.callConflict==null||"".equals(Conf.callConflict)) {
			return true;
		}else {
			if(conflict.getSig().equals(Conf.callConflict.replace("+", ":"))) 
				return true;
			return false;
		}
	}

	/**
	 * 得到所有冲突，返回冲突容器
	 * @return
	 */
	public List<Conflict> getConflicts() {
		return container;
	}

	/**
	 * 添加冲突
	 * 如果容器中已经存在一个conflict和本nodeAdapter是相同的构件
	 * 则为这个conflict添加本节点适配器
	 * 如果容器中不存在
	 * 则本nodeAdapter作为一个conflict加入容器
	 * 然后为这个conflict加入本节点
	 * @param nodeAdapter
	 */
	private void addNodeAdapter(NodeAdapter nodeAdapter) {
		Conflict conflict = null;
		for (Conflict existConflict : container) {
			if (existConflict.sameArtifact(nodeAdapter.getGroupId(), nodeAdapter.getArtifactId())) {
				conflict = existConflict;
			}
		}
		if (null == conflict) {
			conflict = new Conflict(nodeAdapter.getGroupId(), nodeAdapter.getArtifactId());
			container.add(conflict);
		}
		conflict.addNodeAdapter(nodeAdapter);
	}

	@Override
	public String toString() {
		String str = "project has " + container.size() + " conflict-dependency:+\n";
		for (Conflict conflictDep : container) {
			str = str + conflictDep.toString() + "\n";
		}
		return str;
	}
}
