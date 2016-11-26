package org.chernovia.lib.netgames.roomserv;

import java.util.Vector;

public interface NetServ extends ConnListener {

	//TODO: perhaps remove datagram-specific code here and in ConnAdapter/LocServ
	
	public static final int	LOCAL = 0, MUD = 1, IRC = 2, ICC = 3, OTHER = 4;
	
	String DIR = System.getProperty("user.dir");
	String FS  = System.getProperty("file.separator");
	String PS  = System.getProperty("path.separator");
	String[] newline  = {
			System.getProperty("line.separator"), //local
			System.getProperty("line.separator"), //mud
			" ", //irc
			"\\n", //icc
			System.getProperty("line.separator") //other
	};

	//boolean login(Connection conn, String handle, String passwd);
	public void addConnListener(ConnListener l);
	public void removeConnListener(ConnListener l);
	Connection getConnByHandle(String handle);
	Vector<Connection> getAllConnections();
	String who();
	String whois(String handle);
	void broadcast(String msg);
	void broadcast(String msg, boolean datagram);
	void tell(String hand1, String hand2, String mess);
	//void tell(String handle, String msg, boolean quietly);
	void tell(String handle, String msg, boolean quietly, boolean dg);
	//void tch(int chan, String msg, boolean quietly);
	void tch(int chan, String msg, boolean quietly, boolean datagram);
	void newTell(String handle, String msg);
	void newChanTell(int chan, String handle, String msg);
	void send(String msg); //send to piggybacked server
	void incoming(String data); //handle anonymous data
	void startSrv();
	int getType();
	int getMaxConnections();
	void setMaxConnections(int c);
	int getMaxChannels();
	void setMaxChannels(int c);
	
}
