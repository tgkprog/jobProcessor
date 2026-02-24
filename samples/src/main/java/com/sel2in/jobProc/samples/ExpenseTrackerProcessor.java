package com.sel2in.jobProc.samples;
//com.sel2in.jobProc.samples.ExpenseTrackerProcessor
import com.sel2in.jobProc.processor.InputData;
import com.sel2in.jobProc.processor.JobEstimate;
import com.sel2in.jobProc.processor.JobProcessor;
import com.sel2in.jobProc.processor.OutputData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExpenseTrackerProcessor — A JobProcessor that:
 *   1. Reads CSV or pipe-separated expense files from inputFiles.
 *   2. Auto-categorizes each expense based on keyword matching in the description.
 *   3. Produces a standalone HTML report w/ inline SVG bar chart + donut chart.
 *
 * Expected CSV columns (header optional):
 *   date, description, amount
 * Separator: auto-detected (comma or pipe '|').
 *
 * Categories and their trigger keywords (case-insensitive):
 *   Housing      → rent, lease, housing, "home emi", mortgage, property tax
 *   Food         → food, grocery, groceries, restaurant, dining, cafe, bakery, takeout, swiggy, zomato
 *   Utilities    → electricity, water, gas, internet, broadband, wifi, phone, mobile, recharge
 *   Transport    → fuel, petrol, diesel, uber, ola, taxi, bus, metro, parking, toll
 *   Healthcare   → medical, medicine, pharmacy, hospital, doctor, health, insurance
 *   Education    → school, college, tuition, books, course, training, education
 *   Entertainment→ movie, netflix, spotify, subscription, game, concert, streaming
 *   Shopping     → amazon, flipkart, clothing, shoes, electronics, furniture, appliance
 *   Other        → anything that doesn't match above
 *
 * Output:
 *   An HTML file saved to ./outputFiles/<jobId>/expense_report.html
 */
public class ExpenseTrackerProcessor implements JobProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseTrackerProcessor.class);

    // ──────────────── Category ↔ keywords mapping ────────────────
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put("Housing",       Arrays.asList("rent", "lease", "housing", "home emi", "mortgage", "property tax"));
        CATEGORY_KEYWORDS.put("Food",          Arrays.asList("food", "grocery", "groceries", "restaurant", "dining", "cafe", "bakery", "takeout", "swiggy", "zomato", "lunch", "dinner", "breakfast"));
        CATEGORY_KEYWORDS.put("Utilities",     Arrays.asList("electricity", "water bill", "gas bill", "internet", "broadband", "wifi", "phone", "mobile", "recharge", "utility"));
        CATEGORY_KEYWORDS.put("Transport",     Arrays.asList("fuel", "petrol", "diesel", "uber", "ola", "taxi", "bus", "metro", "parking", "toll", "travel"));
        CATEGORY_KEYWORDS.put("Healthcare",    Arrays.asList("medical", "medicine", "pharmacy", "hospital", "doctor", "health", "insurance", "clinic"));
        CATEGORY_KEYWORDS.put("Education",     Arrays.asList("school", "college", "tuition", "books", "course", "training", "education", "udemy"));
        CATEGORY_KEYWORDS.put("Entertainment", Arrays.asList("movie", "netflix", "spotify", "subscription", "game", "concert", "streaming", "hotstar", "prime video"));
        CATEGORY_KEYWORDS.put("Shopping",      Arrays.asList("amazon", "flipkart", "clothing", "shoes", "electronics", "furniture", "appliance", "mall"));
    }

    // Colors for each category (inclusive of "Other")
    private static final String[] CHART_COLORS = {
        "#6366f1", // Housing  – indigo
        "#f59e0b", // Food     – amber
        "#10b981", // Utilities– emerald
        "#3b82f6", // Transport– blue
        "#ef4444", // Healthcare – red
        "#8b5cf6", // Education– violet
        "#ec4899", // Entertainment – pink
        "#14b8a6", // Shopping – teal
        "#78716c"  // Other    – stone
    };

    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");

    // ──────────────── JobProcessor contract ────────────────

    @Override
    public JobEstimate reviewJob(InputData inputData) {
        long baseEstimateMs = 60_000; // 60 seconds base
        
        // Add 105% of sleep parameter if present
        Map<String, Object> params = inputData.getParameters();
        if (params != null && params.containsKey("sleep")) {
            try {
                long sleepMs = Long.parseLong(params.get("sleep").toString());
                long sleepOverhead = (long)(sleepMs * 1.05);
                baseEstimateMs += sleepOverhead;
                logger.info("ExpenseTracker: Added {}ms (105% of sleep) to estimate", sleepOverhead);
            } catch (NumberFormatException e) {
                // Ignore invalid sleep values
            }
        }
        
        return new JobEstimate(baseEstimateMs);
    }

    @Override
    public OutputData processJob(InputData inputData) {
        logger.info("═══════════════════════════════════════════════════════");
        logger.info("ExpenseTrackerProcessor VERSION 001");
        logger.info("═══════════════════════════════════════════════════════");
        logger.info("ExpenseTracker: Starting expense analysis for job: {}", inputData.getJobName());
        
        // Debug: Print input parameters
        Map<String, Object> inputParams = inputData.getParameters();
        logger.info("ExpenseTracker: Checking for input parameters...");
        if (inputParams != null && !inputParams.isEmpty()) {
            logger.info("ExpenseTracker: Received {} input parameter(s):", inputParams.size());
            for (Map.Entry<String, Object> entry : inputParams.entrySet()) {
                logger.info("  - {} = {} ({})", entry.getKey(), entry.getValue(), entry.getValue().getClass().getSimpleName());
            }
        } else {
            logger.info("ExpenseTracker: No input parameters received.");
        }

        OutputData output = new OutputData();
        output.setInputDataId(inputData.getInputDataId());

        List<String> inputFiles = inputData.getInputFiles();
        if (inputFiles == null || inputFiles.isEmpty()) {
            output.setStatus("FAILED");
            output.setMainErrorCode("NO_INPUT");
            output.setMainErrorReason("No input files provided. Expected CSV or pipe-delimited expense files.");
            return output;
        }

        try {
            // ── 1. Parse all input files ──
            List<ExpenseEntry> allEntries = new ArrayList<>();
            for (String filePath : inputFiles) {
                allEntries.addAll(parseFile(filePath));
            }

            if (allEntries.isEmpty()) {
                output.setStatus("FAILED");
                output.setMainErrorCode("EMPTY_DATA");
                output.setMainErrorReason("Could not parse any expense entries from the provided files.");
                return output;
            }

            // ── 2. Categorize ──
            Map<String, Double> categoryTotals = new LinkedHashMap<>();
            Map<String, List<ExpenseEntry>> categoryEntries = new LinkedHashMap<>();
            for (String cat : CATEGORY_KEYWORDS.keySet()) {
                categoryTotals.put(cat, 0.0);
                categoryEntries.put(cat, new ArrayList<>());
            }
            categoryTotals.put("Other", 0.0);
            categoryEntries.put("Other", new ArrayList<>());

            for (ExpenseEntry e : allEntries) {
                String cat = categorize(e.description);
                categoryTotals.merge(cat, e.amount, Double::sum);
                categoryEntries.get(cat).add(e);
            }

            double grandTotal = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

            // ── 3. Generate HTML report ──
            String html = buildHtmlReport(allEntries, categoryTotals, categoryEntries, grandTotal,
                    inputFiles.size(), inputData.getJobName());

            // ── 4. Save report ──
            Path outputDir = Paths.get("./outputFiles", String.valueOf(inputData.getInputDataId()));
            Files.createDirectories(outputDir);
            Path reportPath = outputDir.resolve("expense_report.html");
            Files.write(reportPath, html.getBytes("UTF-8"));

            logger.info("ExpenseTracker: Report saved to {}", reportPath.toAbsolutePath());

            // ── 5. Return output ──
            output.setStatus("SUCCESS");
            output.setOutputNote("Processed " + allEntries.size() + " expense entries across " +
                    inputFiles.size() + " file(s). Total: ₹" + DF.format(grandTotal));
            output.setOutputFiles(Collections.singletonList(reportPath.toAbsolutePath().toString()));

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("totalExpense", grandTotal);
            params.put("entryCount", allEntries.size());
            params.put("categoriesFound", categoryTotals.entrySet().stream()
                    .filter(en -> en.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()));
            params.put("reportFile", reportPath.toAbsolutePath().toString());
            params.put("processedAt", new Date().toString());
            output.setOutputParameters(params);

            // ── 6. Optional sleep if 'sleep' parameter is provided ──
            if (inputParams != null && inputParams.containsKey("sleep")) {
                Object sleepObj = inputParams.get("sleep");
                if (sleepObj != null) {
                    try {
                        long sleepMs = Long.parseLong(sleepObj.toString());
                        if (sleepMs > 0) {
                            double sleepSec = sleepMs / 1000.0;
                            logger.warn("⚠️  WARNING: Sleeping for {} seconds ({} ms) before completion", String.format("%.2f", sleepSec), sleepMs);
                            Thread.sleep(sleepMs);
                        }
                    } catch (NumberFormatException nfe) {
                        logger.warn("⚠️  WARNING: 'sleep' parameter value is not a valid number: {}", sleepObj);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Sleep interrupted");
                    }
                }
            }

        } catch (Exception ex) {
            output.setStatus("FAILED");
            output.setMainErrorCode("PROCESSING_ERROR");
            output.setMainErrorReason(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            logger.error("Processing error", ex);
        }

        logger.info("ExpenseTracker: Completed job: {}", inputData.getJobName());
        return output;
    }

    // ══════════════════════════════════════════════════════
    //  PARSING
    // ══════════════════════════════════════════════════════

    private List<ExpenseEntry> parseFile(String path) throws IOException {
        List<ExpenseEntry> entries = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(path));
        if (lines.isEmpty()) return entries;

        // Auto-detect separator
        String firstDataLine = lines.size() > 1 ? lines.get(1) : lines.get(0);
        char sep = firstDataLine.contains("|") ? '|' : ',';

        boolean headerSkipped = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Skip header row if first field looks like a label
            if (!headerSkipped) {
                String lower = line.toLowerCase();
                if (lower.startsWith("date") || lower.startsWith("\"date") || lower.startsWith("description")) {
                    headerSkipped = true;
                    continue;
                }
                headerSkipped = true;
            }

            String[] parts = splitLine(line, sep);
            if (parts.length < 3) continue;

            ExpenseEntry e = new ExpenseEntry();
            e.dateStr = parts[0].trim().replaceAll("^\"|\"$", "");
            e.description = parts[1].trim().replaceAll("^\"|\"$", "");
            try {
                e.amount = Double.parseDouble(parts[2].trim().replaceAll("[^0-9.\\-]", ""));
            } catch (NumberFormatException nfe) {
                continue; // skip unparseable rows
            }
            // Try to parse date for sorting
            e.date = parseDate(e.dateStr);
            entries.add(e);
        }
        return entries;
    }

    /** Split respecting quoted fields. */
    private String[] splitLine(String line, char sep) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == sep && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static final DateTimeFormatter[] DATE_FMTS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    };

    private LocalDate parseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FMTS) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // ══════════════════════════════════════════════════════
    //  CATEGORIZATION
    // ══════════════════════════════════════════════════════

    private String categorize(String description) {
        String lower = description.toLowerCase();
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) {
                    return entry.getKey();
                }
            }
        }
        return "Other";
    }

    // ══════════════════════════════════════════════════════
    //  HTML REPORT GENERATION
    // ══════════════════════════════════════════════════════

    private String buildHtmlReport(List<ExpenseEntry> entries,
                                   Map<String, Double> categoryTotals,
                                   Map<String, List<ExpenseEntry>> categoryEntries,
                                   double grandTotal,
                                   int fileCount,
                                   String jobName) {

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Expense Report – ").append(esc(jobName)).append("</title>\n");
        sb.append("<style>\n");
        sb.append(CSS);
        sb.append("</style>\n</head>\n<body>\n");

        // ── Header ──
        sb.append("<header class=\"hero\">\n");
        sb.append("  <h1>📊 Expense Report</h1>\n");
        sb.append("  <p class=\"subtitle\">").append(esc(jobName))
          .append(" &bull; ").append(entries.size()).append(" transactions")
          .append(" &bull; ").append(fileCount).append(" file(s) processed</p>\n");
        sb.append("  <p class=\"grand-total\">Grand Total: <strong>₹").append(DF.format(grandTotal)).append("</strong></p>\n");
        sb.append("</header>\n");

        sb.append("<main class=\"container\">\n");

        // ── Summary Cards Row ──
        sb.append("<section class=\"cards\">\n");
        int ci = 0;
        List<String> allCats = new ArrayList<>(CATEGORY_KEYWORDS.keySet());
        allCats.add("Other");
        for (String cat : allCats) {
            double total = categoryTotals.getOrDefault(cat, 0.0);
            if (total <= 0) { ci++; continue; }
            double pct = grandTotal > 0 ? (total / grandTotal * 100) : 0;
            String color = CHART_COLORS[ci % CHART_COLORS.length];
            sb.append("<div class=\"card\" style=\"border-top:4px solid ").append(color).append("\">\n");
            sb.append("  <div class=\"card-cat\" style=\"color:").append(color).append("\">").append(cat).append("</div>\n");
            sb.append("  <div class=\"card-amount\">₹").append(DF.format(total)).append("</div>\n");
            sb.append("  <div class=\"card-pct\">").append(String.format("%.1f", pct)).append("% of total</div>\n");
            sb.append("  <div class=\"card-count\">").append(categoryEntries.get(cat).size()).append(" transactions</div>\n");
            sb.append("</div>\n");
            ci++;
        }
        sb.append("</section>\n");

        // ── Charts Row ──
        sb.append("<section class=\"charts\">\n");

        // BAR CHART
        sb.append("<div class=\"chart-box\">\n<h2>Category Breakdown</h2>\n");
        sb.append(buildBarChartSvg(categoryTotals, grandTotal, allCats));
        sb.append("</div>\n");

        // DONUT CHART
        sb.append("<div class=\"chart-box\">\n<h2>Spending Distribution</h2>\n");
        sb.append(buildDonutChartSvg(categoryTotals, grandTotal, allCats));
        sb.append("</div>\n");

        sb.append("</section>\n");

        // ── Detailed Table per Category ──
        sb.append("<section class=\"details\">\n<h2>Transaction Details</h2>\n");
        ci = 0;
        for (String cat : allCats) {
            List<ExpenseEntry> catEntries = categoryEntries.get(cat);
            if (catEntries == null || catEntries.isEmpty()) { ci++; continue; }
            String color = CHART_COLORS[ci % CHART_COLORS.length];
            sb.append("<details class=\"cat-detail\">\n");
            sb.append("  <summary style=\"border-left:4px solid ").append(color).append("\">")
              .append(cat).append(" (").append(catEntries.size()).append(" items — ₹")
              .append(DF.format(categoryTotals.get(cat))).append(")</summary>\n");
            sb.append("  <table>\n    <tr><th>Date</th><th>Description</th><th class=\"amt\">Amount</th></tr>\n");
            for (ExpenseEntry e : catEntries) {
                sb.append("    <tr><td>").append(esc(e.dateStr))
                  .append("</td><td>").append(esc(e.description))
                  .append("</td><td class=\"amt\">₹").append(DF.format(e.amount))
                  .append("</td></tr>\n");
            }
            sb.append("  </table>\n</details>\n");
            ci++;
        }
        sb.append("</section>\n");

        // Footer
        sb.append("<footer>Generated by ExpenseTrackerProcessor on " + new Date() + "</footer>\n");

        sb.append("</main>\n</body>\n</html>\n");
        return sb.toString();
    }

    // ── SVG Bar Chart ──
    private String buildBarChartSvg(Map<String, Double> totals, double grandTotal, List<String> cats) {
        double maxVal = totals.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        int barH = 32, gap = 8, labelW = 120, chartW = 520, rightPad = 80;
        List<String> activeCats = cats.stream().filter(c -> totals.getOrDefault(c, 0.0) > 0).collect(Collectors.toList());
        int svgH = activeCats.size() * (barH + gap) + 20;
        int svgW = labelW + chartW + rightPad;

        StringBuilder s = new StringBuilder();
        s.append("<svg viewBox=\"0 0 ").append(svgW).append(" ").append(svgH)
         .append("\" class=\"bar-chart\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        int y = 10;
        int ci = 0;
        for (String cat : cats) {
            double val = totals.getOrDefault(cat, 0.0);
            if (val <= 0) { ci++; continue; }
            double ratio = val / maxVal;
            int bw = (int)(ratio * chartW);
            String color = CHART_COLORS[ci % CHART_COLORS.length];

            // label
            s.append("  <text x=\"").append(labelW - 8).append("\" y=\"").append(y + barH / 2 + 5)
             .append("\" text-anchor=\"end\" class=\"bar-label\">").append(cat).append("</text>\n");
            // bar
            s.append("  <rect x=\"").append(labelW).append("\" y=\"").append(y)
             .append("\" width=\"").append(bw).append("\" height=\"").append(barH)
             .append("\" rx=\"4\" fill=\"").append(color).append("\" class=\"bar-rect\"/>\n");
            // value
            s.append("  <text x=\"").append(labelW + bw + 6).append("\" y=\"").append(y + barH / 2 + 5)
             .append("\" class=\"bar-val\">₹").append(DF.format(val)).append("</text>\n");

            y += barH + gap;
            ci++;
        }
        s.append("</svg>\n");
        return s.toString();
    }

    // ── SVG Donut Chart ──
    private String buildDonutChartSvg(Map<String, Double> totals, double grandTotal, List<String> cats) {
        int size = 300, cx = 150, cy = 150, r = 110, innerR = 60;

        StringBuilder s = new StringBuilder();
        s.append("<svg viewBox=\"0 0 ").append(size + 200).append(" ").append(size)
         .append("\" class=\"donut-chart\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        if (grandTotal <= 0) {
            s.append("  <text x=\"").append(cx).append("\" y=\"").append(cy)
             .append("\" text-anchor=\"middle\">No data</text>\n");
            s.append("</svg>\n");
            return s.toString();
        }

        double startAngle = -90;
        int ci = 0;
        int legendY = 20;
        for (String cat : cats) {
            double val = totals.getOrDefault(cat, 0.0);
            if (val <= 0) { ci++; continue; }
            double pct = val / grandTotal;
            double sweepAngle = pct * 360;
            String color = CHART_COLORS[ci % CHART_COLORS.length];

            // Arc path
            double startRad = Math.toRadians(startAngle);
            double endRad = Math.toRadians(startAngle + sweepAngle);

            double x1 = cx + r * Math.cos(startRad);
            double y1 = cy + r * Math.sin(startRad);
            double x2 = cx + r * Math.cos(endRad);
            double y2 = cy + r * Math.sin(endRad);

            double ix1 = cx + innerR * Math.cos(startRad);
            double iy1 = cy + innerR * Math.sin(startRad);
            double ix2 = cx + innerR * Math.cos(endRad);
            double iy2 = cy + innerR * Math.sin(endRad);

            int largeArc = sweepAngle > 180 ? 1 : 0;

            s.append("  <path d=\"M ").append(fmt(x1)).append(" ").append(fmt(y1))
             .append(" A ").append(r).append(" ").append(r).append(" 0 ").append(largeArc).append(" 1 ")
             .append(fmt(x2)).append(" ").append(fmt(y2))
             .append(" L ").append(fmt(ix2)).append(" ").append(fmt(iy2))
             .append(" A ").append(innerR).append(" ").append(innerR).append(" 0 ").append(largeArc).append(" 0 ")
             .append(fmt(ix1)).append(" ").append(fmt(iy1))
             .append(" Z\" fill=\"").append(color).append("\" class=\"donut-slice\"/>\n");

            // Legend
            int lx = size + 20;
            s.append("  <rect x=\"").append(lx).append("\" y=\"").append(legendY)
             .append("\" width=\"14\" height=\"14\" rx=\"3\" fill=\"").append(color).append("\"/>\n");
            s.append("  <text x=\"").append(lx + 20).append("\" y=\"").append(legendY + 12)
             .append("\" class=\"legend-text\">").append(cat).append(" (")
             .append(String.format("%.0f", pct * 100)).append("%)</text>\n");

            startAngle += sweepAngle;
            legendY += 28;
            ci++;
        }

        // Center label
        s.append("  <text x=\"").append(cx).append("\" y=\"").append(cy - 6)
         .append("\" text-anchor=\"middle\" class=\"donut-center-line1\">Total</text>\n");
        s.append("  <text x=\"").append(cx).append("\" y=\"").append(cy + 18)
         .append("\" text-anchor=\"middle\" class=\"donut-center-line2\">₹").append(DF.format(grandTotal)).append("</text>\n");

        s.append("</svg>\n");
        return s.toString();
    }

    private String fmt(double v) { return String.format("%.2f", v); }
    private String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    // ══════════════════════════════════════════════════════
    //  INNER TYPE
    // ══════════════════════════════════════════════════════
    private static class ExpenseEntry {
        String dateStr;
        String description;
        double amount;
        LocalDate date;
    }

    // ══════════════════════════════════════════════════════
    //  INLINE CSS — dark-mode modern aesthetic
    // ══════════════════════════════════════════════════════
    private static final String CSS = "\n"
        + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
        + "body {\n"
        + "  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;\n"
        + "  background: #0f172a; color: #e2e8f0;\n"
        + "  line-height: 1.6;\n"
        + "}\n"
        + ".hero {\n"
        + "  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);\n"
        + "  padding: 2.5rem 2rem; text-align: center;\n"
        + "  border-bottom: 1px solid #334155;\n"
        + "}\n"
        + ".hero h1 { font-size: 2rem; letter-spacing: -0.02em; }\n"
        + ".subtitle { color: #94a3b8; margin-top: 0.4rem; }\n"
        + ".grand-total { margin-top: 1rem; font-size: 1.3rem; color: #a5f3fc; }\n"
        + ".container { max-width: 1100px; margin: 0 auto; padding: 1.5rem; }\n"
        + "\n/* Cards */\n"
        + ".cards { display: flex; flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem; }\n"
        + ".card {\n"
        + "  background: #1e293b; border-radius: 10px; padding: 1.2rem 1.4rem;\n"
        + "  flex: 1 1 200px; min-width: 180px;\n"
        + "  transition: transform 0.2s;\n"
        + "}\n"
        + ".card:hover { transform: translateY(-3px); }\n"
        + ".card-cat { font-weight: 700; font-size: 0.95rem; text-transform: uppercase; letter-spacing: 0.04em; }\n"
        + ".card-amount { font-size: 1.5rem; font-weight: 700; margin: 0.3rem 0; }\n"
        + ".card-pct, .card-count { font-size: 0.82rem; color: #94a3b8; }\n"
        + "\n/* Charts */\n"
        + ".charts { display: flex; flex-wrap: wrap; gap: 2rem; margin-bottom: 2rem; }\n"
        + ".chart-box {\n"
        + "  background: #1e293b; border-radius: 12px; padding: 1.5rem;\n"
        + "  flex: 1 1 420px;\n"
        + "}\n"
        + ".chart-box h2 { font-size: 1.1rem; margin-bottom: 1rem; color: #cbd5e1; }\n"
        + ".bar-chart { width: 100%; height: auto; }\n"
        + ".bar-label { fill: #cbd5e1; font-size: 13px; }\n"
        + ".bar-val   { fill: #94a3b8; font-size: 12px; }\n"
        + ".bar-rect  { transition: opacity 0.2s; } .bar-rect:hover { opacity: 0.8; }\n"
        + ".donut-chart { width: 100%; height: auto; }\n"
        + ".donut-slice { transition: opacity 0.2s; } .donut-slice:hover { opacity: 0.85; }\n"
        + ".legend-text { fill: #cbd5e1; font-size: 13px; }\n"
        + ".donut-center-line1 { fill: #94a3b8; font-size: 14px; }\n"
        + ".donut-center-line2 { fill: #f1f5f9; font-size: 18px; font-weight: 700; }\n"
        + "\n/* Details */\n"
        + ".details { margin-top: 1rem; }\n"
        + ".details h2 { font-size: 1.2rem; margin-bottom: 1rem; }\n"
        + ".cat-detail { margin-bottom: 0.6rem; }\n"
        + ".cat-detail summary {\n"
        + "  cursor: pointer; padding: 0.7rem 1rem;\n"
        + "  background: #1e293b; border-radius: 8px;\n"
        + "  font-weight: 600; list-style: none;\n"
        + "}\n"
        + ".cat-detail summary::-webkit-details-marker { display: none; }\n"
        + ".cat-detail summary::before { content: '▸ '; }\n"
        + ".cat-detail[open] summary::before { content: '▾ '; }\n"
        + "table { width: 100%; border-collapse: collapse; margin: 0.5rem 0 1rem; }\n"
        + "th { text-align: left; color: #94a3b8; border-bottom: 1px solid #334155; padding: 0.5rem 0.8rem; font-size: 0.85rem; }\n"
        + "td { padding: 0.45rem 0.8rem; border-bottom: 1px solid #1e293b; font-size: 0.9rem; }\n"
        + ".amt { text-align: right; font-variant-numeric: tabular-nums; }\n"
        + "footer { text-align: center; color: #475569; font-size: 0.78rem; padding: 2rem 0 1rem; }\n"
        + "@media (max-width: 640px) { .cards { flex-direction: column; } .charts { flex-direction: column; } }\n";
}
