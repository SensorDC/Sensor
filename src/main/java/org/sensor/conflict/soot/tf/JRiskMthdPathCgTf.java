package org.sensor.conflict.soot.tf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensor.conflict.graph.Graph4path;
import org.sensor.conflict.graph.Node4path;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.DupClsJarPair;
import org.sensor.conflict.vo.MethodCall;
import soot.Scene;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class JRiskMthdPathCgTf extends JRiskCgTf {

    public JRiskMthdPathCgTf(DepJarJRisk depJarJRisk) {
        super(depJarJRisk);
    }

    public JRiskMthdPathCgTf(Set<String> entryMethods) {
        super(entryMethods);
    }

    public JRiskMthdPathCgTf(DepJarJRisk depJarJRisk, Set<String> entryMethods) {
        super(depJarJRisk, entryMethods);
    }

    public JRiskMthdPathCgTf(DupClsJarPair dupClsJarPair, Set<String> entryMethods) {
        super(dupClsJarPair, entryMethods);
    }
    public JRiskMthdPathCgTf(DepJarJRisk depJarJRisk, boolean filterusedDepJarParent, Set<String> entryMethods) {
        super(depJarJRisk, depJarJRisk.getUsedDepJar().getAllParentDepJar(), entryMethods);
    }

    @Override
    protected void formGraph() {
        if (graph == null) {
            MavenUtil.i().getLog().info("start form graph...");
            // get call-graph.
            Map<String, Node4path> name2node = new HashMap<String, Node4path>();
            List<MethodCall> mthdRlts = new ArrayList<MethodCall>();
            Map<String, String> methodMappingASMMethod = new HashMap<>();

            CallGraph cg = Scene.v().getCallGraph();

            Iterator<Edge> ite = cg.iterator();
            while (ite.hasNext()) {
                Edge edge = ite.next();

                String srcMthdName = edge.src().getSignature();
                String tgtMthdName = edge.tgt().getSignature();

                String srcMethodNameASMsignature = edge.src().getBytecodeSignature();
                String tgtMethodNameASMsignature = edge.tgt().getBytecodeSignature();
                // //TODO1
                // if("<com.fasterxml.jackson.core.JsonFactory: boolean
                // requiresPropertyOrdering()>".equals(tgtMthdName)) {
                // MavenUtil.i().getLog().info("srcMthdName:"+srcMthdName);
                // }
                String srcClsName = edge.src().getDeclaringClass().getName();
                String tgtClsName = edge.tgt().getDeclaringClass().getName();
                if (edge.src().isJavaLibraryMethod() || edge.tgt().isJavaLibraryMethod()) {
                    // filter relation contains javaLibClass
//				} else if (usedJarClses.contains(SootUtil.mthdSig2cls(srcMthdName))
//						&& usedJarClses.contains(SootUtil.mthdSig2cls(tgtMthdName))) {
//					 filter relation inside conflictJar
                } else if (conflictJarClses.contains(SootUtil.mthdSig2cls(srcMthdName))
                        && conflictJarClses.contains(SootUtil.mthdSig2cls(tgtMthdName))) {
                    // filter relation inside conflictJar 过滤掉conflictJar中的类
                } else {
                    if (edge.src().isConcrete() || edge.tgt().isConcrete()) {
//						if (riskMthds.contains(srcMthdName)) {
//							System.out.println(edge.src().getSignature());
//							System.out.println(edge.src().getActiveBody().getAllUnitBoxes());
//						}
                        if (!name2node.containsKey(srcMthdName)) {
                            name2node.put(srcMethodNameASMsignature,
                                    new Node4path(srcMethodNameASMsignature, isHostClass(srcClsName) && !edge.src().isPrivate(),
                                            riskMthds.contains(srcMthdName)));
                        }
                        if (!name2node.containsKey(tgtMthdName)) {
                            name2node.put(tgtMethodNameASMsignature,
                                    new Node4path(tgtMethodNameASMsignature, isHostClass(tgtClsName) && !edge.tgt().isPrivate(),
                                            riskMthds.contains(tgtMthdName)));
                        }
                        mthdRlts.add(new MethodCall(srcMethodNameASMsignature, tgtMethodNameASMsignature));
                        //保存 risk method 的 asm版本
                        if (riskMthds.contains(srcMthdName)) {
                            methodMappingASMMethod.put(srcMethodNameASMsignature, srcMthdName);
                        } else if (riskMthds.contains(tgtMthdName)) {
                            methodMappingASMMethod.put(tgtMethodNameASMsignature, tgtMthdName);
                        }
                    }
                }
            }
            graph = new Graph4path(name2node, mthdRlts, methodMappingASMMethod);
            MavenUtil.i().getLog().info("end form graph.");
        }
    }

    @Override
    protected void initMthd2branch() {

    }

}
