package org.sensor.evosuiteshell.search;

import java.util.*;

import fj.P;
import soot.SootClass;
import soot.SootMethod;

public class ClassInfo {

    private String sig;
    private List<ClassInfo> children;//direct and indirect
    private LinkedHashSet<MethodInfo> mthds;
    private boolean isConcrete;
    private boolean isPublic;
    private boolean isPrivate;
    private Class<?> clazz;

    public Class<?> getClazz() {
        return clazz;
    }

    public ClassInfo(SootClass stCls) {
        clazz = stCls.getType().getClass();
        this.sig = stCls.getName();
        this.isPublic = stCls.isPublic();
        this.isPrivate = stCls.isPrivate();
        mthds = new LinkedHashSet<MethodInfo>();
        this.isConcrete = stCls.isConcrete();
        for (SootMethod method : stCls.getMethods()) {
            mthds.add(new MethodInfo(this, method));
        }
        children = new ArrayList<ClassInfo>();
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    private String getType2Cons() {
        if (isConcrete()) {
            return sig;
        } else {
            return getBestChildType();
        }
    }

    public void addChild(ClassInfo childVO) {
        children.add(childVO);
    }

    private String getBestChildType() {
        //find type in path
        for (ClassInfo childVO : children) {
            if (ProjectInfo.i().isClsInPath(childVO.getSig()))
                return childVO.getConcreteType();
        }
        //return a concrete.
        return getConcreteType();
    }

    /**
     * if class is concrete,then return.
     * else return a concrete subclass.
     *
     * @return
     */
    private String getConcreteType() {
        if (isConcrete()) {
            return sig;
        } else {
            for (ClassInfo childVO : children) {
                if (childVO.isConcrete)
                    return childVO.getSig();
            }
        }
        return null;
    }

    /**
     * if class is concrete,then return.
     * else return a concrete subclass.
     *
     * @return
     */
    public Set<String> getAllConcreteType() {
        HashSet<String> sigs = new HashSet<>();
        if (isConcrete()) {
            sigs.add(sig);
        }
        for (ClassInfo childVO : children) {
            if (childVO.isConcrete())
                sigs.add(childVO.getSig());
        }
        return sigs;
    }


    public boolean hasTargetChildren(ClassInfo classInfo) {
        return children.contains(classInfo);
    }


    public boolean isConcrete() {
        return isConcrete;
    }

    public String getSig() {
        return sig;
    }

    public Collection<MethodInfo> getMthds() {
        return mthds;
    }

    /**
     * type of this object may be not the type of construct.
     * if this type is abstract,the type to construct should be its subType.
     *
     * @param
     * @return
     */
    public MethodInfo getBestCons(boolean scanStatic) {
        //find static constructor.
        if (scanStatic) {
            List<MethodInfo> consesOfStatic = getConsesOfStatic();

            if (this.isConcrete && !consesOfStatic.isEmpty()) {
                //this class is concrete and has constructor of this type.
                System.out.println(sig + " has static constructor.");
                return getMinParamCons(consesOfStatic);
            }
            for (ClassInfo child : children) {
                consesOfStatic.addAll(child.getConsesOfStatic());
            }
            if (!consesOfStatic.isEmpty()) {
                //has constructor of this subType.
                System.out.println(sig + " has static constructor of subType.");
                return getMinParamCons(consesOfStatic);
            }

        }
        //find new constructor of this type.
        if (this.isConcrete) {
            List<MethodInfo> consesOfnew = getConsesOfnew();
            if (!consesOfnew.isEmpty()) {
                //has constructor of this type.
//                System.out.println(sig + " has new constructor.");
                return getMinParamCons(consesOfnew);

            }
        }
        //find new constructor of subType.
        List<MethodInfo> consesOfnew = new ArrayList<MethodInfo>();
        for (ClassInfo child : children) {
            if (child.isConcrete) {
                consesOfnew.addAll(consesOfnew);
            }
        }
        if (!consesOfnew.isEmpty()) {
            //has new constructor of subType.
            System.out.println(sig + " has new constructor of subType.");
            return getMinParamCons(consesOfnew);

        }
        //		String typeToCons = getType2Cons();
        //		System.out.println("best consType for " + sig + " is " + typeToCons);
        //		if (typeToCons == null) {
        //			return null;
        //		}
        //		//public new
        //		List<MethodInfo> consesOfNew = ProjectInfo.i().getClassVO(typeToCons).getConsesOfnew();
        //		//		System.out.println(notStaticConses.size());
        //		if (!consesOfNew.isEmpty()) {
        //			return getMinParamCons(consesOfNew);
        //		}

        return null;
    }

    /**
     * type of this object may be not the type of construct.
     * if this type is abstract,the type to construct should be its subType.
     * get all construct
     *
     * @param
     * @return
     */
    public List<MethodInfo> getAllConstructor(boolean scanStatic) {
        //find static constructor.
        if (scanStatic) {
            List<MethodInfo> consesOfStatic = getConsesOfStatic();

            if (this.isConcrete && !consesOfStatic.isEmpty()) {
                //this class is concrete and has constructor of this type.
//                System.out.println(sig + " has static constructor.");
                return consesOfStatic;
            }
            for (ClassInfo child : children) {
                consesOfStatic.addAll(child.getConsesOfStatic());
            }
            if (!consesOfStatic.isEmpty()) {
                //has constructor of this subType.
//                System.out.println(sig + " has static constructor of subType.");
                return consesOfStatic;
            }

        }
        //获取所有子类的构造方法，不仅限于此类如果是具体的就只返回此类中的构造方法
//        find new constructor of this type.
        if (this.isConcrete) {
            List<MethodInfo> consesOfnew = getConsesOfnew();
            if (!consesOfnew.isEmpty()) {
                //has constructor of this type.
//                System.out.println(sig + " has new constructor.");
                return consesOfnew;

            }
        }
        //find new constructor of subType.
        List<MethodInfo> consesOfnew = new ArrayList<MethodInfo>();
//        System.out.println(children.size());
        for (ClassInfo child : children) {
            if (child.isConcrete) {
                consesOfnew.addAll(child.getConsesOfnew());
            }
        }
        if (!consesOfnew.isEmpty()) {
            //has new constructor of subType.
//            System.out.println(sig + " has new constructor of subType.");
            return consesOfnew;

        }
        //		String typeToCons = getType2Cons();
        //		System.out.println("best consType for " + sig + " is " + typeToCons);
        //		if (typeToCons == null) {
        //			return null;
        //		}
        //		//public new
        //		List<MethodInfo> consesOfNew = ProjectInfo.i().getClassVO(typeToCons).getConsesOfnew();
        //		//		System.out.println(notStaticConses.size());
        //		if (!consesOfNew.isEmpty()) {
        //			return getMinParamCons(consesOfNew);
        //		}

        return null;
    }

    /**
     * type of this object may be not the type of construct.
     * if this type is abstract,the type to construct should be its subType.
     * get all construct contains static and children
     *
     * @param
     * @return
     */
    public List<MethodInfo> getAllConstructorContainsChildren() {
        //find static constructor.
        List<MethodInfo> allMethodInfos = new ArrayList<>();

        if (this.isConcrete) {
            allMethodInfos.addAll(getConsesOfStatic());
            allMethodInfos.addAll(getConsesOfnew());
        }

        //add all children static concrete method and concrete
        for (ClassInfo child : children) {
            if (child.isConcrete) {
                allMethodInfos.addAll(child.getConsesOfStatic());
                allMethodInfos.addAll(child.getConsesOfnew());
            }
        }
        return allMethodInfos;
    }

    /**
     * @return static constructor of this type.
     */
    private List<MethodInfo> getConsesOfStatic() {
        List<MethodInfo> consOfStatic = new ArrayList<MethodInfo>();
        for (MethodInfo method : ProjectInfo.i().getAllMethod()) {
            if (method.getReturnType().equals(sig) && method.isStatic() && ProjectInfo.i().isInvokeable(method)) {
                //				System.out.println("find cons:" + method.getSig());
                consOfStatic.add(method);
            }
        }

        //		for (MethodInfo method : ProjectInfo.i().getAllMethod()) {
        //			//if this type and entryClass is on same package,get default and protected static construct.
        ////			if(method.getCls().getSig().equals("org.apache.http.impl.client.CloseableHttpResponseProxy")) {
        ////				System.out.println("isEntry:"+ProjectInfo.i().isEntryPck(SootUtil.cls2pck(method.getCls().getSig(), ".")));
        ////			}
        //			if (ProjectInfo.i().isEntryPck(SootUtil.cls2pck(method.getCls().getSig(), ".")))
        //				if (method.getReturnType().equals(sig) && method.isStatic() && !method.isPrivate()) {
        //					consOfStatic.add(method);
        //				}
        //		}
        return consOfStatic;
    }

    /**
     * @return new constructor of this type.
     */
    private List<MethodInfo> getConsesOfnew() {
        List<MethodInfo> publicCons = new ArrayList<MethodInfo>();
        //public constructor
        for (MethodInfo methodInfo : mthds) {
            if (methodInfo.isConstructor() && methodInfo.isPublic()) {
                publicCons.add(methodInfo);
            }
        }
        //if this type and entryClass is on same package,get default and protected new construct.
        if (ProjectInfo.i().isEntryPck(SootUtil.cls2pck(sig, "."))) {
            for (MethodInfo methodInfo : mthds) {
                if (methodInfo.isConstructor() && !methodInfo.isPrivate()) {
                    //去重
                    if (publicCons.contains(methodInfo)) {
                        continue;
                    }
                    publicCons.add(methodInfo);
                }
            }
        }

        return publicCons;
    }

    /**
     * return constructor that has minimum parameter.
     *
     * @param conses
     * @return
     */
    private MethodInfo getMinParamCons(List<MethodInfo> conses) {
        MethodInfo minParamCons = null;
        int paraNum = Integer.MAX_VALUE;
        for (MethodInfo cons : conses) {
            if (cons.getParamNum() < paraNum) {
                paraNum = cons.getParamNum();
                minParamCons = cons;
            }
        }
        return minParamCons;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sig == null) ? 0 : sig.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ClassInfo other = (ClassInfo) obj;
        if (sig == null) {
            if (other.sig != null) {
                return false;
            }
        } else if (!sig.equals(other.sig)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("info for ");
        sb.append(sig);
        sb.append(" isConcrete:");
        sb.append(isConcrete);
        sb.append(System.lineSeparator());

        for (ClassInfo child : children) {
            sb.append("child:");
            sb.append(child.getSig());
            sb.append(" isConcrete:");
            sb.append(child.isConcrete);
            sb.append(System.lineSeparator());
        }
        for (MethodInfo mthd : mthds) {
            sb.append("mthd:");
            sb.append(mthd.getSig());
            sb.append(" isConstructor:");
            sb.append(mthd.isConstructor());
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

}
