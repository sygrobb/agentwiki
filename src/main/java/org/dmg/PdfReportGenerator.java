package org.dmg;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PdfReportGenerator {

    /**
     * Сигнатура сумісна з WikiApp.java:
     * generateSinglePagePdf(String outputPath, Map<String, Map<LocalDate, Long>> timeSeriesMap, List<TrendAnalyzer.MetricDetails> results)
     */
    public static void generateSinglePagePdf(
            String outputPath,
            Map<String, Map<LocalDate, Long>> timeSeriesMap,
            List<TrendAnalyzer.MetricDetails> results
    ) throws Exception {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        // Шрифти
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.DARK_GRAY);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.GRAY);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

        // Заголовок
        document.add(new Paragraph("Wikipedia Analytics Phase 3 Executive Report", titleFont));
        document.add(new Paragraph("Market Intelligence & Trend Decomposition Analysis\n\n", subFont));

        // 1. Графік JFreeChart (Сирі дані + Десезонований Тренд)
        TimeSeriesCollection dataset = new TimeSeriesCollection();

        for (TrendAnalyzer.MetricDetails res : results) {
            String articleKey = res.article();

            // Додаємо Raw Series із timeSeriesMap
            Map<LocalDate, Long> rawSeriesData = timeSeriesMap != null ? timeSeriesMap.get(articleKey) : null;
            if (rawSeriesData != null && !rawSeriesData.isEmpty()) {
                TimeSeries rawSeries = new TimeSeries(articleKey + " (Raw)");
                rawSeriesData.forEach((date, views) ->
                        rawSeries.add(new Month(date.getMonthValue(), date.getYear()), views)
                );
                dataset.addSeries(rawSeries);
            }

            // Додаємо Cleaned Trend Series із MetricDetails
            if (res.seasonalityDetected() && res.trendSeries() != null && !res.trendSeries().isEmpty()) {
                TimeSeries trendSeries = new TimeSeries(articleKey + " (Trend)");
                res.trendSeries().forEach((date, views) ->
                        trendSeries.add(new Month(date.getMonthValue(), date.getYear()), views)
                );
                dataset.addSeries(trendSeries);
            }
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Monthly Views & Demand Trends", "Month", "Pageviews", dataset, true, true, false
        );

        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, 520, 210);
        Image chartImg = Image.getInstance(chartOut.toByteArray());
        chartImg.setAlignment(Element.ALIGN_CENTER);
        document.add(chartImg);

        document.add(new Paragraph("\nStatistical & Market Analytics Summary:", boldFont));

        // 2. Таблиця результатів (7 колонок)
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setWidths(new float[]{2.5f, 1.2f, 1.2f, 1.1f, 1.1f, 1.0f, 2.0f});

        addHeaderCell(table, "Article", boldFont);
        addHeaderCell(table, "Raw Growth", boldFont);
        addHeaderCell(table, "Clean Growth", boldFont);
        addHeaderCell(table, "R² Qual", boldFont);
        addHeaderCell(table, "Confidence", boldFont);
        addHeaderCell(table, "Anomalies", boldFont);
        addHeaderCell(table, "Market Verdict", boldFont);

        for (TrendAnalyzer.MetricDetails res : results) {
            String lang = res.article().contains(":") ? res.article().split(":")[0] : "uk";

            // Розрахунок вердикту на льоту через MarketIntelligenceService (як у WikiApp)
            MarketIntelligenceService.MarketVerdict verdict =
                    MarketIntelligenceService.evaluateDemand(res.article(), res.deseasonalizedGrowthPct(), lang);

            table.addCell(new Paragraph(res.article(), textFont));
            table.addCell(new Paragraph(String.format("%.1f%%", res.rawMonthlyGrowthPct()), textFont));
            table.addCell(new Paragraph(String.format("%.1f%%", res.deseasonalizedGrowthPct()), textFont));
            table.addCell(new Paragraph(String.format("%.2f", res.rSquared()), textFont));
            table.addCell(new Paragraph(String.format("%.0f%%", res.confidenceScore() * 100), textFont));
            table.addCell(new Paragraph(String.valueOf(res.anomalyCount()), textFont));
            table.addCell(new Paragraph(verdict != null ? verdict.verdict() : "N/A", boldFont));
        }

        document.add(table);
        document.close();
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}