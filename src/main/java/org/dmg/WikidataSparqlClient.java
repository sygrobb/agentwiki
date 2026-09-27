package org.dmg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WikidataSparqlClient {

    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record ClusterArticle(String qid, String label, String lang, String wikiTitle) {}

    /**
     * Знаходить пов'язані статті для заданого Wikidata ID (або початкової назви)
     */
    public static List<ClusterArticle> fetchClusterArticles(String wikidataId, List<String> targetLangs) {
        List<ClusterArticle> results = new ArrayList<>();
        
        // Формуємо список мов для FILTER
        StringBuilder langFilter = new StringBuilder();
        for (String lang : targetLangs) {
            langFilter.append(String.format("<https://%s.wikipedia.org/>,", lang));
        }
        if (langFilter.length() > 0) {
            langFilter.setLength(langFilter.length() - 1); // видаляємо останню кому
        }

        String query = """
            SELECT DISTINCT ?item ?itemLabel ?langCode ?wikiTitle WHERE {
              BIND(wd:%s AS ?mainItem)
              {
                ?item wdt:P279 ?mainItem .
              } UNION {
                ?item wdt:P101 ?mainItem .
              } UNION {
                BIND(?mainItem AS ?item)
              }
              ?sitelink schema:about ?item ;
                        schema:isPartOf ?site ;
                        schema:name ?wikiTitle .
              FILTER(?site IN (%s))
              BIND(REPLACE(STR(?site), "https://([a-z]+)\\\\..*", "$1") AS ?langCode)
              SERVICE wikibase:label { bd:serviceParam wikibase:language "en,uk". }
            } LIMIT 20
            """.formatted(wikidataId, langFilter.toString());

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = SPARQL_ENDPOINT + "?query=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "WikipediaAnalyticsSkill/3.0 (agent@example.com)")
                    .header("Accept", "application/sparql-results+json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode bindings = root.path("results").path("bindings");

                for (JsonNode b : bindings) {
                    String qid = b.path("item").path("value").asText().replaceAll(".*/", "");
                    String label = b.path("itemLabel").path("value").asText();
                    String lang = b.path("langCode").path("value").asText();
                    String title = b.path("wikiTitle").path("value").asText();

                    results.add(new ClusterArticle(qid, label, lang, title));
                }
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to fetch Wikidata cluster: " + e.getMessage());
        }

        return results;
    }
}