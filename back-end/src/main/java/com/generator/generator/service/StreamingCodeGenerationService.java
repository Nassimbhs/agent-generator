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

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingCodeGenerationService {

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model.name:qwen2.5-coder}")
    private String modelName;

    @Value("${ollama.timeout:300}")
    private Long timeoutSeconds;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public Flux<String> generateSpringBootCrudStream(String prompt) {
        String systemPrompt = buildSpringBootPrompt(prompt);
        log.info("Streaming Spring Boot CRUD code generation for prompt: {}", prompt);
        return generateCodeStream(systemPrompt);
    }

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
                            .filter(line -> !line.trim().isEmpty());
                })
                .map(this::parseOllamaResponse)
                .filter(response -> response != null)
                .filter(response -> {
                    // Skip if response contains refusal messages
                    String responseText = response.getResponse();
                    if (responseText != null) {
                        String lower = responseText.toLowerCase();
                        if (lower.contains("i'm sorry") || lower.contains("i can't") || 
                            lower.contains("cannot") || lower.contains("unable to") ||
                            lower.contains("not able")) {
                            log.warn("LLM returned refusal, skipping: {}", responseText);
                            return false;
                        }
                    }
                    return responseText != null && !responseText.isEmpty();
                })
                .takeUntil(response -> Boolean.TRUE.equals(response.getDone()))
                .map(OllamaResponse::getResponse)
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
            Generate Spring Boot CRUD code. Return ONLY Java code, no explanations.
            
            Requirements: %s
            
            Create:
            1. Entity class with JPA annotations
            2. Repository interface extending JpaRepository<Entity, Long>
            3. Service interface with CRUD methods
            4. Service implementation with all CRUD operations  
            5. REST Controller with @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
            6. DTO classes for request and response
            7. Use Lombok: @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
            8. Add validation: @NotNull, @NotBlank, @Size
            9. Add Swagger: @Operation, @ApiResponse
            10. Include error handling
            
            Use Spring Boot 4.0.1, Java 17, PostgreSQL. Return complete Java code only.
            """.formatted(userPrompt);
    }

    private String buildAngularPrompt(String userPrompt) {
        return """
            Generate Angular TypeScript interfaces. Return ONLY TypeScript code, no explanations.
            
            Requirements: %s
            
            Create TypeScript interfaces with:
            1. Proper types: string, number, boolean, Date
            2. Optional properties with '?'
            3. Export interfaces
            4. PascalCase for interfaces, camelCase for properties
            5. All fields from backend entity
            6. Types for relationships (arrays, nested objects)
            
            Use Angular 17+, TypeScript 5+. Return complete TypeScript code only.
            """.formatted(userPrompt);
    }
}

