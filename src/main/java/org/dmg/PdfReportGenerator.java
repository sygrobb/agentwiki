package org.dmg;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Image;
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

    public static void generateSinglePagePdf(List<TrendAnalyzer.AnalysisResult> results, String outputPath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font textFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        document.add(new Paragraph("Wikipedia Analytics Executive Report", titleFont));
        document.add(new Paragraph("Generated automatically by Agent Skill\n\n", textFont));

        // Створення графіку JFreeChart
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (var res : results) {
            TimeSeries series = new TimeSeries(res.article());
            res.timeSeries().forEach((date, views) -> 
                series.add(new Month(date.getMonthValue(), date.getYear()), views)
            );
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Search Interest Over Time", "Date", "Monthly Pageviews", dataset, true, true, false
        );

        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, 500, 260);

        Image chartImg = Image.getInstance(chartOut.toByteArray());
        document.add(chartImg);

        // Таблиця результатів / Коротке резюме
        document.add(new Paragraph("\nKey Findings:", new Font(Font.HELVETICA, 12, Font.BOLD)));
        for (var res : results) {
            String line = String.format("- %s: Monthly Growth Trend = %.2f%% | Confidence = %.0f%% | Anomalies = %d",
                    res.article(), res.slopePercentage(), res.confidenceScore() * 100, res.anomalyCount());
            document.add(new Paragraph(line, textFont));
        }

        document.close();
    }
}