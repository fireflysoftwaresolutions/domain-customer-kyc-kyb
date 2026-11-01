package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingAuthorizationRequest(ReproofingBinding binding, UUID authorizationId, UUID executionId) {
    @Override public String toString() { return "ReproofingAuthorizationRequest[redacted]"; }
}
