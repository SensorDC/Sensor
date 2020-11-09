package neu.lab.evosuiteshell.search;

import java.util.HashMap;
import java.util.HashSet;

public class ClassInherit {
//是否需要按顺序添加和取出
	private static ClassInherit instance = new ClassInherit();

	private HashMap<String, HashSet<ClassInfo>> classSig;
//	private HashMap<String, HashSet<String>> methodSig;// ?

	private ClassInherit() {
		classSig = new HashMap<String, HashSet<ClassInfo>>();
//		methodSig = new HashMap<String, HashSet<String>>();
	}

	public static ClassInherit getInstance() {
		return instance;
	}

	public void addClass(ClassInfo cls) {

	}

	public void addInheritInfo(ClassInfo superClass, ClassInfo childClass) {
		String superClassSig = superClass.getSig();
		HashSet<ClassInfo> childClassInfos = classSig.get(superClassSig);
		if (childClassInfos == null) {
			childClassInfos = new HashSet<ClassInfo>();
			classSig.put(superClassSig, childClassInfos);
		}
		childClassInfos.add(childClass);
	}

	public HashMap<String, HashSet<ClassInfo>> getClassSig() {
		return classSig;
	}

}
