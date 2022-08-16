package com.eppo.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.eppo.sdk.dto.AssignmentLogData;
import com.eppo.sdk.dto.EppoClientConfig;
import com.eppo.sdk.dto.IAssignmentLogger;
import com.eppo.sdk.dto.SubjectAttributes;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  static class AssignmentTestCase {
    String experiment;
    Map<String, SubjectAttributes> subjectAttributes;
    List<String> subjects;
    List<String> expectedAssignments;
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
    this.mockServer.stubFor(WireMock.get(WireMock.urlMatching(".*randomized_assignment.*")).willReturn(WireMock.okJson(racResponseJson)));
  }

  @AfterEach
  void teardown() {
    this.mockServer.stop();
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  void testAssignments(AssignmentTestCase testCase) throws IOException {
    List<String> assignments = testCase.subjects.stream().map(subject -> getAssignment(subject, testCase)).collect(Collectors.toList());
    assertEquals(testCase.expectedAssignments, assignments);
  }

  private String getAssignment(String subject, AssignmentTestCase testCase) {
    EppoClient client = EppoClient.getInstance();
    if (testCase.subjectAttributes != null && testCase.subjectAttributes.containsKey(subject)) {
      return client.getAssignment(subject, testCase.experiment, testCase.subjectAttributes.get(subject)).orElse(null);
    }
    return client.getAssignment(subject, testCase.experiment).orElse(null);
  }

  private static Stream<Arguments> getAssignmentTestData() throws IOException {
    File testCaseFolder = new File(EppoClientTest.class.getResource("resources/assignment/").getFile());
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
    File mockRacResponse = new File(EppoClientTest.class.getResource("resources/rac-experiments.json").getFile());
    try {
    return FileUtils.readFileToString(mockRacResponse, "UTF8");
    } catch (Exception e) {
      throw new RuntimeException("Error reading mock RAC data", e);
    }
  }
}
