package io.fireflysoftware.domain.kyc.kyb.infra.reproofing;

import com.sun.net.httpserver.HttpServer;
import io.fireflysoftware.core.kycb.sdk.api.IdentityReproofingApi;
import io.fireflysoftware.core.kycb.sdk.invoker.ApiClient;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.impl.*;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import static org.assertj.core.api.Assertions.*;

class GeneratedReproofingSdkTest {
    @Test void actualGeneratedSdkPostsExactIndependentBindingAndPreservesProducerInstants() throws Exception {
        var mapper=ReproofingCodecsImpl.mapper();
        var binding=new ReproofingBinding("https://id.test/realms/customers","customers","tenant","subject",UUID.randomUUID(),"HUMAN","CREDENTIAL_REPLACEMENT","epoch","revision","nonce");
        var response=new ReproofingCaseResponse(UUID.randomUUID(),UUID.randomUUID(),binding,"PENDING","provider","policy",null,Instant.parse("2026-11-01T12:01:02Z"),null);
        var received=new AtomicReference<String>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/v1/identity-recovery/cases",exchange->{
            received.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            byte[] body=mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {
            var client=new ApiClient(WebClient.builder().exchangeStrategies(ReproofingCodecsImpl.strategies()).build());
            client.setBasePath("http://127.0.0.1:"+server.getAddress().getPort());
            var adapter=new GeneratedReproofingCoreAdapterImpl(new IdentityReproofingApi(client),mapper);
            assertThat(adapter.create(new CreateReproofingCaseRequest(binding)).block()).isEqualTo(response);
            assertThat(mapper.readTree(received.get()).get("binding").get("nonce").asText()).isEqualTo(binding.nonce());
            assertThat(received.get()).doesNotContain("approved","password","totp");
        } finally {server.stop(0);}
    }
}
