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


package io.fireflysoftware.domain.kyc.kyb.core.kyb.ports;

import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.BusinessVerificationRequest;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.BusinessVerificationResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.CorporateDocumentCheckRequest;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.CorporateDocumentCheckResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.UboVerificationRequest;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.UboVerificationResult;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.dto.VerificationStatusResult;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interface for third-party KYB verification providers.
 *
 * <p>Implementations connect to external KYB providers such as Onfido, Jumio, or Veriff
 * to perform business verification, corporate document checks, and UBO verification.
 * The sandbox adapter requires explicit selection and supplies only synthetic sample results.
 */
public interface KybProviderPort {

    /**
     * Verifies a business entity against the external provider.
     *
     * @param partyId the party identifier of the business
     * @param request the verification request with business details
     * @return verification result with score and risk level
     */
    Mono<BusinessVerificationResult> verifyBusiness(UUID partyId, BusinessVerificationRequest request);

    /**
     * Checks a corporate document against the external provider.
     *
     * @param documentId the document identifier
     * @param request the document check request
     * @return document check result with validity and confidence
     */
    Mono<CorporateDocumentCheckResult> checkCorporateDocument(UUID documentId, CorporateDocumentCheckRequest request);

    /**
     * Verifies an Ultimate Beneficial Owner against the external provider.
     *
     * @param uboId the UBO identifier
     * @param request the UBO verification request
     * @return UBO verification result with match score
     */
    Mono<UboVerificationResult> verifyUbo(UUID uboId, UboVerificationRequest request);

    /**
     * Retrieves the status of an ongoing verification from the external provider.
     *
     * @param externalReferenceId the external provider's reference identifier
     * @return the current verification status
     */
    Mono<VerificationStatusResult> getVerificationStatus(String externalReferenceId);
}
