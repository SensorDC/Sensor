package neu.lab.conflict.vo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;

public class SemantemeMethod {

	String methodName; // 方法名
	private Set<Unit> units = new HashSet<Unit>(); // 这个方法单元所包含的单元数量
	private Set<Value> values = new HashSet<Value>(); // 这个方法单元所定义和使用的Value数量
	private int branchForUnit = 0;

	public SemantemeMethod(String methodName) {
		super();
		this.methodName = methodName;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}


	public Set<Unit> getUnits() {
		return units;
	}


	public Set<Value> getValues() {
		return values;
	}


	public void setUnits(List<UnitBox> unitBoxs) {
		for (UnitBox unitBox : unitBoxs) {
			this.units.add(unitBox.getUnit());
			if (unitBox.isBranchTarget()) {
				this.setBranchForUnit(this.getBranchForUnit() + 1);
			}
		}
	}


	public void setValues(List<ValueBox> valueBoxs) {
		for (ValueBox valueBox : valueBoxs) {
			this.values.add(valueBox.getValue());
		}
	}

	public int getBranchForUnit() {
		return branchForUnit;
	}

	public void setBranchForUnit(int branchForUnit) {
		this.branchForUnit = branchForUnit;
	}

}
