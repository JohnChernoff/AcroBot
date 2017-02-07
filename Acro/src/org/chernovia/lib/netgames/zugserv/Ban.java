package org.chernovia.lib.netgames.zugserv;

import java.net.InetAddress;

public class Ban {
	private String bannor;
	private long banStart, banEnd;
	private InetAddress address;
	private String handle;
	
	public Ban(String h,long t,InetAddress a, String b) { this(h,System.currentTimeMillis(),t,a,b); }
	public Ban(String h, long startTime, long t, InetAddress a, String b) {
		handle = h; banStart = startTime; 
		banEnd = startTime + t; address = a;
		bannor = b;
	}
	
	public String getBannor() { return bannor; }
	
	public void extend(int t) {
		banEnd = System.currentTimeMillis() + t;
	}
	
	public long getEnd() { return banEnd; }
	
	public boolean inEffect() {
		long t = System.currentTimeMillis();
		return t > banStart && t < banEnd;
	}
	
	//private boolean addressMatch(InetAddress a) { return addressMatch(a,-1); }
	private boolean addressMatch(InetAddress a, int level) {
		if (address == null || a == null) return false;
		if (level == -1) return a.equals(address);
		else {
			for (int i=0;i<level;i++) {
				if (a.getAddress()[i] != address.getAddress()[i]) return false;
			}
			return true;
		}
	}
	
	private boolean handMatch(String h) {
		return handle.equals(h);
	}
	
	public boolean match(String h, InetAddress a) { return match(h,a,-1); }
	public boolean match(String h, InetAddress a, int l) {
		if (a != null) return addressMatch(a,l);
		else return handMatch(h);
	}
	
	public String toString() {
		return handle + ", " + address;
	}
}
