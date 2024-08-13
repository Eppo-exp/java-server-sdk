package com.eppo.sdk;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static cloud.eppo.helpers.BanditTestCase.parseBanditTestCaseFile;
import static cloud.eppo.helpers.BanditTestCase.runBanditTestCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.helpers.BanditTestCase;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import cloud.eppo.ufc.dto.Attributes;
import cloud.eppo.ufc.dto.BanditActions;
import cloud.eppo.ufc.dto.BanditResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
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

  @SuppressWarnings("ExtractMethodRecommender")
  @Test
  public void testLoggers() {
    Date testStart = new Date();
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

    Date inTheNearFuture = new Date(System.currentTimeMillis() + 1);

    // Verify experiment assignment log
    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    Assignment capturedAssignment = assignmentLogCaptor.getValue();
    assertTrue(capturedAssignment.getTimestamp().after(testStart));
    assertTrue(capturedAssignment.getTimestamp().before(inTheNearFuture));
    assertEquals("banner_bandit_flag-training", capturedAssignment.getExperiment());
    assertEquals(flagKey, capturedAssignment.getFeatureFlag());
    assertEquals("training", capturedAssignment.getAllocation());
    assertEquals("banner_bandit", capturedAssignment.getVariation());
    assertEquals(subjectKey, capturedAssignment.getSubject());
    assertEquals(subjectAttributes, capturedAssignment.getSubjectAttributes());
    assertEquals("false", capturedAssignment.getMetaData().get("obfuscated"));

    // Verify bandit log
    ArgumentCaptor<BanditAssignment> banditLogCaptor =
      ArgumentCaptor.forClass(BanditAssignment.class);
    verify(mockBanditLogger, times(1)).logBanditAssignment(banditLogCaptor.capture());
    BanditAssignment capturedBanditAssignment = banditLogCaptor.getValue();
    assertTrue(capturedBanditAssignment.getTimestamp().after(testStart));
    assertTrue(capturedBanditAssignment.getTimestamp().before(inTheNearFuture));
    assertEquals(flagKey, capturedBanditAssignment.getFeatureFlag());
    assertEquals("banner_bandit", capturedBanditAssignment.getBandit());
    assertEquals(subjectKey, capturedBanditAssignment.getSubject());
    assertEquals("adidas", capturedBanditAssignment.getAction());
    assertEquals(0.099, capturedBanditAssignment.getActionProbability(), 0.0002);
    assertEquals("v123", capturedBanditAssignment.getModelVersion());

    Attributes expectedSubjectNumericAttributes = new Attributes();
    expectedSubjectNumericAttributes.put("age", 25);
    assertEquals(
      expectedSubjectNumericAttributes, capturedBanditAssignment.getSubjectNumericAttributes());

    Attributes expectedSubjectCategoricalAttributes = new Attributes();
    expectedSubjectCategoricalAttributes.put("country", "USA");
    expectedSubjectCategoricalAttributes.put("gender_identity", "female");
    assertEquals(
      expectedSubjectCategoricalAttributes,
      capturedBanditAssignment.getSubjectCategoricalAttributes());

    Attributes expectedActionNumericAttributes = new Attributes();
    expectedActionNumericAttributes.put("brand_affinity", -1.0);
    assertEquals(
      expectedActionNumericAttributes, capturedBanditAssignment.getActionNumericAttributes());

    Attributes expectedActionCategoricalAttributes = new Attributes();
    expectedActionCategoricalAttributes.put("loyalty_tier", "bronze");
    assertEquals(
      expectedActionCategoricalAttributes,
      capturedBanditAssignment.getActionCategoricalAttributes());

    assertEquals("false", capturedBanditAssignment.getMetaData().get("obfuscated"));
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
