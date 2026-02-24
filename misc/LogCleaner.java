import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LogCleaner {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java LogCleaner <directory> <days>");
            return;
        }

        String directoryPath = args[0];
        int days = Integer.parseInt(args[1]);

        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Directory not found: " + directoryPath);
            return;
        }

        Instant threshold = Instant.now().minus(days, ChronoUnit.DAYS);
        System.out.println("Cleaning files older than " + days + " days in " + directoryPath);

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        Instant lastModified = attr.lastModifiedTime().toInstant();
                        if (lastModified.isBefore(threshold)) {
                            if (file.delete()) {
                                System.out.println("Deleted: " + file.getName());
                            } else {
                                System.out.println("Failed to delete: " + file.getName());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}
