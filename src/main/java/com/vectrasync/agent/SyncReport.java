package com.vectrasync.agent;

import java.time.Instant;

public record SyncReport(
        Instant startedAt,
        Instant finishedAt,
        String fileName,
        String crmName,
        long total,
        long created,
        long updated,
        long failed
) {
    public long processed() { return created + updated + failed; }

    public double successRate() {
        long p = processed();
        return p == 0 ? 0 : (double) (created + updated) / p;
    }
}
