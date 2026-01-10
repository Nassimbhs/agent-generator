package com.generator.generator.service;

import com.generator.generator.dto.ProjectFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProjectParserService {

    // More flexible pattern that handles:
    // FILE: path/to/file.ext
    // ```language
    // content
    // ```
    // OR
    // FILE: path/to/file.ext
    // content (without code blocks)
    private static final Pattern FILE_PATTERN = Pattern.compile(
        "(?i)FILE:\\s*([^\\n\\r]+?)\\s*(?:\\r?\\n)?(?:```(\\w+)?\\s*\\r?\\n)?([\\s\\S]*?)(?:```|(?=FILE:|$))",
        Pattern.DOTALL | Pattern.MULTILINE
    );

    public List<ProjectFile> parseProjectFiles(String generatedCode) {
        List<ProjectFile> files = new ArrayList<>();
        
        if (generatedCode == null || generatedCode.trim().isEmpty()) {
            return files;
        }

        Matcher matcher = FILE_PATTERN.matcher(generatedCode);
        
        while (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String language = matcher.group(2) != null ? matcher.group(2).trim() : detectLanguage(filePath);
            String content = matcher.group(3).trim();
            
            // Extract filename from path
            String fileName = filePath.contains("/") 
                ? filePath.substring(filePath.lastIndexOf("/") + 1)
                : filePath;
            
            ProjectFile file = ProjectFile.builder()
                    .path(filePath)
                    .name(fileName)
                    .content(content)
                    .language(language)
                    .type("file")
                    .build();
            
            files.add(file);
            log.debug("Parsed file: {} ({} bytes)", filePath, content.length());
        }
        
        // If no FILE: patterns found, treat entire code as a single file
        if (files.isEmpty() && !generatedCode.trim().isEmpty()) {
            ProjectFile defaultFile = ProjectFile.builder()
                    .path("generated-code.txt")
                    .name("generated-code.txt")
                    .content(generatedCode)
                    .language("text")
                    .type("file")
                    .build();
            files.add(defaultFile);
        }
        
        return files;
    }

    private String detectLanguage(String filePath) {
        if (filePath.endsWith(".html")) return "html";
        if (filePath.endsWith(".css")) return "css";
        if (filePath.endsWith(".js")) return "javascript";
        if (filePath.endsWith(".json")) return "json";
        if (filePath.endsWith(".md")) return "markdown";
        if (filePath.endsWith(".java")) return "java";
        if (filePath.endsWith(".ts")) return "typescript";
        if (filePath.endsWith(".tsx")) return "typescript";
        if (filePath.endsWith(".py")) return "python";
        if (filePath.endsWith(".xml")) return "xml";
        if (filePath.endsWith(".yml") || filePath.endsWith(".yaml")) return "yaml";
        return "text";
    }
}

