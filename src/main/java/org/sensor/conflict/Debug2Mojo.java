package org.sensor.conflict;


import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.sensor.conflict.util.Conf;


@Mojo(name = "debug2", defaultPhase = LifecyclePhase.VALIDATE)
public class Debug2Mojo extends ConflictMojo {

	@Override
	public void run() {
		TestCaseGenerator testCaseGenerator = new TestCaseGenerator(Conf.outDir, append);
		testCaseGenerator.writePath();
	}

}
