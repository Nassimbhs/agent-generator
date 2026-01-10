package com.generator.generator.controller;

import com.generator.generator.dto.ProjectStructure;
import com.generator.generator.service.IProjectStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Templates", description = "Empty project template download APIs")
public class TemplateController {

    private final IProjectStructureService structureService;

    @GetMapping("/backend/empty")
    @Operation(summary = "Download empty Spring Boot project template", description = "Downloads an empty Spring Boot project structure as ZIP")
    @ApiResponse(responseCode = "200", description = "Template downloaded successfully")
    public ResponseEntity<byte[]> downloadEmptyBackendTemplate() {
        try {
            ProjectStructure structure = createEmptyBackendStructure();
            byte[] zipBytes = structureService.createZipFile(structure);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "spring-boot-template.zip");
            headers.setContentLength(zipBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);
        } catch (Exception e) {
            log.error("Error creating backend template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/frontend/empty")
    @Operation(summary = "Download empty frontend project template", description = "Downloads an empty HTML/CSS/JS project structure as ZIP")
    @ApiResponse(responseCode = "200", description = "Template downloaded successfully")
    public ResponseEntity<byte[]> downloadEmptyFrontendTemplate() {
        try {
            ProjectStructure structure = createEmptyFrontendStructure();
            byte[] zipBytes = structureService.createZipFile(structure);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "frontend-template.zip");
            headers.setContentLength(zipBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);
        } catch (Exception e) {
            log.error("Error creating frontend template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ProjectStructure createEmptyBackendStructure() {
        List<com.generator.generator.dto.ProjectFile> files = new ArrayList<>();
        
        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("pom.xml")
                .name("pom.xml")
                .language("xml")
                .type("file")
                .content("""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.1</version>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("src/main/java/com/example/demo/DemoApplication.java")
                .name("DemoApplication.java")
                .language("java")
                .type("file")
                .content("""package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("src/main/resources/application.properties")
                .name("application.properties")
                .language("properties")
                .type("file")
                .content("""server.port=8080
spring.application.name=demo""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("README.md")
                .name("README.md")
                .language("markdown")
                .type("file")
                .content("""# Spring Boot Application

Empty Spring Boot project template.
""")
                .build());

        return structureService.buildProjectStructure(
                files.stream()
                        .map(f -> String.format("FILE: %s\n```%s\n%s\n```\n", 
                                f.getPath(), f.getLanguage(), f.getContent()))
                        .reduce("", String::concat)
        );
    }

    private ProjectStructure createEmptyFrontendStructure() {
        List<com.generator.generator.dto.ProjectFile> files = new ArrayList<>();
        
        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("index.html")
                .name("index.html")
                .language("html")
                .type("file")
                .content("""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My App</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div id="app"></div>
    <script src="app.js"></script>
</body>
</html>""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("styles.css")
                .name("styles.css")
                .language("css")
                .type("file")
                .content("""* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: Arial, sans-serif;
    line-height: 1.6;
}

#app {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("app.js")
                .name("app.js")
                .language("javascript")
                .type("file")
                .content("""// Your JavaScript code here
document.addEventListener('DOMContentLoaded', function() {
    console.log('App loaded');
});""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("package.json")
                .name("package.json")
                .language("json")
                .type("file")
                .content("""{
  "name": "my-app",
  "version": "1.0.0",
  "description": "Frontend application",
  "main": "app.js",
  "scripts": {
    "start": "npx http-server . -p 8080"
  }
}""")
                .build());

        files.add(com.generator.generator.dto.ProjectFile.builder()
                .path("README.md")
                .name("README.md")
                .language("markdown")
                .type("file")
                .content("""# Frontend Application

Empty frontend project template.

## Getting Started

Run the application:
\`\`\`bash
npm start
\`\`\`
""")
                .build());

        return structureService.buildProjectStructure(
                files.stream()
                        .map(f -> String.format("FILE: %s\n```%s\n%s\n```\n", 
                                f.getPath(), f.getLanguage(), f.getContent()))
                        .reduce("", String::concat)
        );
    }
}

