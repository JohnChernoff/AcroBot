package org.chernovia.lib.netgames.zugserv;

import java.util.Vector;

public abstract class ConnAdapter implements Connection {
	
	private Vector<Integer> channels = new Vector<Integer>();
	private String handle;
	private ZugServ server;
	private boolean auto;
	private boolean borg;
	private int status;
	
	public void setStatus(int s) { status = s; }
	public int getStatus() { return status; }
	
	@Override
	public void setHandle(String h) { handle = h; }

	@Override
	public String getHandle() { return handle; }

	@Override
	public void setServ(ZugServ serv) { server = serv; }

	@Override
	public ZugServ getServ() { return server; }

	@Override
	public boolean isAuto() { return auto; }

	@Override
	public void automate(boolean a) { auto = a; }

	@Override
	public boolean isBorg() { return borg; }

	@Override
	public void borg(boolean b) { borg = b; }

	@Override
	public boolean isBanned() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFlooding(int limit, long span) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void ban(long t) {
		// TODO Auto-generated method stub
	}
	
	public Vector<Integer> getChannels() { return channels;	}
	public boolean joinChan(int c) { 
		Integer chan = new Integer(c);
		if (c >= 0 && !channels.contains(chan)) { channels.addElement(new Integer(c)); return true; }
		else return false;
	}
	public boolean partChan(int c) { return channels.remove(new Integer(c)); }

}
