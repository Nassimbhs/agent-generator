package com.generator.generator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeGenerationService {

    private final OllamaClientService ollamaClientService;

    public String generateSpringBootCrud(String prompt) {
        String systemPrompt = buildSpringBootPrompt(prompt);
        log.info("Generating Spring Boot CRUD code for prompt: {}", prompt);
        return ollamaClientService.generateCode(systemPrompt);
    }

    public String generateAngularInterfaces(String prompt) {
        String systemPrompt = buildAngularPrompt(prompt);
        log.info("Generating Angular TypeScript interfaces for prompt: {}", prompt);
        return ollamaClientService.generateCode(systemPrompt);
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

