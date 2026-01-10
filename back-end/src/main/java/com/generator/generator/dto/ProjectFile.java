package com.generator.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFile {
    private String path;
    private String name;
    private String content;
    private String language;
    private String type; // "file" or "folder"
}

