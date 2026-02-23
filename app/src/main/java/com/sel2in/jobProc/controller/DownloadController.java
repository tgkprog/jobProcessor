package com.sel2in.jobProc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serves files and directory listings from the outputFiles directory
 * under the /dwn/ URL path.
 *
 *   GET /dwn/          → list job output folders
 *   GET /dwn/5/        → list files in job 5's output
 *   GET /dwn/5/report.html → serve/download the file
 */
@Slf4j
@Controller
@RequestMapping("/dwn")
public class DownloadController {

    private static final String OUTPUT_DIR = "./outputFiles";
    private static final String INPUT_DIR = "./inputFiles";

    /**
     * Root listing: show all job output folders.
     */
    @GetMapping({"", "/"})
    @ResponseBody
    public ResponseEntity<String> listRoot() throws IOException {
        Path root = Paths.get(OUTPUT_DIR);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return ResponseEntity.ok(buildHtml("Output Files", "<p>No output files yet.</p>"));
        }

        List<Path> dirs;
        try (var stream = Files.list(root)) {
            dirs = stream.filter(Files::isDirectory)
                         .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                         .collect(Collectors.toList());
        }

        StringBuilder rows = new StringBuilder();
        for (Path dir : dirs) {
            String name = dir.getFileName().toString();
            long count = 0;
            try (var s = Files.list(dir)) { count = s.count(); }
            rows.append("<tr>")
                .append("<td><a href=\"/dwn/").append(name).append("/\">📁 Job ").append(name).append("</a></td>")
                .append("<td>").append(count).append(" file(s)</td>")
                .append("</tr>\n");
        }

        String body = dirs.isEmpty()
                ? "<p>No output files yet.</p>"
                : "<table><tr><th>Job Output</th><th>Files</th></tr>\n" + rows + "</table>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildHtml("Output Files", body));
    }

    /**
     * Job folder listing: show files in a specific job's output.
     */
    @GetMapping("/{jobId}/")
    @ResponseBody
    public ResponseEntity<String> listJob(@PathVariable String jobId) throws IOException {
        Path jobDir = Paths.get(OUTPUT_DIR, jobId);
        if (!Files.exists(jobDir) || !Files.isDirectory(jobDir)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildHtml("Not Found", "<p>No output folder for job " + esc(jobId) + ".</p>"));
        }

        List<Path> files;
        try (var stream = Files.list(jobDir)) {
            files = stream.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                          .collect(Collectors.toList());
        }

        StringBuilder rows = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#,##0");
        for (Path file : files) {
            String name = file.getFileName().toString();
            boolean isDir = Files.isDirectory(file);
            long size = isDir ? 0 : Files.size(file);
            String icon = isDir ? "📁" : guessIcon(name);
            String link = isDir
                    ? "/dwn/" + jobId + "/" + name + "/"
                    : "/dwn/" + jobId + "/" + name;
            rows.append("<tr>")
                .append("<td><a href=\"").append(link).append("\">").append(icon).append(" ").append(esc(name)).append("</a></td>")
                .append("<td>").append(isDir ? "—" : df.format(size) + " bytes").append("</td>")
                .append("</tr>\n");
        }

        String nav = "<p><a href=\"/dwn/\">← Back to all jobs</a></p>\n";
        String body = files.isEmpty()
                ? nav + "<p>No files in this folder.</p>"
                : nav + "<table><tr><th>File</th><th>Size</th></tr>\n" + rows + "</table>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildHtml("Job " + esc(jobId) + " — Output Files", body));
    }

    /**
     * Serve a file from a job's output folder.
     */
    @GetMapping("/{jobId}/{fileName:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String jobId,
                                              @PathVariable String fileName) {
        // Prevent path traversal
        if (jobId.contains("..") || fileName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(OUTPUT_DIR, jobId, fileName);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(fileName);
        boolean isInline = contentType.startsWith("text/") || contentType.equals("application/pdf")
                || contentType.startsWith("image/");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (isInline ? "inline" : "attachment") + "; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }

    /**
     * Serve a file from a job's input folder.
     */
    @GetMapping("/input/{jobId}/{fileName:.+}")
    public ResponseEntity<Resource> serveInputFile(@PathVariable String jobId,
                                                   @PathVariable String fileName) {
        if (jobId.contains("..") || fileName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(INPUT_DIR, jobId, fileName);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }

    // ── Helpers ──

    private String guessContentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".txt") || lower.endsWith(".log")) return "text/plain";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    private String guessIcon(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "🌐";
        if (lower.endsWith(".csv") || lower.endsWith(".txt") || lower.endsWith(".log")) return "📄";
        if (lower.endsWith(".pdf")) return "📕";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".svg")) return "🖼";
        if (lower.endsWith(".json") || lower.endsWith(".xml")) return "📋";
        if (lower.endsWith(".zip") || lower.endsWith(".jar")) return "📦";
        return "📄";
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String buildHtml(String title, String bodyContent) {
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
             + "<meta charset=\"UTF-8\">\n"
             + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
             + "<title>" + title + " — Job Processor</title>\n"
             + "<link rel=\"stylesheet\" href=\"/res/main.css\">\n"
             + "<style>\n"
             + "body { max-width: 900px; margin: 2rem auto; padding: 0 1rem; }\n"
             + "table { margin-top: 1rem; }\n"
             + "td a { text-decoration: none; font-weight: 600; }\n"
             + "td a:hover { text-decoration: underline; }\n"
             + "</style>\n"
             + "</head>\n<body>\n"
             + "<h1>" + title + "</h1>\n"
             + bodyContent
             + "\n</body>\n</html>\n";
    }
}
