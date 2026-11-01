package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record CreateReproofingCaseRequest(ReproofingBinding binding) {
    @Override public String toString() { return "CreateReproofingCaseRequest[redacted]"; }
}
