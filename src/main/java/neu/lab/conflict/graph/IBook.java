package neu.lab.conflict.graph;

import java.util.Collection;

public abstract class IBook {

	protected INode node;
	protected Collection<IRecord> records;
	
	public IBook(INode node) {
		this.node = node;
	}

	/**
	 * 当dog返回时，把信息记录到book中
	 */
	public abstract void afterAddAllChildren();// when dog is back,add self information to book.

	/**
	 * 加入子book的路径给自己
	 * @param doneChildBook
	 */
	public abstract void addChild(IBook doneChildBook);// add child book path to self.

	public String getNodeName() {
		return node.getName();
	}

	public Collection<IRecord> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("book for " + this.getNodeName() + "\n");
		for (IRecord recordI : this.getRecords()) {
			sb.append(recordI.toString() + "\n");
		}
		return sb.toString();
	}

}
