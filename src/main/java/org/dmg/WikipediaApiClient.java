package org.dmg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class WikipediaApiClient {
    private static final String PAGEVIEWS_URL = 
        "https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/%s.wikipedia/all-access/user/%s/monthly/%s/%s";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<LocalDate, Long> fetchMonthlyPageviews(String lang, String article, int months) throws Exception {
        LocalDate end = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        LocalDate start = end.minusMonths(months);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM01");
        String url = String.format(PAGEVIEWS_URL, lang, article, start.format(fmt), end.format(fmt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "WikiAnalyticsAgentSkill/1.0 (contact@example.com)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Wikimedia API error: HTTP " + response.statusCode());
        }

        JsonNode items = objectMapper.readTree(response.body()).get("items");
        Map<LocalDate, Long> result = new LinkedHashMap<>();

        for (JsonNode item : items) {
            String timestamp = item.get("timestamp").asText(); // "2024010100"
            LocalDate date = LocalDate.parse(timestamp.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
            long views = item.get("views").asLong();
            result.put(date, views);
        }

        return result;
    }
}