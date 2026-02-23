package com.sel2in.jobProc.service;

import com.sel2in.jobProc.processor.JobProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProcessorLoader {

    private final Map<String, URLClassLoader> classLoaderCache = new ConcurrentHashMap<>();

    /**
     * Loads a JobProcessor instance from an external JAR file.
     * Uses a cache to reuse ClassLoaders for the same JAR path.
     *
     * @param jarPath           Path to the JAR file
     * @param className         Fully qualified class name
     * @param expectedChecksum  Optional SHA-256 checksum to verify
     */
    public JobProcessor load(String jarPath, String className, String expectedChecksum) {
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            String actual = calculateChecksum(jarPath);
            if (!expectedChecksum.equalsIgnoreCase(actual)) {
                log.error("Security Breach? Checksum mismatch for {}. Expected: {}, Actual: {}", jarPath, expectedChecksum, actual);
                throw new SecurityException("JAR checksum validation failed for " + jarPath);
            }
        }

        try {
            URLClassLoader loader = classLoaderCache.computeIfAbsent(jarPath, path -> {
                try {
                    File jarFile = new File(path);
                    if (!jarFile.exists()) {
                        throw new IllegalArgumentException("JAR file not found at: " + path);
                    }
                    log.info("Creating new ClassLoader for JAR: {}", path);
                    return new URLClassLoader(
                        new URL[]{jarFile.toURI().toURL()},
                        this.getClass().getClassLoader()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize ClassLoader for " + path, e);
                }
            });

            Class<?> clazz = Class.forName(className, true, loader);
            if (!JobProcessor.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(className + " does not implement " + JobProcessor.class.getName());
            }

            return (JobProcessor) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Failed to load processor {} from {}", className, jarPath, e);
            throw new RuntimeException("Processor loading failed: " + e.getMessage(), e);
        }
    }

    private String calculateChecksum(String path) {
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[8192];
            int length;
            while ((length = is.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate checksum for " + path, e);
        }
    }

    public void evictCache(String jarPath) {
        URLClassLoader loader = classLoaderCache.remove(jarPath);
        if (loader != null) {
            try {
                loader.close();
                log.info("Closed ClassLoader for JAR: {}", jarPath);
            } catch (Exception e) {
                log.warn("Failed to close ClassLoader for {}", jarPath, e);
            }
        }
    }
}
