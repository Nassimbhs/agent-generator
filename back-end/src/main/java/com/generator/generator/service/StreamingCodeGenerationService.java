package com.generator.generator.service;

import com.generator.generator.dto.ollama.OllamaRequest;
import com.generator.generator.dto.ollama.OllamaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

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
                .bodyToFlux(OllamaResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .filter(response -> response.getResponse() != null && !response.getResponse().isEmpty())
                .map(OllamaResponse::getResponse)
                .doOnError(error -> log.error("Error in streaming Ollama call: {}", error.getMessage()))
                .doOnComplete(() -> log.info("Streaming completed"));
    }

    private String buildSpringBootPrompt(String userPrompt) {
        return """
            You are an expert Spring Boot developer. Generate complete CRUD REST API code based on the following requirements.
            
            Requirements:
            %s
            
            Generate the following Spring Boot components:
            1. Entity class with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, etc.)
            2. Repository interface extending JpaRepository
            3. Service interface (IService) with CRUD methods
            4. Service implementation with all CRUD operations
            5. Controller with REST endpoints (@RestController, @RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping)
            6. DTO classes for request and response
            7. Use Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
            8. Include proper validation annotations (@NotNull, @NotBlank, @Size, etc.)
            9. Add Swagger/OpenAPI annotations (@Operation, @ApiResponse, etc.)
            10. Include proper error handling
            
            Return ONLY the Java code, no explanations. Format the code properly with proper indentation.
            Use Spring Boot 4.0.1, Java 17, and PostgreSQL.
            
            Generate complete, production-ready code.
            """.formatted(userPrompt);
    }

    private String buildAngularPrompt(String userPrompt) {
        return """
            You are an expert Angular developer. Generate TypeScript interface/model files based on the following requirements.
            
            Requirements:
            %s
            
            Generate the following Angular TypeScript components:
            1. Model/Interface files for each entity
            2. Use proper TypeScript types (string, number, boolean, Date, etc.)
            3. Include optional properties with '?' where appropriate
            4. Add proper comments/documentation
            5. Export interfaces properly
            6. Use naming conventions: PascalCase for interfaces, camelCase for properties
            7. Include all fields from the backend entity
            8. Add proper types for relationships (arrays, nested objects)
            
            Return ONLY the TypeScript code, no explanations. Format the code properly with proper indentation.
            Use Angular 17+ and TypeScript 5+.
            
            Generate complete, production-ready TypeScript interfaces/models.
            """.formatted(userPrompt);
    }
}

