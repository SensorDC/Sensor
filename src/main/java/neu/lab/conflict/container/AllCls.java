package neu.lab.conflict.container;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import neu.lab.conflict.vo.DepJar;

/**
 * @author asus
 *FinalClasses is set of ClassVO,but AllCls is set of class signature.
 *FinalClasses是ClassVO的集合，但AllCls是类签名的集合。
 */
public class AllCls {
	private static AllCls instance; //实例
	private Set<String> classes;

	public static void init(DepJars depJars) {
		if (instance == null) {
			instance = new AllCls(depJars);
		}
	}
	/**
	 * 初始化的时候用其他depJar
	 * @param depJars
	 * @param depJar
	 */
	public static void init(DepJars depJars, DepJar depJar) {
			instance = new AllCls(depJars, depJar);
	}
	public static AllCls i() {
		return instance;
	}
	
	//构造函数
	private AllCls(DepJars depJars){
		classes = new HashSet<String>();
		for (DepJar depJar : depJars.getAllDepJar()) {
			if (depJar.isSelected()) {
				//得到depJar中所有的类
				classes.addAll(depJar.getAllCls(true));
			}
		}
	}
	/*
	 * 重构方法，使初始化方法有默认参数
	 */
	private AllCls(DepJars depJars, DepJar usedDepJar) {
		classes = new HashSet<String>();
		for (DepJar depJar : depJars.getAllDepJar()) {
			if (depJar.isSelected()) {
				//得到depJar中所有的类
				if (depJar.isSameLib(usedDepJar)) {
					classes.addAll(usedDepJar.getAllCls(true));
				} else {
					classes.addAll(depJar.getAllCls(true));
				}
			}
		}
	}
	
	public Set<String> getAllCls() {
		return classes;
	}
	
	//classes是否包含class
	public boolean contains(String cls) {
		return classes.contains(cls);
	}
	
	//得到不在本类集合中的类
	public Set<String> getNotInClasses(Collection<String> testSet){
		Set<String> notInClasses = new HashSet<String>();
		for(String cls:testSet) {
			if(!this.contains(cls)) {
				notInClasses.add(cls);
			}
		}
		return notInClasses;
	}
}
