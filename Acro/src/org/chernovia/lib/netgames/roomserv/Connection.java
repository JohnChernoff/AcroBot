package org.chernovia.lib.netgames.roomserv;

public interface Connection {
	public static int MAX_VERB = 10;
	public String ask(String q,int t);
	public String ask(String q,String d,int t);
	public int ask(String q,int d,int t);
	public void tell(String msg);
	//public void tell(String msg,boolean quietly);
	public void tell(String msg,boolean quietly,boolean dg);
	public void setHandle(String h);
	public String getHandle();
	public void handleMsg(String msg);
	public boolean isAuto();
	public void automate(boolean a);
	public boolean isBorg();
	public void borg(boolean b);
	public int getVerbosity();
	public void setVerbosity(int v);
	public boolean isGUI();
	public void setGUI(boolean gui);
	public boolean pondering();
	public void prod();
	public void setResponse(String r);
	public NetServ getServ();
	public boolean equals(Connection rc);
	public Room getRoom();
	public void setRoom(Room r);
	public int getChan();
	public void setChan(int c);
}
