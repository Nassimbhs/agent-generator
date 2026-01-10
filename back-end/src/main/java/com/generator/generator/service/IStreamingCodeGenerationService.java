package com.generator.generator.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

public interface IStreamingCodeGenerationService {
    Flux<String> generateSpringBootCrudStream(String prompt);
    Flux<String> generateSpringBootCrudStream(String prompt, String existingProjectPath);
    Flux<String> generateAngularInterfacesStream(String prompt);
    Flux<String> generateAngularInterfacesStream(String prompt, String existingProjectPath);
    SseEmitter streamBackendCode(String prompt, SseEmitter emitter);
    SseEmitter streamFrontendCode(String prompt, SseEmitter emitter);
}

