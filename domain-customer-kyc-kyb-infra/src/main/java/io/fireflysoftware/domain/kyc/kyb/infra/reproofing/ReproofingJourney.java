package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingJourney(@com.fasterxml.jackson.annotation.JsonProperty("case") ReproofingCaseResponse reproofingCase, String verificationUrl) {
    @Override public String toString() { return "ReproofingJourney[redacted]"; }
}
