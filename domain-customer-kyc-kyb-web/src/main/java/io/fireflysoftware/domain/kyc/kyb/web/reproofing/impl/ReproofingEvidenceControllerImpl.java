package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.ReproofingServiceImpl;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Hidden
@RestController
public final class ReproofingEvidenceControllerImpl {
    private final org.springframework.beans.factory.ObjectProvider<ReproofingServiceImpl> services;
    public ReproofingEvidenceControllerImpl(org.springframework.beans.factory.ObjectProvider<ReproofingServiceImpl> services){this.services=services;}
    private ReproofingServiceImpl service(){var service=services.getIfAvailable(); if(service==null) throw io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.ReproofingPolicyImpl.unavailable(); return service;}
    @org.fireflyframework.web.idempotency.annotation.DisableIdempotency
    @PostMapping("/internal/identity-recovery/evidence")
    public Mono<VerifiedReproofingEvidence> evidence(@RequestBody ReproofingEvidenceRequest request){return service().evidence(request);}
}
