package com.eppo.sdk;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import cloud.eppo.rac.dto.*;
import cloud.eppo.rac.exception.ExperimentConfigurationNotFound;
import com.eppo.sdk.helpers.Converter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@ExtendWith(WireMockExtension.class)
public class EppoClientTest {

  private static final int TEST_PORT = 4001;

  private WireMockServer mockServer;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Data
  static class SubjectWithAttributes {
    @JsonProperty String subjectKey;
    @JsonProperty EppoAttributes subjectAttributes;
  }

  @Data
  static class AssignmentTestCase {
    @JsonProperty String experiment;

    @JsonDeserialize(using = AssignmentValueTypeDeserializer.class)
    AssignmentValueType valueType = AssignmentValueType.STRING;

    @JsonProperty List<SubjectWithAttributes> subjectsWithAttributes;
    @JsonProperty List<String> subjects;
    @JsonProperty List<String> expectedAssignments;
  }

  enum AssignmentValueType {
    STRING("string"),
    BOOLEAN("boolean"),
    JSON("json"),
    NUMERIC("numeric");

    private final String strValue;

    AssignmentValueType(String value) {
      this.strValue = value;
    }

    String value() {
      return this.strValue;
    }

    static AssignmentValueType getByString(String str) {
      for (AssignmentValueType valueType : AssignmentValueType.values()) {
        if (valueType.value().compareTo(str) == 0) {
          return valueType;
        }
      }
      return null;
    }
  }

  static class AssignmentValueTypeDeserializer extends StdDeserializer<AssignmentValueType> {

    public AssignmentValueTypeDeserializer() {
      this((Class<?>) null);
    }

    protected AssignmentValueTypeDeserializer(Class<?> vc) {
      super(vc);
    }

    protected AssignmentValueTypeDeserializer(JavaType valueType) {
      super(valueType);
    }

    protected AssignmentValueTypeDeserializer(StdDeserializer<?> src) {
      super(src);
    }

    @Override
    public AssignmentValueType deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
      AssignmentValueType value = AssignmentValueType.getByString(node.asText());
      if (value == null) {
        throw new RuntimeException("Invalid assignment value type");
      }

      return value;
    }
  }

  private IAssignmentLogger mockAssignmentLogger;
  private IBanditLogger mockBanditLogger;

  @BeforeEach
  void init() {
    mockAssignmentLogger = mock(IAssignmentLogger.class);
    mockBanditLogger = mock(IBanditLogger.class);

    // For now, use our special bandits RAC until we fold it into the shared test case suite
    this.mockServer = new WireMockServer(TEST_PORT);
    this.mockServer.start();
    String racResponseJson =
        getMockRandomizedAssignmentResponse(
            "src/test/resources/bandits/rac-experiments-bandits-beta.json");
    this.mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*"))
            .willReturn(WireMock.okJson(racResponseJson)));
    String banditResponseJson =
        getMockRandomizedAssignmentResponse("src/test/resources/bandits/bandits-parameters-1.json");
    this.mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*flag-config/v1/bandits\\?.*"))
            .willReturn(WireMock.okJson(banditResponseJson)));

    // Initialize our client with the mock loggers we can spy on
    EppoClientConfig config =
        new EppoClientConfig(
            "mock-api-key", "http://localhost:4001", mockAssignmentLogger, mockBanditLogger);
    EppoClient.init(config);
  }

  @AfterEach
  void teardown() {
    this.mockServer.stop();
  }

  @Test()
  void testGracefulModeOn() {
    EppoClientConfig config =
        new EppoClientConfig("mock-api-key", "http://localhost:4001", logData -> {}, null);
    EppoClient realClient = EppoClient.init(config);
    EppoClient spyClient = spy(realClient);

    doThrow(new ExperimentConfigurationNotFound("Exception thrown by mock"))
        .when(spyClient)
        .getAssignmentValue(anyString(), anyString(), any(EppoAttributes.class), anyMap());

    assertDoesNotThrow(() -> spyClient.getBooleanAssignment("subject1", "experiment1"));
    assertDoesNotThrow(() -> spyClient.getDoubleAssignment("subject1", "experiment1"));
    assertDoesNotThrow(() -> spyClient.getParsedJSONAssignment("subject1", "experiment1"));
    assertDoesNotThrow(() -> spyClient.getJSONStringAssignment("subject1", "experiment1"));
    assertDoesNotThrow(() -> spyClient.getStringAssignment("subject1", "experiment1"));

    assertEquals(Optional.empty(), spyClient.getBooleanAssignment("subject1", "experiment1"));
    assertEquals(Optional.empty(), spyClient.getDoubleAssignment("subject1", "experiment1"));
    assertEquals(Optional.empty(), spyClient.getParsedJSONAssignment("subject1", "experiment1"));
    assertEquals(Optional.empty(), spyClient.getJSONStringAssignment("subject1", "experiment1"));
    assertEquals(Optional.empty(), spyClient.getStringAssignment("subject1", "experiment1"));
  }

  @Test()
  void testGracefulModeOff() {
    EppoClientConfig config =
        new EppoClientConfig(
            "mock-api-key",
            "http://localhost:4001",
            logData -> {
              // Auto-generated method stub
            },
            null);
    config.setGracefulMode(false);
    EppoClient.init(config);
    EppoClient realClient = EppoClient.getInstance();

    EppoClient spyClient = spy(realClient);

    doThrow(new ExperimentConfigurationNotFound("Exception thrown by mock"))
        .when(spyClient)
        .getAssignmentValue(anyString(), anyString(), any(EppoAttributes.class), any());

    assertThrows(
        ExperimentConfigurationNotFound.class,
        () -> spyClient.getBooleanAssignment("subject1", "experiment1"));
    assertThrows(
        ExperimentConfigurationNotFound.class,
        () -> spyClient.getDoubleAssignment("subject1", "experiment1"));
    assertThrows(
        ExperimentConfigurationNotFound.class,
        () -> spyClient.getParsedJSONAssignment("subject1", "experiment1"));
    assertThrows(
        ExperimentConfigurationNotFound.class,
        () -> spyClient.getJSONStringAssignment("subject1", "experiment1"));
    assertThrows(
        ExperimentConfigurationNotFound.class,
        () -> spyClient.getStringAssignment("subject1", "experiment1"));
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  void testAssignments(AssignmentTestCase testCase) {

    // These test cases rely on the currently shared non-bandit RAC, so we need to re-initialize our
    // client to use that
    String racResponseJson =
        getMockRandomizedAssignmentResponse("src/test/resources/rac-experiments-v3.json");
    this.mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*"))
            .willReturn(WireMock.okJson(racResponseJson)));
    EppoClientConfig config =
        new EppoClientConfig("mock-api-key", "http://localhost:4001", mockAssignmentLogger, null);
    EppoClient.init(config);

    switch (testCase.valueType) {
      case NUMERIC:
        List<Double> expectedDoubleAssignments =
            Converter.convertToDecimal(testCase.expectedAssignments);
        List<Double> actualDoubleAssignments = this.getDoubleAssignments(testCase);
        assertEquals(expectedDoubleAssignments, actualDoubleAssignments);
        break;
      case BOOLEAN:
        List<Boolean> expectedBooleanAssignments =
            Converter.convertToBoolean(testCase.expectedAssignments);
        List<Boolean> actualBooleanAssignments = this.getBooleanAssignments(testCase);
        assertEquals(expectedBooleanAssignments, actualBooleanAssignments);
        break;
      case JSON:
        List<JsonNode> actualJSONAssignments = this.getJSONAssignments(testCase);
        for (JsonNode node : actualJSONAssignments) {
          assertEquals(JsonNodeType.OBJECT, node.getNodeType());
        }

        assertEquals(
            testCase.expectedAssignments,
            actualJSONAssignments.stream().map(JsonNode::toString).collect(Collectors.toList()));
        break;
      default:
        List<String> actualStringAssignments = this.getStringAssignments(testCase);
        assertEquals(testCase.expectedAssignments, actualStringAssignments);
    }
  }

  private List<?> getAssignments(AssignmentTestCase testCase, AssignmentValueType valueType) {
    EppoClient client = EppoClient.getInstance();
    if (testCase.subjectsWithAttributes != null) {
      return testCase.subjectsWithAttributes.stream()
          .map(
              subject -> {
                try {
                  switch (valueType) {
                    case NUMERIC:
                      return client
                          .getDoubleAssignment(
                              subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                          .orElse(null);
                    case BOOLEAN:
                      return client
                          .getBooleanAssignment(
                              subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                          .orElse(null);
                    case JSON:
                      return client
                          .getParsedJSONAssignment(
                              subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                          .orElse(null);
                    default:
                      return client
                          .getStringAssignment(
                              subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                          .orElse(null);
                  }
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              })
          .collect(Collectors.toList());
    }
    return testCase.subjects.stream()
        .map(
            subject -> {
              try {
                switch (valueType) {
                  case NUMERIC:
                    return client.getDoubleAssignment(subject, testCase.experiment).orElse(null);
                  case BOOLEAN:
                    return client.getBooleanAssignment(subject, testCase.experiment).orElse(null);
                  case JSON:
                    return client
                        .getParsedJSONAssignment(subject, testCase.experiment)
                        .orElse(null);
                  default:
                    return client.getStringAssignment(subject, testCase.experiment).orElse(null);
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toList());
  }

  private List<String> getStringAssignments(AssignmentTestCase testCase) {
    //noinspection unchecked
    return (List<String>) this.getAssignments(testCase, AssignmentValueType.STRING);
  }

  private List<Double> getDoubleAssignments(AssignmentTestCase testCase) {
    //noinspection unchecked
    return (List<Double>) this.getAssignments(testCase, AssignmentValueType.NUMERIC);
  }

  private List<Boolean> getBooleanAssignments(AssignmentTestCase testCase) {
    //noinspection unchecked
    return (List<Boolean>) this.getAssignments(testCase, AssignmentValueType.BOOLEAN);
  }

  private List<JsonNode> getJSONAssignments(AssignmentTestCase testCase) {
    //noinspection unchecked
    return (List<JsonNode>) this.getAssignments(testCase, AssignmentValueType.JSON);
  }

  private static Stream<Arguments> getAssignmentTestData() throws IOException {
    File testCaseFolder = new File("src/test/resources/assignment-v2/");
    File[] testCaseFiles = testCaseFolder.listFiles();
    assertNotNull(testCaseFiles);
    List<Arguments> arguments = new ArrayList<>();
    for (File testCaseFile : testCaseFiles) {
      String json = FileUtils.readFileToString(testCaseFile, "UTF8");
      AssignmentTestCase testCase = MAPPER.readValue(json, AssignmentTestCase.class);
      arguments.add(Arguments.of(testCase));
    }
    return arguments.stream();
  }

  private static String getMockRandomizedAssignmentResponse(String jsonToReturnFilePath) {
    File mockRacResponse = new File(jsonToReturnFilePath);
    try {
      return FileUtils.readFileToString(mockRacResponse, "UTF8");
    } catch (Exception e) {
      throw new RuntimeException("Error reading mock RAC data: " + e.getMessage(), e);
    }
  }

  @Test
  public void testBanditColdStartAction() {
    Set<String> banditActions =
        Stream.of("option1", "option2", "option3").collect(Collectors.toSet());

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject1", "cold-start-bandit-experiment", new EppoAttributes(), banditActions);

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertTrue(banditActions.contains(stringAssignment.get()));

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor =
        ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedAssignmentLog.getExperiment());
    assertEquals("cold-start-bandit-experiment", capturedAssignmentLog.getFeatureFlag());
    assertEquals("bandit", capturedAssignmentLog.getAllocation());
    assertEquals("cold-start-bandit", capturedAssignmentLog.getVariation());
    assertEquals("subject1", capturedAssignmentLog.getSubject());
    assertEquals(new EppoAttributes(), capturedAssignmentLog.getSubjectAttributes());

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedBanditLog.getExperiment());
    assertEquals("cold-start-bandit", capturedBanditLog.getBanditKey());
    assertEquals("subject1", capturedBanditLog.getSubject());
    assertEquals(new HashMap<>(), capturedBanditLog.getSubjectNumericAttributes());
    assertEquals(new HashMap<>(), capturedBanditLog.getSubjectCategoricalAttributes());
    assertEquals("option1", capturedBanditLog.getAction());
    assertEquals(new HashMap<>(), capturedBanditLog.getActionNumericAttributes());
    assertEquals(new HashMap<>(), capturedBanditLog.getActionCategoricalAttributes());
    assertEquals(0.3333, capturedBanditLog.getActionProbability(), 0.0002);
    assertEquals("falcon cold start", capturedBanditLog.getModelVersion());
  }

  @Test
  public void testBanditUninitializedAction() {
    Set<String> banditActions =
        Stream.of("option1", "option2", "option3").collect(Collectors.toSet());

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject8", "uninitialized-bandit-experiment", new EppoAttributes(), banditActions);

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertTrue(banditActions.contains(stringAssignment.get()));

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor =
        ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("uninitialized-bandit-experiment-bandit", capturedAssignmentLog.getExperiment());
    assertEquals("uninitialized-bandit-experiment", capturedAssignmentLog.getFeatureFlag());
    assertEquals("bandit", capturedAssignmentLog.getAllocation());
    assertEquals("this-bandit-does-not-exist", capturedAssignmentLog.getVariation());
    assertEquals("subject8", capturedAssignmentLog.getSubject());
    assertEquals(new EppoAttributes(), capturedAssignmentLog.getSubjectAttributes());

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("uninitialized-bandit-experiment-bandit", capturedBanditLog.getExperiment());
    assertEquals("this-bandit-does-not-exist", capturedBanditLog.getBanditKey());
    assertEquals("subject8", capturedBanditLog.getSubject());
    assertEquals(new HashMap<>(), capturedBanditLog.getSubjectNumericAttributes());
    assertEquals(new HashMap<>(), capturedBanditLog.getSubjectCategoricalAttributes());
    assertEquals("option1", capturedBanditLog.getAction());
    assertEquals(new HashMap<>(), capturedBanditLog.getActionNumericAttributes());
    assertEquals(new HashMap<>(), capturedBanditLog.getActionCategoricalAttributes());
    assertEquals(0.3333, capturedBanditLog.getActionProbability(), 0.0002);
    assertEquals("uninitialized", capturedBanditLog.getModelVersion());
  }

  @Test
  public void testBanditModelActionLogging() {
    // Note: some of the passed in attributes are not used for scoring, but we do still want to make
    // sure they are logged

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("gender_identity", EppoValue.valueOf("female"));
    subjectAttributes.put(
        "days_since_signup",
        EppoValue.valueOf(130)); // unused for scoring (which looks for account_age)
    subjectAttributes.put("is_premium", EppoValue.valueOf(false)); // unused for scoring
    subjectAttributes.put("numeric_string", EppoValue.valueOf("123")); // unused for scoring
    subjectAttributes.put("unpopulated", EppoValue.nullValue()); // unused for scoring

    Map<String, EppoAttributes> actionsWithAttributes = new HashMap<>();

    EppoAttributes nikeAttributes = new EppoAttributes();
    nikeAttributes.put("brand_affinity", EppoValue.valueOf(0.25));
    actionsWithAttributes.put("nike", nikeAttributes);

    EppoAttributes adidasAttributes = new EppoAttributes();
    adidasAttributes.put("brand_affinity", EppoValue.valueOf(0.1));
    adidasAttributes.put("num_brand_purchases", EppoValue.valueOf(5)); // unused for scoring
    adidasAttributes.put("in_email_campaign", EppoValue.valueOf(true)); // unused for scoring
    adidasAttributes.put("also_unpopulated", EppoValue.nullValue()); // unused for scoring
    actionsWithAttributes.put("adidas", adidasAttributes);

    actionsWithAttributes.put("puma", new EppoAttributes());

    // Get our assigned action
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject2", "banner-bandit-experiment", subjectAttributes, actionsWithAttributes);

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertEquals("adidas", stringAssignment.get());

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor =
        ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("banner-bandit-experiment-bandit", capturedAssignmentLog.getExperiment());
    assertEquals("banner-bandit-experiment", capturedAssignmentLog.getFeatureFlag());
    assertEquals("bandit", capturedAssignmentLog.getAllocation());
    assertEquals("banner-bandit", capturedAssignmentLog.getVariation());
    assertEquals("subject2", capturedAssignmentLog.getSubject());
    assertEquals(subjectAttributes, capturedAssignmentLog.getSubjectAttributes());

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("banner-bandit-experiment-bandit", capturedBanditLog.getExperiment());
    assertEquals("banner-bandit", capturedBanditLog.getBanditKey());
    assertEquals("subject2", capturedBanditLog.getSubject());
    assertEquals("adidas", capturedBanditLog.getAction());
    assertEquals(0.2899, capturedBanditLog.getActionProbability(), 0.0002);
    assertEquals("falcon v123", capturedBanditLog.getModelVersion());

    Map<String, Double> expectedSubjectNumericAttributes = new HashMap<>();
    expectedSubjectNumericAttributes.put("days_since_signup", 130.0);
    assertEquals(expectedSubjectNumericAttributes, capturedBanditLog.getSubjectNumericAttributes());

    Map<String, String> expectedSubjectCategoricalAttributes = new HashMap<>();
    expectedSubjectCategoricalAttributes.put("gender_identity", "female");
    expectedSubjectCategoricalAttributes.put("is_premium", "false");
    expectedSubjectCategoricalAttributes.put("numeric_string", "123");
    assertEquals(
        expectedSubjectCategoricalAttributes, capturedBanditLog.getSubjectCategoricalAttributes());

    Map<String, Double> expectedActionNumericAttributes = new HashMap<>();
    expectedActionNumericAttributes.put("brand_affinity", 0.1);
    expectedActionNumericAttributes.put("num_brand_purchases", 5.0);
    assertEquals(expectedActionNumericAttributes, capturedBanditLog.getActionNumericAttributes());

    Map<String, String> expectedActionCategoricalAttributes = new HashMap<>();
    expectedActionCategoricalAttributes.put("in_email_campaign", "true");
    assertEquals(
        expectedActionCategoricalAttributes, capturedBanditLog.getActionCategoricalAttributes());
  }

  @Test
  public void testBanditModelActionAssignmentFullContext() {
    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("gender_identity", EppoValue.valueOf("male"));
    subjectAttributes.put("account_age", EppoValue.valueOf(3));

    Map<String, EppoAttributes> actionAttributes = new HashMap<>();

    EppoAttributes nikeAttributes = new EppoAttributes();
    nikeAttributes.put("brand_affinity", EppoValue.valueOf(0.05));
    nikeAttributes.put("purchased_last_30_days", EppoValue.valueOf(true));
    nikeAttributes.put("loyalty_tier", EppoValue.valueOf("gold"));
    actionAttributes.put("nike", nikeAttributes);

    EppoAttributes adidasAttributes = new EppoAttributes();
    adidasAttributes.put("brand_affinity", EppoValue.valueOf(0.30));
    adidasAttributes.put("purchased_last_30_days", EppoValue.valueOf(true));
    adidasAttributes.put("loyalty_tier", EppoValue.valueOf("gold"));
    actionAttributes.put("adidas", adidasAttributes);

    EppoAttributes pumaAttributes = new EppoAttributes();
    pumaAttributes.put("brand_affinity", EppoValue.valueOf(1.00));
    pumaAttributes.put("purchased_last_30_days", EppoValue.valueOf(false));
    pumaAttributes.put("loyalty_tier", EppoValue.valueOf("bronze"));
    actionAttributes.put("puma", pumaAttributes);

    // Get our assigned action
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject30", "banner-bandit-experiment", subjectAttributes, actionAttributes);

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertEquals("adidas", stringAssignment.get());
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals(0.8043, capturedBanditLog.getActionProbability(), 0.0002);
  }

  @Test
  public void testBanditModelActionAssignmentNoContext() {
    EppoAttributes subjectAttributes = new EppoAttributes();
    Set<String> actions = Stream.of("nike", "adidas", "puma").collect(Collectors.toSet());

    // Get our assigned action
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject39", "banner-bandit-experiment", subjectAttributes, actions);

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertEquals("puma", stringAssignment.get());
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals(0.1613, capturedBanditLog.getActionProbability(), 0.0002);
  }

  @Test
  public void testBanditControlAction() {

    Set<String> banditActions =
        Stream.of("option1", "option2", "option3").collect(Collectors.toSet());

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("account_age", EppoValue.valueOf(90));
    subjectAttributes.put("loyalty_tier", EppoValue.valueOf("gold"));
    subjectAttributes.put("is_account_admin", EppoValue.valueOf(false));

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject10", "cold-start-bandit-experiment", subjectAttributes, banditActions);

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertEquals("control", stringAssignment.get());

    // Manually log an action

    EppoAttributes controlActionAttributes = new EppoAttributes();
    controlActionAttributes.put("brand", EppoValue.valueOf("skechers"));
    controlActionAttributes.put("num_past_purchases", EppoValue.valueOf(0));
    controlActionAttributes.put("has_promo_code", EppoValue.valueOf(true));

    Exception banditLoggingException =
        EppoClient.getInstance()
            .logNonBanditAction(
                "subject10",
                "cold-start-bandit-experiment",
                subjectAttributes,
                "option0",
                controlActionAttributes);
    assertNull(banditLoggingException);

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor =
        ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedAssignmentLog.getExperiment());
    assertEquals("cold-start-bandit-experiment", capturedAssignmentLog.getFeatureFlag());
    assertEquals("bandit", capturedAssignmentLog.getAllocation());
    assertEquals("control", capturedAssignmentLog.getVariation());
    assertEquals("subject10", capturedAssignmentLog.getSubject());
    assertEquals(subjectAttributes, capturedAssignmentLog.getSubjectAttributes());

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedBanditLog.getExperiment());
    assertEquals("control", capturedBanditLog.getBanditKey());
    assertEquals("subject10", capturedBanditLog.getSubject());
    assertEquals("option0", capturedBanditLog.getAction());
    assertNull(capturedBanditLog.getActionProbability());
    assertNull(capturedBanditLog.getModelVersion());

    Map<String, Double> expectedSubjectNumericAttributes = new HashMap<>();
    expectedSubjectNumericAttributes.put("account_age", 90.0);
    assertEquals(expectedSubjectNumericAttributes, capturedBanditLog.getSubjectNumericAttributes());

    Map<String, String> expectedSubjectCategoricalAttributes = new HashMap<>();
    expectedSubjectCategoricalAttributes.put("loyalty_tier", "gold");
    expectedSubjectCategoricalAttributes.put("is_account_admin", "false");
    assertEquals(
        expectedSubjectCategoricalAttributes, capturedBanditLog.getSubjectCategoricalAttributes());

    Map<String, Double> expectedActionNumericAttributes = new HashMap<>();
    expectedActionNumericAttributes.put("num_past_purchases", 0.0);
    assertEquals(expectedActionNumericAttributes, capturedBanditLog.getActionNumericAttributes());

    Map<String, String> expectedActionCategoricalAttributes = new HashMap<>();
    expectedActionCategoricalAttributes.put("brand", "skechers");
    expectedActionCategoricalAttributes.put("has_promo_code", "true");
    assertEquals(
        expectedActionCategoricalAttributes, capturedBanditLog.getActionCategoricalAttributes());
  }

  @Test
  public void testBanditNotInAllocation() {
    Set<String> banditActions =
        Stream.of("option1", "option2", "option3").collect(Collectors.toSet());

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment =
        EppoClient.getInstance()
            .getBanditAssignment(
                "subject2", "cold-start-bandit", new EppoAttributes(), banditActions);

    // Verify assignment
    assertFalse(stringAssignment.isPresent());
  }
}
