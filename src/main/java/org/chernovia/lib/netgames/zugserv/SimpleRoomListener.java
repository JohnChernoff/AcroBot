package org.chernovia.lib.netgames.zugserv;

public interface SimpleRoomListener {
	public void roomCreated(SimpleRoom room);
	public void roomClosing(SimpleRoom room);
	public void update(SimpleRoom room);
}
