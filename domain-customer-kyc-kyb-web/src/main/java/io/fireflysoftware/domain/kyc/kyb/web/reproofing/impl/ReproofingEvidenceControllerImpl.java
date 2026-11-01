package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.ReproofingServiceImpl;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Hidden
@RestController
@ConditionalOnProperty(name="firefly.reproofing.enabled",havingValue="true")
public final class ReproofingEvidenceControllerImpl {
    private final ReproofingServiceImpl service;
    public ReproofingEvidenceControllerImpl(ReproofingServiceImpl service){this.service=service;}
    @PostMapping("/internal/identity-recovery/evidence")
    public Mono<VerifiedReproofingEvidence> evidence(@RequestBody ReproofingEvidenceRequest request){return service.evidence(request);}
}
