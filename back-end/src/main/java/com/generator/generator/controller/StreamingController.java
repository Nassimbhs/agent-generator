package com.generator.generator.controller;

import com.generator.generator.entity.Project;
import com.generator.generator.entity.User;
import com.generator.generator.repository.ProjectRepository;
import com.generator.generator.repository.UserRepository;
import com.generator.generator.service.StreamingCodeGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Streaming", description = "Streaming code generation APIs")
@SecurityRequirement(name = "bearerAuth")
public class StreamingController {

    private final StreamingCodeGenerationService streamingService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @GetMapping(value = "/{id}/generate/backend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream backend code generation", description = "Streams Spring Boot CRUD code generation in real-time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streaming started"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public SseEmitter streamBackendCode(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            Authentication authentication) {
        
        // Authentication is handled by Spring Security filter
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

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
                
                Flux<String> codeStream = streamingService.generateSpringBootCrudStream(project.getPrompt());
                
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
                            // Save the complete code to database
                            project.setBackendCode(fullCode.toString());
                            projectRepository.save(project);
                            
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("Code generation completed"));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("Error completing stream", e);
                            emitter.completeWithError(e);
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

    @GetMapping(value = "/{id}/generate/frontend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream frontend code generation", description = "Streams Angular TypeScript interfaces generation in real-time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streaming started"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public SseEmitter streamFrontendCode(
            @PathVariable Long id,
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
        
        // Authentication is handled by Spring Security filter
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));
        
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                
                Flux<String> codeStream = streamingService.generateAngularInterfacesStream(project.getPrompt());
                
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
                            // Save the complete code to database
                            project.setFrontendCode(fullCode.toString());
                            projectRepository.save(project);
                            
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("Code generation completed"));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("Error completing stream", e);
                            emitter.completeWithError(e);
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

