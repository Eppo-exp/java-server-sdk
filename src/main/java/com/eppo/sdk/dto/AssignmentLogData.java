package com.eppo.sdk.dto;

import java.util.Date;

/**
 * Assignment Log Data Class
 */
public class AssignmentLogData {
    public String experiment;
    public String variation;
    public Date timestamp;
    public String subject;
    public SubjectAttributes subjectAttributes;

    public AssignmentLogData(String experiment, String variation, String subject, SubjectAttributes subjectAttributes) {
        this.experiment = experiment;
        this.variation = variation;
        this.timestamp = new Date();
        this.subject = subject;
        this.subjectAttributes = subjectAttributes;
    }
}
