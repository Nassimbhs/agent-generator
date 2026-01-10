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
                .map(this::parseOllamaResponse)
                .filter(response -> response != null) // Filter nulls first
                .filter(response -> {
                    // Check if this is a done marker - skip it, takeUntil will handle termination
                    Boolean done = response.getDone();
                    if (Boolean.TRUE.equals(done)) {
                        return false; // Don't process done markers
                    }
                    
                    // Must have valid response content
                    String responseText = response.getResponse();
                    if (responseText == null || responseText.isEmpty()) {
                        return false;
                    }
                    
                    // Skip if response contains refusal messages
                    String lower = responseText.toLowerCase();
                    if (lower.contains("i'm sorry") || lower.contains("i can't") || 
                        lower.contains("cannot") || lower.contains("unable to") ||
                        lower.contains("not able")) {
                        log.warn("LLM returned refusal, skipping: {}", responseText);
                        return false;
                    }
                    return true;
                })
                .takeUntil(response -> Boolean.TRUE.equals(response.getDone()))
                .map(response -> {
                    // Response should never be null here due to previous filters
                    // But check anyway to prevent null mapper error
                    if (response == null) {
                        log.error("Unexpected null response in map");
                        return "";
                    }
                    String resp = response.getResponse();
                    if (resp == null) {
                        log.warn("Null response content in map");
                        return "";
                    }
                    return resp;
                })
                .filter(text -> text != null && !text.isEmpty())
                .doOnNext(chunk -> log.debug("Received chunk: {}", chunk.substring(0, Math.min(50, chunk.length()))))
                .doOnError(error -> log.error("Error in streaming Ollama call: {}", error.getMessage(), error))
                .doOnComplete(() -> log.info("Streaming completed successfully"))
                .onErrorResume(error -> {
                    log.error("Streaming error occurred", error);
                    return Flux.just("// Error generating code: " + error.getMessage() + "\n");
                });
    }

    private OllamaResponse parseOllamaResponse(String jsonLine) {
        try {
            String trimmed = jsonLine.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(trimmed, OllamaResponse.class);
        } catch (Exception e) {
            log.debug("Failed to parse Ollama response line (might be partial): {}", jsonLine.substring(0, Math.min(100, jsonLine.length())), e);
            return null;
        }
    }

    private String buildSpringBootPrompt(String userPrompt) {
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
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                
                Flux<String> codeStream = generateSpringBootCrudStream(prompt);
                
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

    @Override
    public SseEmitter streamFrontendCode(String prompt, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullCode = new StringBuilder();
                
                Flux<String> codeStream = generateAngularInterfacesStream(prompt);
                
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

