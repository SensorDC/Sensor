package neu.lab.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import neu.lab.conflict.util.Conf;
import neu.lab.conflict.writer.RiskLevelWriter;

@Mojo(name = "printRiskLevel", defaultPhase = LifecyclePhase.VALIDATE)
public class PrintRiskLevelMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		RiskLevelWriter riskLevelWriter = new RiskLevelWriter();
		riskLevelWriter.writeRiskLevelXML(Conf.outDir, append, subdivisionLevel);	//mac下路径
	}

}