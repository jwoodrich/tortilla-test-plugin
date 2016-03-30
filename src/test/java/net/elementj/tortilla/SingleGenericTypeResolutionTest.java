package net.elementj.tortilla;

import static org.junit.Assert.assertEquals;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.util.Queue;

import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * TortillaReflection unit test for generic type resolution with a single generic property declaration.
 */
public class SingleGenericTypeResolutionTest {
	@Test
	public void testGenericPropertyOfSuperclass() throws Exception {
		TeslaChargingStation session=new TeslaChargingStation();
		BeanInfo info=Introspector.getBeanInfo(TeslaChargingStation.class);
		Class<?> clazz=TortillaReflection.getActualPropertyType(session.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("car"));
		assertEquals(Tesla.class,clazz);
		clazz=TortillaReflection.getActualPropertyType(session.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("queue"));
		assertEquals(Queue.class,clazz);
	}
	@Test
	public void testGenericPropertyOfMultiSuperclass() throws Exception {
		ModelSChargingStation session=new ModelSChargingStation();
		BeanInfo info=Introspector.getBeanInfo(TeslaChargingStation.class);
		Class<?> clazz=TortillaReflection.getActualPropertyType(session.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("car"));
		assertEquals(ModelS.class,clazz);
		clazz=TortillaReflection.getActualPropertyType(session.getClass(),TestUtil.map(info.getPropertyDescriptors()).get("queue"));
		assertEquals(Queue.class,clazz);
	}
	private static void out(Class<?> clazz) {
		System.out.printf("%s.typeParameters=%s\n", clazz.getSimpleName(), Lists.newArrayList(clazz.getTypeParameters()));
		System.out.printf("%s.genericInterfaces=%s\n", clazz.getSimpleName(), Lists.newArrayList(clazz.getGenericInterfaces()));
		System.out.printf("%s.genericSuperclass=%s\n", clazz.getSimpleName(), Lists.newArrayList(clazz.getGenericSuperclass()));
	}
	public interface Car {
		void start();
		void stop();
	}	
	public abstract class ChargingStation<T extends Car> {
		private T car;
		private String name;
		private int capacity;
		private Queue<T> queue;

		public T getCar() {
			return car;
		}
		public void setCar(T car) {
			this.car = car;
		}		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getCapacity() {
			return capacity;
		}
		public void setCapacity(int capacity) {
			this.capacity = capacity;
		}		
		public Queue<T> getQueue() {
			return queue;
		}
		public void setQueue(Queue<T> queue) {
			this.queue = queue;
		}
		public abstract void charge();
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + capacity;
			result = prime * result + ((car == null) ? 0 : car.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		public boolean canEqual(Object obj) {
			return (obj instanceof ChargingStation);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ChargingStation other = (ChargingStation) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (capacity != other.capacity)
				return false;
			if (car == null) {
				if (other.car != null)
					return false;
			} else if (!car.equals(other.car))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		private SingleGenericTypeResolutionTest getOuterType() {
			return SingleGenericTypeResolutionTest.this;
		}		
	}
	public class Tesla implements Car {
		public void start() {			
		}
		public void stop() {
		}
	}
	public class ModelS extends Tesla {
		
	}
	public abstract class TeslaModelSpecificStation<T extends Car> extends ChargingStation<T> {
		
	}
	public class ModelSChargingStation extends TeslaModelSpecificStation<ModelS> {
		@Override
		public void charge() {
		}
	}
	public class TeslaChargingStation extends ChargingStation<Tesla> {
		@Override
		public void charge() {
		}
	}
	public class SolarTeslaChargingStation extends TeslaChargingStation {
		@Override
		public ModelS getCar() {
			return (ModelS)super.getCar();
		}
	}
}
