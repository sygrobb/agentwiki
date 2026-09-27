package org.dmg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class WikipediaApiClient {
    private static final String PAGEVIEWS_URL =
            "https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/%s.wikipedia/all-access/user/%s/monthly/%s/%s";

    private static final File CACHE_DIR = new File(".cache/wiki");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WikipediaApiClient() {
        if (!CACHE_DIR.exists()) CACHE_DIR.mkdirs();
    }

    public Map<LocalDate, Long> fetchMonthlyPageviews(String lang, String article, int months) throws Exception {
        LocalDate end = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        LocalDate start = end.minusMonths(months);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM01");
        String startStr = start.format(fmt);
        String endStr = end.format(fmt);

        // Формуємо ім'я файлу кешу
        String cacheKey = String.format("%s_%s_%s_%s.json", lang, article.replaceAll("[^a-zA-Z0-9_]", "_"), startStr, endStr);
        File cacheFile = new File(CACHE_DIR, cacheKey);

        String jsonResponse;
        if (cacheFile.exists()) {
            // Read from Cache
            jsonResponse = Files.readString(cacheFile.toPath());
        } else {
            // Fetch from Wikimedia REST API
            String url = String.format(PAGEVIEWS_URL, lang, article, startStr, endStr);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "WikiAnalyticsAgentSkill/2.0 (contact@example.com)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Wikimedia API HTTP Error: " + response.statusCode());
            }
            jsonResponse = response.body();
            // Save to Cache
            Files.writeString(cacheFile.toPath(), jsonResponse);
        }

        return parseWikimediaJson(jsonResponse);
    }

    private Map<LocalDate, Long> parseWikimediaJson(String json) throws Exception {
        JsonNode items = objectMapper.readTree(json).get("items");
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                String timestamp = item.get("timestamp").asText();
                LocalDate date = LocalDate.parse(timestamp.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
                long views = item.get("views").asLong();
                result.put(date, views);
            }
        }
        return result;
    }
}