package com.example.personalassistant.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PersonalAssistantServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalAssistantServerApplication.class, args);
    }
}
