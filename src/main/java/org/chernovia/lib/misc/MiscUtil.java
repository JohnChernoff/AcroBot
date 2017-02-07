package org.chernovia.lib.misc;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.imageio.ImageIO;
import org.chernovia.lib.misc.IOUtil;

//TODO: get rid of strToInt

public class MiscUtil {

	public static String listArray(Object[] array) {
		return listArray(array, array.length);
	}

	public static String listArray(Object[] array, String LS) {
		return listArray(array, array.length, LS);
	}

	public static String listArray(Object[] array, int len) {
		return listArray(array, len, " ");
	}

	public static String listArray(Object[] array, int len, String LS) {
		if (array == null || array.length < 1)
			return "-";
		StringBuffer SB = new StringBuffer("");
		for (int i = 0; i < len; i++)
			SB.append(array[i].toString() + LS);
		return SB.toString();
	}

	public static String listVec(Vector<?> v) {
		return listVec(v, " ");
	}

	public static String listVec(Vector<?> v, String CR) {
		if (v == null)
			return "";
		StringBuffer SB = new StringBuffer("");
		for (Object o: v) {
			SB.append(o);
			SB.append(CR);
		}
		return SB.toString();
	}

	public static int strToInt(String s) {
		if (s == null)
			return 0;
		int n = -1;
		try {
			n = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			n = -1;
		}
		return n;
	}

	public static long strToLong(String s) {
		if (s == null)
			return 0;
		long n = -1;
		try {
			n = Long.parseLong(s);
		} catch (NumberFormatException e) {
			n = -1;
		}
		return n;
	}

	public static float strToFloat(String s) {
		if (s == null)
			return 0;
		float n = -.01f;
		try {
			n = Float.parseFloat(s);
		} catch (NumberFormatException e) {
			n = -.01f;
		}
		return n;
	}

	public static int findNum(String s) {
		int i = -1;
		StringTokenizer ST = new StringTokenizer(s);
		while (ST.hasMoreTokens() && i < 0)
			i = strToInt(ST.nextToken());
		return i;
	}

	public static String txtPad(int n) {
		StringBuffer S = new StringBuffer(" ");
		for (int x = 0; x < n; x++)
			S.append(" ");
		return S.toString();
	}

	public static int lastSpc(String S) {
		for (int x = S.length() - 1; x >= 0; x--) {
			if (S.charAt(x) == ' ')
				return x;
		}
		return -1;
	}

	public static String removeSpaces(String name) {
		StringBuffer SB = new StringBuffer();
		for (int i = 0; i < name.length(); i++)
			if (name.charAt(i) != ' ')
				SB.append(name.charAt(i));
		return SB.toString();
	}

	public static String getSuffix(int x) {
		String s = Integer.toString(x);
		if (x > 3 && x < 21)
			return x + "th";
		switch (strToInt(s.substring(s.length() - 1))) {
		case 1:
			return x + "st";
		case 2:
			return x + "nd";
		case 3:
			return x + "rd";
		default:
			return x + "th";
		}
	}

	public static String[] mergeStrArrays(String[] a1, String[] a2) {
		int len = a1.length + a2.length;
		String[] newStr = new String[len];
		for (int i = 0; i < a1.length; i++)
			newStr[i] = a1[i];
		for (int i = 0; i < a2.length; i++)
			newStr[i + a1.length] = a2[i];
		return newStr;
	}

	// all random functions return 0-(n-1)
	public static int randInt(int n) {
		return (int) (Math.random() * n);
	}

	public static long randLong(long n) {
		return (long) (Math.random() * n);
	}

	public static float rndFloat(float n) {
		return (float) (Math.random() * n);
	}

	public static int sign(int x, int y) {
		if (x > y)
			return 1;
		else if (x < y)
			return -1;
		else
			return 0;
	}

	public static boolean mergeStr(String str, StringBuffer SB, String CR) {
		int i = SB.indexOf(str);
		if (i > 1) {
			int numidx = SB.substring(0, i).lastIndexOf(CR);
			if (numidx < 0)
				numidx = 0;
			else
				numidx += 2;
			int numoff = i - numidx;
			String numstr = SB.substring(numidx, i - 1);
			int n = strToInt(numstr) + 1;
			String repstr = n + " " + str;
			SB.replace(numidx, numidx + numoff + str.length(), repstr);
			return true;
		} else {
			SB.append("1 " + str);
			return false;
		}
	}

	public static float[] getGraph(String filename) {
		List<String> DataVec = IOUtil.readFile(filename);
		if (DataVec == null)
			return null;
		int e = 0;
		int offset = 0;
		if (DataVec.size() > 1) {
			offset = strToInt(DataVec.get(0));
			e++;
		}
		StringTokenizer ST = new StringTokenizer(DataVec.get(e));
		int graphlen = ST.countTokens();
		float[] pcurve = new float[graphlen + 2];
		pcurve[0] = offset;
		pcurve[1] = 0; // offset,sum
		for (int i = 2; i < pcurve.length; i++) {
			pcurve[i] = MiscUtil.strToFloat(ST.nextToken());
			pcurve[1] += pcurve[i];
		}
		return pcurve;
	}

	public static int rndGraph(float[] pcurve) {
		int i;
		float pt = 0;
		float r = rndFloat(pcurve[1]);
		for (i = 2; i < pcurve.length; i++) {
			pt += pcurve[i];
			if (pt > r)
				break;
		}
		return (i - (int) pcurve[0]) - 2;
	}

	public static String tokenizerToString(StringTokenizer ST) {
		StringBuffer SB = new StringBuffer("");
		while (ST.hasMoreTokens())
			SB.append(ST.nextToken() + " ");
		return SB.toString();
	}

	public static Method[] getAllMethods(Class<?> C) {
		Vector<Method> MV = new Vector<Method>();
		getAllMethods(C, MV);
		Method[] meths = new Method[MV.size()];
		MV.toArray(meths);
		return meths;
	}

	private static void getAllMethods(Class<?> C, Vector<Method> MV) {
		Method[] meths = C.getDeclaredMethods();
		for (Method m : meths)
			MV.add(m);
		if (C.getSuperclass() != null)
			getAllMethods(C.getSuperclass(), MV);
	}

	public static double mapValueToRange(double val, double val_min,
			double val_max, double range_min, double range_max) {
		if (val < val_min || val > val_max) { // should really throw an
			// exception here
			return (val_min - 1);
		}
		double val_scope = val_max - val_min;
		double range_scope = range_max - range_min;
		return (((val - val_min) / val_scope) * range_scope) + range_min;
	}

	public static long calibrate() {
		double z = 0;
		long t = System.currentTimeMillis();
		for (double i = 0; i < 100000; i++) {
			z = Math.atan(i);
			z = Math.sin(z);
			z = Math.cos(z);
		}
		return 250 - (System.currentTimeMillis() - t);
	}

	public static void delay(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignore) {
		}
	}

	public static void dumpArray(int[] array) {
		for (int i = 0; i < array.length; i++)
			System.out.print(i + ":" + array[i] + " ");
		System.out.println();
	}

	public static void dumpArray(Object[] array) {
		for (int i = 0; i < array.length; i++)
			System.out.print(array[i] + " ");
		System.out.println();
	}

	public static int[] reverseSort(int[] array) {
		Arrays.sort(array);
		int[] reverseArray = new int[array.length];
		int a = 0;
		for (int i = reverseArray.length - 1; i >= 0; i--)
			reverseArray[i] = array[a++];
		array = reverseArray;
		return array;
	}
	
	public static BufferedImage getScaledImage(String file,int x, int y) {
		System.out.println("File: " + file);
		Image img = null;
		try { 
			img = ImageIO.read(new File(file)).getScaledInstance(
			x,y, Image.SCALE_SMOOTH);	
		}
		catch (Exception e) { e.printStackTrace(); return null; }
		BufferedImage buffImg = new BufferedImage(x,y,BufferedImage.TYPE_INT_ARGB);
		Graphics g = buffImg.createGraphics();
		g.drawImage(img,0,0,null); g.dispose();
		return buffImg;
	}
	
	public static String[] getStringData(String fileName) {
		String[] names = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(
			new InputStreamReader(new FileInputStream(fileName)));
		    String line; int i=0;
		    br.mark(1000); while ((line = br.readLine()) != null) { i++; }
		    names = new String[i];
		    br.reset(); i = 0;
		    while ((line = br.readLine()) != null) {
		    	names[i++] = line;
		    }
		} 
		catch (Exception e) { e.printStackTrace(); }
		try { br.close(); } catch (IOException fark) {}
		return names;
	}
	
    public static <T extends Enum<?>> T randomEnum(Class<T> clazz){
        int x = new Random().nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

}
