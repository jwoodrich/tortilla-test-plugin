package net.elementj.tortilla;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Base class for generated JUnit tests
 */
public class TortillaBase {
	private static final char MIN_CHAR=0x20, MAX_CHAR=0x7e, CHAR_RANGE=MAX_CHAR-MIN_CHAR;
	private final Random random=new Random();
	/**(
	 * Mock an instance of an interface.
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected<T> T mockInterface(Class<?> clazz) {
		return (T)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{clazz}, NOOPInvocationHandler.INSTANCE);		
	}
	/**
	 * Create an instance of a class for testing purposes.
	 * @param clazz The class to instantiate.
	 * @return The instance
	 */
	protected<T> T create(Class<T> clazz) {	
		return create(new HashSet<Class<?>>(),clazz);
	}
	/**
	 * Create an instance of a class for testing purposes, keeping a history of objects created to avoid loops.
	 * History is really just a placeholder at this point, since we're not attempting to use non-nullary constructors.
	 * @param history A set of classes that have already been instantiated.
	 * @param clazz The class to instantiate.
	 * @return The instance
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected<T> T create(Set<Class<?>> history, Class<T> clazz) {
		if (clazz==null) { return null; }
		if (history.contains(clazz)) { 
			throw new IllegalArgumentException("Loop detected in class creation.");
		}
		// TODO improve this
		history.add(clazz);
		if (clazz.isInterface()) {
			if (clazz==List.class||clazz==Collection.class) { return (T)new ArrayList(); }
			if (clazz==Set.class) { return (T)new HashSet(); }
			if (clazz==Map.class) { return (T)new HashMap(); }
			return mockInterface(clazz);
		}
		if (clazz.isArray()) {
			return (T)createArray(clazz.getComponentType()); 
		}
		if (clazz==boolean.class||clazz==Boolean.class) { return (T)Boolean.valueOf(random.nextInt(2)>0); }
		if (clazz==int.class||clazz==Integer.class) { return (T)Integer.valueOf(random.nextInt()); }
		if (clazz==long.class||clazz==Long.class) { return (T)Long.valueOf(random.nextLong()); }
		if (clazz==short.class||clazz==Short.class) { return (T)Short.valueOf((short)random.nextInt(Short.MAX_VALUE)); }
		if (clazz==char.class||clazz==Character.class) { return (T)Character.valueOf((char)random.nextInt(Character.MAX_VALUE)); }
		if (clazz==byte.class||clazz==Byte.class) { return (T)Byte.valueOf((byte)random.nextInt(Byte.MAX_VALUE)); }
		if (clazz==float.class||clazz==Float.class) { return (T)Float.valueOf(random.nextFloat()); }
		if (clazz==double.class||clazz==Double.class) { return (T)Double.valueOf(random.nextDouble()); }
		if (clazz==String.class) {
			char[] data=new char[random.nextInt(256)];
			for (int i=0;i<data.length;i++) {
				data[i]=(char)(random.nextInt(CHAR_RANGE)+MIN_CHAR);
			}
			return (T)new String(data);
		}
		if (clazz==StackTraceElement.class) { return (T)new StackTraceElement(clazz.getName(),"equals",clazz.getSimpleName()+".java",0); }
		if (Enum.class.isAssignableFrom(clazz)) {
			Set<T> set=EnumSet.allOf((Class<Enum>)clazz);
			int idx=random.nextInt(set.size());
			Iterator<T> it=set.iterator();
			for (int i=0;i<idx;i++) {
				it.next();
			}
			return it.next();
		}
		// try nullary constructor
		try { 
			return clazz.getConstructor().newInstance();
		}
		catch (Exception e) {
		}
		for (Method m : clazz.getMethods()) {
			if (Modifier.isStatic(m.getModifiers())&&Modifier.isPublic(m.getModifiers())&&"valueOf".equals(m.getName())&&m.getParameterTypes().length==1) {
				try { return (T)m.invoke(null, create(m.getParameterTypes()[0])); }
				catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		// At some point it might make sense to try to use the non-nullary constructors,
		// but let's start simple.
		throw new IllegalArgumentException("I can't create a "+clazz);
	}
	/**
	 * Create an array of a type, using a random length, loaded with objects instantiated using create.
	 * @param clazz The class to create an array of.
	 * @return The array instance.
	 */
	@SuppressWarnings("unchecked")
	protected<T> T[] createArray(Class<T> clazz) { 
		int size=random.nextInt(16);
		Object ret=Array.newInstance(clazz, size);
		for (int i=0;i<size;i++) {
			Array.set(ret, i, create(clazz));			
		}
		return (T[])ret;
	}
	/**
	 * JUnit assert to compare arrays.
	 * @param name
	 * @param expected
	 * @param actual
	 */
	protected static<T> void assertArrayEquals(String name, T[] expected, T[] actual) {
		assertEquals(name+" null comparison does not match",expected!=null,actual!=null);
		if (expected==null) { return; }
		assertEquals(name+" length does not match",expected.length,actual.length);
		for (int i=0;i<expected.length;i++) {
			assertEquals(name+"["+i+"] values do not match",expected[i],actual[i]);
		}
	}
	/**
	 * No-operation invocation handler for mocked interfaces. 
	 */
	public static class NOOPInvocationHandler implements InvocationHandler {
		private static final NOOPInvocationHandler INSTANCE=new NOOPInvocationHandler();
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				return proxy==args[0];
			} else if (method.getName().equals("hashCode")) {
				return this.hashCode();
			}
			return null;
		}
	}
}
