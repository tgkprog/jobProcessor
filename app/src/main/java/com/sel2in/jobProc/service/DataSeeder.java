package com.sel2in.jobProc.service;

import com.sel2in.jobProc.entity.AppParam;
import com.sel2in.jobProc.repo.AppParamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Map;

/**
 * Seeds the database with default AppParams on first run.
 * Supports --dbReset flag to wipe all tables and re-seed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final AppParamRepository appParamRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (args.containsOption("dbReset") || args.getNonOptionArgs().contains("--dbReset")) {
            log.warn("*** --dbReset detected: wiping all tables ***");
            resetDatabase();
            seed();
        } else if (appParamRepository.count() == 0) {
            log.info("Database empty - seeding default AppParams...");
            seed();
        } else {
            log.info("Database already has {} AppParams, skipping seed.", appParamRepository.count());
        }
    }

    private void resetDatabase() {
        String[] tables = {
            "JOB_ERROR", "OUTPUT_DATA_FILE", "OUTPUT_DATA_PARAM", "OUTPUT_DATA",
            "INPUT_DATA_FILE", "INPUT_DATA_PARAM", "INPUT_DATA",
            "JOB_PROCESSOR_INSTANCES", "JOB_PROCESSOR", "APP_PARAMS"
        };
        for (String table : tables) {
            try {
                entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
                log.info("  Cleared table: {}", table);
            } catch (Exception e) {
                // Table may not exist yet on first run, ignore
            }
        }
    }

    private void seed() {
        Map<String, String[]> defaults = Map.of(
            "numberOfThreads",       new String[]{"5",              "Thread pool size"},
            "processorJarDirectory", new String[]{"./processors",   "Directory for processor JARs"},
            "inputFileDirectory",    new String[]{"./inputFiles",   "Directory for input files"},
            "outputFileDirectory",   new String[]{"./outputFiles",  "Directory for output files"}
        );

        defaults.forEach((name, vals) -> {
            AppParam p = new AppParam();
            p.setName(name);
            p.setValue(vals[0]);
            p.setDescription(vals[1]);
            appParamRepository.save(p);
            log.info("  Seeded: {} = {}", name, vals[0]);
        });
    }
}
