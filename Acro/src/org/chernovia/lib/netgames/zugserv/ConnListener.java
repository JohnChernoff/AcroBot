package org.chernovia.lib.netgames.zugserv;

public interface ConnListener {
	void newMsg(Connection conn, String msg);
	void joinChan(Connection conn, int chan);
	void partChan(Connection conn, int chan);
	void loggedIn(Connection conn);
	void disconnected(Connection conn);
}
