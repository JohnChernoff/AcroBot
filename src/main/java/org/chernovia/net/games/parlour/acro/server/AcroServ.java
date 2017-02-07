//TODO: 
//history is a bit wonky (spam, topics, etc)
//line breaks are weird for voting results

package org.chernovia.net.games.parlour.acro.server;

import java.util.StringTokenizer;
import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.netgames.db.GameData;
import org.chernovia.lib.netgames.zugserv.*;

public class AcroServ implements ConnListener {
	static final String VERSION = "Version 0.1. Whee.";
	static String CR;
	static String DATAFILE = "res/acrodata.txt";
	static String ACROLOG = "res/acrolog.txt";
	static String MGRFILE = "res/managers.txt";
	static String TOPFILE = "res/topics.txt";
	static String HELPFILE = "res/acrohelp.txt";
	static String ABCDEF = "res/deflet";
	String acroCmdPfx = "!", mgrCmdPfx = "~";
	AcroGame[] games;
	ZugServ serv; 
	
	public AcroServ(String name, String host, String oauth, String channel) {
		initTwitch(name,host,oauth,channel);
	}
	
	public void initTwitch(String name, String host, String oauth, String channel) {
		serv = new TwitchServ(name,"irc.twitch.tv",oauth,"#zugaddict",this);
		CR = "\n";
		startGames(1);
	}
	
	//TODO: implement this somehow
	public void initLocal(int port) {
		//CR = NetServ.newline[NetServ.LOCAL];
		//serv = new Loc_Serv(port,this);
		startGames(12);
	}
	
	//TODO: games on different IRC channels
	public void startGames(int n) {
		games = new AcroGame[n];
		for (int i=0;i<games.length;i++) {
			games[i] = new AcroGame(serv,i);
			games[i].start();
		}
	}

	public static void main(String[] args) {
		//AcroServ S = new AcroServ(args.length > 0 ? Integer.parseInt(args[0]) : 5678);
		AcroServ S = new AcroServ(args[0],args[1],args[2],args[3]);
		AcroBase.CR = CR;
		AcroBase.DATAFILE = DATAFILE;
		GameData.initData(AcroBase.initFields());
		AcroBase.editStats( //just a test
		new GameData(AcroBase.newPlayer("Zippy")));
		S.serv.startSrv();
	}
	
	//TODO: this only returns one game, but conn could be in more than one channel
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
			if (msg.equals("") || msg.equals(mgrCmdPfx) || msg.equals(acroCmdPfx)) { 
				conn.tell(ZugServ.MSG_SERV,"Eh?"); 
			}
			else if (msg.equals("?")) conn.tell(ZugServ.MSG_SERV,showCmds());
			else if (msg.startsWith(mgrCmdPfx)) {
				mgrCmd(G,conn,msg.substring(1));
			}
			else if (msg.startsWith(acroCmdPfx)) {
				acroCmd(G,conn,msg.substring(1));
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
			if (G.TOPICS && p != null && G.lastWin.equals(p)) {
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
			conn.tell(ZugServ.MSG_SERV,"No game!"); return;
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
			else if (s.equalsIgnoreCase("SHOUTING")) {
				G.SHOUTING = !G.SHOUTING;
				serv.tch(G.getChan(),ZugServ.MSG_SERV,"Shouting: " + G.SHOUTING);
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
		case 1:
			String a = tokens.nextToken();
			//int i = Integer.parseInt(a);
			if (s.equalsIgnoreCase("LOADLET")) {
				G.newLetters(a);
			}
			else if (serv.getType() == ZugServ.ServType.TYPE_TWITCH && s.equalsIgnoreCase("CR")) { 
				((TwitchServ)serv).setTwitchCR(" " + a + " ");
			}
			//TODO: implement these!
			/*else if (s.equalsIgnoreCase("CHANNEL")) {
				serv.tch(G.getChan(),"New Channel: " + i,false);
				G.setChan(i);
			}
			else if (s.equalsIgnoreCase("ACROTIME")) {
				G.acrotime = i;
				serv.tch(G.getChan(),"Acro Time: " + i,false);
			}
			else if (s.equalsIgnoreCase("VOTETIME")) {
				G.votetime = i;
				serv.tch(G.getChan(),"Vote Time: " + i,false);
			}
			else if (s.equalsIgnoreCase("WAITTIME")) {
				G.waittime = i;
				serv.tch(G.getChan(),"Wait Time: " + i,false);
			} */
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
				conn.tell(ZugServ.MSG_SERV,AcroBase.listFile(HELPFILE));
				//conn.tell("Rules: each round a randomly generated acronym " + 
				//"is created.  First, enter your own expansion by whispering " +
				//"'/w ZugNet (your acronym)'.  Then, all acronyms are voted upon, and " +
				//"you can vote for one like so: '/w ZugNet (acro number).  GLHF!");
			}
			else if (s.equalsIgnoreCase("START"))  {
				if (G == null) {
					conn.tell(ZugServ.MSG_SERV,"No game!");
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
				if (G == null) conn.tell(ZugServ.MSG_SERV,"No game!");
				else {
					//if (conn.isGUI()) G.dumpAll(conn); 
					conn.tell("vardump",G.listVars());
				}
			}
			else if (s.equalsIgnoreCase("LETTERS")) {
				if (G == null) conn.tell(ZugServ.MSG_SERV,"No game!");
				else conn.tell(ZugServ.MSG_SERV,G.showLetters());
			}
			else if (s.equalsIgnoreCase("ACRO")) {
				if (G == null) conn.tell(ZugServ.MSG_SERV,"No game!");
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

	public void loggedIn(Connection conn) {
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			//((Loc_Conn)conn).setPrompt(">");
			conn.tell(ZugServ.MSG_SERV,"Welcome, " + conn.getHandle() + "!");
			conn.tell(ZugServ.MSG_SERV,"Commands: who, shout (msg), ch (channel)");
		}
	};
	public void disconnected(Connection conn) {};
}
