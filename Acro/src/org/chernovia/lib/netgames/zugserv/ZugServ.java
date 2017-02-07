package org.chernovia.lib.netgames.zugserv;

import java.util.Vector;

public interface ZugServ {
	public boolean PASSWORD = false;
	public static enum ServType { TYPE_SOCK, TYPE_WEBSOCK, TYPE_IRC, TYPE_TWITCH, TYPE_UNKNOWN };
	public static String 
	MSG_TXT = "txt",
	MSG_LOGIN = "login",
	MSG_PASS = "pwd",
	MSG_SERV = "serv_msg", 
	MSG_ERR = "err_msg",
	MSG_PRIV = "priv_msg", 
	MSG_CAST = "broadcast";
	void connect(Connection conn);
	void newMsg(Connection conn, String msg);
	void loggedIn(Connection conn);
	void disconnected(Connection conn);
	Connection getConnByHandle(String handle);
	Vector<Connection> getAllConnections();
	void broadcast(String type,String msg);
	void tch(int ch, String type, String msg);
	void send(String msg); 
	void incoming(String data);
	void startSrv();
	ServType getType();
	int getMaxConnections();
	void setMaxConnections(int c);
}
