package neu.lab.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import neu.lab.conflict.util.Conf;
import neu.lab.conflict.writer.CountProjectWriter;

@Mojo(name = "countProject", defaultPhase = LifecyclePhase.VALIDATE)
public class CountProjectMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
//		new CountProjectWriter().writeForRiskMethodInProject(Conf.outDir);
		new CountProjectWriter().writeTofileForSourceObjectCount(Conf.outDir);
	}

}
