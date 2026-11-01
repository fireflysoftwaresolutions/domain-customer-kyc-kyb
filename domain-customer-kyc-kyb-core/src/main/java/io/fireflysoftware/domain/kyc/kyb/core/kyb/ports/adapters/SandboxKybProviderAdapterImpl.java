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


package io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.adapters;

import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.KybProviderPort;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.BusinessVerificationRequest;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.BusinessVerificationResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.CorporateDocumentCheckRequest;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.CorporateDocumentCheckResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.UboVerificationRequest;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.UboVerificationResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.VerificationStatusResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Deterministic sample KYB provider available only through explicit sandbox selection.
 */
@Slf4j
@Component("sandboxKybProviderAdapter")
@ConditionalOnProperty(name = "kyb.provider", havingValue = "sandbox")
public class SandboxKybProviderAdapterImpl implements KybProviderPort {

    static final String PROVIDER_NAME = "SANDBOX";
    static final int BUSINESS_SCORE = 85;
    static final int DOCUMENT_CONFIDENCE = 92;
    static final int UBO_MATCH_SCORE = 90;
    static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Override
    public Mono<BusinessVerificationResult> verifyBusiness(UUID partyId, BusinessVerificationRequest request) {
        log.debug("Sandbox KYB provider verifying business partyId={}", partyId);
        return Mono.just(BusinessVerificationResult.builder()
                .verified(true)
                .score(BUSINESS_SCORE)
                .riskLevel("LOW")
                .details("Sandbox verification passed for business")
                .provider(PROVIDER_NAME)
                .build());
    }

    @Override
    public Mono<CorporateDocumentCheckResult> checkCorporateDocument(UUID documentId, CorporateDocumentCheckRequest request) {
        log.debug("Sandbox KYB provider checking corporate document documentId={}", documentId);
        return Mono.just(CorporateDocumentCheckResult.builder()
                .valid(true)
                .documentType(request.getDocumentType())
                .confidence(DOCUMENT_CONFIDENCE)
                .build());
    }

    @Override
    public Mono<UboVerificationResult> verifyUbo(UUID uboId, UboVerificationRequest request) {
        log.debug("Sandbox KYB provider verifying UBO uboId={}", uboId);
        return Mono.just(UboVerificationResult.builder()
                .verified(true)
                .matchScore(UBO_MATCH_SCORE)
                .build());
    }

    @Override
    public Mono<VerificationStatusResult> getVerificationStatus(String externalReferenceId) {
        log.debug("Sandbox KYB provider checking verification status externalReferenceId={}", externalReferenceId);
        return Mono.just(VerificationStatusResult.builder()
                .status("COMPLETED")
                .externalId(externalReferenceId)
                .completedAt(COMPLETED_AT)
                .build());
    }
}
