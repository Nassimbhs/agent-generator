package com.generator.generator.service.impl;

import com.generator.generator.dto.ProjectRequest;
import com.generator.generator.dto.ProjectResponse;
import com.generator.generator.entity.Project;
import com.generator.generator.entity.User;
import com.generator.generator.repository.ProjectRepository;
import com.generator.generator.repository.UserRepository;
import com.generator.generator.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService implements IProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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

        // TODO: Integrate with Qwen2.5-Coder LLM to generate code
        // For now, return project with placeholder
        String generatedCode = "// Code generation will be implemented with Qwen2.5-Coder LLM\n" +
                "// Prompt: " + project.getPrompt();

        project.setGeneratedCode(generatedCode);
        Project updatedProject = projectRepository.save(project);

        return mapToProjectResponse(updatedProject);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .prompt(project.getPrompt())
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

