package io.fireflysoftware.domain.kyc.kyb.infra.reproofing.impl;

import io.fireflysoftware.domain.kyc.kyb.infra.reproofing.*;
import java.time.Duration;
import java.util.UUID;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public final class NativeReproofingHttpAdapterImpl implements ReproofingNativePort {
    private final WebClient web;
    private final String baseUrl;
    public NativeReproofingHttpAdapterImpl(WebClient web,String baseUrl){this.web=web;this.baseUrl=baseUrl;}
    public Mono<ReproofingAdmission> prepare(ReproofingBinding b,UUID id){return call("prepare",b,id);}
    public Mono<ReproofingAdmission> admit(ReproofingBinding b,UUID id){return call("admit",b,id);}
    private Mono<ReproofingAdmission> call(String action,ReproofingBinding b,UUID id){
        return web.post().uri(baseUrl+"/"+action).bodyValue(new ReproofingNativeRequest(b,id,null))
            .retrieve().bodyToMono(ReproofingAdmission.class).timeout(Duration.ofSeconds(5));
    }
}
