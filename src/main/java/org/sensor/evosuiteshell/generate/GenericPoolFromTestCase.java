package org.sensor.evosuiteshell.generate;

import fj.Hash;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.evosuiteshell.Config;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.instrumentation.InstrumentingClassLoader;
import org.evosuite.seeding.ObjectPool;
import org.evosuite.seeding.ObjectPoolManager;
import org.evosuite.utils.generic.GenericClass;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class GenericPoolFromTestCase {

    private static final org.evosuite.testcarver.extraction.CarvingClassLoader classLoader = new org.evosuite.testcarver.extraction.CarvingClassLoader();

    private static InstrumentingClassLoader instrumentingClassLoader = TestGenerationContext.getInstance().getClassLoaderForSUT();

    /**
     * 通过完全限定名来定位相应的测试类位置，并从测试用例中获取相对应的对象池数据
     *
     * @param fullyQualifiedName 完全限定名
     *                           neu.lab.evosuiteshell.generate.GenericPoolFromTestCase
     */
    public static void receiveTargetClass(String fullyQualifiedName) {
        File dir = new File(Config.PROJECT_TESTCASE_DIR);
        if (!dir.exists()) {
            MavenUtil.i().getLog().info("project does not have test case");
            return;
        }
        String packageName = fullyQualifiedName.substring(0, fullyQualifiedName.lastIndexOf('.') + 1);
        String className = fullyQualifiedName.replace(packageName, "");
        String packagePath = dir.getPath() + "/" + packageName.replace('.', '/');
        File packageDir = new File(packagePath);
        if (!packageDir.exists()) {
            MavenUtil.i().getLog().info("project does not have " + fullyQualifiedName + " test case");
            return;
        }
        File[] testFiles = packageDir.listFiles();
        if (testFiles == null) return;
        for (File testFile : testFiles) {
            if (testFile.getName().contains(className)) {
                String testName = testFile.getName().replace(".java", "");
                genericPool(packageName, className, testName);
            }
        }
    }


    private static void genericPool(String packageName, String className, String testName) {
        String targetClass = packageName + className;
        String selectedJunit = packageName + testName;
        Properties.TARGET_CLASS = targetClass;
        Properties.SELECTED_JUNIT = selectedJunit;

        Properties.CLASSPATH = Properties.CP.split(Config.CLASSPATH_SEPARATOR);
        Properties.SOURCEPATH = Properties.CLASSPATH;

        Class<?> objectClass;
        Class<?> objectTestClass;
        try {
            objectClass = instrumentingClassLoader.loadClass(targetClass);
            objectTestClass = classLoader.loadClass(selectedJunit);
        } catch (Exception e) {
            MavenUtil.i().getLog().error("load class error " + e.getMessage());
//            e.printStackTrace();
            return;
        }
        ObjectPool pool = ObjectPool.getPoolFromJUnit(new GenericClass(objectClass), objectTestClass);
//        MavenUtil.i().getLog().info("success get pool. size : " + pool.getNumberOfSequences());
        ObjectPoolManager.getInstance().addPool(pool);
    }


    /**
     * 不好用
     */
    @Deprecated
    public static void genericStringPool() {
        Properties.TARGET_CLASS = "java.lang.String";
        File dir = new File(Config.PROJECT_TESTCASE_DIR);
        if (!dir.exists()) {
            MavenUtil.i().getLog().info("project does not have test case");
            return;
        }
        Set<String> fileAbsolutePathSet = new HashSet<>();
        forEachTestDir(dir, fileAbsolutePathSet);
        for (String fileAbsolutePath : fileAbsolutePathSet) {
            String TestFullyQualifiedName = fileAbsolutePath.replace(Config.PROJECT_TESTCASE_DIR, "").replace(".java", "").replace("/", ".");
            Properties.SELECTED_JUNIT = TestFullyQualifiedName;
            Class<?> objectTestClass;
            try {
                objectTestClass = classLoader.loadClass(TestFullyQualifiedName);
            } catch (Exception e) {
                MavenUtil.i().getLog().error("load class error " + e.getMessage());
                return;
            }
            ObjectPool pool = ObjectPool.getPoolFromJUnit(new GenericClass(String.class), objectTestClass);
            if (pool.getNumberOfSequences() > 0) {
                MavenUtil.i().getLog().info("success get pool. size : " + pool.getNumberOfSequences());
                ObjectPoolManager.getInstance().addPool(pool);
            }

        }
    }

    private static void forEachTestDir(File dir, Set<String> fileAbsolutePathSet) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                forEachTestDir(file, fileAbsolutePathSet);
            }
            if (file.getName().contains("Test")) {
                fileAbsolutePathSet.add(file.getAbsolutePath());
            }
        }
    }

//    public static void main(String[] args) {
//        Properties.CP = "/Users/wangchao/IdeaProjects/X/target/test-classes/:/Users/wangchao/IdeaProjects/X/target/classes/";
//        receiveTargetClass("neu.lab.evosuiteshell.generate.GenericPoolFromTestCase");
//    }
}
