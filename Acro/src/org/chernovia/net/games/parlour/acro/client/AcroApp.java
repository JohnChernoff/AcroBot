package org.chernovia.net.games.parlour.acro.client;

import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import org.chernovia.lib.misc.MiscUtil;

public class AcroApp extends Frame implements 
ActionListener, KeyListener, Runnable {
	
	public static void main(String[] args) {
		AcroApp F = new AcroApp();
		F.setSize(1200, 800);
		F.init();
		F.setVisible(true);
	}
	
	private class ClockThread extends Thread {
		int seconds;
		public ClockThread(int s) { seconds = s; }
		public void run() {
			 do {
				clockLab.setText(seconds + ":00");
				try { sleep(1000); }
				catch (InterruptedException ergh) {
					clockLab.setText("--:--"); return; 
				}
			} while (seconds-- >= 0);
		}
	}
	private static final long serialVersionUID = 1L;
	static final String	CR = System.getProperty("line.separator");
	static final String 
	ACRO_DG = "&", ACRO_DELIM = "~", SERV_DG = "%", SERV_DELIM = " ";
	static final int LOBBY = 0,
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
	DG_NEXT_ACRO = 10,
	DG_LOG_FAIL= -1, DG_LOGIN = 0,
	DG_CHAN = 1, DG_NEW_TELL = 2, DG_ASK = 3,
	DG_CHAN_LEAVE = 0, DG_CHAN_ENTER = 1,
	CH_CHAT = 0, CH_ACRO = 1, CH_CMD = 2, CH_MODES = 3;
	
	static final int MAX_TEXT = 32768; //yeah baby
	ClockThread countdown;
	String prompt = ">";
	Panel loginPan, mainPan, chanGrid, acroPan, infoPan, votePan, 
	chanPan,chatPan;
	AcroCan acroCan;
	Choice chatChoice;
	TextField chatText, nameText; //, passTxt;
	TextArea helpText, servText,infoText,varText,playText;
	Label logLab, chanLab, clockLab;
	Button logButt, lobButt, startButt;
	Button[] chanButts;
	VoteButt[] voteButts;
	Socket servSock;
	PrintWriter out;
	BufferedReader in;
	String handle = "Handel";
	int maxChan = 0, maxAcros = 12;
	static int port = 5678;
	boolean PLAYING = false;
	//AudioClip clip;
	
	public void init() {
		//clip = getAudioClip(getCodeBase(),"acro.wav");
		setLayout(new BorderLayout());
		nameText = new TextField(handle);
		nameText.selectAll();
		logLab = new Label("Enter your handle:");
		logButt = new Button("Login");
		logButt.addActionListener(this);
		loginPan = new Panel();
		loginPan.add(logLab);
		loginPan.add(nameText);
		loginPan.add(logButt);
		add(loginPan,BorderLayout.NORTH);
		helpText = new TextArea("Welcome to Acrophobia!" + CR +
			"Join channel one (or higher) to play." + CR +
			"Click the 'start' button in the top right corner " +
			"to begin a new game." + CR + 
			"Use the Control or Tab key to toggle between " +
			"sending chat messages and acro messages." + CR + "Enjoy!");
		helpText.setEditable(false);
		helpText.setBackground(Color.green);
		
		add(helpText,BorderLayout.CENTER);
		//preload...
		acroPan = new Panel();
		acroPan.setLayout(new BorderLayout());
		acroCan = new AcroCan(
		getWidth(),80,loadLetterImgs(),Color.white);
		acroPan.add(acroCan,BorderLayout.CENTER);
		//MOAR
		servText = new TextArea("",8,25,
		TextArea.SCROLLBARS_VERTICAL_ONLY);
		servText.setEditable(false);
		servText.setBackground(Color.darkGray);
		servText.setForeground(Color.white);
		Font font =	new Font(Font.SERIF,Font.PLAIN,14);
		servText.setFont(font);
		chatText = new TextField(8); 
		chatText.setText("Enter text here, use CTRL or TAB to toggle");
		chatText.selectAll();
		chatText.addKeyListener(this);
		chatText.setFocusTraversalKeysEnabled(false);
		chatChoice = new Choice();
		chatChoice.insert("Acro",CH_ACRO);
		chatChoice.insert("Chat",CH_CHAT); 
		chatChoice.insert("Cmd",CH_CMD);
		chatPan = new Panel();
		chatPan.setLayout(new BorderLayout());
		chatPan.add(chatText,BorderLayout.CENTER);
		chatPan.add(chatChoice,BorderLayout.WEST);
		mainPan = new Panel();
		mainPan.setLayout(new BorderLayout());
		mainPan.add(servText,BorderLayout.CENTER);
		mainPan.add(chatPan,BorderLayout.SOUTH);
		clockLab = new Label("--:--");
		clockLab.setBackground(Color.white); //Color.red);
		clockLab.setForeground(Color.black); //new Color(59,89,152));
		clockLab.setFont(new Font(Font.MONOSPACED,Font.BOLD,36));
		acroPan.add(clockLab,BorderLayout.EAST);
		mainPan.add(acroPan,BorderLayout.NORTH);
		infoPan = new Panel();
		infoPan.setLayout(new BorderLayout());
		infoPan.setBackground(Color.black);
		infoText = new TextArea("",5,16,TextArea.SCROLLBARS_NONE);
		infoText.setEditable(false);
		infoText.setBackground(Color.cyan);
		infoText.setForeground(Color.black);
		infoPan.add(infoText,BorderLayout.NORTH);
		varText = new TextArea("",9,16,TextArea.SCROLLBARS_NONE);
		varText.setEditable(false);
		varText.setBackground(Color.pink);
		varText.setForeground(Color.black);
		infoPan.add(varText,BorderLayout.CENTER);
		playText = new TextArea("",17,16,TextArea.SCROLLBARS_NONE);
		playText.setEditable(false);
		playText.setBackground(Color.lightGray);
		playText.setForeground(Color.black);
		infoPan.add(playText,BorderLayout.SOUTH);
	}
	
	public Image[] loadLetterImgs() {
		Image[] letters = new Image[26];
		String dir = System.getProperty("user.dir");
		try {
		for (int i=0;i<26;i++) {
			String loc = dir + "\\res\\acro\\letters\\" + 
					(char)(i+65) + ".png";
			System.out.println("Loading: " + loc);
			letters[i] = Toolkit.getDefaultToolkit().getImage(loc);
			System.out.println(letters[i]);
			System.out.println(prepareImage(letters[i], 60, 80, null));
		}
		}
		catch (Exception e) {
			System.out.println("Augh: " + e);
		}
		return letters;
	}
	
	public void enterGame() {
		removeAll();
		setLayout(new BorderLayout());
		//build channels, now that we know how many there are
		chanPan = new Panel();
		chanPan.setBackground(Color.black);
		chanPan.setLayout(new BorderLayout());
		chanLab = new Label("Channels:");
		chanLab.setForeground(Color.black);
		chanLab.setBackground(Color.gray);
		chanPan.add(chanLab,BorderLayout.WEST);
		chanGrid = new Panel();
		chanGrid.setBackground(Color.black);
		chanGrid.setLayout(new GridLayout(1,maxChan-1));
		chanButts = new Button[maxChan];
		lobButt = new Button("Lobby"); 
		lobButt.addActionListener(this);
		chanGrid.add(lobButt);
		for (int i=0; i<(maxChan-1); i++) {
			chanButts[i] = new Button("" + (i+1));
			chanButts[i].addActionListener(this);
			chanGrid.add(chanButts[i]);
		}
		chanPan.add(chanGrid,BorderLayout.CENTER);
		startButt = new Button("Start"); 
		startButt.addActionListener(this);
		chanPan.add(startButt,BorderLayout.EAST);
		mainPan.add(chanPan,BorderLayout.NORTH);
		//build main screen
		add(mainPan,BorderLayout.CENTER);
		add(acroPan,BorderLayout.NORTH);
		add(infoPan,BorderLayout.WEST);
		add(chatPan,BorderLayout.SOUTH);
		paintAll(getGraphics());
		chatText.requestFocus();
		//clip.loop();
	}
	
	public void showVotePanel(StringTokenizer line) {
		int n = MiscUtil.strToInt(line.nextToken());
		votePan = new Panel();
		votePan.setLayout(new GridLayout(maxAcros,1));
		voteButts = new VoteButt[maxAcros];
		for (int i=0;i<n;i++) {
			voteButts[i] = new VoteButt(i+1,line.nextToken(),
			line.nextToken());
			line.nextToken(); //ignore votes
		}
		for (int i=n;i<maxAcros;i++) {
			voteButts[i] = new VoteButt(0,"","");
			voteButts[i].setVisible(false);
		}
		for (int i=0;i<maxAcros;i++) {
			voteButts[i].setSize(getWidth()-100,getHeight()/n);
			voteButts[i].setBackground(Color.orange);
			voteButts[i].setForeground(Color.black);
			if (!voteButts[i].author.equals(handle)) {
				voteButts[i].addActionListener(this);
				voteButts[i].setBackground(Color.green);
			}
			votePan.add(voteButts[i]);
		}
		remove(mainPan); add(votePan,BorderLayout.CENTER);
		paintAll(getGraphics());
	}
	
	public void restoreMainPanel() {
		if (votePan != null) {
			remove(votePan); votePan = null; 
			add(mainPan,BorderLayout.CENTER); 
			paintAll(getGraphics());
		}
	}
	
	public boolean connect() {
		if (connected()) return true;
		while (servSock == null) {
			try {
				servSock = new Socket("",port);
			}
			catch (Exception e) {
				blurb("Connection Error: " +
						e.getMessage());
				servSock = null;
				try { Thread.sleep(2500); }
				catch (InterruptedException i) {}
			}
		}
		try {
			out = new PrintWriter(
					servSock.getOutputStream(),true);
			in = new BufferedReader(
					new InputStreamReader(
							servSock.getInputStream()));
		}
		catch (IOException e) {
			blurb("IO Error: " + e.getMessage()); return false;
		}
		if (servSock != null) {
			new Thread(this).start(); return true;
		}
		else return false;
	}
	
	public void stop() {
		blurb("Applet stopped...");
		/* if (PLAYING) try { out.close(); in.close(); }
		catch (IOException e) {
			blurb("IO Error: " + e.getMessage()); return;
		}*/
	}
	
	public void run() {
		PLAYING = true;
		String line = "";
		while (connected()) {
			try { line = in.readLine(); }
			catch (IOException augh) {
				System.out.println("Error: " + augh);
				return; 
			}
			System.out.println(line);
			if (line.startsWith(ACRO_DG)) {
				parseAcroDG(chop(line,ACRO_DELIM));
			}
			else if (line.startsWith(SERV_DG)) {
				parseServerDG(chop(line," "));
			}
			else if (!line.equals(prompt)) blurb(line);
		}
	}
	
	public StringTokenizer chop(String str,String delim) {
		StringTokenizer ST = new StringTokenizer(str,delim);
		if (ST.hasMoreTokens()) { ST.nextToken(); return ST;	}
		else return null;
	}
	
	public void parseAcroDG(StringTokenizer line) {
		int dg = MiscUtil.strToInt(line.nextToken());
		switch (dg) {
		case DG_INFO:
			infoText.setText("");
			infoText.append("Channel:" + line.nextToken() + CR);
			infoText.append("Manager:" + line.nextToken() + CR);
			infoText.append("Round:" + line.nextToken() + CR);
			infoText.append("Topic:" + line.nextToken() + CR);
			infoText.append("Acro:" + line.nextToken() + CR);
			//infoText.append("Mode:" + line.nextToken() + CR);
			paintAll(getGraphics());
			break;
		case DG_VARS:
			varText.setText("");
			while (line.hasMoreTokens()) { 
				varText.append(line.nextToken() + CR);
			}
			break;
		case DG_PLAYERS:
			playText.setText("Players: " + line.nextToken() + CR);
			while (line.countTokens() > 1) { 
				playText.append(line.nextToken() + ": " + 
				line.nextToken() + CR);
			}
			break;
		case DG_NEW_ACRO:
			acroCan.setAcro(line.nextToken());
			restoreMainPanel();
			break;
		case DG_ACRO_TIME:
			countdown = new ClockThread(
			MiscUtil.strToInt(line.nextToken()));
			countdown.start();
			break;
		case DG_SHOW_ACROS:
			showVotePanel(line);
			break;
		}
	}
	
	public void parseServerDG(StringTokenizer line) {
		int dg = MiscUtil.strToInt(line.nextToken());
		switch (dg) {
		case DG_LOGIN:
			if (line.countTokens() >= 2) {
				handle = line.nextToken();
				System.out.println("Handle: " + handle);
				maxChan = MiscUtil.strToInt(line.nextToken());
				sendServ("!GUI");
				sendServ("!ch 1");
				sendServ("!vars");
				enterGame();
				break;
			}
		case DG_LOG_FAIL:
			logLab.setText("Bad handle!");
			logButt.setEnabled(true);
			break;
		case DG_CHAN:
			sendServ("!vars");
		}
	}
	
	public boolean connected() {
		return servSock == null ? false : servSock.isConnected();
	}

	public void blurb(String txt) { blurb(txt,true); }
	public void blurb(String txt, boolean newline) {
		if (servText == null) {
			System.out.println(txt); return;
		}
		else if (newline) servText.append(txt + CR);
		else servText.append(txt);
		if (servText.getText().length() > MAX_TEXT) {
			String text = servText.getText();
			servText.setText(text.substring(
			text.length() -	MAX_TEXT/8));
		}
	}
	
	public void sendServ(String txt) {
		if (connected()) out.println(txt);
	}
	
	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {
		if (e.getSource() == chatText) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				String txt = 
				chatText.getText().replace(ACRO_DELIM, "_");
				servText.append(CR + "> " + txt + CR);
				switch (chatChoice.getSelectedIndex()) {
				case CH_ACRO: 
					sendServ(txt); break;
				case CH_CHAT: 
					sendServ("!kib " + chatText.getText()); break;
				case CH_CMD: 
					sendServ("!" + chatText.getText()); break;
				}
				chatText.setText("");
			}
			else if (e.getKeyCode() == KeyEvent.VK_TAB) {
				chatChoice.select(
				(chatChoice.getSelectedIndex() == CH_ACRO) ?
				CH_CHAT : CH_ACRO);
			}
			else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
				int i = chatChoice.getSelectedIndex();
				if (++i == CH_MODES) i = 0;
				chatChoice.select(i);
			}
		}
	}
	
	public void login(String name) {
		connect(); sendServ(name);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == logButt) {
			logButt.setEnabled(false);
			login(nameText.getText());
		}
		else if (e.getSource() == lobButt) {
			changeChan("" + LOBBY);
		}
		else if (e.getSource() == startButt) {
			sendServ("!start");
		}
		else if (e.getSource().getClass() == VoteButt.class) {
			sendServ("" + ((VoteButt)e.getSource()).number);
			restoreMainPanel();
		}
		else {
			changeChan(((Button)e.getSource()).getLabel());
		}
	}
	
	public void changeChan(String ch) {
		if (countdown != null) countdown.interrupt();
		sendServ("!ch " + ch);
	}
}
