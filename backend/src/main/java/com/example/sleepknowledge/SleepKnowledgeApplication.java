package com.example.sleepknowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SleepKnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SleepKnowledgeApplication.class, args);
    }
}
