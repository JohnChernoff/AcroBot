package org.chernovia.lib.netgames.roomserv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

import org.chernovia.lib.misc.MiscUtil;

public class Loc_Conn extends ConnAdapter {

	private Socket sock;
	private BufferedReader in;
	private PrintWriter out;
	private String prompt;
	private boolean CONNECTED;
	private long lastTell;

	public Loc_Conn(Loc_Serv server, Socket socket) {
		super(server);
		sock = socket;
		try {
			out = new PrintWriter(sock.getOutputStream(),
					true);
			in = new BufferedReader(new InputStreamReader(
					sock.getInputStream()));
		}
		catch (IOException e) {	augh(e.toString());	}
		CONNECTED = false; prompt = null; lastTell = 0;
	}

	public void setPrompt(String str) { prompt = str; }

	public void augh(String msg) {
		System.out.println(msg); System.exit(1);
	}

	@Override
	public void handleMsg(String msg) {
		String cmd = "DEF";
		StringTokenizer ST = new StringTokenizer(msg);
		if (msg.length() > 1) cmd = ST.nextToken();

		if (msg.equalsIgnoreCase("!GUI")) {
			setGUI(!isGUI());
			tell("GUI set to " + isGUI());
		}
		else if (msg.equalsIgnoreCase("!EXIT")) {
			try { sock.close(); }
			catch (IOException e) { augh(e.toString()); }
		}
		else if (msg.equalsIgnoreCase("!WHO")) {
			tell(getServ().who());
		}
		else if (cmd.equalsIgnoreCase("!WHOIS")) {
			if (ST.hasMoreElements()) {
				String hand = ST.nextToken();
				tell(getServ().whois(hand));
			}
		}
		else if (cmd.equalsIgnoreCase("!TELL")) {
			if (ST.countTokens() > 1) {
				String hand = ST.nextToken();
				String mess = MiscUtil.tokenizerToString(ST);
				getServ().tell(getHandle(),hand,mess);
			}
		}
		else if (cmd.equals("!") || cmd.equalsIgnoreCase("!SHOUT")) {
			if (ST.hasMoreElements()) {
				getServ().broadcast(getHandle() + " shouts: " +
				MiscUtil.tokenizerToString(ST));
			}
		}
		else if (cmd.equals("!") || cmd.equalsIgnoreCase("!KIB")) {
			if (ST.hasMoreElements()) {
				getServ().tch(getChan(),getHandle() + ": " +
				MiscUtil.tokenizerToString(ST),false,false);
			}
		}
		else if (cmd.equals("@") || cmd.equalsIgnoreCase("!CH")) {
			if (ST.hasMoreElements()) {
				int c = MiscUtil.strToInt(ST.nextToken());
				setChan(c);
			}
		}
		else {
			super.handleMsg(msg);
		}
		lastTell = System.currentTimeMillis();
	}

	private String getPromptCmd(String prom) {
		String cmd = ""; boolean oops = false;
		tell(prom);
		try { cmd = in.readLine(); }
		catch (IOException e) {	oops = true; }
		if (!oops && cmd != null) return cmd;
		else { CONNECTED = false; return null; }
	}

	public void run() {
		//must leave this to conn so not to bog server down
		CONNECTED = true;
		while (CONNECTED && 
		!((Loc_Serv)getServ()).login(this,getPromptCmd("Name?"),"")) {};
		if (prompt == null) setPrompt("%" + getHandle() + "%");
		while (CONNECTED) {
			String response = getPromptCmd(prompt);
			if (response != null) handleMsg(response);
		}
		getServ().disconnected(this);
	}

	public PrintWriter getOut() { return out; }
	
	public long idleTime() {
		return System.currentTimeMillis() - lastTell;
	}
	
	public void ban(int seconds) {
		try { sock.close(); }
		catch (IOException augh) {}
	}

}
