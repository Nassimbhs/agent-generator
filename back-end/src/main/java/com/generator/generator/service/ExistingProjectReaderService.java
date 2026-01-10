package com.generator.generator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class ExistingProjectReaderService {

    private static final Set<String> JAVA_EXTENSIONS = Set.of(".java", ".properties", ".xml", ".yml", ".yaml", ".json");
    private static final Set<String> EXCLUDED_DIRS = Set.of("target", ".git", "node_modules", ".idea", ".vscode", "build", "dist");
    private static final int MAX_FILE_SIZE = 100_000; // 100KB per file
    private static final int MAX_TOTAL_SIZE = 500_000; // 500KB total
    private static final int MAX_FILES = 50;

    /**
     * Reads existing Spring Boot project files and formats them as context for LLM.
     * Returns formatted string with all relevant project files.
     *
     * @param projectPath Path to the existing Spring Boot project directory
     * @return Formatted string with existing code files, or empty string if path is invalid
     */
    public String readProjectFiles(String projectPath) {
        if (projectPath == null || projectPath.trim().isEmpty()) {
            log.debug("No project path provided, skipping file reading");
            return "";
        }

        try {
            Path path = Paths.get(projectPath).toAbsolutePath().normalize();
            
            // Security check: ensure path exists and is a directory
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                log.warn("Project path does not exist or is not a directory: {}", path);
                return "";
            }

            log.info("Reading existing project files from: {}", path);
            Map<String, String> files = scanProjectFiles(path);
            
            if (files.isEmpty()) {
                log.warn("No Java files found in project path: {}", path);
                return "";
            }

            String context = formatFilesForContext(files);
            log.info("Read {} files ({} chars) from existing project", files.size(), context.length());
            return context;

        } catch (Exception e) {
            log.error("Error reading project files from path: {}", projectPath, e);
            return "";
        }
    }

    /**
     * Reads project files and returns a map of file paths to their content.
     * Only includes relevant Spring Boot files (Java, properties, XML, etc.)
     */
    public Map<String, String> readProjectStructure(String projectPath) {
        if (projectPath == null || projectPath.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Path path = Paths.get(projectPath).toAbsolutePath().normalize();
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return Collections.emptyMap();
            }
            return scanProjectFiles(path);
        } catch (Exception e) {
            log.error("Error reading project structure from path: {}", projectPath, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Scans a directory for relevant Spring Boot files.
     */
    private Map<String, String> scanProjectFiles(Path rootPath) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        int totalSize = 0;
        int fileCount = 0;

        try (Stream<Path> paths = Files.walk(rootPath)) {
            for (Path path : paths.toList()) {
                // Skip excluded directories
                if (isExcluded(path, rootPath)) {
                    continue;
                }

                // Only process files with relevant extensions
                if (!Files.isRegularFile(path) || !hasRelevantExtension(path)) {
                    continue;
                }

                // Check file size limit
                long fileSize = Files.size(path);
                if (fileSize > MAX_FILE_SIZE || fileSize == 0) {
                    log.debug("Skipping large or empty file: {} ({} bytes)", path, fileSize);
                    continue;
                }

                // Check total size and file count limits
                if (totalSize + fileSize > MAX_TOTAL_SIZE || fileCount >= MAX_FILES) {
                    log.warn("Reached size/file limit. Stopping file reading. Total: {} bytes, Files: {}", totalSize, fileCount);
                    break;
                }

                try {
                    String content = Files.readString(path);
                    String relativePath = rootPath.relativize(path).toString().replace('\\', '/');
                    files.put(relativePath, content);
                    totalSize += fileSize;
                    fileCount++;
                    log.debug("Read file: {} ({} bytes)", relativePath, fileSize);
                } catch (IOException e) {
                    log.warn("Error reading file: {}", path, e);
                    // Continue with other files
                }
            }
        }

        return files;
    }

    /**
     * Checks if a path should be excluded from scanning.
     */
    private boolean isExcluded(Path path, Path rootPath) {
        Path relativePath = rootPath.relativize(path);
        for (String segment : relativePath) {
            if (EXCLUDED_DIRS.contains(segment.toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a file has a relevant extension for Spring Boot projects.
     */
    private boolean hasRelevantExtension(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return JAVA_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * Formats files as context string for LLM prompt.
     * Uses the same format as generated code: FILE: path with code blocks.
     */
    private String formatFilesForContext(Map<String, String> files) {
        if (files.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("EXISTING PROJECT FILES:\n");
        context.append("The following files already exist in the project:\n\n");

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();
            String language = detectLanguage(filePath);

            context.append("FILE: ").append(filePath).append("\n");
            context.append("```").append(language).append("\n");
            context.append(content);
            if (!content.endsWith("\n")) {
                context.append("\n");
            }
            context.append("```\n\n");
        }

        return context.toString();
    }

    /**
     * Detects programming language from file extension.
     */
    private String detectLanguage(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".properties")) return "properties";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        return "text";
    }
}

