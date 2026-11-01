package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record BindReproofingSessionRequest(ReproofingBinding binding, String providerSessionId) {
    @Override public String toString() { return "BindReproofingSessionRequest[redacted]"; }
}
