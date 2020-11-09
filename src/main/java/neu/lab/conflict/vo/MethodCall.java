package neu.lab.conflict.vo;

public class MethodCall {
	private String source;
	private String target;

	public MethodCall(String source, String target) {
		this.source = source;
		this.target = target;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MethodCall) {
			MethodCall rlt = (MethodCall) obj;
			return source.equals(rlt.getSrc()) && target.equals(rlt.getTgt());
		} else {
			return false;
		}

	}

	@Override
	public int hashCode() {
		return source.hashCode() * 31 + target.hashCode();
	}

	public String getSrc() {
		return source;
	}

	public String getTgt() {
		return target;
	}

	@Override
	public String toString() {
		return source + " to " + target;
	}

}
