package com.interviewassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InterviewAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewAssistantApplication.class, args);
    }
}
