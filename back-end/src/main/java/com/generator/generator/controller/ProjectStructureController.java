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
            byte[] zipBytes = structureService.createZipFile(structure);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "project.zip");
            headers.setContentLength(zipBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);
        } catch (Exception e) {
            log.error("Error creating ZIP file", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

