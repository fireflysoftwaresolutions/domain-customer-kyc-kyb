package io.fireflysoftware.domain.kyc.kyb.web.reproofing;

import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class ReproofingFixtureProviderTest {
    @TempDir Path directory;
    @Test void providerReconcilesOriginalKeyAcrossRestartAndAuthenticatesStoredResults() throws Exception {
        Path secret=directory.resolve("secret"); Files.writeString(secret,"fixture-secret-012345678901234567890123456789");
        var env=new MockEnvironment().withProperty("firefly.reproofing.fixture-state",directory.resolve("provider").toString())
            .withProperty("firefly.reproofing.verification-origin","https://domain.test").withProperty("firefly.reproofing.fixture-secret-file",secret.toString());
        var provider=new ReproofingFixtureApplication.FixtureProviderImpl(env);
        var b=new ReproofingBinding("https://id.test/realms/customer","customer","tenant","subject",UUID.randomUUID(),"HUMAN","CREDENTIAL_REPLACEMENT","epoch","revision","nonce");
        var c=new ReproofingCaseResponse(UUID.randomUUID(),UUID.randomUUID(),b,"PENDING",provider.providerId(),"fixture-policy-v1",null,Instant.now().plusSeconds(600),null);
        var first=provider.start(c);
        var restarted=new ReproofingFixtureApplication.FixtureProviderImpl(env);
        assertThat(restarted.start(c)).isEqualTo(first);
        restarted.control(first.providerSessionId(),"APPROVED");
        var request=new ReproofingEvidenceRequest(c.caseId(),b,c.providerId(),c.policyId(),first.providerSessionId());
        assertThat(restarted.verified(request).referenceId()).isEqualTo(b.partyId());
        assertThatThrownBy(()->restarted.verified(new ReproofingEvidenceRequest(UUID.randomUUID(),b,c.providerId(),c.policyId(),first.providerSessionId()))).isInstanceOf(SecurityException.class);
        Path stored=directory.resolve("provider").resolve(first.providerSessionId()+".json");
        Files.writeString(stored,Files.readString(stored).replace("APPROVED","FORGED"));
        assertThatThrownBy(()->restarted.verified(request)).isInstanceOf(SecurityException.class);
    }
}
