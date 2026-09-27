package org.dmg;

import java.time.LocalDate;
import java.util.*;

public class TrendAnalyzer {

    public record MetricDetails(
            String article,
            long totalViews,
            double rawMonthlyGrowthPct,     // Сирий тренд
            double deseasonalizedGrowthPct, // Тренд без сезонності
            double rSquared,                // Якість лінійної апроксимації (0..1)
            double confidenceScore,         // Підсумковий скор довіри (0..1)
            boolean seasonalityDetected,   // Чи наявна сезонність
            int anomalyCount,               // Кількість виявлених спайків
            Map<LocalDate, Long> rawSeries,
            Map<LocalDate, Double> trendSeries // Очищений часовий ряд
    ) {}

    public static MetricDetails analyze(String article, Map<LocalDate, Long> timeSeries) {
        List<LocalDate> dates = new ArrayList<>(timeSeries.keySet());
        List<Long> rawValues = new ArrayList<>(timeSeries.values());
        int n = rawValues.size();

        if (n < 6) { // Мінімальна кількість точок для Фази 2
            return createFallbackResult(article, timeSeries);
        }

        // 1. Детекція та фільтрація аномалій (Outliers via IQR)
        double[] valuesArr = rawValues.stream().mapToDouble(Long::doubleValue).toArray();
        double[] sorted = valuesArr.clone();
        Arrays.sort(sorted);
        double q1 = sorted[(int) (n * 0.25)];
        double q3 = sorted[(int) (n * 0.75)];
        double iqr = q3 - q1;
        double upperFence = q3 + 1.5 * iqr;
        double lowerFence = q1 - 1.5 * iqr;

        int anomalyCount = 0;
        double[] cleanedValues = new double[n];
        for (int i = 0; i < n; i++) {
            if (valuesArr[i] > upperFence || valuesArr[i] < lowerFence) {
                anomalyCount++;
                cleanedValues[i] = (i > 0) ? cleanedValues[i - 1] : q2(sorted); // Заміна на медіану
            } else {
                cleanedValues[i] = valuesArr[i];
            }
        }

        // 2. Декомпозиція та сезонний аналіз (Centered Moving Average, Window = 12)
        double[] trendComponent = new double[n];
        int window = Math.min(12, n > 12 ? 12 : 3);
        int half = window / 2;

        for (int i = 0; i < n; i++) {
            int start = Math.max(0, i - half);
            int end = Math.min(n - 1, i + half);
            double sum = 0;
            for (int j = start; j <= end; j++) sum += cleanedValues[j];
            trendComponent[i] = sum / (end - start + 1);
        }

        // Перевірка сезонної варіативності
        double maxTrend = Arrays.stream(trendComponent).max().orElse(1);
        double minTrend = Arrays.stream(trendComponent).min().orElse(1);
        double seasonalVariance = 0;
        for (int i = 0; i < n; i++) {
            seasonalVariance += Math.abs(cleanedValues[i] - trendComponent[i]);
        }
        boolean seasonalityDetected = (seasonalVariance / (n * maxTrend)) > 0.15;

        // 3. Лінійна регресія та R-Squared (Коефіцієнт детермінації)
        RegressionResult rawReg = calculateRegression(valuesArr);
        RegressionResult cleanReg = calculateRegression(trendComponent);

        // 4. Метрика довіри (Confidence Score)
        // Враховує R^2, наявність аномалій та зашумленість
        double noisePenalty = (double) anomalyCount / n * 0.4;
        double confidenceScore = Math.max(0.1, Math.min(1.0, cleanReg.rSquared * 0.7 + (1.0 - noisePenalty) * 0.3));

        Map<LocalDate, Double> trendSeriesMap = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            trendSeriesMap.put(dates.get(i), trendComponent[i]);
        }

        long totalViews = rawValues.stream().mapToLong(Long::longValue).sum();

        return new MetricDetails(
                article,
                totalViews,
                rawReg.slopePct,
                cleanReg.slopePct,
                cleanReg.rSquared,
                confidenceScore,
                seasonalityDetected,
                anomalyCount,
                timeSeries,
                trendSeriesMap
        );
    }

    private record RegressionResult(double slopePct, double rSquared) {}

    private static RegressionResult calculateRegression(double[] y) {
        int n = y.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += y[i];
            sumXY += i * y[i];
            sumX2 += i * i;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        double totalSS = 0, residualSS = 0;
        double meanY = sumY / n;
        for (int i = 0; i < n; i++) {
            double fitted = slope * i + intercept;
            totalSS += Math.pow(y[i] - meanY, 2);
            residualSS += Math.pow(y[i] - fitted, 2);
        }
        double rSquared = (totalSS == 0) ? 0 : Math.max(0, 1.0 - (residualSS / totalSS));
        double slopePct = (meanY == 0) ? 0 : (slope / meanY) * 100;

        return new RegressionResult(slopePct, rSquared);
    }

    private static double q2(double[] sorted) {
        int n = sorted.length;
        return (n % 2 == 0) ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0 : sorted[n / 2];
    }

    private static MetricDetails createFallbackResult(String article, Map<LocalDate, Long> timeSeries) {
        long total = timeSeries.values().stream().mapToLong(Long::longValue).sum();
        return new MetricDetails(article, total, 0, 0, 0, 0.1, false, 0, timeSeries, Map.of());
    }
}