package org.sensor.evosuiteshell.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sensor.evosuiteshell.Config;
import org.sensor.evosuiteshell.TestCaseUtil;

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
        String dir = System.getProperty("user.dir") + Config.FILE_SEPARATOR + "src";
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
//        System.out.println(fileName);
//        if (fileName.endsWith("Test"))
//            fileName = fileName.replace("Test", "");
        BufferedReader reader;
//        try {
//            reader = new BufferedReader(new FileReader(file));
//            String line = reader.readLine();
//            while (line != null) {
//                HashSet<String> result = matchString(line);
//                if (result.size() > 0)
//                    SearchConstantPool.getInstance().setPool(fileName, result);
//                line = reader.readLine();
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }


        //精确定位每个类的生成
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("new ")) {
                    matchStringByClass(line);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private HashSet<String> matchString(String line) {
        HashSet<String> result = new HashSet<String>();
        Pattern pattern = Pattern.compile("(?<=\").*?(?=\")");// 匹配双引号中的内容
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private void matchStringByClass(String line) {
        HashSet<String> result = new HashSet<>();

        String className = line.split("new ")[1].split("\\(")[0].replaceAll("<.*?>", "");
//        System.out.println(className);
        Pattern pattern = Pattern.compile("(?<=\\().*?(?=\\))");// 匹配双引号中的内容
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            if (matcher.group().contains("\"")) {
                Pattern patternInner = Pattern.compile("(?<=\").*?(?=\")");// 匹配双引号中的内容
                Matcher matcherInner = patternInner.matcher(line);
                while (matcherInner.find()) {
                    result.add(matcherInner.group());
//                    System.out.println("1" + className);
//                    System.out.println("2" + matcherInner.group());
                }
            }
        }
        if (result.size() > 0)
            SearchConstantPool.getInstance().setPool(className, result);
    }


//    public static void main(String[] args) {
//        SearchPrimitiveManager.getInstance().search("/Users/wangchao/eclipse-workspace/Host/src/test/neu/lab/Host/HostTest.java");
//        System.out.println(SearchConstantPool.getInstance().getPoolValues("Host1"));
//        HashSet<String> filesPath = TestCaseUtil.getFiles("/Users/wangchao/eclipse-workspace/Host/src/");
//        for (String file : filesPath) {
//            SearchPrimitiveManager.getInstance().search(file);
//        }
//        SearchPrimitiveManager.getInstance().matchStringByClass("a new HashSet<>(\"asssffs\")");
//    }
}
