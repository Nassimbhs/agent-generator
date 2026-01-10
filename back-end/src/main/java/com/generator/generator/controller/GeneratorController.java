package com.generator.generator.controller;

import com.generator.generator.service.IStreamingCodeGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Generator", description = "Direct code generation APIs")
@SecurityRequirement(name = "bearerAuth")
public class GeneratorController {

    private final IStreamingCodeGenerationService streamingService;

    @GetMapping(value = "/backend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream backend code generation", description = "Streams Spring Boot CRUD code generation in real-time from prompt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streaming started"),
        @ApiResponse(responseCode = "400", description = "Invalid prompt")
    })
    public SseEmitter streamBackendCode(
            @RequestParam String prompt,
            @RequestParam(required = false) String token,
            Authentication authentication) {
        
        SseEmitter emitter = new SseEmitter(300000L);
        emitter.onCompletion(() -> log.info("SSE connection completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE error", ex);
            emitter.completeWithError(ex);
        });
        
        return streamingService.streamBackendCode(prompt, emitter);
    }

    @GetMapping(value = "/frontend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream frontend code generation", description = "Streams Angular TypeScript interfaces generation in real-time from prompt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streaming started"),
        @ApiResponse(responseCode = "400", description = "Invalid prompt")
    })
    public SseEmitter streamFrontendCode(
            @RequestParam String prompt,
            @RequestParam(required = false) String token,
            Authentication authentication) {
        
        SseEmitter emitter = new SseEmitter(300000L);
        emitter.onCompletion(() -> log.info("SSE connection completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE error", ex);
            emitter.completeWithError(ex);
        });
        
        return streamingService.streamFrontendCode(prompt, emitter);
    }
}

