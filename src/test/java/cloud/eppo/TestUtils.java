package cloud.eppo;

import cloud.eppo.helpers.JacksonJsonDeserializer;
import java.lang.reflect.Field;

public class TestUtils {

  static {
    Utils.setJsonDeserializer(new JacksonJsonDeserializer());
  }

  @SuppressWarnings("SameParameterValue")
  public static cloud.eppo.helpers.TestUtils.MockHttpClient mockHttpResponse(String responseBody) {
    cloud.eppo.helpers.TestUtils.MockHttpClient mockHttpClient =
        new cloud.eppo.helpers.TestUtils.MockHttpClient(responseBody.getBytes());

    setBaseClientHttpClientOverrideField(mockHttpClient);
    return mockHttpClient;
  }

  public static void mockHttpError() {
    setBaseClientHttpClientOverrideField(new cloud.eppo.helpers.TestUtils.ThrowingHttpClient());
  }

  public static void setBaseClientHttpClientOverrideField(IEppoHttpClient httpClient) {
    setBaseClientOverrideField("httpClientOverride", httpClient);
  }

  /** Uses reflection to set a static override field used for tests (e.g., httpClientOverride) */
  @SuppressWarnings("SameParameterValue")
  public static <T> void setBaseClientOverrideField(String fieldName, T override) {
    try {
      Field httpClientOverrideField = BaseEppoClient.class.getDeclaredField(fieldName);
      httpClientOverrideField.setAccessible(true);
      httpClientOverrideField.set(null, override);
      httpClientOverrideField.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static class MockHttpClient extends cloud.eppo.helpers.TestUtils.DelayedHttpClient {
    public MockHttpClient(byte[] responseBody) {
      super(responseBody);
      flush();
    }

    public void changeResponse(byte[] responseBody) {
      this.responseBody = responseBody;
    }
  }

  public static class ThrowingHttpClient implements IEppoHttpClient {

    @Override
    public byte[] get(String path) {
      throw new RuntimeException("Intentional Error");
    }

    @Override
    public void getAsync(String path, Callback callback) {
      callback.onFailure(new RuntimeException("Intentional Error"));
    }
  }

  public static class DelayedHttpClient implements IEppoHttpClient {
    protected byte[] responseBody;
    private Callback callback;
    private boolean flushed = false;
    private Throwable error = null;

    public DelayedHttpClient(byte[] responseBody) {
      this.responseBody = responseBody;
    }

    @Override
    public byte[] get(String path) {
      return responseBody;
    }

    @Override
    public void getAsync(String path, Callback callback) {
      if (flushed) {
        callback.onSuccess(responseBody);
      } else if (error != null) {
        callback.onFailure(error);
      } else {
        this.callback = callback;
      }
    }

    public void fail(Throwable error) {
      this.error = error;
      if (this.callback != null) {
        this.callback.onFailure(error);
      }
    }

    public void flush() {
      flushed = true;
      if (callback != null) {
        callback.onSuccess(responseBody);
      }
    }
  }
}
