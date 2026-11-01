package io.fireflysoftware.domain.kyc.kyb.web.reproofing;

import io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl.ReproofingBodyLimitFilterImpl;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.fireflyframework.web.idempotency.cache.IdempotencyCache;
import org.fireflyframework.web.idempotency.config.IdempotencyProperties;
import org.fireflyframework.web.idempotency.filter.IdempotencyWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReproofingIdempotencyTest {
    @RestController static class Endpoint { public void route(){} }
    @Test void actualGenericFrameworkCacheIsBypassedForRecoveryEvenWithCustomHeader() throws Exception {
        try(var context=new AnnotationConfigApplicationContext()){
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",Map.of("firefly.web.idempotency.header-name","X-Custom-Cache")));
            context.register(ReproofingBodyLimitFilterImpl.class);context.refresh();
            var product=context.getBean(ReproofingBodyLimitFilterImpl.class);
            var cache=mock(IdempotencyCache.class);
            when(cache.get(any())).thenReturn(Mono.error(new AssertionError("Recovery must not read generic cached authority")));
            var props=new IdempotencyProperties();props.setHeaderName("X-Custom-Cache");
            var framework=new IdempotencyWebFilter(props,cache);
            var calls=new AtomicInteger();
            for(String path:List.of("/api/v1/identity-recovery/cases","/internal/identity-recovery/evidence")){
                var exchange=MockServerWebExchange.from(MockServerHttpRequest.post(path).header("X-Custom-Cache","same-key"));
                exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,new HandlerMethod(new Endpoint(),Endpoint.class.getMethod("route")));
                product.filter(exchange,clean->{
                    assertThat(clean.getRequest().getHeaders().getFirst("X-Custom-Cache")).isNull();
                    return framework.filter(clean,downstream->{calls.incrementAndGet();return Mono.empty();});
                }).block();
            }
            assertThat(calls.get()).isEqualTo(2);verifyNoInteractions(cache);
        }
    }
}
