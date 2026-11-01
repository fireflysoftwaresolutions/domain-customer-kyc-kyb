package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.time.Instant;
import java.util.UUID;

public record ReproofingBinding(String issuer, String realm, String tenant, String subject, UUID partyId, String population, String purpose, String generation, String credentialRevision, String nonce) {
    @Override public String toString() { return "ReproofingBinding[redacted]"; }
}
