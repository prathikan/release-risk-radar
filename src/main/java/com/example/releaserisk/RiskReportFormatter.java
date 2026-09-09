package com.example.releaserisk;

import java.util.List;
import java.util.Locale;

public final class RiskReportFormatter {
    private RiskReportFormatter() {}

    public static String text(List<RiskAnalyzer.RiskReport> reports) {
        StringBuilder output = new StringBuilder();
        output.append("RELEASE RISK RADAR\n");
        output.append("==================\n");
        for (int index = 0; index < reports.size(); index++) {
            if (index > 0) output.append("\n\n");
            output.append(singleText(reports.get(index)));
        }
        return output.toString();
    }

    private static String singleText(RiskAnalyzer.RiskReport report) {
        var release = report.release();
        StringBuilder output = new StringBuilder();
        output.append(release.releaseId()).append(" / ").append(release.service()).append('\n');
        output.append("Risk score: ").append(String.format(Locale.ROOT, "%.1f", report.score()))
            .append(" / 100 (").append(report.band()).append(")\n\n");
        output.append("FACTORS\n-------\n");
        for (var factor : report.factors()) {
            output.append(String.format(Locale.ROOT, "%-20s %5.1f points  %s%n",
                factor.name(), factor.points(), factor.explanation()));
        }
        output.append("\nRECOMMENDED ACTIONS\n-------------------\n");
        for (String recommendation : report.recommendations()) {
            output.append("- ").append(recommendation).append('\n');
        }
        return output.toString();
    }

    public static String json(List<RiskAnalyzer.RiskReport> reports) {
        StringBuilder output = new StringBuilder("[");
        for (int index = 0; index < reports.size(); index++) {
            if (index > 0) output.append(',');
            var report = reports.get(index);
            output.append("{\"releaseId\":\"").append(escape(report.release().releaseId()))
                .append("\",\"service\":\"").append(escape(report.release().service()))
                .append("\",\"score\":").append(number(report.score()))
                .append(",\"band\":\"").append(report.band()).append("\",\"factors\":[");
            for (int factorIndex = 0; factorIndex < report.factors().size(); factorIndex++) {
                if (factorIndex > 0) output.append(',');
                var factor = report.factors().get(factorIndex);
                output.append("{\"name\":\"").append(escape(factor.name()))
                    .append("\",\"weight\":").append(factor.weight())
                    .append(",\"signal\":").append(number(factor.signal()))
                    .append(",\"points\":").append(number(factor.points()))
                    .append(",\"explanation\":\"").append(escape(factor.explanation())).append("\"}");
            }
            output.append("],\"recommendations\":[");
            for (int recommendationIndex = 0; recommendationIndex < report.recommendations().size(); recommendationIndex++) {
                if (recommendationIndex > 0) output.append(',');
                output.append("\"").append(escape(report.recommendations().get(recommendationIndex))).append("\"");
            }
            output.append("]}");
        }
        return output.append(']').toString();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}