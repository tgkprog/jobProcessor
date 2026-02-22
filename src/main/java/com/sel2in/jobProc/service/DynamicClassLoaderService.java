package com.sel2in.jobProc.service;

import com.sel2in.jobProc.processor.IJobProcessor;
import org.springframework.stereotype.Service;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

@Service
public class DynamicClassLoaderService {

    private final Map<String, URLClassLoader> cache = new HashMap<>();

    public IJobProcessor loadProcessor(String jarPath, String className) throws Exception {
        URLClassLoader loader;
        if (cache.containsKey(jarPath)) {
            loader = cache.get(jarPath);
        } else {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                throw new RuntimeException("JAR file not found: " + jarPath);
            }
            loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());
            cache.put(jarPath, loader);
        }

        Class<?> clazz = Class.forName(className, true, loader);
        return (IJobProcessor) clazz.getDeclaredConstructor().newInstance();
    }
    
    public void clearCache(String jarPath) {
        cache.remove(jarPath);
    }
}
