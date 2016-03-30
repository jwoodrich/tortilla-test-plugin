package net.elementj.tortilla;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

/**
 * Common test utility methods.
 */
public class TestUtil {
	public static Map<String,PropertyDescriptor> map(PropertyDescriptor[] pds) {
		Map<String,PropertyDescriptor> ret=new HashMap<String,PropertyDescriptor>();
		for (PropertyDescriptor pd : pds) {
			ret.put(pd.getName(), pd);
		}
		return ret;
	}
	private TestUtil() { }
}
