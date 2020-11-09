package neu.lab.conflict.graph;

import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author asus
 *
 */
public class Cross {
	Iterator<String> cross;

	Cross(INode node) {
		Collection<String> inMthds = node.getNexts(); // 得到下一个点的集合，即路口
		if (null != inMthds) {
			this.cross = inMthds.iterator();
		} else {
			this.cross = null;
		}
	}

	/**
	 * 是否有分支，若cross不为空，则有分支
	 * 
	 * @return
	 */
	boolean hasBranch() {
		if (null == cross)
			return false;
		return cross.hasNext();
	}

	/**
	 * 得到分支，即下一个cross
	 * 
	 * @return
	 */
	String getBranch() {
		return cross.next();
	}
}
