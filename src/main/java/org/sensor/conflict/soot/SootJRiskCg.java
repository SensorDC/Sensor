package org.sensor.conflict.soot;

import java.util.Arrays;
import java.util.List;

import org.sensor.conflict.GlobalVar;
import org.sensor.conflict.graph.IGraph;
import org.sensor.conflict.risk.jar.DepJarJRisk;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.util.SootUtil;
import org.sensor.conflict.vo.DepJar;
import org.sensor.conflict.soot.tf.JRiskCgTf;
import org.sensor.conflict.vo.DupClsJarPair;
import soot.PackManager;
import soot.Transform;

public class SootJRiskCg extends SootAna {
    private static SootJRiskCg instance = new SootJRiskCg();

    private SootJRiskCg() {

    }

    public static SootJRiskCg i() {
        return instance;
    }

    public IGraph getGraph(DepJarJRisk depJarJRisk, JRiskCgTf transformer) {
        MavenUtil.i().getLog().info("use soot to compute reach methods for " + depJarJRisk.toString());
        IGraph graph = null;
        long start = System.currentTimeMillis();
        try {

            SootUtil.modifyLogOut();

            PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", transformer));

            soot.Main.main(getArgs(depJarJRisk.getPrcDirPaths().toArray(new String[0])).toArray(new String[0]));

            graph = transformer.getGraph();

        } catch (Exception e) {
            MavenUtil.i().getLog().warn("cg error: ", e);
        }
        soot.G.reset();
        long runtime = (System.currentTimeMillis() - start) / 1000;
        GlobalVar.time2cg += runtime;
        return graph;
    }

    public IGraph getGraph(DupClsJarPair dupClsJarPair, JRiskCgTf transformer) {
        MavenUtil.i().getLog().info("use soot to compute reach methods for " + dupClsJarPair.toString());
        IGraph graph = null;
        long start = System.currentTimeMillis();
        try {

            SootUtil.modifyLogOut();

            PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", transformer));

            soot.Main.main(getArgs(dupClsJarPair.getPrcDirPaths().toArray(new String[0])).toArray(new String[0]));

            graph = transformer.getGraph();

        } catch (Exception e) {
            MavenUtil.i().getLog().warn("cg error: ", e);
        }
        soot.G.reset();
        long runtime = (System.currentTimeMillis() - start) / 1000;
        GlobalVar.time2cg += runtime;
        return graph;
    }

    public IGraph getGraph(String[] jarFilePaths, JRiskCgTf transformer) {
//		MavenUtil.i().getLog().info("use soot to compute reach methods for " + depJarJRisk.toString());
        IGraph graph = null;
        long start = System.currentTimeMillis();
        try {

            SootUtil.modifyLogOut();

            PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", transformer));

            soot.Main.main(getArgs(jarFilePaths).toArray(new String[0]));

            graph = transformer.getGraph();

        } catch (Exception e) {
            MavenUtil.i().getLog().warn("cg error: ", e);
        }
        soot.G.reset();
        long runtime = (System.currentTimeMillis() - start) / 1000;
        GlobalVar.time2cg += runtime;
        return graph;
    }

    public IGraph getGraph(DepJar entryDepJar, JRiskCgTf transformer) {
        MavenUtil.i().getLog().info("use soot to form methods graph for " + entryDepJar.toString());
        IGraph graph = null;
        long start = System.currentTimeMillis();
        try {

            SootUtil.modifyLogOut();

            PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", transformer));

//			if(needParentDepJar) {
//				soot.Main.main(getArgs(entryDepJar.getAllParentJarClassPaths(true).toArray(new String[0])).toArray(new String[0]));
//			}else {

//            soot.Main.main(getArgs(entryDepJar.getJarFilePaths(true).toArray(new String[0])).toArray(new String[0]));
            soot.Main.main(getArgs(entryDepJar.getPrcDirPaths().toArray(new String[0])).toArray(new String[0]));
//			}

            graph = transformer.getGraph();

        } catch (Exception e) {
            MavenUtil.i().getLog().warn("cg error: ", e);
        }
        soot.G.reset();
        long runtime = (System.currentTimeMillis() - start) / 1000;
        GlobalVar.time2cg += runtime;
        return graph;
    }

    @Override
    protected void addCgArgs(List<String> argsList) {
        argsList.addAll(Arrays.asList(new String[]{"-p", "cg", "off",}));
    }
}


