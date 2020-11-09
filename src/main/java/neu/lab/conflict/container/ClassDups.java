package neu.lab.conflict.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import neu.lab.conflict.container.DepJars;
import neu.lab.conflict.risk.jar.DepJarJRisk;
import neu.lab.conflict.vo.ClassDup;
import neu.lab.conflict.vo.DepJar;

public class ClassDups {
	private List<ClassDup> container;

	public ClassDups(DepJars depJars) {
		container = new ArrayList<ClassDup>();
		for (DepJar depJar : depJars.getAllDepJar()) {
			if (depJar.isSelected()) {
				for (String cls : depJar.getAllCls(false)) {
					addCls(cls, depJar);
				}
			}
		}
		Iterator<ClassDup> ite = container.iterator();
		while (ite.hasNext()) {
			ClassDup conflict = ite.next();
			if (!conflict.isDup()) {// delete conflict if there is only one version
				ite.remove();
			} else if (conflict.hasTestScope()) {
				ite.remove();
			}
		}
	}

	public ClassDups(DepJars depJars, DepJarJRisk depJarJRisk) {
		container = new ArrayList<ClassDup>();
		Set<String> thrownClasses = depJarJRisk.getThrownClasses();
		for (String cls : thrownClasses) {
			addCls(cls, depJarJRisk.getConflictDepJar());
		}
		for (DepJar depJar : depJars.getAllDepJar()) {
			if (depJar.isSelected() && !depJar.isSameLib(depJarJRisk.getUsedDepJar())) {
				for (String cls : depJar.getAllCls(false)) {
					addCls(cls, depJar);
				}
			}
		}
		Iterator<ClassDup> ite = container.iterator();
		while (ite.hasNext()) {
			ClassDup conflict = ite.next();
			if (!conflict.isDup()) {// delete conflict if there is only one version
				ite.remove();
			} else if (!conflict.getDepJars().contains(depJarJRisk.getConflictDepJar())) {
				ite.remove();
			} else if (conflict.hasTestScope()) {
				ite.remove();
			}
		}
	}

	public List<ClassDup> getAllClsDup() {
		return container;
	}

	private void addCls(String classSig, DepJar depJar) {
		ClassDup clsDup = null;
		for (ClassDup existDup : container) {
			if (existDup.isSelf(classSig))
				clsDup = existDup;
		}
		if (null == clsDup) {
			clsDup = new ClassDup(classSig);
			container.add(clsDup);
		}
		clsDup.addDepJar(depJar);
	}

}
