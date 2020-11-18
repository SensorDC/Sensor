package org.sensor.evosuiteshell.generate;

import org.sensor.conflict.CountProjectMojo;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.soot.JarAna;
import org.sensor.conflict.util.Conf;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.evosuiteshell.TestCaseUtil;
import org.sensor.evosuiteshell.search.*;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.instrumentation.InstrumentingClassLoader;
import org.evosuite.seeding.ObjectPool;
import org.evosuite.seeding.ObjectPoolManager;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericClass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GenericObjectSet {

    private InstrumentingClassLoader instrumentingClassLoader = TestGenerationContext.getInstance().getClassLoaderForSUT();

//    private TestCaseBuilder testCaseBuilder;

    private Map<String, List<Integer>> CountClassNum = new HashMap<>();

    public void generateObject(String targetClass) {
        TestCaseBuilder testCaseBuilder = new TestCaseBuilder();
        List<Integer> classNum = CountClassNum.get(targetClass);
        if (classNum == null) {
            classNum = new ArrayList<>();
        }
        ClassInfo targetClassInfo = ProjectInfo.i().getClassInfo(targetClass);
        if (targetClassInfo == null) {
            return;
        }
        ProjectInfo.i().setEntryCls(targetClass);
        //获取所有构造方法包括子类
        List<MethodInfo> methodInfoList = targetClassInfo.getAllConstructorContainsChildren();

        //添加所有返回值为target class的方法
        for (MethodInfo methodInfo : ProjectInfo.i().getAllMethod()) {
//            System.out.println(methodInfo.getSig() + " return : " + methodInfo.getReturnType());
            if (methodInfo.getReturnType().equals(targetClass)) {
                if (methodInfoList.contains(methodInfo)) {
                    continue;
                }
                methodInfoList.add(methodInfo);
            }
        }
        int constructorNum = 0;
        int methodCallNum = 0;
        //methodInfoList 包括所有可用的构造方法和返回值为target class的方法
        for (MethodInfo methodInfo : methodInfoList) {
//            boolean generate = false;
            if (targetClassInfo.getAllConcreteType().contains(methodInfo.getCls().getSig())) {
//                generate = generate(targetClassInfo, methodInfo);
                if (generate(testCaseBuilder, targetClassInfo, methodInfo)) {
                    constructorNum++;
                }
            } else {
                String returnType = methodInfo.getReturnType();
                ClassInfo returnTypeClass = ProjectInfo.i().getClassInfo(returnType);
                if (returnTypeClass.getAllConcreteType().contains(targetClass)) {
//                    generate = generateMethodCall(targetClassInfo, methodInfo);
                    if (generateMethodCall(testCaseBuilder, targetClassInfo, methodInfo)) {
                        methodCallNum++;
                    }
                }
            }
//            if (generate) {
//                num++;
//            }
        }
        classNum.add(constructorNum);
        classNum.add(methodCallNum);
        CountClassNum.put(targetClass, classNum);
    }

    private boolean generateMethodCall(TestCaseBuilder testCaseBuilder, ClassInfo classInfo, MethodInfo methodInfo) {
//        testCaseBuilder = new TestCaseBuilder();
        ClassInfo methodClass = methodInfo.getCls();
        MethodInfo bestConcreteForMethodClass = methodClass.getBestCons(false);
        List<NeededObj> neededParamsForMethodClass = new ArrayList<>();

        if (bestConcreteForMethodClass == null) {
            return false;
        }

        for (String paramType : bestConcreteForMethodClass.getParamTypes()) {
            neededParamsForMethodClass.add(new NeededObj(paramType, 0));

        }
        VariableReference variableReferenceForMethodClass;

        variableReferenceForMethodClass = structureParamTypes(testCaseBuilder, bestConcreteForMethodClass.getCls(), neededParamsForMethodClass, 0);
        if (variableReferenceForMethodClass == null) {
            return false;
        }
        Method method;
        List<VariableReference> variableReferenceList = new ArrayList<VariableReference>();
        List<Class<?>> classList = new ArrayList<Class<?>>();
        try {
            Class<?> methodClazz = instrumentingClassLoader.loadClass(methodClass.getSig());
            List<NeededObj> neededObjList = new ArrayList<>();
            for (String paramType : methodInfo.getParamTypes()) {
                neededObjList.add(new NeededObj(paramType, 0));

            }

            for (NeededObj neededObj : neededObjList) {
                VariableReference variableReference = null;
                Class<?> type = null;
                if (neededObj.isSimpleType()) {
                    switch (neededObj.getClassSig()) {
                        case "boolean":
                            variableReference = testCaseBuilder.appendBooleanPrimitive(Randomness.nextBoolean());
                            type = boolean.class;
                            break;
                        case "byte":
                            variableReference = testCaseBuilder.appendBytePrimitive(Randomness.nextByte());
                            type = byte.class;
                            break;
                        case "char":
                            variableReference = testCaseBuilder.appendCharPrimitive(Randomness.nextChar());
                            type = char.class;
                            break;
                        case "short":
                            variableReference = testCaseBuilder.appendShortPrimitive(Randomness.nextShort());
                            type = short.class;
                            break;
                        case "int":
                            variableReference = testCaseBuilder.appendIntPrimitive(Randomness.nextInt());
                            type = int.class;
                            break;
                        case "long":
                            variableReference = testCaseBuilder.appendLongPrimitive(Randomness.nextLong());
                            type = long.class;
                            break;
                        case "float":
                            variableReference = testCaseBuilder.appendFloatPrimitive(Randomness.nextFloat());
                            type = float.class;
                            break;
                        case "double":
                            variableReference = testCaseBuilder.appendDoublePrimitive(Randomness.nextDouble());
                            type = double.class;
                            break;
                        case "java.lang.String":
                            String paramString = SearchConstantPool.getInstance().getPoolValueRandom(classInfo.getSig().split("\\.")[classInfo.getSig().split("\\.").length - 1]);
                            if (paramString == null) {
                                paramString = Randomness.nextString(1);
                            }
                            variableReference = testCaseBuilder.appendStringPrimitive(paramString);
                            type = String.class;
                            break;
                    }
                    if (variableReference != null) {
                        variableReferenceList.add(variableReference);
                        classList.add(type);
                    }
                } else {//不是简单类型
                    try {
                        type = instrumentingClassLoader.loadClass(neededObj.getClassInfo().getSig());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        MavenUtil.i().getLog().error(e);
                    }
                    classList.add(type);
                    MethodInfo bestConcrete = neededObj.getClassInfo().getBestCons(false);
                    variableReferenceList.add(structureParamTypes(testCaseBuilder, neededObj.getClassInfo(), neededObj.getConsParamObs(bestConcrete), 0));
                }
            }
            method = methodClazz.getDeclaredMethod(methodInfo.getName(), classList.toArray(new Class[]{}));
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        VariableReference variableReference = null;
        try {
            variableReference = testCaseBuilder.appendMethod(variableReferenceForMethodClass, method, variableReferenceList.toArray(new VariableReference[]{}));

        } catch (Throwable e) {
            return false;
        }

        return addSequenceToPool(testCaseBuilder, variableReference, classInfo);
    }


    private boolean generate(TestCaseBuilder testCaseBuilder, ClassInfo classInfo, MethodInfo methodInfo) {
        testCaseBuilder = new TestCaseBuilder();
        List<NeededObj> neededParams = new ArrayList<>();
        for (String paramType : methodInfo.getParamTypes()) {
            neededParams.add(new NeededObj(paramType, 0));
        }
        VariableReference variableReference;
        if (classInfo.hasTargetChildren(methodInfo.getCls())) {
            variableReference = structureParamTypes(testCaseBuilder, methodInfo.getCls(), neededParams, 0);
            // ？用classInfo 还是 methodInfo.getCls()
//            return addSequenceToPool(variableReference, methodInfo.getCls());
        } else {
            variableReference = structureParamTypes(testCaseBuilder, classInfo, neededParams, 0);
//            return addSequenceToPool(variableReference, classInfo);
        }
        if (variableReference == null) {
            return false;
        }
        return addSequenceToPool(testCaseBuilder, variableReference, classInfo);
    }

    private boolean addSequenceToPool(TestCaseBuilder testCaseBuilder, VariableReference variableReference, ClassInfo classInfo) {
        if (variableReference == null) {
            return false;
        } else {
            ObjectPool objectPool = new ObjectPool();
            try {
                objectPool.addSequence(new GenericClass(instrumentingClassLoader.loadClass(classInfo.getSig())), testCaseBuilder.getDefaultTestCase());
            } catch (Throwable e) {
//                e.printStackTrace();
                MavenUtil.i().getLog().error(e);
                return false;
            }
            ObjectPoolManager.getInstance().addPool(objectPool);
//            System.out.println(objectPool.getNumberOfSequences());
            return true;
        }
    }


    private static GenericObjectSet instance = null;

    private GenericObjectSet() {
        //单例模式，只用soot解析一次host包内所有的classinfo和methodinfo
        String hostJarPath = DepJars.i().getHostDepJar().getJarFilePaths(true).toArray(new String[]{})[0];
        new SootExe().initProjectInfo(new String[]{hostJarPath});
    }

    private Set<String> hostClassesSig;

    public GenericObjectSet(String a) {
        hostClassesSig = new HashSet<>();
        //单例模式，只用soot解析一次host包内所有的classinfo和methodinfo
//        String hostJarPath = DepJars.i().getHostDepJar().getJarFilePaths(true).toArray(new String[]{})[0];
        hostClassesSig = JarAna.i().deconstruct(Arrays.asList(a)).keySet();
        new SootExe().initProjectInfo(new String[]{a});
    }

    public static GenericObjectSet getInstance() {
        if (instance == null)
            instance = new GenericObjectSet();
        return instance;
    }


    //构建参数列表
    public VariableReference structureParamTypes(TestCaseBuilder testCaseBuilder, ClassInfo classInfo, List<NeededObj> neededObjList, int depth) {
        if (depth > 2) {
            return null;
        }
        List<VariableReference> variableReferenceList = new ArrayList<VariableReference>();
        List<Class<?>> classList = new ArrayList<Class<?>>();
        for (NeededObj neededObj : neededObjList) {
            VariableReference variableReference = null;
            Class<?> type = null;
            if (neededObj.isSimpleType()) {
                switch (neededObj.getClassSig()) {
                    case "boolean":
                        variableReference = testCaseBuilder.appendBooleanPrimitive(Randomness.nextBoolean());
                        type = boolean.class;
                        break;
                    case "byte":
                        variableReference = testCaseBuilder.appendBytePrimitive(Randomness.nextByte());
                        type = byte.class;
                        break;
                    case "char":
                        variableReference = testCaseBuilder.appendCharPrimitive(Randomness.nextChar());
                        type = char.class;
                        break;
                    case "short":
                        variableReference = testCaseBuilder.appendShortPrimitive(Randomness.nextShort());
                        type = short.class;
                        break;
                    case "int":
                        variableReference = testCaseBuilder.appendIntPrimitive(Randomness.nextInt());
                        type = int.class;
                        break;
                    case "long":
                        variableReference = testCaseBuilder.appendLongPrimitive(Randomness.nextLong());
                        type = long.class;
                        break;
                    case "float":
                        variableReference = testCaseBuilder.appendFloatPrimitive(Randomness.nextFloat());
                        type = float.class;
                        break;
                    case "double":
                        variableReference = testCaseBuilder.appendDoublePrimitive(Randomness.nextDouble());
                        type = double.class;
                        break;
                    case "java.lang.String":
                        String paramString = SearchConstantPool.getInstance().getPoolValueRandom(classInfo.getSig().split("\\.")[classInfo.getSig().split("\\.").length - 1]);
//                        variableReference = testCaseBuilder.appendStringPrimitive(ConstantPoolManager.getInstance().getConstantPool().getRandomString());
                        if (paramString == null) {
                            paramString = Randomness.nextString((int) (Math.random() * 5) + 1);
                        }
                        variableReference = testCaseBuilder.appendStringPrimitive(paramString);
//                        variableReference = testCaseBuilder.appendStringPrimitive("AWS-size");
                        type = String.class;
                        break;
                }
                if (variableReference != null) {
                    variableReferenceList.add(variableReference);
                    classList.add(type);
                }
            } else {//不是简单类型
                try {
                    type = instrumentingClassLoader.loadClass(classInfo.getSig());
                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                    MavenUtil.i().getLog().error(e);
                    return null;
                }
                classList.add(type);
                ClassInfo neededObjClassSig = neededObj.getClassInfo();
                if (neededObjClassSig == null) {
                    return null;
                }
                MethodInfo bestConcrete = neededObjClassSig.getBestCons(false);
                if (bestConcrete == null) {
                    return null;
                }
                variableReferenceList.add(structureParamTypes(testCaseBuilder, neededObj.getClassInfo(), neededObj.getConsParamObs(bestConcrete), depth + 1));
            }
        }
        VariableReference variableReferenceConstructor = null;
        Class<?> clazz = null;
        try {
            clazz = instrumentingClassLoader.loadClass(classInfo.getSig());
//            System.out.println(clazz.getName());
            Constructor<?> con = null;
            try {
                con = clazz.getConstructor(classList.toArray(new Class<?>[]{}));
            } catch (Error e) {
//                System.out.println(e);
                return null;
            }
            variableReferenceConstructor = testCaseBuilder.appendConstructor(con, variableReferenceList.toArray(new VariableReference[]{}));
        } catch (Throwable e) {
//            e.printStackTrace();
//            MavenUtil.i().getLog().error(e);
            return null;
        }

        return variableReferenceConstructor;
    }

    public void generateAllObjectForJar() {
        MavenUtil.i().getLog().info("decompiler......");
        MavenUtil.i().getLog().info("use thread num: " + Conf.nThreads);
        ExecutorService executor = Executors.newFixedThreadPool(Conf.nThreads);
        for (String classSig : hostClassesSig) {
            executor.execute(new Thread(new Runnable() {
                /**
                 * When an object implementing interface <code>Runnable</code> is used
                 * to create a thread, starting the thread causes the object's
                 * <code>run</code> method to be called in that separately executing
                 * thread.
                 * <p>
                 * The general contract of the method <code>run</code> is that it may
                 * take any action whatsoever.
                 *
                 * @see Thread#run()
                 */
                @Override
                public void run() {
                    generateObject(classSig);
                }
            }));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, ConstructionFailedException {
        MavenUtil.i().setMojo(new CountProjectMojo());
        HashSet<String> filesPath = TestCaseUtil.getFiles("/Users/wangchao/eclipse-workspace/Host/src/");
        for (String file : filesPath) {
            SearchPrimitiveManager.getInstance().search(file);
        }
//        String cp = "/Users/wangchao/eclipse-workspace/Host/target/classes";
//        ClassPathHandler.getInstance().addElementToTargetProjectClassPath(cp);
//        Properties.CP = cp;
//        System.out.println("a.b.c.d".split("\\.")["a.b.c.d".split("\\.").length - 1]);
//        String hostJar = "/Users/wangchao/eclipse-workspace/Host/target/Host-1.0.jar";
        String test = "/Users/wangchao/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar";
        ClassPathHandler.getInstance().addElementToTargetProjectClassPath(test);
        Properties.CP = test;
//        new SootExe().initProjectInfo(new String[]{hostJar});
//        System.out.println(ProjectInfo.i().getAllClassInfo().size());
        GenericObjectSet genericObjectSet = new GenericObjectSet(test);
        genericObjectSet.generateAllObjectForJar();
//        genericObjectSet.generateObject("neu.lab.Host.Host");
//        TestCase tc = ObjectPoolManager.getInstance().getRandomSequence(new GenericClass(genericObjectSet.instrumentingClassLoader.loadClass("neu.lab.Host.A")));
//        System.out.println(tc.toCode());
        int countNum = 0;
        int classesNum = 0;
        for (GenericClass genericClass : ObjectPoolManager.getInstance().getClasses()) {
            System.out.println(genericClass.getClassName());
            if (ObjectPoolManager.getInstance().getSequences(genericClass).size() > 0) {
                classesNum++;
                for (TestCase testCase : ObjectPoolManager.getInstance().getSequences(genericClass)) {
                    System.out.println(testCase.toCode());
                    countNum++;
                }
            }
        }
        System.out.println("classes : " + classesNum);
        System.out.println("count : " + countNum);

        System.out.println("\n\n\n");

        classesNum = 0;
        int constructorNum = 0;
        int methodCallNum = 0;
        for (String className : genericObjectSet.CountClassNum.keySet()) {
//            System.out.println(className);
            List<Integer> classNum = genericObjectSet.CountClassNum.get(className);
            if (classNum != null) {
                classesNum++;
                constructorNum += classNum.get(0);
                methodCallNum += classNum.get(1);
            }
        }
        System.out.println("classes : " + classesNum);
        System.out.println("constructor num : " + constructorNum);
        System.out.println("method return num : " + methodCallNum);
//        MethodStatement methodStatement = new MethodStatement()
//        System.out.println(tc.addStatement());
//        for (ClassInfo c : ProjectInfo.i().getAllClassInfo()) {
//            System.out.println(c.getSig());
//        }
//
//        for (MethodInfo m : ProjectInfo.i().getAllMethod()) {
//            System.out.println("1"+m.getSig());
//            System.out.println("2"+m.getName());
//        }
//        ClassInfo classInfo = ProjectInfo.i().getClassInfo("neu.lab.Host.A");
//        if (classInfo == null) {
//            return;
//        }
//        System.out.println(classInfo.getSig());
//        ProjectInfo.i().setEntryCls("neu.lab.Host.A");
//        List<MethodInfo> methodInfoList = classInfo.getAllConstructorContainsChildren();
//        for (MethodInfo methodInfo : methodInfoList) {
//            System.out.println(methodInfo.getSig());
//        }
////        System.out.println(methodInfoList.size());
//        //添加所有返回值为target class的方法
//        for (MethodInfo methodInfo : ProjectInfo.i().getAllMethod()) {
//            if (methodInfo.getReturnType().equals("neu.lab.Host.A")) {
//                if (methodInfoList.contains(methodInfo)) {
//                    continue;
//                }
//                methodInfoList.add(methodInfo);
//            }
//        }
//        for (MethodInfo methodInfo : methodInfoList) {
//            System.out.println(methodInfo.getSig());
//        }

//        ClassInfo s = ProjectInfo.i().getClassInfo("");
//        System.out.println(s.getSig());
    }
}
