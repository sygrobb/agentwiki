package org.dmg;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;

public class PdfReportGenerator {

    public static void generateSinglePagePdf(List<TrendAnalyzer.MetricDetails> results, String outputPath) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        document.add(new Paragraph("Wikipedia Analytics Phase 2 Executive Report", titleFont));
        document.add(new Paragraph("De-seasonalized trend analysis & Statistical Confidence Assessment\n\n", subFont));

        // Графік JFreeChart (Сирі дані + Тренд)
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (var res : results) {
            TimeSeries rawSeries = new TimeSeries(res.article() + " (Raw)");
            res.rawSeries().forEach((date, views) ->
                    rawSeries.add(new Month(date.getMonthValue(), date.getYear()), views)
            );
            dataset.addSeries(rawSeries);

            if (res.seasonalityDetected() && !res.trendSeries().isEmpty()) {
                TimeSeries trendSeries = new TimeSeries(res.article() + " (Trend)");
                res.trendSeries().forEach((date, views) ->
                        trendSeries.add(new Month(date.getMonthValue(), date.getYear()), views)
                );
                dataset.addSeries(trendSeries);
            }
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Monthly Views & Seasonality Trend", "Month", "Pageviews", dataset, true, true, false
        );

        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, 520, 240);
        Image chartImg = Image.getInstance(chartOut.toByteArray());
        document.add(chartImg);

        document.add(new Paragraph("\nStatistical Summary Table:", boldFont));

        // Таблиця результатів
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        table.addCell(new Paragraph("Article", boldFont));
        table.addCell(new Paragraph("Raw Growth", boldFont));
        table.addCell(new Paragraph("Clean Growth", boldFont));
        table.addCell(new Paragraph("R² Quality", boldFont));
        table.addCell(new Paragraph("Confidence", boldFont));
        table.addCell(new Paragraph("Anomalies", boldFont));

        for (var res : results) {
            table.addCell(new Paragraph(res.article(), textFont));
            table.addCell(new Paragraph(String.format("%.1f%%", res.rawMonthlyGrowthPct()), textFont));
            table.addCell(new Paragraph(String.format("%.1f%%", res.deseasonalizedGrowthPct()), textFont));
            table.addCell(new Paragraph(String.format("%.2f", res.rSquared()), textFont));
            table.addCell(new Paragraph(String.format("%.0f%%", res.confidenceScore() * 100), textFont));
            table.addCell(new Paragraph(String.valueOf(res.anomalyCount()), textFont));
        }

        document.add(table);
        document.close();
    }
}