package net.elementj.tortilla;

import java.beans.PropertyDescriptor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.google.common.collect.Lists;

/**
 * Reflection utilities used by Tortilla.
 */
public class TortillaReflection {
	private TortillaReflection() { }
	/**
	 * Finds the inheritance path to a declaration of a generic type.
	 * @param start The starting point
	 * @param declaration The generics declaration
	 * @return A list of classes in order of inheritance from the start class to the location where the declaration is made.
	 */
    public static List<Class<?>> findPathToDeclaration(Class<?> start, GenericDeclaration declaration) {
    	List<Class<?>> lst=new LinkedList<Class<?>>();
    	Class<?> current=start;
    	boolean found=false;
		lst.add(current);		
		while (!(found=declaration.equals(current))&&current!=null) {
    		for (Class<?> iface : current.getInterfaces()) {
    			List<Class<?>> sub=findPathToDeclaration(iface,declaration);
    			if (sub.size()>0) {
    				lst.addAll(sub);
    				return lst;
    			}
    		}
    		current=current.getSuperclass();
    		lst.add(current);
    	} 
    	if (found) {
    		return lst;
    	} else {
    		return Collections.emptyList();
    	}
    }
    public static Class<?> getActualPropertyType(Class<?> parent, PropertyDescriptor descriptor) {
    	return getReturnType(parent,descriptor.getReadMethod());
    }
    private static int indexOf(TypeVariable<?>[] typeParameters,String name) {
    	for (int i=0; i<typeParameters.length; i++) {
    		if (name.equals(typeParameters[i].getName())) { return i; }
    	}
    	return -1;    	
    }
    /**
     * Gets the return type for a method in the given class, taking generics into account (as much is possible).
     * @param parent The class
     * @param method The method to which the class belongs.
     * @return The generic return type, if it can be identified.
     */
    public static Class<?> getReturnType(Class<?> parent, Method method) {
    	Type type=method.getGenericReturnType();
    	if (type instanceof TypeVariable) {
    		TypeVariable<?> var=(TypeVariable<?>)type;
    		final GenericDeclaration gendec=var.getGenericDeclaration();
    		
    		// find path from parent class to generic declaration
    		List<Class<?>> path=findPathToDeclaration(parent,gendec);
    		if (path.size()==0) { 
    			throw new IllegalArgumentException("Unable to resolve generic declaration path to "+type); 
			}
    		ListIterator<Class<?>> it=path.listIterator();
    		Class<?> current=null;
    		
    		// look for the generic declaration in the list iterator so we can find related classes in the list
    		while (it.hasNext()&&!gendec.equals(current=it.next()));
    		if (!gendec.equals(current)) { throw new RuntimeException("Generic declaration not found in the list hierarchy."); }
    		int paramIndex=indexOf(current.getTypeParameters(),var.getName());
    		int paramCount=current.getTypeParameters().length;
    		
    		do {
        		Type currentType=current.getGenericSuperclass();
	    		// if the variable wasn't found in the generic declaration then we can't proceed
	    		// if subclasses don't have the variable that could be okay
	    		if (gendec.equals(current)&&paramIndex<0) { 
	    			throw new RuntimeException("Unable to find index of generic parameter "+var.getName()+" in generic declaration."); 
				}
	    		assert currentType!=null;
	    		if (currentType instanceof ParameterizedType) {
	    			ParameterizedType pt=(ParameterizedType)currentType;
	    			assert pt.getActualTypeArguments().length==paramCount;
	    			Type actual=pt.getActualTypeArguments()[paramIndex];
	    			if (actual instanceof Class<?>) { return (Class<?>)actual; }
	    			if (actual instanceof TypeVariable) {
	    				var=(TypeVariable<?>)actual;
	    				paramIndex=indexOf(current.getTypeParameters(),var.getName());
	    				paramCount=current.getTypeParameters().length;
    				}
	    		}
    		} while (it.hasPrevious() && (current=it.previous())!=null);
    		// if nothing was found it's likely this is a wildcard type, so return the base type of the wildcard
    		return method.getReturnType();
    	} else if (type instanceof Class) {
    		return (Class<?>)type;
    	} else if (type instanceof ParameterizedType) {
    		ParameterizedType var=(ParameterizedType)type;
    		return (Class<?>)var.getRawType();
    	}
    	throw new IllegalArgumentException("Unknown type "+type+" implements "+Lists.newArrayList(type.getClass().getInterfaces()));
    }
}
