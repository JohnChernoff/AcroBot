package org.chernovia.net.games.parlour.acro.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import org.chernovia.lib.netgames.db.GameBase;

public class AcroLetter {
	static String LETTEXT = ".ltr";
	int prob; String c;

	public AcroLetter(String s, int p) { c = s; prob = p; }

	public static AcroLetter[] loadABC(String ABCFILE) {
		AcroLetter[] alphabet = new AcroLetter[26];
		StringTokenizer S = null;
		try {
			BufferedReader in =
				new BufferedReader(new FileReader(
				//AcroServ.class.getResourceAsStream(ABCFILE)));
				new File(ABCFILE)));
			for (int x=0;x<26;x++) {
				S = new StringTokenizer(in.readLine());
				if (S.countTokens()<2) { in.close(); return null; }
				alphabet[x] = new AcroLetter(S.nextToken(),
						Integer.parseInt(S.nextToken()));
			}
			in.close();
		}
		catch (IOException e) {
			GameBase.IOError(e); return null;
		}
		return alphabet;
	}

	public static boolean writeABC(AcroLetter[] letters,
			String ABCFILE) {
		try {
			File F = new File(ABCFILE + LETTEXT);
			if (F.exists()) return false;
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(F)));
			for (int x=0;x<26;x++)
				out.println(letters[x].c + " " + letters[x].prob);
			out.close();
		}
		catch (IOException e) {
			GameBase.IOError(e); return false;
		}
		return true;
	}

	public static String listFiles() {
		StringBuffer SB = new StringBuffer();
		File[] files = new File("./res/acro").listFiles();
		for (int f=0;f<files.length;f++) {
			String fn = files[f].getName();
			int i = fn.indexOf(".");
			if (i>0 && fn.substring(i).equalsIgnoreCase(LETTEXT))
				SB.append(fn + AcroServ.CR);
		}
		return SB.toString();
	}
}
