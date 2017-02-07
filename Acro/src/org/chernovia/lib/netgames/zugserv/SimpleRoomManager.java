package org.chernovia.lib.netgames.zugserv;

import java.util.Vector;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class SimpleRoomManager implements SimpleRoomListener {
	
	public static final String ROOM_MGR_MSG = "room_mgr_msg";
	protected static String cr = "\n";
	protected static final String ERR_MOD = "Sorry, you need to be a mod to do that.",
	ERR_CREATOR = "Sorry, you need to be the creator/owner of the room to do that.",
	ERR_NO_ROOM = "You need to be in a room to do that.  Try 'list' to show existing ones, 'join' to join one, or 'new' to create one.";
	protected Vector<SimpleOccupant> occupants = new Vector<SimpleOccupant>();
	protected Vector<SimpleRoom> rooms = new Vector<SimpleRoom>();
	boolean verbose = true;

	@Override
	public void roomCreated(SimpleRoom room) {
		rooms.add(room); update(room);
	}

	@Override
	public void roomClosing(SimpleRoom room) {
		rooms.remove(room); update(room);
	}
	
	public JsonObject dumpAll() {
		JsonObject list = new JsonObject();
		JsonArray roomArray = new JsonArray();
		for (SimpleRoom r: rooms) roomArray.add(r.serialize());
		list.add("rooms", roomArray);
		return list;
	}
	
	public void mgrTell(SimpleOccupant o, String msg) { mgrTell(o,ROOM_MGR_MSG,msg); }
	public void mgrTell(SimpleOccupant o, String type, String msg) {
		JsonObject obj = new JsonObject();
		obj.add("type", new JsonPrimitive(type));
		obj.add("msg", new JsonPrimitive(msg));
		o.getConn().tell(ZugServ.MSG_TXT, obj.toString());
	}
	
	public boolean available(String title) {
		for (SimpleRoom room: rooms) if (room.getTitle().equalsIgnoreCase(title)) return false;
		return true;
	}

	public boolean handleRoomCmds(SimpleOccupant occupant, String msg) {
		String[] args = msg.split(" "); String cmd = args[0];
		SimpleRoom room = occupant.getRoom();
		boolean isMod = room != null && room.isMod(occupant);
		boolean isCreator = room != null && room.getCreator().equals(occupant);
		
		if (msg.startsWith("!") && msg.length() > 1) {
			if (room != null) room.spam(occupant.getConn().getHandle() + ": " + msg.substring(1));
			else if (verbose) mgrTell(occupant,ERR_NO_ROOM);
			return true;
		}

		switch(args.length-1) {
			case 0: {
				if (cmd.equalsIgnoreCase("NEW")) {
					String title = occupant.getConn().getHandle() + "'s " + SimpleRoom.ROOM_NOUN;
					if (available(title)) setupRoom(occupant,title);
					else mgrTell(occupant,"Title already taken!");
				}
				else if  (cmd.equalsIgnoreCase("LIST")) mgrTell(occupant,listRooms());
				else if (cmd.equalsIgnoreCase("LEAVE")) {
					if (room == null) { if (verbose) mgrTell(occupant,ERR_NO_ROOM); }
					else occupant.getRoom().exitRoom(occupant);
				}
				else if (cmd.equalsIgnoreCase("LOCK")) {
					//if (room == null) { if (verbose) mgrTell(occupant,ERR_NO_ROOM); }
					if (isMod) room.setLock(true); else if (verbose) mgrTell(occupant,ERR_MOD);
				}
				else if (cmd.equalsIgnoreCase("UNLOCK")) {
					//if (room == null) { if (verbose) mgrTell(occupant,ERR_NO_ROOM); }
					if (isMod) room.setLock(false); else if (verbose) mgrTell(occupant,ERR_MOD);
				}
				else if (cmd.equalsIgnoreCase("BANLIST")) {
					//if (room == null) { if (verbose) mgrTell(occupant,ERR_NO_ROOM); }
					if (isMod) mgrTell(occupant,room.getBanList()); else if (verbose) mgrTell(occupant,ERR_MOD);
				}
				else return false;
				break;
			}
			case 1: {
				if (cmd.equalsIgnoreCase("NEW")) {
					if (available(args[1])) setupRoom(occupant,args[1]);
					else mgrTell(occupant,"Title already taken!"); 
				}
				else if (cmd.equalsIgnoreCase("JOIN")) {
					SimpleRoom r = findRoom(args[1]); if (r != null) r.enterRoom(occupant);
				}
				else if (cmd.equalsIgnoreCase("LEAVE")) {
					SimpleRoom r = findRoom(args[1]); if (r != null) r.exitRoom(occupant);
				}
				else if (cmd.equalsIgnoreCase("MOD")) {
					if (isMod) room.mod(room.getOccupant(args[1]),true); else if (verbose) mgrTell(occupant,ERR_MOD);
				}
				else if (cmd.equalsIgnoreCase("UNMOD")) {
					if (isMod) room.mod(room.getOccupant(args[1]),false); else if (verbose) mgrTell(occupant,ERR_MOD);
				}
				else if (cmd.equalsIgnoreCase("PASSWORD")) {
					if (isCreator) { room.setPwd(args[1]); } else if (verbose) mgrTell(occupant,ERR_CREATOR);
				}
				else if (cmd.equalsIgnoreCase("BAN")) ban(room,occupant,args[1],60);
				else if (cmd.equalsIgnoreCase("UNBAN")) { 
					if (isMod) room.unBan(getOccupant(args[1])); else if (verbose) mgrTell(occupant,ERR_MOD);
				}
				else return false;
				break;
			}
			case 2: {
				if (cmd.equalsIgnoreCase("JOIN")) {
					SimpleRoom r = findRoom(args[1]); if (r != null) r.enterRoom(occupant,args[2]);
				}
				else if (cmd.equalsIgnoreCase("BAN")) ban(room,occupant,args[1],getInt(args[2]));
				else return false;
				break;
			}
			default: return false;
		}
		return true;
	}
	
	public void ban(SimpleRoom room, SimpleOccupant bannor, String banHand, int t) {
		boolean isMod = room != null && room.isMod(bannor);
		if (isMod) {
			SimpleOccupant banned = room.getOccupant(banHand);
			if (banned == null) { if (verbose) mgrTell(bannor,"Occupant not found."); }
			else if (room.isMod(banned) && !room.getCreator().equals(bannor)) {
				if (verbose) mgrTell(bannor,ERR_CREATOR);
			}
			else if (!room.getCreator().equals(banned)) {
				room.ban(bannor,room.getOccupant(banned),60); 
			}
			else if (verbose) mgrTell(bannor,"You can't ban the owner/creator of the room!");
		}
		else if (verbose) mgrTell(bannor,ERR_MOD);
	}

	public SimpleRoom findRoom(String id) {
		for (SimpleRoom room: rooms) {
			if ((room.getID() + "").equalsIgnoreCase(id) || room.getTitle().equalsIgnoreCase(id)) {
				return room;
			}
		}
		return null;
	}
	
	public String listRooms() {
		String list = "Rooms: " + cr;
		for (SimpleRoom room: rooms) list += room;
		return list;
	}
	
	public JsonObject serializeAllRooms(boolean deep) {
		JsonObject obj = new JsonObject();
		JsonArray roomlist = new JsonArray();
		for (SimpleRoom room: rooms) {
			if (deep) roomlist.add(room.serialize());
			else {
				JsonObject roomObj = new JsonObject();
				roomObj.add("id", new JsonPrimitive(room.getID()));
				roomObj.add("title", new JsonPrimitive(room.getTitle()));
				roomObj.add("color", new JsonPrimitive(SimpleRoom.colorString(room.getColor())));
			}
		}
		if (deep) obj.add("roomlist_deep", roomlist); else obj.add("roomlist_shallow", roomlist);
		return obj;
	}
	
	public int getInt(String s) {
		try { return Integer.parseInt(s); }
		catch (NumberFormatException e) { return 0; }
	}

	private void setupRoom(SimpleOccupant o, String title) {
		if (o.getRoom() != null) o.getRoom().exitRoom(o);
		newRoom(o,title);
	}
	
	public SimpleOccupant getOccupant(String handle) {
		for (SimpleOccupant occ: occupants) {
			if (occ.getConn().getHandle().equalsIgnoreCase(handle)) return occ;
		}
		return null;
	};

	public abstract void newRoom(SimpleOccupant o, String title);

}
