package neu.lab.conflict.distance;

import java.util.HashMap;
import java.util.Map;

public abstract class NodeDistances {

	protected Map<String, Map<String, Double>> bottomToTopToDistance;// <bottom,<top,distance>>

	public NodeDistances() {
		bottomToTopToDistance = new HashMap<String, Map<String, Double>>();
	}

	// public static NodeDistances i() {
	// if (instance == null) {
	// instance = new NodeDistances();
	// }
	// return instance;
	// }
	
	public boolean isEmpty() {
		return bottomToTopToDistance.isEmpty();
	}

	public void addDistance(String bottom, String top, Double newDis) {
		Map<String, Double> t2d = bottomToTopToDistance.get(bottom);
		if (t2d == null) {
			t2d = new HashMap<String, Double>();
			bottomToTopToDistance.put(bottom, t2d);
		}
		Double oldDis = t2d.get(top);
		if (oldDis == null) {
			t2d.put(top, newDis);
		} else {// put min
			if (newDis < oldDis)
				t2d.put(top, newDis);
		}
	}

	public void addDistances(Map<String, Map<String, Double>> newData) {
		for (String bottom : newData.keySet()) {
			if (bottomToTopToDistance.containsKey(bottom)) {// has this bottom.
				Map<String, Double> oldT2d = bottomToTopToDistance.get(bottom);
				Map<String, Double> newT2d = newData.get(bottom);
				for (String top : newT2d.keySet()) {
					if (oldT2d.containsKey(top)) {// has this top
						if (newT2d.get(top) < oldT2d.get(top)) {// put the min.
							oldT2d.put(top, newT2d.get(top));
						}
					} else {// new target
						oldT2d.put(top, newT2d.get(top));
					}
				}
			} else {// new source
				bottomToTopToDistance.put(bottom, newData.get(bottom));
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String source : bottomToTopToDistance.keySet()) {
			Map<String, Double> dises = bottomToTopToDistance.get(source);
			for (String target : dises.keySet()) {
				sb.append(source + "," + target + "," + dises.get(target) + "," + isHostNode(target));
				sb.append(System.lineSeparator());
			}
		}
		return sb.toString();
	}

	public abstract boolean isHostNode(String nodeName);
}
