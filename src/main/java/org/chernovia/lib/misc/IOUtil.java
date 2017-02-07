package org.chernovia.lib.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IOUtil {

	public static final String CR = System.getProperty("line.separator");

	private static Logger logger = Logger.getLogger(IOUtil.class.getName());

	public static boolean addToFile(String txt, String fileName) {
		return writeFile(txt, fileName, true);
	}

	public static void closeQuietly(InputStream in) {
		if (in == null)
			return;
		try {
			in.close();
		} catch (java.lang.Exception e) {
			logger
					.log(
							Level.WARNING,
							"Exception caught and trapped while closing InputStream",
							e);
		} catch (java.lang.Error e) {
			logger.log(Level.WARNING,
					"Error detected but not caught while closing InputStream",
					e);
		}
	}

	public static void closeQuietly(OutputStream out) {
		if (out == null)
			return;
		try {
			out.close();
		} catch (java.lang.Exception e) {
			logger.log(Level.WARNING,
					"Exception caught and trapped while closing OutputStream",
					e);
		} catch (java.lang.Error e) {
			logger.log(Level.WARNING,
					"Error detected but not caught while closing OutputStream",
					e);
		}
	}

	public static void closeQuietly(Reader in) {
		if (in == null)
			return;
		try {
			in.close();
		} catch (java.lang.Exception e) {
			logger.log(Level.WARNING,
					"Exception caught and trapped while closing Reader", e);
		} catch (java.lang.Error e) {
			logger.log(Level.WARNING,
					"Error detected but not caught while closing Reader", e);
		}
	}

	public static void closeQuietly(Writer out) {
		if (out == null)
			return;
		try {
			out.close();
		} catch (java.lang.Exception e) {
			logger.log(Level.WARNING,
					"Exception caught and trapped while closing Writer", e);
		} catch (java.lang.Error e) {
			logger.log(Level.WARNING,
					"Error detected but not caught while closing Writer", e);
		}
	}

	public static int countLines(String fileName) {
		int x = 0;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			while (true) {
				String s = in.readLine();
				if (s == null)
					break;
				x++;
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE,
					"Unable to read file \"{0}\".  Returning 0.", fileName);
			return 0;
		} finally {
			closeQuietly(in);
		}
		return x;
	}

	public static String listFile(String fileName, String cr) {
		StringBuffer s = new StringBuffer("");
		List<String> result = readFile(fileName);
		if (result != null) {
			for (String line : result) {
				s.append(line);
				s.append(cr);
			}
		}
		return s.toString();
	}

	public static String listFile(URL url, String cr) {
		StringBuffer s = new StringBuffer("");
		List<String> result = readFile(url);
		if (result != null) {
			for (String line : result) {
				s.append(line);
				s.append(cr);
			}
		}
		return s.toString();
	}

	public static Properties loadProperties(Class<?> clazz, String resourceName) {
		Properties props = new Properties();
		InputStream in = null;
		try {
			in = clazz.getResourceAsStream(resourceName);
			props.load(in);
		} catch (IOException e) {
			AssertionError ex = new AssertionError("Unable to load resource: " + resourceName);
			ex.initCause(e);
			logger.log(Level.SEVERE, "Unable to load resource: " + resourceName, ex);
		} finally {
			closeQuietly(in);
		}
		return props;
	}

	public static boolean makeFile(String txt, String fileName) {
		return writeFile(txt, fileName, false);
	}

	public static List<String> readFile(String fileName) {
		List<String> result = new ArrayList<String>();
		String s = null;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			do {
				s = in.readLine();
				if (s != null)
					result.add(s);
			} while (s != null);
		} catch (IOException e) {
			logger.log(Level.SEVERE,
					"Unable to read file \"{0}\".  Returning null.", fileName);
			return null;
		} finally {
			closeQuietly(in);
		}
		return result;
	}

	public static List<String> readFile(URL url) {
		List<String> result = new ArrayList<String>();
		String s = null;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			do {
				s = in.readLine();
				if (s != null)
					result.add(s);
			} while (s != null);
		} catch (IOException e) {
			logger.log(Level.SEVERE,
					"Unable to read \"{0}\".  Returning null.", url);
			return null;
		} finally {
			closeQuietly(in);
		}
		return result;
	}

	public static int searchFile(String txt, String fileName) {
		if (txt == null)
			return -1;
		txt = txt.trim();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			int lineNum = 0;
			while (true) {
				String s = in.readLine();
				if (s == null)
					break;
				s = s.trim();
				if (s.equalsIgnoreCase(txt)) {
					return lineNum;
				}
				lineNum++;
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE,
					"Unable to read file \"{0}\".  Returning 0.", fileName);
		} finally {
			closeQuietly(in);
		}
		return -1;
	}

	public static boolean writeFile(String txt, String fileName, boolean append) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName,
					append)));
			out.println(txt);
			out.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to write to \"{0}\".", fileName);
			return false;
		} finally {
			closeQuietly(out);
		}
		return true;
	}

	/*
	 * Thanks to Bret Taylor for the following FTP Class...
	 */
	// public static boolean uploadFTP(String host, String username,
	// String password, String remoteFileName, String localFileName) {
	// boolean ok = true;
	// FTPConnection connection = new FTPConnection();
	// try {
	// if (connection.connect(host)
	// && connection.login(username, password)) {
	// if (!connection.uploadFile(remoteFileName, localFileName))
	// ok = false;
	// } else
	// ok = false;
	// connection.disconnect();
	// } catch (UnknownHostException e) {
	// ok = false;
	// } catch (IOException e) {
	// ok = false;
	// }
	// return ok;
	// }
	//
	// public static boolean downloadFTP(String host, String username,
	// String password, String remoteFileName, String localFileName) {
	// boolean ok = true;
	// FTPConnection connection = new FTPConnection();
	// try {
	// if (connection.connect(host)
	// && connection.login(username, password)) {
	// if (!connection.downloadFile(remoteFileName, localFileName))
	// ok = false;
	// } else
	// ok = false;
	// connection.disconnect();
	// } catch (UnknownHostException e) {
	// ok = false;
	// } catch (IOException e) {
	// ok = false;
	// }
	// return ok;
	// }
}
