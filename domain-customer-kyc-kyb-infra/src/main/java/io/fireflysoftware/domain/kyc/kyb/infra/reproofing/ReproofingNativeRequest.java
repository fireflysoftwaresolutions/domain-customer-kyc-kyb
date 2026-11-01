package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingNativeRequest(ReproofingBinding binding, UUID caseId, UUID executionId) {
    @Override public String toString() { return "ReproofingNativeRequest[redacted]"; }
}
