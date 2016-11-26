package org.chernovia.lib.netgames.roomserv;

//IDEA: Lobbies?

import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

abstract public class Room extends Thread {

	abstract public void runRoom() throws Exception;
	//abstract public Occupant newOccupant(Connection conn);
	public Occupant newOccupant(Connection conn) { return new Occupant(conn); }

	public static final char
	NEXTCHR = '!',NEWCHR = '+',REFCHR = '=';
	private String cr;
	private RoomManager roomMgr = null;
	private Vector<Object> occupants, observers;
	private Occupant creator;
	private long created;
	private int id;
	private int TIMEOUT = 300, MAX_OCCUPANCY = 999;
	private boolean STARTED,CLOSING,CLOSED;
	private static boolean MID_JOIN = false;
	private static int ROOM_COUNT = 0;
	private static Stack<Integer> FREE_IDS = new Stack<Integer>();

	public Room(Connection c, RoomManager gamebot) {
		roomMgr = gamebot;
		cr = NetServ.newline[roomMgr.getServ().getType()];
		created = System.currentTimeMillis();

		STARTED = false; CLOSING = false; CLOSED = false;
		occupants = new Vector<Object>();
		observers = new Vector<Object>();

		ROOM_COUNT++;
		if (FREE_IDS.empty()) id = ROOM_COUNT;
		else id = (FREE_IDS.pop()).intValue();
		creator = enterRoom(c);
		setName(roomMgr.getRoomNoun() +
				"_" + creator.getConn() + getName());
	}
	
	public int getID() { return id; }

	//dangerous to implement...
	public void timeout(Connection rc) {}

	public RoomManager getMgr() { return roomMgr; }

	public Occupant getCreator() { return creator; }
	public long getTimeCreated() { return created; }

	public void setTimeout(int t) { TIMEOUT = t; }
	public int getTimeout() { return TIMEOUT; }

	public static void setMidJoin(boolean midjoin) {
		MID_JOIN = midjoin;
	}
	public static boolean getMidJoin() {
		return MID_JOIN;
	}

	public boolean started() { return STARTED; }
	public boolean isClosing() { return CLOSING; }

	public int getNumOccupants() {
		return occupants.size();
	}

	public int getMaxOccupancy() { return MAX_OCCUPANCY; }
	public void setMaxOccupancy(int occupancy) {
		MAX_OCCUPANCY = occupancy;
	}

	//TODO: here's what's tricky.
	//given a Conn, should we make an Occupant,
	//OK, I think I see...
	public Occupant enterRoom(Connection c) {
		Occupant occ = null;
		if (isAlive() && !MID_JOIN) {
			c.tell("Too late!  Already begun.");
		}
		else if (getOccupant(c) != null) {
			c.tell("You're already there.");
		}
		else if (getNumOccupants() >= MAX_OCCUPANCY) {
			c.tell("Sorry, " +
					roomMgr.getRoomNoun() + " full.");
		}
		else {
			occ = newOccupant(c);
			if (occ != null) {
				occupants.addElement(occ);
				occ.setRoom(this);
				getMgr().getServ().broadcast(c.getHandle() +
						" enters " +  (creator != null ?
								creator.getConn().getHandle() :
									c.getHandle()) + "'s " +
									getMgr().getRoomNoun());
			}
		}
		return occ;
	}

	public void clearConn(Connection c) {
		c.prod();
		c.setRoom(null);
	}

	public void removeAllOccupants() {
		for (Enumeration<Object> e =
			occupants.elements();e.hasMoreElements();) {
			clearConn(
					((Occupant)e.nextElement()).getConn());
		}
		occupants.clear();
	}

	public Occupant getOccupant(Connection c) {
		for (Enumeration<Object> e =
			occupants.elements();e.hasMoreElements();) {
			Occupant occ = (Occupant)e.nextElement();
			if (c == occ.getConn()) return occ;
		}
		return null;
	}

	public Occupant getOccupantByConn(Connection c) {
		//System.out.println("Finding..." + c);
		for (Enumeration<Object> e =
			occupants.elements();e.hasMoreElements();) {
			Occupant o = (Occupant)e.nextElement();
			if (o.getConn().equals(c)) { return o; }
		}
		//System.out.println("Not found.");
		return null;
	}

	public Occupant getOccupantByIndex(int index) {
		return (Occupant)occupants.elementAt(index);
	}

	public void replace(Occupant o, Connection c) {
		o.getConn().setRoom(null);
		o.setConn(c);
		o.setRoom(this);
	}

	public void exitRoom(Occupant occ) {
		if (occ != null) {
			clearConn(occ.getConn());
			occupants.remove(occ);
			getMgr().getServ().broadcast(
					occ.getConn().getHandle() + " leaves " +
					getCreator().getConn().getHandle() +
					"'s " + getMgr().getRoomNoun());
		}
		else System.err.println("Null Occupant in " +
		"exitRoom()");
	}

	public void addObs(Connection c) {
		if (!observers.contains(c)) {
			observers.addElement(c);
			spam(c.getHandle() + " is now observing");
		}
		else c.tell("You're already observing.");
	}

	public void removeObs(Connection c) {
		if (observers.remove(c)) {
			c.tell("No longer observing.");
		}
		else c.tell("Whatever.");
	}

	public boolean isObserving(Connection c) {
		return observers.contains(c);
	}

	public void roomTell(Connection c, String msg) {
		roomTell(c,msg,0,true);
	}
	public void roomTell(Connection c, String msg,
			boolean qtell) {
		roomTell(c,msg,0,qtell);
	}
	public void roomTell(Connection c,
			String msg, int verbosity, boolean qtell) {
		if (c.getVerbosity() > verbosity  &&
			!c.isBorg() && getOccupant(c) != null) {
			c.tell(msg,qtell,false);
		}
	}

	public Connection nextOccupantConn(Enumeration<Object> e) {
		return ((Occupant)e.nextElement()).getConn();
	}

	public void spam(String msg) { spam(msg,0,true); }

	public void spam(String msg, int verbosity, boolean qtell) {
		for	(Enumeration<Object> e =
			occupants.elements();e.hasMoreElements();) {
			roomTell(nextOccupantConn(e),msg,
					verbosity,true);
		}
		for (Enumeration<Object> e =
			observers.elements();e.hasMoreElements();) {
			Connection c = nextOccupantConn(e);
			if (c.getVerbosity() > verbosity) {
				c.tell(msg,qtell,false);
			}
		}
	}

	public void spamRoom() {
		for	(Enumeration<Object> e =
			occupants.elements();e.hasMoreElements();) {
			Connection c = nextOccupantConn(e);
			roomTell(c,toString(c,NEXTCHR),0,true);
		}
		for (Enumeration<Object> e =
			observers.elements();e.hasMoreElements();){
			Connection c = nextOccupantConn(e);
			c.tell(toString(c,NEXTCHR),true,false);
		}
	}

	public String showOccupants() {
		StringBuffer S = new StringBuffer();
		Connection c = null;
		for	(Enumeration<Object> e =
			occupants.elements();e.hasMoreElements();) {
			c = nextOccupantConn(e);
			if (c.pondering()) S.append("(!)");
			S.append(c.getHandle() + cr);
		}
		return S.toString();
	}

	public int getManuals() {
		int m = 0; Enumeration<Object> e;
		for (e=occupants.elements();e.hasMoreElements();)
			if (!nextOccupantConn(e).isAuto()) m++;
		return m;
	}

	public void closeRoom() { closeRoom(false); }
	public void closeRoom(boolean waitClose) {
		for (int i = 0; i < getNumOccupants(); i++) {
			getOccupantByIndex(i).getConn().prod();
		}
		if (isAlive()) {
			spam("Closing active room...");
			CLOSING = true;
			if (waitClose) try { join(); }
			catch (InterruptedException ignore) {}
		}
		else mopUp();
	}

	@Override
	public void run() {
		if (!isClosing()) {
			STARTED = true;
			try { runRoom(); } catch (Exception augh) {
				augh.printStackTrace(roomMgr.getLog());
				spam(augh.toString());
				roomMgr.getServ().broadcast("Augh: " +
						augh.toString());
			}
		}
		mopUp();
	}

	private void mopUp() {
		if (CLOSED) return; else CLOSED = true;
		removeAllOccupants();
		roomMgr.getRooms().remove(this);
		FREE_IDS.push(new Integer(id));
		roomMgr.getServ().broadcast(
				"Room #" + id + " (" +
				getCreator().getConn().getHandle() +
		") closed.");
	}

	public String toString(Connection c) {
		return toString(c,REFCHR);
	}

	public String toString(Connection c, char dumptype) {
		if (c.isGUI()) return dumptype + toGUI();
		else return toString();
	}

	public String toGUI() { return toString(); }

	@Override
	public String toString() {
		return "ID: " + id + ", creator: " +
		creator.getConn().getHandle();
	}

	public void shiftAll(int n) {
		for (int i=0;i<n;i++) {
			occupants.insertElementAt(
					occupants.elementAt(occupants.size()-1),0);
			occupants.removeElementAt(occupants.size()-1);
		}
	}

}