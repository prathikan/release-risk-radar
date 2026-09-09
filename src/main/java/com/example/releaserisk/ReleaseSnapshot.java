package com.example.releaserisk;

public record ReleaseSnapshot(
    String releaseId,
    String service,
    int changedFiles,
    int linesAdded,
    int linesRemoved,
    double testCoverage,
    int filesWithoutTests,
    int uniqueOwners,
    int hotspotFiles,
    int daysSinceLastRelease,
    int rollbackCount
) {
    public ReleaseSnapshot {
        if (releaseId == null || releaseId.isBlank() || service == null || service.isBlank()) {
            throw new IllegalArgumentException("release_id and service are required");
        }
        if (changedFiles < 0 || linesAdded < 0 || linesRemoved < 0 || filesWithoutTests < 0
            || uniqueOwners < 0 || hotspotFiles < 0 || daysSinceLastRelease < 0 || rollbackCount < 0) {
            throw new IllegalArgumentException("count and duration fields cannot be negative");
        }
        if (testCoverage < 0 || testCoverage > 100) {
            throw new IllegalArgumentException("test_coverage must be between 0 and 100");
        }
        if (filesWithoutTests > changedFiles || hotspotFiles > changedFiles) {
            throw new IllegalArgumentException("file subsets cannot exceed changed_files");
        }
    }
}