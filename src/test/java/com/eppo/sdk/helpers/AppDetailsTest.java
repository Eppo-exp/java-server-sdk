package com.eppo.sdk.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertTrue(appDetails.getVersion().matches("^\\d+\\.\\d+\\.\\d+"));
  }

  @Test
  public void testAppPropertyReadFailure() {
      ClassLoader mockClassloader = Mockito.mock(ClassLoader.class);
      Mockito.when(mockClassloader.getResourceAsStream("app.properties")).thenReturn(null);

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(mockClassloader);
      AppDetails.getInstance(); // Initialize with mock class loader
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    AppDetails appDetails = AppDetails.getInstance();
    assertEquals("java-server-sdk", appDetails.getName());
    assertEquals("1.0.0", appDetails.getVersion());
  }
}
