package net.elementj.tortilla;

import static org.junit.Assert.assertEquals;

import java.beans.BeanInfo;
import java.beans.Introspector;

import org.junit.Test;

/**
 * TortillaReflection test case for classes with multiple generic declaration.
 */
public class MultiGenericTypeResolutionTest {
	@Test
	public void testGenericPropertyOfSuperclass() throws Exception {
		SheepFarm container=new SheepFarm();		
		BeanInfo info=Introspector.getBeanInfo(container.getClass());
		assertEquals(Alfalfa.class,TortillaReflection.getActualPropertyType(container.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("plant")));
		assertEquals(Sheep.class,TortillaReflection.getActualPropertyType(container.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("animal")));
		assertEquals(Barn.class,TortillaReflection.getActualPropertyType(container.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("structure")));
	}
	@Test
	public void testGenericPropertyOfMultiSuperclass() throws Exception {
		ChickenFarm container=new ChickenFarm();
		BeanInfo info=Introspector.getBeanInfo(container.getClass());
		assertEquals(Chicken.class,TortillaReflection.getActualPropertyType(container.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("animal")));
		assertEquals(Corn.class,TortillaReflection.getActualPropertyType(container.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("plant")));
		assertEquals(Coupe.class,TortillaReflection.getActualPropertyType(container.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("structure")));
	}
	@Test
	public void testGetActualType() throws Exception {
		Class<?> clazz=TortillaReflection.getReturnType(ChickenFarm.class, ChickenFarm.class.getMethod("getPlant"));
		assertEquals(Corn.class,clazz);
		clazz=TortillaReflection.getReturnType(ChickenFarm.class, MultiGenericBase.class.getMethod("getPlant"));
		assertEquals(Corn.class,clazz);
		
	}
	public interface Plant { }
	public interface Animal { }
	public interface Structure { }
	public class Elderberry implements Plant { }
	public class AppleTree implements Plant { }
	public class Alfalfa implements Plant { }
	public class Corn implements Plant { } 
	public abstract class Bird implements Animal { }
	public class Chicken extends Bird { }
	public class Sheep implements Animal { }
	public class Barn implements Structure { }
	public class Greenhouse implements Structure { }
	public class Coupe implements Structure { }
	public class MultiGenericBase<P extends Plant,A extends Animal,S extends Structure> {
		private P plant;
		private A animal;
		private S structure;
		public P getPlant() {
			return plant;
		}
		public void setPlant(P plant) {
			this.plant = plant;
		}
		public A getAnimal() {
			return animal;
		}
		public void setAnimal(A animal) {
			this.animal = animal;
		}
		public S getStructure() {
			return structure;
		}
		public void setStructure(S structure) {
			this.structure = structure;
		}		
	}
	public class SheepFarm extends MultiGenericBase<Alfalfa,Sheep,Barn> { }
	public abstract class BirdFarm<P extends Plant,B extends Bird> extends MultiGenericBase<P,B,Coupe> {
	}
	public class ChickenFarm extends BirdFarm<Corn,Chicken> {
		
	}
}
