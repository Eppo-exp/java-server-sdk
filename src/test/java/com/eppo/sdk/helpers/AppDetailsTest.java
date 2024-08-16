package com.eppo.sdk.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AppDetailsTest {

  @BeforeEach
  public void nullOutInstanceToReset() {
    try {
      Class<?> appDetailsClass = Class.forName("com.eppo.sdk.helpers.AppDetails");
      Field instanceField = appDetailsClass.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testReadAppProperties() {
    AppDetails appDetails = AppDetails.getInstance();
    assertEquals("java-server-sdk", appDetails.getName());
    Pattern p = Pattern.compile("^\\d+\\.\\d+\\.\\d+");
    Matcher matcher = p.matcher(appDetails.getVersion());
    assertTrue(matcher.find());
  }

  @Test
  public void testAppPropertyReadFailure() {
    ClassLoader mockClassloader = Mockito.mock(ClassLoader.class);
    Mockito.when(mockClassloader.getResourceAsStream("filteredResources/app.properties"))
        .thenReturn(null);

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(mockClassloader);
      AppDetails.getInstance(); // Initialize with mock class loader
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    AppDetails appDetails = AppDetails.getInstance();
    assertEquals("java-server-sdk", appDetails.getName());
    assertEquals("3.0.0", appDetails.getVersion());
  }
}
