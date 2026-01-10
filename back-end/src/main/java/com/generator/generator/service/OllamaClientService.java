package com.generator.generator.service;

import com.generator.generator.dto.ollama.OllamaRequest;
import com.generator.generator.dto.ollama.OllamaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaClientService {

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model.name:qwen2.5-coder}")
    private String modelName;

    @Value("${ollama.timeout:300}")
    private Long timeoutSeconds;

    private final WebClient.Builder webClientBuilder;

    public String generateCode(String prompt) {
        try {
            log.info("Calling Ollama API at {} with model {}", ollamaApiUrl, modelName);
            
            OllamaRequest request = OllamaRequest.builder()
                    .model(modelName)
                    .prompt(prompt)
                    .stream(false)
                    .build();

            WebClient webClient = webClientBuilder
                    .baseUrl(ollamaApiUrl)
                    .build();

            OllamaResponse response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null && response.getResponse() != null) {
                log.info("Successfully received response from Ollama");
                return response.getResponse();
            } else {
                log.error("Empty response from Ollama");
                throw new RuntimeException("Empty response from Ollama API");
            }
        } catch (Exception e) {
            log.error("Error calling Ollama API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate code with Ollama: " + e.getMessage(), e);
        }
    }

    public Mono<String> generateCodeAsync(String prompt) {
        OllamaRequest request = OllamaRequest.builder()
                .model(modelName)
                .prompt(prompt)
                .stream(false)
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(ollamaApiUrl)
                .build();

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(OllamaResponse::getResponse)
                .doOnError(error -> log.error("Error in async Ollama call: {}", error.getMessage()));
    }
}

