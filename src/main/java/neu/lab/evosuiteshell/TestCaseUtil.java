package neu.lab.evosuiteshell;

import java.io.File;
import java.util.HashSet;

public class TestCaseUtil {
	public static boolean removeFileDir(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				removeFileDir(f);
			}
		}
		return file.delete();
	}

	public static HashSet<String> getFiles(String dir) {
		HashSet<String> paths = new HashSet<String>();
		File base = new File(dir);
		if (base.isDirectory()) {
			for (File file : base.listFiles()) {
				if (file.isDirectory()) {
					paths.addAll(getFiles(file.getAbsolutePath()));
				} else {
					paths.add(file.getAbsolutePath());
				}
			}
		}
		return paths;
	}

	public static void main(String[] args) {
		for (String path : getFiles("C:\\Users\\Flipped\\eclipse-workspace\\Host\\src")) {
			if (path.endsWith(".java"))
				System.out.println(path);
		}

	}
}
