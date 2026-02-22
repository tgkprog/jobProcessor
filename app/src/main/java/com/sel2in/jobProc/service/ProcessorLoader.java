package com.sel2in.jobProc.service;

import com.sel2in.jobProc.processor.JobProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProcessorLoader {

    private final Map<String, URLClassLoader> classLoaderCache = new ConcurrentHashMap<>();

    /**
     * Loads a JobProcessor instance from an external JAR file.
     * Uses a cache to reuse ClassLoaders for the same JAR path.
     */
    public JobProcessor load(String jarPath, String className) {
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
            throw new RuntimeException("Processor loading failed", e);
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
