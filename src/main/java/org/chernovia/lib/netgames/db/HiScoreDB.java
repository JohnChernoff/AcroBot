//rewrite with Files/Printwriters/etc instead of filename strings?

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

public class HiScoreDB {
	public static String CR = System.getProperty("line.separator");

	String DATAFILE = null, TEMPFILE = null;

	PrintStream log = System.out;

	boolean EDITING = false, VERBOSE = false;

	public HiScoreDB(String datafile, String tempfile) {
		DATAFILE = datafile;
		TEMPFILE = tempfile;
		try {
			new File(DATAFILE).createNewFile();
		} catch (IOException augh) {
			IOError(augh);
		}
		// catch (Exception augh) { System.err.println(augh); }
	}

	public HiScoreDB(String datafile) {
		this(datafile, "tmp.txt");
	}

	public HiScoreDB() {
		this("gamebase.txt", "tmp.txt");
	}

	public void setCR(String cr) {
		CR = cr;
	}

	public void IOError(IOException e) { // override?
		log.println(e.getMessage());
		System.exit(-1);
	}

	public String listFile(String F) {
		return IOUtil.listFile(F, CR);
	}

	public String topXStrs(String criteria, int num, boolean desc) {
		StringBuffer S = new StringBuffer("Top " + num + " " + criteria + ":"
				+ CR + HiScoreData.getHeader() + CR);
		HiScoreData[] D = topX(criteria, num, desc);
		for (int i = 0; i < D.length; i++) {
			S.append((i + 1) + ". " + D[i].playStr() + CR);
		}
		return S.toString();
	}

	public HiScoreData[] topX(String criteria, int num, boolean desc) {
		int n = IOUtil.countLines(DATAFILE);
		if (n > num)
			n = num;
		HiScoreData[] D = sortStat(criteria, desc);
		HiScoreData[] topScores = new HiScoreData[n];
		for (int i = 0; i < n; i++)
			topScores[i] = D[i];
		return topScores;
	}

	public HiScoreData[] sortStat(String s, boolean desc) {
		HiScoreData.setSort(s, desc);
		BufferedReader in = null;
		int n = IOUtil.countLines(DATAFILE);
		try {
			HiScoreData[] SortTab = new HiScoreData[n];
			in = new BufferedReader(new FileReader(DATAFILE));
			for (int x = 0; x < n; x++)
				SortTab[x] = new HiScoreData(new StringTokenizer(in.readLine()));
			in.close();
			Arrays.sort(SortTab);
			return SortTab;
		} catch (IOException e) {
			IOError(e);
			return null;
		}
	}

	public HiScoreData newEntry(String handle) {
		return new HiScoreData(new StringTokenizer(handle + " "
				+ HiScoreData.getInitData()));
	}

	// if there are < MAX_PLAYER_ENTRIES entries by a handle, make another
	public void enterEntry(HiScoreData entry, int maxEntries) {
		int waittime = 1;
		while (EDITING && waittime < 16) {
			try {
				Thread.sleep(waittime * 1000);
			} catch (InterruptedException ignore) {
			}
			waittime *= 2;
		}
		if (EDITING) {
			IOError(new IOException("Couldn't edit gamefile: file busy."));
			return;
		}
		EDITING = true;
		if (VERBOSE)
			log.println("Editing " + entry.getHandle());
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(TEMPFILE)));
			in = new BufferedReader(new FileReader(DATAFILE));
			int found = 0;
			int numLines = IOUtil.countLines(DATAFILE);
			String line = null;
			for (int i = 0; i < numLines; i++) {
				line = in.readLine();
				HiScoreData D = new HiScoreData(new StringTokenizer(line));
				if (D.getHandle().equalsIgnoreCase(entry.getHandle())
						&& found++ >= maxEntries) {
					out.println(entry);
				} else
					out.println(line);
			}
			if (found < maxEntries)
				out.println(entry); // add new entry
			in.close();
			out.close();
			File TempFile = new File(TEMPFILE), DataFile = new File(DATAFILE);
			if (!DataFile.delete() || !TempFile.renameTo(DataFile)) {
				IOError(new IOException("Augh, error renaming file: "
						+ DATAFILE));
			} else {
				if (VERBOSE)
					log.println("Renamed: " + DATAFILE);
			}
		} catch (IOException e) {
			IOError(e);
		}
		EDITING = false;
	}
}
