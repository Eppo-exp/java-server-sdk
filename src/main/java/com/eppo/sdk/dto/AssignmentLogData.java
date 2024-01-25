package com.eppo.sdk.dto;

import java.util.Date;

/**
 * Assignment Log Data Class
 */
public class AssignmentLogData {
    public static final String OVERRIDE_ALLOCATION_KEY = "override";

    public static final String OVERRIDE_ASSIGNMENT_NAME = "override_assignment";

    public String experiment;
    public String featureFlag;
    public String allocation;
    public String variation;
    public String variationName;
    public EppoValue variationValue;
    public Date timestamp;
    public String subject;
    public SubjectAttributes subjectAttributes;

    public AssignmentLogData(
            String experiment,
            String featureFlag,
            String allocation,
            String variationName,
            EppoValue variationValue,
            String subject,
            SubjectAttributes subjectAttributes
    ) {
        this.experiment = experiment;
        this.featureFlag = featureFlag;
        this.allocation = allocation;
        this.variationName = variationName;
        this.variationValue = variationValue;
        this.variation = variationValue.stringValue();
        this.timestamp = new Date();
        this.subject = subject;
        this.subjectAttributes = subjectAttributes;
    }
}
