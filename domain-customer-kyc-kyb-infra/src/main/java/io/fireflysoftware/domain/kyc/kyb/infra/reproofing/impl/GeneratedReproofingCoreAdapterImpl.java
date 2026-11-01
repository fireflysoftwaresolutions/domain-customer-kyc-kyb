package io.fireflysoftware.domain.kyc.kyb.infra.reproofing.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.core.kycb.sdk.api.IdentityReproofingApi;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.time.Duration;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Maps independent product records to the original producer's generated SDK models. */
public final class GeneratedReproofingCoreAdapterImpl implements ReproofingCorePort {
    private final IdentityReproofingApi api;
    private final ObjectMapper mapper;
    public GeneratedReproofingCoreAdapterImpl(IdentityReproofingApi api, ObjectMapper mapper) { this.api=api; this.mapper=mapper; }
    private <T> T map(Object source, Class<T> type) { return mapper.convertValue(source,type); }
    private <T> Mono<T> result(Mono<?> source, Class<T> type) { return source.map(v->map(v,type)).timeout(Duration.ofSeconds(10)); }
    public Mono<ReproofingCaseResponse> create(CreateReproofingCaseRequest r) { return result(api.createReproofingCase(map(r,io.fireflysoftware.core.kycb.sdk.model.CreateReproofingCaseRequest.class),null),ReproofingCaseResponse.class); }
    public Mono<ReproofingCaseResponse> inspect(UUID id, ConfirmReproofingRequest r) { return result(api.inspectReproofingCase(id,map(r,io.fireflysoftware.core.kycb.sdk.model.ConfirmReproofingRequest.class),null),ReproofingCaseResponse.class); }
    public Mono<ReproofingCaseResponse> bind(UUID id, BindReproofingSessionRequest r) { return result(api.bindReproofingSession(id,map(r,io.fireflysoftware.core.kycb.sdk.model.BindReproofingSessionRequest.class),null),ReproofingCaseResponse.class); }
    public Mono<ReproofingCaseResponse> confirm(UUID id, ConfirmReproofingRequest r) { return result(api.confirmReproofingCase(id,map(r,io.fireflysoftware.core.kycb.sdk.model.ConfirmReproofingRequest.class),null),ReproofingCaseResponse.class); }
    public Mono<ReproofingAuthorization> consume(UUID id, ReproofingAuthorizationRequest r) { return result(api.consumeReproofingAuthorization(id,map(r,io.fireflysoftware.core.kycb.sdk.model.ReproofingAuthorizationRequest.class),null),ReproofingAuthorization.class); }
    public Mono<ReproofingAuthorization> verify(UUID id, ReproofingAuthorizationRequest r) { return result(api.verifyReproofingAuthorization(id,map(r,io.fireflysoftware.core.kycb.sdk.model.ReproofingAuthorizationRequest.class),null),ReproofingAuthorization.class); }
    public Mono<ReproofingCaseResponse> record(UUID id, RecordReproofingExecutionRequest r) { return result(api.recordReproofingExecution(id,map(r,io.fireflysoftware.core.kycb.sdk.model.RecordReproofingExecutionRequest.class),null),ReproofingCaseResponse.class); }
}
