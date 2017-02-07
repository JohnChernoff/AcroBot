package org.chernovia.lib.netgames.zugserv;

import com.google.gson.JsonObject;

public abstract class SimpleOccupant {
	private Connection conn;
	private SimpleRoom room;
	public SimpleOccupant(Connection c) {
		conn = c;
	}
	public SimpleRoom getRoom() { return room; }
	public void setRoom(SimpleRoom r) { room = r; }
	public Connection getConn() { return conn; }
	public void setConn(Connection c) { conn = c; }
	public String toString() { 
		if (getConn() == null) return "(disconnected)"; else return getConn().getHandle();
	}
	abstract public JsonObject serialize();
}
