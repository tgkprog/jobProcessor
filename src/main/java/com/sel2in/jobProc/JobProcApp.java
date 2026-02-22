package com.sel2in.jobProc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application
 *
 * Author: Tushar Kapila
 *
 */
@SpringBootApplication
public class JobProcApp {

    public static void main(String[] args) {

        SpringApplication.run(JobProcApp.class, args);

        System.out.println("Job Processor Engine Started");

    }

}