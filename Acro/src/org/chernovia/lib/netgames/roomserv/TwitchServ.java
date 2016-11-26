//TODO: have the game determine the channel to send to
//maybe make an adapter for NetServ

package org.chernovia.lib.netgames.roomserv;

import java.util.StringTokenizer;
import java.util.Vector;
import org.jibble.pircbot.PircBot;

public class TwitchServ extends PircBot implements NetServ {
	
	public static final int MAX_CHARS = 255; 
	
	class TwitchConn extends ConnAdapter {
		public TwitchConn(NetServ server, String name) { 
			super(server); setHandle(name);
		}
		public void ban(int seconds) {
			sendRawLine("PRIVMSG " + channel + " :/timeout " + getHandle() + " " + seconds);
		}
	}
	
	String name, host, oauth, channel, lastMsg = "";
	Vector<String> mods;
	String Twitch_CR = " <3 ";
	
	public static String WELCOME_MSG = "Hello! Basic commands are: " +
	"!who, !help, and !start";
	private Vector<Connection> conns;
	private ConnListener game;
	private int max_conn = 999, max_chan = 999;
	//public static NetServ whisperSrv;
	
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
		//newMsg(getConn(sender),message);
		getConn(sender).handleMsg(message);
	}
	
	public void onMessage(String channel, String sender, 
	String login, String hostname, String message) {
		if (message.startsWith("!")) newMsg(getConn(sender),message);
		//tell(sender,"Whisper to me like so: /w " + this.name + " " + message);
	}
	
	//public void onPrivateMessage(String sender, 
	//		String login, String hostname, String message) {
	//	if (sender.equalsIgnoreCase("jtv") && message.startsWith(
	//	"Your message was not sent")) {
	//		try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
	//		send(lastMsg);
	//	}
	//	else newMsg(getConn(sender), message);
	//}
		
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
	
	@Override
	public void startSrv() {
		setName(name);
        setVerbose(true);
        try { connect(host,6667,oauth); }
        catch (Exception augh) { augh.printStackTrace(); }
	}

	public int getType() { return IRC; }
	public boolean newMsg(Connection conn, String msg) { 
		//((TwitchConn)conn).id
		return game.newMsg(conn,msg); 
	}
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

	//overriden method stubs from NetServ... whee...
	public void shout(String handle, String msg) { send(msg); }
	public void tch(int chan, String msg, boolean quietly) { send(msg); }
	public void tch(int chan, String msg, boolean quietly, boolean dg) { send(msg); }
	public void broadcast(String msg) {	send(msg); }
	public void broadcast(String msg, boolean dg) { send(msg); }
	public void tell(String handle, String msg, boolean q, boolean dg) { 
		tell(handle,msg);
	}
	public void tell(String handle, String msg, boolean quietly) { 
		tell(handle,msg);
	}
	public void tell(String handle, String msg) {
		sendRawLine("PRIVMSG " + channel + " :/w " + handle + " " + msg); 
	}
	public void tell(String h1, String h2, String msg) { 
		sendRawLine("PRIVMSG " + channel + " :/w" + h2 + " " + h1 + " tells you: " + msg);
	}
	public void send(String msg) { 
		lastMsg = msg; //sendMessage(channel,trim(msg));
		sendChan(channel,trim(msg));
	}
	
	public void sendChan(String chan, String msg) {
		if (msg == null || msg.length() < 1) return;
		boolean overflow;
		try { do {
			overflow = msg.length() > MAX_CHARS;
			if (overflow) {
				int lastCR = msg.substring(0,MAX_CHARS).lastIndexOf(Twitch_CR);
				sendMessage(chan,msg.substring(0,lastCR));
				msg = msg.substring(lastCR,msg.length());
			}
			else sendMessage(chan,msg);
		} while (overflow); }
		catch (Exception augh) { sendMessage(chan,"Augh: " + augh.getMessage()); }
	}

	public String who() {
		StringBuffer SB = new StringBuffer("Currently Connected: " + newline[IRC]);
		for (int i=0;i<conns.size();i++) {
			SB.append(conns.elementAt(i).getHandle() + newline[IRC]);
		}
		return SB.toString();
	}

	public String whois(String handle) {
		return "Augh: command not yet implemented (whois)";
	}
	
	public int getMaxChannels() { return max_chan; }
	public void setMaxChannels(int c) { max_chan = c; }
	public int getMaxConnections() { return max_conn; }
	public void setMaxConnections(int c) { max_conn = c; }
	
	//unused methods
	public void incoming(String data) {}
	public void newTell(String handle, String msg) {}
	public void newChanTell(int chan, String handle, String msg) {}
	public boolean login(Connection c, String n, String pwd) { return false; }
	
	public void setTwitchCR(String cr) { Twitch_CR = cr; }
	public String trim(String str) {
		String s = "";
		StringTokenizer ST = new StringTokenizer(str,NetServ.newline[NetServ.IRC]);
		while (ST.hasMoreTokens()) {
			s += ST.nextToken(); if (ST.hasMoreTokens()) s += Twitch_CR;
		}
		return s;
	}
}


