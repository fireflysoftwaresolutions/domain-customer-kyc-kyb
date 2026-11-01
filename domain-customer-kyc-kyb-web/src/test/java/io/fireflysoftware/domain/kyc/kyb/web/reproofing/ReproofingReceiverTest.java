package io.fireflysoftware.domain.kyc.kyb.web.reproofing;

import io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl.*;
import io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.ReproofingServiceImpl;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.*;

class ReproofingReceiverTest {
    @TempDir Path directory;
    @Configuration(proxyBeanMethods=false) @EnableWebFlux static class Web { }
    @Test void exactWorkloadAndActionAndTlsAreRequired() throws Exception {
        Path nativeSecret=directory.resolve("native"), coreSecret=directory.resolve("core");
        Files.writeString(nativeSecret,"native-secret-012345678901234567890123456789");
        Files.writeString(coreSecret,"core-secret-012345678901234567890123456789");
        String nativeValue=Files.readString(nativeSecret), coreValue=Files.readString(coreSecret);
        var service=mock(ReproofingServiceImpl.class);
        when(service.create(any())).thenReturn(Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)));
        try(var context=new AnnotationConfigApplicationContext()){
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",Map.of(
                "firefly.security.resource-server.enabled","false","firefly.reproofing.enabled","true","firefly.reproofing.native-client","native","firefly.reproofing.verifier-client","core",
                "firefly.reproofing.native-secret-file",nativeSecret.toString(),"firefly.reproofing.verifier-secret-file",coreSecret.toString())));
            context.register(Web.class,ReproofingSecurityConfigurationImpl.class,ReproofingControllerImpl.class,ReproofingEvidenceControllerImpl.class);
            context.registerBean(ReproofingServiceImpl.class,()->service);context.refresh();
            var client=WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("https://domain.test").build();
            client.post().uri("/api/v1/identity-recovery/cases").exchange().expectStatus().is4xxClientError();
            client.post().uri("/api/v1/identity-recovery/cases").headers(h->h.setBasicAuth("unknown",nativeValue)).exchange().expectStatus().is4xxClientError();
            client.post().uri("/internal/identity-recovery/evidence").headers(h->h.setBasicAuth("native",nativeValue)).bodyValue(Map.of()).exchange().expectStatus().isForbidden();
            client.post().uri("/api/v1/identity-recovery/cases").headers(h->h.setBasicAuth("core",coreValue)).bodyValue(Map.of()).exchange().expectStatus().isForbidden();
            client.post().uri("http://domain.test/api/v1/identity-recovery/cases").headers(h->h.setBasicAuth("native",nativeValue)).bodyValue(Map.of()).exchange().expectStatus().isForbidden();
            client.post().uri("/api/v1/identity-recovery/cases/id/provider-session").headers(h->h.setBasicAuth("native",nativeValue)).bodyValue(Map.of()).exchange().expectStatus().isForbidden();
            client.post().uri("/api/v1/identity-recovery/cases").headers(h->h.setBasicAuth("native",nativeValue)).bodyValue(Map.of("binding",Map.of())).exchange().expectStatus().isEqualTo(503);
            verify(service,times(1)).create(any());verifyNoMoreInteractions(service);
        }
    }
}
