package com.example.releaserisk;

import java.util.List;

public final class RiskAnalyzerTest {
    public static void main(String[] args) {
        lowRiskReleaseStaysLow();
        highRiskReleaseExplainsItsRisk();
        scoreIsBoundedAndBandThresholdsAreStable();
        invalidSnapshotIsRejected();
        System.out.println("Release Risk Radar tests passed.");
    }

    private static void lowRiskReleaseStaysLow() {
        var release = new ReleaseSnapshot("safe", "catalog", 3, 30, 10, 98, 0, 1, 0, 2, 0);
        var report = new RiskAnalyzer().analyze(release);
        assertEquals(RiskAnalyzer.RiskBand.LOW, report.band());
        assertTrue(report.score() < 25);
        assertTrue(report.recommendations().get(0).contains("normal review"));
    }

    private static void highRiskReleaseExplainsItsRisk() {
        var release = new ReleaseSnapshot("risky", "identity", 45, 3000, 1500, 38, 30, 7, 35, 60, 3);
        var report = new RiskAnalyzer().analyze(release);
        assertTrue(report.score() >= 75);
        assertEquals(RiskAnalyzer.RiskBand.CRITICAL, report.band());
        assertTrue(report.factors().stream().anyMatch(factor -> factor.name().equals("Test confidence")
            && factor.points() > 15));
        assertTrue(report.recommendations().stream().anyMatch(action -> action.contains("staged rollout")));
    }

    private static void scoreIsBoundedAndBandThresholdsAreStable() {
        var minimum = new ReleaseSnapshot("min", "svc", 0, 0, 0, 100, 0, 0, 0, 0, 0);
        var maximum = new ReleaseSnapshot("max", "svc", 100, 10000, 10000, 0, 100, 100, 100, 100, 100);
        var analyzer = new RiskAnalyzer();
        assertNear(0, analyzer.analyze(minimum).score());
        assertNear(100, analyzer.analyze(maximum).score());
        assertEquals(RiskAnalyzer.RiskBand.LOW, RiskAnalyzer.RiskBand.fromScore(24.99));
        assertEquals(RiskAnalyzer.RiskBand.MEDIUM, RiskAnalyzer.RiskBand.fromScore(25));
        assertEquals(RiskAnalyzer.RiskBand.HIGH, RiskAnalyzer.RiskBand.fromScore(50));
        assertEquals(RiskAnalyzer.RiskBand.CRITICAL, RiskAnalyzer.RiskBand.fromScore(75));
    }

    private static void invalidSnapshotIsRejected() {
        try {
            new ReleaseSnapshot("bad", "svc", 2, 1, 1, 101, 0, 1, 0, 1, 0);
            throw new AssertionError("Expected invalid coverage to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("test_coverage"));
        }
    }

    private static void assertNear(double expected, double actual) {
        assert Math.abs(expected - actual) < 0.001 :
            "Expected " + expected + " but got " + actual;
    }

    private static void assertEquals(Object expected, Object actual) {
        assert expected.equals(actual) : "Expected " + expected + " but got " + actual;
    }

    private static void assertTrue(boolean value) {
        assert value : "Expected condition to be true";
    }
}