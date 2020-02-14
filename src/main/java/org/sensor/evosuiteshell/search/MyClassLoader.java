package org.sensor.evosuiteshell.search;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class MyClassLoader {
    @Deprecated
    public static Class<?> classLoader(File file, String className) {
        try {
            URL url = file.toURI().toURL();
//          URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {url});
            //得到系统类加载器，利用该加载器加载指定路径下的jar包
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(urlClassLoader, new Object[]{url});

            urlClassLoader.loadClass(className);
            Class<?> c = urlClassLoader.loadClass(className);
            //列出所有方法
//          Method[] methods = c.getMethods();
//          for (Method m : methods) {
//              System.out.println(m.getName());
//          }

//          urlClassLoader.close();
            return c;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

    public static void jarLoader(File file) {
        try {
            URL url = file.toURI().toURL();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(urlClassLoader, new Object[]{url});
        } catch (Exception e) {
        }
    }

    public static Class<?> loaderClass(String className) {
        Class<?> c = null;
        try {
            urlClassLoader.loadClass(className);
            c = urlClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return c;
    }

//    public static void main(String[] args) throws ClassNotFoundException {
////        MyClassLoader.classLoader(new File("/Users/wangchao/Host-1.0.jar"), "org.sensor.Host.Host");
//        MyClassLoader.jarLoader(new File("/Users/wangchao/eclipse-workspace/Host/target/Host-1.0.jar"));
//        System.out.println(MyClassLoader.loaderClass("org.sensor.Host.Host"));
//    }
}
