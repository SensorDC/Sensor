package org.sensor.evosuiteshell.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.soot.JarAna;
import org.sensor.conflict.vo.ClassVO;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.util.Chain;

public class ClassTransformer extends SceneTransformer {
    //    private Map<String, ClassVO> allclasses = JarAna.i().deconstruct(Arrays.asList(new String[]{"/Users/wangchao/个人文件/东北大学/实验室/实验室台式/eclipse/Host/target/Host-1.0.jar"}));
    public ClassTransformer() {

    }

    Set<String> classesSig = new HashSet<>();

    public ClassTransformer(Set<String> classesSig) {
        this.classesSig = classesSig;
    }

    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        // TODO Auto-generated method stub

//        Set<String> entryClses = DepJars.i().getHostDepJar().getAllCls(true);

        Chain<SootClass> allClass = Scene.v().getClasses();
//        HashSet<SootClass> filterAllclasses = new HashSet<SootClass>();
//        for(SootClass sootClass : allClass){
//if(allclasses.keySet().contains(sootClass.getName())){
//    filterAllclasses.add(sootClass);
//}
//        }
        for (SootClass sootClass : allClass) {
            ClassInfo clsVO = new ClassInfo(sootClass);
//            if (!classesSig.contains(clsVO.getSig())) {
//                continue;
//            }
            ProjectInfo.i().addClass(clsVO);
            for (MethodInfo methodInfo : clsVO.getMthds()) {
                ProjectInfo.i().addMethod(methodInfo);
            }
//			for (SootMethod method : sootClass.getMethods()) {
//				System.out.println("static:" + method.isStatic() + " " + "public:" + method.isPublic() + " "
//						+ method.getSignature());
//			}
        }
        //make the CHA.
        for (SootClass sootClass : allClass) {
            Set<SootClass> allSuper = new HashSet<SootClass>();
            getSuper(sootClass, allSuper);
            for (SootClass superClass : allSuper) {
                ProjectInfo.i().addInheritInfo(superClass.getName(), sootClass.getName());
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
