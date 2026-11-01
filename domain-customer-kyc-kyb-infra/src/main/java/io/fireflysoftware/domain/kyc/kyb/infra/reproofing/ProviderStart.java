package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ProviderStart(String providerSessionId, String verificationUrl) {
    @Override public String toString() { return "ProviderStart[redacted]"; }
}
