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

import io.fireflysoftware.core.kycb.sdk.api.UboManagementApi;
import io.fireflysoftware.core.kycb.sdk.model.UboDTO;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.RegisterUbosCommand;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.SubmissionResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.UboData;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.mappers.KybMapper;
import io.fireflysoftware.domain.kyc.kyb.core.util.IdempotencyKeysImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Registers each Ultimate Beneficial Owner via {@code UboManagementApi}.
 * UBOs are processed concurrently (Flux.flatMap with unbounded concurrency).
 * Returns a {@link SubmissionResult} containing all created UBO IDs.
 */
@Slf4j
@RequiredArgsConstructor
@CommandHandlerComponent(timeout = 60000, retries = 2, metrics = true, tracing = true)
public class RegisterUbosHandler extends CommandHandler<RegisterUbosCommand, SubmissionResult> {

    private final UboManagementApi uboManagementApi;
    private final KybMapper kybMapper;

    @Override
    protected Mono<SubmissionResult> doHandle(RegisterUbosCommand cmd) {
        log.info("Registering {} UBO(s) for caseId={}", cmd.getUbos().size(), cmd.getCaseId());

        return Flux.fromIterable(cmd.getUbos())
                .flatMap(ubo -> registerUbo(ubo, cmd.getPartyId()))
                .collectList()
                .map(SubmissionResult::new)
                .doOnSuccess(result ->
                        log.info("Registered {} UBO(s) for caseId={}", result.ids().size(), cmd.getCaseId()));
    }

    private Mono<UUID> registerUbo(UboData ubo, UUID partyId) {
        UboDTO dto = kybMapper.toDto(ubo);
        dto.partyId(partyId);

        // Deterministic key: same partyId + same naturalPersonId must produce
        // the same key on every retry (handler retry policy is retries=2).
        // Without this, a transient failure followed by a successful retry
        // would create a duplicate UBO row in core.
        String idempotencyKey = IdempotencyKeysImpl.of(
                "register-ubo", partyId.toString(),
                ubo.getNaturalPersonId() != null ? ubo.getNaturalPersonId().toString() : "null");

        return uboManagementApi
                .addUbo(partyId, dto, idempotencyKey)
                .map(result -> Objects.requireNonNull(result.getUboId(),
                        "Core service returned null uboId"));
    }
}
