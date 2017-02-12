package org.chernovia.lib.netgames.zugserv;

import java.io.PrintWriter;
import java.util.Vector;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class ServAdapter implements ZugServ {
	
	public static String CR = "\n";  
	private ConnListener listener;
	private Vector<Connection> conns = new Vector<Connection>();
	private PrintWriter log =  new PrintWriter(System.out,true);
	private int max_conn = 999;
	
	public ServAdapter(ConnListener l) { listener = l; } //TODO: should this support multiple listeners?
	
	public void connect(Connection conn) {
		Vector<Connection> conns = getAllConnections();
        log("Accepting connection #" + conns.size());
		conns.add(conn);
		conn.setStatus(Connection.STATUS_LOGIN);
		conn.tell(ZugServ.MSG_SERV,"Hello! Please enter your name."); 
	}
	
	public void loggedIn(Connection conn) {
		conn.setStatus(Connection.STATUS_OK);
		conn.tell(ZugServ.MSG_LOGIN,conn.getHandle());
		listener.loggedIn(conn);
	}

	public void disconnected(Connection conn) {
		log.println(conn.getHandle() + " disconnected.");
		conns.remove(conn);
		listener.disconnected(conn);
	}

	public Vector<Connection> getAllConnections(){ return conns; }
	public Connection getConnByHandle(String handle) {
		return getConnByHandle(handle,false);
	}
	public Connection getConnByHandle(String handle, boolean caseSensitive) {
		for (Connection conn: conns) {
			String h = conn.getHandle();
			if (h != null && ((!caseSensitive &&
			h.equalsIgnoreCase(handle)) ||
			h.equals(handle))) return conn;
		}
		return null;
	}
		
	public int getMaxConnections() { return max_conn; }
	public void setMaxConnections(int c) { max_conn = c; }

	//TODO: anti-spam?
	public void newMsg(Connection conn, String msg) {
		boolean handled = true;
		String[] tokens = msg.split(" ");
		String cmd = tokens[0];
		if (msg.equalsIgnoreCase("EXIT")) {
			conn.close();
		}
		else if (conn.getStatus() == Connection.STATUS_LOGIN) { 
			login(conn,msg); 
		}
		else if (conn.getStatus() == Connection.STATUS_PASS) {
			password(conn,msg);
		}
		else if (cmd.equalsIgnoreCase("TELL") && tokens.length > 2) {
			pTell(conn,getConnByHandle(tokens[1]),msg.substring(tokens[0].length() + 1 + tokens[1].length() + 1));
		}
		else if (cmd.equalsIgnoreCase("SHOUT") && tokens.length > 1) {
			shout(conn,msg.substring(tokens[0].length()+1));
		}
		else if (cmd.equalsIgnoreCase("WHO")) {
			String list = "Currently connected: " + CR;
			for (Connection c: conns) list += c.getHandle() + CR; 
			conn.tell(ZugServ.MSG_SERV, list);
		}
		else if (cmd.equalsIgnoreCase("-CH") && tokens.length == 2) {
			int c = Integer.parseInt(tokens[1]);
			if (conn.partChan(c)) listener.partChan(conn,c);
				else conn.tell(ZugServ.MSG_SERV, "Unknown channel.");
		}
		else if (cmd.equalsIgnoreCase("+CH") && tokens.length == 2) {
			int c = Integer.parseInt(tokens[1]);
			if (conn.joinChan(c)) listener.joinChan(conn,c);
			else conn.tell(ZugServ.MSG_SERV, "Unknown channel.");
		}
		else handled = false;
		if (!handled) listener.newMsg(conn, msg);
	}
	
	public void broadcast(String type, String msg) {
		for (Connection conn: conns) conn.tell(type, msg);
	}
	
	public void tch(int chan, String type, String msg) {
		for (Connection conn: conns) {
			if (conn.getChannels().contains(new Integer(chan)))	conn.tell(type, msg);
		}
	}
		
	@Override
	public void send(String msg) {}

	@Override
	public void incoming(String data) {}
	
	private void shout(Connection conn, String msg) {
		JsonObject obj = new JsonObject();
		if (conn != null) obj.add("caster",new JsonPrimitive(conn.getHandle()));
		obj.add("type", new JsonPrimitive(ZugServ.MSG_CAST));
		obj.add("msg",new JsonPrimitive(msg));
		for (int i=0;i<conns.size();i++) {
			Connection c = (Connection)conns.elementAt(i);
			c.tell(ZugServ.MSG_TXT,obj.toString());
		}
	}
	
	//TODO: look up password
	private void password(Connection conn, String pwd) {
		if (isLegalPwd(pwd)) {
			loggedIn(conn);
		}
		else {
			conn.tell(ZugServ.MSG_ERR,"Invalid password - try again!");
		}
	}
	
	private void login(Connection conn, String handle) {
		if (isLegalName(handle)) {
			conn.setHandle(handle);
			if (PASSWORD) {
				conn.setStatus(Connection.STATUS_PASS);
				conn.tell(ZugServ.MSG_PASS,"Password?");
			}
			else loggedIn(conn);
		}
		else {
			conn.tell(ZugServ.MSG_ERR,"Invalid handle - try again!");
		}
	}
	
	private boolean isLegalName(String n) {
		if (n==null || n.length()<2 || n.length()>16) {
			return false;
		}
		for (int i = 0; i < n.length(); i++) {
			char c = n.charAt(i);
			if (!Character.isLetter(c)) return false;
		}
		return (getConnByHandle(n) == null);
	}
	
	private boolean isLegalPwd(String pwd) {
		if (pwd==null || pwd.length()<2 || pwd.length()>16) {
			return false;
		}
		return true;
	}
		
	private void pTell(Connection sender, Connection receiver, String msg) {
		if (receiver == null) { sender.tell(ZugServ.MSG_ERR,"No such handle"); return; }
		JsonObject obj = new JsonObject();
		obj.add("type", new JsonPrimitive(ZugServ.MSG_PRIV));
		obj.add("sender",new JsonPrimitive(sender.getHandle()));
		obj.add("msg",new JsonPrimitive(msg));
		receiver.tell(ZugServ.MSG_TXT,obj.toString());
		sender.tell(ZugServ.MSG_SERV,"Told " + receiver.getHandle() + ".");
	}
	
	private void log(String msg) { log(msg,true); }
	private void log(String msg, boolean newline) { if (newline) log.println(msg); else log.print(msg); }
	
}
