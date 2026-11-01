package io.fireflysoftware.domain.kyc.kyb.web.reproofing;

import io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl.*;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.impl.ReproofingCodecsImpl;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Explicit hosted test process only. This class and its signed provider are absent from all production JARs. */
@SpringBootConfiguration
@Profile("reproofing-fixture")
@EnableAutoConfiguration
@Import({ReproofingSecurityConfigurationImpl.class,ReproofingExistingSecurityGuardConfigurationImpl.class,ReproofingConfigurationImpl.class,ReproofingControllerImpl.class,
    ReproofingEvidenceControllerImpl.class,ReproofingBodyLimitFilterImpl.class,ReproofingErrorHandlerImpl.class,ReproofingFixtureApplication.FixtureController.class})
public class ReproofingFixtureApplication {
    public static void main(String[] args){var app=new SpringApplication(ReproofingFixtureApplication.class);app.setAdditionalProfiles("reproofing-fixture");app.run(args);}
    @Bean FixtureProviderImpl fixtureProvider(Environment env){return new FixtureProviderImpl(env);}
    @Bean @Order(-201) SecurityWebFilterChain fixtureBoundary(ServerHttpSecurity http,Environment env){
        String id=env.getRequiredProperty("firefly.reproofing.fixture-client");
        String secret=ReproofingSecurityConfigurationImpl.secret(env,"fixture-secret-file");
        ReactiveAuthenticationManager manager=auth->id.equals(auth.getName()) && MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),String.valueOf(auth.getCredentials()).getBytes(StandardCharsets.UTF_8))
            ?Mono.just(UsernamePasswordAuthenticationToken.authenticated(id,null,List.of(new SimpleGrantedAuthority("FIXTURE")))):Mono.error(new BadCredentialsException("Fixture refused"));
        return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/fixture/**")).securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .csrf(ServerHttpSecurity.CsrfSpec::disable).formLogin(ServerHttpSecurity.FormLoginSpec::disable).authenticationManager(manager)
            .httpBasic(org.springframework.security.config.Customizer.withDefaults()).authorizeExchange(a->a.pathMatchers(HttpMethod.GET,"/fixture/proofing/*").permitAll()
                .pathMatchers(HttpMethod.POST,"/fixture/reproofing/sessions/*").hasAuthority("FIXTURE").anyExchange().denyAll()).build();
    }
    static final class FixtureProviderImpl implements ReproofingProviderPort {
        private final Path directory;
        private final String origin;
        private final byte[] key;
        private final com.fasterxml.jackson.databind.ObjectMapper mapper=ReproofingCodecsImpl.mapper();
        record Stored(ReproofingCaseResponse reproofingCase,String mode,String signature){}
        FixtureProviderImpl(Environment env){
            directory=Path.of(env.getRequiredProperty("firefly.reproofing.fixture-state"));
            origin=env.getRequiredProperty("firefly.reproofing.verification-origin");
            key=ReproofingSecurityConfigurationImpl.secret(env,"fixture-secret-file").getBytes(StandardCharsets.UTF_8);
            try{Files.createDirectories(directory);}catch(Exception error){throw new IllegalStateException("Fixture storage unavailable");}
        }
        public String providerId(){return "fixture-proofing-v1";}
        public Mono<ProviderStart> begin(ReproofingCaseResponse c){return Mono.fromCallable(()->start(c));}
        synchronized ProviderStart start(ReproofingCaseResponse c)throws Exception{
            String session="fixture-"+c.providerStartId();
            Path path=file(session);
            if(Files.exists(path)){
                Stored stored=read(session);
                if(!stored.reproofingCase().caseId().equals(c.caseId()) || !stored.reproofingCase().binding().equals(c.binding())) throw new SecurityException("Fixture binding refused");
            } else write(session,c,"PENDING");
            return new ProviderStart(session,origin+"/fixture/proofing/"+session);
        }
        synchronized void control(String session,String mode)throws Exception{
            if(!Set.of("APPROVED","FORGED","EXPIRED","WRONG_REFERENCE","REPLAYED","UNAVAILABLE").contains(mode)) throw new IllegalArgumentException("Fixture mode refused");
            Stored stored=read(session);write(session,stored.reproofingCase(),mode);
        }
        public Mono<VerifiedReproofingEvidence> verify(ReproofingEvidenceRequest request){return Mono.fromCallable(()->verified(request));}
        synchronized VerifiedReproofingEvidence verified(ReproofingEvidenceRequest r)throws Exception{
            Stored stored=read(r.providerSessionId());ReproofingCaseResponse c=stored.reproofingCase();
            if(!c.caseId().equals(r.caseId()) || !c.binding().equals(r.binding()) || !c.providerId().equals(r.providerId()) || !c.policyId().equals(r.policyId())) throw new SecurityException("Fixture binding refused");
            if(stored.mode().equals("PENDING") || stored.mode().equals("UNAVAILABLE")) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Proofing unavailable");
            if(stored.mode().equals("FORGED")) throw new SecurityException("Fixture provider signature refused");
            Instant now=Instant.now();
            return new VerifiedReproofingEvidence(stored.mode().equals("REPLAYED")?UUID.randomUUID():c.caseId(),c.binding(),c.providerId(),c.policyId(),r.providerSessionId(),
                "evidence-"+c.providerStartId(),sign(c,stored.mode()),stored.mode().equals("WRONG_REFERENCE")?UUID.randomUUID():c.binding().partyId(),now,
                stored.mode().equals("EXPIRED")?now.minusSeconds(1):c.expiresAt(),true,true,true,true);
        }
        private Path file(String session){if(session==null || !session.matches("fixture-[0-9a-f-]{36}")) throw new SecurityException("Fixture session refused");return directory.resolve(session+".json");}
        private Stored read(String session)throws Exception{
            Stored stored=mapper.readValue(Files.readAllBytes(file(session)),Stored.class);
            if(!MessageDigest.isEqual(sign(stored.reproofingCase(),stored.mode()).getBytes(StandardCharsets.UTF_8),stored.signature().getBytes(StandardCharsets.UTF_8))) throw new SecurityException("Fixture signature refused");
            return stored;
        }
        private void write(String session,ReproofingCaseResponse c,String mode)throws Exception{
            Path temp=Files.createTempFile(directory,"pending-",".json");
            Files.write(temp,mapper.writeValueAsBytes(new Stored(c,mode,sign(c,mode))));
            Files.move(temp,file(session),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        }
        private String sign(ReproofingCaseResponse c,String mode)throws Exception{
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(mapper.writeValueAsBytes(List.of(c.caseId(),c.providerStartId(),c.binding(),c.providerId(),c.policyId(),c.expiresAt(),mode))));
        }
    }
    @Profile("reproofing-fixture")
    @RestController static class FixtureController {
        private final FixtureProviderImpl provider;
        FixtureController(FixtureProviderImpl provider){this.provider=provider;}
        record Control(String mode){}
        @PostMapping("/fixture/reproofing/sessions/{session}") @ResponseStatus(HttpStatus.NO_CONTENT)
        void control(@PathVariable String session,@RequestBody Control control)throws Exception{provider.control(session,control.mode());}
        @GetMapping(value="/fixture/proofing/{session}",produces=MediaType.TEXT_HTML_VALUE)
        String journey(@PathVariable String session){provider.file(session);return "<!doctype html><html lang='en'><meta name='viewport' content='width=device-width'><title>Synthetic identity verification</title><main><h1>Identity verification</h1><p>This hosted test provider requires independently controlled synthetic evidence.</p><p>Return to the recovery page after verification is complete.</p></main></html>";}
    }
}
