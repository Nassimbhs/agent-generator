package com.generator.generator.service;

import com.generator.generator.dto.ProjectRequest;
import com.generator.generator.dto.ProjectResponse;

import java.util.List;

public interface IProjectService {
    ProjectResponse createProject(ProjectRequest request, String username);
    ProjectResponse getProjectById(Long id, String username);
    List<ProjectResponse> getAllProjectsByUser(String username);
    ProjectResponse updateProject(Long id, ProjectRequest request, String username);
    void deleteProject(Long id, String username);
    ProjectResponse generateCode(Long projectId, String username);
    ProjectResponse generateBackendCode(Long projectId, String username);
    ProjectResponse generateFrontendCode(Long projectId, String username);
}

