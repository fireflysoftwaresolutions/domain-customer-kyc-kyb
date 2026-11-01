package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.core.reproofing.impl.*;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.impl.*;
import io.fireflysoftware.core.kycb.sdk.api.IdentityReproofingApi;
import io.fireflysoftware.core.kycb.sdk.invoker.ApiClient;
import java.net.URI;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="firefly.reproofing.enabled",havingValue="true")
public class ReproofingConfigurationImpl {
    @Bean public ReproofingPolicyImpl reproofingPolicy(Environment env) {
        return new ReproofingPolicyImpl(required(env,"issuer"),required(env,"realm"),required(env,"tenant"),
            required(env,"provider-id"),required(env,"policy-id"),required(env,"verification-origin"),Clock.systemUTC());
    }
    @Bean public ReproofingCorePort reproofingCore(Environment env) {
        String origin=required(env,"core-origin");
        ApiClient client=new ApiClient(client(env,origin,"core-client","core-secret-file"));
        client.setBasePath(origin);
        return new GeneratedReproofingCoreAdapterImpl(new IdentityReproofingApi(client),ReproofingCodecsImpl.mapper());
    }
    @Bean public ReproofingNativePort reproofingNative(Environment env) {
        String base=required(env,"native-base-url");
        if(!base.equals(required(env,"issuer")+"/firefly-recovery/lost-credentials/rpc")) throw new IllegalStateException("Exact native realm RPC required");
        URI uri=URI.create(base);
        WebClient web=client(env,uri.getScheme()+"://"+uri.getRawAuthority(),"native-rpc-client","native-rpc-secret-file");
        return new NativeReproofingHttpAdapterImpl(web,base);
    }
    @Bean public ReproofingServiceImpl reproofingService(ReproofingCorePort core, ReproofingNativePort nativePort,
            ObjectProvider<ReproofingProviderPort> providers, ReproofingPolicyImpl policy) {
        var list=providers.orderedStream().toList();
        if(list.size()>1) throw new IllegalStateException("Exactly one approved proofing adapter required");
        return new ReproofingServiceImpl(core,nativePort,list.isEmpty()?null:list.getFirst(),policy);
    }
    private static String required(Environment e,String key){return e.getRequiredProperty("firefly.reproofing."+key);}
    private static WebClient client(Environment env,String origin,String clientKey,String secretKey) {
        ReproofingPolicyImpl.https(origin);
        URI uri=URI.create(origin);
        if(uri.getRawQuery()!=null || (uri.getPath()!=null && !uri.getPath().isEmpty())) throw new IllegalStateException("Exact HTTPS service origin required");
        String user=required(env,clientKey), secret=ReproofingSecurityConfigurationImpl.secret(env,secretKey);
        return WebClient.builder().baseUrl(origin).clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .followRedirect(false).responseTimeout(Duration.ofSeconds(8))))
            .exchangeStrategies(ReproofingCodecsImpl.strategies())
            .filter((request,next)->{
                URI target=request.url();
                if(!uri.getScheme().equals(target.getScheme()) || !uri.getRawAuthority().equals(target.getRawAuthority())) return Mono.error(ReproofingPolicyImpl.denied());
                return next.exchange(org.springframework.web.reactive.function.client.ClientRequest.from(request).headers(h->h.setBasicAuth(user,secret)).build());
            }).build();
    }
}
