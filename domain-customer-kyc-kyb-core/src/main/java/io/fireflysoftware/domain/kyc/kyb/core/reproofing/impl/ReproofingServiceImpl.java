package io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.time.Duration;
import java.util.*;
import reactor.core.publisher.Mono;

public final class ReproofingServiceImpl {
    private final ReproofingCorePort core;
    private final ReproofingNativePort nativePort;
    private final ReproofingProviderPort provider;
    private final ReproofingPolicyImpl policy;
    public ReproofingServiceImpl(ReproofingCorePort core, ReproofingNativePort nativePort, ReproofingProviderPort provider, ReproofingPolicyImpl policy) {
        this.core=core; this.nativePort=nativePort; this.provider=provider; this.policy=policy;
    }
    public Mono<ReproofingJourney> create(CreateReproofingCaseRequest request) {
        return Mono.defer(()->{
            policy.binding(request.binding());
            if(provider==null) return Mono.error(ReproofingPolicyImpl.unavailable());
            return nativePort.prepare(request.binding(),null).switchIfEmpty(Mono.error(ReproofingPolicyImpl.unavailable())).doOnNext(a->policy.preparation(request.binding(),a))
                .flatMap(a->core.create(request)).flatMap(c->{
                    policy.reproofingCase(request.binding(),c);
                    if(!provider.providerId().equals(c.providerId())) return Mono.error(ReproofingPolicyImpl.unavailable());
                    // The persisted Core reservation is the only begin key, including after an ambiguous response or restart.
                    return provider.begin(c).switchIfEmpty(Mono.error(ReproofingPolicyImpl.unavailable())).flatMap(start->{
                        ReproofingPolicyImpl.text(start.providerSessionId());
                        if(c.providerSessionId()!=null && !c.providerSessionId().equals(start.providerSessionId())) throw ReproofingPolicyImpl.denied();
                        String url=policy.journeyUrl(start.verificationUrl());
                        return core.bind(c.caseId(),new BindReproofingSessionRequest(c.binding(),start.providerSessionId()))
                            .map(bound->{policy.reproofingCase(c.binding(),bound);
                                if(!start.providerSessionId().equals(bound.providerSessionId()) || !c.providerStartId().equals(bound.providerStartId())
                                    || !c.expiresAt().equals(bound.expiresAt())) throw ReproofingPolicyImpl.denied();
                                return new ReproofingJourney(bound,url);});
                    });
                });
        }).timeout(Duration.ofSeconds(15));
    }
    public Mono<ReproofingCaseResponse> inspect(UUID id, ConfirmReproofingRequest r) {
        return Mono.defer(()->{policy.binding(r.binding()); return core.inspect(id,r).doOnNext(c->{
            if(!id.equals(c.caseId()) || !r.binding().equals(c.binding())) throw ReproofingPolicyImpl.denied();
        });});
    }
    public Mono<ReproofingCaseResponse> confirm(UUID id, ConfirmReproofingRequest r) {
        return prepared(r.binding(),id).then(Mono.defer(()->core.confirm(id,r)));
    }
    public Mono<ReproofingAuthorization> consume(UUID id, ReproofingAuthorizationRequest r) {
        return admitted(r.binding(),id).then(Mono.defer(()->core.consume(id,r)));
    }
    public Mono<ReproofingAuthorization> verify(UUID id, ReproofingAuthorizationRequest r) {
        return admitted(r.binding(),id).then(Mono.defer(()->core.verify(id,r)));
    }
    public Mono<ReproofingCaseResponse> record(UUID id, RecordReproofingExecutionRequest r) {
        return Mono.defer(()->{policy.binding(r.binding()); if(!"CREDENTIALS_REPLACED".equals(r.outcome())) throw ReproofingPolicyImpl.denied(); return core.record(id,r);});
    }
    public Mono<VerifiedReproofingEvidence> evidence(ReproofingEvidenceRequest r) {
        return Mono.defer(()->{
            policy.binding(r.binding());
            if(provider==null || !provider.providerId().equals(r.providerId())) return Mono.error(ReproofingPolicyImpl.unavailable());
            return core.inspect(r.caseId(),new ConfirmReproofingRequest(r.binding())).flatMap(c->{
                policy.reproofingCase(r.binding(),c);
                if(!Objects.equals(c.providerSessionId(),r.providerSessionId()) || !c.providerId().equals(r.providerId()) || !c.policyId().equals(r.policyId())) throw ReproofingPolicyImpl.denied();
                return provider.verify(r).switchIfEmpty(Mono.error(ReproofingPolicyImpl.unavailable())).doOnNext(e->policy.evidence(r,c,e));
            });
        }).timeout(Duration.ofSeconds(10));
    }
    private Mono<Void> prepared(ReproofingBinding b, UUID id) {
        return Mono.defer(()->{policy.binding(b); return nativePort.prepare(b,id).doOnNext(a->policy.preparation(b,a)).switchIfEmpty(Mono.error(ReproofingPolicyImpl.unavailable())).then();});
    }
    private Mono<Void> admitted(ReproofingBinding b, UUID id) {
        return Mono.defer(()->{policy.binding(b); return nativePort.admit(b,id).doOnNext(a->policy.admission(b,a)).switchIfEmpty(Mono.error(ReproofingPolicyImpl.unavailable())).then();});
    }
}
