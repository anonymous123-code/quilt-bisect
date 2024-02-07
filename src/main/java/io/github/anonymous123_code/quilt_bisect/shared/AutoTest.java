package io.github.anonymous123_code.quilt_bisect.shared;

public record AutoTest(
	AutoTest.AutoJoinType autoJoinType,
	String autoJoinName,
	String autoJoinCommands,
	boolean disableWorldSaving,
	int autoAcceptTime,
	boolean autoAccept
) {

	public enum AutoJoinType {
		None, World, Server, Realm, LastJoined
	}
}

