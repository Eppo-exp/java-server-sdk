package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.BanditActions;
import cloud.eppo.api.BanditResult;
import cloud.eppo.api.Configuration;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  private static final byte[] EMPTY_CONFIG = "{\"flags\":{}}".getBytes();

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
    try {
      EppoClient.getInstance().stopPolling();
    } catch (IllegalStateException ex) {
      // pass: Indicates that the singleton Eppo Client has not yet been initialized.
    }
  }

  @AfterAll
  public static void tearDown() {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  // TODO: Re-enable once sdk-common-jvm:tests artifact is updated with package relocations
  // The test helpers reference cloud.eppo.ufc.dto.VariationType which has moved to
  // cloud.eppo.api.dto
  // @ParameterizedTest
  // @MethodSource("getAssignmentTestData")
  // public void testUnobfuscatedAssignments(File testFile) {
  //   AssignmentTestCase testCase = parseTestCaseFile(testFile);
  //   EppoClient eppoClient = initClient(DUMMY_FLAG_API_KEY);
  //   runTestCase(testCase, eppoClient);
  // }

  // private static Stream<Arguments> getAssignmentTestData() {
  //   return AssignmentTestCase.getAssignmentTestData();
  // }

  // @ParameterizedTest
  // @MethodSource("getBanditTestData")
  // public void testUnobfuscatedBanditAssignments(File testFile) {
  //   BanditTestCase testCase = parseBanditTestCaseFile(testFile);
  //   EppoClient eppoClient = initClient(DUMMY_BANDIT_API_KEY);
  //   runBanditTestCase(testCase, eppoClient);
  // }

  // private static Stream<Arguments> getBanditTestData() {
  //   return BanditTestCase.getBanditTestData();
  // }

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
  public void testReinitializeWithoutForcing() {
    EppoClient firstInstance = initClient(DUMMY_FLAG_API_KEY);
    EppoClient secondInstance = EppoClient.builder(DUMMY_FLAG_API_KEY).buildAndInit();

    assertSame(firstInstance, secondInstance);
  }

  @Test
  public void testReinitializeWitForcing() {
    EppoClient firstInstance = initClient(DUMMY_FLAG_API_KEY);
    EppoClient secondInstance =
        EppoClient.builder(DUMMY_FLAG_API_KEY).forceReinitialize(true).buildAndInit();

    assertNotSame(firstInstance, secondInstance);
  }

  @Test
  public void testPolling() {
    // Initialize with polling enabled
    EppoClient.builder(DUMMY_FLAG_API_KEY)
        .apiBaseUrl(TEST_HOST)
        .pollingIntervalMs(100)
        .forceReinitialize(true)
        .buildAndInit();

    // Verify polling can be stopped without errors
    EppoClient.getInstance().stopPolling();
  }

  // NOTE: Graceful mode during init is intrinsically true since the call is non-blocking and
  // exceptions are caught without rethrowing in `FetchConfigurationsTask`

  @Test
  public void testClientMakesDefaultAssignmentsAfterFailingToInitialize() {
    // Set up bad HTTP response via WireMock
    mockHttpError();

    // Initialize with a bad URL that will fail to fetch config
    // No exception should be thrown in graceful mode
    try {
      mockAssignmentLogger = mock(AssignmentLogger.class);
      mockBanditLogger = mock(BanditLogger.class);

      EppoClient eppoClient =
          EppoClient.builder("error-api-key")
              .apiBaseUrl(TEST_HOST)
              .assignmentLogger(mockAssignmentLogger)
              .banditLogger(mockBanditLogger)
              .isGracefulMode(true)
              .forceReinitialize(true)
              .buildAndInit();

      Thread.sleep(25); // Sleep to allow the async config fetch call to happen (and fail)
      assertEquals("default", eppoClient.getStringAssignment("experiment1", "subject1", "default"));
    } catch (Exception e) {
      fail("Unexpected exception: " + e);
    }
  }

  @Test
  public void testGetConfiguration() {
    EppoClient eppoClient = initClient(DUMMY_FLAG_API_KEY);
    Configuration configuration = eppoClient.getConfiguration();
    assertNotNull(configuration);
    assertNotNull(configuration.getFlag("numeric_flag"));
    assertEquals(VariationType.NUMERIC, configuration.getFlagType("numeric_flag"));
  }

  @Test
  public void testConfigurationChangeListener() {
    List<Configuration> received = new ArrayList<>();

    EppoClient.Builder clientBuilder =
        EppoClient.builder(DUMMY_FLAG_API_KEY)
            .apiBaseUrl(TEST_HOST)
            .forceReinitialize(true)
            .onConfigurationChange(received::add)
            .isGracefulMode(false);

    // Initialize and the callback should be triggered
    EppoClient eppoClient = clientBuilder.buildAndInit();

    // Configuration change callback should have been called at least once
    assertTrue(received.size() >= 1);

    // Trigger a reload of the client
    eppoClient.loadConfiguration();

    // Should have received another configuration
    assertTrue(received.size() >= 2);
  }

  public static void mockHttpError() {
    // Configure WireMock to return an error for the error API key
    mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*flag-config/v1/config\\?.*apiKey=error-api-key.*"))
            .willReturn(WireMock.serverError().withBody("Intentional Error")));
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

    return EppoClient.builder(apiKey)
        .apiBaseUrl(TEST_HOST)
        .assignmentLogger(mockAssignmentLogger)
        .banditLogger(mockBanditLogger)
        .isGracefulMode(false)
        .forceReinitialize(true) // Useful for tests
        .buildAndInit();
  }

  private EppoClient initFailingGracefulClient(boolean isGracefulMode) {
    mockAssignmentLogger = mock(AssignmentLogger.class);
    mockBanditLogger = mock(BanditLogger.class);

    return EppoClient.builder(DUMMY_FLAG_API_KEY)
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

  private static final byte[] BOOL_FLAG_CONFIG =
      ("{\n"
              + "  \"createdAt\": \"2024-04-17T19:40:53.716Z\",\n"
              + "  \"format\": \"SERVER\",\n"
              + "  \"environment\": {\n"
              + "    \"name\": \"Test\"\n"
              + "  },\n"
              + "  \"flags\": {\n"
              + "    \"9a2025738dde19ff44cd30b9d2967000\": {\n"
              + "      \"key\": \"9a2025738dde19ff44cd30b9d2967000\",\n"
              + "      \"enabled\": true,\n"
              + "      \"variationType\": \"BOOLEAN\",\n"
              + "      \"variations\": {\n"
              + "        \"b24=\": {\n"
              + "          \"key\": \"b24=\",\n"
              + "          \"value\": \"dHJ1ZQ==\"\n"
              + "        }\n"
              + "      },\n"
              + "      \"allocations\": [\n"
              + "        {\n"
              + "          \"key\": \"b24=\",\n"
              + "          \"doLog\": true,\n"
              + "          \"splits\": [\n"
              + "            {\n"
              + "              \"variationKey\": \"b24=\",\n"
              + "              \"shards\": []\n"
              + "            }\n"
              + "          ]\n"
              + "        }\n"
              + "      ],\n"
              + "      \"totalShards\": 10000\n"
              + "    }\n"
              + "  }\n"
              + "}")
          .getBytes();
}
