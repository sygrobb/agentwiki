Wikipedia Analytics Skill for AI AgentsAn enterprise-ready Agent Skill designed for LLM agents (e.g., Claude 3.5 Haiku) 
to analyze topic interest dynamics across Wikipedia language editions,
clean data from seasonality/outliers, and generate 
executive 1-page PDF reports.

📌 Executive SummaryArchitecture: External LLM Agent $\rightarrow$ 
Bash Wrapper (run.sh) $\rightarrow$ High-Performance Java 21 CLI.Why No Frameworks? 
Pure Java CLI starts in ~0.2s (vs 3–5s in Spring Boot), avoiding LLM tool call timeouts
Token Saver: Math, seasonal decomposition (STL), 
and PDF rendering are offloaded to Java, returning only a compact JSON summary to the LLM.

🛠 Core Capabilities (Phase 2)
FeatureDescriptionSeasonality Clean-upSeparates organic growth from annual cyclic peaks (e.g., academic seasonality).Anomaly
FilteringSuppresses viral spikes, news noise, and bot traffic using Interquartile Range (IQR).Confidence ScoringComputes $R^2$ 
linear regression quality to output a confidence_score (0.0 – 1.0).
Disk CachingLocal .cache/wiki/ stores raw API responses
for instant sub-second re-runs.Cyrillic PDF EngineGenerates 1-page charts & tables using OpenPDF + ttf-dejavu fonts.

📂 Project Structure 
├── SKILL.md                 # Agent Skill instructions & JSON Schema for LLM
├── Dockerfile               # Multi-stage reproducible build (Maven + JRE 21)
├── run.sh                   # Entrypoint script (UTF-8, Headless AWT, CLI args)
├── pom.xml                  # Build configuration & dependencies
├── src/main/java/com/example/wiki/
│   ├── WikiApp.java              # Main CLI & JSON DTO mapper
│   ├── WikipediaApiClient.java   # HTTP Client with disk caching
│   ├── TrendAnalyzer.java        # STL decomposition, IQR & R² math
│   └── PdfReportGenerator.java   # JFreeChart + OpenPDF renderer
└── tests/
└── test_e2e.py          # Python E2E test harness for Claude Haiku
🚀 Quick Start Guide1. 
Build Docker Image Bash docker build -t wikipedia-analytics-skill:latest.

Execution Modes 
 Mode A: JSON Output for LLM (Docker)
 Bash docker run --rm -v "$(pwd)/reports:/skill/reports" \wikipedia-analytics-skill:latest \
   --articles "uk:Астрономія,pl:Astronomia" \
   --period-months 24 \
   --output-pdf "reports/astronomy.pdf" \
   --json
 Mode B: Human-Readable Console Output 
 Bash docker run --rm \
   wikipedia-analytics-skill:latest \
   --articles "uk:Інтервальне_голодування,pl:Intermittent_fasting" \
   --period-months 12
   📋 CLI Options ReferenceOptionTypeRequiredDefaultDescription--articlesStringYes—Comma-separated lang:Title (spaces replaced by _).--period-monthsIntNo24Months of historical data to analyze.--output-pdfStringNo—Relative path to save 1-page PDF report.--jsonFlagNofalseReturns token-optimized JSON summary for LLM.🧪 Testing & RoadmapBash# Run Python E2E harness with Anthropic API
   export ANTHROPIC_API_KEY="your-key"
   python3 tests/test_e2e.py
   [x] Phase 1: MVP Core (Pageviews REST API, PDF generation, basic regression).
   [x] Phase 2: Advanced Analytics (STL Seasonality, IQR Outliers, $R^2$, Disk Caching).
   [ ] Phase 3: Multi-Source Expansion (Google Trends API, Wikidata SPARQL integration)