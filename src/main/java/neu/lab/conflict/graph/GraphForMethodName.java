package neu.lab.conflict.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class GraphForMethodName implements IGraph {
	public GraphForMethodName(HashMap<String, ArrayList<String>> accessibleMethod) {
		super();
		this.accessibleMethod = accessibleMethod;
	}

	private HashMap<String, ArrayList<String>> accessibleMethod = new HashMap<String, ArrayList<String>>();

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

	public HashMap<String, ArrayList<String>> getAccessibleMethod() {
		return accessibleMethod;
	}

	public void setAccessibleMethod(HashMap<String, ArrayList<String>> accessibleMethod) {
		this.accessibleMethod = accessibleMethod;
	}

}
