package com.eppo.sdk.dto;

import java.util.Date;

/**
 * Assignment Log Data Class
 */
public class BanditLogData {
    public String experiment;
    public String variation;
    public String subject;
    public EppoAttributes subjectAttributes;
    public String action;
    public EppoAttributes actionAttributes;
    public Float actionProbability;
    public String modelVersion;
    public Date timestamp;

    public BanditLogData(
            String experiment,
            String variation,
            String subject,
            EppoAttributes subjectAttributes,
            String action,
            EppoAttributes actionAttributes,
            Float actionProbability,
            String modelVersion
    ) {
        this.experiment = experiment;
        this.variation = variation;
        this.subject = subject;
        this.subjectAttributes = subjectAttributes;
        this.action = action;
        this.actionAttributes = actionAttributes;
        this.actionProbability = actionProbability;
        this.modelVersion = modelVersion;
        this.timestamp = new Date();
    }
}
