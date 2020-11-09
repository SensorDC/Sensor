package neu.lab.conflict.soot.tf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import neu.lab.conflict.graph.GraphForMethodName;
import neu.lab.conflict.util.MavenUtil;
import neu.lab.conflict.util.SootUtil;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

public class JRiskObjectCgTf extends JRiskCgTf {
	@Override
	protected void initMthd2branch() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void formGraph() {
		// TODO Auto-generated method stub
		if (graph == null) {
			HashSet<String> hostAccessibleClasses = new HashSet<String>();
			HashMap<String, ArrayList<String>> accessibleMethod = new HashMap<String, ArrayList<String>>();
			MavenUtil.i().getLog().info("start form graph...");
			// get call-graph.
			Chain<SootClass> classes = Scene.v().getClasses();
			for (SootClass cls : classes) {
				if (cls.isJavaLibraryClass()) {
					continue;
				}
				if (cls.isPublic() || cls.isStatic()) {
					if (entryClses.contains(cls.getName())) {
						hostAccessibleClasses.add(cls.getName());
						List<SootMethod> methods = cls.getMethods();
						for (SootMethod method : methods) {
							if (method.isPublic() || method.isStatic()) {
								if (entryClses.contains(method.getReturnType().toString())) {
									String returnType = method.getReturnType().toString();
									ArrayList<String> srcToMethods = accessibleMethod.get(returnType);
									if (srcToMethods == null) {
										srcToMethods = new ArrayList<String>();
										accessibleMethod.put(returnType, srcToMethods);
									}
									srcToMethods.add(method.getSignature());
								}
								if (method.getSignature().contains("init")) {
									String className = SootUtil.mthdSig2cls(method.getSignature());
									ArrayList<String> srcToMethods = accessibleMethod.get(className);
									if (srcToMethods == null) {
										srcToMethods = new ArrayList<String>();
										accessibleMethod.put(className, srcToMethods);
									}
									srcToMethods.add(method.getSignature());
								}
							}
						}
					}
				}
			}
//			CallGraph cg = Scene.v().getCallGraph();
//
//			Iterator<Edge> edges = cg.iterator();
//			while (edges.hasNext()) {
//				Edge edge = edges.next();
//				MethodOrMethodContext srcEdge = edge.getSrc();
//				SootMethod srcMethod = srcEdge.method();
//				if (hostAccessibleClasses.contains(SootUtil.mthdSig2cls(srcMethod.getSignature()))
//						&& isAccessible(srcMethod)) {
//					MethodOrMethodContext tgtEdge = edge.getTgt();
//					SootMethod tgtMethod = tgtEdge.method();
//					if (entryClses.contains(tgtMethod.getReturnType().toString())) {
//						ArrayList<String> srcToMethods = accessibleMethod.get(srcMethod.getSignature());
//						if (srcToMethods == null) {
//							srcToMethods = new ArrayList<String>();
//							accessibleMethod.put(srcMethod.getSignature(), srcToMethods);
//						}
//						srcToMethods.add(tgtMethod.getSignature());
//					}
//				}
//			}
			graph = new GraphForMethodName(accessibleMethod);
//			System.out.println(accessibleMethod.size());
//				if (cls.isJavaLibraryClass() || !cls.isApplicationClass()) {
//					continue;
//				}
//				String className = cls.getName();
//				System.out.println("class name : " + className);

//			}
		}
	}

	public boolean isAccessible(SootMethod srcMethod) {
		return srcMethod.isPublic() || srcMethod.isStatic();
	}
}
