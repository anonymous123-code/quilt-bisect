package io.github.anonymous123_code.quilt_bisect.plugin;

public class InvalidCommunicationFileStateException extends Exception {
	public final String type;
	public InvalidCommunicationFileStateException(String type) {
		this.type = type;
	}

	public String titleText() {
		return "In bisect, but unable to read " + type + ".";
	}
}
