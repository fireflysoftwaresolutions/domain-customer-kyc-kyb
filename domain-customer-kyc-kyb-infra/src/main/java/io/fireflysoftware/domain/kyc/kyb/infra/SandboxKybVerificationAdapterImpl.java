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


package io.fireflysoftware.domain.kyc.kyb.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Deterministic sample KYB verification available only through explicit sandbox selection.
 * Every case returns {@code VERIFIED}.
 */
@Slf4j
@Component("sandboxKybVerificationAdapter")
@ConditionalOnProperty(
        name = "integration.kyb-verification.provider",
        havingValue = "sandbox"
)
public class SandboxKybVerificationAdapterImpl implements KybVerificationPort {

    @Override
    public Mono<KybVerificationResult> verify(UUID caseId) {
        log.info("Sandbox KYB verification for caseId={}", caseId);
        return Mono.just(KybVerificationResult.verified());
    }
}
