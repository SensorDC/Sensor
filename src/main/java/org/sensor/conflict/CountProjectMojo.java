package org.sensor.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.sensor.conflict.util.Conf;
import org.sensor.conflict.writer.CountProjectWriter;

@Mojo(name = "countProject", defaultPhase = LifecyclePhase.VALIDATE)
public class CountProjectMojo extends ConflictMojo {

    @Override
    public void run() {
        // TODO Auto-generated method stub

//        new CountProjectWriter().writeForRiskMethodInProject(Conf.outDir);
//		new CountProjectWriter().writeTofileForSourceObjectCount(Conf.outDir);
//        new CountProjectWriter().writeToFileForCountInfo(Conf.outDir);
        new CountProjectWriter().writeDependencyCountInfo(Conf.outDir);
    }

}
