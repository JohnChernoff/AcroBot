/* TODO: 
(re)implement acro histories
line breaks are sometimes weird for voting results
static variables everywhere, ugh.  Perhaps should refactor things and pass AcroServ to games (instead of serv)
get rid of Acrobase and use mySQL or whatevs, sheesh
*/

package org.chernovia.net.games.parlour.acro.server;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.net.zugserv.ConnListener;
import org.chernovia.lib.net.zugserv.Connection;
import org.chernovia.lib.net.zugserv.DiscordServ;
import org.chernovia.lib.net.zugserv.TwitchServ;
import org.chernovia.lib.net.zugserv.WebSockServ;
import org.chernovia.lib.net.zugserv.ZugServ;
import org.chernovia.lib.netgames.db.GameBase;
import org.chernovia.lib.netgames.db.GameData;
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
	static String newLine;
	static String acroLog = "res/acrolog.txt";
	static String dataFile = "res/acrodata.txt";
	static String mgrFile = "res/managers.txt";
	static String topFile = "res/topics.txt";
	static String twitchHelp = "res/twithelp.txt";
	static String sockHelp = "res/sockhelp.txt";
	static String letterFile = "res/deflet";
	ArrayList<AcroChan> channelMap; 
	String acroCmdPfx = "!", mgrCmdPfx = "~", chanTellPfx = "$";

	AcroGame[] games;
	ZugServ serv; 
	
	public AcroServ(String name, String host, String oauth, String channel) {
		serv = new TwitchServ(name,"irc.twitch.tv",oauth,"#zugaddict",this);  
		newLine = "\n"; initChannels();
	}
	
	public AcroServ(int port) {
		serv = new WebSockServ(port,this);  
		newLine = "\r"; initChannels();
	}
	
	public AcroServ(String token) {
		serv = new DiscordServ(token,this);
		newLine = "\n"; 
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
		else if (args.length == 1) bot = new AcroServ(args[0]); //new AcroServ(Integer.parseInt(args[0])); 
		else { log("Error: Invalid number of arguments"); System.exit(-1); }
		AcroBase.CR = newLine;
		AcroBase.DATAFILE = dataFile;
		GameData.initData(AcroBase.initFields());
		AcroBase.editStats(new GameData(AcroBase.newPlayer("Zippy"))); //just a test
		bot.serv.startSrv();
		switch (bot.serv.getType()) {
			case TYPE_TWITCH: bot.startGames(1); break;
			case TYPE_WEBSOCK: bot.startGames(bot.channelMap.size()); break;
			case TYPE_DISCORD: bot.startGames(bot.serv.getMaxChannels()); break;
			default: log("Error: unknown server type");
		}
	}
	
	//NOTE: this only returns one game, but conn could be in more than one channel
	public AcroGame getGameByConn(Connection conn) {
		for (int i=0;i<games.length;i++) {
			if (conn.getChannels().contains(games[i].getChan())) return games[i];
		}
		return null;
	}
	
	//only for direct tells/whispers
	public void newMsg(Connection conn, int chan, String msg) {
		//System.out.println("New Message: " + conn + ": " + msg); // + ", idle: " + conn.idleTime());
		if (conn.isFlooding(4, 1000)) return;
		AcroGame game = (chan != ZugServ.NO_CHAN) ? games[chan] : getGameByConn(conn);
		try {
			if (msg.equals("") || msg.equals(mgrCmdPfx) || msg.equals(acroCmdPfx) || msg.equals(chanTellPfx)) { 
				conn.tell(ZugServ.MSG_SERV,"Eh?"); 
			}
			else if (msg.equals("?")) conn.tell(ZugServ.MSG_SERV,showCmds());
			else if (msg.startsWith(mgrCmdPfx)) {
				mgrCmd(game,conn,msg.substring(1));
			}
			else if (msg.startsWith(acroCmdPfx)) {
				acroCmd(game,conn,chan,msg.substring(1));
			}
			else if (msg.startsWith(chanTellPfx)) {
				for (Integer c : conn.getChannels()) games[c].tch(conn.getHandle() + " says: " + msg.substring(1));
			}
			else {
				if (game == null) {
					conn.tell(ZugServ.MSG_SERV,"Please enter a game");
				}
				else if (game.getMode() == AcroGame.MOD_PAUSE) {
					conn.tell(ZugServ.MSG_SERV,"Paused right now.");
				}
				else gameTell(game,chan,conn,msg);
			}
		}
		catch (Exception augh) {
			serv.broadcast(ZugServ.MSG_SERV,"Augh: " + augh.getMessage());
			augh.printStackTrace();
		}
	}

	private void gameTell(AcroGame game, int chan, Connection conn, String msg) {
		switch(game.getMode()) {
		case AcroGame.MOD_ACRO:
			if (!game.isLegal(msg.toUpperCase())) conn.tell(ZugServ.MSG_SERV,"Illegal acro: " + msg);
			else game.newAcro(conn,msg);
			break;
		case AcroGame.MOD_VOTE: 
			int i = MiscUtil.strToInt(msg);
			if (i > 0 && game.getNumAcros() >= i) {
				game.newVote(conn,i-1);
			}
			else conn.tell(ZugServ.MSG_SERV,"Bad vote.");
			break;
		case AcroGame.MOD_WAIT:
			AcroPlayer p = game.getPlayer(conn.getHandle());
			if (game.usingTopics && p != null && game.lastWin != null && game.lastWin.equals(p)) {
				game.newTopic(p,msg);
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

	private void mgrCmd(AcroGame game, Connection conn, String cmd) {
		String handle = conn.getHandle();
		if (AcroBase.searchFile(handle, mgrFile) < 0 &&
		(game == null || conn != game.getManager())) {
			conn.tell(ZugServ.MSG_SERV,"You're not a manager.");
			return;
		}
		//general manager commands
		else if (cmd.equalsIgnoreCase("LETFILES")) {
			conn.tell(ZugServ.MSG_SERV,AcroLetter.listFiles()); return;
		}
		//else if (cmd.equalsIgnoreCase("OFF")) {	System.exit(-1); }
		if (game == null) {
			conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG); return;
		}
		StringTokenizer tokens = new StringTokenizer(cmd);
		String s = tokens.nextToken();
		switch (tokens.countTokens())  {
		case 0:
			if (s.equalsIgnoreCase("RESET")) {
				if (game.getMode() != AcroGame.MOD_PAUSE) {
					game.setMode(AcroGame.MOD_RESET);
					game.interrupt();
				}
				else conn.tell(ZugServ.MSG_SERV,"First unpause me.");
			}
			else if (s.equalsIgnoreCase("IDLE")) {
				if (game.getMode() != AcroGame.MOD_PAUSE) {
					game.setMode(AcroGame.MOD_IDLE);
					game.interrupt();
				}
				else conn.tell(ZugServ.MSG_SERV,"First unpause me.");
			}
			else if (s.equalsIgnoreCase("PAUSE")) {
				boolean wasIdling =
					(game.getMode() == AcroGame.MOD_IDLE);
				if (game.getMode() != AcroGame.MOD_PAUSE)
					game.setMode(AcroGame.MOD_PAUSE);
				if (!wasIdling) game.interrupt();
				else serv.tch(game.getChan(),ZugServ.MSG_SERV,"Paused.");
			}
			else if (s.equalsIgnoreCase("NEXT")) {
				if (game.getMode() > AcroGame.MOD_IDLE) {
					game.interrupt();
				}
				else conn.tell(ZugServ.MSG_SERV,"Invalid Game State.");
			}
			else if (s.equalsIgnoreCase("FLATTIME")) {
				game.flatTime = !game.flatTime;
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Flat time: " + game.flatTime);
			}
			else if (s.equalsIgnoreCase("REVEAL")) {
				game.reveal = !game.reveal;
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Reveal: " + game.reveal);
			}
			else if (s.equalsIgnoreCase("TOPICS")) {
				game.usingTopics = !game.usingTopics;
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Topics: " + game.usingTopics);
			}
			else if (s.equalsIgnoreCase("TIEBONUS")) {
				game.tieBonus = !game.tieBonus;
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"TieBonus: " + game.tieBonus);
			}
			else if (s.equalsIgnoreCase("ADULT")) {
				game.adultTopics = !game.adultTopics;
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Adult: " + game.adultTopics);
			}
			else if (s.equalsIgnoreCase("DUMP")) {
				conn.tell(ZugServ.MSG_SERV,game.toString());
			}
			else conn.tell(ZugServ.MSG_SERV,"Oops: no such command.");
			break;
		case 1: //TODO: some bounds checking would be nice on these  
			String a = tokens.nextToken(); int i = Integer.parseInt(a);
			if (s.equalsIgnoreCase("LOADLET")) {
				game.newLetters(a);
			}
			else if (serv.getType() == ZugServ.ServType.TYPE_TWITCH && s.equalsIgnoreCase("CR")) { 
				((TwitchServ)serv).setTwitchCR(" " + a + " ");
			}
			else if (s.equalsIgnoreCase("ACROTIME")) { 
				game.setAcrotime(i); 
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Acro Time: " + i);
			}
			else if (s.equalsIgnoreCase("VOTETIME")) {
				game.setVotetime(i);
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Vote Time: " + i);
			}
			else if (s.equalsIgnoreCase("WAITTIME")) {
				game.setWaittime(i);
				serv.tch(game.getChan(),ZugServ.MSG_SERV,"Wait Time: " + i);
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

	private void acroCmd(AcroGame game, Connection conn, int chan, String msg) {
		System.out.println("AcroCMD: " + msg + ", " + game);
		//conn.tell(ZugServ.MSG_SERV,"Your command: " + msg);
		String handle = conn.getHandle();
		StringTokenizer tokens = new StringTokenizer(msg);
		String s = tokens.nextToken();
		switch (tokens.countTokens())  {
		case 0:
			if (s.equalsIgnoreCase("HELP")) {
				switch(serv.getType()) {
					case TYPE_TWITCH: conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(twitchHelp)); break;
					case TYPE_WEBSOCK: conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(sockHelp)); break;
					default:
				}
			}
			else if (s.equalsIgnoreCase("START"))  {
				if (game == null) {
					conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				}
				else if (game.getMode() == AcroGame.MOD_IDLE) {
					game.setManager(conn);
					game.setMode(AcroGame.MOD_ACRO);
					game.interrupt();
				}
				else if (game.getMode() >= AcroGame.MOD_NEW) {
					conn.tell(ZugServ.MSG_SERV,"Already playing!");
				}
				else conn.tell(ZugServ.MSG_SERV,"Current mode: " + game.getMode());
			}
			//else if (s.equalsIgnoreCase("WHO")) { conn.tell(serv.who(),true,false);	}
			else if (s.equalsIgnoreCase("VERSION")) {
				conn.tell(ZugServ.MSG_SERV,VERSION);
			}
			else if (s.equalsIgnoreCase("VARS") || s.equalsIgnoreCase("INFO")) {
				if (game == null) conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				else {
					//if (conn.isGUI()) G.dumpAll(conn); 
					conn.tell("vardump",game.listVars());
				}
			}
			else if (s.equalsIgnoreCase("LETTERS")) {
				if (game == null) conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				else conn.tell(ZugServ.MSG_SERV,game.showLetters());
			}
			else if (s.equalsIgnoreCase("ACRO")) {
				if (game == null) conn.tell(ZugServ.MSG_SERV,NOCHAN_MSG);
				else conn.tell(ZugServ.MSG_SERV,"Current Acro: " + game.getAcro());
			}
			else if (s.equalsIgnoreCase("FINGER")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.statLine(
				AcroBase.getStats(handle,null)));
			}
			else if (s.equalsIgnoreCase("TOPTEN")) {
				conn.tell(ZugServ.MSG_SERV,GameBase.topTen("wins"));
			}
			else if (s.equalsIgnoreCase("MANAGERS")) {
				conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(mgrFile));
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
		return "Commands: " + newLine +
		mgrCmdPfx + "off" + newLine +
		mgrCmdPfx + "idle" + newLine +
		mgrCmdPfx + "reset" + newLine +
		mgrCmdPfx + "pause" + newLine +
		mgrCmdPfx + "shouting" + newLine +
		mgrCmdPfx + "flattime" + newLine +
		mgrCmdPfx + "reval" + newLine +
		mgrCmdPfx + "topics" + newLine +
		mgrCmdPfx + "tiebonus" + newLine +
		mgrCmdPfx + "letfiles" + newLine +
		mgrCmdPfx + "loadlet" + newLine +
		mgrCmdPfx + "channel" + newLine +
		mgrCmdPfx + "acrotime" + newLine +
		mgrCmdPfx + "votetime" + newLine +
		mgrCmdPfx + "waittime" + newLine +
		mgrCmdPfx + "dump" + newLine +
		acroCmdPfx + "help" + newLine +
		acroCmdPfx + "start" + newLine +
		acroCmdPfx + "ver" + newLine +
		acroCmdPfx + "vars" + newLine +
		acroCmdPfx + "letters" + newLine +
		acroCmdPfx + "acro" + newLine +
		acroCmdPfx + "finger" + newLine +
		acroCmdPfx + "topten" + newLine +
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
	
	public void joinChan(Connection conn, int chan) {
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			games[chan].tch("playlist",games[chan].dumpPlayers().toString()); 
			changeChannel(conn,"joinchan",chan);
		}
	}


	public void partChan(Connection conn, int chan) {
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			games[chan].removePlayer(games[chan].getPlayer(conn.getHandle())); //leave channel = leave game
			games[chan].tch("playlist",games[chan].dumpPlayers().toString()); 
			changeChannel(conn,"partchan",chan);
		}
	};
	
	public void changeChannel(Connection conn, String change, int chan) {
		for (Connection c : serv.getAllConnections(true)) {
			if (c.getChannels().contains(chan)) {
				JsonObject obj = new JsonObject();
				obj.addProperty("name",conn.getHandle());
				obj.addProperty("chan",chan);
				c.tell(change,obj.toString());
			}
		}
	}

	@Override
	public void disconnected(Connection conn) {
		for (Integer chan : conn.getChannels())	partChan(conn,chan);
	}
}
