package com.eppo.sdk.dto;

import java.util.Date;

/**
 * Assignment Log Data Class
 */
public class AssignmentLogData {
    public String experiment;
    public String featureFlag;
    public String assignmentModelVersion;
    public String allocation;
    public String variation;
    public Float variationProbability;
    public EppoAttributes variationAttributes;
    public Date timestamp;
    public String subject;
    public EppoAttributes subjectAttributes;


    public AssignmentLogData(
            String experiment,
            String featureFlag,
            String assignmentModelVersion,
            String allocation,
            String variation,
            Float variationProbability,
            EppoAttributes variationAttributes,
            String subject,
            EppoAttributes subjectAttributes
    ) {
        this.experiment = experiment;
        this.featureFlag = featureFlag;
        this.assignmentModelVersion = assignmentModelVersion;
        this.allocation = allocation;
        this.variation = variation;
        this.variationProbability = variationProbability;
        this.variationAttributes = variationAttributes;
        this.timestamp = new Date();
        this.subject = subject;
        this.subjectAttributes = subjectAttributes;
    }
}
