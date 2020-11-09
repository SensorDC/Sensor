package neu.lab.conflict.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import neu.lab.conflict.GlobalVar;
import neu.lab.conflict.util.MavenUtil;

public class Dog {
	public enum Strategy { // 是否重置BOOK
		RESET_BOOK, NOT_RESET_BOOK
	}

	private IGraph graph;
	protected String pos;
	protected List<String> route;

	protected Map<String, Cross> graphMap = new HashMap<String, Cross>();

	protected Map<String, List<String>> circleMap = new HashMap<String, List<String>>();

	protected Map<String, IBook> entryBooks;

	protected Map<String, IBook> books;

	protected Map<String, IBook> tempBooks = new HashMap<String, IBook>();// books for nodes in current route.
																			// 当前路线的books

	public Dog(IGraph graph) {
		this.graph = graph;
	}

	protected IBook buyNodeBook(String nodeName) {
		return graph.getNode(nodeName).getBook();
	}

	/**
	 * 
	 * @param entrys       输入节点集合
	 * @param maxDep       最大深度
	 * @param strategyType 策略
	 * @return
	 */
	public Map<String, IBook> findRlt(Collection<String> entrys, int maxDep, Dog.Strategy strategyType) {
		MavenUtil.i().getLog().info("dog starts running with depth " + maxDep);
		Set<String> sortedEntrys = new TreeSet<String>(); // 对输入节点排序
		// TODO
		for (String entry : entrys) {// filter entry that don't exist in graph. 过滤掉不存在于图中的节点
			if (graph.getAllNode().contains(entry))
				sortedEntrys.add(entry);
		}
		Set<String> linkedEntrys = new LinkedHashSet<String>();
		if (entrys.contains("<org.wisdom.test.internals.ChameleonExecutor: void deployApplication()>")) {
			linkedEntrys.add("<org.wisdom.test.internals.ChameleonExecutor: void deployApplication()>");
		} else {
			linkedEntrys.addAll(sortedEntrys);
		}
		if (Strategy.NOT_RESET_BOOK.equals(strategyType))
			return findResultNotResetBook(linkedEntrys, maxDep);
		else
			return findResultResetBook(linkedEntrys, maxDep);
	}

	private Map<String, IBook> findResultNotResetBook(Collection<String> entrys, int maxDep) {
		MavenUtil.i().getLog().info("dog won't reset doneBook.");// 不重置已完成的book
		books = new HashMap<String, IBook>();
		long start = System.currentTimeMillis();
		for (String mthd : entrys) {
			route = new ArrayList<String>();
			if (books.containsKey(mthd))
				continue;
			else {
				forward(mthd);
				while (pos != null) {
					if (needChildBook(maxDep)) {
						String frontNode = graphMap.get(pos).getBranch();
						getChildBook(frontNode);
					} else {
						back();
					}
				}
			}
		}
		long runtime = (System.currentTimeMillis() - start) / 1000;
		MavenUtil.i().getLog().info("dog finishes running.");
		MavenUtil.i().getLog().info("dog run time:" + runtime);
		GlobalVar.time2runDog += runtime;
		return this.books;
	}

	/**
	 * reset doneBook for each entry to guarantee depth. depth may short than
	 * configuration because of doneBook in each search.
	 * 
	 * @param entrys
	 * @param maxDep
	 * @return
	 */
	private Map<String, IBook> findResultResetBook(Collection<String> entrys, int maxDep) {
		MavenUtil.i().getLog().info("dog will reset doneBook.");
		entryBooks = new HashMap<String, IBook>();
		long start = System.currentTimeMillis();
		for (String mthd : entrys) {
			route = new ArrayList<String>();
			books = new HashMap<String, IBook>();
			forward(mthd);
			while (pos != null) {
				if (needChildBook(maxDep)) {
					String frontNode = graphMap.get(pos).getBranch();
					getChildBook(frontNode);
				} else {
					back();
				}
			}
			entryBooks.put(mthd, books.get(mthd));
		}
		long runtime = (System.currentTimeMillis() - start) / 1000;
		MavenUtil.i().getLog().info("dog finishes running.");
		MavenUtil.i().getLog().info("dog run time:" + runtime);
		GlobalVar.time2runDog += runtime;
		MavenUtil.i().getLog().info("this.entryBooks:" + this.entryBooks.size());
		return this.entryBooks;
	}

	public boolean needChildBook(int maxDep) {
		return graphMap.get(pos).hasBranch() && route.size() < maxDep;
	}

	private void getChildBook(String frontNode) {
		if (books.containsKey(frontNode)) {
			addChildBookInfo(frontNode, pos);
		} else {
			forward(frontNode);
		}
	}

	/**
	 * frontNode是一个手册没有完成的节点，需要为这个节点建立手册
	 * 
	 * @param frontNode
	 */
	private void forward(String frontNode) {
		// TODO debug dog
		// DebugUtil.print(UserConf.getOutDir() + "tdogTrace.txt", frontNode + " " +
		// route.size());
		INode node = graph.getNode(frontNode);
		if (node != null) {
			if (!route.contains(frontNode)) {
				pos = frontNode;
				route.add(pos);
				IBook nodeRch = buyNodeBook(frontNode);
				this.tempBooks.put(frontNode, nodeRch);
				graphMap.put(pos, new Cross(node));
			} else {
				List<String> circle = new ArrayList<String>();
				int index = route.indexOf(frontNode) + 1;
				while (index < route.size()) {
					circle.add(route.get(index));
					index++;
				}
				this.circleMap.put(frontNode, circle);
			}
		}
	}

	private void back() {
		String donePos = route.get(route.size() - 1);
		graphMap.remove(donePos);
		IBook book = this.tempBooks.get(donePos);
		book.afterAddAllChildren();

		this.tempBooks.remove(donePos);
		this.books.put(donePos, book);

		if (circleMap.containsKey(donePos)) {

			dealLoopNd(donePos);
			circleMap.remove(donePos);
		}

		route.remove(route.size() - 1);

		if (route.size() == 0) {
			pos = null;
		} else {
			pos = route.get(route.size() - 1);
			addChildBookInfo(donePos, pos);
		}
	}

	private void addChildBookInfo(String donePos, String pos) {
		IBook doneBook = this.books.get(donePos);
		IBook doingBook = this.tempBooks.get(pos);
		doingBook.addChild(doneBook);
	}

	protected void dealLoopNd(String donePos) {
	}

}
