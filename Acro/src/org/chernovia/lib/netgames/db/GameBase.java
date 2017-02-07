package org.chernovia.lib.netgames.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.chernovia.lib.misc.IOUtil;

//TODO: create blank file if no datafile is found
abstract public class GameBase extends IOUtil {
	public static String CR = System.getProperty("line.separator");

	public static String DATAFILE = "gamebase.txt";

	public static String TEMPFILE = "tmp.txt";

	public static PrintStream log = System.out;

	public static void IOError(IOException e) { // override?
		log.println(e.getMessage());
		e.printStackTrace();
	}

	public static String listFile(String F) { // for clarity
		return listFile(F, CR);
	}

	public static String topTen(String s) {
		StringBuffer S = new StringBuffer("Top ten " + s + ":" + CR
				+ GameData.statHead() + CR);
		int n = countLines(DATAFILE);
		if (n > 10)
			n = 10;
		GameData[] D = sortStat(s);
		for (int x = 0; x < n; x++) {
			S.append((x + 1) + ". " + D[x].playStr() + CR);
		}
		return S.toString();
	}

	public static GameData[] sortStat(String s) {
		GameData.setSort(s);
		BufferedReader in = null;
		int n = countLines(DATAFILE);
		try {
			GameData[] SortTab = new GameData[n];
			in = new BufferedReader(new FileReader(DATAFILE));
			for (int x = 0; x < n; x++)
				SortTab[x] = new GameData(new StringTokenizer(in.readLine()));
			in.close();
			Arrays.sort(SortTab);
			return SortTab;
		} catch (IOException e) {
			IOError(e);
			return null;
		}
	}

	public static GameData getStats(String P, StringTokenizer NP) {
		log.println("Looking for " + P);
		BufferedReader in = null;
		int n;
		try {
			in = new BufferedReader(new FileReader(DATAFILE));
			n = countLines(DATAFILE);
			for (int x = 0; x < n; x++) {
				GameData D = new GameData(new StringTokenizer(in.readLine()));
				if (D.getField("Handle").equals(P)) {
					in.close();
					return D;
				}
			}
			in.close();
			if (NP != null)
				return new GameData(NP); // new player
			else
				return null;
		} catch (IOException e) {
			IOError(e);
			return null;
		}
	}

	public static void editStats(GameData P) {
		log.println("Editing " + P.getHandle());
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			int n = countLines(DATAFILE);
			String S = null;
			out = new PrintWriter(new BufferedWriter(new FileWriter(TEMPFILE)));
			in = new BufferedReader(new FileReader(DATAFILE));
			boolean found = false;
			for (int x = 0; x < n; x++) {
				S = in.readLine();
				GameData D = new GameData(new StringTokenizer(S));
				if (D.getHandle().equalsIgnoreCase(P.getHandle())) {
					out.println(P.makeStr());
					found = true;
				} else
					out.println(S);
			}
			if (!found)
				out.println(P.makeStr()); // add new player
			in.close();
			out.close();
			File TempFile = new File(TEMPFILE);
			File DataFile = new File(DATAFILE);
			if (!DataFile.delete() || !TempFile.renameTo(DataFile)) {
				log.println("Augh: error renaming file");
			}
			// else { log.println("Renamed"); }
		} catch (IOException e) {
			IOError(e);
		}
	}
}
