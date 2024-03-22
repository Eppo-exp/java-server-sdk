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
    InputStream throwingInputStream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("Intentional Exception For Test");
      }
    };

    // Override the getResourceAsStream method to return the custom InputStream
    ClassLoader mockClassLoader = Mockito.mock(ClassLoader.class);
    Mockito.when(mockClassLoader.getResourceAsStream("app.properties")).thenReturn(throwingInputStream);

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(mockClassLoader);
      AppDetails.getInstance();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    AppDetails appDetails = AppDetails.getInstance();
    assertEquals("java-server-sdk", appDetails.getName());
    assertEquals("1.0.0", appDetails.getVersion());
  }
}
