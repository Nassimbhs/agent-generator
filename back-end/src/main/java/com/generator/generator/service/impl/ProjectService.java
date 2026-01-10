package com.generator.generator.service.impl;

import com.generator.generator.dto.ProjectRequest;
import com.generator.generator.dto.ProjectResponse;
import com.generator.generator.entity.Project;
import com.generator.generator.entity.User;
import com.generator.generator.repository.ProjectRepository;
import com.generator.generator.repository.UserRepository;
import com.generator.generator.service.CodeGenerationService;
import com.generator.generator.service.IProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements IProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CodeGenerationService codeGenerationService;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prompt(request.getPrompt())
                .projectType(request.getProjectType())
                .user(user)
                .isActive(true)
                .build();

        Project savedProject = projectRepository.save(project);
        return mapToProjectResponse(savedProject);
    }

    @Override
    public ProjectResponse getProjectById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        return mapToProjectResponse(project);
    }

    @Override
    public List<ProjectResponse> getAllProjectsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setPrompt(request.getPrompt());
        project.setProjectType(request.getProjectType());

        return mapToProjectResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public ProjectResponse generateCode(Long projectId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        try {
            log.info("Starting code generation for project: {}", project.getName());
            
            // Generate Spring Boot CRUD code
            String backendCode = codeGenerationService.generateSpringBootCrud(project.getPrompt());
            project.setBackendCode(backendCode);
            log.info("Backend code generated successfully");
            
            // Generate Angular TypeScript interfaces
            String frontendCode = codeGenerationService.generateAngularInterfaces(project.getPrompt());
            project.setFrontendCode(frontendCode);
            log.info("Frontend code generated successfully");
            
            // Keep legacy field for compatibility
            project.setGeneratedCode(backendCode + "\n\n// === FRONTEND CODE ===\n\n" + frontendCode);
            
            Project updatedProject = projectRepository.save(project);
            log.info("Code generation completed for project: {}", project.getName());
            
            return mapToProjectResponse(updatedProject);
        } catch (Exception e) {
            log.error("Error generating code for project {}: {}", project.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate code: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ProjectResponse generateBackendCode(Long projectId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        try {
            log.info("Generating Spring Boot CRUD code for project: {}", project.getName());
            String backendCode = codeGenerationService.generateSpringBootCrud(project.getPrompt());
            project.setBackendCode(backendCode);
            project.setGeneratedCode(backendCode); // Update legacy field
            
            Project updatedProject = projectRepository.save(project);
            log.info("Backend code generated successfully for project: {}", project.getName());
            
            return mapToProjectResponse(updatedProject);
        } catch (Exception e) {
            log.error("Error generating backend code for project {}: {}", project.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate backend code: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ProjectResponse generateFrontendCode(Long projectId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        try {
            log.info("Generating Angular TypeScript interfaces for project: {}", project.getName());
            String frontendCode = codeGenerationService.generateAngularInterfaces(project.getPrompt());
            project.setFrontendCode(frontendCode);
            
            Project updatedProject = projectRepository.save(project);
            log.info("Frontend code generated successfully for project: {}", project.getName());
            
            return mapToProjectResponse(updatedProject);
        } catch (Exception e) {
            log.error("Error generating frontend code for project {}: {}", project.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate frontend code: " + e.getMessage(), e);
        }
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .prompt(project.getPrompt())
                .backendCode(project.getBackendCode())
                .frontendCode(project.getFrontendCode())
                .generatedCode(project.getGeneratedCode())
                .projectType(project.getProjectType())
                .userId(project.getUser().getId())
                .username(project.getUser().getUsername())
                .isActive(project.getIsActive())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}

