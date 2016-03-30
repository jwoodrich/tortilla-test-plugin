# tortilla-test-plugin
Tortilla is a Maven plugin that dynamically generates JUnit test cases for Java Beans, including equals and hashCode verification.
As you might guess by the version number it's pretty new, but it's been really useful for expanding test coverage at work
without wasting anyone's time on monotonous unit tests for Java Beans.

Feedback and code contributions are welcome!

Created by: Jason D. Woodrich, https://github.com/jwoodrich/tortilla-test-plugin

## Usage

I haven't had a chance to publish this into Maven Central as of yet.  For now, clone this repository and do a "mvn clean install" 
to install the plugin to your local repository.  Next you'll want to add the plugin to your pom.xml as follows:

```<project>
  <build>
    <plugins>
      <plugin>
        <groupId>net.elementj</groupId>
        <artifactId>tortilla-test-plugin</artifactId>
        <version>0.1-SNAPSHOT</version>
        <configuration>
          ... see below for configuration options
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
## Configuration Options
```outputDir - The directory to which the generated sources are written.  Default: ${project.build.directory}/generated-test-sources/tortilla
sourceDir - The directory containing the Java Beans that should be evaluated for test case generation.  Default: ${project.build.directory}/classes
baseClass - The full class name of the base class to extend for test cases.
includes - A collection of class/package specs that should be evaluated for inclusion in test case generation.
excludes - A collection of class/package specs that should be evaluated for exclusion in test case generation.
superExcludes - Classes whose descendents should be excluded from test case generation.
equalsVerifierEnabled - true to enable equals verifier test cases, otherwise false
equalsVerifierParams - parameters to pass to equals verifier when generating the test case.  Default: .suppress(nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS,nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE)
equalsVerifierExcludes - A collection of class/package specs that should be evaluated for exclusion for equals verifier test case generation.
equalsVerifierIncludes - A collection of class/package specs that should be evaluated for inclusion for equals verifier test case generation.
allowNoNullary - true to allow testing of classes without nullary constructors.  default: false
```

Class/package specifications can be explicit class names, such as java.lang.Object, or can contain wildcards, such as java.lang.*.  Wildcards are evaluated according to the rules from Apache Commons IO [FilenameUtils](https://commons.apache.org/proper/commons-io/javadocs/api-1.4/org/apache/commons/io/FilenameUtils.html#wildcardMatch%28java.lang.String,%20java.lang.String%29).

Exclusions are evaluated first, meaning if a class matches both an inclusion and an exclusion the exclusion takes priority.
