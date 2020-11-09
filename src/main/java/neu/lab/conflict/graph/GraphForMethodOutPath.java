package neu.lab.conflict.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neu.lab.conflict.vo.SemantemeMethod;


public class GraphForMethodOutPath implements IGraph{
	
	private Map<String, SemantemeMethod> semantemeMethods;
	
	public GraphForMethodOutPath(Map<String, SemantemeMethod> semantemeMethods) {
		this.semantemeMethods = semantemeMethods;
	}
	
public Map<String, SemantemeMethod> getSemantemeMethods() {
		return semantemeMethods;
	}

//public Set<String> comparedMethodOutPath(Map<String,List<String>> entryMehtodOutPath, Set<String> thrownMethods){
//	semantemeMethods = new HashMap<String, SemantemeMethod>();
//	Map<String, Integer> differenceMethod = new TreeMap<String, Integer>();
//	int intersection;
//	for (String method : methodOutPath.keySet()) {
////		if (differenceMethod.size() >= 100) {
////			break;
////		}
//		List<String> entryOutPath = entryMehtodOutPath.get(method);
//		List<String> thisOutPath = methodOutPath.get(method);
//		if (entryOutPath == null && thisOutPath == null) {
//		}
//		else if (entryOutPath == null) {
//			differenceMethod.put(method, thisOutPath.size());
//			semantemeMethods.put(method, new SemantemeMethod(method, 0, thisOutPath.size(), thrownMethods.contains(SootUtil.mthdSig2name(method))));
//		}
//		else if (thisOutPath == null) {
//			differenceMethod.put(method, entryOutPath.size());
//			semantemeMethods.put(method, new SemantemeMethod(method, 0, entryOutPath.size(), thrownMethods.contains(SootUtil.mthdSig2name(method))));
//		}
//		else {
////			if (entryOutPath.size() <= 12 || thisOutPath.size() <= 12) {
////				continue;
////			}
//			if (thisOutPath.size() != entryOutPath.size()) {
//				intersection = 0;
//				for (String intersectionMethod : thisOutPath) {
//					if (entryOutPath.contains(intersectionMethod)) {
//						intersection ++;
//					}
//				}
//				semantemeMethods.put(method, new SemantemeMethod(method, intersection, Math.abs(thisOutPath.size() - entryOutPath.size()), thrownMethods.contains(SootUtil.mthdSig2name(method))));
//				differenceMethod.put(method, /*Math.abs(*/Math.abs(thisOutPath.size() - entryOutPath.size())/* - intersection*/);		//仅以差集排序，不算交集
//			}
////			else {
////				for (String outMethod : entryOutPath) {
////					if (!thisOutPath.contains(outMethod)) {
////						differenceMethod.put(method, 1);
////						break;
////					}
////				}
////			}
//		}
////		System.out.println("thisOutPath" + thisOutPath.size() + ">>>>>" + "entryOutPath" + entryOutPath.size());
//	}
//	
//	enhancedLevelForInheritanceMethod(differenceMethod, thrownMethods);
//	
//	Set<String> differenceMethodBySort = sortMap(differenceMethod);
//	
//	
//	return differenceMethodBySort;
//}

/**
 * 提高来自动态绑定、多态导致的来自父类的方法
 * @param differenceMethod
 */
public void enhancedLevelForInheritanceMethod(Map<String, Integer> differenceMethod, Set<String> thrownMethods) {
	for (String method : thrownMethods) {
		if (differenceMethod.containsKey(method)) {
			System.out.println("enhancedLevelForInheritanceMethod>>>>>" + method);
			differenceMethod.replace(method, 100);
		}
	}
}

/**
 * 对Map排序后，输出前N个Intger最大的method
 * @param map
 * @return
 */
public Set<String>  sortMap(Map<String,Integer> map){ 
	Set<String> afterSortMethods = new HashSet<String>();
    List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());  
    Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {  
        public int compare(Map.Entry<String, Integer> obj1 , Map.Entry<String, Integer> obj2) {  
            return obj2.getValue() - obj1.getValue();  
        }  
    }); 
    int size = 0;
    if (map.size() > 100) {
    	size = 100;
    }
    else {
    	size = map.size();
    }
    for( int i=0;i<size;i++){  
//        System.out.print(entries.get(i).getKey() + ":" + entries.get(i).getValue());
        afterSortMethods.add(entries.get(i).getKey());
  } 
     return afterSortMethods;  
   }  


@Override
public INode getNode(String nodeName) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Collection<String> getAllNode() {
	// TODO Auto-generated method stub
	return null;
}
//public Map<String,List<String>> getMethodOutPath(){
//	return methodOutPath;
//}
}
