package org.duckstorm.common.javatools;

/**
 * Generic utility methods for working with any form of java.lang.Throwable.
 * 
 * @author Doug Bateman ("DuckStorm")
 */
public class ExceptionUtil {

	public static <T extends Throwable> T initCause(T e, Throwable cause) {
		e.initCause(cause);
		return e;
	}

	public static void throwException(Throwable e) throws Exception {
		if (e == null) {
			return;
		} else if (e instanceof Exception) {
			throw (Exception) e;
		} else if (e instanceof Error) {
			throw (Error) e;
		} else {
			throw new IllegalArgumentException(
					"Neither an exception nor error", e);
		}
	}
}
