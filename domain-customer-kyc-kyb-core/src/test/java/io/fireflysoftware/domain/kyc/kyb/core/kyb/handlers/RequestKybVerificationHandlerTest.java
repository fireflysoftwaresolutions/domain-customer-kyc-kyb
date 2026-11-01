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

import io.fireflysoftware.core.kycb.sdk.api.KybVerificationApi;
import io.fireflysoftware.core.kycb.sdk.model.KybVerificationDTO;
import io.fireflysoftware.domain.kyc.kyb.core.kyb.commands.RequestKybVerificationCommand;
import io.fireflysoftware.domain.kyc.kyb.infra.KybVerificationPort;
import io.fireflysoftware.domain.kyc.kyb.infra.KybVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestKybVerificationHandlerTest {

    @Mock
    private KybVerificationPort verificationPort;

    @Mock
    private KybVerificationApi kybVerificationApi;

    private RequestKybVerificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RequestKybVerificationHandler(verificationPort, kybVerificationApi);
    }

    @Test
    void shouldCallPortThenPersistAndReturnVerificationDto_whenVerified() {
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID verificationId = UUID.randomUUID();

        when(verificationPort.verify(caseId))
                .thenReturn(Mono.just(KybVerificationResult.verified()));

        KybVerificationDTO persistedDto = new KybVerificationDTO(null, null, verificationId);
        persistedDto.verificationStatus("VERIFIED");
        when(kybVerificationApi.createKybVerification(eq(partyId), any(KybVerificationDTO.class), any()))
                .thenReturn(Mono.just(persistedDto));

        RequestKybVerificationCommand cmd = RequestKybVerificationCommand.builder()
                .caseId(caseId)
                .partyId(partyId)
                .build();

        StepVerifier.create(handler.doHandle(cmd))
                .assertNext(dto -> {
                    assertThat(dto.getKybVerificationId()).isEqualTo(verificationId);
                    assertThat(dto.getVerificationStatus()).isEqualTo("VERIFIED");
                })
                .verifyComplete();

        verify(verificationPort).verify(caseId);
        verify(kybVerificationApi).createKybVerification(eq(partyId), any(KybVerificationDTO.class), any());
    }

    @Test
    void shouldPersistRejectedStatus_whenVerificationFails() {
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID verificationId = UUID.randomUUID();

        when(verificationPort.verify(caseId))
                .thenReturn(Mono.just(KybVerificationResult.rejected("Opaque ownership structure")));

        KybVerificationDTO persistedDto = new KybVerificationDTO(null, null, verificationId);
        persistedDto.verificationStatus("REJECTED");
        when(kybVerificationApi.createKybVerification(eq(partyId), any(KybVerificationDTO.class), any()))
                .thenReturn(Mono.just(persistedDto));

        RequestKybVerificationCommand cmd = RequestKybVerificationCommand.builder()
                .caseId(caseId)
                .partyId(partyId)
                .build();

        StepVerifier.create(handler.doHandle(cmd))
                .assertNext(dto -> assertThat(dto.getVerificationStatus()).isEqualTo("REJECTED"))
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenPortFails() {
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        RuntimeException providerFailure = new RuntimeException("External provider timeout");
        when(verificationPort.verify(caseId))
                .thenReturn(Mono.error(providerFailure));

        RequestKybVerificationCommand cmd = RequestKybVerificationCommand.builder()
                .caseId(caseId)
                .partyId(partyId)
                .build();

        StepVerifier.create(handler.doHandle(cmd))
                .expectErrorMatches(error -> error == providerFailure)
                .verify();

        verifyNoInteractions(kybVerificationApi);
    }
}
