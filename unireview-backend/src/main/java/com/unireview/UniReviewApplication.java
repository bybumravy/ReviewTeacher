package com.unireview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UniReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniReviewApplication.class, args);
    }
}
