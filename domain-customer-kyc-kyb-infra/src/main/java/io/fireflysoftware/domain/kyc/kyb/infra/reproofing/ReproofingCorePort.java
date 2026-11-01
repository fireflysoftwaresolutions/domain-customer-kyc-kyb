package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ReproofingCorePort {
    Mono<ReproofingCaseResponse> create(CreateReproofingCaseRequest request);
    Mono<ReproofingCaseResponse> inspect(UUID id, ConfirmReproofingRequest request);
    Mono<ReproofingCaseResponse> bind(UUID id, BindReproofingSessionRequest request);
    Mono<ReproofingCaseResponse> confirm(UUID id, ConfirmReproofingRequest request);
    Mono<ReproofingAuthorization> consume(UUID id, ReproofingAuthorizationRequest request);
    Mono<ReproofingAuthorization> verify(UUID id, ReproofingAuthorizationRequest request);
    Mono<ReproofingCaseResponse> record(UUID id, RecordReproofingExecutionRequest request);
}
