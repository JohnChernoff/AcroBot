package org.chernovia.lib.netgames.roomserv;

//BUG: ask is broken for Room-less servers
public abstract class ConnAdapter implements Connection, Runnable {

	public static final int NO_CHAN = -1;
	private NetServ serv;
	private Room room;
	private String handle;
	protected String response;
	protected long lastTell;
	private int verbosity, channel;
	private boolean GUI, AUTO, BORG;
	Thread askThread;

	public ConnAdapter(NetServ s) {
		serv = s;
		GUI = false; AUTO = false; BORG = false;
		handle = "new"; room = null; response = null;
		verbosity = MAX_VERB; channel = NO_CHAN;
		askThread = null; channel = 0; lastTell = 0;
	}

	class AskThread extends Thread {
		Connection conn; int timeout;
		public AskThread(Connection rc, int t) {
			conn = rc; timeout = t;
		}
		@Override
		public void run () {
			try { Thread.sleep(timeout*1000); }
			catch (InterruptedException e) { return; }
			if (room != null) room.timeout(conn);
		}
	}

	public void run() {}
	public void handleMsg(String msg) {
		serv.newMsg(this,msg);
		lastTell = System.currentTimeMillis();
	}
	public void tell(String msg) {
		serv.tell(handle,msg,false,false);
	}
	public void tell(String msg,boolean quietly) {
		serv.tell(handle,msg,quietly,false);
	}
	public void tell(String msg,boolean quietly,boolean datagram) {	serv.tell(handle,msg,quietly,datagram); }
	public NetServ getServ() { return serv; }
	public boolean equals(Connection rc) {
		return getHandle().equals(rc.getHandle());
	}
	public void setHandle(String h) { handle = h; }
	public String getHandle() { return handle; }
	public boolean isAuto() { return AUTO; }
	public void automate(boolean auto) { AUTO = auto; }
	public boolean isBorg() { return BORG; }
	public void borg(boolean borg) { BORG = borg; }
	public int getVerbosity() { return verbosity; }
	public void setVerbosity(int v) { verbosity = v; }
	public boolean isGUI() { return GUI; }
	public void setGUI(boolean gui) { GUI = gui; }
	public Room getRoom() { return room; }
	public void setRoom(Room r) { room = r; }
	public boolean pondering() {
		return askThread != null && askThread.isAlive();
	}
	public void prod() {
		if (pondering()) askThread.interrupt();
	}
	public void setResponse(String r) {
		response = r; prod();
	}
	public String ask(String h, int t) {
		return ask(h,"",t);
	}
	public int ask(String q, int d, int t) {
		try { return Integer.parseInt(ask(q,"" + d, t)); }
		catch (NumberFormatException e) { return d; }
	}
	public String ask(String query, String def, int timeout) {
		if (room == null) return def;
		if (room.getManuals() == 0) {
			try { Thread.sleep(1000); }
			catch (InterruptedException ignore) {}
		}
		if (isBorg() || room.isClosing())  return def;
		tell(query);
		//if (GUI) tell(NetServ.SERV_DG + " " + NetServ.DG_ASK + " " + query + " " + def + " " + timeout);	
		if (isAuto()) return def;
		room.getMgr().getLog().println("Asking: " + handle);
		//ok, here we go...
		response = "";
		askThread = new AskThread(this,timeout);
		askThread.start();
		try { askThread.join(); }
		catch (InterruptedException ignore) {}
		if (response.equals("")) response = def;
		return response;
	}
	
	public int getChan() { return channel; }
	public void setChan(int chan) { 
		if (chan >= NO_CHAN && chan < getServ().getMaxChannels()) {
			channel = chan;
			//if (GUI) {
			//	getServ().tch(channel,DG(NetServ.DG_CHAN,
			//	NetServ.DG_CHAN_LEAVE + NetServ.DG_DELIM + handle),
			//	false,true);
			//	getServ().tch(channel,DG(NetServ.DG_CHAN,
			//	NetServ.DG_CHAN_ENTER + NetServ.DG_DELIM + handle),
			//	false,true);
			//}
			//if (chan == NO_CHAN) tell("Channel unset.");
			//else tell("Channel set to: " + chan);
			//tell(DG(NetServ.DG_CHAN,"" + chan),false,true);
		}
		else tell("Invalid channel");
	}
	@Override
	public String toString() {
		return getHandle();
	}
	public long idleTime() {
		return System.currentTimeMillis() - lastTell;
	}
}
