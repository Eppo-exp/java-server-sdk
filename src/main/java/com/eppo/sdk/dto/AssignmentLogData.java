package com.eppo.sdk.dto;

import java.util.Date;

/**
 * Assignment Log Data Class
 */
public class AssignmentLogData {
    public static final String OVERRIDE_ALLOCATION_KEY = "override";

    public String experiment;
    public String featureFlag;
    public String allocation;
    public EppoValue variation;
    public Date timestamp;
    public String subject;
    public SubjectAttributes subjectAttributes;

    public AssignmentLogData(
            String experiment,
            String featureFlag,
            String allocation,
            EppoValue variation,
            String subject,
            SubjectAttributes subjectAttributes
    ) {
        this.experiment = experiment;
        this.featureFlag = featureFlag;
        this.allocation = allocation;
        this.variation = variation;
        this.timestamp = new Date();
        this.subject = subject;
        this.subjectAttributes = subjectAttributes;
    }
}
