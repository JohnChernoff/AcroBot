package org.duckstorm.common.javatools;

/**
 * Generic utility methods for working with Objects.
 * 
 * @author Doug Bateman ("DuckStorm")
 */
public class ObjectUtil {

	/**
	 * Returns true if both objects are equal() and of the same class.
	 * 
	 * @return true if both objects are equal() and of the same class.
	 */
	public static boolean equal(Object a, Object b) {
		if (a == b)
			return true;
		if (a == null)
			return false;
		if (b == null)
			return false;
		Class<?> aType = a.getClass();
		Class<?> bType = b.getClass();
		if (!aType.equals(bType))
			return false;
		return a.equals(b);
	}

	/**
	 * Returns true of all the provided values are ==, false otherwise.
	 * 
	 * @return true of all the provided values are ==.
	 */
	public static boolean allSame(Object... values) {
		return ArrayUtil.allSame(values);
	}

	/**
	 * Returns true of all the provided values are equals() and of the same
	 * class, false otherwise.
	 * 
	 * @return true of all the provided values are equals() and of the same
	 *         class.
	 */
	public static boolean allEqual(Object... values) {
		return ArrayUtil.allEqual(values);
	}

	/**
	 * Returns true of all the provided values are null.
	 * 
	 * @return true of all the provided values are null.
	 */
	public static boolean allNull(Object... values) {
		return ArrayUtil.allNull(values);
	}

}
