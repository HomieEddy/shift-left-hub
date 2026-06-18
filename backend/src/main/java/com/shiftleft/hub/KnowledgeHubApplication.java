package com.shiftleft.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Shift-Left Knowledge Hub application.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class KnowledgeHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeHubApplication.class, args);
    }

}
