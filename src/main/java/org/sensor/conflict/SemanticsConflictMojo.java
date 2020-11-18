package org.sensor.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.sensor.conflict.util.Conf;
import org.sensor.conflict.writer.SemanticsConflictWriter;

@Mojo(name = "semanticsConflict", defaultPhase = LifecyclePhase.VALIDATE)
public class SemanticsConflictMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
//		SemanticsConflictWriter.outPath = Conf.outDir;
//		new SemanticsConflictWriter().writeSemanticsConflict();

		new SemanticsConflictWriter().writeSemanticsConflict(Conf.outDir);
	}
}
