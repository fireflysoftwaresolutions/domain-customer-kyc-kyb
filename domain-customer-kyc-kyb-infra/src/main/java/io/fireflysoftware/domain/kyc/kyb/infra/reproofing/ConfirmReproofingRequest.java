package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ConfirmReproofingRequest(ReproofingBinding binding) {
    @Override public String toString() { return "ConfirmReproofingRequest[redacted]"; }
}
