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

  @BeforeEach
  void init() {
    setupMockRacServer("src/test/resources/rac-experiments-v3.json");
    EppoClientConfig config = EppoClientConfig.builder()
        .apiKey("mock-api-key")
        .baseURL("http://localhost:4001")
        .assignmentLogger(mock())
        .build();
    EppoClient.init(config);
  }

  private void setupMockRacServer(String jsonToReturnFilePath) {
    this.mockServer = new WireMockServer(TEST_PORT);
    this.mockServer.start();
    String racResponseJson = getMockRandomizedAssignmentResponse(jsonToReturnFilePath);
    this.mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*")).willReturn(WireMock.okJson(racResponseJson)));
  }

  private void addBanditRacToMockServer(WireMockServer server, String banditJsonFilePath) {
    String racResponseJson = getMockRandomizedAssignmentResponse(banditJsonFilePath);
    server.stubFor(
      WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/bandits\\?.*"))
        .willReturn(WireMock.okJson(racResponseJson))
    );
  }

  @AfterEach
  void teardown() {
    this.mockServer.stop();
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  void testAssignments(AssignmentTestCase testCase) throws IOException {
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
  public void testBanditBanditAction() {
    // For now, use our special bandits RAC until we fold it into the shared test case suite
    String racResponseJson = getMockRandomizedAssignmentResponse("src/test/resources/bandits/rac-experiments-bandits-beta.json");
    this.mockServer.stubFor(
      WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*")).willReturn(WireMock.okJson(racResponseJson))
    );
    this.addBanditRacToMockServer(this.mockServer, "src/test/resources/bandits/bandits-parameters-1.json");

    // Re-initialize client with our bandit RAC and a mock logger we can spy on
    IAssignmentLogger mockAssignmentLogger = mock();
    IBanditLogger mockBanditLogger = mock();
    EppoClientConfig config = EppoClientConfig.builder()
      .apiKey("mock-api-key")
      .baseURL("http://localhost:4001")
      .assignmentLogger(mockAssignmentLogger)
      .banditLogger(mockBanditLogger)
      .build();
    EppoClient.init(config);

    Set<String> banditActions = Set.of("option1", "option2", "option3");

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
      "subject1",
      "test_bandit_1",
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
    assertEquals("test_bandit_1-bandit", capturedAssignmentLog.experiment);
    assertEquals("test_bandit_1", capturedAssignmentLog.featureFlag);
    assertEquals("bandit", capturedAssignmentLog.allocation);
    assertEquals("banner-bandit", capturedAssignmentLog.variation);
    assertEquals("subject1", capturedAssignmentLog.subject);
    assertEquals(Map.of(), capturedAssignmentLog.subjectAttributes);

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("test_bandit_1-bandit", capturedBanditLog.experiment);
    assertEquals("banner-bandit", capturedBanditLog.variation);
    assertEquals("subject1", capturedBanditLog.subject);
    assertEquals(Map.of(), capturedBanditLog.subjectAttributes);
    assertEquals("option3", capturedBanditLog.action);
    assertEquals(Map.of(), capturedBanditLog.actionAttributes);
    assertEquals(0.3333, capturedBanditLog.actionProbability, 0.0002);
    assertEquals("random 0.1", capturedBanditLog.modelVersion);
  }

  @Test
  public void testBanditControlAction() {
    // For now, use our special bandits RAC until we fold it into the shared test case suite
    String racResponseJson = getMockRandomizedAssignmentResponse("src/test/resources/bandits/rac-experiments-bandits-beta.json");
    this.mockServer.stubFor(
            WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*")).willReturn(WireMock.okJson(racResponseJson))
    );
    this.addBanditRacToMockServer(this.mockServer, "src/test/resources/bandits/bandits-parameters-1.json");

    // Re-initialize client with our bandit RAC and a mock logger we can spy on
    IAssignmentLogger mockAssignmentLogger = mock();
    IBanditLogger mockBanditLogger = mock();
    EppoClientConfig config = EppoClientConfig.builder()
            .apiKey("mock-api-key")
            .baseURL("http://localhost:4001")
            .assignmentLogger(mockAssignmentLogger)
            .banditLogger(mockBanditLogger)
            .build();
    EppoClient.init(config);

    Set<String> banditActions = Set.of("option1", "option2", "option3");

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
            "subject8",
            "test_bandit_1",
            new EppoAttributes(),
            banditActions
    );

    // Verify assignment
    assertFalse(stringAssignment.isEmpty());
    assertEquals("control", stringAssignment.get());

    // Manually log an action
    EppoClient.getInstance().logNonBanditAction(
      "subject8",
      "test_bandit_1",
      new EppoAttributes(),
      "option0",
      new EppoAttributes()
    );

    // Verify experiment assignment log
    ArgumentCaptor<AssignmentLogData> assignmentLogCaptor = ArgumentCaptor.forClass(AssignmentLogData.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    AssignmentLogData capturedAssignmentLog = assignmentLogCaptor.getValue();
    assertEquals("test_bandit_1-bandit", capturedAssignmentLog.experiment);
    assertEquals("test_bandit_1", capturedAssignmentLog.featureFlag);
    assertEquals("bandit", capturedAssignmentLog.allocation);
    assertEquals("control", capturedAssignmentLog.variation);
    assertEquals("subject8", capturedAssignmentLog.subject);
    assertEquals(Map.of(), capturedAssignmentLog.subjectAttributes);

    // Verify bandit log
    ArgumentCaptor<BanditLogData> banditLogCaptor = ArgumentCaptor.forClass(BanditLogData.class);
    verify(mockBanditLogger, times(1)).logBanditAction(banditLogCaptor.capture());
    BanditLogData capturedBanditLog = banditLogCaptor.getValue();
    assertEquals("test_bandit_1-bandit", capturedBanditLog.experiment);
    assertEquals("control", capturedBanditLog.variation);
    assertEquals("subject8", capturedBanditLog.subject);
    assertEquals(Map.of(), capturedBanditLog.subjectAttributes);
    assertEquals("option0", capturedBanditLog.action);
    assertEquals(Map.of(), capturedBanditLog.actionAttributes);
    assertNull(capturedBanditLog.actionProbability);
    assertNull(capturedBanditLog.modelVersion);
  }

  @Test
  public void testBanditNotInAllocation() {
    // For now, use our special bandits RAC until we fold it into the shared test case suite
    String racResponseJson = getMockRandomizedAssignmentResponse("src/test/resources/bandits/rac-experiments-bandits-beta.json");
    this.mockServer.stubFor(
            WireMock.get(WireMock.urlMatching(".*randomized_assignment/v3/config\\?.*")).willReturn(WireMock.okJson(racResponseJson))
    );
    this.addBanditRacToMockServer(this.mockServer, "src/test/resources/bandits/bandits-parameters-1.json");

    // Re-initialize client with our bandit RAC and a mock logger we can spy on
    EppoClientConfig config = EppoClientConfig.builder()
            .apiKey("mock-api-key")
            .baseURL("http://localhost:4001")
            .assignmentLogger(mock())
            .banditLogger(mock())
            .build();
    EppoClient.init(config);

    Set<String> banditActions = Set.of("option1", "option2", "option3");

    // Attempt to get a bandit assignment
    Optional<String> stringAssignment = EppoClient.getInstance().getStringAssignment(
            "subject2",
            "test_bandit_1",
            new EppoAttributes(),
            banditActions
    );

    // Verify assignment
    assertTrue(stringAssignment.isEmpty());
  }
}
