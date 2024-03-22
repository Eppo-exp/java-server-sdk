package com.eppo.sdk.helpers;

import com.eppo.sdk.EppoClient;
import com.eppo.sdk.dto.EppoAttributes;
import com.eppo.sdk.exception.ExperimentConfigurationNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

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

  /*
  @Test
  public void testAppPropertyReadFailure() {

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader spyCassLoader = spy(originalClassLoader);


      @Override
      public int read() throws IOException {
        System.out.println(">>>> throwing intentional error");
        throw new IOException("Intentional Exception For Test");
      }
    });


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
   */
}
