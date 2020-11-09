package neu.lab.conflict.vo;

import org.dom4j.Element;

public class DependencyInfo {
	private String groupId;
	private String artifactId;
	private String version;

	public DependencyInfo() {

	}

	public DependencyInfo(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void addDependencyElement(Element dependency) {
		dependency.addElement("artifactId").setText(artifactId);
		dependency.addElement("groupId").setText(groupId);
		dependency.addElement("version").setText(version);
	}
}
