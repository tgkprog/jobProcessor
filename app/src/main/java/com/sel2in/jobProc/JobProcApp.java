package com.sel2in.jobProc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.*;

/**
 * Main Application - Job Processor Engine
 * Author: Tushar Kapila
 *
 * Startup:
 *  1. Create working directories (data, processors, inputFiles, outputFiles)
 *  2. Spring Boot starts, Hibernate creates/updates tables (ddl-auto: update)
 *  3. DataSeeder runs to populate AppParams if empty
 */
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class JobProcApp {
    private static final Logger logger = LoggerFactory.getLogger(JobProcApp.class);

    public static void main(String[] args) {
        logger.info("=== Job Processor Engine ===");

        try {
            preLoad();
        } catch (Exception e) {
            logger.warn("Pre-load warning: {}", e.getMessage());
        }

        SpringApplication.run(JobProcApp.class, args);
        logger.info("Job Processor Engine is ready on port 8087");
    }

    /**
     * Pre-Spring filesystem setup.
     * DB is handled by Hibernate (ddl-auto: update) + DataSeeder bean.
     */
    static void preLoad() throws Exception {
        String[] dirs = {"./data", "./processors", "./inputFiles", "./outputFiles"};
        for (String dir : dirs) {
            Files.createDirectories(Paths.get(dir));
        }
        logger.info("  Working directories OK.");
    }
}