package com.example.taskhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TaskHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskHubApplication.class, args);
    }
}
