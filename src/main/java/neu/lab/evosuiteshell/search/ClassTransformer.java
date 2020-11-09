package neu.lab.evosuiteshell.search;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.util.Chain;

public class ClassTransformer extends SceneTransformer {

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		// TODO Auto-generated method stub
		Chain<SootClass> allClass = Scene.v().getClasses();
		for (SootClass sootClass : allClass) {
			if (sootClass.isJavaLibraryClass())
				continue;
			Set<SootClass> allSuper = new HashSet<SootClass>();
			getSuper(sootClass, allSuper);
			for (SootClass superClass : allSuper) {
				ClassInherit.getInstance().addInheritInfo(new ClassInfo(superClass), new ClassInfo(sootClass));
			}
		}
	}

	private void getSuper(SootClass cls, Set<SootClass> allSuper) {
		Set<SootClass> allDirectSuper = new HashSet<SootClass>();

		if (cls.hasSuperclass()) {
			allDirectSuper.add(cls.getSuperclass());
			allSuper.add(cls.getSuperclass());
		}

		Chain<SootClass> superInters = cls.getInterfaces();
		if (null != superInters) {
			for (SootClass superInter : superInters) {
				allDirectSuper.add(superInter);
				allSuper.add(superInter);
			}
		}
		if (!allDirectSuper.isEmpty()) {
			for (SootClass superC : allDirectSuper) {
				getSuper(superC, allSuper);
			}
		}

	}
}
