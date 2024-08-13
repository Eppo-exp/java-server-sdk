package com.eppo.sdk;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static org.mockito.Mockito.*;

import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
  private AssignmentLogger mockAssignmentLogger;
  private BanditLogger mockBanditLogger;

  @BeforeAll
  public static void initMockServer() {
    mockServer = new WireMockServer(TEST_PORT);
    mockServer.start();
    // TODO: different UFC response based on API key
    String ufcResponseJson =
        readConfig(
            "src/test/resources/shared/ufc/flags-v1.json");
    mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*flag-config/v1/config\\?.*"))
            .willReturn(WireMock.okJson(ufcResponseJson)));
    String banditResponseJson =
        readConfig("src/test/resources/shared/ufc/bandit-models-v1.json");
    mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*flag-config/v1/bandits\\?.*"))
            .willReturn(WireMock.okJson(banditResponseJson)));
  }

  private static String readConfig(String jsonToReturnFilePath) {
    File mockResponseFile = new File(jsonToReturnFilePath);
    try {
      return FileUtils.readFileToString(mockResponseFile, "UTF8");
    } catch (Exception e) {
      throw new RuntimeException("Error reading mock data: " + e.getMessage(), e);
    }
  }

  @BeforeEach
  public void initClient() {
    mockAssignmentLogger = mock(AssignmentLogger.class);
    mockBanditLogger = mock(BanditLogger.class);

    new EppoClient.Builder()
      .apiKey(DUMMY_FLAG_API_KEY)
      .host(TEST_HOST)
      .assignmentLogger(mockAssignmentLogger)
      .banditLogger(mockBanditLogger)
      .isGracefulMode(false)
      .buildAndInit();
  }

  @AfterEach
  void tearDown() {
    if (mockServer != null) {
      mockServer.stop();
      }
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testUnobfuscatedAssignments(File testFile) {
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase);
  }

  private static Stream<Arguments> getAssignmentTestData() {
    return AssignmentTestCase.getAssignmentTestData();
  }

  // TODO: test for force reinitialization
}
