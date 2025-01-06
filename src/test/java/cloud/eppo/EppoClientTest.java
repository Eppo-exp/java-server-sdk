package cloud.eppo;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static cloud.eppo.helpers.BanditTestCase.parseBanditTestCaseFile;
import static cloud.eppo.helpers.BanditTestCase.runBanditTestCase;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.BanditActions;
import cloud.eppo.api.BanditResult;
import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.helpers.BanditTestCase;
import cloud.eppo.helpers.TestUtils;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@ExtendWith(WireMockExtension.class)
public class EppoClientTest {

  private static final int TEST_PORT = 4001;
  private static final String TEST_HOST = "http://localhost:" + TEST_PORT;
  private static WireMockServer mockServer;

  private static final String DUMMY_FLAG_API_KEY = "dummy-flags-api-key"; // Will load flags-v1
  private static final String DUMMY_BANDIT_API_KEY =
      "dummy-bandits-api-key"; // Will load bandit-flags-v1
  private AssignmentLogger mockAssignmentLogger;
  private BanditLogger mockBanditLogger;

  @BeforeAll
  public static void initMockServer() {
    mockServer = new WireMockServer(TEST_PORT);
    mockServer.start();

    // If we get the dummy flag API key, return flags-v1.json
    String ufcFlagsResponseJson = readConfig("src/test/resources/shared/ufc/flags-v1.json");
    mockServer.stubFor(
        WireMock.get(
                WireMock.urlMatching(
                    ".*flag-config/v1/config\\?.*apiKey=" + DUMMY_FLAG_API_KEY + ".*"))
            .willReturn(WireMock.okJson(ufcFlagsResponseJson)));

    // If we get the dummy bandit API key, return bandit-flags-v1.json
    String banditFlagsResponseJson =
        readConfig("src/test/resources/shared/ufc/bandit-flags-v1.json");
    mockServer.stubFor(
        WireMock.get(
                WireMock.urlMatching(
                    ".*flag-config/v1/config\\?.*apiKey=" + DUMMY_BANDIT_API_KEY + ".*"))
            .willReturn(WireMock.okJson(banditFlagsResponseJson)));

    // Return bandit models (no need to switch on API key)
    String banditModelsResponseJson =
        readConfig("src/test/resources/shared/ufc/bandit-models-v1.json");
    mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*flag-config/v1/bandits\\?.*"))
            .willReturn(WireMock.okJson(banditModelsResponseJson)));
  }

  private static String readConfig(String jsonToReturnFilePath) {
    File mockResponseFile = new File(jsonToReturnFilePath);
    try {
      return FileUtils.readFileToString(mockResponseFile, "UTF8");
    } catch (Exception e) {
      throw new RuntimeException("Error reading mock data: " + e.getMessage(), e);
    }
  }

  @AfterEach
  public void cleanUp() {
    TestUtils.setBaseClientHttpClientOverrideField(null);
    EppoClient.stopPolling();
  }

  @AfterAll
  public static void tearDown() {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testUnobfuscatedAssignments(File testFile) {
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    EppoClient eppoClient = initClient(DUMMY_FLAG_API_KEY);
    runTestCase(testCase, eppoClient);
  }

  private static Stream<Arguments> getAssignmentTestData() {
    return AssignmentTestCase.getAssignmentTestData();
  }

  @ParameterizedTest
  @MethodSource("getBanditTestData")
  public void testUnobfuscatedBanditAssignments(File testFile) {
    BanditTestCase testCase = parseBanditTestCaseFile(testFile);
    EppoClient eppoClient = initClient(DUMMY_BANDIT_API_KEY);
    runBanditTestCase(testCase, eppoClient);
  }

  private static Stream<Arguments> getBanditTestData() {
    return BanditTestCase.getBanditTestData();
  }

  @SuppressWarnings("ExtractMethodRecommender")
  @Test
  public void testLoggers() {
    EppoClient eppoClient = initClient(DUMMY_BANDIT_API_KEY);

    String flagKey = "banner_bandit_flag";
    String subjectKey = "bob";
    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("age", 25);
    subjectAttributes.put("country", "USA");
    subjectAttributes.put("gender_identity", "female");

    BanditActions actions = new BanditActions();

    Attributes nikeAttributes = new Attributes();
    nikeAttributes.put("brand_affinity", 1.5);
    nikeAttributes.put("loyalty_tier", "silver");
    actions.put("nike", nikeAttributes);

    Attributes adidasAttributes = new Attributes();
    adidasAttributes.put("brand_affinity", -1.0);
    adidasAttributes.put("loyalty_tier", "bronze");
    actions.put("adidas", adidasAttributes);

    Attributes rebookAttributes = new Attributes();
    rebookAttributes.put("brand_affinity", 0.5);
    rebookAttributes.put("loyalty_tier", "gold");
    actions.put("reebok", rebookAttributes);

    BanditResult banditResult =
        eppoClient.getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "control");

    // Verify assignment
    assertEquals("banner_bandit", banditResult.getVariation());
    assertEquals("adidas", banditResult.getAction());

    // Verify experiment assignment logger called
    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());

    // Verify bandit logger called
    ArgumentCaptor<BanditAssignment> banditLogCaptor =
        ArgumentCaptor.forClass(BanditAssignment.class);
    verify(mockBanditLogger, times(1)).logBanditAssignment(banditLogCaptor.capture());
  }

  @Test
  public void getInstanceWhenUninitialized() {
    uninitClient();
    assertThrows(RuntimeException.class, EppoClient::getInstance);
  }

  @Test
  public void testErrorGracefulModeOn() {
    initBuggyClient();
    EppoClient.getInstance().setIsGracefulFailureMode(true);
    assertEquals(
        1.234, EppoClient.getInstance().getDoubleAssignment("numeric_flag", "subject1", 1.234));
  }

  @Test
  public void testErrorGracefulModeOff() {
    initBuggyClient();
    EppoClient.getInstance().setIsGracefulFailureMode(false);
    assertThrows(
        Exception.class,
        () -> EppoClient.getInstance().getDoubleAssignment("numeric_flag", "subject1", 1.234));
  }

  @Test
  public void testReinitializeWithoutForcing() {
    EppoClient firstInstance = initClient(DUMMY_FLAG_API_KEY);
    EppoClient secondInstance = new EppoClient.Builder().apiKey(DUMMY_FLAG_API_KEY).buildAndInit();

    assertSame(firstInstance, secondInstance);
  }

  @Test
  public void testReinitializeWitForcing() {
    EppoClient firstInstance = initClient(DUMMY_FLAG_API_KEY);
    EppoClient secondInstance =
        new EppoClient.Builder().apiKey(DUMMY_FLAG_API_KEY).forceReinitialize(true).buildAndInit();

    assertNotSame(firstInstance, secondInstance);
  }

  @Test
  public void testPolling() {
    EppoHttpClient httpClient = new EppoHttpClient(TEST_HOST, DUMMY_FLAG_API_KEY, "java", "3.0.0");
    EppoHttpClient httpClientSpy = spy(httpClient);
    TestUtils.setBaseClientHttpClientOverrideField(httpClientSpy);

    new EppoClient.Builder()
        .apiKey(DUMMY_FLAG_API_KEY)
        .pollingIntervalMs(20)
        .forceReinitialize(true)
        .buildAndInit();

    // Method will be called immediately on init
    verify(httpClientSpy, times(1)).get(anyString());

    // Sleep for 25 ms to allow another polling cycle to complete
    sleepUninterruptedly(25);

    // Now, the method should have been called twice
    verify(httpClientSpy, times(2)).get(anyString());

    EppoClient.stopPolling();
    sleepUninterruptedly(25);

    // No more calls since stopped
    verify(httpClientSpy, times(2)).get(anyString());
  }

  // NOTE: Graceful mode during init is intrinsically true since the call is non-blocking and
  // exceptions are caught without rethrowing in `FetchConfigurationsTask`

  @Test
  public void testClientMakesDefaultAssignmentsAfterFailingToInitialize() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize and no exception should be thrown.
    try {
      EppoClient eppoClient = initFailingGracefulClient(true);
      Thread.sleep(25); // Sleep to allow the async config fetch call to happen (and fail)
      assertEquals("default", eppoClient.getStringAssignment("experiment1", "subject1", "default"));
    } catch (Exception e) {
      fail("Unexpected exception: " + e);
    }
  }

  public static void mockHttpError() {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    // Mock sync get
    when(mockHttpClient.get(anyString())).thenThrow(new RuntimeException("Intentional Error"));

    // Mock async get
    CompletableFuture<byte[]> mockAsyncResponse = new CompletableFuture<>();
    when(mockHttpClient.getAsync(anyString())).thenReturn(mockAsyncResponse);
    mockAsyncResponse.completeExceptionally(new RuntimeException("Intentional Error"));

    setBaseClientHttpClientOverrideField(mockHttpClient);
  }

  @SuppressWarnings("SameParameterValue")
  private void sleepUninterruptedly(long sleepMs) {
    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private EppoClient initClient(String apiKey) {
    mockAssignmentLogger = mock(AssignmentLogger.class);
    mockBanditLogger = mock(BanditLogger.class);

    return new EppoClient.Builder()
        .apiKey(apiKey)
        .apiBaseUrl(Constants.appendApiPathToHost(TEST_HOST))
        .assignmentLogger(mockAssignmentLogger)
        .banditLogger(mockBanditLogger)
        .isGracefulMode(false)
        .forceReinitialize(true) // Useful for tests
        .buildAndInit();
  }

  private EppoClient initFailingGracefulClient(boolean isGracefulMode) {
    mockAssignmentLogger = mock(AssignmentLogger.class);
    mockBanditLogger = mock(BanditLogger.class);

    return new EppoClient.Builder()
        .apiKey(DUMMY_FLAG_API_KEY)
        .apiBaseUrl("blag")
        .assignmentLogger(mockAssignmentLogger)
        .banditLogger(mockBanditLogger)
        .isGracefulMode(isGracefulMode)
        .forceReinitialize(true) // Useful for tests
        .buildAndInit();
  }

  private void uninitClient() {
    try {
      Field httpClientOverrideField = EppoClient.class.getDeclaredField("instance");
      httpClientOverrideField.setAccessible(true);
      httpClientOverrideField.set(null, null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private void initBuggyClient() {
    try {
      EppoClient eppoClient = initClient(DUMMY_FLAG_API_KEY);
      Field configurationStoreField = BaseEppoClient.class.getDeclaredField("configurationStore");
      configurationStoreField.setAccessible(true);
      configurationStoreField.set(eppoClient, null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setBaseClientHttpClientOverrideField(EppoHttpClient httpClient) {
    // Uses reflection to set a static override field used for tests (e.g., httpClientOverride)
    try {
      Field httpClientOverrideField = BaseEppoClient.class.getDeclaredField("httpClientOverride");
      httpClientOverrideField.setAccessible(true);
      httpClientOverrideField.set(null, httpClient);
      httpClientOverrideField.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
