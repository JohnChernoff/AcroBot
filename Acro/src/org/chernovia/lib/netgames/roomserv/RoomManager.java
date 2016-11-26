//TODO:
//(~ means possibily fixed)
//Document!
//have accessor methods do bounds checking
//make sure lists are sent with qtell
//check if observing commands work, there could be bugs!
//?maybe qtell should be called listTell
//~leaving a room before starting can lead to buggy scenarios
//~especially if creator does it, then opens a new room

package org.chernovia.lib.netgames.roomserv;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.chernovia.lib.misc.MiscUtil;

abstract public class RoomManager implements ConnListener {

	protected abstract Room newRoom(Connection creator);

	public abstract boolean newMsg(Connection conn, String msg);

	public static String VERSION = "0.1";

	private NetServ serv;

	private Vector<Room> rooms;

	private Vector<String> mgrVec;

	private Vector<Connection> GUIVec;

	private RoomMonitor roomMon;

	private PrintWriter log;

	private String CR, roomNoun = "room";

	private int MAXROOM = 64;
	
	private String adminPrefix = "!", longPrefix = "@";

	/**
	 * Constructs a Game Bot. Note that setServ()
	 * and Room.setMgr(RoomManager) should be
	 * called after construction. (?)
	 *
	 * @see #setServ()
	 * @see #JBoard.setBot(JGameBot)
	 */
	public RoomManager() {
		log = new PrintWriter(System.out, true);
		rooms = new Vector<Room>();
		GUIVec = new Vector<Connection>();
		mgrVec = new Vector<String>();

		// Managers on ICC.
		// TODO: Store these in a file.
		mgrVec.add("Scherzo");
		mgrVec.add("DuckStorm");
		mgrVec.add("OrionsKnight");
	}

	public RoomManager(long timeout) {
		this();
		roomMon = new RoomMonitor(timeout, this);
		roomMon.start();
	}

	public Vector<Room> getRooms() {
		return rooms;
	}

	public void setRoomNoun(String n) {
		roomNoun = n;
	}

	public String getRoomNoun() {
		return roomNoun;
	}

	public void setServ(NetServ server) {
		serv = server;
		CR = NetServ.newline[serv.getType()];
	}

	public NetServ getServ() {
		return serv;
	}

	public Vector<String> getManagers() {
		return mgrVec;
	}

	public void setManagers(Vector<String> mgrs) {
		mgrVec = mgrs;
	}

	public boolean isManager(String manHand) {
		return mgrVec.contains(manHand);
	}

	public void setLog(PrintWriter pw) {
		log = pw;
	}

	public PrintWriter getLog() {
		return log;
	}

	public boolean isGUI(String handle) {
		return GUIVec.contains(handle);
	}

	public int getMaxRoom() {
		return MAXROOM;
	}

	public void setMaxRoom(int n) {
		MAXROOM = n;
	}

	//listener overwrites
	public void loggedIn(Connection conn) {
		getServ().tell(conn.getHandle(),
		"Hello, " + conn.getHandle() + "!",false,false);
	}

	public void disconnected(Connection conn) {
		getServ().broadcast(conn.getHandle() + " left.");
		if (conn.getRoom() != null) {
			conn.getRoom().exitRoom(conn.getRoom().getOccupant(conn));
		}
	}
	
	public void setAdminPrefix(String p) { adminPrefix = p; }
	public void setLongPrefix(String p) { longPrefix = p; }

	public final boolean handleCmd(Connection conn, String msg) {

		//getLog().println("Handing message: " + msg);
		boolean longarg = false;
		if (msg.startsWith(adminPrefix)) {
			return handleAdminCmd(msg.substring(1), conn);
		}
		//if (msg.startsWith("#") && msg.length() > 1) { msg = "@kib " + msg.substring(1); }
		if (msg.startsWith(longPrefix) && msg.length() > 1) {
			msg = msg.substring(1);
			longarg = true;
		}

		StringTokenizer ST = new StringTokenizer(msg);
		Method[] meths = null;
		String cmd = null;

		int tokens = -1;
		if (ST.countTokens() > 0) {
			cmd = ST.nextToken();
			if (longarg)
				tokens = 1;
			else
				tokens = ST.countTokens();
			meths = MiscUtil.getAllMethods(getClass());
		}

		if (meths != null)
			for (int m = 0; m < meths.length; m++) {
				if (meths[m].getName().equalsIgnoreCase("cmd" + cmd)
						&& meths[m].getParameterTypes().length == tokens + 1) {
					Object[] params = new Object[tokens + 1];
					params[0] = conn;
					for (int i = 1; i <= tokens; i++) {
						if (longarg) {
							params[i] = MiscUtil.tokenizerToString(ST);
						} else
							params[i] = ST.nextToken();
					}
					try {
						meths[m].invoke(this, params);
					} catch (Exception augh) {
						log.println("Augh: " + augh);
						augh.printStackTrace(log);
						return false;
					}
					return true;
				}
			}
		//not a cmd, so check if asked something
		if (conn.pondering()) {
			conn.setResponse(msg);
			return true;
		}
		return false;
	}

	protected boolean handleAdminCmd(String msg, Connection conn) {
		if (!mgrVec.contains(conn.getHandle())) {
			return false;
		}

		Method[] meths = MiscUtil.getAllMethods(getClass());
		if (meths != null) {
			for (int m = 0; m < meths.length; m++) {
				if (meths[m].getName().equalsIgnoreCase("adminCmd" + msg)) {
					try {
						meths[m].invoke(this, (Object[])null);
						return true;
					} catch (Exception e) {
						return false;
					}
				}
			}
		}

		return false;
	}

	// Admin commands.
	protected void adminCmdReconnect() {
		System.exit(2);
	}

	protected void adminCmdDie() {
		System.exit(1);
	}

	//server commands

	protected void cmdOpen(Connection c) {
		Room r = c.getRoom();
		if (r == null || !isCreator(c, r)) {
			serv.broadcast(c.getHandle() + 
			" creates a " + roomNoun + ".");
			rooms.addElement(newRoom(c));
		} else {
			c.tell("You already have a " + roomNoun + ".");
		}
	}

	protected void cmdStart(Connection c) {
		Room r = c.getRoom();
		if (r == null) {
			c.tell("You're not at any " + roomNoun + ".");
		} else if (!isCreator(c, r)) {
			c.tell("You don't own that " + roomNoun + ".");
		} else {
			if (!r.isAlive()) {
				r.start();
			} else {
				c.tell("That game's already begun.");
			}
		}
	}

	protected void cmdClose(Connection c) {
		Room r = c.getRoom();
		if (r == null) {
			c.tell("You're not at any " + roomNoun + ".");
		} else if (!isCreator(c, r)) {
			c.tell("You don't own that " + roomNoun + ".");
		} else
			r.closeRoom();
	}

	protected void cmdLeave(Connection c) {
		Room r = c.getRoom();
		if (r == null) {
			c.tell("You're not at a " + roomNoun + ".");
		} else
			r.exitRoom(r.getOccupant(c));
	}

	protected void cmdUnobs(Connection c) {
		Room r = findRoomByObs(c);
		if (r != null)
			r.removeObs(c);
		else
			c.tell("No such " + roomNoun + ".");
	}

	protected void cmdUnobs(Connection c, String str) {
		Room r = findRoom(str);
		if (r != null)
			r.removeObs(c);
		else
			c.tell("No such " + roomNoun + ".");
	}

	protected void cmdList(Connection c) {
		c.tell(showRooms(),true,false);
	}

	protected void cmdLook(Connection c) {
		Room r = c.getRoom();
		if (r == null)
			r = findRoomByObs(c);
		c.tell(r.getCreator().getConn().getHandle() + "'s " + roomNoun + ":");
		if (r != null)
			c.tell(r.toString(c),true,false);
		//else c.tell("(No " + roomNoun + ")");
	}

	protected void cmdOccupants(Connection c) {
		Room r = c.getRoom();
		if (r == null)
			r = findRoomByObs(c);
		c.tell("At your " + roomNoun + ":");
		if (r != null)
			c.tell(r.showOccupants());
		else
			c.tell("(No " + roomNoun + ")");
	}

	protected void cmdFinger(Connection c) {
		c.tell(c.toString(),true,false);
	}

	protected void cmdThreads(Connection c) {
		c.tell(showThreads(),true,false);
	}

	protected void cmdDump(Connection c) {
		c.tell(dump(),true,false);
	}

	protected void cmdGUI(Connection c) {
		if (!GUIVec.contains(c)) {
			GUIVec.add(c);
			c.tell("GUI set.");
		}
	}

	protected void cmdNoGUI(Connection c) {
		if (GUIVec.contains(c)) {
			GUIVec.remove(c);
			c.tell("GUI unset.");
		}
	}

	protected void cmdJoin(Connection c, String str) {
		if (c.getRoom() == null) {
			Room r = findRoom(str);
			if (r != null)
				r.enterRoom(c);
			else
				c.tell("No such " + roomNoun + ": " + str);
		} else {
			c.tell("First leave the " + roomNoun + " you're at.");
		}
	}

	protected void cmdObs(Connection c, String str) {
		if (c.getRoom() == null) {
			Room r = findRoom(str);
			if (r != null)
				r.addObs(c);
			else
				c.tell("No such " + roomNoun + ".");
		} else {
			c.tell("First leave the " + roomNoun + " you're at.");
		}
	}

	protected void cmdShow(Connection c, String handle) {
		c.tell(handle + ":");
		Connection c2 = serv.getConnByHandle(handle);
		if (c2 != null)
			c.tell(c2.toString(),true,false);
		else
			c.tell(handle + ": not connected.");
	}

	protected void cmdVerb(Connection c, String verbosity) {
		int v = MiscUtil.strToInt(verbosity);
		if (v >= 0 && v <= Connection.MAX_VERB) {
			c.setVerbosity(v);
			c.tell("Verbosity set to: " + v);
		} else
			c.tell("Bad verbosity, bud.");
	}

	protected void cmdOff(Connection c) {
		if (isManager(c.getHandle()))
			augh("Bye!",true);
	}

	protected void cmdManagers(Connection c) {
		c.tell(MiscUtil.listVec(mgrVec, CR),true,false);
	}

	protected void cmdTimeout(Connection c, String to) {
		Room r = c.getRoom();
		if (r == null) {
			c.tell("You're not at any " + roomNoun + ".");
		} else if (!isCreator(c, r)) {
			c.tell("You didn't create the " + roomNoun + " you're at.");
		} else {
			int t = MiscUtil.strToInt(to);
			if (t > 0 && t < 1000)
				r.setTimeout(t);
			r.spam("Turn time: " + t + " seconds.");
		}
	}

	protected void cmdAuto(Connection c) {
		Room r = c.getRoom();
		c.automate(!c.isAuto()); //shrug
		if (r != null) {
			r.spam(c.getHandle() + " sets auto: " + c.isAuto());
		}
	}

	//force auto
	protected void cmdAuto(Connection c, String handle) {
		Connection c2 = serv.getConnByHandle(handle);
		if (chkForce(c, c2)) {
			Room r = c2.getRoom();
			r.spam(c + " is automating " + handle + "...");
			c2.automate(true);
			c.tell("Automated " + handle + ".");
		}
	}

	//information functions
	protected boolean chkForce(Connection c, Connection c2) {
		boolean forceChk = false;
		Room r = c.getRoom();
		if (!isManager(c.getHandle()) && r == null) {
			c.tell("You're not at any " + roomNoun + ".");
		} else if (!isManager(c.getHandle()) && !isCreator(c, r)) {
			c.tell("You can't do that.");
		} else if (c2 == null) {
			c.tell("No such connection.");
		} else if (c2.getRoom() == null) {
			c.tell(c2.getHandle() + " isn't at any + " + roomNoun + "!");
		} else {
			forceChk = true;
		}
		return forceChk;
	}

	public Room findRoom(String str) {
		try {
			return findRoomByID(Integer.parseInt(str));
		} catch (NumberFormatException oops) {
			return findRoomByCreator(str);
		}
	}

	public Room findRoomByID(int id) {
		Room r;
		for (Enumeration<Room> e = rooms.elements(); e.hasMoreElements();) {
			r = e.nextElement();
			if (r.getID() == id)
				return r;
		}
		return null;
	}

	public Room findRoomByCreator(String creator) {
		Room r;
		for (Enumeration<Room> e = rooms.elements(); e.hasMoreElements();) {
			r = e.nextElement();
			if (r.getCreator().getConn().getHandle().equalsIgnoreCase(creator))
				return r;
		}
		return null;
	}

	//TODO: what if more than 1 board is being observed?
	public Room findRoomByObs(Connection rc) {
		Room r;
		for (Enumeration<Room> e = rooms.elements(); e.hasMoreElements();) {
			r = e.nextElement();
			if (r.isObserving(rc))
				return r;
		}
		return null;
	}

	public String showRooms() {
		StringBuffer S = new StringBuffer(roomNoun + " List:" + CR);
		Room r = null;
		for (Enumeration<Room> e = rooms.elements(); e.hasMoreElements();) {
			r = e.nextElement();
			S.append(roomNoun + " #" + r.getID() + "("
					+ r.getCreator().getConn().getHandle() + ") "
					+ (r.isAlive() ? " (Running) " : " (Open) "));
			S.append(CR + r.showOccupants());
		}
		return S.toString();
	}

	public String showThreads() {
		StringBuffer SB = new StringBuffer("Threads: " + CR);
		int a = Thread.activeCount();
		Thread[] ThreadList = new Thread[a];
		int e = Thread.enumerate(ThreadList);
		SB.append("Total Threads: " + a + CR);
		SB.append("Running Threads: " + e + CR);
		for (int x = 0; x < e; x++) {
			SB.append(ThreadList[x].toString() + CR);
			//ThreadList[x].dumpStack();
		}
		return SB.toString();
	}

	//Misc. functions

	public String dump() {
		return (VERSION + CR + System.getProperty("java.version") + CR
				+ System.getProperty("os.name") + CR
				+ System.getProperty("os.version") + CR
				+ System.getProperty("os.arch") + CR + System
				.getProperty("user.name"));
	}



	public boolean isCreator(Connection c, Room r) {
		return r.getCreator().getConn().equals(c);
	}

	//longarg commands
	protected void cmdKib(Connection c, String kib) {
		Room r = c.getRoom();
		if (r == null)
			r = findRoomByObs(c);
		if (r != null)
			r.spam(c + " kibitzes: " + kib);
	}
	
	protected void augh(String aughMsg) { 
		augh(aughMsg,false);
	}
	protected void augh(String aughMsg, boolean croak) {
		System.err.println(aughMsg);
		serv.broadcast(aughMsg);
		if (croak) System.exit(-1);
	}
}