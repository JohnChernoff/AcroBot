package org.chernovia.lib.netgames.roomserv;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Loc_Serv extends Thread implements NetServ {

	public static final String SERV_DG = "%", DG_DELIM = " ";
	public static final int DG_LOG_FAIL = -1, 
	DG_LOGIN = 0, DG_CHAN = 1, DG_NEW_TELL = 2, DG_ASK = 3, DG_CHAN_LEAVE = 0, DG_CHAN_ENTER = 1;
	public static String WELCOME_MSG = "Hello!" +
	newline[LOCAL] + "Basic commands are: " +
	"who, shout, tell, help, and exit";
	private ServerSocket sock;
	private int port = 5678;
	private Vector<Connection> conns;
	private Vector<ConnListener> listeners;
	private PrintWriter log = null;
	private int max_conn = 999, max_chan = 999;

	public Loc_Serv(int p) { this(p,null); }
	public Loc_Serv(int p, ConnListener cl) {
		log = new PrintWriter(System.out,true);
		port = p;
		conns = new Vector<Connection>(); 
		listeners = new Vector<ConnListener>();
		listeners.add(cl);
		do {
			try { sock = new ServerSocket(port); }
			catch (IOException e) {
				log.println("ServSock Creation Error: " +
						e);
			}
		} while (sock == null);
		log.println("Server created on port " +
				port + "...");
	}

	public boolean newMsg(Connection conn, String msg) {
		boolean ok = true;
		for (ConnListener l: listeners) 	if (!l.newMsg(conn, msg)) ok = false;
		//for (int i=0;i<listeners.size();i++) {
		//	if (!((ConnListener)(listeners.elementAt(i))).newMsg(
		//conn,msg)) ok = false;
		//}
		return ok;
	}

	public void addConnListener(ConnListener l) {
		if (!listeners.contains(l)) listeners.add(l);
	}

	public void removeConnListener(ConnListener l) {
		if (listeners.contains(l)) listeners.remove(l);
	}

	public void notifyLogin(Connection conn) {
		for (int i=0;i<listeners.size();i++)
			((ConnListener)
			(listeners.elementAt(i))).loggedIn(conn);
	}

	public void notifyDisconnect(Connection conn) {
		for (int i=0;i<listeners.size();i++)
			((ConnListener)
			(listeners.elementAt(i))).disconnected(conn);
	}

	@Override
	public void run() {
		log.println("Accepting connections...");
		while (true) {
			if (conns.size() < getMaxConnections()) {
				Socket S = null;
				do {
					try { S = sock.accept(); }
					catch (IOException e) {
						log.println(e);
					}
				}
				while (S == null);
				yield();
				Loc_Conn newConn =
					new Loc_Conn(this,S);
				conns.add(newConn);
				log.println("Accepting connection #" + conns.size());
				new Thread(newConn).start(); //calls login()
			}
		}
	}

	public boolean login(Connection conn,String handle,String passwd) {
		if (isLegalName(handle)) {
			conn.setHandle(handle);
			loggedIn(conn);
			return true;
		}
		else {
			conn.tell(SERV_DG + DG_DELIM + DG_LOG_FAIL);
			return false;
		}
	}

	public void loggedIn(Connection conn) {
		notifyLogin(conn);
		conn.tell(SERV_DG + " " + DG_LOGIN + " " + 
		conn.getHandle() + " " + getMaxChannels());
	}

	public void disconnected(Connection conn) {
		log.println(conn.getHandle() + " disconnected.");
		conns.remove(conn);
		notifyDisconnect(conn);
		tch(conn.getChan(),DG(DG_CHAN,DG_CHAN_LEAVE + DG_DELIM + conn.getHandle()),false,true);
	}

	public boolean isLegalName(String n) {
		if (n==null || n.length()<2 || n.length()>16) {
			return false;
		}
		for (int i = 0; i < n.length(); i++) {
			char c = n.charAt(i);
			if (!Character.isLetter(c)) return false;
		}
		return (getConnByHandle(n) == null);
	}
	
	public Vector<Connection> getAllConnections(){ return conns; }
	public Connection getConnByHandle(String handle) {
		return getConnByHandle(handle,false);
	}
	public Connection getConnByHandle(String handle,
			boolean caseSensitive) {
		for (int i=0;i<conns.size();i++) {
			Connection conn = (Connection)conns.elementAt(i);
			if ((!caseSensitive &&
					conn.getHandle().equalsIgnoreCase(handle)) ||
					conn.getHandle().equals(handle)) return conn;
		}
		return null;
	}

	//overriden method stubs from NetServ

	public void startSrv() { start(); }
	public int getType() { return LOCAL; }

	public void tch(int chan, String msg, boolean quietly) {
		tch(chan,msg,quietly,false);
	}
	public void tch(int chan, String msg, boolean quietly, boolean dg) {
		for (int i=0;i<conns.size();i++) {
			Connection c = (Connection)conns.elementAt(i);
			if (c.getChan() == chan && (!dg || c.isGUI()))
			c.tell(msg);
		}
	}
	public void broadcast(String msg) {
		broadcast(msg,false);
	}
	public void broadcast(String msg, boolean dg) {
		for (int i=0;i<conns.size();i++){
			Connection c = (Connection)conns.elementAt(i);
			if (!dg || c.isGUI()) c.tell(msg);
		}
	}
	public void tell(String handle, String msg, boolean quietly) {
		tell(handle,msg,quietly,false);
	}
	public void tell(String handle, String msg, boolean q, boolean dg) {
		Loc_Conn conn = (Loc_Conn)getConnByHandle(handle);
		if (conn != null && (!dg || conn.isGUI())) 
		conn.getOut().println(msg);
	}

	public String who() {
		StringBuffer SB =
			new StringBuffer("Currently Connected: " + newline[LOCAL]);
		for (int i=0;i<conns.size();i++) {
			Connection conn = (Connection)conns.elementAt(i);
			SB.append(conn.getHandle() + newline[LOCAL]);
		}
		return SB.toString();
	}

	public String whois(String handle) {
		return "Augh: cmd not yet implemented (whois)";
	}

	public void tell(String h1, String h2, String msg) {
		Connection conn1 = getConnByHandle(h1);
		Connection conn2 = getConnByHandle(h2);
		if (conn1 != null) {
			if (conn2 == null) {
				conn1.tell("Player not found.");
			}
			else conn2.tell(h1 + " tells you: " + msg);
		}
	}

	public void shout(String handle, String msg) {
		broadcast(handle + " shouts: " + msg);
	}
	
	public int getMaxChannels() { return max_chan; }
	public void setMaxChannels(int c) { max_chan = c; }
	public int getMaxConnections() { return max_conn; }
	public void setMaxConnections(int c) { max_conn = c; }
	
	//unused methods
	public void send(String msg) {}
	public void incoming(String data) {}
	public void newTell(String handle, String msg) {}
	public void newChanTell(int chan, String handle, String msg) {}
	
	public static String DG(int dg, String arg) {	return SERV_DG + DG_DELIM + dg + DG_DELIM + arg; }
}
