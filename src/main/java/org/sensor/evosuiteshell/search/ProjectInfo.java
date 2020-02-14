package org.sensor.evosuiteshell.search;


import org.sensor.conflict.util.MavenUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class ProjectInfo {
    private static ProjectInfo instance = new ProjectInfo();


    LinkedHashMap<String, ClassInfo> sig2class;
    LinkedHashMap<String, MethodInfo> sig2method;
    Set<String> clsesInPath;
    String entryCls;

    private ProjectInfo() {
        sig2class = new LinkedHashMap<String, ClassInfo>();
        sig2method = new LinkedHashMap<String, MethodInfo>();
    }

    public void setEntryCls(String entryCls) {
        this.entryCls = entryCls;
    }

    public boolean isEntryPck(String pck) {
        return SootUtil.cls2pck(entryCls, ".").equals(pck);
    }

    public void addInheritInfo(String superCls, String childCls) {
        ClassInfo superVO = sig2class.get(superCls);
        ClassInfo childVO = sig2class.get(childCls);
        if (superVO != null && childVO != null) {
            superVO.addChild(childVO);
        }
    }

    public boolean isClsInPath(String clsSig) {
        return clsesInPath.contains(clsSig);
    }

    public void initClsesInPath(List<String> mthdInPath) {
        clsesInPath = new HashSet<String>();
        for (String mthd : mthdInPath) {
            clsesInPath.add(SootUtil.mthdSig2cls(mthd));
        }
    }

    public ClassInfo getClassInfo(String clsSig) {
        ClassInfo clsVO = sig2class.get(clsSig);
        if (clsVO == null) {
//			for (StackTraceElement ele : Thread.currentThread().getStackTrace()) {
//				System.out.println("lzw trace:" + ele);
//			}
//            MavenUtil.i().getLog().warn("can't find ClassInfo for:" + clsSig);
//            System.out.println("can't find ClassInfo for:" + clsSig);
        }
        return clsVO;
    }

    public MethodInfo getMethodVO(String mthdSig) {
        return sig2method.get(mthdSig);
    }

    public void addClass(ClassInfo cls) {
        sig2class.put(cls.getSig(), cls);
    }

    public void addMethod(MethodInfo mthd) {
        sig2method.put(mthd.getSig(), mthd);
    }

    public static ProjectInfo i() {
        return instance;
    }

    public Collection<MethodInfo> getAllMethod() {
        return sig2method.values();
    }

    public Collection<ClassInfo> getAllClassInfo() {
        return sig2class.values();
    }

    public Set<String> getAllClassSig() {
        return sig2class.keySet();
    }

    public boolean isInvokeable(MethodInfo mthd) {
        ClassInfo ClassInfo = mthd.getCls();
        boolean isEntryPck = isEntryPck(SootUtil.cls2pck(ClassInfo.getSig(), "."));
        if (isEntryPck) {
            return !ClassInfo.isPrivate() && !mthd.isPrivate();
        } else {
            return ClassInfo.isPublic() && mthd.isPublic();
        }
    }

    //	Map<String,MethodInfo>
}
