package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ReproofingNativePort {
    Mono<ReproofingAdmission> prepare(ReproofingBinding binding, UUID caseId);
    Mono<ReproofingAdmission> admit(ReproofingBinding binding, UUID caseId);
}
