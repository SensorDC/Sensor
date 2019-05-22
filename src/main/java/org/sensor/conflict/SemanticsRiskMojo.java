package org.sensor.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.sensor.conflict.util.Conf;
import org.sensor.conflict.writer.SemanticsRiskWriter;

@Mojo(name = "semanticsRisk", defaultPhase = LifecyclePhase.VALIDATE)
public class SemanticsRiskMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		new SemanticsRiskWriter().writeSemanticsRiskToFile(Conf.outDir);
	}

}
