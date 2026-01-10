package com.generator.generator.controller;

import com.generator.generator.service.StreamingCodeGenerationService;
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
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Generator", description = "Direct code generation APIs")
@SecurityRequirement(name = "bearerAuth")
public class GeneratorController {

    private final StreamingCodeGenerationService streamingService;

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
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        emitter.onCompletion(() -> log.info("SSE connection completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE error", ex);
            emitter.completeWithError(ex);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                
                Flux<String> codeStream = streamingService.generateSpringBootCrudStream(prompt);
                
                codeStream.subscribe(
                    chunk -> {
                        try {
                            fullCode.append(chunk);
                            emitter.send(SseEmitter.event()
                                    .name("code-chunk")
                                    .data(chunk));
                        } catch (IOException e) {
                            log.error("Error sending chunk", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("Error in stream", error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error: " + error.getMessage()));
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        try {
                            log.info("Stream completed, sending final event");
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("Code generation completed"));
                            // Small delay before completing to ensure last event is sent
                            Thread.sleep(100);
                            emitter.complete();
                            log.info("SSE emitter completed successfully");
                        } catch (Exception e) {
                            log.error("Error completing stream", e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("Error completing emitter with error", ex);
                            }
                        }
                    }
                );
            } catch (Exception e) {
                log.error("Error starting stream", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
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
        
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                
                Flux<String> codeStream = streamingService.generateAngularInterfacesStream(prompt);
                
                codeStream.subscribe(
                    chunk -> {
                        try {
                            fullCode.append(chunk);
                            emitter.send(SseEmitter.event()
                                    .name("code-chunk")
                                    .data(chunk));
                        } catch (IOException e) {
                            log.error("Error sending chunk", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("Error in stream", error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error: " + error.getMessage()));
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        try {
                            log.info("Stream completed, sending final event");
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("Code generation completed"));
                            // Small delay before completing to ensure last event is sent
                            Thread.sleep(100);
                            emitter.complete();
                            log.info("SSE emitter completed successfully");
                        } catch (Exception e) {
                            log.error("Error completing stream", e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("Error completing emitter with error", ex);
                            }
                        }
                    }
                );
            } catch (Exception e) {
                log.error("Error starting stream", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}

