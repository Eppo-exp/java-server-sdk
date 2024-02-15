package com.eppo.sdk;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.eppo.sdk.dto.*;
import com.eppo.sdk.helpers.Converter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import lombok.Data;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(WireMockExtension.class)
public class EppoClientTest {

  private static final int TEST_PORT = 4001;

  private WireMockServer mockServer;
  private static ObjectMapper MAPPER = new ObjectMapper();
  static {
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Data
  static class SubjectWithAttributes {
    String subjectKey;
    EppoAttributes subjectAttributes;
  }

  @Data
  static class AssignmentTestCase {
    String experiment;
    @JsonDeserialize(using = AssignmentValueTypeDeserializer.class)
    AssignmentValueType valueType = AssignmentValueType.STRING;
    List<SubjectWithAttributes> subjectsWithAttributes;
    List<String> subjects;
    List<String> expectedAssignments;
  }

  static enum AssignmentValueType {
    STRING("string"),
    BOOLEAN("boolean"),
    JSON("json"),
    NUMERIC("numeric");

    private String strValue;

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
    public AssignmentValueType deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
      AssignmentValueType value = AssignmentValueType.getByString(node.asText());
      if (value == null) {
        throw new RuntimeException("Invalid assignment value type");
      }

      return value;
    }
  }

  private IAssignmentLogger mockAssignmentLogger = mock();
  private IBanditLogger mockBanditLogger = mock();

  @BeforeEach
  void init() {
    mockAssignmentLogger = mock();
    mockBanditLogger = mock();

    // For now, use our special bandits RAC until we fold it into the shared test case suite
    this.mockServer = new WireMockServer(TEST_PORT);
    this.mockServer.start();
    String racResponseJson = getMockRandomizedAssignmentResponse("src/test/resources/bandits/rac-experiments-bandits-beta.json");
    this.mockServer.stubFor(
      WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*")).willReturn(WireMock.okJson(racResponseJson))
    );
    String banditResponseJson = getMockRandomizedAssignmentResponse("src/test/resources/bandits/bandits-parameters-1.json");
    this.mockServer.stubFor(
      WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/bandits\\?.*"))
        .willReturn(WireMock.okJson(banditResponseJson))
    );

    // Initialize our client with the mock loggers we can spy on
    EppoClientConfig config = EppoClientConfig.builder()
      .apiKey("mock-api-key")
      .baseURL("http://localhost:4001")
      .assignmentLogger(mockAssignmentLogger)
      .banditLogger(mockBanditLogger)
      .build();
    EppoClient.init(config);
  }

  @AfterEach
  void teardown() {
    this.mockServer.stop();
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  void testAssignments(AssignmentTestCase testCase) {

    // These test cases rely on the currently shared non-bandit RAC, so we need to re-initialize our client to use that
    String racResponseJson = getMockRandomizedAssignmentResponse("src/test/resources/rac-experiments-v3.json");
    this.mockServer.stubFor(
      WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*")).willReturn(WireMock.okJson(racResponseJson))
    );
    EppoClientConfig config = EppoClientConfig.builder()
      .apiKey("mock-api-key")
      .baseURL("http://localhost:4001")
      .assignmentLogger(mockAssignmentLogger)
      .build();
    EppoClient.init(config);

    switch (testCase.valueType) {
      case NUMERIC:
        List<Double> expectedDoubleAssignments = Converter.convertToDecimal(testCase.expectedAssignments);
        List<Double> actualDoubleAssignments = this.getDoubleAssignments(testCase);
        assertEquals(expectedDoubleAssignments, actualDoubleAssignments);
        break;
      case BOOLEAN:
        List<Boolean> expectedBooleanAssignments = Converter.convertToBoolean(testCase.expectedAssignments);
        List<Boolean> actualBooleanAssignments = this.getBooleanAssignments(testCase);
        assertEquals(expectedBooleanAssignments, actualBooleanAssignments);
        break;
      case JSON:
        List<JsonNode> actualJSONAssignments = this.getJSONAssignments(testCase);
        for (JsonNode node : actualJSONAssignments) {
          assertEquals(JsonNodeType.OBJECT, node.getNodeType());
        }

        assertEquals(testCase.expectedAssignments, actualJSONAssignments.stream()
            .map(node -> node.toString())
            .collect(Collectors.toList()));
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
          .map(subject -> {
            try {
              switch (valueType) {
                case NUMERIC:
                  return client.getDoubleAssignment(subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                      .orElse(null);
                case BOOLEAN:
                  return client.getBooleanAssignment(subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                      .orElse(null);
                case JSON:
                  return client
                      .getParsedJSONAssignment(subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                      .orElse(null);
                default:
                  return client.getStringAssignment(subject.subjectKey, testCase.experiment, subject.subjectAttributes)
                      .orElse(null);
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(Collectors.toList());
    }
    return testCase.subjects.stream()
        .map(subject -> {
          try {
            switch (valueType) {
              case NUMERIC:
                return client.getDoubleAssignment(subject, testCase.experiment)
                    .orElse(null);
              case BOOLEAN:
                return client.getBooleanAssignment(subject, testCase.experiment)
                    .orElse(null);
              case JSON:
                return client.getParsedJSONAssignment(subject, testCase.experiment)
                    .orElse(null);
              default:
                return client.getStringAssignment(subject, testCase.experiment)
                    .orElse(null);
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());
  }

  private List<String> getStringAssignments(AssignmentTestCase testCase) {
    return (List<String>) this.getAssignments(testCase, AssignmentValueType.STRING);
  }

  private List<Double> getDoubleAssignments(AssignmentTestCase testCase) {
    return (List<Double>) this.getAssignments(testCase, AssignmentValueType.NUMERIC);
  }

  private List<Boolean> getBooleanAssignments(AssignmentTestCase testCase) {
    return (List<Boolean>) this.getAssignments(testCase, AssignmentValueType.BOOLEAN);
  }

  private List<JsonNode> getJSONAssignments(AssignmentTestCase testCase) {
    return (List<JsonNode>) this.getAssignments(testCase, AssignmentValueType.JSON);
  }

  private static Stream<Arguments> getAssignmentTestData() throws IOException {
    File testCaseFolder = new File("src/test/resources/assignment-v2/");
    File[] testCaseFiles = testCaseFolder.listFiles();
    List<Arguments> arguments = new ArrayList<>();
    for (int i = 0; i < testCaseFiles.length; i++) {
      String json = FileUtils.readFileToString(testCaseFiles[i], "UTF8");
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
      throw new RuntimeException("Error reading mock RAC data: "+e.getMessage(), e);
    }
  }

  @Test
  public void testBanditColdStartAction() {
    Set<String> banditActions = Set.of("option1", "option2", "option3");

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
      "subject2",
      "cold-start-bandit-experiment",
      new EppoAttributes(),
      banditActions
    );

    // Verify assignment
    assertFalse(stringAssignment.isEmpty());
    assertTrue(banditActions.contains(stringAssignment.get()));

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor = ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedAssignmentLog.experiment);
    assertEquals("cold-start-bandit-experiment", capturedAssignmentLog.featureFlag);
    assertEquals("bandit", capturedAssignmentLog.allocation);
    assertEquals("cold-start-bandit", capturedAssignmentLog.variation);
    assertEquals("subject2", capturedAssignmentLog.subject);
    assertEquals(Map.of(), capturedAssignmentLog.subjectAttributes);

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedBanditLog.experiment);
    assertEquals("cold-start-bandit", capturedBanditLog.variation);
    assertEquals("subject2", capturedBanditLog.subject);
    assertEquals(Map.of(), capturedBanditLog.subjectNumericAttributes);
    assertEquals(Map.of(), capturedBanditLog.subjectCategoricalAttributes);
    assertEquals("option3", capturedBanditLog.action);
    assertEquals(Map.of(), capturedBanditLog.actionNumericAttributes);
    assertEquals(Map.of(), capturedBanditLog.actionCategoricalAttributes);
    assertEquals(0.3333, capturedBanditLog.actionProbability, 0.0002);
    assertEquals("uninitialized", capturedBanditLog.modelVersion);
  }

  @Test
  public void testBanditModelActionLogging() {
    // Note: some of the passed in attributes are not used for scoring, but we do still want to make sure they are logged

    EppoAttributes subjectAttributes = new EppoAttributes(Map.of(
    "gender_identity", EppoValue.valueOf("female"),
    "days_since_signup", EppoValue.valueOf(130), // unused for scoring (which looks for account_age)
    "is_premium", EppoValue.valueOf(false), // unused for scoring
    "numeric_string", EppoValue.valueOf("123"), // unused for scoring
    "unpopulated", EppoValue.nullValue() // unused for scoring
    ));

    Map<String, EppoAttributes> actionsWithAttributes = Map.of(
      "nike", new EppoAttributes(Map.of(
        "brand_affinity", EppoValue.valueOf(0.25)
      )),
      "adidas", new EppoAttributes(Map.of(
        "brand_affinity", EppoValue.valueOf(0.1),
        "num_brand_purchases", EppoValue.valueOf(5), // unused for scoring
        "in_email_campaign", EppoValue.valueOf(true), // unused for scoring
        "also_unpopulated", EppoValue.nullValue() // unused for scoring
      )),
      "puma", new EppoAttributes(Map.of())
    );

    // Get our assigned action
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
      "subject2",
      "banner-bandit-experiment",
      subjectAttributes,
      actionsWithAttributes
    );

    // Verify assignment
    assertFalse(stringAssignment.isEmpty());
    assertEquals("adidas", stringAssignment.get());

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor = ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("banner-bandit-experiment-bandit", capturedAssignmentLog.experiment);
    assertEquals("banner-bandit-experiment", capturedAssignmentLog.featureFlag);
    assertEquals("bandit", capturedAssignmentLog.allocation);
    assertEquals("banner-bandit", capturedAssignmentLog.variation);
    assertEquals("subject2", capturedAssignmentLog.subject);
    assertEquals(subjectAttributes, capturedAssignmentLog.subjectAttributes);

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("banner-bandit-experiment-bandit", capturedBanditLog.experiment);
    assertEquals("banner-bandit", capturedBanditLog.variation);
    assertEquals("subject2", capturedBanditLog.subject);
    assertEquals("adidas", capturedBanditLog.action);
    assertEquals(0.2899, capturedBanditLog.actionProbability, 0.0002);
    assertEquals("falcon v123", capturedBanditLog.modelVersion);
    assertEquals(Map.of("days_since_signup", 130.0), capturedBanditLog.subjectNumericAttributes);
    assertEquals(Map.of(
      "gender_identity", "female",
      "is_premium", "false",
      "numeric_string", "123"
    ), capturedBanditLog.subjectCategoricalAttributes);
    assertEquals(Map.of(
      "brand_affinity", 0.1,
      "num_brand_purchases", 5.0
    ), capturedBanditLog.actionNumericAttributes);
    assertEquals(Map.of("in_email_campaign", "true"), capturedBanditLog.actionCategoricalAttributes);
  }

  @Test
  public void testBanditModelActionAssignmentFullContext() {
    EppoAttributes subjectAttributes = new EppoAttributes(Map.of(
      "gender_identity", EppoValue.valueOf("male"),
      "account_age", EppoValue.valueOf(3)
    ));

    Map<String, EppoAttributes> actionAttributes = Map.of(
      "nike", new EppoAttributes(Map.of(
          "brand_affinity", EppoValue.valueOf(0.05),
          "purchased_last_30_days", EppoValue.valueOf(true),
          "loyalty_tier", EppoValue.valueOf("gold")
        )),
      "adidas", new EppoAttributes(Map.of(
          "brand_affinity", EppoValue.valueOf(0.30),
          "purchased_last_30_days", EppoValue.valueOf(true),
          "loyalty_tier", EppoValue.valueOf("gold")
        )),
      "puma", new EppoAttributes(Map.of(
          "brand_affinity", EppoValue.valueOf(1.00),
          "purchased_last_30_days", EppoValue.valueOf(false),
          "loyalty_tier", EppoValue.valueOf("bronze")
        ))
    );

    // Get our assigned action
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
      "subject30",
      "banner-bandit-experiment",
      subjectAttributes,
      actionAttributes
    );

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertEquals("adidas", stringAssignment.get());
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals(0.8043, capturedBanditLog.actionProbability, 0.0002);
  }

  @Test
  public void testBanditModelActionAssignmentNoContext() {
    EppoAttributes subjectAttributes = new EppoAttributes();
    Set<String> actions = Set.of("nike", "adidas", "puma");

    // Get our assigned action
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
      "subject39",
      "banner-bandit-experiment",
      subjectAttributes,
      actions
    );

    // Verify assignment
    assertTrue(stringAssignment.isPresent());
    assertEquals("puma", stringAssignment.get());
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals(0.1613, capturedBanditLog.actionProbability, 0.0002);
  }

  @Test
  public void testBanditControlAction() {

    Set<String> banditActions = Set.of("option1", "option2", "option3");

    EppoAttributes subjectAttributes = new EppoAttributes(Map.of(
      "account_age", EppoValue.valueOf(90),
      "loyalty_tier", EppoValue.valueOf("gold"),
      "is_account_admin", EppoValue.valueOf(false)
    ));

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
            "subject10",
            "cold-start-bandit-experiment",
            subjectAttributes,
            banditActions
    );

    // Verify assignment
    assertFalse(stringAssignment.isEmpty());
    assertEquals("control", stringAssignment.get());

    // Manually log an action

    EppoAttributes controlActionAttributes = new EppoAttributes(Map.of(
      "brand", EppoValue.valueOf("skechers"),
      "num_past_purchases", EppoValue.valueOf(0),
      "has_promo_code", EppoValue.valueOf(true)
    ));

    Exception banditLoggingException = EppoClient.getInstance().logNonBanditAction(
      "subject10",
      "cold-start-bandit-experiment",
      subjectAttributes,
      "option0",
      controlActionAttributes
    );
    assertNull(banditLoggingException);

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor = ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedAssignmentLog.experiment);
    assertEquals("cold-start-bandit-experiment", capturedAssignmentLog.featureFlag);
    assertEquals("bandit", capturedAssignmentLog.allocation);
    assertEquals("control", capturedAssignmentLog.variation);
    assertEquals("subject10", capturedAssignmentLog.subject);
    assertEquals(subjectAttributes, capturedAssignmentLog.subjectAttributes);

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("cold-start-bandit-experiment-bandit", capturedBanditLog.experiment);
    assertEquals("control", capturedBanditLog.variation);
    assertEquals("subject10", capturedBanditLog.subject);
    assertEquals("option0", capturedBanditLog.action);
    assertNull(capturedBanditLog.actionProbability);
    assertNull(capturedBanditLog.modelVersion);
    assertEquals(Map.of("account_age", 90.0), capturedBanditLog.subjectNumericAttributes);
    assertEquals(Map.of("loyalty_tier", "gold", "is_account_admin", "false"), capturedBanditLog.subjectCategoricalAttributes);
    assertEquals(Map.of("num_past_purchases", 0.0), capturedBanditLog.actionNumericAttributes);
    assertEquals(Map.of("brand", "skechers", "has_promo_code", "true"), capturedBanditLog.actionCategoricalAttributes);
  }

  @Test
  public void testBanditNotInAllocation() {
    Set<String> banditActions = Set.of("option1", "option2", "option3");

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
            "subject2",
            "cold-start-bandit",
            new EppoAttributes(),
            banditActions
    );

    // Verify assignment
    assertTrue(stringAssignment.isEmpty());
  }
}
