package com.example.smallapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmallAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmallAppApplication.class, args);
	}

}
