package org.dmg;

public class MarketIntelligenceService {

    public record MarketVerdict(
            String article,
            double articleGrowthPct,
            double baselineWikiGrowthPct,
            double relativeGrowthPct,
            String verdict
    ) {}

    /**
     * Оцінює, чи росте тема швидше за весь мовний розділ Вікіпедії
     */
    public static MarketVerdict evaluateDemand(String article, double articleGrowthPct, String lang) {
        // Умовний середній приплив трафіку мовного розділу (за бажанням інтегрується з Site-wide Pageviews API)
        double baselineWikiGrowth = switch (lang.toLowerCase()) {
            case "uk" -> 4.5;  // Загальне зростання укр. Вікіпедії ~4.5% на рік
            case "pl" -> 2.1;  // Загальне зростання пол. Вікіпедії ~2.1%
            default -> 3.0;
        };

        double relativeGrowth = articleGrowthPct - baselineWikiGrowth;
        String verdict;

        if (relativeGrowth > 10.0) {
            verdict = "HIGH_OUTPERFORMER"; // Значно випереджає ринок
        } else if (relativeGrowth > 0.0) {
            verdict = "MODERATE_GROWTH";    // Росте разом із ринком
        } else if (relativeGrowth > -10.0) {
            verdict = "UNDERPERFORMING";    // Росте повільніше за загальний трафік
        } else {
            verdict = "DECLINING_INTEREST"; // Втрачає частку уваги
        }

        return new MarketVerdict(article, articleGrowthPct, baselineWikiGrowth, relativeGrowth, verdict);
    }
}