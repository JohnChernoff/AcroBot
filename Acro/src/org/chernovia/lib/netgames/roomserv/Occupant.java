package org.chernovia.lib.netgames.roomserv;

public class Occupant {
	private Connection conn;

	public Occupant(Connection c) {
		conn = c;
	}

	public Room getRoom() { return conn.getRoom(); }
	public void setRoom(Room r) { conn.setRoom(r); }
	public Connection getConn() { return conn; }
	public void setConn(Connection c) { conn = c; }
}
