package com.xuggle.ferry;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

/**
 * Tests for the Xuggler "no install" system.  This application MUST
 * be set to point to a DLL or Shared Object (unpacked) on the disk
 * to completely work.  It checks that "no-install" of the full install
 * works, and that we still revert back to on-disk loading if we cannot
 * find native code in the jar.
 * 
 * Other key note: This package MUST NOT depend on xuggle-xuggler itself
 * otherwise the tests will fail because the root classloader will load everything.
 * @author aclarke
 *
 */
public class JNILibraryExhaustiveTest
{
  
//  private static final String MAVEN_BASE="file://"+System.getProperty("user.home")+"/.m2/repository";
  private static final String MAVEN_BASE="http://xuggle.googlecode.com/svn/trunk/repo/share/java";
  private static final String MAVEN_GROUP="xuggle";
  private static final String MAVEN_ARTIFACT="xuggle-xuggler";
//  private static final String MAVEN_VERSION="5.5-SNAPSHOT";
  private static final String MAVEN_VERSION="5.4";
  
  private static final String XUGGLE_CLASS="com.xuggle.ferry.Ferry";
  private static final String XUGGLE_METHOD="init";

  public Class load(ClassLoader loader) throws Exception {
    // This should fail if the non-classpath loader is used.
    final Class clazz = loader.loadClass(XUGGLE_CLASS);
    @SuppressWarnings("unchecked")
    final Method method = clazz.getMethod(XUGGLE_METHOD, (Class[])null);
    method.invoke(null);
    return clazz;
    
  }
  public Class testURLLoad(String jarFile) throws Exception
  {
    final URLClassLoader loader = URLClassLoader.newInstance(new URL[]{new URL(jarFile)});
    return load(loader);
  }

  private static String getMavenURL(String artifact) {
    return getMavenURL(artifact, MAVEN_VERSION);
  }
  private static String getMavenURL(String artifact, String version)
  {
    return getMavenURL(MAVEN_GROUP, artifact, version);
  }
  private static String getMavenURL(String group, String artifact, String version)
  {
    return getMavenURL(group, artifact, version, artifact);
  }
  public static String getMavenURL(String group, String artifact, String version, String file)
  {
    return MAVEN_BASE+"/"+group+"/"+artifact+"/"+version+"/"+file+"-"+version+".jar";
  }
  @Test
  public void testLoadLibrariesInMultipleClassloadersFromOneVMFromMangledFile() throws Exception
  {
    // should succeed with bundled jar, and we keep it around
    final Class a = new JNILibraryExhaustiveTest().testURLLoad(getMavenURL(MAVEN_ARTIFACT));
    // should succeed because default xuggler loads from classpath with mangled names
    final Class b = new JNILibraryExhaustiveTest().testURLLoad(getMavenURL(MAVEN_ARTIFACT));
    // names of classes should be the same, but they should not be equal if they came from
    // separate class loaders
    assertEquals(XUGGLE_CLASS, a.getCanonicalName());
    assertEquals(XUGGLE_CLASS, b.getCanonicalName());
    assertNotSame("this test MUST NOT depend at compile time on the xuggler class in order to be valid", a, this.getClass().getClassLoader());
    assertNotSame(a, b);
  }

  @Test
  public void testLoadLibrariesInMultipleClassloadersFromOneVMFromSameFile() throws Exception
  {
    // should succeed with bundled jar, and we keep it around
    final Class a = new JNILibraryExhaustiveTest().testURLLoad(getMavenURL(MAVEN_GROUP, MAVEN_ARTIFACT, MAVEN_VERSION, "xuggle-xuggler-noarch"));
    // should fail if loaded directly from disk since it was loaded in other class loader
    try {
      final Class b = new JNILibraryExhaustiveTest().testURLLoad(getMavenURL(MAVEN_GROUP, MAVEN_ARTIFACT, MAVEN_VERSION, "xuggle-xuggler-noarch"));
      fail("second load should fail because first load added the native library (without name mangling) into the other classloader: " + b.getCanonicalName());
    } catch (UnsatisfiedLinkError e) {
      // if we get here the test passed because the second load should not succeed if loading directly
      // from files on disk.
    }
    assertNotSame("this test MUST NOT depend at compile time on the xuggler class in order to be valid", a, this.getClass().getClassLoader());
  }

}
