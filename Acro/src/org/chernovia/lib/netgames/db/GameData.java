package org.chernovia.lib.netgames.db;

import java.util.StringTokenizer;
import java.util.Vector;

import org.chernovia.lib.misc.MiscUtil;

//Key Field is always the first one!
public class GameData implements Comparable<GameData> {
	protected Vector<String> GameFields;
	protected static Vector<FieldData> FieldTab;
	private static int SortField = 0;

	public static void initData(Vector<FieldData> FT) {
		FieldTab = FT;
	}

	public GameData(StringTokenizer ST) {
		GameFields = new Vector<String>();
		if (ST.countTokens()!=FieldTab.size()) {
			System.out.println("Bad Format: " + ST.countTokens() +
					" != " + FieldTab.size());	return;
		}
		while (ST.hasMoreTokens()) GameFields.add(ST.nextToken());
	}

	public String getHandle() { //handle is always first
		return getField(
				(FieldTab.firstElement()).FName);
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

	public int compareTo(GameData D) {
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
	public static String statHead() {
		StringBuffer SB = new StringBuffer();
		for (int f=0;f<FieldTab.size();f++) {
			FieldData FD = (FieldTab.elementAt(f));
			SB.append(FD.FName + " ");
		}
		return SB.toString();
	}
}