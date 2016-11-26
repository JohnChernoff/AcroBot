package org.chernovia.lib.netgames.db;

//NOTE: GROUP BY eliminates dups in that field, use ORDER BY!
//NOTE: should probably throw exceptions instead of returning silly int error codes

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class SQLConn {
	public static final int SQL_ERR = -1, SQL_EXEC_ERR = 0, SQL_EXEC_OK = 1;

	public static final int VERB_MAX = 10, VERB_LOW = 3, VERB_NORM = 5,
	VERB_HIGH = 8;

	private static Connection Conn = null;

	private static PrintWriter log = null;

	private static boolean ERRFREAK = false;

	private static int VERBOSITY = VERB_NORM;

	private static int MAX_LOG_LEN = 72;

	public static void sqlError(SQLException e) {
		if (!ERRFREAK)
			return;
		close();
		System.exit(-1);
	}

	public static boolean getError() {
		return ERRFREAK;
	}

	public static boolean setError(boolean newerr) {
		boolean preverr = ERRFREAK;
		ERRFREAK = newerr;
		return preverr;
	}

	public static void setVerbosity(int level) {
		VERBOSITY = level;
		if (VERBOSITY > VERB_MAX)
			VERBOSITY = VERB_MAX;
	}

	public static void setLog(PrintWriter pw) {
		DriverManager.setLogWriter(pw);
		log = pw;
	}

	public static void log(String logstr) {
		log.println(logstr);
	}

	public static void initSQL(String DBName, String drivestr, PrintWriter pw) {
		try {
			setLog(pw);
			Class.forName(drivestr).newInstance();
		} catch (Exception e) {
			e.printStackTrace(log);
			System.exit(-1);
		}
		connectDB(DBName);
	}

	public static boolean connectDB(String DBName) {
		try {
			Conn = DriverManager.getConnection("jdbc:mysql://localhost/"
					+ DBName);
			log.println("Driver Name: " + Conn.getMetaData().getDriverName());
			return true;
		} catch (SQLException e) {
			sqlError(e);
			return false;
		}
	}

	public static boolean close() {
		try {
			Conn.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace(log);
			return false;
		}
	}

	public static int loadData(String datafile, String datatab) {
		String S = "LOAD DATA INFILE '" + datafile + "' INTO TABLE " + datatab
		+ " FIELDS TERMINATED BY ','";
		return execDB(S);
	}

	// TODO: throw exceptions!
	public static int execDB(String cmd) {
		if (VERBOSITY > VERB_LOW) {
			if (cmd.length() < MAX_LOG_LEN)
				log.println("Cmd: " + cmd);
			else
				log.println("Cmd: " + cmd.substring(0, MAX_LOG_LEN - 1));
		}
		try {
			Statement S = Conn.createStatement();
			if (S.execute(cmd))
				return SQL_EXEC_OK;
			else
				return SQL_EXEC_ERR;
		} catch (SQLException e) {
			sqlError(e);
			return SQL_ERR;
		}
	}

	public static ResultSet getRS(String qstr) {
		if (VERBOSITY > VERB_NORM) {
			if (qstr.length() < MAX_LOG_LEN)
				log.println("Qry: " + qstr);
			else
				log.println("Qry: " + qstr.substring(0, MAX_LOG_LEN - 1));
		}
		try {
			Statement S = Conn.createStatement();
			ResultSet RS = S.executeQuery(qstr);
			if (!RS.first())
				return null;
			else
				return RS;
		} catch (SQLException e) {
			sqlError(e);
			return null;
		}
	}

	public static String getStr(String sqlstr) {
		ResultSet RS = getRS("SELECT " + sqlstr);
		if (RS == null)
			return null;
		try {
			return RS.getString(1);
		} catch (SQLException e) {
			sqlError(e);
			return null;
		}
	}

	public static int getInt(String sqlstr) {
		ResultSet RS = getRS("SELECT " + sqlstr);
		if (RS == null)
			return Integer.MIN_VALUE;
		try {
			return RS.getInt(1);
		} catch (SQLException e) {
			sqlError(e);
			return Integer.MIN_VALUE;
		}
	}

	// TODO: use SQL Count() function
	public static int count(String tabstr) {
		ResultSet RS = getRS("SELECT * FROM " + tabstr);
		if (RS == null)
			return 0;
		try {
			RS.last();
			return RS.getRow();
		} catch (SQLException e) {
			sqlError(e);
			return 0;
		}
	}

	public static int lastID(String tabstr) {
		return getInt("id FROM " + tabstr + " ORDER BY id DESC LIMIT 1");
	}

	public static boolean addRec(String tabstr, String addstr) {
		int c = count(tabstr);
		execDB("INSERT INTO " + tabstr + " " + addstr);
		if (count(tabstr) > c)
			return true;
		else
			return false;
	}

	public static Vector<String> getData(String sqlstr) {
		Vector<String> vdata = new Vector<String>();
		ResultSet RS = getRS(sqlstr);
		if (RS == null)
			return null;
		try {
			do
				vdata.add(RS.getString(1));
			while (RS.next());
		} catch (SQLException e) {
			sqlError(e);
			return null;
		}
		return vdata;
	}

	public static Vector<Integer> getIntData(String sqlstr) {
		Vector<Integer> vdata = new Vector<Integer>();
		ResultSet RS = getRS(sqlstr);
		if (RS == null)
			return null;
		try {
			do
				vdata.add(RS.getInt(1));
			while (RS.next());
		} catch (SQLException e) {
			sqlError(e);
			return null;
		}
		return vdata;
	}

	public static Vector<String> getData(String fld, String tab) {
		return getData(fld, tab, "ORDER BY " + fld);
	}

	public static Vector<String> getData(String fld, String tab, String crit) {
		Vector<String> vdata = new Vector<String>();
		ResultSet RS = getRS("SELECT * FROM " + tab + " " + crit);
		if (RS == null)
			return null;
		try {
			do
				vdata.add(RS.getString(fld));
			while (RS.next());
		} catch (SQLException e) {
			sqlError(e);
			return null;
		}
		return vdata;
	}

	public static int editTab(String table, String field, int value, String crit) {
		return SQLConn.execDB("UPDATE " + table + " SET " + field + " = "
				+ value + " WHERE " + crit);
	}

	public static int editTab(String table, String field, String strvalue,
			String crit) {
		return SQLConn.execDB("UPDATE " + table + " SET " + field + " = "
				+ strvalue + " WHERE " + crit);
	}

	// NOTE: Perhaps move this to some more general library
	public static String getVecString(Vector<String> v) {
		int n = v.size();
		StringBuffer SB = new StringBuffer();
		for (int i = 0; i < n; i++) {
			if (i > 0)
				SB.append(",");
			SB.append(v.elementAt(i));
		}
		return SB.toString();
	}
}
