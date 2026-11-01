package io.fireflysoftware.domain.kyc.kyb.core.reproofing;

import io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.*;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReproofingOrchestrationTest {
    final Instant now=Instant.parse("2026-10-31T12:00:00Z");
    final ReproofingBinding b=new ReproofingBinding("https://id.test/realms/customer","customer","tenant","subject",UUID.randomUUID(),"HUMAN","CREDENTIAL_REPLACEMENT","epoch","revision","nonce");
    final ReproofingCaseResponse c=new ReproofingCaseResponse(UUID.randomUUID(),UUID.randomUUID(),b,"PENDING","provider","policy","session",now.plusSeconds(600),null);
    final ReproofingPolicyImpl policy=new ReproofingPolicyImpl(b.issuer(),b.realm(),b.tenant(),"provider","policy","https://proof.test",Clock.fixed(now,ZoneOffset.UTC));
    final ReproofingCorePort core=mock(ReproofingCorePort.class);
    final ReproofingNativePort nativePort=mock(ReproofingNativePort.class);
    final ReproofingProviderPort provider=mock(ReproofingProviderPort.class);
    ReproofingServiceImpl service;
    @BeforeEach void setup(){ service=new ReproofingServiceImpl(core,nativePort,provider,policy); }
    @Test void missingProviderIsUnavailableWithoutOpeningCase(){
        StepVerifier.create(new ReproofingServiceImpl(core,nativePort,null,policy).create(new CreateReproofingCaseRequest(b))).expectErrorMatches(e->e.getMessage().contains("503")).verify();
        verifyNoInteractions(core,nativePort);
    }
    @Test void beginUsesExactDurableReservationAndPreservesSessionAndExpiryOnRetries(){
        when(nativePort.prepare(b,null)).thenReturn(Mono.just(new ReproofingAdmission(b,true,c.expiresAt())));
        when(core.create(new CreateReproofingCaseRequest(b))).thenReturn(Mono.just(c));
        when(provider.providerId()).thenReturn("provider");
        when(provider.begin(c)).thenReturn(Mono.just(new ProviderStart("session","https://proof.test/journey")));
        when(core.bind(c.caseId(),new BindReproofingSessionRequest(b,"session"))).thenReturn(Mono.just(c));
        for(int i=0;i<2;i++) StepVerifier.create(service.create(new CreateReproofingCaseRequest(b))).assertNext(j->{assertThat(j.reproofingCase()).isEqualTo(c);assertThat(j.verificationUrl()).isEqualTo("https://proof.test/journey");}).verifyComplete();
        verify(provider,times(2)).begin(c);
    }
    @Test void ambiguousBeginNeverRetriesWithinOperationOrUsesFreshCase(){
        when(nativePort.prepare(b,null)).thenReturn(Mono.just(new ReproofingAdmission(b,true,c.expiresAt())));
        when(core.create(new CreateReproofingCaseRequest(b))).thenReturn(Mono.just(c));
        when(provider.providerId()).thenReturn("provider"); when(provider.begin(c)).thenReturn(Mono.error(new IllegalStateException("ambiguous")));
        StepVerifier.create(service.create(new CreateReproofingCaseRequest(b))).expectError(IllegalStateException.class).verify();
        verify(provider).begin(c); verify(core,never()).bind(any(),any());
    }
    VerifiedReproofingEvidence evidence(UUID reference,Instant expiry,boolean authentic){return new VerifiedReproofingEvidence(c.caseId(),b,"provider","policy","session","evidence","digest",reference,now,expiry,authentic,true,true,true);}
    @Test void actualStoredEvidenceIsVerifiedAndBoundToExistingReference(){
        var r=new ReproofingEvidenceRequest(c.caseId(),b,"provider","policy","session");
        when(provider.providerId()).thenReturn("provider");when(core.inspect(c.caseId(),new ConfirmReproofingRequest(b))).thenReturn(Mono.just(c));
        var verified=evidence(b.partyId(),c.expiresAt(),true);when(provider.verify(r)).thenReturn(Mono.just(verified));
        StepVerifier.create(service.evidence(r)).expectNext(verified).verifyComplete();
    }
    @ParameterizedTest @ValueSource(strings={"reference","expired","forged","future"})
    void invalidProviderEvidenceNeverAuthorizes(String kind){
        var r=new ReproofingEvidenceRequest(c.caseId(),b,"provider","policy","session");
        when(provider.providerId()).thenReturn("provider");when(core.inspect(c.caseId(),new ConfirmReproofingRequest(b))).thenReturn(Mono.just(c));
        var e=evidence(kind.equals("reference")?UUID.randomUUID():b.partyId(),kind.equals("expired")?now:kind.equals("future")?now.plusSeconds(601):c.expiresAt(),!kind.equals("forged"));
        when(provider.verify(r)).thenReturn(Mono.just(e));
        StepVerifier.create(service.evidence(r)).expectErrorMatches(error->error.getMessage().contains("403")).verify();
    }
    @Test void substitutedSessionDoesNotQueryProvider(){
        var r=new ReproofingEvidenceRequest(c.caseId(),b,"provider","policy","other");
        when(provider.providerId()).thenReturn("provider");when(core.inspect(c.caseId(),new ConfirmReproofingRequest(b))).thenReturn(Mono.just(c));
        StepVerifier.create(service.evidence(r)).expectErrorMatches(error->error.getMessage().contains("403")).verify();
        verify(provider,never()).verify(any());
    }
    @Test void currentNativeHoldRefusesBeforeCoreConfirmation(){
        when(nativePort.admit(b,c.caseId())).thenReturn(Mono.just(new ReproofingAdmission(b,false,c.expiresAt())));
        StepVerifier.create(service.confirm(c.caseId(),new ConfirmReproofingRequest(b))).expectErrorMatches(error->error.getMessage().contains("403")).verify();verifyNoInteractions(core);
    }
    @ParameterizedTest @ValueSource(strings={"https://proof.test.evil/journey","https://proof.test:444/journey","http://proof.test/journey","https://user@proof.test/journey"})
    void providerJourneyRequiresExactConfiguredHttpsOrigin(String url){assertThatThrownBy(()->policy.journeyUrl(url)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
    @Test void customerAuthorityRefusesStaff(){var staff=new ReproofingBinding(b.issuer(),b.realm(),b.tenant(),b.subject(),b.partyId(),"STAFF",b.purpose(),b.generation(),b.credentialRevision(),b.nonce()); assertThatThrownBy(()->policy.binding(staff)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
}
