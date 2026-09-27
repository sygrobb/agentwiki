Wikipedia Analytics Skill for AI Agents
An enterprise-ready Agent Skill designed for LLM agents (e.g., Claude 3.5 Haiku) to analyze topic interest dynamics across Wikipedia language editions, decompose seasonality/outliers, compare performance against language baselines, and generate executive 1-page PDF reports.

📌 Executive Summary
Architecture: External LLM Agent → Bash Wrapper (run.sh) → High-Performance Java 21 CLI.

Pure Java CLI starts in ~0.2s (vs 3–5s in Spring Boot), avoiding LLM tool call timeouts.

Token Saver: Heavy math, seasonal decomposition (STL), baseline comparisons, and PDF rendering are offloaded to Java, returning only a compact JSON summary to the LLM.

🛠 Prerequisites & Dependencies
To build and run this skill locally or inside containerized environments, ensure you have the following installed:

System Requirements
Docker Engine (Recommended): Version 20.10+ (for containerized, reproducible execution without installing Java locally).

Local Development Requirements (if running without Docker)
JDK 21 (Java Development Kit 21 or higher).

Apache Maven 3.8+.

System Fonts: TrueType DejaVu Fonts (ttf-dejavu or fontconfig) for Cyrillic/Unicode rendering in PDFs.

Python 3.10+ (Optional, required only for running the Anthropic E2E test harness in tests/test_e2e.py).

⚙️ Key Assumptions & Constants (Phase 3)
Market Intelligence Baseline Growth
MarketIntelligenceService evaluates whether a specific topic is outperforming or underperforming its overall language Wikipedia edition.

Baseline Growth Constants (ANNUAL_BASELINE_GROWTH):

Ukrainian Wikipedia (uk): 4.5% annual growth baseline.

Polish Wikipedia (pl): 2.1% annual growth baseline.

English / Default (en / others): 3.0% annual growth baseline.

Note on Assumptions: These baselines represent average year-over-year baseline traffic growth for these language sections. Relative growth is calculated as:

Relative Growth=Deseasonalized Article Growth−Baseline Growth
Verdict Thresholds
Verdict	Condition	Business Meaning
HIGH_OUTPERFORMER	Relative Growth>+10.0%	Article demand significantly outpaces total language traffic.
MODERATE_GROWTH	0.0%<Relative Growth≤+10.0%	Article grows inline with general language Wikipedia growth.
UNDERPERFORMING	−10.0%≤Relative Growth≤0.0%	Article interest grows slower than the overall Wikipedia market.
DECLINING_INTEREST	Relative Growth<−10.0%	Article is losing mindshare/traffic share rapidly.
🛠 Core Capabilities
Feature	Description
Market Intelligence (Phase 3)	Benchmarks article performance against language baseline traffic and outputs a market verdict.
Semantic Clustering (Phase 3)	Automatically expands a single Wikidata QID into across-language Wikipedia article titles via SPARQL.
Seasonality Clean-up	Separates organic growth from annual cyclic peaks (e.g., academic calendar spikes).
Anomaly Filtering	Suppresses viral spikes, news noise, and bot traffic using Interquartile Range (IQR).
Confidence Scoring	Computes R
2
linear regression quality to output a confidence_score (0.0−1.0).
Disk Caching	Local .cache/wiki/ stores raw API responses for instant sub-second re-runs.
Cyrillic PDF Engine	Generates 1-page executive charts & tables using OpenPDF + DejaVu UTF-8 fonts.
📂 Project Structure
Plaintext
├── SKILL.md                 # Agent Skill instructions & JSON Schema for LLM
├── Dockerfile               # Multi-stage reproducible build (Maven + JRE 21)
├── run.sh                   # Entrypoint script (UTF-8, Headless AWT, CLI args)
├── pom.xml                  # Build configuration & dependencies
├── src/main/java/org/dmg/
│   ├── WikiApp.java                  # Main CLI & JSON DTO mapper
│   ├── WikipediaApiClient.java       # HTTP Client with disk caching
│   ├── TrendAnalyzer.java            # STL decomposition, IQR & R² math
│   ├── MarketIntelligenceService.java# Baseline growth & market verdict engine
│   ├── WikidataSparqlClient.java     # Wikidata QID semantic expansion
│   └── PdfReportGenerator.java       # JFreeChart + OpenPDF renderer
└── tests/
└── test_e2e.py          # Python E2E test harness for LLM agents
🚀 Quick Start Guide & How to Run
1. Build the Docker Image
   Bash
   docker build -t wikipedia-analytics-skill:latest .
2. Execution Modes
   Mode A: JSON Output for LLM Agent (Docker)
   Returns token-optimized JSON summary containing metrics, market verdicts, and semantic cluster data.

Bash
docker run --rm -v "$(pwd)/reports:/skill/reports" \
wikipedia-analytics-skill:latest \
--articles "uk:Астрономія,pl:Astronomia" \
--period-months 24 \
--output-pdf "reports/astronomy.pdf" \
--json
Mode B: Semantic Expansion via Wikidata QID (Phase 3)
Pass a Wikidata QID (e.g., Q11033 for Astronomy) to automatically resolve titles in multiple languages (uk, pl, en).

Bash
docker run --rm -v "$(pwd)/reports:/skill/reports" \
wikipedia-analytics-skill:latest \
--wikidata-qid "Q11033" \
--expand-cluster \
--period-months 24 \
--output-pdf "reports/astronomy_cluster.pdf" \
--json
Mode C: Human-Readable Console Output
Useful for manual testing or inspecting output directly in the console.

Bash
docker run --rm \
wikipedia-analytics-skill:latest \
--articles "uk:Інтервальне_голодування,pl:Intermittent_fasting" \
--period-months 12
📋 CLI Options Reference
Option	Type	Required	Default	Description
--articles	String	Conditional	—	Comma-separated lang:Title (spaces replaced by _). Required unless --wikidata-qid is provided.
--wikidata-qid	String	Conditional	—	Wikidata Entity ID (e.g., Q11033) for cross-language cluster discovery.
--expand-cluster	Flag	No	false	Automatically resolves titles across languages using Wikidata SPARQL.
--period-months	Int	No	24	Historical window in months to analyze (6, 12, or 24).
--output-pdf	String	No	—	Relative path where the 1-page PDF report will be saved.
--json	Flag	No	false	Emits token-optimized JSON payload for LLM parsing.
🧪 Testing & Roadmap
Bash
# Run Python E2E harness with Anthropic API
export ANTHROPIC_API_KEY="your-key"
python3 tests/e2etest.py
[x] Phase 1: MVP Core (Pageviews REST API, PDF generation, basic regression).

[x] Phase 2: Advanced Analytics (STL Seasonality, IQR Outliers, R
2
, Disk Caching).

[x] Phase 3: Market Intelligence (Baseline Growth Alignment, Wikidata SPARQL Clusters, Extended PDF Report).

[ ] Phase 4: Multi-Source Expansion (Google Trends API Integration).