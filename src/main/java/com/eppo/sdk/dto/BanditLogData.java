package com.eppo.sdk.dto;

import java.util.Date;
import java.util.Map;

/**
 * Assignment Log Data Class
 */
public class BanditLogData {
  public Date timestamp;

  public String experiment;
  public String banditKey;
  public String subject;
  public String action;
  public Double actionProbability;
  public String modelVersion;
  public Map<String, Double> subjectNumericAttributes;
  public Map<String, String> subjectCategoricalAttributes;
  public Map<String, Double> actionNumericAttributes;
  public Map<String, String> actionCategoricalAttributes;

  public BanditLogData(
    String experiment,
    String banditKey,
    String subject,
    String action,
    Double actionProbability,
    String modelVersion,
    Map<String, Double> subjectNumericAttributes,
    Map<String, String> subjectCategoricalAttributes,
    Map<String, Double> actionNumericAttributes,
    Map<String, String> actionCategoricalAttributes
  ) {
    this.timestamp = new Date();
    this.experiment = experiment;
    this.banditKey = banditKey;
    this.subject = subject;
    this.action = action;
    this.actionProbability = actionProbability;
    this.modelVersion = modelVersion;
    this.subjectNumericAttributes = subjectNumericAttributes;
    this.subjectCategoricalAttributes = subjectCategoricalAttributes;
    this.actionNumericAttributes = actionNumericAttributes;
    this.actionCategoricalAttributes = actionCategoricalAttributes;
  }
}
