package org.dmg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDate;
import java.util.*;

public class WikiApp {

    public record Phase3JsonOutput(
            List<TrendAnalyzer.MetricDetails> metrics,
            List<MarketIntelligenceService.MarketVerdict> marketVerdicts,
            List<WikidataSparqlClient.ClusterArticle> semanticCluster,
            String generatedPdfPath
    ) {
    }

    public static void main(String[] args) {
        String articlesArg = null;
        int periodMonths = 24;
        String outputPdf = null;
        boolean jsonOutput = false;
        boolean expandCluster = false;
        String wikidataQid = null;

        // Парсинг аргументів CLI
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--articles" -> articlesArg = args[++i];
                case "--period-months" -> periodMonths = Integer.parseInt(args[++i]);
                case "--output-pdf" -> outputPdf = args[++i];
                case "--json" -> jsonOutput = true;
                case "--expand-cluster" -> expandCluster = true;
                case "--wikidata-qid" -> wikidataQid = args[++i];
            }
        }

        if (articlesArg == null && wikidataQid == null) {
            System.err.println("Error: --articles or --wikidata-qid is required.");
            System.exit(1);
        }

        try {
            List<String> articles = new ArrayList<>();
            List<WikidataSparqlClient.ClusterArticle> clusterArticles = new ArrayList<>();

            // 1. Якщо активовано Wikidata Cluster
            if (expandCluster && wikidataQid != null) {
                List<String> langs = List.of("uk", "pl", "en");
                clusterArticles = WikidataSparqlClient.fetchClusterArticles(wikidataQid, langs);
                for (var ca : clusterArticles) {
                    articles.add(ca.lang() + ":" + ca.wikiTitle().replace(" ", "_"));
                }
            } else if (articlesArg != null) {
                articles = Arrays.asList(articlesArg.split(","));
            }

            List<TrendAnalyzer.MetricDetails> metricsList = new ArrayList<>();
            List<MarketIntelligenceService.MarketVerdict> verdicts = new ArrayList<>();
            Map<String, Map<LocalDate, Long>> allSeries = new LinkedHashMap<>();

            WikipediaApiClient client = new WikipediaApiClient();

            // 2. Аналіз кожної статті
            for (String art : articles) {
                String[] parts = art.trim().split(":", 2);
                if (parts.length < 2) continue;

                String lang = parts[0];
                String title = parts[1];

                Map<LocalDate, Long> timeSeries = client.fetchMonthlyPageviews(lang, title, periodMonths);
                allSeries.put(art, timeSeries);

                TrendAnalyzer.MetricDetails details = TrendAnalyzer.analyze(art, timeSeries);
                metricsList.add(details);

                // Фаза 3: Market Intelligence Verdict
                MarketIntelligenceService.MarketVerdict verdict =
                        MarketIntelligenceService.evaluateDemand(art, details.deseasonalizedGrowthPct(), lang);
                verdicts.add(verdict);
            }

            // 3. Генерація PDF
            if (outputPdf != null) {
                PdfReportGenerator.generateSinglePagePdf(outputPdf, allSeries, metricsList);
            }

            // 4. Вивід результату
            if (jsonOutput) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                Phase3JsonOutput output = new Phase3JsonOutput(metricsList, verdicts, clusterArticles, outputPdf);
                System.out.println(mapper.writeValueAsString(output));
            } else {
                System.out.println("=== Phase 3 Analytics Completed ===");
                for (var v : verdicts) {
                    System.out.printf("Article: %s | Relative Growth: %.2f%% | Verdict: %s%n",
                            v.article(), v.relativeGrowthPct(), v.verdict());
                }
            }

        } catch (Exception e) {
            System.err.println("Fatal Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}