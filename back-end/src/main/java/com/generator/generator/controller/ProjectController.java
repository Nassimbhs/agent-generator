package com.generator.generator.controller;

import com.generator.generator.dto.ProjectRequest;
import com.generator.generator.dto.ProjectResponse;
import com.generator.generator.service.IProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final IProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project", description = "Creates a new CRUD generation project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Project created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        ProjectResponse project = projectService.createProject(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @GetMapping
    @Operation(summary = "Get all projects", description = "Returns all projects for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projects retrieved successfully")
    })
    public ResponseEntity<List<ProjectResponse>> getAllProjects(Authentication authentication) {
        List<ProjectResponse> projects = projectService.getAllProjectsByUser(authentication.getName());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Returns a project by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or access denied")
    })
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            Authentication authentication) {
        ProjectResponse project = projectService.getProjectById(id, authentication.getName());
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project", description = "Updates project information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project updated successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or access denied"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        ProjectResponse project = projectService.updateProject(id, request, authentication.getName());
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Deletes a project by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or access denied")
    })
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            Authentication authentication) {
        projectService.deleteProject(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate")
    @Operation(summary = "Generate all code for project", description = "Generates both Spring Boot CRUD and Angular TypeScript interfaces using Qwen2.5-Coder LLM")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Code generated successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or access denied")
    })
    public ResponseEntity<ProjectResponse> generateCode(
            @PathVariable Long id,
            Authentication authentication) {
        ProjectResponse project = projectService.generateCode(id, authentication.getName());
        return ResponseEntity.ok(project);
    }

    @PostMapping("/{id}/generate/backend")
    @Operation(summary = "Generate Spring Boot CRUD code", description = "Generates Spring Boot CRUD REST API code using Qwen2.5-Coder LLM")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backend code generated successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or access denied")
    })
    public ResponseEntity<ProjectResponse> generateBackendCode(
            @PathVariable Long id,
            Authentication authentication) {
        ProjectResponse project = projectService.generateBackendCode(id, authentication.getName());
        return ResponseEntity.ok(project);
    }

    @PostMapping("/{id}/generate/frontend")
    @Operation(summary = "Generate Angular TypeScript interfaces", description = "Generates Angular TypeScript interface/model files using Qwen2.5-Coder LLM")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Frontend code generated successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or access denied")
    })
    public ResponseEntity<ProjectResponse> generateFrontendCode(
            @PathVariable Long id,
            Authentication authentication) {
        ProjectResponse project = projectService.generateFrontendCode(id, authentication.getName());
        return ResponseEntity.ok(project);
    }
}

