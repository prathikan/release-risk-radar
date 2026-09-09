package com.example.releaserisk;

import java.nio.file.Path;
import java.util.Arrays;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        var releases = CsvReleaseReader.read(options.input());
        var reports = releases.stream()
            .filter(release -> options.releaseId() == null || release.releaseId().equals(options.releaseId()))
            .map(new RiskAnalyzer()::analyze)
            .sorted((left, right) -> Double.compare(right.score(), left.score()))
            .toList();
        if (reports.isEmpty()) {
            throw new IllegalArgumentException("No matching releases found.");
        }
        System.out.println(options.json()
            ? RiskReportFormatter.json(reports)
            : RiskReportFormatter.text(reports));
    }

    private record Options(Path input, boolean json, String releaseId) {
        static Options parse(String[] args) {
            Path input = Path.of("data/releases.csv");
            boolean json = false;
            String releaseId = null;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--input" -> input = Path.of(next(args, ++i, "--input"));
                    case "--format" -> json = "json".equalsIgnoreCase(next(args, ++i, "--format"));
                    case "--release" -> releaseId = next(args, ++i, "--release");
                    case "--help", "-h" -> {
                        System.out.println("Usage: ./bin/run.sh [--input file] [--release id] [--format text|json]");
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]
                        + "\nUse --help for usage.");
                }
            }
            return new Options(input, json, releaseId);
        }

        private static String next(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value. Arguments: "
                    + Arrays.toString(args));
            }
            return args[index];
        }
    }
}