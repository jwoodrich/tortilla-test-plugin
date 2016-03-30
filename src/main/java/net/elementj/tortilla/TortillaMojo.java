package net.elementj.tortilla;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Goal which generates unit tests for Java Beans.
 * @requiresDependencyResolution
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution=ResolutionScope.TEST )
public class TortillaMojo
    extends AbstractMojo
{
    private static final String[] IMPORTS={"org.junit.Test","org.junit.Before","nl.jqno.equalsverifier.EqualsVerifier","static org.junit.Assert.*"};
    private static final int MAX_PARENT=30;
	private static final String DEFAULT_BASE="net.elementj.tortilla.TortillaBase";
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}/generated-test-sources/tortilla", property = "outputDir", required = true )
    private File outputDirectory;
    @Parameter( defaultValue = "${project.build.directory}/classes", property = "sourceDir", required = true )
    private File sourceDirectory;
    @Parameter( defaultValue = DEFAULT_BASE, property="baseClass", required=true)
    private String baseClass;
    @Parameter( alias = "includes", required=false)
    private String[] includes;
    @Parameter( alias = "excludes", required=false)
    private String[] excludes;
    @Parameter( alias = "superExcludes", required=false)
    private String[] superExcludes={"java.lang.Exception"};
    private Class<?>[] superExcludesClasses;
    @Parameter( alias="equalsVerifierEnabled", required=true, defaultValue="true")
    private boolean equalsVerifier;
    @Parameter( alias="equalsVerifierParams", required=true, defaultValue=".suppress(nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS,nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE)")
    private String equalsVerifierParameters;
    @Parameter( alias="equalsVerifierExcludes", required=false)
    private String[] equalsVerifierExcludes;
    @Parameter( alias="equalsVerifierIncludes", required=false)
    private String[] equalsVerifierIncludes;
    @Parameter( alias="allowNoNullary", required=true, defaultValue="false")
    private boolean allowNoNullary;
    @Component
    private MavenProject project;
    private ClassLoader loader;
    /**
     * Scan for beans to generate test cases for.
     * @param classes A set to load found classes into.
     * @param path The path to scan for classes.
     */
    private void scanForBeans(Set<Class<?>> classes, File path) {
    	getLog().debug("scanForBeans("+classes+","+path+");");
    	String[] files=path.list();
    	for (String filename : files) {
    		File f=new File(path,filename);
    		if (f.isDirectory()) { scanForBeans(classes,f); }
    		else if (ClassesOnlyFilter.INSTANCE.accept(path, filename)) {
    			String className=determineClassName(path, filename);
				try {
					getLog().debug("Attempting to load "+className);
					Class<?> clazz=Class.forName(className,false,loader);
					if (accepted(excludes,superExcludesClasses,includes,clazz)) { 
						classes.add(clazz); 
					}
				} catch (ClassNotFoundException e) {					
					getLog().error("Couldn't load "+className, e);
				}
    		}
    	}
    }
    /**
     * Determine if a bean property is acceptable for testing according to exclusion rules
     * and the requirement that the property be read/write.
     * @param pd The property descriptor
     * @return true if the property is acceptable for testing, otherwise false
     */
    private boolean accepted(PropertyDescriptor pd) {
    	if (pd.getReadMethod()==null||pd.getWriteMethod()==null) { return false; }
    	String className=pd.getReadMethod().getDeclaringClass().getName(), propertyName=pd.getName();
    	if (excludes!=null) {
    		for (String exclude : excludes) {
    			int idx=exclude.lastIndexOf('.');
    			if (idx<0) {
    				getLog().warn("Excluded method "+exclude+" is invalid.  Expected format: is package.name.BeanClassName or package.name.BeanClassName.propertyName");
    				continue;
    			}
    			String excludeClass=exclude.substring(0, idx);
    			String excludeProperty=exclude.substring(idx+1);
    			if (FilenameUtils.wildcardMatch(className, excludeClass, IOCase.SENSITIVE)&&
    				FilenameUtils.wildcardMatch(propertyName, excludeProperty, IOCase.SENSITIVE)) { return false; }
    		}
    	}
    	return true;
    }
    /**
     * Determine if a java bean is acceptable for testing.
     * @param excludes Class/package specifications to exclude.
     * @param superExcludesClasses An array of classes whose subclasses should be excluded from test case generation. 
     * @param includes Class/package specifications to explicitly include.
     * @param clazz The class to evaluate for acceptance.
     * @return true if the class should be included, otherwise false
     */
    private static boolean accepted(String[] excludes, Class<?>[] superExcludesClasses, String[] includes, Class<?> clazz) {
    	final String name=clazz.getName();
    	if (excludes!=null) {
    		for (String exclude : excludes) {
    			if (FilenameUtils.wildcardMatch(name, exclude, IOCase.SENSITIVE)) {
    				return false;
    			}
    		}
    	}    	
    	if (superExcludesClasses!=null) {
    		for (Class<?> exclude : superExcludesClasses) {
    			if (exclude.isAssignableFrom(clazz)) { return false; }
    		}
    	}
    	if (includes!=null) {
    		for (String include : includes) {
    			if (FilenameUtils.wildcardMatch(name, include, IOCase.SENSITIVE)) {
    				return true;
    			}
    		}
    		return false;
    	} 
    	
		return true;
    }
    /**
     * Determine the name of a class.
     * @param path The directory in which the class file resides.
     * @param filename The class filename.
     * @return The full class name.
     */
    private String determineClassName(File path, String filename) {
    	int count=0;
		StringBuilder sb=new StringBuilder();
		Stack<String> stack=new Stack<String>();
		String className=filename.substring(0,filename.length()-6); // remove .class from the end 
		stack.add(className);
		do {
			stack.add(path.getName());			
		} while ((path=path.getParentFile())!=null &&
				 !sourceDirectory.equals(path) && 
				 ++count<MAX_PARENT);

		sb.append(stack.pop());
		
		while (!stack.isEmpty()) {
			sb.append(".");
			sb.append(stack.pop());
		}
		return sb.toString();
    }
    /**
     * A filter that only accepts class files.
     */
    private static class ClassesOnlyFilter implements FilenameFilter {
    	public static final ClassesOnlyFilter INSTANCE=new ClassesOnlyFilter();
    	public boolean accept(File dir, String name) {
    		return name.endsWith(".class")||name.endsWith(".CLASS");
    	}
    }
    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
    	Set<Class<?>> classes=new HashSet<Class<?>>();
    	
        File f = outputDirectory;
        if ( !f.exists() )
        {
            f.mkdirs();
        }
        if (sourceDirectory.exists()) {
        	try {
				configureClassLoader();
			} catch (DependencyResolutionRequiredException e) {
				throw new MojoExecutionException("Unable to configure ClassLoader!",e);
			}
        	if (superExcludes!=null) {
        		initSuperExcludes();
        	}
        	getLog().info("Scanning for beans deserving of test cases ...");
        	scanForBeans(classes, sourceDirectory);
        	for (Class<?> clazz : classes) {
        		try { generateTestCase(clazz); }
        		catch (IntrospectionException e) {
        			getLog().error("Failed to generate test case for "+clazz.getName(),e);
        		}
        		catch (Exception e) {
        			throw new MojoExecutionException("Failed to generate test case for "+clazz.getName(),e);
        		}
        	}
    		try { copySource(TortillaBase.class.getName()); }
    		catch (IOException e) {
    			throw new MojoExecutionException("Failed to copy base class "+baseClass+" into "+outputDirectory,e);
    		}
        } else {
        	throw new MojoExecutionException("Build directory does not exist.");
        }
    }
    /**
     * Initialize the superclass excludes.
     */
    private void initSuperExcludes() {
		Set<Class<?>> set=new HashSet<Class<?>>();
		for (String exclude : superExcludes) {
			try {
    			set.add(Class.forName(exclude,false,loader));
			} catch (ClassNotFoundException e) {
				getLog().warn("Exclusion class "+exclude+" does not exist.");
			}
		}
		superExcludesClasses=set.toArray(new Class<?>[set.size()]);
	}
    /**
     * Copy a source file from the classpath into the outputDirectory.
     * @param className The full name of the class.
     * @throws IOException If an IO error occurs while reading or writing the file.
     */
	private void copySource(String className) throws IOException {
    	String filename=className.replace('.','/')+".java";
    	InputStream is=getClass().getClassLoader().getResourceAsStream(filename);
    	if (is!=null) {
    		byte[] buffer=new byte[4096];
    		int count, idx=filename.lastIndexOf('/');
    		File dir=idx>0?new File(outputDirectory,filename.substring(0, idx)):outputDirectory;
			if (!dir.exists()) { dir.mkdirs(); }
			File file=new File(dir,idx>0?filename.substring(idx+1):filename);
			OutputStream os=new FileOutputStream(file);
    		while ((count=is.read(buffer))>0) {
    			os.write(buffer, 0, count);
    		}
    		os.close();
    	}
    }
	/**
	 * Parses and adds a collection of Strings to a collection of URLs.
	 * @param lst The collection of URLs to load.
	 * @param elements The collection of strings to parse.
	 * @throws MojoExecutionException If one of the URLs cannot be parsed.
	 */
    private void addAll(Collection<URL> lst,Collection<String> elements) throws MojoExecutionException {
    	getLog().debug("Loading elements into list: "+elements);
    	for (String element : elements) {
    		File f=new File(element);
    		if (f.exists()) {
        		try { 
        			lst.add(f.toURI().toURL());
        		} catch (MalformedURLException e) {
        			throw new MojoExecutionException("Failed to generate URL for classpath element "+element,e);
        		}
    		} else {
    			getLog().warn("Element "+element+" is a listed classpath element, but could not be found.");
    		}
    	}    	
    }
    /**
     * Instantiates and configures a ClassLoader to include elements from the test case classpath for the Maven proejct.
     * @throws MojoExecutionException
     * @throws DependencyResolutionRequiredException
     */
    @SuppressWarnings("unchecked")
	private void configureClassLoader() throws MojoExecutionException, DependencyResolutionRequiredException {
    	Collection<URL> urls=new LinkedList<URL>();
		try { 
	    	urls.add(sourceDirectory.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new MojoExecutionException("Failed to generate URL for source directory!",e);
		}
    	addAll(urls,project.getTestClasspathElements());
    	getLog().debug("Generated classpath of "+urls);
		loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
    }
    /**
     * Generate a test case for a class.
     * @param clazz The class to generate the test case for.
     * @throws IOException If the test case cannot be written.
     * @throws IntrospectionException If the bean cannot be introspected.
     */
    private void generateTestCase(Class<?> clazz) throws IOException, IntrospectionException {
    	if ((clazz.getModifiers()&(Modifier.ABSTRACT|Modifier.INTERFACE))>0) {
    		getLog().debug("Test case will not be generated for "+clazz.getName()+" as it is not an implementation class.");
    		return;
    	}
    	if (clazz.isEnum()||Enum.class.isAssignableFrom(clazz)) {
    		getLog().debug("Test case will not be generated for "+clazz.getName()+" as it is an enum.");
    	}
    	if (!Modifier.isPublic(clazz.getModifiers())) {
    		getLog().info("Test case will not be generated for "+clazz.getName()+" as it is not public.");
    		return;
    	}
    	try { clazz.getConstructor(); }
    	catch (NoSuchMethodException e) {
    		if (!allowNoNullary) {
    			getLog().info("Test case will not be generated for "+clazz.getName()+" as it does not have a nullary constructor.");
        		return;
    		}
    	}
    	Set<PropertyDescriptor> descriptors=new HashSet<PropertyDescriptor>();
		for (PropertyDescriptor d : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
			descriptors.add(d);
		}
		if (descriptors.size()==0) {
			getLog().debug("No property descriptors for "+clazz.getName());
			return;
		}
    	String pkg=clazz.getPackage().getName().replace('.','/');
    	File dir=new File(outputDirectory,pkg);
    	if (!dir.exists()) { 
    		if (dir.mkdirs()) {
    			getLog().debug("Created directory "+dir.getPath());
    		} else {
    			getLog().error("Failed to create directory "+dir.getPath());
    			throw new IOException("Failed to create directory "+dir.getPath());
    		}
    	}
    	String beanClass=clazz.getName().replace('$', '.');
    	String className=clazz.getSimpleName()+"BeanTest";
    	final File file=new File(dir,className+".java");
    	OutputStream os=new FileOutputStream(file);
    	Writer writer=new BufferedWriter(new OutputStreamWriter(os));
    	try {
	    	writer.append("package ");
	    	writer.append(clazz.getPackage().getName());
	    	writer.append(";\n\n");
	    	for (String in : IMPORTS) {
	    		writer.append("import ");
	    		writer.append(in);
	    		writer.append(";\n");
	    	}
	    	writer.append("\npublic class ");
	    	writer.append(className);
	    	writer.append(" extends ");
	    	writer.append(baseClass);
	    	writer.append(" {\n");
	    	writer.append("\t");
	    	writer.append(beanClass);
	    	writer.append(" instance;\n");
			writer.append("\n\t@Before\n");
			writer.append("\tpublic void setUp() throws Exception {\n");
			writer.append("\t\tinstance=new ");
			writer.append(beanClass);
			writer.append("();\n\t}\n\n");
			for (PropertyDescriptor d : descriptors) {
				if (accepted(d)) {
					generateReadWriteTest(writer,clazz,d);
				} else {
					getLog().debug("Property "+clazz.getName()+"."+d.getName()+" was not accepted, skipping.");
				}
			}
			if (equalsVerifier && accepted(equalsVerifierExcludes,null,equalsVerifierIncludes,clazz)) {
				generateEqualsContractTest(writer,clazz);
			}
	    	writer.append("}\n");
    	} finally {
    		writer.close();
    	}
    }
    /**
     * Generates an equals contract test using jqno.nl's equalsverifier.
     * @param writer The writer to write to.
     * @param beanClass The class to write the test for.
     * @throws IOException If an IO error occurs.
     */
    private void generateEqualsContractTest(Writer writer, Class<?> beanClass) throws IOException {
		writer.append("\t@Test\n");
		writer.append("\tpublic void equalsContract() throws Exception {\n\t\tEqualsVerifier.forClass(");
		writer.append(beanClass.getName());
		writer.append(".class)");
		if (hasRedefinedSuperclass(beanClass)) {
			writer.append(".withRedefinedSuperclass()");
		}
		if (equalsVerifierParameters!=null) {
			writer.append(equalsVerifierParameters);
		}
		writer.append(".verify();\n");
		writer.append("\t}\n\n");
    }
    /**
     * Determine if a class has a redefined equals method.  This is useful for
     * configuring the jqno.nl equals verifier.
     * @param clazz The class to evaluate
     * @return true if any of the superclasses overrode equals
     */
    private static boolean hasRedefinedSuperclass(final Class<?> clazz) {
    	int count=0;
    	Class<?> c=clazz;
    	// count overrides for equals
    	do {
    		try { c.getDeclaredMethod("equals", Object.class); count++; }
    		catch (NoSuchMethodException e) { }
    	} while ((c=c.getSuperclass())!=Object.class);
    	if (count>0) { return true; }
    	// count overrides for hashCode
    	c=clazz;
    	do {
    		try { c.getDeclaredMethod("hashCode"); count++; }
    		catch (NoSuchMethodException e) { }
    	} while ((c=c.getSuperclass())!=Object.class);
    	return count>0;
    }
    /**
     * Generate a JUnit read/write test for a bean.
     * @param writer The write to write to.
     * @param beanClass The bean to generate the test for.
     * @param descriptor The descriptor of the property to generate the test for.
     * @throws IOException If an IO error occurs while writing the test.
     */
    private static void generateReadWriteTest(Writer writer, Class<?> beanClass, PropertyDescriptor descriptor) throws IOException {
    	String propertyType;
    	Class<?> propertyTypeClass=TortillaReflection.getActualPropertyType(beanClass, descriptor);
    	if (!propertyTypeClass.isArray()) {
    		propertyType=propertyTypeClass.getName().replace('$', '.');
    	} else {
    		propertyType=propertyTypeClass.getComponentType().getName().replace('$', '.')+"[]";
    	}
		writer.append("\t@Test\n");
		writer.append("\tpublic void test");
		writer.append(upperFirstChar(descriptor.getName()));
		writer.append("RW() throws Exception {\n");
		writer.append("\t\tfinal ");
    	writer.append(propertyType);
    	writer.append(" expected=create(");
    	writer.append(propertyType);
    	writer.append(".class);\n\n");
    	if (!propertyTypeClass.isPrimitive()) {
        	writer.append("\t\tinstance.");
        	writer.append(descriptor.getWriteMethod().getName());
        	writer.append("((");
        	writer.append(propertyType);
        	writer.append(")null);\n");
	    	writer.append("\t\tassertEquals(\"Expected null returned from ");
	    	writer.append(descriptor.getReadMethod().toGenericString());
	    	writer.append("\",null,instance.");
	    	writer.append(descriptor.getReadMethod().getName());
	    	writer.append("());\n");
    	}
    	writer.append("\t\tinstance.");
    	writer.append(descriptor.getWriteMethod().getName());
    	writer.append("(expected);\n\t\t");
    	if (!propertyTypeClass.isArray()) {
	    	writer.append("assertEquals(\"Unexpected response from ");
	    	writer.append(descriptor.getReadMethod().toGenericString());
	    	writer.append("\",expected,instance.");
	    	writer.append(descriptor.getReadMethod().getName());
	    	writer.append("());\n");
    	} else {
	    	writer.append("assertArrayEquals(\"");
	    	writer.append(descriptor.getName());
	    	writer.append("\",expected,instance.");
	    	writer.append(descriptor.getReadMethod().getName());
	    	writer.append("());\n");
    	}
    	writer.append("\t}\n\n");
    }
    /**
     * Returns a copy of a string with the first character in upper case.
     * @param str The string to copy.
     * @return The copied string.
     */
    private static String upperFirstChar(String str) {
    	if (str==null) { return null; }
    	if (str.length()==0) { return str; }
		char[] prop=str.toCharArray();
		prop[0]=Character.toUpperCase(prop[0]);
		return new String(prop); 
    }
}
