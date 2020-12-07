package org.sensor.evosuiteshell;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.google.common.io.Files;

import org.sensor.conflict.vo.DependencyInfo;

public class ReadXML {
    public static String COPY_CONFLICT = "copyConflictDependency.xml";
    public static String COPY_JUNIT = "copyJunit.xml";
    public static String COYT_EVOSUITE = "copyEvosuiteRuntime.xml";

    /**
     * add dependency to new empty
     *
     * @param dependencyInfo
     */
    public static void setCopyDependency(DependencyInfo dependencyInfo, String xmlFilePath) {
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(xmlFilePath);
            Element rootElement = document.getRootElement();
            Element dependencies = rootElement.element("dependencies");
            Element dependency = dependencies.addElement("dependency");
            dependencyInfo.addDependencyElement(dependency);
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            XMLWriter writer = new XMLWriter(new FileWriter(xmlFilePath), outputFormat);
            writer.write(document);
            writer.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * copy empty dependency xml to target path
     *
     * @return XMLName.xml path
     */
    public static String copyPom(String XMLName) {
        InputStream fileInputStream = ReadXML.class.getResourceAsStream("/" + XMLName);
        String xmlFileName = System.getProperty("user.dir") + Config.FILE_SEPARATOR + Config.SENSOR_DIR + Config.FILE_SEPARATOR + XMLName;
        byte[] buffer;
        try {
            buffer = new byte[fileInputStream.available()];
            fileInputStream.read(buffer);
            Files.write(buffer, new File(xmlFileName));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return xmlFileName;
    }

    /**
     * execute Maven dependency:copy-dependencies
     *
     * @param xmlFileName
     * @param dir         for dependency jar
     */
    public static void executeMavenCopy(String xmlFileName, String dir) {
        String mvnCmd = Config.getMaven() + Command.MVN_POM + xmlFileName + Command.MVN_COPY + dir;
        try {
            ExecuteCommand.exeCmd(mvnCmd);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
