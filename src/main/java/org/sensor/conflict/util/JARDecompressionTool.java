package org.sensor.conflict.util;

import org.sensor.evosuiteshell.Config;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JARDecompressionTool {

    public final static String decompressionPath = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR + "decompress" + Config.FILE_SEPARATOR;

    /**
     * 解压jar文件
     */
    public static synchronized void decompress(String filePath, String outputPath) {
        if (!outputPath.endsWith(File.separator)) {
            outputPath += File.separator;
        }
        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        JarFile jf = null;
        try {
            jf = new JarFile(filePath);
            for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
                JarEntry je = (JarEntry) e.nextElement();
                String outFileName = outputPath + je.getName();
                File f = new File(outFileName);
                if (je.isDirectory()) {
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } else {
                    File pf = f.getParentFile();
                    if (!pf.exists()) {
                        pf.mkdirs();
                    }
                    InputStream in = jf.getInputStream(je);
                    OutputStream out = new BufferedOutputStream(
                            new FileOutputStream(f));
                    byte[] buffer = new byte[2048];
                    int nBytes = 0;
                    while ((nBytes = in.read(buffer)) > 0) {
                        out.write(buffer, 0, nBytes);
                    }
                    out.flush();
                    out.close();
                    in.close();
                }
            }
            MavenUtil.i().getLog().info(filePath + " decompression success");
        } catch (Exception e) {
            MavenUtil.i().getLog().error("Decompression " + filePath + " error---" + e.getMessage());
//            System.out.println("Decompression " + filePath + " error---" + e.getMessage());
//        } finally {
//            if (jf != null) {
//                try {
//                    jf.close();
//                    File jar = new File(jf.getName());
////                    if (jar.exists()) {
////                        jar.delete();
////                        System.out.println(jar.getName() + " exists");
////                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        }
    }
}
