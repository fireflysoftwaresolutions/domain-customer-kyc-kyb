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

import io.fireflysoftware.core.kycb.sdk.api.ComplianceCasesApi;
import io.fireflysoftware.core.kycb.sdk.api.CorporateDocumentsApi;
import io.fireflysoftware.core.kycb.sdk.api.KybVerificationApi;
import io.fireflysoftware.core.kycb.sdk.api.KycVerificationApi;
import io.fireflysoftware.core.kycb.sdk.api.KycVerificationDocumentsApi;
import io.fireflysoftware.core.kycb.sdk.api.PowersOfAttorneyApi;
import io.fireflysoftware.core.kycb.sdk.api.RiskAssessmentApi;
import io.fireflysoftware.core.kycb.sdk.api.UboManagementApi;
import io.fireflysoftware.core.kycb.sdk.api.VerificationDocumentsApi;
import io.fireflysoftware.core.kycb.sdk.invoker.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Creates reactive API client beans for the core-common-kycb-mgmt downstream service.
 * All beans share a single {@link ApiClient} configured from {@link KycbClientConfigurationProperties}.
 */
@Component("kycbClientFactory")
public class KycbClientFactoryImpl {

    private static final int MAX_IN_MEMORY_SIZE = 20 * 1024 * 1024;

    private final ApiClient apiClient;

    public KycbClientFactoryImpl(KycbClientConfigurationProperties props) {
        WebClient webClient = ApiClient.buildWebClientBuilder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
        this.apiClient = new ApiClient(webClient);
        this.apiClient.setBasePath(props.getBasePath());
    }

    /**
     * API for KYC verification lifecycle (open case, verify, fail, expire, renew).
     */
    @Bean
    public KycVerificationApi kycVerificationApi() {
        return new KycVerificationApi(apiClient);
    }

    /**
     * API for KYC identity document uploads and management.
     */
    @Bean
    public KycVerificationDocumentsApi kycVerificationDocumentsApi() {
        return new KycVerificationDocumentsApi(apiClient);
    }

    /**
     * API for KYB (Know Your Business) verification lifecycle.
     */
    @Bean
    public KybVerificationApi kybVerificationApi() {
        return new KybVerificationApi(apiClient);
    }

    /**
     * API for KYB corporate document uploads and management.
     */
    @Bean
    public CorporateDocumentsApi corporateDocumentsApi() {
        return new CorporateDocumentsApi(apiClient);
    }

    /**
     * API for compliance case management (shared by KYC and KYB flows).
     */
    @Bean
    public ComplianceCasesApi complianceCasesApi() {
        return new ComplianceCasesApi(apiClient);
    }

    /**
     * API for UBO (Ultimate Beneficial Owner) management in KYB flows.
     */
    @Bean
    public UboManagementApi uboManagementApi() {
        return new UboManagementApi(apiClient);
    }

    /**
     * API for Power-of-Attorney (authorized signer) management in KYB flows.
     */
    @Bean
    public PowersOfAttorneyApi powersOfAttorneyApi() {
        return new PowersOfAttorneyApi(apiClient);
    }

    /**
     * API for generic verification document management.
     */
    @Bean
    public VerificationDocumentsApi verificationDocumentsApi() {
        return new VerificationDocumentsApi(apiClient);
    }

    /**
     * API for party risk-assessment lifecycle (used by KYC verification flows).
     */
    @Bean
    public RiskAssessmentApi riskAssessmentApi() {
        return new RiskAssessmentApi(apiClient);
    }
}
