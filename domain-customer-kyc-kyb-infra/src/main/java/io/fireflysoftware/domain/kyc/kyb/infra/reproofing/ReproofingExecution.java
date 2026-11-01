package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingExecution(UUID caseId, UUID executionId, ReproofingBinding binding, String outcome, Instant committedAt) {
    @Override public String toString() { return "ReproofingExecution[redacted]"; }
}
