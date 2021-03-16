package org.chernovia.lib.misc.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class SQLUtil {
	private static Connection conn = null;
	private static PrintWriter log = null;
	private static int logstr = 128;
	private static boolean VERBOSE = true;
	private static boolean ERRFREAK = false;
	private static String db_name = "NODB";

	public static void log(String logstr) {
		log.println(logstr);
	}

	public static void initSQl(String DBName) {
		initSQL("com.mysql.jdbc.Driver",new PrintWriter(System.out,true),DBName,"","");
	}

	public static void initSQl(String DBName,String usr,String pwd) {
		initSQL("com.mysql.jdbc.Driver",new PrintWriter(System.out,true),DBName,usr,pwd);
	}

	public static void initSQL(String drivestr, PrintWriter pw,
		String DBName,String usr,String pwd) {
		try { setLog(pw); Class.forName(drivestr).newInstance(); }
		catch (Exception e) { e.printStackTrace(log); } //System.exit(-1); }
		connectDB(DBName,usr,pwd);
	}

	private static boolean connectDB(String DBName, String usr, String pwd) {
		db_name = DBName;
		try {
			if (usr.equals("")) conn = DriverManager.getConnection("jdbc:mysql://localhost/" + DBName);
			else conn = DriverManager.getConnection("jdbc:mysql://localhost/" + DBName,usr,pwd);
			log.println("Driver Name: " +  conn.getMetaData().getDriverName());
			return true;
		}
		catch (SQLException e) { sqlError(e); return false; }
	}
	
	//alternative method, maybe not needed (or even desired)
	public static void init(String db, String usr, String pwd) {
		setLog(new PrintWriter(System.out,true));
		try {
			conn = DriverManager.getConnection(
		    "jdbc:mysql://localhost/" + db + "?user=" + usr + "&password=" + pwd);
			log.println("Driver Name: " +  conn.getMetaData().getDriverName());
		} catch (SQLException augh) { augh.printStackTrace(log); System.exit(-1); }
	}

	public static String getDBName() { return db_name; }

	public static int execDB(String cmd) {
		if (VERBOSE) {
			if (cmd.length() < logstr) log.println("Cmd: " + cmd);
			else log.println("Cmd: " + cmd.substring(0,logstr-1));
		}
		try {
			Statement S = conn.createStatement();
			if (S.execute(cmd)) return 1; else return 0;
		}
		catch (SQLException e) { sqlError(e); return -1; }
	}

	public static ResultSet getRS(String qstr) {
		if (VERBOSE) {
			if (qstr.length()<logstr) log.println("Qry: " + qstr);
			else log.println("Qry: "+ qstr.substring(0,logstr-1));
		}
		try {
			Statement S = conn.createStatement();
			ResultSet RS = S.executeQuery(qstr);
			if (!RS.first()) return null; else return RS;
		}
		catch (SQLException e) { sqlError(e); return null; }
	}

	public static int loadData(String datafile,
			String datatab) {
		String S = "LOAD DATA INFILE '" + datafile +
		"' INTO TABLE " + datatab +
		" FIELDS TERMINATED BY ','";
		return execDB(S);
	}

	public static String getStr(String sqlstr) {
		ResultSet RS = getRS("SELECT " + sqlstr);
		if (RS == null) return null;
		try { return RS.getString(1); }
		catch (SQLException e) { sqlError(e); return null; }
	}

	public static int getInt(String sqlstr) {
		ResultSet RS = getRS("SELECT " + sqlstr);
		if (RS == null) return Integer.MIN_VALUE;
		try { return RS.getInt(1); }
		catch (SQLException e) {
			sqlError(e); return Integer.MIN_VALUE;
		}
	}

	//TODO: use SQL Count() function
	public static int count(String tabstr) {
		ResultSet RS = getRS("SELECT * FROM " + tabstr);
		if (RS == null) return 0;
		try { RS.last(); return RS.getRow(); }
		catch (SQLException e) { sqlError(e); return 0;	}
	}

	public static int count(String tabstr,String crit) {
		ResultSet RS = getRS("SELECT * FROM " + tabstr + " " + crit);
		if (RS == null) return 0;
		try { RS.last(); return RS.getRow(); }
		catch (SQLException e) { sqlError(e); return 0;	}
	}

	public static int lastID(String tabstr) {
		return getInt("id FROM " + tabstr + " ORDER BY id DESC LIMIT 1");
	}

	public static boolean addRec(String tabstr, String addstr) {
		int c = count(tabstr);
		execDB("INSERT INTO " + tabstr + " " + addstr);
		if (count(tabstr) > c) return true; else return false;
	}

	public static String[] getSingleColData(String sqlstr,int cols) {
		ResultSet RS = getRS(sqlstr);
		if (RS == null) return null;
		String[] data = new String[cols];
		try { RS.first(); for (int i=1;i<=cols;i++) data[i-1] = RS.getString(i); }
		catch (SQLException e) { sqlError(e); return null; }
		return data;
	}

	public static String[][] getData(String sqlstr,int cols) {
		ResultSet RS = getRS(sqlstr);
		if (RS == null) return null;
		int row = 0;
		String[][] data;
		try {
			RS.last();
			data = new String[cols][RS.getRow()];
			RS.first();
			do {
				for (int i=0;i<cols;i++) data[i][row] = RS.getString(i);
				row++;
			} while (RS.next());
		}
		catch (SQLException e) { sqlError(e); return null; }
		return data;
	}

	public static Vector<String> getFieldList(String sqlstr) {
		Vector<String> vdata = new Vector<String>();
		ResultSet RS = getRS(sqlstr);
		if (RS == null) return null;
		try {
			do vdata.add(RS.getString(1)); while (RS.next());
		}
		catch (SQLException e) { sqlError(e); return null; }
		return vdata;
	}
	public static Vector<Integer> getIntData(String sqlstr) {
		Vector<Integer> vdata = new Vector<Integer>();
		ResultSet RS = getRS(sqlstr);
		if (RS == null) return null;
		try {
			do vdata.add(RS.getInt(1));
			while (RS.next());
		}
		catch (SQLException e) { sqlError(e); return null; }
		return vdata;
	}
	public static Vector<String> getFieldData(String fld, String tab) {
		return getFieldData(fld,tab,"ORDER BY " + fld);
	}
	public static Vector<String> getFieldData(String fld, String tab,
			String crit) {
		Vector<String> vdata = new Vector<String>(); ResultSet RS = getRS(
				"SELECT * FROM " + tab + " " + crit);
		if (RS == null) return null;
		try {
			do vdata.add(RS.getString(fld)); while (RS.next());
		}
		catch (SQLException e) { sqlError(e); return null; }
		return vdata;
	}

	public static String sqlStr(Vector<String> v) {
		int n = v.size(); StringBuffer SB = new StringBuffer();
		for (int i=0;i<n;i++) {
			if (i>0) SB.append(",");
			SB.append(v.get(i));
		}
		return SB.toString();
	}

	public static int editTab(String table, String field,
			int value, String crit) {
		return SQLUtil.execDB("UPDATE " + table + " SET " +
				field + " = " + value + " " + crit);
	}

	public static int editTab(String table, String field,
			String strvalue, String crit) {
		return SQLUtil.execDB("UPDATE " + table + " SET " +
				field + " = " + strvalue + " " + crit);
	}

	public static boolean close() {
		try { conn.close(); return true; }
		catch (SQLException e) {
			e.printStackTrace(log);  return false;
		}
	}

	public static void sqlError(SQLException e) {
		if (ERRFREAK) { close(); System.exit(-1); }
		else e.printStackTrace();
	}
	public static boolean getError() { return ERRFREAK; }
	public static boolean setError(boolean newerr) {
		boolean preverr = ERRFREAK; ERRFREAK = newerr;
		return preverr;
	}

	public static void setLog(PrintWriter pw) {
		DriverManager.setLogWriter(pw); log = pw;
	}

	public static void setVerb(boolean v) { VERBOSE = v; }
}

