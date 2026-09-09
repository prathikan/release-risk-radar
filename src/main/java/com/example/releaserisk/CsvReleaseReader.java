package com.example.releaserisk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CsvReleaseReader {
    private CsvReleaseReader() {}

    public static List<ReleaseSnapshot> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) return List.of();
        List<String> headers = parseLine(lines.get(0));
        Map<String, Integer> positions = headers.stream()
            .map(String::trim)
            .collect(Collectors.toMap(header -> header, headers::indexOf));
        String[] required = {
            "release_id", "service", "changed_files", "lines_added", "lines_removed",
            "test_coverage", "files_without_tests", "unique_owners", "hotspot_files",
            "days_since_last_release", "rollback_count"
        };
        Arrays.stream(required).filter(column -> !positions.containsKey(column)).findFirst()
            .ifPresent(column -> { throw new IllegalArgumentException("Missing CSV column: " + column); });

        List<ReleaseSnapshot> releases = new ArrayList<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            if (lines.get(lineNumber).isBlank()) continue;
            List<String> row = parseLine(lines.get(lineNumber));
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException("Line " + (lineNumber + 1)
                    + " has " + row.size() + " columns; expected " + headers.size());
            }
            try {
                releases.add(new ReleaseSnapshot(
                    value(row, positions, "release_id"),
                    value(row, positions, "service"),
                    integer(row, positions, "changed_files"),
                    integer(row, positions, "lines_added"),
                    integer(row, positions, "lines_removed"),
                    decimal(row, positions, "test_coverage"),
                    integer(row, positions, "files_without_tests"),
                    integer(row, positions, "unique_owners"),
                    integer(row, positions, "hotspot_files"),
                    integer(row, positions, "days_since_last_release"),
                    integer(row, positions, "rollback_count")
                ));
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("Invalid release on line " + (lineNumber + 1)
                    + ": " + error.getMessage(), error);
            }
        }
        return releases;
    }

    private static String value(List<String> row, Map<String, Integer> positions, String name) {
        return row.get(positions.get(name)).trim();
    }

    private static int integer(List<String> row, Map<String, Integer> positions, String name) {
        return Integer.parseInt(value(row, positions, name));
    }

    private static double decimal(List<String> row, Map<String, Integer> positions, String name) {
        return Double.parseDouble(value(row, positions, name));
    }

    static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted CSV field");
        values.add(value.toString());
        return values;
    }
}