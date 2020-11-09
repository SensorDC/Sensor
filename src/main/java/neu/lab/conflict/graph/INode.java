package neu.lab.conflict.graph;

import java.util.Collection;

public interface INode {

	public String getName();

	/**
	 * 当dog走到这个节点是，得到下一个dog应该走的节点
	 * 
	 * @return
	 */
	public Collection<String> getNexts();// next nodes that dog should go when writes book about this node.

	public IBook getBook();

	// if this node is a end node,node should form a new record.Else nodes change
	// the copy of end node.
	// call by afterAddAllChildren.
	/**
	 * 如果这个节点是结束节点，节点应该生成一条新的记录。否则节点应该改变终止节点的副本
	 * 被afterAddAllChildren调用
	 * @return
	 */
	public IRecord formNewRecord();

}
