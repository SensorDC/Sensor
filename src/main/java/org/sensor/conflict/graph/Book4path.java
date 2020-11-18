package org.sensor.conflict.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.sensor.conflict.util.Conf;

public class Book4path extends IBook {
	Set<String> visitedFathes;//record father who copy book from this node,when all father has visited,this book can be deleted.

	public Book4path(Node4path node) {
		super(node);
		this.records = new ArrayList<IRecord>();
		visitedFathes = new HashSet<String>();
	}

	@Override
	public void afterAddAllChildren() {
		if (getNode().isRisk()) {
			this.records.add(getNode().formNewRecord());
		}
	}

	private Node4path getNode() {
		return (Node4path) this.node;
	}

	@Override
	public void addChild(IBook doneChildBook) {
		for (IRecord iRecord : doneChildBook.getRecords()) {
			Record4path record = (Record4path) iRecord;
            addRecord(record.getRiskMethod(), this.getNodeName() + "\n" + record.getPathStr(), record.getPathlen() + 1);
		}
	}

	private void addRecord(String riskMthd, String pathStr, int length) {
		if (Conf.findAllpath) {
			this.records.add(new Record4path(riskMthd, pathStr, length));
		} else {//find shortest path 找最短路径
			for (IRecord iRecord : this.records) {
				Record4path record = (Record4path) iRecord;
                if (riskMthd.equals(record.getRiskMethod())) {
					if (length < record.getPathlen()) {
						record.setPathStr(pathStr);
						record.setPathlen(length);
					}
					return;
				}
			}
			this.records.add(new Record4path(riskMthd, pathStr, length));
		}
	}

}
