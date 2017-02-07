package org.chernovia.lib.netgames.db;

import java.util.StringTokenizer;
import java.util.Vector;

import org.chernovia.lib.misc.MiscUtil;

//Key Field is always the first one (handle)!
//check INITIALIZED
public class HiScoreData implements Comparable<HiScoreData> {
	public static String CR = System.getProperty("line.separator");

	public static boolean INITIALIZED = false, SORT_DESC = true;

	static Vector<FieldData> FieldTab = null;

	static String InitData = null;

	static int SortField = 0;

	Vector<String> GameFields;

	public static void initFields(String FieldStr, String initData) {
		StringTokenizer ST = new StringTokenizer("STR Handle " + FieldStr);
		Vector<FieldData> V= new Vector<FieldData>();
		while (ST.countTokens() >= 2) {
			V.add(new FieldData(ST.nextToken(), ST.nextToken()));
		}
		FieldTab = V;
		InitData = initData;
		INITIALIZED = true;
	}

	public static String getInitData() {
		return InitData;
	}

	public static String getFields() {
		StringBuffer SB = new StringBuffer();
		for (int f = 0; f < FieldTab.size(); f++) {
			FieldData F = FieldTab.elementAt(f);
			SB.append(F.FName + ": " + F.FType + CR);
		}
		return SB.toString();
	}

	public static String getHeader() {
		StringBuffer SB = new StringBuffer();
		for (int f = 0; f < FieldTab.size(); f++) {
			FieldData FD = (FieldTab.elementAt(f));
			SB.append(FD.FName + " ");
		}
		return SB.toString();
	}

	//public static void setSort(String fld) { setSort(fld,true); }
	public static void setSort(String fld, boolean sortDescending) {
		SORT_DESC = sortDescending;
		SortField = 0;
		for (int f = 0; f < FieldTab.size(); f++) {
			if ((FieldTab.elementAt(f)).FName
					.equalsIgnoreCase(fld)) {
				SortField = f;
				return;
			}
		}
	}

	public HiScoreData(StringTokenizer ST) {
		GameFields = new Vector<String>();
		if (ST.countTokens() != FieldTab.size()) {
			System.out.println("Bad Format: " + ST.countTokens() + " != "
					+ FieldTab.size());
			System.exit(-1);
			return;
		}
		while (ST.hasMoreTokens()) {
			GameFields.add(ST.nextToken());
		}
	}

	public String getHandle() { //handle is always first
		return getStrField((FieldTab.firstElement()).FName);
	}

	public String getStrField(String fld) {
		for (int f = 0; f < FieldTab.size(); f++) {
			if ((FieldTab.elementAt(f)).FName
					.equalsIgnoreCase(fld)) {
				return GameFields.elementAt(f);
			}
		}
		return "Augh: Field Not Found"; //or null?
	}

	public int getIntField(String fld) {
		return MiscUtil.strToInt(getStrField(fld));
	}

	public boolean setField(String fld, int V) {
		return setField(fld, String.valueOf(V));
	}

	public boolean setField(String fld, String V) {
		for (int f = 0; f < FieldTab.size(); f++) {
			if ((FieldTab.elementAt(f)).FName
					.equalsIgnoreCase(fld)) {
				GameFields.setElementAt(V, f);
				return true;
			}
		}
		return false; //"Augh: Field Not Found";
	}

	public String playStr() {
		return GameFields.toString();
	}

	@Override
	public String toString() {
		StringBuffer SB = new StringBuffer();
		for (int f = 0; f < GameFields.size(); f++)
			SB.append(GameFields.elementAt(f) + " ");
		return SB.toString();
	}

	public int compareTo(HiScoreData D) {
		if (!this.getClass().equals(D.getClass()))
			return -1;
		FieldData FD = FieldTab.elementAt(SortField);
		if (FD.FType.equals("INT")) {
			int x = MiscUtil.strToInt(GameFields.elementAt(SortField));
			int y = MiscUtil.strToInt(D.GameFields
					.elementAt(SortField));
			if (SORT_DESC)
				return y - x;
			else
				return x - y;
		} else {
			if (SORT_DESC) {
				return (GameFields.elementAt(SortField))
				.compareTo(D.GameFields.elementAt(SortField));
			} else {
				return (D.GameFields.elementAt(SortField))
				.compareTo(GameFields.elementAt(SortField));
			}
		}
	}
}
