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

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port for external KYB (Know Your Business) verification providers.
 * Implementations may delegate to an external provider.
 *
 * <p>{@link SandboxKybVerificationAdapterImpl} requires explicit sandbox selection and is not a live verification provider.</p>
 */
public interface KybVerificationPort {

    /**
     * Submits a KYB verification request for the given case identifier.
     *
     * @param caseId the KYB compliance case identifier
     * @return a {@link Mono} emitting the verification result
     */
    Mono<KybVerificationResult> verify(UUID caseId);
}
