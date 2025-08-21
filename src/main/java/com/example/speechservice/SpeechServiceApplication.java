package com.example.speechservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpeechServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeechServiceApplication.class, args);
    }
}
