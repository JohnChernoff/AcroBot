//TODO: have the game determine the channel to send to

package org.chernovia.lib.netgames.zugserv;

import java.net.InetAddress;
import java.util.StringTokenizer;
import java.util.Vector;
import org.jibble.pircbot.PircBot;

public class TwitchServ extends PircBot implements ZugServ {
	
	class TwitchConn extends ConnAdapter {
		public TwitchConn(TwitchServ serv, String name) { setServ(serv); setHandle(name); joinChan(0); }
		public void close() {}
		public InetAddress getAddress() { return null; }
		public void tell(String type, String msg) { 
			TwitchServ srv = (TwitchServ)getServ();
			srv.tell(this, ZugServ.MSG_SERV, msg); 
		}
		public boolean isFlooding(int limit, long span) { return false; } //TODO: implement flood protection
	}
	
	String name, host, oauth, channel, lastMsg = "";
	Vector<String> mods;
	String Twitch_CR = " <3 ";
	String CR = "\n";
	
	public static String WELCOME_MSG = "Hello! Basic commands are: !who, !help, and !start";
	private Vector<Connection> conns;
	private ConnListener game;
	private int max_conn = 999, max_chan = 999;
	public static ZugServ whisperSrv;
	
	public TwitchServ(String n, String h, String o, String c, ConnListener l) {
		name = n; host = h; oauth = o; channel = c;
		conns = new Vector<Connection>();
		addConnListener(l);
		setMessageDelay(2000); 
        mods = new Vector<String>(); mods.add("zugaddict");
	}
		
	public void onConnect() {
		System.out.println("Connected!");
        joinChannel(channel);
		sendRawLine("CAP REQ :twitch.tv/commands");
	}
	
	public void onWhisper(String channel, String sender, 
	String login, String hostname, String message) {
		newMsg(getConn(sender),message);
	}
	
	public void onMessage(String channel, String sender, 
	String login, String hostname, String message) {
		if (message.startsWith("!")) newMsg(getConn(sender),message);
		//tell(sender,"Whisper to me like so: /w " + this.name + " " + message);
	}
	
	public void onPrivateMessage(String sender, 
			String login, String hostname, String message) {
		if (sender.equalsIgnoreCase("jtv") && message.startsWith("Your message was not sent")) {
			try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
			send(lastMsg);
		}
		else newMsg(getConn(sender), message);
	}
		
	public void onJoin(String channel, String sender, String login, String hostname) {
		System.out.println(getConn(sender).getHandle() + " joins.");
	}
	
	public void onPart(String channel, String sender, String login, String hostname) {
		disconnected(getConnByHandle(sender));
	}
	
	public Connection getConn(String n) {
		Connection c = getConnByHandle(n);
		if (c == null) { c = new TwitchConn(this,n); loggedIn(c); }
		return c;
	}
	
	public void startSrv() {
		setName(name);
        setVerbose(true);
        try { connect(host,6667,oauth); }
        catch (Exception augh) { augh.printStackTrace(); }
	}

	//public int getType() { return IRC; }
	public void newMsg(Connection conn, String msg) { game.newMsg(conn,msg); }
	public void addConnListener(ConnListener l) { game = l; }
	public void removeConnListener(ConnListener l) { game = null; }
	public void loggedIn(Connection c) { conns.add(c); game.loggedIn(c); }
	public void disconnected(Connection conn) {
		if (conn == null) return;
		System.out.println(conn.getHandle() + " disconnected.");
		conns.remove(conn);	game.disconnected(conn);
	}
	public Vector<Connection> getAllConnections(){ return conns; }
	public Connection getConnByHandle(String handle) {
		for (int i=0;i<conns.size();i++) {
			Connection conn = (Connection)conns.elementAt(i);
			if (conn.getHandle().equals(handle)) return conn;
		}
		return null;
	}

	public void send(String msg) { 
		lastMsg = msg; sendMessage(channel,trim(msg));
	}

	public String who() {
		StringBuffer SB = new StringBuffer("Currently Connected: " + CR);
		for (int i=0;i<conns.size();i++) {
			SB.append(conns.elementAt(i).getHandle() + CR);
		}
		return SB.toString();
	}

	public int getMaxChannels() { return max_chan; }
	public void setMaxChannels(int c) { max_chan = c; }
	public int getMaxConnections() { return max_conn; }
	public void setMaxConnections(int c) { max_conn = c; }
	
	//unused methods
	public void incoming(String data) {}
	
	public void setTwitchCR(String cr) { Twitch_CR = cr; }
	public String trim(String str) {
		String s = "";
		StringTokenizer ST = new StringTokenizer(str,CR);
		while (ST.hasMoreTokens()) {
			s += ST.nextToken(); if (ST.hasMoreTokens()) s += Twitch_CR;
		}
		return s;
	}

	@Override
	public void broadcast(String type, String msg) {
		send(msg);
	}
	
	@Override
	public void tch(int ch, String type, String msg) {
		send(msg);
	}

	public void tell(Connection conn, String type, String msg) {
		send("/w " + conn.getHandle() + " " + msg); 
	}

	public void tell2(Connection conn1, Connection conn2, String msg) {
		send("/w" + conn2.getHandle() + " " + conn1.getHandle() + " tells you: " + msg);
	}

	@Override
	public void connect(Connection conn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServType getType() { return ServType.TYPE_TWITCH; }
	
}


