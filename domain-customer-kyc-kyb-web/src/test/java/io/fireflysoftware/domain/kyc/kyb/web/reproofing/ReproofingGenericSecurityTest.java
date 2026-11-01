package io.fireflysoftware.domain.kyc.kyb.web.reproofing;

import io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl.ReproofingSecurityConfigurationImpl;
import org.fireflyframework.security.rs.*;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReproofingGenericSecurityTest {
    final ReactiveWebApplicationContextRunner runner=new ReactiveWebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ResourceServerAutoConfiguration.class))
        .withUserConfiguration(Web.class,ReproofingSecurityConfigurationImpl.class)
        .withBean(ReactiveJwtDecoder.class,()->token->Mono.error(new BadJwtException("Refused")));
    @Configuration(proxyBeanMethods=false) @EnableWebFlux static class Web { }
    @RestController static class UnrelatedController {
        @GetMapping("/unrelated") String get(){return "unrelated";}
    }
    @Configuration(proxyBeanMethods=false) static class ExistingPolicy {
        @Bean UnrelatedController unrelated(){return new UnrelatedController();}
        @Bean SecurityWebFilterChain fireflySecurityWebFilterChain(ServerHttpSecurity http,ReactiveJwtDecoder decoder){
            return new ResourceServerAutoConfiguration().fireflySecurityWebFilterChain(http,decoder,
                jwt->Mono.error(new BadJwtException("Refused")),mock(PolicyDecisionPort.class),new ResourceServerProperties());
        }
    }
    @Test void suppressedOptionalGenericPolicyRefusesStartup(){
        runner.run(context->{assertThat(context).hasFailed();assertThat(context.getStartupFailure()).hasRootCauseMessage("Independent existing application security chain required");});
    }
    @Test void explicitlyInstalledOriginalGenericPolicyStillDeniesMissingAndWrongBearer(){
        runner.withUserConfiguration(ExistingPolicy.class).run(context->{
            assertThat(context).hasNotFailed();
            var client=WebTestClient.bindToApplicationContext(context).build();
            client.get().uri("/unrelated").exchange().expectStatus().is4xxClientError();
            client.get().uri("/unrelated").headers(h->h.setBearerAuth("wrong-token")).exchange().expectStatus().is4xxClientError();
        });
    }
}
