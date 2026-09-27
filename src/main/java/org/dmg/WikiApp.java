package org.dmg;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WikiApp {
    public static void main(String[] args) {
        try {
            Map<String, String> params = parseArgs(args);
            if (!params.containsKey("articles")) {
                System.err.println("Usage: --articles lang:Title1,lang:Title2 [--period-months 24] [--output-pdf path] [--json]");
                System.exit(1);
            }

            String[] articles = params.get("articles").split(",");
            int months = Integer.parseInt(params.getOrDefault("period-months", "24"));
            boolean jsonOutput = params.containsKey("json");
            String pdfPath = params.get("output-pdf");

            WikipediaApiClient client = new WikipediaApiClient();
            List<TrendAnalyzer.AnalysisResult> results = new ArrayList<>();

            for (String art : articles) {
                String[] parts = art.trim().split(":", 2);
                var timeSeries = client.fetchMonthlyPageviews(parts[0], parts[1], months);
                var analysis = TrendAnalyzer.analyze(art, timeSeries);
                results.add(analysis);
            }

            if (pdfPath != null) {
                PdfReportGenerator.generateSinglePagePdf(results, pdfPath);
            }

            if (jsonOutput) {
                ObjectMapper mapper = new ObjectMapper();
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
            } else {
                results.forEach(r -> System.out.printf("Article: %s, Trend: %.2f%%\n", r.article(), r.slopePercentage()));
            }

        } catch (Exception e) {
            System.err.println("Execution Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    params.put(key, args[++i]);
                } else {
                    params.put(key, "true");
                }
            }
        }
        return params;
    }
}