package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingEvidenceRequest(UUID caseId, ReproofingBinding binding, String providerId, String policyId, String providerSessionId) {
    @Override public String toString() { return "ReproofingEvidenceRequest[redacted]"; }
}
