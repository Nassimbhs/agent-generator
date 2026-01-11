package com.generator.generator.controller;

import com.generator.generator.entity.Project;
import com.generator.generator.entity.User;
import com.generator.generator.repository.ProjectRepository;
import com.generator.generator.repository.UserRepository;
import com.generator.generator.service.CodeFormatterService;
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
    private final CodeFormatterService codeFormatterService;

    @GetMapping(value = "/{id}/generate/backend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream backend code generation", description = "Streams Spring Boot CRUD code generation in real-time. Optionally provide existingProjectPath to enhance with existing project files.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streaming started"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public SseEmitter streamBackendCode(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @RequestParam(required = false, value = "existingProjectPath") String existingProjectPath,
            Authentication authentication) {
        
        // CRITICAL: Validate authentication BEFORE creating SSE emitter
        // Once emitter is created, response is committed and Spring Security can't handle errors
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated request to SSE endpoint /api/projects/{}/generate/backend/stream", id);
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("Unauthenticated: Please provide a valid authentication token"));
                errorEmitter.completeWithError(new RuntimeException("Authentication required"));
            } catch (IOException e) {
                log.error("Error sending authentication error", e);
            }
            return errorEmitter;
        }
        
        String username = authentication.getName();
        if (username == null || username.isEmpty()) {
            log.error("Authentication object exists but username is null");
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("Authentication failed: Invalid user"));
                errorEmitter.completeWithError(new RuntimeException("Invalid authentication"));
            } catch (IOException e) {
                log.error("Error sending authentication error", e);
            }
            return errorEmitter;
        }
        
        log.info("Authenticated user: {} requesting backend code generation for project: {}", username, id);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new RuntimeException("User not found: " + username);
                });

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.error("Project not found or access denied. Project ID: {}, User ID: {}", id, user.getId());
                    return new RuntimeException("Project not found or access denied");
                });

        // Only create SSE emitter after all validations pass
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
                
                // Use hybrid approach: pass existing project path if provided (Option 3)
                Flux<String> codeStream = streamingService.generateSpringBootCrudStream(
                        project.getPrompt(), 
                        existingProjectPath);
                
                codeStream.subscribe(
                    chunk -> {
                        try {
                            if (chunk != null && !chunk.isEmpty()) {
                                fullCode.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .name("code-chunk")
                                        .data(chunk));
                            }
                        } catch (IOException e) {
                            log.error("Error sending chunk (stream may be closed): {}", e.getMessage());
                            // Don't try to complete again if stream is already closed
                            // IOException typically means the connection was closed
                            log.debug("Stream connection likely closed, stopping chunk sending");
                        } catch (IllegalStateException e) {
                            log.debug("Emitter already completed or closed: {}", e.getMessage());
                        } catch (Exception e) {
                            log.error("Unexpected error sending chunk: {}", e.getMessage(), e);
                        }
                    },
                    error -> {
                        log.error("Error in code generation stream: {}", error.getMessage(), error);
                        try {
                            // Try to send error event before completing
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error generating code: " + error.getMessage()));
                            Thread.sleep(100); // Give time for error event to be sent
                        } catch (Exception e) {
                            log.debug("Could not send error event (stream may be closed): {}", e.getMessage());
                        } finally {
                            try {
                                emitter.completeWithError(error);
                            } catch (Exception e) {
                                log.debug("Emitter already closed, ignoring error completion");
                            }
                        }
                    },
                    () -> {
                        try {
                            String finalCode = fullCode.toString();
                            log.info("Stream completed. Total code length: {} chars", finalCode.length());
                            
                            // Save the complete code to database
                            if (!finalCode.isEmpty()) {
                                // Format the code before saving
                                String formattedCode = codeFormatterService.formatGeneratedCode(finalCode);
                                project.setBackendCode(formattedCode);
                                projectRepository.save(project);
                                log.info("Saved formatted backend code to database for project: {} (original: {} chars, formatted: {} chars)", 
                                    id, finalCode.length(), formattedCode.length());
                            } else {
                                log.warn("Stream completed but no code was generated!");
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("No code was generated. Please check your prompt and Ollama connection."));
                                Thread.sleep(100);
                                emitter.complete();
                                return;
                            }
                            
                            // Send completion event and ensure it's flushed before closing
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Code generation completed. Total: " + finalCode.length() + " characters"));
                                // Give more time for complete event to be sent and flushed
                                Thread.sleep(500);
                            } catch (Exception sendEx) {
                                log.warn("Error sending complete event: {}", sendEx.getMessage());
                            }
                            
                            // Complete the emitter
                            try {
                                emitter.complete();
                                log.info("SSE emitter completed successfully for project: {}", id);
                            } catch (Exception completeEx) {
                                log.warn("Error completing emitter (may already be closed): {}", completeEx.getMessage());
                            }
                        } catch (Exception e) {
                            log.error("Error completing stream: {}", e.getMessage(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.debug("Emitter already closed, ignoring completion error");
                            }
                        }
                    }
                );
            } catch (Exception e) {
                log.error("Error starting code generation stream: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Failed to start code generation: " + e.getMessage()));
                    Thread.sleep(100);
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("Could not send error event: {}", ex.getMessage());
                }
            }
        });

        return emitter;
    }

    @GetMapping(value = "/{id}/generate/frontend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream frontend code generation", description = "Streams Angular TypeScript interfaces generation in real-time. Optionally provide existingProjectPath to enhance with existing project files.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streaming started"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public SseEmitter streamFrontendCode(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @RequestParam(required = false, value = "existingProjectPath") String existingProjectPath,
            Authentication authentication) {
        
        // CRITICAL: Validate authentication BEFORE creating SSE emitter
        // Once emitter is created, response is committed and Spring Security can't handle errors
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated request to SSE endpoint /api/projects/{}/generate/frontend/stream", id);
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("Unauthenticated: Please provide a valid authentication token"));
                errorEmitter.completeWithError(new RuntimeException("Authentication required"));
            } catch (IOException e) {
                log.error("Error sending authentication error", e);
            }
            return errorEmitter;
        }
        
        String username = authentication.getName();
        if (username == null || username.isEmpty()) {
            log.error("Authentication object exists but username is null");
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("Authentication failed: Invalid user"));
                errorEmitter.completeWithError(new RuntimeException("Invalid authentication"));
            } catch (IOException e) {
                log.error("Error sending authentication error", e);
            }
            return errorEmitter;
        }
        
        log.info("Authenticated user: {} requesting frontend code generation for project: {}", username, id);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new RuntimeException("User not found: " + username);
                });

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.error("Project not found or access denied. Project ID: {}, User ID: {}", id, user.getId());
                    return new RuntimeException("Project not found or access denied");
                });
        
        // Only create SSE emitter after all validations pass
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
                
                // Use hybrid approach: pass existing project path if provided (Option 3)
                Flux<String> codeStream = streamingService.generateAngularInterfacesStream(
                        project.getPrompt(), 
                        existingProjectPath);
                
                codeStream.subscribe(
                    chunk -> {
                        try {
                            if (chunk != null && !chunk.isEmpty()) {
                                fullCode.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .name("code-chunk")
                                        .data(chunk));
                            }
                        } catch (IOException e) {
                            log.error("Error sending chunk (stream may be closed): {}", e.getMessage());
                            // Don't try to complete again if stream is already closed
                            // IOException typically means the connection was closed
                            log.debug("Stream connection likely closed, stopping chunk sending");
                        } catch (IllegalStateException e) {
                            log.debug("Emitter already completed or closed: {}", e.getMessage());
                        } catch (Exception e) {
                            log.error("Unexpected error sending chunk: {}", e.getMessage(), e);
                        }
                    },
                    error -> {
                        log.error("Error in code generation stream: {}", error.getMessage(), error);
                        try {
                            // Try to send error event before completing
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error generating code: " + error.getMessage()));
                            Thread.sleep(100); // Give time for error event to be sent
                        } catch (Exception e) {
                            log.debug("Could not send error event (stream may be closed): {}", e.getMessage());
                        } finally {
                            try {
                                emitter.completeWithError(error);
                            } catch (Exception e) {
                                log.debug("Emitter already closed, ignoring error completion");
                            }
                        }
                    },
                    () -> {
                        try {
                            String finalCode = fullCode.toString();
                            log.info("Stream completed. Total code length: {} chars", finalCode.length());
                            
                            // Save the complete code to database
                            if (!finalCode.isEmpty()) {
                                // Format the code before saving
                                String formattedCode = codeFormatterService.formatGeneratedCode(finalCode);
                                project.setFrontendCode(formattedCode);
                                projectRepository.save(project);
                                log.info("Saved formatted frontend code to database for project: {} (original: {} chars, formatted: {} chars)", 
                                    id, finalCode.length(), formattedCode.length());
                            } else {
                                log.warn("Stream completed but no code was generated!");
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("No code was generated. Please check your prompt and Ollama connection."));
                                Thread.sleep(100);
                                emitter.complete();
                                return;
                            }
                            
                            // Send completion event and ensure it's flushed before closing
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Code generation completed. Total: " + finalCode.length() + " characters"));
                                // Give more time for complete event to be sent and flushed
                                Thread.sleep(500);
                            } catch (Exception sendEx) {
                                log.warn("Error sending complete event: {}", sendEx.getMessage());
                            }
                            
                            // Complete the emitter
                            try {
                                emitter.complete();
                                log.info("SSE emitter completed successfully for project: {}", id);
                            } catch (Exception completeEx) {
                                log.warn("Error completing emitter (may already be closed): {}", completeEx.getMessage());
                            }
                        } catch (Exception e) {
                            log.error("Error completing stream: {}", e.getMessage(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.debug("Emitter already closed, ignoring completion error");
                            }
                        }
                    }
                );
            } catch (Exception e) {
                log.error("Error starting code generation stream: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Failed to start code generation: " + e.getMessage()));
                    Thread.sleep(100);
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("Could not send error event: {}", ex.getMessage());
                }
            }
        });

        return emitter;
    }
}

