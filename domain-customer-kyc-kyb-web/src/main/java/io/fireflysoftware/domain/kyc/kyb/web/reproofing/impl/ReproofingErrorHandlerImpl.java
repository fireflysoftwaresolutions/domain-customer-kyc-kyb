package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice(assignableTypes={ReproofingControllerImpl.class,ReproofingEvidenceControllerImpl.class})
public final class ReproofingErrorHandlerImpl {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> refused(Exception error){
        HttpStatusCode status=HttpStatus.SERVICE_UNAVAILABLE;
        if(error instanceof ResponseStatusException typed) status=typed.getStatusCode();
        else if(error instanceof SecurityException) status=HttpStatus.FORBIDDEN;
        else if(error instanceof IllegalArgumentException) status=HttpStatus.BAD_REQUEST;
        else if(error instanceof WebClientResponseException remote && remote.getStatusCode().is4xxClientError()) status=HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).build();
    }
}
