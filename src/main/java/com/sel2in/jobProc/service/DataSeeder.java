package com.sel2in.jobProc.service;

import com.sel2in.jobProc.entity.AppParam;
import com.sel2in.jobProc.repo.AppParamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Seeds the database with default AppParams on first run.
 * Skips silently if data already exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final AppParamRepository appParamRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (appParamRepository.count() == 0) {
            log.info("Database empty - seeding default AppParams...");
            seed();
        } else {
            log.info("Database already has {} AppParams, skipping seed.", appParamRepository.count());
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
