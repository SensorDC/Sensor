package neu.lab.evosuiteshell.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import neu.lab.evosuiteshell.TestCaseUtil;

public class SearchPrimitiveManager {
	private static SearchPrimitiveManager instance = null;

	private SearchPrimitiveManager() {
		init();
	}

	public static SearchPrimitiveManager getInstance() {
		if (instance == null)
			instance = new SearchPrimitiveManager();
		return instance;
	}

	private void init() {
		getValueFromJavaFile();
	}

	private void getValueFromJavaFile() {
		String dir = System.getProperty("user.dir") + "\\src";
		HashSet<String> filesPath = TestCaseUtil.getFiles(dir);
		for (String path : filesPath) {
			if (path.endsWith(".java")) {
				search(path);
			}
		}
	}

	public void search(String path) {
		File file = new File(path);
		String fileName = file.getName().split("\\.")[0];
		if (fileName.endsWith("Test"))
			fileName = fileName.replace("Test", "");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				HashSet<String> result = matchString(line);
				if (result.size() > 0)
					SearchConstantPool.getInstance().setPool(fileName, result);
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HashSet<String> matchString(String line) {
		HashSet<String> result = new HashSet<String>();
		Pattern pattern = Pattern.compile("(?<=\").*?(?=\")");// 匹配双引号中的内容
		Matcher matcher = pattern.matcher(line);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	public static void main(String[] args) {
		SearchPrimitiveManager.getInstance().search("C:\\Users\\Flipped\\eclipse-workspace\\Host\\src");
		System.out.println(SearchConstantPool.getInstance().getPoolValues("Host1"));
	}
}
