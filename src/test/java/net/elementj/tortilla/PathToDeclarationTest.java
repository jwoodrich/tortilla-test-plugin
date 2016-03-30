package net.elementj.tortilla;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import org.junit.Test;

/**
 * Tests the TortillaReflection getPathToDeclaration method.
 */
public class PathToDeclarationTest {
	@Test
	public void testFindPathToDeclarationViaSuperClass() throws Exception {
		List<Class<?>> lst=TortillaReflection.findPathToDeclaration(Sub3.class,Sub1.class);
		assertNotNull(lst);
		assertEquals(3,lst.size());
		assertEquals(Sub3.class,lst.get(0));
		assertEquals(Sub2.class,lst.get(1));
		assertEquals(Sub1.class,lst.get(2));
	}
	@Test
	public void testFindPathToDeclarationViaInterface() throws Exception {
		List<Class<?>> lst=TortillaReflection.findPathToDeclaration(Sub3.class,If2.class);
		System.err.println("Got back "+lst);
		assertNotNull(lst);
		assertEquals(3,lst.size());
		assertEquals(Sub3.class,lst.get(0));
		assertEquals(Sub2.class,lst.get(1));
		assertEquals(If2.class,lst.get(2));
	}
	@Test
	public void testFindPathToDeclarationViaSubInterface() throws Exception {
		List<Class<?>> lst=TortillaReflection.findPathToDeclaration(Sub3.class,If1.class);
		System.err.println("Got back "+lst);
		assertNotNull(lst);
		assertEquals(4,lst.size());
		assertEquals(Sub3.class,lst.get(0));
		assertEquals(Sub2.class,lst.get(1));
		assertEquals(If2.class,lst.get(2));
		assertEquals(If1.class,lst.get(3));
	}
	@Test
	public void testFindPathUsingGenericReturnType() throws Exception {
		Type returnType=Sub3.class.getMethod("getB").getGenericReturnType();
		GenericDeclaration declaration=((TypeVariable<?>)returnType).getGenericDeclaration();
		System.err.println("declaration="+declaration);
		assertNotNull(returnType);
		List<Class<?>> lst=TortillaReflection.findPathToDeclaration(Sub3.class, declaration);
		assertNotNull(lst);
		assertEquals(2,lst.size());
		assertEquals(Sub3.class,lst.get(0));
		assertEquals(Sub2.class,lst.get(1));
		assertEquals(true,lst.get(1).getGenericSuperclass() instanceof ParameterizedType);
		ParameterizedType pt=(ParameterizedType)lst.get(1).getGenericSuperclass();
		assertNotNull(pt.getActualTypeArguments());
		assertEquals(1,pt.getActualTypeArguments().length);
		assertEquals(true,pt.getActualTypeArguments()[0] instanceof Class<?>);
		assertEquals(Gen3.class,pt.getActualTypeArguments()[0]);		
	}
	@Test
	public void testFindPathUsingMultipleGenericReturnTypes() throws Exception {
		Type returnType=DoubleSub3.class.getMethod("getA").getGenericReturnType();
		if (returnType instanceof TypeVariable) {
			System.err.println("getA returns variable name "+((TypeVariable)returnType).getName());
		}
		GenericDeclaration declaration=((TypeVariable<?>)returnType).getGenericDeclaration();
		System.err.println("declaration="+declaration);
		assertNotNull(returnType);
		List<Class<?>> lst=TortillaReflection.findPathToDeclaration(DoubleSub3.class, declaration);
		assertNotNull(lst);
		System.err.println("lst="+lst);
		assertEquals(3,lst.size());
		assertEquals(DoubleSub3.class,lst.get(0));
		assertEquals(DoubleSub2.class,lst.get(1));
		assertEquals(DoubleSub1.class,lst.get(2));
		assertEquals(true,lst.get(0).getGenericSuperclass() instanceof ParameterizedType);
		assertEquals(true,lst.get(1).getGenericSuperclass() instanceof ParameterizedType);
	}
	@Test
	public void testGetReturnType() throws Exception {
		Class<?> actual=TortillaReflection.getReturnType(DoubleSub3.class, DoubleSub3.class.getMethod("getA"));
		assertNotNull(actual);
		assertEquals(Gen3.class,actual);
	}
	interface If1 { }
	interface If2 extends If1 { }
	interface If3 extends If2 { }
	interface IfA { }
	interface IfB { }
	interface IfC { }
	class Sub1<A extends Gen1> { public A getA() { return (A)null; } }
	class Sub2<B extends Gen1> extends Sub1<Gen3> implements If2, GenC<B> { 
		public B getB() { return null; }
	}
	class Sub3 extends Sub2<Gen3> { }
	interface DoubleSubIf<A extends Gen1, B extends GenC<?>> { A getA(); B getB(); }
	class DoubleSub1<C extends Gen1, D extends GenC<?>> implements DoubleSubIf<C,D> { 
		public C getA() {
			return null;
		}
		public D getB() {
			return null;
		}
	}
	class DoubleSub2<D extends GenC<?>> extends DoubleSub1<Gen3,D> { }
	class DoubleSub3 extends DoubleSub2<GenC<?>> { }
	interface Gen1 { }
	interface Gen2 extends Gen1 { }
	interface Gen3 extends Gen2 { }
	class Gen3i implements Gen3 { }
	interface GenA { }
	interface GenB { }
	interface GenC<B extends Gen1> { B getB(); }
}
