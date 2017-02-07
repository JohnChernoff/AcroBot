package org.chernovia.lib.netgames.zugserv;

import java.net.InetAddress;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@WebSocket
public class WebSockConn extends ConnAdapter {
	
	public static WebSockServ SERVER;
	public static boolean VERBOSE = true;
	private Session session;
	private String last_msg = "";
	private int repeats = 0;
	
	public WebSockConn() { setServ(SERVER); }
	
	@OnWebSocketConnect
    public void onConnect(Session s) {
		session = s; 
		getServ().connect(this);
    }
		
	public void log(String msg) {
		System.out.println(msg);
	}
	
	@OnWebSocketMessage
    public void onMessage(String message){
		if (last_msg.equals(message)) repeats++; 
		else { 
			if (repeats > 0) log("(repeats " + repeats + "x)");  
			last_msg = message; repeats = 0; 
			log("Message received: " + message);
		}
		getServ().newMsg(this,message);
    }
 
	@OnWebSocketClose
    public void onClose(int statusCode, String reason){
        log(reason);
        getServ().disconnected(this);
    }
	
    @OnWebSocketError
    public void onError(Throwable t) {
    	log("Error: " + t.getMessage());
    }
	
    public void augh(String msg) { System.out.println(msg); System.exit(1); }
	public Session getSession() { return session; }

	@Override
	public void close() { session.close(); }
	
	@Override
	public InetAddress getAddress() { return session.getRemoteAddress().getAddress(); }
	
	@Override
	public void tell(String type, String msg) {
		JsonObject obj = new JsonObject();
		obj.add(type, new JsonPrimitive(msg));
		String message = obj.toString();
		try {
			if (VERBOSE) log("Sending: " + message);
			getSession().getRemote().sendString(message,null);
		} 
		catch (Exception e) { 
			log("Error writing to socket: " + e.getMessage());
			getSession().close();
		}
	}

}