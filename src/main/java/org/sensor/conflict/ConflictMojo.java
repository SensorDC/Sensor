package org.sensor.conflict;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sensor.conflict.vo.NodeAdapter;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import org.sensor.conflict.container.AllCls;
import org.sensor.conflict.container.DepJars;
import org.sensor.conflict.container.NodeAdapters;
import org.sensor.conflict.container.Conflicts;
import org.sensor.conflict.util.Conf;
import org.sensor.conflict.util.MavenUtil;
import org.sensor.conflict.vo.DepJar;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public abstract class ConflictMojo extends AbstractMojo {
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    public MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    public List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    public List<ArtifactRepository> remoteRepositories;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    public ArtifactRepository localRepository;

    @Component
    public DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    public File buildDir;

    @Component
    public ArtifactFactory factory;

    @Component
    public ArtifactHandlerManager artifactHandlerManager;
    @Component
    public ArtifactResolver resolver;
    DependencyNode root;

    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
    public List<String> compileSourceRoots;

    @Parameter(property = "ignoreTestScope", defaultValue = "true")
    public boolean ignoreTestScope;

    @Parameter(property = "ignoreProvidedScope", defaultValue = "false")
    public boolean ignoreProvidedScope;

    @Parameter(property = "ignoreRuntimeScope", defaultValue = "false")
    public boolean ignoreRuntimeScope;

    @Parameter(property = "append", defaultValue = "false")
    public boolean append;

    @Parameter(property = "useAllJar", defaultValue = "true")
    public boolean useAllJar;

    @Parameter(property = "disDepth")
    public int disDepth = Integer.MAX_VALUE;

    @Parameter(property = "pathDepth")
    public int pathDepth = Integer.MAX_VALUE;

    @Parameter(property = "callConflict")
    public String callConflict = null;

    // 自定义输出目录
    @Parameter(property = "resultPath")
    public String resultPath = "." + File.separator;

    // 设置是否细分1234等级
    @Parameter(property = "subdivisionLevel", defaultValue = "false")
    public boolean subdivisionLevel;

    @Parameter(property = "classMissing")
    public boolean classMissing = false;

    @Parameter(property = "findAllPath")
    public boolean findAllPath = false;

    @Parameter(property = "runTime")
    public int runTime = 1;

    @Parameter(property = "printDiff", defaultValue = "false")
    public boolean printDiff;

    @Parameter(property = "semanticsPrintNum")
    public int semanticsPrintNum = Integer.MAX_VALUE;

    @Parameter(property = "targetJar")
    public String targetJar;

    public int systemSize = 0;

    public long systemFileSize = 0;// byte

    public List<String> noPomPaths = new ArrayList<>();

    // 初始化全局变量
    protected void initGlobalVar() throws Exception {
        MavenUtil.i().setMojo(this);
        Conf.CLASS_MISSING = classMissing;
        Conf.DOG_DEP_FOR_DIS = disDepth;
        Conf.DOG_DEP_FOR_PATH = pathDepth;
        Conf.callConflict = callConflict;
        Conf.findAllpath = findAllPath;
        Conf.outDir = resultPath;
        Conf.append = append;
        Conf.runTime = runTime;
        Conf.printDiff = printDiff;
        Conf.semanticsPrintNum = semanticsPrintNum;
        Conf.targetJar = targetJar;
        GlobalVar.useAllJar = useAllJar;

        // 初始化NodeAdapters
        NodeAdapters.init(root);

        getNodePomPath();

        // 初始化DepJars
        DepJars.init(NodeAdapters.i());// occur jar in tree
        // 验证系统大小
        validateSystemSize();
        // 初始化所有的类集合
        AllCls.init(DepJars.i());
        // 初始化树中的版本冲突
        Conflicts.init(NodeAdapters.i());// version conflict in tree 初始化树中的版本冲突
    }

    /**
     * get node pom path
     */
    private void getNodePomPath(){
        String localPomPath;
        for(NodeAdapter node : NodeAdapters.i().getAllNodeAdapter()){
            if(node.isNodeSelected()) {
                if (NodeAdapters.i().getNodeAdapter(root).getGroupId().equals(node.getGroupId())
                        && NodeAdapters.i().getNodeAdapter(root).getArtifactId().equals(node.getArtifactId())){
                    localPomPath = MavenUtil.i().getProjectPom();
                }else {
                    String localDirPath = MavenUtil.i().getMvnRep() + node.getGroupId().replace(".", File.separator) +
                            File.separator + node.getArtifactId() + File.separator + node.getVersion() + File.separator;
                    localPomPath = localDirPath + node.getArtifactId() + "-" + node.getVersion() + ".pom";
                }
                detectExclude(localPomPath, node);
            }
        }
//		MavenUtil.i().getLog().warn("size : " + Conf.dependencyMap.size());
//		for(String excludeNode : Conf.dependencyMap.keySet()){
//			MavenUtil.i().getLog().warn("excludeNode : " + excludeNode);
//			List<NodeAdapter> dependencyNodeList = Conf.dependencyMap.get(excludeNode);
//			for(NodeAdapter dependencyNode : dependencyNodeList){
//				MavenUtil.i().getLog().warn(dependencyNode.getSelectedNodeWholeSig());
//			}
//		}
    }

    /**
     * detect whether has exclusion
     * @param localPomPath : local pom path
     * @param node
     */
    private void detectExclude(String localPomPath, NodeAdapter node){
        File file = new File(localPomPath);
        if (file.exists()){
            SAXReader reader = new SAXReader();
            try {
                Document document = reader.read(file);
                Element root = document.getRootElement();
                Element dependencies = root.element("dependencies");
                if(dependencies != null) {
                    for (Object o : dependencies.elements("dependency")) {
                        Element dependency = (Element) o;
                        Element exclusions = dependency.element("exclusions");
                        if (exclusions != null) {
                            for (Object oTwo : exclusions.elements("exclusion")) {
                                Element exclusion = (Element) oTwo;
                                Element groupId = exclusion.element("groupId");
                                Element artId = exclusion.element("artifactId");
                                String name = groupId.getText() + ":" + artId.getText();
                                if (Conf.dependencyMap.containsKey(name)) {
                                    List<NodeAdapter> dependencyNodeList = Conf.dependencyMap.get(name);
                                    dependencyNodeList.add(node);
                                    Conf.dependencyMap.put(name, dependencyNodeList);
                                } else {
                                    List<NodeAdapter> dependencyNodeList = new ArrayList<>();
                                    dependencyNodeList.add(node);
                                    Conf.dependencyMap.put(name, dependencyNodeList);
                                }
                            }
                        }
                    }
                }
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        } else {
            noPomPaths.add(localPomPath);
        }
    }

    private void validateSystemSize() throws Exception {

        for (DepJar depJar : DepJars.i().getAllDepJar()) {
            if (depJar.isSelected()) {
                systemSize++;
                for (String filePath : depJar.getJarFilePaths(true)) {
                    systemFileSize = systemFileSize + new File(filePath).length();
                }
            }
        }

        MavenUtil.i().getLog().info("tree size:" + DepJars.i().getAllDepJar().size() + ", used size:" + systemSize
                + ", usedFile size:" + systemFileSize / 1000);

    }

    @Override
    public void execute() throws MojoExecutionException {
        this.getLog().info("method detect start:");
        long startTime = System.currentTimeMillis();
        String pckType = project.getPackaging(); // 得到项目的打包类型
        if ("jar".equals(pckType) || "war".equals(pckType) || "maven-plugin".equals(pckType)
                || "bundle".equals(pckType)) {
            try {
                // project.
                root = dependencyTreeBuilder.buildDependencyTree(project, localRepository, null);
            } catch (DependencyTreeBuilderException e) {
                throw new MojoExecutionException(e.getMessage());
            }
            try {
                initGlobalVar();
            } catch (Exception e) {
                MavenUtil.i().getLog().error(e);
                throw new MojoExecutionException("project size error!");
            }
            run();

        } else {
            this.getLog()
                    .info("this project fail because package type is neither jar nor war:" + project.getGroupId() + ":"
                            + project.getArtifactId() + ":" + project.getVersion() + "@"
                            + project.getFile().getAbsolutePath());
        }
        long runtime = (System.currentTimeMillis() - startTime) / 1000;
        GlobalVar.runTime = runtime;
        printRunTime();
        this.getLog().debug("method detect end");

    }

    private void printRunTime() {
        this.getLog().info("time to run:" + GlobalVar.runTime);
        this.getLog().info("time to call graph:" + GlobalVar.time2cg);
        this.getLog().info("time to run dog:" + GlobalVar.time2runDog);
        this.getLog().info("time to calculate branch:" + GlobalVar.branchTime);
        this.getLog().info("time to calculate reference:" + GlobalVar.time2calRef);
        this.getLog().info("time to filter riskMethod:" + GlobalVar.time2filterRiskMthd);
    }

    public abstract void run();
}
