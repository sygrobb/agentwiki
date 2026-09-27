package org.dmg;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrendAnalyzerTest {

    @Test
    void testLinearGrowthAndConfidence() {
        Map<LocalDate, Long> timeSeries = new LinkedHashMap<>();
        LocalDate start = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < 24; i++) {
            timeSeries.put(start.plusMonths(i), 1000L + (i * 100L)); // +100 переглядів щомісяця
        }

        TrendAnalyzer.MetricDetails result = TrendAnalyzer.analyze("uk:Test", timeSeries);

        assertTrue(result.rawMonthlyGrowthPct() > 0, "Growth should be positive");
        assertTrue(result.rSquared() > 0.95, "R2 should be close to 1.0 for perfect linear data");
        assertTrue(result.confidenceScore() > 0.8, "Confidence score should be high");
        assertEquals(0, result.anomalyCount(), "No anomalies should be detected in synthetic smooth data");
    }

    @Test
    void testAnomalyDetection() {
        Map<LocalDate, Long> timeSeries = new LinkedHashMap<>();
        LocalDate start = LocalDate.of(2024, 1, 1);

        for (int i = 0; i < 24; i++) {
            timeSeries.put(start.plusMonths(i), 1000L);
        }
        timeSeries.put(start.plusMonths(10), 50000L);

        TrendAnalyzer.MetricDetails result = TrendAnalyzer.analyze("uk:TestSpike", timeSeries);

        assertTrue(result.anomalyCount() >= 1, "Should detect at least 1 anomaly spike via IQR");
    }
}