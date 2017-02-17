//TODO: 
//(re)implement acro histories
//line breaks are sometimes weird for voting results
//static variables everywhere, ugh.  Perhaps should refactor things and pass AcroServ to games (instead of serv)
//get rid of Acrobase and use mySQL or whatevs, sheesh
package org.chernovia.net.games.parlour.acro.server;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.netgames.db.GameData;
import org.chernovia.lib.netgames.zugserv.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AcroServ implements ConnListener {
	
	public class AcroChan {
		String name;
		int index;
		public AcroChan(String n, int i) { name = n; index = i; }
	}
	static final String VERSION = "Version 0.1a. Whee.", NOCHAN_MSG = "Error: not in a channel. Please join one.";
	static final String[] defaultChannels = { "General","Clean","Adult","Chess","Twitch"};
	static String CR;
	static String DATAFILE = "res/acrodata.txt";
	static String ACROLOG = "res/acrolog.txt";
	static String MGRFILE = "res/managers.txt";
	static String TOPFILE = "res/topics.txt";
	static String TWITHELP = "res/twithelp.txt";
	static String SOCKHELP = "res/sockhelp.txt";
	static String ABCDEF = "res/deflet";
	ArrayList<AcroChan> channelMap; 
	String acroCmdPfx = "!", mgrCmdPfx = "~", chanTellPfx = "$";

	AcroGame[] games;
	ZugServ serv; 
	
	public AcroServ(String name, String host, String oauth, String channel) {
		serv = new TwitchServ(name,"irc.twitch.tv",oauth,"#zugaddict",this);  
		CR = "\n"; initChannels();
	}
	
	public AcroServ(int port) {
		serv = new WebSockServ(port,this);  
		CR = "\r"; initChannels();
	}
	
	public void initChannels() {
		channelMap = new ArrayList<AcroChan>();
		for (int i=0;i<defaultChannels.length;i++) channelMap.add(new AcroChan(defaultChannels[i],i));
	}
	
	//starts the game threads, but doesn't start the gameplay (which is begun with the "!start" user cmd) 
	public void startGames(int n) {
		games = new AcroGame[n];
		for (int i=0;i<games.length;i++) {
			games[i] = new AcroGame(serv,i);
			games[i].start();
		}
	}
	
	public static void log(String msg) { System.out.println(msg); }

	public static void main(String[] args) {
		AcroServ bot = null;
		if (args.length == 4) bot = new AcroServ(args[0],args[1],args[2],args[3]); 
		else if (args.length == 1) bot = new AcroServ(Integer.parseInt(args[0])); 
		else { log("Error: Invalid number of arguments"); System.exit(-1); }
		AcroBase.CR = CR;
		AcroBase.DATAFILE = DATAFILE;
		GameData.initData(AcroBase.initFields());
		AcroBase.editStats(new GameData(AcroBase.newPlayer("Zippy"))); //just a test
		bot.serv.startSrv();
		switch (bot.serv.getType()) {
		case TYPE_TWITCH: bot.startGames(1); break;
		case TYPE_WEBSOCK: bot.startGames(bot.channelMap.size()); break;
		default: log("Error: unknown server type");
	}
	}
	
	//NOTE: this only returns one game, but conn could be in more than one channel
	public AcroGame getGameByConn(Connection conn) {
		for (int i=0;i<games.length;i++) {
			if (conn.getChannels().contains(new Integer(games[i].getChan()))) return games[i];
		}
		return null;
	}
	
	//only for direct tells/whispers
	public void newMsg(Connection conn, String msg) {
		//System.out.println("New Message: " + conn + ": " + msg + ", idle: " + conn.idleTime());
		if (conn.isFlooding(4, 1000)) return;
		AcroGame G = getGameByConn(conn);
		try {
			if (msg.equals("") || msg.equals(mgrCmdPfx) || msg.equals(acroCmdPfx) || msg.equals(chanTellPfx)) { 
				conn.tell(ZugServ.MSG_SERV,"Eh?"); 
			}
			else if (msg.equals("?")) conn.tell(ZugServ.MSG_SERV,showCmds());
			else if (msg.startsWith(mgrCmdPfx)) {
				mgrCmd(G,conn,msg.substring(1));
			}
			else if (msg.startsWith(acroCmdPfx)) {
				acroCmd(G,conn,msg.substring(1));
			}
			else if (msg.startsWith(chanTellPfx)) {
				for (Integer c : conn.getChannels()) games[c].tch(conn.getHandle() + " says: " + msg.substring(1));
			}
			else {
				if (G == null) {
					conn.tell(ZugServ.MSG_SERV,"Please enter a game");
				}
				else if (G.getMode() == AcroGame.MOD_PAUSE) {
					conn.tell(ZugServ.MSG_SERV,"Paused right now.");
				}
				else gameTell(G,conn,msg);
			}
		}
		catch (Exception augh) {
			serv.broadcast(ZugServ.MSG_SERV,"Augh: " + augh.getMessage());
			augh.printStackTrace();
		}
	}

	private void gameTell(AcroGame G, Connection conn, String msg) {
		switch(G.getMode()) {
		case AcroGame.MOD_ACRO:
			if (!G.isLegal(msg.toUpperCase()))
				conn.tell(ZugServ.MSG_SERV,"Illegal acro: " + msg);
			else G.newAcro(conn,msg);
			break;
		case AcroGame.MOD_VOTE: 
			int i = MiscUtil.strToInt(msg);
			if (i > 0 && G.getNumAcros() >= i) {
				G.newVote(conn,i-1);
			}
			else conn.tell(ZugServ.MSG_SERV,"Bad vote.");
			break;
		case AcroGame.MOD_WAIT:
			AcroPlayer p = G.getPlayer(conn.getHandle());
			if (G.TOPICS && p != null && G.lastWin != null && G.lastWin.equals(p)) {
				G.newTopic(p,msg);
			}
			else conn.tell(ZugServ.MSG_SERV,"Next round coming...");
			break;
		case AcroGame.MOD_NEW:
			conn.tell(ZugServ.MSG_SERV,"New game coming...");
			break;
		default:
			conn.tell(ZugServ.MSG_SERV,"Idle. Tell me " + acroCmdPfx + "start.");
		}
	}

	private void mgrCmd(AcroGame G, Connection conn, String cmd) {
		String handle = conn.getHandle();
		if (AcroBase.searchFile(handle, MGRFILE) < 0 &&
		(G == null || conn != G.getManager())) {
			conn.tell(ZugServ.MSG_SERV,"You're not a manager.");
			return;
		}
		//general manager commands
		if (cmd.toUpperCase().startsWith("SPOOF") &&
		cmd.length() > 7) {
			String m = cmd.substring(6);
			serv.send(m);
			conn.tell(ZugServ.MSG_SERV,"Spoofed: " + m);
			return;
		}
		else if (cmd.equalsIgnoreCase("LETFILES")) {
			conn.tell(ZugServ.MSG_SERV,AcroLetter.listFiles()); return;
		}
		//else if (cmd.equalsIgnoreCase("OFF")) {	System.exit(-1); }
		if (G == null) {
			conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG); return;
		}
		StringTokenizer tokens = new StringTokenizer(cmd);
		String s = tokens.nextToken();
		switch (tokens.countTokens())  {
		case 0:
			if (s.equalsIgnoreCase("RESET")) {
				if (G.getMode() != AcroGame.MOD_PAUSE) {
					G.setMode(AcroGame.MOD_RESET);
					G.interrupt();
				}
				else conn.tell(ZugServ.MSG_SERV,"First unpause me.");
			}
			else if (s.equalsIgnoreCase("IDLE")) {
				if (G.getMode() != AcroGame.MOD_PAUSE) {
					G.setMode(AcroGame.MOD_IDLE);
					G.interrupt();
				}
				else conn.tell(ZugServ.MSG_SERV,"First unpause me.");
			}
			else if (s.equalsIgnoreCase("PAUSE")) {
				boolean wasIdling =
					(G.getMode() == AcroGame.MOD_IDLE);
				if (G.getMode() != AcroGame.MOD_PAUSE)
					G.setMode(AcroGame.MOD_PAUSE);
				if (!wasIdling) G.interrupt();
				else serv.tch(G.getChan(),ZugServ.MSG_SERV,"Paused.");
			}
			else if (s.equalsIgnoreCase("NEXT")) {
				if (G.getMode() > AcroGame.MOD_IDLE) {
					G.interrupt();
				}
				else conn.tell(ZugServ.MSG_SERV,"Invalid Game State.");
			}
			else if (s.equalsIgnoreCase("FLATTIME")) {
				G.FLATTIME = !G.FLATTIME;
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Flat time: " + G.FLATTIME);
			}
			else if (s.equalsIgnoreCase("REVEAL")) {
				G.REVEAL = !G.REVEAL;
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Reveal: " + G.REVEAL);
			}
			else if (s.equalsIgnoreCase("TOPICS")) {
				G.TOPICS = !G.TOPICS;
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Topics: " + G.TOPICS);
			}
			else if (s.equalsIgnoreCase("TIEBONUS")) {
				G.TIEBONUS = !G.TIEBONUS;
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"TieBonus: " + G.TIEBONUS);
			}
			else if (s.equalsIgnoreCase("ADULT")) {
				G.ADULT = !G.ADULT;
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Adult: " + G.ADULT);
			}
			else if (s.equalsIgnoreCase("DUMP")) {
				conn.tell(ZugServ.MSG_SERV,G.toString());
			}
			else conn.tell(ZugServ.MSG_SERV,"Oops: no such command.");
			break;
		case 1: //TODO: some bounds checking would be nice on these  
			String a = tokens.nextToken(); int i = Integer.parseInt(a);
			if (s.equalsIgnoreCase("LOADLET")) {
				G.newLetters(a);
			}
			else if (serv.getType() == ZugServ.ServType.TYPE_TWITCH && s.equalsIgnoreCase("CR")) { 
				((TwitchServ)serv).setTwitchCR(" " + a + " ");
			}
			else if (s.equalsIgnoreCase("ACROTIME")) { 
				G.setAcrotime(i); 
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Acro Time: " + i);
			}
			else if (s.equalsIgnoreCase("VOTETIME")) {
				G.setVotetime(i);
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Vote Time: " + i);
			}
			else if (s.equalsIgnoreCase("WAITTIME")) {
				G.setWaittime(i);
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Wait Time: " + i);
			} 
			//else if (s.equalsIgnoreCase("CHANNEL")) {
			//	serv.tch(G.getChan(),"New Channel: " + i,false);
			//	G.setChan(i);
			//}
			else conn.tell(ZugServ.MSG_SERV,"D'oh: No such command.");
			break;
		default: conn.tell(ZugServ.MSG_SERV,"Too many tokens!");
		}
	}

	private void acroCmd(AcroGame G, Connection conn, String msg) {
		String handle = conn.getHandle();
		StringTokenizer tokens = new StringTokenizer(msg);
		String s = tokens.nextToken();
		switch (tokens.countTokens())  {
		case 0:
			if (s.equalsIgnoreCase("HELP")) {
				switch(serv.getType()) {
					case TYPE_TWITCH: conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(TWITHELP)); break;
					case TYPE_WEBSOCK: conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(SOCKHELP)); break;
					default:
				}
			}
			else if (s.equalsIgnoreCase("START"))  {
				if (G == null) {
					conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				}
				else if (G.getMode() == AcroGame.MOD_IDLE) {
					G.setManager(conn);
					G.setMode(AcroGame.MOD_ACRO);
					G.interrupt();
				}
				else if (G.getMode() >= AcroGame.MOD_NEW) {
					conn.tell(ZugServ.MSG_SERV,"Already playing!");
				}
				else conn.tell(ZugServ.MSG_SERV,"Current mode: " + G.getMode());
			}
			//else if (s.equalsIgnoreCase("WHO")) { conn.tell(serv.who(),true,false);	}
			else if (s.equalsIgnoreCase("VERSION")) {
				conn.tell(ZugServ.MSG_SERV,VERSION);
			}
			else if (s.equalsIgnoreCase("VARS") || s.equalsIgnoreCase("INFO")) {
				if (G == null) conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				else {
					//if (conn.isGUI()) G.dumpAll(conn); 
					conn.tell("vardump",G.listVars());
				}
			}
			else if (s.equalsIgnoreCase("LETTERS")) {
				if (G == null) conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				else conn.tell(ZugServ.MSG_SERV,G.showLetters());
			}
			else if (s.equalsIgnoreCase("ACRO")) {
				if (G == null) conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				else conn.tell(ZugServ.MSG_SERV,"Current Acro: " + G.getAcro());
			}
			else if (s.equalsIgnoreCase("FINGER")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.statLine(
				AcroBase.getStats(handle,null)));
			}
			else if (s.equalsIgnoreCase("TOPTEN")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.topTen("wins"));
			}
			else if (s.equalsIgnoreCase("MANAGERS")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(MGRFILE));
			}
			else conn.tell(ZugServ.MSG_SERV,"Bad command. Erp.");
			break;
		case 1:
			if (s.equalsIgnoreCase("FINGER")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.statLine(
				AcroBase.getStats(tokens.nextToken(),null)));
			}
			else if (s.equalsIgnoreCase("TOPTEN")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.topTen(
				tokens.nextToken()));
			}
			else conn.tell(ZugServ.MSG_SERV,"Bad command. Erp.");
			break;
		default: conn.tell(ZugServ.MSG_SERV,"Too many tokens!");
		}
	}
	
	public String showCmds() {
		return "Commands: " + CR +
		mgrCmdPfx + "off" + CR +
		mgrCmdPfx + "idle" + CR +
		mgrCmdPfx + "reset" + CR +
		mgrCmdPfx + "pause" + CR +
		mgrCmdPfx + "shouting" + CR +
		mgrCmdPfx + "flattime" + CR +
		mgrCmdPfx + "reval" + CR +
		mgrCmdPfx + "topics" + CR +
		mgrCmdPfx + "tiebonus" + CR +
		mgrCmdPfx + "letfiles" + CR +
		mgrCmdPfx + "loadlet" + CR +
		mgrCmdPfx + "channel" + CR +
		mgrCmdPfx + "acrotime" + CR +
		mgrCmdPfx + "votetime" + CR +
		mgrCmdPfx + "waittime" + CR +
		mgrCmdPfx + "dump" + CR +
		acroCmdPfx + "help" + CR +
		acroCmdPfx + "start" + CR +
		acroCmdPfx + "ver" + CR +
		acroCmdPfx + "vars" + CR +
		acroCmdPfx + "letters" + CR +
		acroCmdPfx + "acro" + CR +
		acroCmdPfx + "finger" + CR +
		acroCmdPfx + "topten" + CR +
		"";
	}
	
	public JsonArray getChannels() {
		JsonArray chanlist = new JsonArray();
		for (AcroChan channel : channelMap) {
			JsonObject obj = new JsonObject();
			obj.addProperty("index", channel.index);
			obj.addProperty("name", channel.name);
			chanlist.add(obj);
		}
		return chanlist;
	}

	public void loggedIn(Connection conn) {
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			conn.tell(ZugServ.MSG_SERV,"Welcome, " + conn.getHandle() + "!");
			//conn.tell(ZugServ.MSG_SERV,"Commands: who, shout (msg), ch (channel)");
			conn.tell("chanlist", getChannels().toString());
		}
	};
	
	public void disconnected(Connection conn) {
		for (Integer chan : conn.getChannels())	partChan(conn,chan);
	}

	@Override
	public void joinChan(Connection conn, int chan) {
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			games[chan].tch("playlist",games[chan].dumpPlayers().toString()); 
			changeChannel(conn,"joinchan",chan);
		}
	}

	@Override
	public void partChan(Connection conn, int chan) {
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			games[chan].removePlayer(games[chan].getPlayer(conn.getHandle())); //leave channel = leave game
			games[chan].tch("playlist",games[chan].dumpPlayers().toString()); 
			changeChannel(conn,"partchan",chan);
		}
	};
	
	public void changeChannel(Connection conn, String change, int chan) {
		for (Connection c : serv.getAllConnections()) {
			if (c.getChannels().contains(chan)) {
				JsonObject obj = new JsonObject();
				obj.addProperty("name",conn.getHandle());
				obj.addProperty("chan",chan);
				c.tell(change,obj.toString());
			}
		}
	}
}
