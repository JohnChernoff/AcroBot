package org.duckstorm.common.javatools;

/**
 * Generic utility methods for working with Arrays.
 * 
 * @author Doug Bateman ("DuckStorm")
 */
public class ArrayUtil {

	/**
	 * An array with zero elements.
	 */
	private static Object[] EMPTY_ARRAY = {};

	/**
	 * Returns an array with zero elements.
	 * 
	 * @return an array with zero elements.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] emptyArray() {
		return (T[]) EMPTY_ARRAY;
	}

	/**
	 * Returns true of all the values in the array are equals() to each other
	 * and of the same class, false otherwise.
	 * 
	 * @return true of all the provided values are equals() and of the same
	 *         class.
	 */
	public static boolean allEqual(Object[] array) {
		if (array == null)
			return true;
		if (array.length <= 1)
			return true;
		Object o = array[0];
		if (o == null) {
			return allNull(array);
		}
		Class<?> oType = o.getClass();
		for (int i = 1; i < array.length; i++) {
			if (null == array[i])
				return false;
		}
		for (int i = 1; i < array.length; i++) {
			Object x = array[i];
			Object xType = x.getClass();
			if (!oType.equals(xType))
				return false;
			if (!o.equals(x))
				return false;
		}
		return true;
	}

	/**
	 * Returns true of all the values in the array are == to each other, false
	 * otherwise.
	 * 
	 * @return true of all the provided values are ==.
	 */
	public static boolean allSame(Object[] array) {
		if (array == null)
			return true;
		if (array.length <= 1)
			return true;
		Object o = array[0];
		for (int i = 1; i < array.length; i++) {
			if (o != array[i])
				return false;
		}
		return true;
	}

	/**
	 * Returns true of all the provided value in the array are null.
	 * 
	 * @return true of all the provided values are null.
	 */
	public static boolean allNull(Object[] array) {
		if (array == null)
			return true;
		for (int i = 0; i < array.length; i++) {
			if (null != array[i])
				return false;
		}
		return true;
	}

}
