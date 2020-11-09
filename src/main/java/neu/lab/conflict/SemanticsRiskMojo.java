package neu.lab.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import neu.lab.conflict.util.Conf;
import neu.lab.conflict.writer.SemanticsRiskWriter;

@Mojo(name = "semanticsRisk", defaultPhase = LifecyclePhase.VALIDATE)
public class SemanticsRiskMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		new SemanticsRiskWriter().writeSemanticsRiskToFile(Conf.outDir);
	}

}
