package com.generator.generator.service;

import com.generator.generator.dto.ProjectStructure;

public interface IProjectStructureService {
    ProjectStructure buildProjectStructure(String generatedCode);
    byte[] createZipFile(ProjectStructure structure) throws Exception;
}


