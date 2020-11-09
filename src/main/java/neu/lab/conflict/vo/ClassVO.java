package neu.lab.conflict.vo;

import java.util.HashSet;
import java.util.Set;

/**
 * 每个jar包中的类VO
 * @author wangchao
 *
 */
public class ClassVO {
	private String classSig;//类标记
	private Set<MethodVO> methods;// methods in class
	private DepJar depJar;//所属的jar

	public ClassVO(String clsSig) {
		this.classSig = clsSig;
		methods = new HashSet<MethodVO>();
	}

	public DepJar getDepJar() {
		return depJar;
	}

	public void setDepJar(DepJar depJar) {
		this.depJar = depJar;
	}

	public boolean addMethod(MethodVO mthd) {
		return methods.add(mthd);
	}

	/**
	 * if contains method called mthdSig(may not same method object)
	 * 是否包含相同方法（可能不是同一个对象）
	 * @param mthdSig
	 * @return
	 */
	public boolean hasMethod(String mthdSig2) {
		for (MethodVO mthd : methods) {
			if (mthd.isSameName(mthdSig2))
				return true;
		}
		return false;
	}

	/**
	 * 是否是同一个类
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassVO) {
			ClassVO classVO = (ClassVO) obj;
			return classSig.equals(classVO.getClassSig());
		} else {
			return false;
		}

	}

	@Override
	public int hashCode() {
		return this.classSig.hashCode();
	}

	public String getClassSig() {
		return classSig;
	}

	public void setClassSig(String clsSig) {
		this.classSig = clsSig;
	}

	public Set<MethodVO> getMethods() {
		return methods;
	}

	public void setMethods(Set<MethodVO> mthds) {
		this.methods = mthds;
	}

}
