package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingAdmission(ReproofingBinding binding, boolean eligible, Instant expiresAt) {
    @Override public String toString() { return "ReproofingAdmission[redacted]"; }
}
