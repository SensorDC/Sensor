package org.sensor.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.sensor.conflict.util.Conf;
import org.sensor.conflict.writer.RiskLevelWriter;

@Mojo(name = "printRiskLevel", defaultPhase = LifecyclePhase.VALIDATE)
public class PrintRiskLevelMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		RiskLevelWriter riskLevelWriter = new RiskLevelWriter();
		riskLevelWriter.writeRiskLevelXML(Conf.outDir, append, subdivisionLevel);	//mac下路径
	}

}