package org.chernovia.lib.netgames.zugserv;

import java.awt.Color;
import java.util.Date;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import com.google.gson.*;

public abstract class SimpleRoom {

	public static String ROOM_NOUN = "Room";		
	public static String ROOM_MSG = "room_msg";
	public static String CR = "\n";
	public static final int 
	STATUS_OK = 0, 
	STATUS_ERR_ALREADY_OCCUPYING = 1, 
	STATUS_ERR_ROOM_FULL = 2,
	STATUS_ERR_UNOCCUPIED_ROOM = 3,
	STATUS_ERR_CLOSING = 4,
	STATUS_ERR_ANOTHER_ROOM = 5,
	STATUS_ERR_BAD_PASS = 6, 
	STATUS_ERR_BANNED = 7,
	STATUS_ERR_UNKNOWN = 8;
	public static final String[] StatusTxt = { 
	"OK,", "already occupying room","room full","room not occupied","room closing","in another room","bad password","banned","unknown" };
	private boolean verbose = true;
	private static Stack<Integer> FREE_IDS = new Stack<Integer>();
	private static int ROOM_COUNT = 0;
	private boolean closing = false;
	private int max_occupancy = 999;
	private long created;
	private SimpleOccupant creator;
	private String title;
	private Vector<SimpleOccupant> occupants = new Vector<SimpleOccupant>();
	private Vector<SimpleOccupant> mods = new Vector<SimpleOccupant>(); 
	private Vector<Ban> ban_list = new Vector<Ban>();
	private int id;
	private SimpleRoomListener roomServ;
	private boolean locked = false;
	private String password = "password";
	private Color color;
	
	public SimpleRoom(SimpleOccupant o, String t, SimpleRoomListener l) {
		roomServ = l;
		creator = o;
		created = System.currentTimeMillis();
		title = t;
		color = rndCol(); 
		ROOM_COUNT++;
		if (FREE_IDS.empty()) id = ROOM_COUNT;
		else id = (FREE_IDS.pop()).intValue();
		roomServ.roomCreated(this);
		enterRoom(creator);
	}
	
	public SimpleRoomListener getListener() { return roomServ; }
	public SimpleOccupant getCreator() { return creator; }
	public String getTitle() { return title; }
	public void setTitle(String t) { title = t; roomServ.update(this);}
	public Color getColor() { return color; }
	public int getID() { return id; }
	public boolean isClosing() { return closing; }
	public void setLock(boolean lock) { locked = lock; roomServ.update(this); }
	public boolean isLocked() { return locked; }
	public String getPwd() { return password; }
	public void setPwd(String pwd) { password = pwd; locked = true; tell(creator, "Password set."); roomServ.update(this); }
	
	public SimpleOccupant getOccupant(String handle) {
		for (SimpleOccupant o: occupants) if (o.getConn().getHandle().equalsIgnoreCase(handle)) return o;
		return null;
	}
	
	public SimpleOccupant getOccupant(SimpleOccupant conn) {
		for (SimpleOccupant o: occupants) if (o == conn) return o;
		return null;
	}
	
	public int enterRoom(SimpleOccupant o) { return enterRoom(o,"password"); }
	public int enterRoom(SimpleOccupant o,String pwd) {
		Connection conn = o.getConn(); Ban ban = getBan(o);
		if (o.getRoom() == this) { //(getOccupant(o) != null) {
			if (verbose) tell(o,"You're already in!" );
			return STATUS_ERR_ALREADY_OCCUPYING; 
		}
		else if (occupants.size() == max_occupancy) {
			if (verbose) tell(o,ROOM_NOUN + " full!");
			return STATUS_ERR_ROOM_FULL;
		}
		else if (locked && !password.equals(pwd)) {
			if (verbose) tell(o,"Bad password!");
			return STATUS_ERR_BAD_PASS;
		}
		else if (ban != null && ban.inEffect()) {
			if (verbose) tell(o,"Sorry, you're banned until " + new Date(ban.getEnd()).toString());
			return STATUS_ERR_BANNED;
		}
		if (o.getRoom() != null) o.getRoom().exitRoom(o,false,true);
		o.setRoom(this); occupants.add(o);
		if (verbose) spam(conn.getHandle() + " enters");
		roomServ.update(this);
		return STATUS_OK;
	}
	
	public int exitRoom(SimpleOccupant o) { return exitRoom(o,true, false); } 
	public int exitRoom(SimpleOccupant o, boolean closeEmpty, boolean quietly) {
		Connection conn = o.getConn();
		if (o.getRoom() != this) { //if (getOccupant(c) == null) {
			if (verbose) tell(o,"Somehow, you're not in there. Hrumph." );
			return STATUS_ERR_UNOCCUPIED_ROOM;
		}
		if (verbose) spam(null,"room",conn.getHandle() + " exits");
		occupants.remove(o);
		o.setRoom(null);
		if (occupants.size() == 0 && closeEmpty) closeRoom();
		else if (!quietly) roomServ.update(this);
		return STATUS_OK; 
	}
	
	public void closeRoom() { 
		closing = true;
		FREE_IDS.push(new Integer(id));
		roomServ.roomClosing(this);
	}
	
	public long ban(SimpleOccupant b, SimpleOccupant o, int t) {
		if (o == null) return -1; 
		Ban ban = getBan(o);
		if (ban == null) {
			ban = new Ban(o.getConn().getHandle(),t * 1000,o.getConn().getAddress(),b.getConn().getHandle());
			ban_list.add(ban);
		}
		else ban.extend(t * 1000);
		spam(o.getConn().getHandle() + " has been banned for " + t + " seconds");
		exitRoom(o);
		return ban.getEnd();
	}
	
	public void unBan(SimpleOccupant o) {
		if (o != null) ban_list.remove(getBan(o));
	}
	
	public Ban getBan(SimpleOccupant o) {
		if (o == null) return null;
		for (Ban ban: ban_list) if (ban.match(o.getConn().getHandle(), o.getConn().getAddress())) return ban;
		return null;
	}
	
	public String getBanList() {
		String list = "Banned: " + CR;
		for (Ban ban: ban_list) if (ban.inEffect()) list += ban + CR;
		return list;
	}
	
	public void mod(SimpleOccupant o, boolean mod) {
		if (mod) if (o != null && !mods.contains(o)) mods.add(o);
		else if (o != null) mods.remove(o);
	}
	
	public boolean isMod(SimpleOccupant o) {
		return (creator == o || mods.contains(o));
	}
	
	public void spam(String type, Object obj) { spam(null,type, obj); }
	public void spam(SimpleOccupant occ, String type, Object obj) { 
		for (SimpleOccupant o: occupants) if (o != occ) o.getConn().tell(type,obj.toString());;
	}
	public void spam(String msg) { spam(null,ROOM_MSG,msg); }
	public void spam(String type, String msg) { spam(null,type,msg); }
	public void spam(SimpleOccupant occ, String type, String msg) {
		for (SimpleOccupant o: occupants) if (o != occ) tell(o,type,msg);
	}
		
	public void tell(SimpleOccupant o,String msg) { tell(o,ROOM_MSG,msg); }
	public void tell(SimpleOccupant o, String type, String msg) { 
		JsonObject obj = new JsonObject();
		obj.add("type", new JsonPrimitive(ROOM_MSG));
		obj.add("msg", new JsonPrimitive(msg));
		o.getConn().tell(ZugServ.MSG_TXT,obj.toString());
	}
	
	public Vector<SimpleOccupant> getOccupants() { return occupants; }
	
	public String toString() {
		String str = 
				"Room #" + id + " (" + creator.getConn().getHandle() + ")" + CR +
				"Title: " + title + CR +
				"Locked: " + locked + CR + 
				"Date created: " + new Date(created).toString() + CR; 
		for (SimpleOccupant o: occupants) str += o.getConn().getHandle() + (isMod(o) ? "(+)" : "") + CR;
		return str;
	}
	
	public JsonObject serialize() {
		JsonObject obj = new JsonObject();
		obj.add("id",new JsonPrimitive(id));
		obj.add("title",new JsonPrimitive(title));
		obj.add("creator",new JsonPrimitive(creator.getConn().getHandle()));
		obj.add("locked",new JsonPrimitive(locked));
		obj.add("created", new JsonPrimitive(created));
		obj.add("color", new JsonPrimitive(colorString(color)));
		JsonArray array = new JsonArray();
		for (SimpleOccupant o: occupants) array.add(o.serialize());
		obj.add("occupants", array);
		return obj;
	}
	
	public static Color rndCol() {
		Random random = new Random();
		final float hue = random.nextFloat();
		// Saturation between 0.1 and 0.3
		final float saturation = (random.nextInt(2000) + 1000) / 10000f;
		final float luminance = 0.9f;
		return Color.getHSBColor(hue, saturation, luminance);
	}
	
	public static String colorString(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
	
}
