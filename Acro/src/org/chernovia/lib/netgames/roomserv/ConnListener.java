package org.chernovia.lib.netgames.roomserv;

public interface ConnListener {
	boolean newMsg(Connection conn, String msg);
	void loggedIn(Connection conn);
	void disconnected(Connection conn);
}
