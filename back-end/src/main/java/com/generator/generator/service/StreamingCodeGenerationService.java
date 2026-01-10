package com.generator.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generator.generator.dto.ollama.OllamaRequest;
import com.generator.generator.dto.ollama.OllamaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingCodeGenerationService implements IStreamingCodeGenerationService {

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model.name:qwen2.5-coder}")
    private String modelName;

    @Value("${ollama.timeout:300}")
    private Long timeoutSeconds;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> generateSpringBootCrudStream(String prompt) {
        String systemPrompt = buildSpringBootPrompt(prompt);
        log.info("Streaming Spring Boot CRUD code generation for prompt: {}", prompt);
        return generateCodeStream(systemPrompt);
    }

    @Override
    public Flux<String> generateAngularInterfacesStream(String prompt) {
        String systemPrompt = buildAngularPrompt(prompt);
        log.info("Streaming Angular TypeScript interfaces generation for prompt: {}", prompt);
        return generateCodeStream(systemPrompt);
    }

    private Flux<String> generateCodeStream(String prompt) {
        OllamaRequest request = OllamaRequest.builder()
                .model(modelName)
                .prompt(prompt)
                .stream(true)  // Enable streaming
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(ollamaApiUrl)
                .build();

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(buffer -> {
                    String content = buffer.toString(StandardCharsets.UTF_8);
                    DataBufferUtils.release(buffer);
                    return content;
                })
                .concatMap(content -> {
                    // Split by newlines and filter empty lines
                    String[] lines = content.split("\\r?\\n");
                    return Flux.fromArray(lines)
                            .filter(line -> line != null && !line.trim().isEmpty());
                })
                .doOnNext(rawContent -> log.debug("Raw Ollama response chunk: {}", rawContent.substring(0, Math.min(100, rawContent.length()))))
                .map(this::parseOllamaResponse)
                .doOnNext(response -> {
                    if (response != null) {
                        log.debug("Parsed response - done: {}, content length: {}", 
                            response.getDone(), 
                            response.getResponse() != null ? response.getResponse().length() : 0);
                    } else {
                        log.warn("Parsed response is null");
                    }
                })
                .filter(response -> response != null) // Filter nulls first
                .filter(response -> {
                    Boolean done = response.getDone();
                    String responseText = response.getResponse();
                    
                    // If done marker, allow it through for takeUntil but don't emit content
                    if (Boolean.TRUE.equals(done)) {
                        log.info("Received done marker from Ollama");
                        return false; // Don't emit done markers as content
                    }
                    
                    // Must have valid response content
                    if (responseText == null || responseText.trim().isEmpty()) {
                        log.debug("Empty response chunk, skipping");
                        return false;
                    }
                    
                    // Skip if response contains refusal messages (but log first 200 chars)
                    String lower = responseText.toLowerCase();
                    if (lower.contains("i'm sorry") || lower.contains("i can't") || 
                        lower.contains("cannot") || lower.contains("unable to") ||
                        lower.contains("not able") || lower.contains("i apologize")) {
                        log.warn("LLM returned refusal message: {}", 
                            responseText.substring(0, Math.min(200, responseText.length())));
                        return false;
                    }
                    
                    log.debug("Accepting response chunk: {} chars", responseText.length());
                    return true;
                })
                .takeUntil(response -> {
                    Boolean done = response.getDone();
                    if (Boolean.TRUE.equals(done)) {
                        log.info("Stream termination triggered by done marker");
                    }
                    return Boolean.TRUE.equals(done);
                })
                .map(response -> {
                    if (response == null) {
                        log.error("Unexpected null response in final map");
                        return "";
                    }
                    String resp = response.getResponse();
                    if (resp == null || resp.isEmpty()) {
                        log.warn("Empty response in final map");
                        return "";
                    }
                    log.debug("Mapping response chunk: {} chars", resp.length());
                    return resp;
                })
                .filter(text -> text != null && !text.trim().isEmpty())
                .doOnNext(chunk -> log.debug("Final chunk being emitted: {} chars - {}", chunk.length(), chunk.substring(0, Math.min(50, chunk.length()))))
                .doOnComplete(() -> log.info("Flux stream completed normally"))
                .doOnError(error -> log.error("Flux stream error: {}", error.getMessage(), error))
                .onErrorResume(error -> {
                    log.error("Streaming error occurred, returning error message", error);
                    return Flux.just("// Error generating code: " + error.getMessage() + "\n");
                });
    }

    private OllamaResponse parseOllamaResponse(String jsonLine) {
        try {
            String trimmed = jsonLine.trim();
            if (trimmed.isEmpty()) {
                log.debug("Empty line in Ollama response");
                return null;
            }
            
            // Remove potential BOM or leading whitespace
            if (trimmed.startsWith("\uFEFF")) {
                trimmed = trimmed.substring(1);
            }
            
            // Try to parse as JSON
            OllamaResponse response = objectMapper.readValue(trimmed, OllamaResponse.class);
            log.debug("Successfully parsed Ollama response - done: {}, has content: {}", 
                response.getDone(), 
                response.getResponse() != null && !response.getResponse().isEmpty());
            return response;
        } catch (Exception e) {
            // Sometimes Ollama sends partial JSON or non-JSON lines
            log.debug("Failed to parse Ollama response line: {} - Error: {}", 
                jsonLine.substring(0, Math.min(200, jsonLine.length())), 
                e.getMessage());
            return null;
        }
    }

    private String buildSpringBootPrompt(String userPrompt) {
        return String.format("""
            You are a Spring Boot expert. Generate a complete Spring Boot CRUD application based on the requirements.
            
            Format each file as:
            
            FILE: path/to/file.ext
            ```language
            // file content here
            ```
            
            Requirements: %s
            
            Generate all Spring Boot files needed:
            1. Entity classes (JPA entities)
            2. Repository interfaces (Spring Data JPA)
            3. Service interfaces (IService)
            4. Service implementations
            5. Controller classes (REST controllers)
            6. DTO classes (Request/Response)
            7. Configuration files (application.properties, pom.xml)
            8. Main application class
            
            For each file, show the complete code with proper imports. Use proper Java package structure.
            Return ONLY the files with the format above, no explanations between files.
            Start generating files now.
            """, userPrompt);
    }

    private String buildAngularPrompt(String userPrompt) {
        return """
            Generate a complete project structure with all files. Format each file as:
            
            FILE: path/to/file.ext
            ```language
            // file content here
            ```
            
            Requirements: %s
            
            Generate all files needed for the project:
            1. HTML files (index.html, etc.)
            2. CSS files (styles.css, etc.)
            3. JavaScript files (app.js, etc.)
            4. Configuration files (package.json, README.md, etc.)
            5. Any other necessary files
            
            For each file, show the full path and complete code. Use proper file structure with folders.
            Return ONLY the files with the format above, no explanations between files.
            """.formatted(userPrompt);
    }

    @Override
    public SseEmitter streamBackendCode(String prompt, SseEmitter emitter) {
        log.info("Starting backend code stream for prompt: {} chars", prompt.length());
        
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                AtomicInteger chunkCount = new AtomicInteger(0);
                
                Flux<String> codeStream = generateSpringBootCrudStream(prompt);
                
                log.info("Flux created, subscribing...");
                
                codeStream.subscribe(
                    chunk -> {
                        try {
                            int count = chunkCount.incrementAndGet();
                            fullCode.append(chunk);
                            log.debug("Sending chunk #{}: {} chars", count, chunk.length());
                            
                            emitter.send(SseEmitter.event()
                                    .name("code-chunk")
                                    .data(chunk));
                        } catch (IOException e) {
                            log.error("Error sending chunk #{}", chunkCount.get(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("Error completing emitter with error", ex);
                            }
                        }
                    },
                    error -> {
                        log.error("Error in stream after {} chunks", chunkCount.get(), error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error generating code: " + error.getMessage()));
                            Thread.sleep(100);
                            emitter.completeWithError(error);
                        } catch (Exception e) {
                            log.error("Error sending error event", e);
                            try {
                                emitter.completeWithError(error);
                            } catch (Exception ex) {
                                log.error("Error completing emitter", ex);
                            }
                        }
                    },
                    () -> {
                        try {
                            log.info("Stream completed successfully. Total chunks: {}, Total code length: {}", 
                                chunkCount.get(), fullCode.length());
                            
                            if (fullCode.length() == 0) {
                                log.warn("Stream completed but no code was generated!");
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("No code was generated. Please check Ollama is running and the model is available."));
                            } else {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Code generation completed"));
                            }
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
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("Error completing emitter with initial error", ex);
                }
            }
        });
        
        return emitter;
    }

    @Override
    public SseEmitter streamFrontendCode(String prompt, SseEmitter emitter) {
        log.info("Starting frontend code stream for prompt: {} chars", prompt.length());
        
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                AtomicInteger chunkCount = new AtomicInteger(0);
                
                Flux<String> codeStream = generateAngularInterfacesStream(prompt);
                
                log.info("Flux created, subscribing...");
                
                codeStream.subscribe(
                    chunk -> {
                        try {
                            int count = chunkCount.incrementAndGet();
                            fullCode.append(chunk);
                            log.debug("Sending chunk #{}: {} chars", count, chunk.length());
                            
                            emitter.send(SseEmitter.event()
                                    .name("code-chunk")
                                    .data(chunk));
                        } catch (IOException e) {
                            log.error("Error sending chunk #{}", chunkCount.get(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("Error completing emitter with error", ex);
                            }
                        }
                    },
                    error -> {
                        log.error("Error in stream after {} chunks", chunkCount.get(), error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error generating code: " + error.getMessage()));
                            Thread.sleep(100);
                            emitter.completeWithError(error);
                        } catch (Exception e) {
                            log.error("Error sending error event", e);
                            try {
                                emitter.completeWithError(error);
                            } catch (Exception ex) {
                                log.error("Error completing emitter", ex);
                            }
                        }
                    },
                    () -> {
                        try {
                            log.info("Stream completed successfully. Total chunks: {}, Total code length: {}", 
                                chunkCount.get(), fullCode.length());
                            
                            if (fullCode.length() == 0) {
                                log.warn("Stream completed but no code was generated!");
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("No code was generated. Please check Ollama is running and the model is available."));
                            } else {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Code generation completed"));
                            }
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

