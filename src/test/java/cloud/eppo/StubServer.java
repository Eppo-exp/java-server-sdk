package cloud.eppo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Stub HTTP server for tests, on OkHttp MockWebServer. Stubs match GET requests by a regex over the
 * full path and query. The most recently added matching stub answers; no match gives 404.
 */
final class StubServer {
  private final MockWebServer server = new MockWebServer();
  private final List<Stub> stubs = new ArrayList<>();
  private final List<RecordedRequest> requests = new ArrayList<>();

  private static final class Stub {
    final Pattern url;
    final Predicate<RecordedRequest> condition;
    final Supplier<MockResponse> response;

    Stub(String urlRegex, Predicate<RecordedRequest> condition, Supplier<MockResponse> response) {
      this.url = Pattern.compile(urlRegex);
      this.condition = condition;
      this.response = response;
    }

    boolean matches(RecordedRequest request) {
      String path = request.getPath();
      return "GET".equals(request.getMethod())
          && path != null
          && url.matcher(path).matches()
          && condition.test(request);
    }
  }

  void start(int port) {
    server.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            synchronized (StubServer.this) {
              requests.add(request);
              for (int i = stubs.size() - 1; i >= 0; i--) {
                if (stubs.get(i).matches(request)) {
                  return stubs.get(i).response.get();
                }
              }
            }
            return new MockResponse().setResponseCode(404);
          }
        });
    try {
      server.start(port);
    } catch (IOException e) {
      throw new RuntimeException("Could not start the stub server on port " + port, e);
    }
  }

  void stop() {
    try {
      server.shutdown();
    } catch (IOException e) {
      throw new RuntimeException("Could not stop the stub server", e);
    }
  }

  synchronized void stub(String urlRegex, Supplier<MockResponse> response) {
    stub(urlRegex, request -> true, response);
  }

  synchronized void stub(
      String urlRegex, Predicate<RecordedRequest> condition, Supplier<MockResponse> response) {
    stubs.add(new Stub(urlRegex, condition, response));
  }

  /** Counts received GET requests that match, like WireMock's request journal. */
  synchronized int count(String urlRegex, Predicate<RecordedRequest> condition) {
    Stub filter = new Stub(urlRegex, condition, () -> null);
    int count = 0;
    for (RecordedRequest request : requests) {
      if (filter.matches(request)) {
        count++;
      }
    }
    return count;
  }

  synchronized void resetRequests() {
    requests.clear();
  }

  synchronized void resetAll() {
    stubs.clear();
    requests.clear();
  }

  static MockResponse okJson(String body) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body);
  }

  static MockResponse status(int code) {
    return new MockResponse().setResponseCode(code);
  }

  static Predicate<RecordedRequest> header(String name, String value) {
    return request -> request.getHeaders().values(name).contains(value);
  }

  static Predicate<RecordedRequest> noHeader(String name) {
    return request -> request.getHeader(name) == null;
  }

  /** Answers the first request with {@code first} and every later one with {@code rest}. */
  static Supplier<MockResponse> firstThen(
      Supplier<MockResponse> first, Supplier<MockResponse> rest) {
    AtomicBoolean used = new AtomicBoolean();
    return () -> used.getAndSet(true) ? rest.get() : first.get();
  }
}
