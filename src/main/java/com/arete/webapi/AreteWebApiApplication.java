package com.arete.webapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AreteWebApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AreteWebApiApplication.class, args);
    }
}
