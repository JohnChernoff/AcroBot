package org.chernovia.lib.netgames.zugserv;

import java.net.InetAddress;
import java.util.Vector;

public interface Connection {
	
	public static final int STATUS_DISCONNECTED = -2, STATUS_ERR = -1, STATUS_OK = 0, STATUS_LOGIN = 1, STATUS_PASS = 2, STATUS_CLOSING = 3;
	public void setHandle(String h);
	public String getHandle();
	public void setServ(ZugServ serv);
	public ZugServ getServ();
	public boolean isAuto();
	public void automate(boolean a);
	public boolean isBorg();
	public void borg(boolean b);
	public boolean isBanned();
	public boolean isFlooding(int limit, long span);
	public void ban(long t);
	public void close();
	public InetAddress getAddress();
	public void tell(String type, String msg);
	public int getStatus();
	public void setStatus(int status);
	public Vector<Integer> getChannels();
	public boolean joinChan(int chan);
	public boolean partChan(int chan);
}
