package org.sensor.evosuiteshell.generate;

import fj.Hash;
import fj.test.Gen;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.soot.JarAna;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.evosuiteshell.search.*;
import org.evosuite.TestGenerationContext;
import org.evosuite.instrumentation.InstrumentingClassLoader;
import org.evosuite.seeding.ObjectPool;
import org.evosuite.seeding.ObjectPoolManager;
import org.evosuite.seeding.sensor.APIPool;
import org.evosuite.seeding.sensor.APIPoolManager;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericClass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class GenericAPISet {
    private static GenericAPISet instance = null;

    private InstrumentingClassLoader instrumentingClassLoader = TestGenerationContext.getInstance().getClassLoaderForSUT();
    private HashSet<String> hostAPISig;

    private GenericAPISet() {
        //单例模式，只用soot解析一次host包内所有的classinfo和methodinfo
        String hostJarPath = DepJars.i().getHostDepJar().getJarFilePaths(true).toArray(new String[]{})[0];
        new SootExe().initProjectInfo(new String[]{hostJarPath});
    }

    public static GenericAPISet getInstance() {
        if (instance == null)
            instance = new GenericAPISet();
        return instance;
    }

    public void generateAllAPI() {
        for (MethodInfo methodInfo : ProjectInfo.i().getAllMethod()) {
            if (methodInfo.isPublic()) {
                generateForTargetAPI(methodInfo);
            }
        }
    }

    public void generateForTargetAPI(MethodInfo methodInfo) {
        TestCaseBuilder testCaseBuilder = new TestCaseBuilder();
        generateMethodCall(testCaseBuilder, methodInfo.getCls(), methodInfo);
    }

    //对目标API生成测试序列
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

    private boolean addSequenceToPool(TestCaseBuilder testCaseBuilder, VariableReference variableReference, ClassInfo classInfo) {
        if (variableReference == null) {
            return false;
        } else {
            APIPool apiPool = new APIPool();
            try {
                apiPool.addSequence(new GenericClass(instrumentingClassLoader.loadClass(classInfo.getSig())), testCaseBuilder.getDefaultTestCase());
            } catch (Throwable e) {
//                e.printStackTrace();
                MavenUtil.i().getLog().error(e);
                return false;
            }
            APIPoolManager.getInstance().addPool(apiPool);
//            System.out.println(objectPool.getNumberOfSequences());
            return true;
        }
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


    //test
    public GenericAPISet(String a) {
        hostAPISig = new HashSet<>();
        //单例模式，只用soot解析一次host包内所有的classinfo和methodinfo
//        String hostJarPath = DepJars.i().getHostDepJar().getJarFilePaths(true).toArray(new String[]{})[0];
//        hostClassesSig = JarAna.i().deconstruct(Arrays.asList(a)).keySet();
        new SootExe().initProjectInfo(new String[]{a});
    }

    public static void main(String[] args) {
        GenericAPISet genericAPISet = new GenericAPISet("/Users/wangchao/eclipse-workspace/Host/target/classes/");
        int i = 0;
        for (MethodInfo methodInfo : ProjectInfo.i().getAllMethod()) {
            if (methodInfo.isPublic()) {
                System.out.println(methodInfo.getSig());
                i++;
            }
        }
        System.out.println(ProjectInfo.i().getAllMethod().size());
        System.out.println(i);
    }
}
