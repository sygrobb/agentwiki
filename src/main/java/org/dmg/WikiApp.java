package org.dmg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDate;
import java.util.*;

public class WikiApp {

    // Спеціальний DTO для JSON-відповіді агенту (без сирих масивів дат, лише статистика)
    public record AgentJsonSummary(
            String article,
            long totalViews,
            double rawMonthlyGrowthPct,
            double deseasonalizedGrowthPct,
            double rSquared,
            double confidenceScore,
            boolean seasonalityDetected,
            int anomalyCount,
            String status
    ) {}

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
            List<TrendAnalyzer.MetricDetails> fullResults = new ArrayList<>();
            List<AgentJsonSummary> agentSummaries = new ArrayList<>();

            for (String art : articles) {
                String cleanArt = art.trim();
                if (cleanArt.isEmpty()) continue;

                String[] parts = cleanArt.split(":", 2);
                if (parts.length < 2) {
                    System.err.println("Warning: Invalid article format '" + cleanArt + "'. Expected 'lang:Title'. Skipping.");
                    continue;
                }

                String lang = parts[0];
                // Замінюємо пробіли на підкреслення для сумісності з Wikipedia API
                String title = parts[1].replace(" ", "_");

                try {
                    Map<LocalDate, Long> timeSeries = client.fetchMonthlyPageviews(lang, title, months);
                    TrendAnalyzer.MetricDetails analysis = TrendAnalyzer.analyze(cleanArt, timeSeries);

                    fullResults.add(analysis);

                    // Створюємо компактний сумарій для LLM
                    agentSummaries.add(new AgentJsonSummary(
                            analysis.article(),
                            analysis.totalViews(),
                            round(analysis.rawMonthlyGrowthPct()),
                            round(analysis.deseasonalizedGrowthPct()),
                            round(analysis.rSquared()),
                            round(analysis.confidenceScore()),
                            analysis.seasonalityDetected(),
                            analysis.anomalyCount(),
                            "SUCCESS"
                    ));
                } catch (Exception e) {
                    System.err.println("Error processing article '" + cleanArt + "': " + e.getMessage());
                    agentSummaries.add(new AgentJsonSummary(
                            cleanArt, 0, 0, 0, 0, 0, false, 0, "ERROR: " + e.getMessage()
                    ));
                }
            }

            if (fullResults.isEmpty()) {
                System.err.println("Error: No articles were successfully processed.");
                System.exit(1);
            }

            // 1. Генерація PDF-звіту (якщо вказано шлях)
            if (pdfPath != null && !pdfPath.isBlank()) {
                PdfReportGenerator.generateSinglePagePdf(fullResults, pdfPath);
            }

            // 2. Вивід результатів для агента
            if (jsonOutput) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                Map<String, Object> finalResponse = new LinkedHashMap<>();
                finalResponse.put("periodMonths", months);
                finalResponse.put("analyzedArticlesCount", agentSummaries.size());
                finalResponse.put("pdfGenerated", pdfPath != null);
                finalResponse.put("pdfPath", pdfPath);
                finalResponse.put("results", agentSummaries);

                System.out.println(mapper.writeValueAsString(finalResponse));
            } else {
                System.out.println("=== Wikipedia Analytics Summary ===");
                for (var s : agentSummaries) {
                    System.out.printf("Article: %-25s | Clean Trend: %6.2f%% | Confidence: %3.0f%% | Anomalies: %d\n",
                            s.article(), s.deseasonalizedGrowthPct(), s.confidenceScore() * 100, s.anomalyCount());
                }
            }

        } catch (Exception e) {
            System.err.println("Fatal Execution Error: " + e.getMessage());
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

    private static double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}