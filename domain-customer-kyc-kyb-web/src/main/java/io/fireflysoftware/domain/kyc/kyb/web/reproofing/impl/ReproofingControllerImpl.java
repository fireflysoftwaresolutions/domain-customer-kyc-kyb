package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.ReproofingServiceImpl;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(name="firefly.reproofing.enabled",havingValue="true")
@RequestMapping("/api/v1/identity-recovery/cases")
public final class ReproofingControllerImpl {
    private final ReproofingServiceImpl service;
    public ReproofingControllerImpl(ReproofingServiceImpl service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(operationId="createReproofingJourney")
    public Mono<ReproofingJourney> create(@RequestBody CreateReproofingCaseRequest r){return service.create(r);}
    @PostMapping("/{caseId}/inspect") @Operation(operationId="inspectReproofingJourney")
    public Mono<ReproofingCaseResponse> inspect(@PathVariable UUID caseId,@RequestBody ConfirmReproofingRequest r){return service.inspect(caseId,r);}
    @PostMapping("/{caseId}/confirm") @Operation(operationId="confirmReproofingJourney")
    public Mono<ReproofingCaseResponse> confirm(@PathVariable UUID caseId,@RequestBody ConfirmReproofingRequest r){return service.confirm(caseId,r);}
    @PostMapping("/{caseId}/authorizations/consume") @Operation(operationId="consumeReproofingJourney")
    public Mono<ReproofingAuthorization> consume(@PathVariable UUID caseId,@RequestBody ReproofingAuthorizationRequest r){return service.consume(caseId,r);}
    @PostMapping("/{caseId}/authorizations/verify") @Operation(operationId="verifyReproofingJourney")
    public Mono<ReproofingAuthorization> verify(@PathVariable UUID caseId,@RequestBody ReproofingAuthorizationRequest r){return service.verify(caseId,r);}
    @PostMapping("/{caseId}/executions") @Operation(operationId="recordReproofingJourney")
    public Mono<ReproofingCaseResponse> record(@PathVariable UUID caseId,@RequestBody RecordReproofingExecutionRequest r){return service.record(caseId,r);}
}
