package com.eppo.sdk.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

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
    // Override the getResourceAsStream method to return the custom InputStream
    System.out.println(">>>> mocking stuff");
    ClassLoader mockClassLoader = Mockito.mock(ClassLoader.class);
    Mockito.when(mockClassLoader.getResourceAsStream("app.properties")).thenReturn(new InputStream() {
      @Override
      public int read() throws IOException {
        System.out.println(">>>> throwing intentional error");
        throw new IOException("Intentional Exception For Test");
      }
    });

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(mockClassLoader);
      System.out.println(">>>> get instance");
      AppDetails.getInstance();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    AppDetails appDetails = AppDetails.getInstance();
    assertEquals("java-server-sdk", appDetails.getName());
    assertEquals("1.0.0", appDetails.getVersion());
  }
}
