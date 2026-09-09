package com.example.releaserisk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RiskAnalyzer {
    public RiskReport analyze(ReleaseSnapshot release) {
        List<Factor> factors = new ArrayList<>();
        factors.add(factor("Change scope", 25, scopeSignal(release),
            "Large diffs increase the number of paths that need validation."));
        factors.add(factor("Test confidence", 25, testSignal(release),
            "Lower coverage and untested changed files reduce confidence."));
        factors.add(factor("Ownership spread", 15, ownerSignal(release),
            "More owners increase coordination and review handoff risk."));
        factors.add(factor("Hotspot exposure", 20, hotspotSignal(release),
            "Known high-churn files deserve extra verification and rollout care."));
        factors.add(factor("Release cadence", 10, cadenceSignal(release),
            "Long gaps can allow more changes to accumulate before feedback."));
        factors.add(factor("Rollback history", 5, rollbackSignal(release),
            "Recent rollbacks are a signal to prefer a smaller rollout."));

        double score = factors.stream().mapToDouble(Factor::points).sum();
        RiskBand band = RiskBand.fromScore(score);
        List<String> recommendations = recommendations(factors, band);
        return new RiskReport(release, score, band, factors, recommendations);
    }

    private Factor factor(String name, int weight, double signal, String explanation) {
        return new Factor(name, weight, signal, weight * signal, explanation);
    }

    private double scopeSignal(ReleaseSnapshot release) {
        double fileSignal = Math.min(1, release.changedFiles() / 30.0);
        double churn = release.linesAdded() + release.linesRemoved();
        double churnSignal = Math.min(1, churn / 1800.0);
        return fileSignal * 0.6 + churnSignal * 0.4;
    }

    private double testSignal(ReleaseSnapshot release) {
        double coverageGap = 1 - release.testCoverage() / 100;
        double untestedRatio = release.changedFiles() == 0
            ? 0
            : Math.min(1, release.filesWithoutTests() / (double) release.changedFiles());
        return coverageGap * 0.7 + untestedRatio * 0.3;
    }

    private double ownerSignal(ReleaseSnapshot release) {
        return Math.min(1, Math.max(0, release.uniqueOwners() - 1) / 5.0);
    }

    private double hotspotSignal(ReleaseSnapshot release) {
        return release.changedFiles() == 0
            ? 0
            : Math.min(1, release.hotspotFiles() / (double) release.changedFiles());
    }

    private double cadenceSignal(ReleaseSnapshot release) {
        return Math.min(1, release.daysSinceLastRelease() / 30.0);
    }

    private double rollbackSignal(ReleaseSnapshot release) {
        return Math.min(1, release.rollbackCount() / 3.0);
    }

    private List<String> recommendations(List<Factor> factors, RiskBand band) {
        List<String> actions = new ArrayList<>();
        factors.stream()
            .filter(factor -> factor.signal() >= 0.6)
            .sorted(Comparator.comparing(Factor::points).reversed())
            .forEach(factor -> actions.add(actionFor(factor.name())));
        if (actions.isEmpty()) {
            actions.add("Proceed with the normal review and monitoring checklist.");
        }
        if (band == RiskBand.CRITICAL || band == RiskBand.HIGH) {
            actions.add("Use a staged rollout with an explicit rollback owner.");
        }
        return List.copyOf(actions);
    }

    private String actionFor(String factor) {
        return switch (factor) {
            case "Change scope" -> "Split the change or validate the highest-impact paths first.";
            case "Test confidence" -> "Add targeted tests around changed files before release.";
            case "Ownership spread" -> "Name one release owner and collect reviews asynchronously.";
            case "Hotspot exposure" -> "Exercise hotspot behavior with production-like traffic.";
            case "Release cadence" -> "Run a smaller canary to shorten the feedback loop.";
            case "Rollback history" -> "Confirm rollback steps and monitor the first rollout slice.";
            default -> "Review the release with the owning team.";
        };
    }

    public enum RiskBand {
        LOW, MEDIUM, HIGH, CRITICAL;

        static RiskBand fromScore(double score) {
            if (score >= 75) return CRITICAL;
            if (score >= 50) return HIGH;
            if (score >= 25) return MEDIUM;
            return LOW;
        }
    }

    public record Factor(String name, int weight, double signal, double points, String explanation) {}

    public record RiskReport(
        ReleaseSnapshot release,
        double score,
        RiskBand band,
        List<Factor> factors,
        List<String> recommendations
    ) {}
}