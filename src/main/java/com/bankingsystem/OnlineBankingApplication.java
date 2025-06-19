package com.bankingsystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OnlineBankingApplication {


    private static final Logger logger = LoggerFactory.getLogger(OnlineBankingApplication.class);

    public static void main(String[] args) {
		SpringApplication.run(OnlineBankingApplication.class, args);
        logger.info("🚀 Online Banking Application started successfully!");
    }

}
