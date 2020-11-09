package neu.lab.conflict;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import neu.lab.conflict.util.Conf;
import neu.lab.conflict.writer.RepairWriter;

@Mojo(name = "conflictRepair", defaultPhase = LifecyclePhase.VALIDATE)
public class ConflictRepairMojo extends ConflictMojo {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		RepairWriter repairWriter = new RepairWriter();
		repairWriter.write(Conf.outDir);	//服务器
	}

}
