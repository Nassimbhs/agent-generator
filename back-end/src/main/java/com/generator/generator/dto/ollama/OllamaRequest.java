package com.generator.generator.dto.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaRequest {
    private String model;
    private String prompt;
    
    @JsonProperty("stream")
    @Builder.Default
    private Boolean stream = false;
}

