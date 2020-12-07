package org.sensor.evosuiteshell.search;

import java.io.File;
import java.util.*;

import org.sensor.conflict.soot.JarAna;
import soot.PackManager;
import soot.Transform;

public class SootExe {
    public void initProjectInfo(String[] jarFilePath) {
//		System.out.println("jarFilePath size"+ jarFilePath);
        List<String> args = getArgs(jarFilePath);
        Set<String> classesSig = JarAna.i().deconstruct(Arrays.asList(jarFilePath)).keySet();
//		System.out.println("args size"+ args);
        if (args.size() != 0) {

            ClassTransformer transformer = new ClassTransformer(classesSig);

            PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", transformer));

            soot.Main.main(args.toArray(new String[0]));

            soot.G.reset();

        }
    }

    protected List<String> getArgs(String[] jarFilePaths) {
        List<String> argsList = new ArrayList<String>();
        addClassPath(argsList, jarFilePaths);
        if (argsList.size() == 0) {// this class can't analysis
            return argsList;
        }

        addGenArg(argsList);
        addCgArgs(argsList);
        addIgrArgs(argsList);

        return argsList;
    }

    protected void addCgArgs(List<String> argsList) {
        argsList.addAll(Arrays.asList(new String[]{"-p", "cg", "off",}));
    }

    protected void addClassPath(List<String> argsList, String[] jarFilePaths) {
        for (String jarFilePath : jarFilePaths) {
            if (new File(jarFilePath).exists()) {
                if (canAna(jarFilePath)) {
                    argsList.add("-process-dir");
                    argsList.add(jarFilePath);
                } else {
                    System.out.println("add classpath error:can't analysis file " + jarFilePath);
                }
            } else {
                System.out.println("add classpath error:doesn't exist file " + jarFilePath);
            }

        }
    }

    private boolean canAna(String jarFilePath) {
//		return true;
        if (jarFilePath.contains("\\asm\\") && jarFilePath.contains("6")) {
            return false;
        }
        return true;
    }

    protected void addGenArg(List<String> argsList) {

        argsList.add("-ire");
        argsList.add("-app");
        argsList.add("-allow-phantom-refs");
        argsList.add("-w");

    }

    protected void addIgrArgs(List<String> argsList) {
        argsList.addAll(Arrays.asList(new String[]{"-p", "wjop", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-p", "wjap", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-p", "jtp", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-p", "jop", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-p", "jap", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-p", "bb", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-p", "tag", "off",}));
        argsList.addAll(Arrays.asList(new String[]{"-f", "n",}));
    }

//	public static void main(String[] args) throws IOException {
//        new SootExe().initProjectInfo(
//                new String[]{"/Users/wangchao/eclipse-workspace/Host/target/Host-1.0.jar"});
//        ProjectInfo.i().setEntryCls("neu.lab.Host.Host");
//        MyClassLoader.jarLoader(new File("/Users/wangchao/eclipse-workspace/Host/target/Host-1.0.jar"));
////		System.out.println(ProjectInfo.i().getClassInfo("neu.lab.A.Principal"));
//        GenericObjectSet.getInstance().generateGenericObject("neu.lab.Host.Host");
////		PrintWriter printer = new PrintWriter(new BufferedWriter(
////				new FileWriter("/Users/wangchao/个人文件/东北大学/实验室/实验室台式/eclipse/Host/target/1.txt", false)));
////		printer.println(ProjectInfo.i().getAllClassInfo());
////		printer.close();
//    }

}
