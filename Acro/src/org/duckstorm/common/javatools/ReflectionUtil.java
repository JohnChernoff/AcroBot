package org.duckstorm.common.javatools;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic utility methods using java.lang.reflect.*.
 * 
 * @author Doug Bateman ("DuckStorm")
 */
public class ReflectionUtil {

	/**
	 * An empty Class array, for use when looking up a method or constructor
	 * which takes no parameters.
	 */
	public static final Class<?>[] NO_ARGS = {};

	/**
	 * An empty Object array, for use when invoking a method which takes no
	 * parameters.
	 */
	public static final Object[] EMPTY_ARGS = {};

	/**
	 * Creates a new instance of the type using the default constructor. If for
	 * any reason this fails or generates an exception, null is returned
	 * instead.
	 * 
	 * @return a new instance of the specified clsas type
	 */
	public static final <T> T newInstanceOrNull(Class<T> type) {
		try {
			T instance = type.newInstance();
			return instance;
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	/**
	 * Returns an object of the specified type. This method is typically only
	 * useful for writing unit tests that require a trivial implementation of an
	 * interface. Repeated calls to this method with a given type are guaranteed
	 * to return the same result. If the type is an interface, all methods will
	 * no-op and return null-object return values. If the type is a non-abstract
	 * class with a default constructor, the default constructor is used. If the
	 * type is an enum, the first enum value is returned.
	 * 
	 * @return an object of the specified type.
	 * @throws InstantiationException
	 *             If an object of the specified type could not be created.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Object createNullObject(Class<T> type)
			throws InstantiationException {
		T instance = (T) nullObjectCache.get(type);
		if (instance == null) {
			if (void.class.equals(type)) {
				return null;
			} else if (type.isEnum()) {
				instance = type.getEnumConstants()[0];
			} else if (type.isInterface()) {
				ClassLoader loader = ObjectUtil.class.getClassLoader();
				instance = (T) Proxy.newProxyInstance(loader,
						new Class[] { type }, HANDLER);
			} else {
				try {
					instance = type.newInstance();
				} catch (IllegalAccessException e) {
					throw ExceptionUtil.initCause(new InstantiationException(),
							e);
				}
			}
			nullObjectCache.put(type, instance);
		}
		return instance;
	}

	/**
	 * Returns an object implementing all of the specified interfaces. This
	 * method is typically only useful for writing unit tests that require a
	 * trivial implementation of an interface. Repeated calls to this method
	 * with a given set of interfaces are guaranteed to return the same result.
	 * If the type is an interface, all methods will no-op and return
	 * null-object return values. If the type is a non-abstract class with a
	 * default constructor, the default constructor is used. If the type is an
	 * enum, the first enum value is returned.
	 * 
	 * @return an object implementing all the provided interfaces.
	 * @throws InstantiationException
	 *             If an object of the specified type could not be created.
	 */
	public static Object createNullObject(Class<?>... interfaces)
			throws InstantiationException {
		Class<?> type = Proxy.getProxyClass(LOADER, interfaces);
		Object instance = createNullObject(type);
		return instance;
	}

	/**
	 * Returns a array of objects, populated by calling createNullObject(Class)
	 * once for each of the specified types. This is typically useful for unit
	 * tests which wish to create dummy parameter values to pass to a method.
	 * 
	 * @return a array of objects of the specified types.
	 * @throws InstantiationException
	 *             If an object of the specified type could not be created.
	 */
	public static Object[] createNullObjectArray(Class<?>[] types)
			throws InstantiationException {
		Object[] instances = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			instances[i] = createNullObject(types[i]);
		}
		return instances;
	}

	private static final class NullObjectHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			Class<?> returnType = method.getReturnType();
			Object result = createNullObject(returnType);
			return result;
		}
	};

	private static final ClassLoader LOADER = ObjectUtil.class.getClassLoader();

	private static final NullObjectHandler HANDLER = new NullObjectHandler();

	private static Map<Class<?>, Object> nullObjectCache;
	static {
		Map<Class<?>, Object> map = new HashMap<Class<?>, Object>();
		map.put(Boolean.class, true);
		map.put(Boolean.TYPE, true);
		map.put(Character.class, 'z');
		map.put(Character.TYPE, 'z');
		map.put(Short.class, (short) 1);
		map.put(Short.TYPE, (short) 1);
		map.put(Integer.class, 1);
		map.put(Integer.TYPE, 1);
		map.put(Long.class, 1L);
		map.put(Long.TYPE, 1L);
		map.put(Float.class, 1F);
		map.put(Float.TYPE, 1F);
		map.put(Double.class, 1D);
		map.put(Double.TYPE, 1D);
		map.put(Class.class, Class.class);
		nullObjectCache = Collections.synchronizedMap(map);
	}

}
