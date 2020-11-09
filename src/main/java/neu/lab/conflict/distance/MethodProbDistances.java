package neu.lab.conflict.distance;

import java.util.HashMap;
import java.util.Map;

import org.evosuite.coverage.method.designation.NodeProbDistance;

import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.util.SootUtil;

public class MethodProbDistances extends NodeDistances {

	protected Map<String, Map<String, Double>> bottomToTopToProbability;// <bottom,<top,probability>>

	public MethodProbDistances() {
		bottomToTopToProbability = new HashMap<String, Map<String, Double>>();
	}

	public void addProb(String bottom, String top, double newLen) {
		Map<String, Double> topToProbability = bottomToTopToProbability.get(bottom);
		if (topToProbability == null) {
			topToProbability = new HashMap<String, Double>();
			bottomToTopToProbability.put(bottom, topToProbability);
		}
		Double oldProb = topToProbability.get(top);
		if (oldProb == null) {
			topToProbability.put(top, newLen);
		} else {// put min
			if (newLen < oldProb)
				topToProbability.put(top, newLen);
		}
	}

	@Override
	public boolean isHostNode(String nodeName) {
		return MavenUtil.i().isHostClass(SootUtil.mthdSig2cls(nodeName));
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		for (String source : bottomToTopToDistance.keySet()) {
			Map<String, Double> topToDistance = bottomToTopToDistance.get(source);
			Map<String, Double> topToProbability = bottomToTopToProbability.get(source);
			for (String target : topToProbability.keySet()) {
				stringBuilder.append(source + "," + target + "," + topToDistance.get(target) + "," + isHostNode(target)
						+ "," + topToProbability.get(target));
				stringBuilder.append(System.lineSeparator());
			}
		}
		return stringBuilder.toString();
	}

	public NodeProbDistance getEvosuiteProbability(String riskMethod) {
		NodeProbDistance distances = new NodeProbDistance();
		for (String bottom : bottomToTopToDistance.keySet()) {
			Map<String, Double> topToDistance = bottomToTopToDistance.get(bottom);
			Map<String, Double> topToProbability = bottomToTopToProbability.get(bottom);
			for (String top : topToProbability.keySet()) {
				if (riskMethod.equals(bottom)) {
					distances.addMetric(top, topToDistance.get(top), topToProbability.get(top));
				}
			}
		}
		return distances;
	}

}
