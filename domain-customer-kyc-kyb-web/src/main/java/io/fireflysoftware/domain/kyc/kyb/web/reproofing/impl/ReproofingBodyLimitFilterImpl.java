package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public final class ReproofingBodyLimitFilterImpl implements WebFilter {
 private final String idempotencyHeader;
 public ReproofingBodyLimitFilterImpl(org.springframework.core.env.Environment env){
  idempotencyHeader=env.getProperty("firefly.web.idempotency.header-name","X-Idempotency-Key");
 }
 @Override public Mono<Void> filter(ServerWebExchange exchange,WebFilterChain chain){
  String path=exchange.getRequest().getPath().pathWithinApplication().value();
  if(!path.equals("/api/v1/identity-recovery/cases") && !path.startsWith("/api/v1/identity-recovery/cases/") && !path.startsWith("/internal/identity-recovery/"))return chain.filter(exchange);
  var request=exchange.getRequest().mutate().headers(headers->{headers.remove(idempotencyHeader);headers.remove("X-Idempotency-Key");}).build();
  return bounded(exchange.mutate().request(request).build(),chain);
 }
 private Mono<Void> bounded(ServerWebExchange exchange,WebFilterChain chain){
  exchange.getResponse().getHeaders().setCacheControl("no-store");
  if(exchange.getRequest().getHeaders().getContentLength()>32768){exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);return exchange.getResponse().setComplete();}
  return DataBufferUtils.join(exchange.getRequest().getBody(),32768).map(buffer->{
   byte[] bytes=new byte[buffer.readableByteCount()];buffer.read(bytes);DataBufferUtils.release(buffer);return bytes;
  }).defaultIfEmpty(new byte[0]).flatMap(bytes->{
   var request=new ServerHttpRequestDecorator(exchange.getRequest()){
    @Override public Flux<DataBuffer> getBody(){return Flux.defer(()->Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));}
   };
   return chain.filter(exchange.mutate().request(request).build());
  }).onErrorResume(DataBufferLimitException.class,error->{exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);return exchange.getResponse().setComplete();});
 }
}
