package neu.lab.conflict.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.vo.SemantemeMethod;

public class SemantemeMethods {

	private Map<String, SemantemeMethod> coveredDepJarSemantemeMethods; // 被覆盖JAR包的语义方法集合
	private Map<String, SemantemeMethod> usedDepJarSemantemeMethods; // 使用JAR包的语义方法集合
	private Map<String, Integer> semantemeMethodForDifferences = new HashMap<String, Integer>(); // 语义方法的差异集合
	private Map<String, List<Integer>> semantemeMethodForReturn = new HashMap<String, List<Integer>>();

	public Map<String, List<Integer>> getSemantemeMethodForReturn() {
		return semantemeMethodForReturn;
	}

	public void setSemantemeMethodForReturn(Map<String, List<Integer>> semantemeMethodForReturn) {
		this.semantemeMethodForReturn = semantemeMethodForReturn;
	}

	public Map<String, Integer> getSemantemeMethodForDifferences() {
		return semantemeMethodForDifferences;
	}

	public SemantemeMethods(Map<String, SemantemeMethod> coveredDepJarSemantemeMethods,
			Map<String, SemantemeMethod> usedDepJarSemantemeMethods) {
		super();
		this.coveredDepJarSemantemeMethods = coveredDepJarSemantemeMethods;
		this.usedDepJarSemantemeMethods = usedDepJarSemantemeMethods;
	}

	public void CalculationDifference() {
		for (String methodName : coveredDepJarSemantemeMethods.keySet()) {
			int difference = 0;
			int same = 0;
			int differenceForUnit = 0;
			int sameForUnit = 0;
//			double differenceForUnitOnlyBranch = 0;
			int differenceForValue = 0;
			int sameForValue = 0;
//			boolean existence;
			if (usedDepJarSemantemeMethods.containsKey(methodName)) {
				SemantemeMethod coveredsemantemeMethod = coveredDepJarSemantemeMethods.get(methodName);
				SemantemeMethod usedsemantemeMethod = usedDepJarSemantemeMethods.get(methodName);

				if (coveredsemantemeMethod.getUnits().size() == usedsemantemeMethod.getUnits().size()) {
//					for (Unit unit : coveredsemantemeMethod.getUnits()) {
//						existence = false;
//						for (Unit unitUsed : usedsemantemeMethod.getUnits()) {
//							if (unitUsed.toString().equals(unit.toString())) {
//								existence = true;
//								break;
//							}
//						}
//						if (!existence) {
//							differenceForUnit++;
//							if (unit.branches()) {
//								differenceForUnit++;
//							}
//						}
//					}
				} else {
					differenceForUnit = Math
							.abs(coveredsemantemeMethod.getUnits().size() - usedsemantemeMethod.getUnits().size())
							+ Math.abs(
									coveredsemantemeMethod.getBranchForUnit() - usedsemantemeMethod.getBranchForUnit());
					if (coveredsemantemeMethod.getUnits().size() > usedsemantemeMethod.getUnits().size()) {
						sameForUnit = sameForUnit + usedsemantemeMethod.getUnits().size();
					} else {
						sameForUnit = sameForUnit + coveredsemantemeMethod.getUnits().size();
					}
					if (coveredsemantemeMethod.getBranchForUnit() > usedsemantemeMethod.getBranchForUnit()) {
						sameForUnit = sameForUnit + usedsemantemeMethod.getBranchForUnit();
					} else {
						sameForUnit = sameForUnit + coveredsemantemeMethod.getBranchForUnit();
					}
				}
				if (coveredsemantemeMethod.getValues().size() == usedsemantemeMethod.getValues().size()) {
//					for (Value value : coveredsemantemeMethod.getValues()) {
//						existence = false;
//						for (Value valueUsed : usedsemantemeMethod.getValues()) {
//							if (valueUsed.toString().equals(value.toString())) {
//								existence = true;
//								break;
//							}
//						}
//						if (!existence) {
//							differenceForValue++;
//						}
//					}
				} else {
					differenceForValue = Math
							.abs(coveredsemantemeMethod.getValues().size() - usedsemantemeMethod.getValues().size());
					if (coveredsemantemeMethod.getValues().size() > usedsemantemeMethod.getValues().size()) {
						sameForValue = sameForValue + usedsemantemeMethod.getValues().size();
					} else {
						sameForValue = sameForValue + coveredsemantemeMethod.getValues().size();
					}
				}
			}
			same = sameForUnit + sameForValue;
			difference = differenceForUnit + differenceForValue;
			if (difference > 0) {
				semantemeMethodForReturn.put(methodName, new ArrayList<Integer>(Arrays.asList(difference, same)));
				semantemeMethodForDifferences.put(methodName, difference);
			}
		}
	}

	/**
	 * 对Map排序后，输出前N个Intger最大的method 降序
	 * 
	 * @param map
	 * @param entrySize 大小限制，输出多少个排序后数组
	 * @return
	 */
	public Set<String> sortMap(int entrySize) {
		if (semantemeMethodForDifferences.size() == 0) {
			return null;
		}
		Set<String> afterSortMethods = new HashSet<String>();
		List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(
				semantemeMethodForDifferences.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> obj1, Map.Entry<String, Integer> obj2) {
				return obj2.getValue() - obj1.getValue();
			}
		});
		int size = 0;
		if (semantemeMethodForDifferences.size() > entrySize) {
			size = entrySize;
		} else {
			size = semantemeMethodForDifferences.size();
		}
		for (int i = 0; i < size; i++) {
			afterSortMethods.add(entries.get(i).getKey());
		}
		return afterSortMethods;
	}
}
