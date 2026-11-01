package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import reactor.core.publisher.Mono;

/** Provider owns authenticated, durable sessions; begin must reconcile the exact Core reservation key. */
public interface ReproofingProviderPort {
    String providerId();
    Mono<ProviderStart> begin(ReproofingCaseResponse reproofingCase);
    Mono<VerifiedReproofingEvidence> verify(ReproofingEvidenceRequest request);
}
