package com.eppo.sdk;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static cloud.eppo.helpers.BanditTestCase.parseBanditTestCaseFile;
import static cloud.eppo.helpers.BanditTestCase.runBanditTestCase;
import static org.mockito.Mockito.*;

import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.helpers.BanditTestCase;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(WireMockExtension.class)
public class EppoClientTest {

  private static final int TEST_PORT = 4001;
  private static final String TEST_HOST = "http://localhost:" + TEST_PORT;
  private static WireMockServer mockServer;

  private static final String DUMMY_FLAG_API_KEY = "dummy-flags-api-key"; // Will load flags-v1
  private static final String DUMMY_BANDIT_API_KEY = "dummy-bandits-api-key"; // Will load bandit-flags-v1
  private AssignmentLogger mockAssignmentLogger;
  private BanditLogger mockBanditLogger;

  @BeforeAll
  public static void initMockServer() {
    mockServer = new WireMockServer(TEST_PORT);
    mockServer.start();

    // If we get the dummy flag API key, return flags-v1.json
    String ufcFlagsResponseJson =
        readConfig(
            "src/test/resources/shared/ufc/flags-v1.json");
    mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*flag-config/v1/config\\?.*apiKey="+DUMMY_FLAG_API_KEY+".*"))
            .willReturn(WireMock.okJson(ufcFlagsResponseJson)));

    // If we get the dummy bandit API key, return bandit-flags-v1.json
    String banditFlagsResponseJson =
      readConfig(
        "src/test/resources/shared/ufc/bandit-flags-v1.json");
    mockServer.stubFor(
      WireMock.get(WireMock.urlMatching(".*flag-config/v1/config\\?.*apiKey="+DUMMY_BANDIT_API_KEY+".*"))
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

  private EppoClient initClient(String apiKey) {
    mockAssignmentLogger = mock(AssignmentLogger.class);
    mockBanditLogger = mock(BanditLogger.class);

    return new EppoClient.Builder()
      .apiKey(apiKey)
      .host(TEST_HOST)
      .assignmentLogger(mockAssignmentLogger)
      .banditLogger(mockBanditLogger)
      .isGracefulMode(false)
      .forceReinitialize(true) // Useful for tests
      .buildAndInit();
  }

  // TODO: test for force reinitialization
}
