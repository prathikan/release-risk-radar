# Release Risk Radar

Release Risk Radar is a dependency-free Java 17 CLI that estimates how much
validation a release deserves before production. It turns a small release
snapshot into a transparent score with named risk factors and concrete
recommendations.

This is not a prediction of whether a release will fail. It is a repeatable
conversation tool for deciding where to spend review, testing, and rollout
attention.

## Risk model

The score is 0–100 and is composed of six explainable factors:

| Factor | Weight | Signal |
| --- | ---: | --- |
| Change scope | 25 | Changed files and code churn |
| Test confidence | 25 | Coverage and changed files without tests |
| Ownership spread | 15 | Number of owners involved in the change |
| Hotspot exposure | 20 | Known high-churn or high-defect files touched |
| Release cadence | 10 | Time since the previous release |
| Rollback history | 5 | Recent rollback count |

Risk bands are `LOW` (0–24), `MEDIUM` (25–49), `HIGH` (50–74), and
`CRITICAL` (75–100). Every report shows the factor contributions so a team can
challenge or tune the model instead of trusting a black box.

## Run it

```bash
./bin/run.sh
./bin/run.sh --input data/releases.csv --release checkout-2025-01-14 --format json
```

## Input format

```csv
release_id,service,changed_files,lines_added,lines_removed,test_coverage,files_without_tests,unique_owners,hotspot_files,days_since_last_release,rollback_count
checkout-2025-01-14,checkout,18,920,240,71,5,4,6,18,1
```

Coverage is a percentage from 0 to 100. Counts must be non-negative. If
`--release` is omitted, all releases are analyzed and ranked highest risk
first.

## Test it

```bash
./bin/test.sh
```

Tests verify the scoring thresholds, factor explanations, input validation, and
JSON output.

## Repository layout

```text
src/main/java/com/example/releaserisk/
  ReleaseSnapshot.java
  RiskAnalyzer.java
  CsvReleaseReader.java
  RiskReportFormatter.java
  Main.java
src/test/java/.../RiskAnalyzerTest.java
data/releases.csv
bin/run.sh
bin/test.sh
```

## Practical use

Run the tool during release preparation, then use the top factor as the next
action: add targeted tests for low coverage, stage a rollout for hotspot
exposure, or assign a focused owner when coordination risk is high. The model
is intentionally small so teams can adjust weights as they learn.