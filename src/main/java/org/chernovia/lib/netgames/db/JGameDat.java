package org.chernovia.lib.netgames.db;

import java.util.StringTokenizer;
import java.util.Vector;

import org.chernovia.lib.misc.MiscUtil;

//Key Field is always the first one (handle)!
public class JGameDat implements Comparable<JGameDat> {
	protected static Vector<FieldData> FieldTab;
	protected Vector<String> GameFields;
	private static int SortField = 0;
	public static String CR = System.getProperty("line.separator");

	public static final String DEF_FLDS =
		"INT Rating INT Games INT Wins INT Currstr " +
		"INT Streak INT Skid STR Title",
		DEF_INITDATA = "1500 0 0 0 0 0 NT";
	public static String INITDATA = null; //DEF_INITDATA;

	public static void initFields(String FieldStr,String InitStr) {
		INITDATA = InitStr; StringTokenizer ST =
			new StringTokenizer("STR Handle " + FieldStr);
		Vector<FieldData> V = new Vector<FieldData>();
		while (ST.countTokens() >= 2)
			V.add(new FieldData(ST.nextToken(),ST.nextToken()));
		FieldTab = V;
	}

	public static void setSort(String S) {
		SortField = 0;
		for (int f=0;f<FieldTab.size();f++) {
			if ((FieldTab.elementAt(f)).FName.
					equalsIgnoreCase(S)) {
				SortField = f; return;
			}
		}
	}

	public static String listFields(String CR) {
		StringBuffer SB = new StringBuffer();
		for (int f=0;f<FieldTab.size();f++) {
			FieldData F = FieldTab.elementAt(f);
			SB.append(F.FName + ": " + F.FType + CR);
		}
		return SB.toString();
	}

	public static String statHead() {
		StringBuffer SB = new StringBuffer();
		for (int f=0;f<FieldTab.size();f++) {
			FieldData FD = (FieldTab.elementAt(f));
			SB.append(FD.FName + " ");
		}
		return SB.toString();
	}

	public JGameDat(StringTokenizer ST) {
		GameFields = new Vector<String>();
		if (ST.countTokens()!=FieldTab.size()) {
			System.out.println("Bad Format: " + ST.countTokens() +
					" != " + FieldTab.size());	System.exit(-1);
					return;
		}
		while (ST.hasMoreTokens()) GameFields.add(ST.nextToken());
	}

	public String getHandle() { //handle is always first
		return getField(
				(FieldTab.firstElement()).FName);
	}

	public String getField(String S) {
		for (int f=0;f<FieldTab.size();f++) {
			if ((FieldTab.elementAt(f)).FName.
					equalsIgnoreCase(S)) {
				return GameFields.elementAt(f);
			}
		}
		return "Augh: Field Not Found"; //or null?
	}

	public int getIntField(String S) {
		return MiscUtil.strToInt(getField(S));
	}

	public boolean setField(String S, int V) {
		return setField(S,String.valueOf(V));
	}

	public boolean setField(String S, String V) {
		for (int f=0;f<FieldTab.size();f++) {
			if ((FieldTab.elementAt(f)).FName.
					equalsIgnoreCase(S)) {
				GameFields.setElementAt(V,f); return true;
			}
		}
		return false; //"Augh: Field Not Found";
	}

	public int compareTo(JGameDat D) {
		if (!this.getClass().equals(D.getClass())) return -1;
		FieldData FD = FieldTab.elementAt(SortField);
		if (FD.FType.equals("INT")) {
			int x = MiscUtil.strToInt(GameFields.elementAt(
					SortField));
			int y = MiscUtil.strToInt(D.GameFields.elementAt(
					SortField));
			return y-x;
		}
		else {
			return (GameFields.elementAt(SortField)).
			compareTo(D.GameFields.elementAt(SortField));
		}
	}

	public String makeStr() {
		StringBuffer SB = new StringBuffer();
		for (int f=0;f<GameFields.size();f++)
			SB.append(GameFields.elementAt(f) + " ");
		return SB.toString();
	}

	public String playStr() { return GameFields.toString(); }
}