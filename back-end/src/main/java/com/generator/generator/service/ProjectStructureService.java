package com.generator.generator.service;

import com.generator.generator.dto.ProjectFile;
import com.generator.generator.dto.ProjectStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectStructureService implements IProjectStructureService {

    private final ProjectParserService parserService;

    @Override
    public ProjectStructure buildProjectStructure(String generatedCode) {
        List<ProjectFile> files = parserService.parseProjectFiles(generatedCode);
        ProjectStructure.TreeNode root = buildTree(files);
        
        return ProjectStructure.builder()
                .files(files)
                .root(root)
                .build();
    }

    private ProjectStructure.TreeNode buildTree(List<ProjectFile> files) {
        ProjectStructure.TreeNode root = ProjectStructure.TreeNode.builder()
                .label("Project")
                .type("folder")
                .icon("pi pi-folder")
                .expanded(true)
                .children(new ArrayList<>())
                .build();

        Map<String, ProjectStructure.TreeNode> folderMap = new HashMap<>();
        folderMap.put("", root);

        // Sort files by path
        files.sort(Comparator.comparing(ProjectFile::getPath));

        for (ProjectFile file : files) {
            String path = file.getPath();
            String[] parts = path.split("/");
            
            StringBuilder currentPath = new StringBuilder();
            ProjectStructure.TreeNode parent = root;

            // Create folder nodes
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) currentPath.append("/");
                currentPath.append(parts[i]);
                String folderPath = currentPath.toString();
                
                if (!folderMap.containsKey(folderPath)) {
                    ProjectStructure.TreeNode folder = ProjectStructure.TreeNode.builder()
                            .label(parts[i])
                            .type("folder")
                            .icon("pi pi-folder")
                            .expanded(true)
                            .children(new ArrayList<>())
                            .build();
                    
                    folderMap.put(folderPath, folder);
                    parent.getChildren().add(folder);
                }
                parent = folderMap.get(folderPath);
            }

            // Create file node
            String fileName = parts[parts.length - 1];
            String icon = getFileIcon(file.getLanguage());
            
            ProjectStructure.TreeNode fileNode = ProjectStructure.TreeNode.builder()
                    .label(fileName)
                    .data(path)
                    .type("file")
                    .icon(icon)
                    .expanded(false)
                    .children(null)
                    .build();
            
            parent.getChildren().add(fileNode);
        }

        return root;
    }

    private String getFileIcon(String language) {
        if (language == null) return "pi pi-file";
        return switch (language.toLowerCase()) {
            case "html" -> "pi pi-code";
            case "css" -> "pi pi-palette";
            case "javascript", "typescript" -> "pi pi-code";
            case "json" -> "pi pi-file-edit";
            case "java" -> "pi pi-file";
            case "markdown" -> "pi pi-file-edit";
            default -> "pi pi-file";
        };
    }

    @Override
    public byte[] createZipFile(ProjectStructure structure) throws Exception {
        log.info("Creating ZIP file from structure with {} files", 
            structure != null && structure.getFiles() != null ? structure.getFiles().size() : 0);
        
        if (structure == null || structure.getFiles() == null || structure.getFiles().isEmpty()) {
            log.warn("Cannot create ZIP: structure is null or has no files");
            throw new IllegalArgumentException("Project structure is empty or null");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int fileCount = 0;
            for (ProjectFile file : structure.getFiles()) {
                if (file == null || file.getPath() == null || file.getPath().isEmpty()) {
                    log.warn("Skipping invalid file entry");
                    continue;
                }
                
                try {
                    ZipEntry entry = new ZipEntry(file.getPath());
                    zos.putNextEntry(entry);
                    String content = file.getContent() != null ? file.getContent() : "";
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                    fileCount++;
                    log.debug("Added file to ZIP: {} ({} bytes)", file.getPath(), content.length());
                } catch (Exception e) {
                    log.error("Error adding file {} to ZIP: {}", file.getPath(), e.getMessage(), e);
                    // Continue with other files
                }
            }
            log.info("ZIP file created successfully with {} files", fileCount);
        }
        
        byte[] zipBytes = baos.toByteArray();
        log.info("ZIP file size: {} bytes", zipBytes.length);
        return zipBytes;
    }
}

