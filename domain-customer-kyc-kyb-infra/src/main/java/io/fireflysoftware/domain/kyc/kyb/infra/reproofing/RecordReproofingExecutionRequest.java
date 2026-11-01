package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record RecordReproofingExecutionRequest(ReproofingBinding binding, UUID authorizationId, UUID executionId, String outcome) {
    @Override public String toString() { return "RecordReproofingExecutionRequest[redacted]"; }
}
