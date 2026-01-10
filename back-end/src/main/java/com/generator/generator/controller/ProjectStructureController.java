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

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Structure", description = "Project structure and download APIs")
public class ProjectStructureController {

    private final IProjectStructureService structureService;

    @PostMapping("/structure")
    @Operation(summary = "Parse project structure", description = "Parses generated code into project file structure")
    @ApiResponse(responseCode = "200", description = "Project structure parsed successfully")
    public ResponseEntity<ProjectStructure> getProjectStructure(@RequestBody String generatedCode) {
        try {
            ProjectStructure structure = structureService.buildProjectStructure(generatedCode);
            return ResponseEntity.ok(structure);
        } catch (Exception e) {
            log.error("Error parsing project structure", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/download")
    @Operation(summary = "Download project as ZIP", description = "Creates and downloads a ZIP file containing all project files")
    @ApiResponse(responseCode = "200", description = "ZIP file created successfully")
    public ResponseEntity<byte[]> downloadProject(@RequestBody ProjectStructure structure) {
        try {
            log.info("Received download request for structure with {} files", 
                structure != null && structure.getFiles() != null ? structure.getFiles().size() : 0);
            
            if (structure == null) {
                log.error("ProjectStructure is null");
                return ResponseEntity.badRequest().build();
            }
            
            if (structure.getFiles() == null || structure.getFiles().isEmpty()) {
                log.error("ProjectStructure has no files");
                return ResponseEntity.badRequest().build();
            }
            
            byte[] zipBytes = structureService.createZipFile(structure);
            
            if (zipBytes == null || zipBytes.length == 0) {
                log.error("Generated ZIP file is empty");
                return ResponseEntity.internalServerError().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "project.zip");
            headers.setContentLength(zipBytes.length);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            
            log.info("Sending ZIP file: {} bytes", zipBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating ZIP file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

