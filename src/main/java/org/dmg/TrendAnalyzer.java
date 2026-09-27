package org.dmg;

import java.time.LocalDate;
import java.util.*;

public class TrendAnalyzer {

    public record AnalysisResult(
            String article,
            double slopePercentage,
            double confidenceScore,
            int anomalyCount,
            long totalViews,
            Map<LocalDate, Long> timeSeries
    ) {}

    public static AnalysisResult analyze(String article, Map<LocalDate, Long> timeSeries) {
        List<Long> values = new ArrayList<>(timeSeries.values());
        int n = values.size();
        if (n < 2) {
            return new AnalysisResult(article, 0.0, 0.0, 0, 0, timeSeries);
        }

        // 1. Обчислення лінійного тренду (Linear Regression)
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        long totalViews = 0;
        for (int i = 0; i < n; i++) {
            double y = values.get(i);
            sumX += i;
            sumY += y;
            sumXY += i * y;
            sumX2 += i * i;
            totalViews += y;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double avgY = sumY / n;
        double slopePercentage = (avgY == 0) ? 0 : (slope / avgY) * 100;

        // 2. Детекція аномалій за стандартизованим відхиленням (Z-score)
        double stdDev = Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - avgY, 2)).sum() / n);
        int anomalyCount = 0;
        for (double v : values) {
            if (stdDev > 0 && Math.abs((v - avgY) / stdDev) > 2.5) {
                anomalyCount++;
            }
        }

        // 3. Confidence Score: знижується при високій зашумленості та аномаліях
        double noiseRatio = stdDev / (avgY == 0 ? 1 : avgY);
        double confidenceScore = Math.max(0.1, Math.min(1.0, 1.0 - (noiseRatio * 0.3) - (anomalyCount * 0.15)));

        return new AnalysisResult(article, slopePercentage, confidenceScore, anomalyCount, totalViews, timeSeries);
    }
}