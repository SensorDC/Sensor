package org.sensor.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.sensor.conflict.util.Conf;
import org.sensor.conflict.writer.VersionWriter;

@Mojo(name = "findVersion", defaultPhase = LifecyclePhase.VALIDATE)
public class FindVersionMojo extends ConflictMojo{

	@Override
	public void run() {
		new VersionWriter().write(Conf.outDir + "projectVersions.txt");
	}

}
