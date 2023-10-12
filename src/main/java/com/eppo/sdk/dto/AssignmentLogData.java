package com.eppo.sdk.dto;

import java.util.Date;

/**
 * Assignment Log Data Class
 */
public class AssignmentLogData {
    public String experiment;
    public String featureFlag;
    public String allocation;
    public String variation;
    public Date timestamp;
    public String subject;
    public EppoAttributes subjectAttributes;

    public AssignmentLogData(
            String experiment,
            String featureFlag,
            String allocation,
            String variation,
            String subject,
            EppoAttributes subjectAttributes
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
