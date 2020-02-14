package org.sensor.conflict.soot.tf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensor.conflict.GlobalVar;
import org.sensor.conflict.graph.Graph4distance;
import org.sensor.conflict.graph.Node4distance;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.MethodCall;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JIfStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class JRiskDistanceCgTf extends JRiskCgTf {

    private static CallGraph instance = null;
//	private static Iterator<Edge> iterator = null;

    public JRiskDistanceCgTf(DepJarJRisk depJarJRisk) {
        super(depJarJRisk);
    }

    /**
     * 重构函数
     *
     * @param depJarJRisk
     */
    public JRiskDistanceCgTf(DepJarJRisk depJarJRisk, Set<String> thrownmethods) {
        super(depJarJRisk, thrownmethods);
    }

    public static CallGraph getThisCallGraph() {
        if (instance == null) {
            instance = Scene.v().getCallGraph();//得到图
        }
        return instance;
    }

    /**
     * 产生图，其中不包含JavaLib的方法，同时不包含conflictJar的方法
     */
    protected void formGraph() {
        if (graph == null) {
            MavenUtil.i().getLog().info("start form graph...");
            // get call-graph.
            Map<String, Node4distance> name2node = new HashMap<String, Node4distance>();
            List<MethodCall> mthdRlts = new ArrayList<MethodCall>();

            CallGraph cg = Scene.v().getCallGraph();//得到图 优化效率从这里开始

            Iterator<Edge> ite = cg.iterator();

            while (ite.hasNext()) {
                Edge edge = ite.next();

                String srcMthdName = edge.src().getSignature();//源方法名
                String tgtMthdName = edge.tgt().getSignature();//目标方法名

                String srcMethodNameASMsignature = edge.src().getBytecodeSignature();
                String tgtMethodNameASMsignature = edge.tgt().getBytecodeSignature();

                String srcClsName = edge.src().getDeclaringClass().getName();//源方法的类名
                String tgtClsName = edge.tgt().getDeclaringClass().getName();//目标方法的类名

                if (edge.src().isJavaLibraryMethod() || edge.tgt().isJavaLibraryMethod()) {
                    // filter relation contains javaLibClass 过滤掉JavaLib的类
                } else if (conflictJarClses.contains(SootUtil.mthdSig2cls(srcMthdName))
                        && conflictJarClses.contains(SootUtil.mthdSig2cls(tgtMthdName))) {
                    // filter relation inside conflictJar 过滤掉conflictJar中的类
                } else {
                    if (!name2node.containsKey(srcMthdName)) {
                        name2node.put(srcMethodNameASMsignature, new Node4distance(srcMethodNameASMsignature, isHostClass(srcClsName) && !edge.src().isPrivate(),
                                riskMthds.contains(srcMthdName), getBranchNum(srcMethodNameASMsignature)));
                    }
                    if (!name2node.containsKey(tgtMthdName)) {
                        name2node.put(tgtMethodNameASMsignature, new Node4distance(tgtMethodNameASMsignature, isHostClass(tgtClsName) && !edge.tgt().isPrivate(),
                                riskMthds.contains(tgtMthdName), getBranchNum(tgtMethodNameASMsignature)));
                    }
//                    mthdRlts.add(new MethodCall(srcMthdName, tgtMthdName));
                    mthdRlts.add(new MethodCall(srcMethodNameASMsignature, tgtMethodNameASMsignature));
                }
            }
//			System.out.println(mthdRlts.size());
            graph = new Graph4distance(name2node, mthdRlts);
            MavenUtil.i().getLog().info("end form graph.");
        }
    }

    private int getBranchNum(String mthd) {
        Integer branchNum = mthd2branch.get(mthd);
        if (null != branchNum)
            return branchNum;
        return 0;
    }

    private int calBranchNum(SootMethod sootMethod) {
        long startTime = System.currentTimeMillis();
        int cnt = 0;
        if (sootMethod.getSource() == null) {

        } else {
            Body body = sootMethod.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                if (isBranchNode(unit)) {
                    cnt++;
                }
            }
        }
        long runtime = (System.currentTimeMillis() - startTime) / 1000;
        GlobalVar.branchTime += runtime;
        return cnt;
    }

    private boolean isBranchNode(Unit unit) {
        if (unit instanceof soot.jimple.internal.JIfStmt) {
            JIfStmt ifS = (JIfStmt) unit;
            if (!ifS.getTargetBox().getUnit().branches()) {
                return true;
            }
        }
        if (unit instanceof soot.jimple.internal.AbstractSwitchStmt) {
            return true;
        }
        return false;
    }

    @Override
    protected void initMthd2branch() {
        mthd2branch = new HashMap<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            List<SootMethod> mthds = new ArrayList<SootMethod>();
            mthds.addAll(sootClass.getMethods());
            for (SootMethod method : mthds) {
//                mthd2branch.put(method.getSignature(), calBranchNum(method));
                mthd2branch.put(method.getBytecodeSignature(), calBranchNum(method));
            }
        }
    }
}
