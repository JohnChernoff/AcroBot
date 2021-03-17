package org.chernovia.net.games.parlour.acro.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.chernovia.lib.misc.IOUtil;
import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.net.zugserv.Connection;
import org.chernovia.lib.net.zugserv.ZugServ;
import org.chernovia.lib.netgames.db.GameBase;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

//Acro!
public class AcroGame extends Thread {
	static final String	NO_TOPIC = "", NO_ACRO = "";
	static final int
	DG_NEW_ACRO = 0,
	DG_ACRO_TIME = 1,
	DG_ACRO_ENTERED = 2,
	DG_SHOW_ACROS = 3,
	DG_VOTE_TIME = 4,
	DG_RESULTS = 5,
	DG_WAIT_TIME = 6,
	DG_PLAYERS = 7,
	DG_INFO = 8,
	DG_VARS = 9,
	DG_NEXT_ACRO = 10;
	
	static final int
	MOD_RESET = -3,MOD_PAUSE = -2,MOD_IDLE = -1,
	MOD_NEW = 0,MOD_ACRO = 1,MOD_VOTE = 2,MOD_WAIT = 3;

	class Acro  {
		String acro; AcroPlayer author; int votes; long time;
		public Acro(String s, AcroPlayer a, long t) {
			acro = s; author = a; votes = 0; time = t;
		}
		public JsonObject toJson() {
			JsonObject a = new JsonObject();
			a.addProperty("acro", acro);
			a.add("author", author.toJson());
			a.addProperty("votes", votes);
			a.addProperty("time", time);
			return a;
		}
	}
	
	private Vector<AcroPlayer> players;
	private Vector<Acro> acrolist;
	private AcroLetter[] letters;
	private String[] topics;
	private String acro,topic; 
	private ZugServ serv;
	private Connection manager;
	private long newtime;
	private int chan, mode,
	acrotime,votetime,waittime,basetime,
	atimevar,vtimevar,maxacro,minacro,
	speedpts,round,winscore,longhand,acrolen,
	maxcol, votecol, maxtopic, maxround, maxplay;
	boolean flatTime,reveal,tieBonus,usingTopics,adultTopics;
	boolean testing = false;
	AcroPlayer lastWin;
	TwitchBox box;
	
	//TODO: add different types for different situations
	public void tell(Connection conn, String msg) { conn.tell("ptell", msg); };
	public void tell(Connection conn, String type, String msg) { conn.tell(type, msg); }
	public void tch(String msg) { tch("ctell", msg); }
	public void tch(String type,String msg) { serv.tch(chan, type, msg); }
	
	public int getNumAcros() { return acrolist.size(); }
	public Vector<AcroPlayer> getPlayers() { return players; }
	
	public JsonObject dumpGame() {
		JsonObject dump = new JsonObject();
		dump.addProperty("manager", manager.getHandle());
		dump.addProperty("channel",chan);
		dump.addProperty("round",round);
		dump.addProperty("topic",topic);
		dump.addProperty("acro",acro);
		dump.addProperty("mode",mode);
		return dump;
	}
	
	public JsonObject dumpVars() {
		JsonObject dump = new JsonObject();
		dump.addProperty("tiebonus", tieBonus);
		dump.addProperty("reveal", reveal);
		dump.addProperty("topics", usingTopics);
		dump.addProperty("flattime", flatTime);
		dump.addProperty("adult", adultTopics);
		dump.addProperty("players", players.size());
		dump.addProperty("maxplay", maxplay);
		dump.addProperty("minacro", minacro);
		dump.addProperty("maxacro", maxacro);
		return dump;
	}
	
	public JsonArray dumpPlayers() {
		ArrayList<AcroPlayer> playlist = new ArrayList<>();
		for (Connection conn : serv.getAllConnections(true)) {
			if (conn.getChannels().contains(chan)) {
				AcroPlayer p = getPlayer(conn.getHandle());
				if (p != null) playlist.add(p);
				else playlist.add(new AcroPlayer(this,conn)); //fake it till ya make it
			}
		}
		Collections.sort(playlist);
		JsonArray playerArray = new JsonArray();
		for (AcroPlayer p : playlist) playerArray.add(p.toJson());
		return playerArray;
	}
	
	private JsonArray dumpAcros() {
		JsonArray acroArr = new JsonArray();
		for (Acro a : acrolist) acroArr.add(a.toJson());
		return acroArr;
	}
	
	//TODO: make some sense of this
	private void updateAll() {
		//tch("gamedump",dumpGame().toString());
		//tch("vardump",dumpVars().toString());
		//tch("playdump",dumpPlayers().toString());
	}
	
	public AcroPlayer getPlayer(String name) {
		for (AcroPlayer p : players) if (p.conn.getHandle().equalsIgnoreCase(name)) return p;
		return null;
	}
	
	public AcroPlayer getLastWinner() {
		return lastWin;
	}
	
	public boolean inChan(Connection conn, int c) {
		return conn.getChannels().contains(c);
	}
	
	public AcroGame(ZugServ srv, int c) {
		manager = null;	serv = srv; chan = c;
		if (serv.getType() == ZugServ.ServType.TYPE_TWITCH) {
			box = new TwitchBox(); box.setVisible(true); 
		}
		else box = null;
	}

	private void initGame() {
		mode = MOD_IDLE; newLetters(AcroServ.letterFile);
		if (testing) { acrotime = 20; votetime = 10; waittime = 5; }
		else { acrotime = 90; votetime = 60; waittime = 30;	}
		atimevar = 6; vtimevar = 6; basetime = 30;
		winscore = 30; speedpts = 2;
		maxacro = 8; minacro = 3;
		maxcol = 60; votecol = 10;
		maxround = 99; maxtopic = 3;
		maxplay = 24;
		flatTime = true; usingTopics = true; 
		reveal = false; tieBonus = false; adultTopics = false;
		tch(AcroServ.VERSION);
	}

	private boolean initNewGame() {
		acro = NO_ACRO; newtime = 0; lastWin = null;
		acrolen = newLength();
		round = 0; topic = NO_TOPIC;
		players = new Vector<>(); 
		topics = new String[maxtopic];
		return true; //TODO: why would/should this return false?
	}

	public void run() {
		try {
			initGame();
			while (initNewGame()) {
				if (mode != MOD_IDLE) mode = MOD_ACRO;
				else idle();
				tch("New Game Starting!"); int deserted = 0;
				while (mode > MOD_NEW) {
					if (box != null)  { //just for the colors
						box.updateHiScores(new StringTokenizer(GameBase.topTen("wins"),AcroServ.newLine));
					}
					acrolist = new Vector<>();
					round++;
					acroRound(); 
					if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) updateAll();
					acrolen = newLength();
					if (acrolist.size() == 0) {
						tch("No acros."); deserted++;
						if (deserted == 3) {
							tch("Bah. noone's here. I sleep.");
							mode = MOD_IDLE;
						}
					}
					else if (!testing && acrolist.size() < 3) {
						deserted = 0;
						tch(showAcros());
						tch("Too few acros to vote on (need at least three).");
					}
					else {
						deserted = 0; 
						voteRound(); 
						if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) updateAll();
						scoreRound(); 
						if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) updateAll();
					}
				}
			}
		}
		catch (Exception augh) {
			tch("Oops: " + augh.getMessage());
			augh.printStackTrace();
		}
	}

	private void idle() {
		try { while (mode != MOD_ACRO) sleep(999999); }
		catch (InterruptedException e) {
			if (mode != MOD_ACRO) idle();
		}
	}

	private String makeAcro(int numlets) {
		int t = 0;
		for (int x=0;x<26;x++) t += letters[x].prob;
		StringBuffer buffer = new StringBuffer(""); int c=0;
		for (int x=0;x<numlets;x++) {
			int r = MiscUtil.randInt(t); int z=0;
			for (c=0;c<26;c++) {
				z += letters[c].prob;
				if (z > r) break;
			}
			buffer.append(letters[c].c.toUpperCase());
		}
		return buffer.toString();
	}

	private void acroRound() {
		if (mode > MOD_IDLE) mode = MOD_ACRO; else return;
		if (!topic.equals(NO_TOPIC)) tch("Topic: " + topic);
		acro = makeAcro(acrolen); if (box != null) box.updateAcro(acro, topic);
		int t = makeAcroTime();
		tch("Round " + round + " Acro: " + acro + AcroServ.newLine + "You have " + t + " seconds.");
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
			if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) {
				JsonObject obj = new JsonObject();
				obj.addProperty("acro",acro);
				obj.addProperty("topic",topic);
				obj.addProperty("round",round);
				obj.addProperty("time", t);
				tch("new_acro",obj.toString());
			}
		}
		newtime = System.currentTimeMillis();
		sleeper((t/2) * 1000);
		tch(t/2 + " seconds remaining.");
		sleeper(((t/2)-(t/6)) * 1000);
		tch(t/6 + " seconds...");
		sleeper((t/6) * 1000);
	}

	private void sleeper(long t) {
		long rt = sleeping(t);
		while (rt > 0) {
			tch("Unpaused. (" +	rt/1000 + " seconds remaining)");
			rt = sleeping(rt);
		}
	}

	private long sleeping(long t) {
		int oldmode = mode;
		if (mode < MOD_NEW) return 0; //skip past stuff
		long s = System.currentTimeMillis();
		try { sleep(t); }
		catch (InterruptedException e) {
			if (mode != MOD_PAUSE) {
				tch("Skipping..."); return 0;
			}
			else {
				tch("Pausing...");
				try { sleep(999999); }
				catch (InterruptedException i) {
					mode = oldmode;
				}
				return t - (System.currentTimeMillis() - s);
			}
		}
		return 0;
	}

	private int makeAcroTime() {
		if (flatTime) return acrotime;
		return basetime + (atimevar*acro.length());
	}

	private int getVoteTime() {
		if (flatTime) return votetime;
		return basetime + (vtimevar*acrolist.size());
	}

	private String showAcros() {
		StringBuffer buffer = new StringBuffer("Round " + round + " Acros: " + AcroServ.newLine);
		Collections.shuffle(acrolist);
		//S.append("____________________________________________" + AcroServ.CR);
		longhand = 0; // for formatting
		int x = 0; 
		for (Acro a: acrolist) {
			buffer.append(++x + ". ");
			buffer.append(a.acro + AcroServ.newLine);
			int l = a.author.getName().length();
			if (l > longhand) longhand = l; //get longest handle
		}
		//S.append("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + AcroServ.CR);
		return buffer.toString();
	}

	private void voteRound() {
		if (mode > MOD_IDLE) mode = MOD_VOTE; else return;
		if (topic != NO_TOPIC) tch("Topic: " + topic);
		tch(showAcros()); 
		if (serv.getType() == ZugServ.ServType.TYPE_WEBSOCK) tch("acrodump",dumpAcros().toString());
		if (box != null) box.updateAcros(acrolist); 
		int t=getVoteTime(); 
		tch("Time to vote!  Enter the number of an acro above. " +
		AcroServ.newLine + "You have " + t + " seconds.");
		sleeper((t-(t/6)) * 1000); tch(t/6 + " seconds...");
		sleeper ((t/6) * 1000);
	}

	private void scoreRound() {
		if (mode > MOD_IDLE) mode = MOD_WAIT; else return;
		if (topic != NO_TOPIC) tch("Topic: " + topic);
		tch(showVote(false));
		tch(showScore());
		if (box != null) box.updateScores(players);
		AcroPlayer winner = winCheck(); 
		if (winner == null) waitRound();
		else  {
			tch(winner.getName() + " wins!" + AcroServ.newLine);
			//serv.broadcast(ZugServ.MSG_SERV,winner.getName() + winshout);
			//tch(summarize());
			//showHistory();
			AcroBase.updateStats(this, winner);
			tch("New game in " + (2*waittime) + " seconds.");
			sleeper(waittime * 2000);
			mode = MOD_NEW;
		}
	}

	private void waitRound() {
		mode = MOD_WAIT; topic = NO_TOPIC; getTopics();
		if (usingTopics &&  lastWin != null) {
			tch(lastWin.getName() + " may choose a topic: " + AcroServ.newLine + showTopics());
			lastWin.conn.tell(ZugServ.MSG_SERV,"Next acro: " + acrolen + " letters");
		}
		tch("Next round in " + waittime + " seconds.");
		sleeper(waittime*1000); lastWin = null;
	}

	private void getTopics() {
		List<String> topicvec = IOUtil.readFile(AcroServ.topFile);
		int lof = topicvec.size();
		int[] toplist = new int[maxtopic];
		for (int x=0;x<maxtopic;x++) {
			boolean match;
			do {
				match = false;
				toplist[x] = MiscUtil.randInt(lof-1); //System.out.println("Topic #" + x + ": " + toplist[x]);
				for (int y=x-1;y>=0;y--)
					if (toplist[x] == toplist[y]) match = true;
			} while (match);
		}
		for (int t=0;t<maxtopic;t++)
			topics[t] = topicvec.get(toplist[t]); //topics[MAXTOPIC-1] = DEFTOPIC;
	}

	private String showTopics() {
		StringBuffer buffer = new StringBuffer();
		for (int t=0;t<maxtopic;t++)
			buffer.append((t+1) + ". " + topics[t] + AcroServ.newLine);
		return buffer.toString();
	}

	protected void newTopic(AcroPlayer p, String msg) {
		String n = p.getName();
		if (msg.length()>6 && msg.startsWith("topic ")) {
			topic = msg.substring(6);
			tch(n + " selects " + topic + "...");
		}
		else {
			int t = MiscUtil.strToInt(msg)-1;
			if (t < 0 || t >= maxtopic) {
				p.conn.tell(ZugServ.MSG_SERV,"Bad Topic.");
			}
			else {
				topic = topics[t];
				tch(n + " selects " + topic + "...");
			}
		}
	}

	private AcroPlayer winCheck() {
		AcroPlayer leader = null;
		for (AcroPlayer p: players) if (leader == null || p.score > leader.score) leader = p;
		if (leader.score >= winscore) return leader; else return null; //TODO: ties!
	}

	private String showVote(boolean fancyformat) {
		String CR = AcroServ.newLine;
		StringBuffer S = new StringBuffer(CR + "Round " + round + " Voting Results: " + CR + CR);
		int acrocol = maxcol - (votecol + longhand);
		if (fancyformat) for (Acro a: acrolist) {
			int h = a.author.getName().length();
			String acroline = 
			a.author.getName() + MiscUtil.txtPad(longhand-h) + a.votes + " votes! (" +	a.time/(float)1000 + ")" + CR;
			if (a.acro.length() < acrocol) {
				S.append("  " + a.acro + MiscUtil.txtPad(acrocol - a.acro.length()) + acroline);
			}
			else {
				int i = MiscUtil.lastSpc(a.acro.substring(0,acrocol));
				S.append("  " + 
				a.acro.substring(0,i) + MiscUtil.txtPad(acrocol - i) + acroline + " " + a.acro.substring(i) + CR);
			}
		}
		else for (Acro a: acrolist) {
			S.append(a.acro + " (" + a.author.getName() + ") " + a.votes + " votes! (" + a.time/(float)1000 + ")" +	CR);
		}
		S.append(CR);
		for (Acro a: acrolist) {
			if (a.votes > 0 && a.author.vote >= 0) a.author.score += a.votes;
		}
		AcroPlayer s = getSpeed(); if (s != null) {
			S.append(" " + s.getName() + " -> " + speedpts + " speed bonus points" + CR);
			s.score += speedpts;
		}
		S.append(winners() + CR);
		return S.toString();
	}

	private AcroPlayer getSpeed() {
		AcroPlayer s = null; long t = 9999999;
		for (Acro a: acrolist) {
			if (a.time < t && a.author.score < winscore-4 &&  a.author.vote >= 0 &&	a.votes > 0) {
				s = a.author; t = a.time;
			}
		}
		return s;
	}

	private String winners() {
		String CR = AcroServ.newLine;
		StringBuffer S = new StringBuffer("");
		for (Acro a: acrolist) {
			if (a.author.vote < 0) {
				S.append(" " + a.author.getName() + " did not vote, and thus forfeits this round." + CR);
			}
		}
		int w = 0, bonusPts = acro.length(); 
		long wintime = 999999;
		for (Acro a: acrolist) {
			if (a.author.vote >= 0 && a.votes > w) w = a.votes;
		}
		for (Acro a: acrolist) {
			if (a.votes == w && w > 0 && a.author.vote >= 0) {
				if (tieBonus) {
					a.author.score += bonusPts;
					S.append(" " + a.author.getName());
				}
				if (a.time < wintime) {
					lastWin = a.author;
					wintime = a.time;
				}
			}
		}
		if (w < 1) S.append(" No winners.");
		else if (tieBonus) {
			S.append(" -> " + bonusPts + " bonus points!");
		}
		else if (lastWin != null) { 
			S.append(" " + lastWin.getName() + " -> " + bonusPts + " bonus points!");
			lastWin.score += bonusPts;
		}
		if (usingTopics && lastWin != null)
			S.append(CR + " " + lastWin.getName() + " gets to choose the next topic.");
		return S.toString();
	}

	private String showScore() {
		// record scores
		for (AcroPlayer p : players) {
			Acro acro = findAcro(p); 
			int v = p.vote;
			if (v < 0) p.save(acro,null); else p.save(acro,acrolist.elementAt(v).author);
		}
		// show scores
		StringBuffer S = new StringBuffer(" Round " + round + " Scores: " + AcroServ.newLine);
		for (AcroPlayer p : players) {
			if (p.score > 0) S.append(" " + p.getName() + ": " + p.score + " "); p.vote = -1;
		}
		return S.toString() + AcroServ.newLine;
	}
	
	protected boolean isLegal(String A) { //System.out.println("Received acro: " + A + " (current: " + acro + ")");
		StringTokenizer ST = new StringTokenizer(A); 
		if (acro.length() != ST.countTokens()) {
			System.out.println("Bad length: " + ST.countTokens()); return false;
		}
		int x=0; 
		while (ST.hasMoreTokens()) {
			String S = ST.nextToken();
			char c = S.charAt(0);
			if (c == '"' && S.length()>1) c = S.charAt(1);
			if (acro.charAt(x)!=c) { //System.out.println("Bad match: " + c + " != " + acro.charAt(x)); 
				return false; 
			}
			x++;
		}
		return true;
	}

	protected String makeAcro(String A) {
		StringBuffer SB = new StringBuffer();
		StringTokenizer ST = new StringTokenizer(A);
		while (ST.hasMoreTokens())
			SB.append(ST.nextToken().charAt(0));
		return SB.toString().toUpperCase();
	}

	private Acro findAcro(AcroPlayer p)  { //find acro by player
		for (Acro a : acrolist) if (a.author.equals(p)) return a;
		return null;
	}

	private int newLength() {
		return MiscUtil.randInt(maxacro-minacro) + minacro;
	}

	protected void newLetters(String ABCFILE) {
		letters =
			AcroLetter.loadABC(ABCFILE + AcroLetter.LETTEXT);
		if (letters == null) {
			tch("Can't find Letter File: " + ABCFILE);
			tch("Using default (" + AcroServ.letterFile + ") " +
			"instead.");
			letters = AcroLetter.loadABC(AcroServ.letterFile +
					AcroLetter.LETTEXT);
		}
		else tch("Loaded Letter File: " + ABCFILE);
	}

	public AcroPlayer addNewPlayer(Connection conn) {
		if (players.size() > maxplay)  {
			conn.tell(ZugServ.MSG_SERV,"Game Full!?"); return null;
		}
		else {
			AcroPlayer p = new AcroPlayer(this,conn); players.add(p);
			conn.tell(ZugServ.MSG_SERV,"Welcome!");
			if (box != null) box.updateScores(players);
			return p;
		}
	}
	
	public void removePlayer(AcroPlayer p) {
		if (p != null) {
			players.remove(p); //TODO: fix all the bugs this probably causes
			tch("playlist",dumpPlayers().toString());
		}
	}

	//public methods

	protected void newAcro(Connection conn, String acro_str) { //throws Exception {
		AcroPlayer p = getPlayer(conn.getHandle()); if (p == null) p = addNewPlayer(conn);	
		if (p == null) return; //zoiks
		Acro a = findAcro(p);
		if (a != null) {
			a.acro = acro_str;
			a.time = System.currentTimeMillis()-newtime;
			p.conn.tell(ZugServ.MSG_SERV,"Changed your acro.");
			p.conn.tell(ZugServ.MSG_SERV,"Time: " + a.time / (float)1000);
		}
		else {
			a = new Acro(acro_str,p,System.currentTimeMillis()-newtime);
			acrolist.add(a);
			p.conn.tell(ZugServ.MSG_SERV,"Entered your acro. Time: " + a.time / (float)1000);
			tch("Acro #" + acrolist.size() + " received!");
		}
	}

	protected void newVote(Connection conn, int v) {
		String handle = conn.getHandle();
		AcroPlayer p = getPlayer(handle);
		if (p == null) { p = addNewPlayer(conn); if (p == null) return; }
		if (acrolist.get(v).author == p) {
			tch("Voting for oneself is not allowed, " + handle + "."); return;
		}
		if (p.vote >= 0) acrolist.get(p.vote).votes--;
		acrolist.get(v).votes++; p.vote = v;
		p.conn.tell(ZugServ.MSG_SERV,"Your vote has been entered.");
	}

	protected String showLetters() {
		if (letters == null) return "Nothing loaded.";
		StringBuffer SB = new StringBuffer();
		for (int x=0;x<26;x++) {
			SB.append(letters[x].c + ": " +
					letters[x].prob + AcroServ.newLine);
		}
		return SB.toString();
	}
	
	//getter/setters, whee
	Connection getManager() { return manager; }
	void setManager(Connection mgr) { manager = mgr; }
	int getMode() { return mode; }
	void setMode(int m) { mode = m; }
	int getChan() { return chan; }
	int getAcrotime() { return acrotime; }
	void setAcrotime(int t) { acrotime = t; }
	int getVotetime() { return votetime; } //note there is also a getVoteTime() method
	void setVotetime(int t) { votetime = t; }
	int getWaittime() { return waittime; }
	void setWaittime(int t) { waittime = t; }
	int getMaxRound() { return maxround; }
	ZugServ getServ() { return serv; }
	String getAcro() { return acro; }
	
	protected String listPlayers() {
		StringBuffer playstr = new StringBuffer("Players: " + AcroServ.newLine);
		for (AcroPlayer p : players) {
			playstr.append(p.conn.getHandle() + ": " + p.score + AcroServ.newLine);
		}
		return playstr.toString();
	}
	
	public String listVars() {
		StringBuffer SB = new StringBuffer();
		//SB.append("Shouting: " + SHOUTING + AcroServ.CR);
		SB.append("Settings: " + AcroServ.newLine);
		SB.append("Channel: " + chan);
		SB.append(AcroServ.newLine);
		SB.append("Manager: " + (manager == null ? "Nobody" : 
		manager.getHandle()));
		SB.append(AcroServ.newLine);
		SB.append(tieBonus ? 
		"fastest acro breaks tie" : "ties are not broken");	
		SB.append(AcroServ.newLine);
		SB.append(reveal ? "public voting" : "private voting");
		SB.append(AcroServ.newLine);
		SB.append(usingTopics ? "topics" : "no topics");
		SB.append(AcroServ.newLine);
		SB.append(flatTime ? 
		"dynamic acro times" : "static acro times");
		SB.append(AcroServ.newLine);
		SB.append(adultTopics ? "adult themes" : "clean themes");
		SB.append(AcroServ.newLine);
		SB.append("Current players: " + players.size());
		SB.append(AcroServ.newLine);
		SB.append("Max players: " + maxplay);
		SB.append(AcroServ.newLine);
		SB.append("Min letters: " + minacro);
		SB.append(AcroServ.newLine);
		SB.append("Max letters: " + maxacro);
		SB.append(AcroServ.newLine);
		return SB.toString();
	}
}
