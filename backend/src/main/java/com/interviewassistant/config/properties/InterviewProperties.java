package com.interviewassistant.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "interview")
public class InterviewProperties {

    @Min(1)
    @Max(20)
    private int defaultQuestionCount = 5;

    @Min(1)
    @Max(50)
    private int maxQuestionCount = 10;

    public int getDefaultQuestionCount() {
        return defaultQuestionCount;
    }

    public void setDefaultQuestionCount(int defaultQuestionCount) {
        this.defaultQuestionCount = defaultQuestionCount;
    }

    public int getMaxQuestionCount() {
        return maxQuestionCount;
    }

    public void setMaxQuestionCount(int maxQuestionCount) {
        this.maxQuestionCount = maxQuestionCount;
    }
}
