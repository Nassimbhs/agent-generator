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
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ExistingProjectReaderService projectReaderService;
    private final CodeFormatterService codeFormatterService;

    @Override
    public Flux<String> generateSpringBootCrudStream(String prompt) {
        return generateSpringBootCrudStream(prompt, null);
    }

    @Override
    public Flux<String> generateSpringBootCrudStream(String prompt, String existingProjectPath) {
        // Conditionally read existing project files (Hybrid Approach - Option 3)
        String existingCode = "";
        if (existingProjectPath != null && !existingProjectPath.trim().isEmpty()) {
            log.info("Reading existing project files from: {}", existingProjectPath);
            existingCode = projectReaderService.readProjectFiles(existingProjectPath);
            if (!existingCode.isEmpty()) {
                log.info("Found existing project files, including in context ({} chars)", existingCode.length());
            } else {
                log.warn("No existing project files found at path: {}", existingProjectPath);
            }
        }
        
        String systemPrompt = buildSpringBootPrompt(prompt, existingCode);
        log.info("Streaming Spring Boot CRUD code generation for prompt: {} (with existing code: {})", 
                prompt, !existingCode.isEmpty() ? "yes" : "no");
        return generateCodeStream(systemPrompt);
    }

    @Override
    public Flux<String> generateAngularInterfacesStream(String prompt) {
        return generateAngularInterfacesStream(prompt, null);
    }

    @Override
    public Flux<String> generateAngularInterfacesStream(String prompt, String existingProjectPath) {
        // Conditionally read existing project files (Hybrid Approach - Option 3)
        String existingCode = "";
        if (existingProjectPath != null && !existingProjectPath.trim().isEmpty()) {
            log.info("Reading existing project files from: {}", existingProjectPath);
            existingCode = projectReaderService.readProjectFiles(existingProjectPath);
            if (!existingCode.isEmpty()) {
                log.info("Found existing project files, including in context ({} chars)", existingCode.length());
            } else {
                log.warn("No existing project files found at path: {}", existingProjectPath);
            }
        }
        
        String systemPrompt = buildAngularPrompt(prompt, existingCode);
        log.info("Streaming Angular TypeScript interfaces generation for prompt: {} (with existing code: {})", 
                prompt, !existingCode.isEmpty() ? "yes" : "no");
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

        // Increase timeout to 15 minutes for large code generation
        // Ensure minimum 15 minutes (900 seconds) for large code generation
        long extendedTimeout = timeoutSeconds != null && timeoutSeconds > 900 ? timeoutSeconds : 900L;
        
        log.info("Starting code generation stream with {} second timeout", extendedTimeout);

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(Duration.ofSeconds(extendedTimeout))
                .map(buffer -> {
                    String content = buffer.toString(StandardCharsets.UTF_8);
                    DataBufferUtils.release(buffer);
                    return content;
                })
                .concatMap(content -> {
                    // Split by newlines and filter empty lines more efficiently
                    String[] lines = content.split("\\r?\\n");
                    return Flux.fromArray(lines)
                            .filter(line -> line != null && !line.trim().isEmpty());
                })
                .map(this::parseOllamaResponse)
                .filter(response -> response != null) // Filter nulls first
                .takeUntil(response -> Boolean.TRUE.equals(response.getDone()))
                // Filter out done markers and invalid responses AFTER takeUntil sees them
                .filter(response -> {
                    Boolean done = response.getDone();
                    if (Boolean.TRUE.equals(done)) {
                        return false; // Don't emit done markers as content
                    }
                    
                    String responseText = response.getResponse();
                    if (responseText == null || responseText.trim().isEmpty()) {
                        return false;
                    }
                    
                    // Skip if response contains refusal messages (quick check)
                    String lower = responseText.toLowerCase();
                    if (lower.contains("i'm sorry") || lower.contains("i can't") || 
                        lower.contains("cannot") || lower.contains("unable to") ||
                        lower.contains("not able") || lower.contains("i apologize")) {
                        log.warn("LLM refusal detected, skipping chunk");
                        return false;
                    }
                    
                    return true;
                })
                .map(response -> {
                    String resp = response.getResponse();
                    return resp != null ? resp : "";
                })
                .filter(text -> !text.trim().isEmpty())
                .doOnNext(chunk -> log.debug("Emitting chunk: {} chars", chunk.length()))
                .doOnComplete(() -> log.info("Stream completed successfully"))
                .doOnError(error -> log.error("Stream error: {}", error.getMessage(), error))
                .onErrorResume(error -> {
                    log.error("Streaming error: {}", error.getMessage());
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

    private String buildSpringBootPrompt(String userPrompt, String existingCode) {
        if (existingCode != null && !existingCode.trim().isEmpty()) {
            // Enhanced prompt with existing code context
            return String.format("""
                You are a Spring Boot 4.0.1 expert working on an EXISTING project. Generate COMPLETE, COMPILATION-READY code.
                
                %s
                
                Based on the existing code above, generate or modify files to meet these requirements:
                %s
                
                CRITICAL FORMATTING RULES (MANDATORY):
                - ALWAYS use proper spacing between keywords: "public class", NOT "publicclass"
                - ALWAYS add newlines between code blocks and annotations
                - ALWAYS format code with proper indentation (4 spaces)
                - ALWAYS include ALL required imports
                - ALWAYS follow Java naming conventions (PascalCase for classes, camelCase for methods/variables)
                - ALWAYS separate annotations with newlines: "@Entity\\npublic class" NOT "@Entitypublic class"
                - ALWAYS add spaces: "private String" NOT "privateString", "public void" NOT "publicvoid"
                - ALWAYS format method signatures: "public void methodName()" NOT "publicvoidmethodName()"
                
                OUTPUT FORMAT (MANDATORY):
                FILE: path/to/file.ext
                ```language
                [EXACT CODE WITH PROPER FORMATTING - NO CONCATENATED KEYWORDS]
                ```
                
                IMPORTANT INSTRUCTIONS:
                - For EXISTING files that need changes: Show ONLY the modified sections or complete file with changes
                - For NEW files: Show the complete file with the FILE: format
                - Preserve existing code structure, patterns, and conventions
                - Maintain package structure and naming conventions from existing code
                - Only generate files that need to be created or modified
                
                Generate only the Spring Boot files needed:
                1. Entity classes (JPA entities) - NEW or MODIFIED only
                2. Repository interfaces (Spring Data JPA) - NEW or MODIFIED only
                3. Service interfaces (IService) - NEW or MODIFIED only
                4. Service implementations - NEW or MODIFIED only
                5. Controller classes (REST controllers) - NEW or MODIFIED only
                6. DTO classes (Request/Response) - NEW or MODIFIED only
                7. Configuration files - MODIFIED sections only if needed
                8. Main application class - MODIFIED only if needed
                
                Each file must be COMPLETE and COMPILATION-READY.
                NO PLACEHOLDERS. NO INCOMPLETE CODE. NO CONCATENATED KEYWORDS.
                
                For each file, show the complete code with proper imports. Use proper Java package structure matching the existing project.
                Return ONLY the files that need to be created or modified with the format above, no explanations between files.
                Start generating files now.
                """, existingCode, userPrompt);
        } else {
            // Enhanced prompt for new projects with explicit formatting rules
            return String.format("""
                You are a Spring Boot 4.0.1 expert. Generate a COMPLETE, COMPILATION-READY Spring Boot CRUD application.
                
                CRITICAL FORMATTING RULES (MANDATORY):
                - ALWAYS use proper spacing between keywords: "public class", NOT "publicclass"
                - ALWAYS add newlines between code blocks and annotations
                - ALWAYS format code with proper indentation (4 spaces)
                - ALWAYS include ALL required imports
                - ALWAYS follow Java naming conventions (PascalCase for classes, camelCase for methods/variables)
                - ALWAYS separate annotations with newlines: "@Entity\\npublic class" NOT "@Entitypublic class"
                - ALWAYS add spaces: "private String" NOT "privateString", "public void" NOT "publicvoid"
                - ALWAYS format method signatures: "public void methodName()" NOT "publicvoidmethodName()"
                - ALWAYS format field declarations: "private String description;" NOT "privateStringdescription;"
                
                OUTPUT FORMAT (MANDATORY):
                FILE: path/to/file.ext
                ```language
                [EXACT CODE WITH PROPER FORMATTING - NO CONCATENATED KEYWORDS]
                ```
                
                Requirements: %s
                
                Generate all Spring Boot files needed:
                1. Entity classes (JPA entities) with @Entity, @Table, @Id, @GeneratedValue
                2. Repository interfaces extending JpaRepository<Entity, Long>
                3. Service interfaces (IService) with CRUD method signatures
                4. Service implementations with @Service, @Autowired, all CRUD operations
                5. Controller classes with @RestController, @RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
                6. DTO classes (Request/Response) with Lombok annotations (@Data, @Builder)
                7. Configuration files (application.properties with database config, pom.xml with all dependencies)
                8. Main application class with @SpringBootApplication
                
                Each file must be COMPLETE and COMPILATION-READY.
                NO PLACEHOLDERS. NO INCOMPLETE CODE. NO CONCATENATED KEYWORDS.
                
                For each file, show the complete code with proper imports. Use proper Java package structure.
                Return ONLY the files with the format above, no explanations between files.
                Start generating files now.
                """, userPrompt);
        }
    }

    private String buildAngularPrompt(String userPrompt, String existingCode) {
        if (existingCode != null && !existingCode.trim().isEmpty()) {
            // Enhanced prompt with existing code context
            return String.format("""
                You are an Angular 17+ / TypeScript 5+ expert working on an EXISTING project. Generate COMPLETE, COMPILATION-READY code.
                
                %s
                
                Based on the existing code above, generate or modify files to meet these requirements:
                %s
                
                CRITICAL FORMATTING RULES (MANDATORY):
                - ALWAYS use proper spacing between keywords: "export class", NOT "exportclass"
                - ALWAYS add newlines between code blocks and decorators
                - ALWAYS format code with proper indentation (2 spaces for TypeScript/HTML)
                - ALWAYS include ALL required imports
                - ALWAYS follow TypeScript/Angular naming conventions
                - ALWAYS separate decorators with newlines: "@Component\\nexport class" NOT "@Componentexport class"
                - ALWAYS add spaces: "private name: string" NOT "privatename: string"
                - ALWAYS format method signatures: "public methodName(): void" NOT "publicmethodName():void"
                
                OUTPUT FORMAT (MANDATORY):
                FILE: path/to/file.ext
                ```language
                [EXACT CODE WITH PROPER FORMATTING - NO CONCATENATED KEYWORDS]
                ```
                
                IMPORTANT INSTRUCTIONS:
                - For EXISTING files that need changes: Show ONLY the modified sections or complete file with changes
                - For NEW files: Show the complete file with the FILE: format
                - Preserve existing code structure, patterns, and conventions
                - Maintain file structure and naming conventions from existing code
                - Only generate files that need to be created or modified
                
                Generate only the files needed:
                1. HTML files - NEW or MODIFIED only
                2. CSS files - NEW or MODIFIED only
                3. TypeScript/JavaScript files - NEW or MODIFIED only
                4. Configuration files (package.json, etc.) - MODIFIED sections only if needed
                5. Any other necessary files - NEW only
                
                Each file must be COMPLETE and COMPILATION-READY.
                NO PLACEHOLDERS. NO INCOMPLETE CODE. NO CONCATENATED KEYWORDS.
                
                For each file, show the full path and complete code. Use proper file structure with folders matching the existing project.
                Return ONLY the files that need to be created or modified with the format above, no explanations between files.
                """, existingCode, userPrompt);
        } else {
            // Enhanced prompt for new projects with explicit formatting rules
            return String.format("""
                You are an Angular 17+ / TypeScript 5+ expert. Generate a COMPLETE, COMPILATION-READY Angular project.
                
                CRITICAL FORMATTING RULES (MANDATORY):
                - ALWAYS use proper spacing between keywords: "export class", NOT "exportclass"
                - ALWAYS add newlines between code blocks and decorators
                - ALWAYS format code with proper indentation (2 spaces for TypeScript/HTML)
                - ALWAYS include ALL required imports
                - ALWAYS follow TypeScript/Angular naming conventions
                - ALWAYS separate decorators with newlines: "@Component\\nexport class" NOT "@Componentexport class"
                - ALWAYS add spaces: "private name: string" NOT "privatename: string"
                - ALWAYS format method signatures: "public methodName(): void" NOT "publicmethodName():void"
                
                OUTPUT FORMAT (MANDATORY):
                FILE: path/to/file.ext
                ```language
                [EXACT CODE WITH PROPER FORMATTING - NO CONCATENATED KEYWORDS]
                ```
                
                Requirements: %s
                
                Generate all files needed for the project:
                1. HTML files (index.html, component templates, etc.)
                2. CSS files (styles.css, component styles, etc.)
                3. TypeScript files (components, services, models, etc.)
                4. Configuration files (package.json with all dependencies, tsconfig.json, angular.json, etc.)
                5. Any other necessary files
                
                Each file must be COMPLETE and COMPILATION-READY.
                NO PLACEHOLDERS. NO INCOMPLETE CODE. NO CONCATENATED KEYWORDS.
                
                For each file, show the full path and complete code. Use proper file structure with folders.
                Return ONLY the files with the format above, no explanations between files.
                """, userPrompt);
        }
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
                                log.warn("Stream completed but no code was generated after {} chunks!", chunkCount.get());
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("No code was generated. Please check Ollama is running and the model is available."));
                                    Thread.sleep(500);
                                } catch (Exception ex) {
                                    log.error("Error sending error event: {}", ex.getMessage());
                                }
                            } else {
                                log.info("Sending complete event with {} characters of code", fullCode.length());
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("complete")
                                            .data("Code generation completed"));
                                    // Wait longer to ensure complete event is sent before closing
                                    Thread.sleep(500);
                                    log.info("Complete event sent, closing emitter...");
                                } catch (Exception ex) {
                                    log.error("Error sending complete event: {}", ex.getMessage(), ex);
                                }
                            }
                            emitter.complete();
                            log.info("SSE emitter completed successfully. Total: {} chunks, {} chars", 
                                chunkCount.get(), fullCode.length());
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
                            log.info("Sending chunk #{}: {} chars", count, chunk.length());
                            
                            // Send chunk with retry mechanism
                            emitter.send(SseEmitter.event()
                                    .name("code-chunk")
                                    .data(chunk));
                        } catch (IOException e) {
                            log.error("Error sending chunk #{}: {}", chunkCount.get(), e.getMessage(), e);
                            // Don't complete on first error, just log it
                        } catch (Exception e) {
                            log.error("Unexpected error sending chunk #{}: {}", chunkCount.get(), e.getMessage(), e);
                        }
                    },
                    error -> {
                        log.error("Error in stream after {} chunks: {}", chunkCount.get(), error.getMessage(), error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Error generating code: " + error.getMessage()));
                            Thread.sleep(200); // Give time for error event to be sent
                            emitter.completeWithError(error);
                        } catch (Exception e) {
                            log.error("Error sending error event: {}", e.getMessage(), e);
                            try {
                                emitter.completeWithError(error);
                            } catch (Exception ex) {
                                log.error("Error completing emitter: {}", ex.getMessage(), ex);
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
                                Thread.sleep(200);
                                emitter.complete();
                            } else {
                                // Send completion event and wait before closing
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Code generation completed"));
                                log.info("Sent complete event, waiting before closing emitter...");
                                Thread.sleep(300); // Wait longer to ensure complete event is sent
                                emitter.complete();
                                log.info("SSE emitter completed successfully");
                            }
                        } catch (Exception e) {
                            log.error("Error completing stream: {}", e.getMessage(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("Error completing emitter with error: {}", ex.getMessage(), ex);
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

