package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.*;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods=false)
@EnableWebFluxSecurity
public class ReproofingSecurityConfigurationImpl {
    public static final String ROOT="/api/v1/identity-recovery/cases";
    @Bean @Order(-200)
    public SecurityWebFilterChain reproofingBoundary(ServerHttpSecurity http, Environment env) {
        var security=http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(ROOT,ROOT+"/**","/internal/identity-recovery/**"))
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .csrf(ServerHttpSecurity.CsrfSpec::disable).formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .exceptionHandling(e->e.authenticationEntryPoint((exchange,error)->{exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);return exchange.getResponse().setComplete();}));
        if(!env.getProperty("firefly.reproofing.enabled",Boolean.class,false)) return security.authorizeExchange(a->a.anyExchange().denyAll()).build();
        String nativeId=env.getRequiredProperty("firefly.reproofing.native-client"), coreId=env.getRequiredProperty("firefly.reproofing.verifier-client");
        if(nativeId.equals(coreId)) throw new IllegalStateException("Distinct reproofing workloads required");
        String nativeSecret=secret(env,"native-secret-file"), coreSecret=secret(env,"verifier-secret-file");
        ReactiveAuthenticationManager manager=auth->{
            String id=auth.getName(), supplied=String.valueOf(auth.getCredentials());
            String expected=id.equals(nativeId)?nativeSecret:id.equals(coreId)?coreSecret:null;
            if(expected==null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) return Mono.error(new BadCredentialsException("Recovery refused"));
            return Mono.just(UsernamePasswordAuthenticationToken.authenticated(id,null,List.of(new SimpleGrantedAuthority(id.equals(nativeId)?"NATIVE":"EVIDENCE"))));
        };
        return security.authenticationManager(manager).httpBasic(org.springframework.security.config.Customizer.withDefaults())
            .authorizeExchange(a->a.pathMatchers(HttpMethod.POST,"/internal/identity-recovery/evidence").hasAuthority("EVIDENCE")
                .pathMatchers(HttpMethod.POST,ROOT,ROOT+"/*/inspect",ROOT+"/*/confirm",ROOT+"/*/authorizations/consume",ROOT+"/*/authorizations/verify",ROOT+"/*/executions").hasAuthority("NATIVE")
                .anyExchange().denyAll())
            .addFilterAt((exchange,chain)->{
                if(!"https".equals(exchange.getRequest().getURI().getScheme())) {exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);return exchange.getResponse().setComplete();}
                return chain.filter(exchange);
            },org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST).build();
    }
    public static String secret(Environment env,String key) {
        try { String value=Files.readString(Path.of(env.getRequiredProperty("firefly.reproofing."+key))).strip();
            if(value.length()<32 || value.length()>512) throw new IllegalStateException("Explicit bounded mounted secret required"); return value;
        } catch(java.io.IOException e) {throw new IllegalStateException("Mounted secret unavailable");}
    }
}
