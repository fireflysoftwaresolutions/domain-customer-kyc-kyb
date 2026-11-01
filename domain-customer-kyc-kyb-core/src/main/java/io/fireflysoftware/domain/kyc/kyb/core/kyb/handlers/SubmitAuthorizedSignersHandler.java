/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.kyc.kyb.core.kyb.handlers;

import io.fireflysoftware.core.kycb.sdk.api.PowersOfAttorneyApi;
import io.fireflysoftware.core.kycb.sdk.model.PowerOfAttorneyDTO;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.SignerData;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.SubmissionResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.SubmitAuthorizedSignersCommand;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.mappers.KybMapper;
import io.fireflysoftware.domain.kyc.kyb.core.util.IdempotencyKeysImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.web.error.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Submits each authorized signer (Power of Attorney) via {@link PowersOfAttorneyApi}.
 *
 * <p>Signers are processed concurrently ({@link Flux#flatMap}) and a per-call
 * idempotency key is generated for every SDK invocation. Returns a
 * {@link SubmissionResult} containing all created power-of-attorney IDs,
 * mirroring {@link RegisterUbosHandler}.</p>
 */
@Slf4j
@RequiredArgsConstructor
@CommandHandlerComponent(timeout = 60000, retries = 2, metrics = true, tracing = true)
public class SubmitAuthorizedSignersHandler
        extends CommandHandler<SubmitAuthorizedSignersCommand, SubmissionResult> {

    private final PowersOfAttorneyApi powersOfAttorneyApi;
    private final KybMapper kybMapper;

    @Override
    protected Mono<SubmissionResult> doHandle(SubmitAuthorizedSignersCommand cmd) {
        log.info("Submitting {} authorized signer(s) for caseId={}",
                cmd.getSigners().size(), cmd.getCaseId());

        return Flux.fromIterable(cmd.getSigners())
                .flatMap(signer -> submitSigner(signer, cmd.getPartyId()))
                .collectList()
                .map(SubmissionResult::new)
                .doOnSuccess(result ->
                        log.info("Submitted {} authorized signer(s) for caseId={}",
                                result.ids().size(), cmd.getCaseId()));
    }

    private Mono<UUID> submitSigner(SignerData signer, UUID partyId) {
        if (signer.getAttorneyId() == null) {
            return Mono.error(new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ATTORNEY_ID_REQUIRED",
                    "attorneyId is required: register the signer as a natural person before submitting the power of attorney"));
        }

        PowerOfAttorneyDTO dto = kybMapper.toPowerOfAttorneyDto(signer);
        dto.partyId(partyId);

        // Deterministic key: same partyId + same attorneyId must produce the
        // same key on every retry (handler retry policy is retries=2). Without
        // this, a transient failure followed by a successful retry would
        // create a duplicate POA row.
        String idempotencyKey = IdempotencyKeysImpl.of(
                "submit-authorized-signer", partyId.toString(),
                signer.getAttorneyId().toString());

        return powersOfAttorneyApi
                .addPowerOfAttorney(dto, idempotencyKey)
                .map(result -> Objects.requireNonNull(result.getPowerOfAttorneyId(),
                        "Core service returned null powerOfAttorneyId"));
    }
}
