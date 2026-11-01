package io.fireflysoftware.domain.kyc.kyb.web.reproofing;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

class ReproofingDisabledBoundaryTest {
    @Configuration(proxyBeanMethods=false)
    @EnableWebFlux
    @ComponentScan("io.fireflysoftware.domain.kyc.kyb.web.reproofing")
    static class Boundary { }

    @Test
    void recoveryAndEvidencePathsRefuseRequestsEvenWithFeatureDisabled() {
        try (var context = new AnnotationConfigApplicationContext(Boundary.class)) {
            var client = WebTestClient.bindToApplicationContext(context).build();
            for (String path : new String[]{"/api/v1/identity-recovery/cases", "/internal/identity-recovery/evidence"}) {
                client.post().uri(path).exchange().expectStatus().isForbidden();
            }
        }
    }
}
