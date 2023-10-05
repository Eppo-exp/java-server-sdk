package com.eppo.sdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.eppo.sdk.dto.*;
import com.eppo.sdk.exception.ExperimentConfigurationNotFound;
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
import java.util.Optional;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import lombok.Data;

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
    SubjectAttributes subjectAttributes;
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
    setupMockRacServer();
    EppoClientConfig config = EppoClientConfig.builder()
        .apiKey("mock-api-key")
        .baseURL("http://localhost:4001")
        .assignmentLogger(new IAssignmentLogger() {
          @Override
          public void logAssignment(AssignmentLogData logData) {
            // Auto-generated method stub
          }
        })
        .build();
    EppoClient.init(config);
  }

  private void setupMockRacServer() {
    this.mockServer = new WireMockServer(TEST_PORT);
    this.mockServer.start();
    String racResponseJson = getMockRandomizedAssignmentResponse();
    this.mockServer.stubFor(
        WireMock.get(WireMock.urlMatching(".*randomized_assignment.*")).willReturn(WireMock.okJson(racResponseJson)));
  }

  @AfterEach
  void teardown() {
    this.mockServer.stop();
  }

  @Test()
  void testGracefulModeOn() {
    EppoClientConfig config = EppoClientConfig.builder()
        .apiKey("mock-api-key")
        .baseURL("http://localhost:4001")
        .isGracefulMode(true)
        .assignmentLogger(new IAssignmentLogger() {
          @Override
          public void logAssignment(AssignmentLogData logData) {
            // Auto-generated method stub
          }
        })
        .build();
    EppoClient.init(config);
    EppoClient realClient = EppoClient.getInstance();

    EppoClient spyClient = spy(realClient);

    doThrow(new ExperimentConfigurationNotFound("Exception thrown by mock")).when(spyClient)
        .getAssignmentValue(anyString(),
            anyString(), any(SubjectAttributes.class));

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
    EppoClientConfig config = EppoClientConfig.builder()
        .apiKey("mock-api-key")
        .baseURL("http://localhost:4001")
        .isGracefulMode(false)
        .assignmentLogger(new IAssignmentLogger() {
          @Override
          public void logAssignment(AssignmentLogData logData) {
            // Auto-generated method stub
          }
        })
        .build();
    EppoClient.init(config);
    EppoClient realClient = EppoClient.getInstance();

    EppoClient spyClient = spy(realClient);

    doThrow(new ExperimentConfigurationNotFound("Exception thrown by mock")).when(spyClient).getAssignmentValue(
        anyString(),
        anyString(), any(SubjectAttributes.class));

    assertThrows(ExperimentConfigurationNotFound.class,
        () -> spyClient.getBooleanAssignment("subject1", "experiment1"));
    assertThrows(ExperimentConfigurationNotFound.class, () -> spyClient.getDoubleAssignment("subject1", "experiment1"));
    assertThrows(ExperimentConfigurationNotFound.class,
        () -> spyClient.getParsedJSONAssignment("subject1", "experiment1"));
    assertThrows(ExperimentConfigurationNotFound.class,
        () -> spyClient.getJSONStringAssignment("subject1", "experiment1"));
    assertThrows(ExperimentConfigurationNotFound.class, () -> spyClient.getStringAssignment("subject1", "experiment1"));
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

  private static String getMockRandomizedAssignmentResponse() {
    File mockRacResponse = new File("src/test/resources/rac-experiments-v3.json");
    try {
      return FileUtils.readFileToString(mockRacResponse, "UTF8");
    } catch (Exception e) {
      throw new RuntimeException("Error reading mock RAC data", e);
    }
  }
}
