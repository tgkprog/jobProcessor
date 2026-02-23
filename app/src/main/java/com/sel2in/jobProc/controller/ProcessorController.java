package com.sel2in.jobProc.controller;

import com.sel2in.jobProc.entity.ProcessorDefinition;
import com.sel2in.jobProc.repo.ProcessorRepository;
import com.sel2in.jobProc.service.ProcessorLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class ProcessorController {

    private static final String JAR_DIR = "./processors";

    private final ProcessorRepository processorRepository;
    private final ProcessorLoader processorLoader;

    @GetMapping("/listAll")
    public List<ProcessorDefinition> listAll() {
        return processorRepository.findAll();
    }

    @PostMapping("/add")
    public String add(@RequestBody ProcessorDefinition definition) {
        String className = definition.getClassName();
        
        // Validate className
        if (className == null || className.trim().isEmpty()) {
            return "Error: className cannot be empty";
        }
        if (className.contains("/") || className.contains("\\") || className.contains("..")) {
            return "Error: className contains invalid characters (/, \\, ..)";
        }
        
        processorRepository.findByClassName(className).ifPresent(existing -> {
            definition.setCreatedTs(existing.getCreatedTs());
        });
        processorRepository.save(definition);
        return "Processor saved successfully";
    }

    /**
     * Upload a processor JAR file and register it.
     * The JAR is saved to ./processors/ directory.
     *
     * @param file       The JAR file
     * @param className  Fully qualified class name of the JobProcessor implementation
     */
    @PostMapping("/uploadJar")
    public String uploadJar(@RequestParam("file") MultipartFile file,
                            @RequestParam String className) throws IOException, NoSuchAlgorithmException {
        if (file.isEmpty()) {
            return "Error: no file uploaded";
        }

        // Validate className
        if (className == null || className.trim().isEmpty()) {
            return "Error: className cannot be empty";
        }
        if (className.contains("/") || className.contains("\\") || className.contains("..")) {
            return "Error: className contains invalid characters (/, \\, ..)";
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".jar")) {
            return "Error: file must be a .jar";
        }

        // Save JAR to processors directory
        Path jarDir = Paths.get(JAR_DIR);
        Files.createDirectories(jarDir);
        Path jarPath = jarDir.resolve(originalName).toAbsolutePath();
        file.transferTo(jarPath.toFile());
        log.info("Uploaded JAR: {} ({} bytes)", jarPath, file.getSize());

        // Calculate Checksum (SHA-256)
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jarPath));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String checksum = hexString.toString();
        log.info("JAR Checksum: {}", checksum);

        // Evict old cached ClassLoader if re-uploading
        processorLoader.evictCache(jarPath.toString());

        // Register or update in DB
        String pathStr = jarPath.toString();
        ProcessorDefinition def = processorRepository.findByClassName(className)
                .orElse(new ProcessorDefinition());
        def.setClassName(className);
        def.setJarPath(pathStr);
        def.setChecksum(checksum);
        processorRepository.save(def);

        return "JAR uploaded and processor registered: " + className + " -> " + pathStr + " (sha256: " + checksum + ")";
    }

    @DeleteMapping("/remove/{className}")
    public String remove(@PathVariable String className) {
        processorRepository.findByClassName(className).ifPresent(proc -> {
            processorLoader.evictCache(proc.getJarPath());
            processorRepository.delete(proc);
        });
        return "Processor removed successfully";
    }
}
