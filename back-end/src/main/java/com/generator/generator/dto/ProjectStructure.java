package com.generator.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStructure {
    private List<ProjectFile> files = new ArrayList<>();
    private TreeNode root;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TreeNode {
        private String label;
        private String data; // file path or null for folders
        private String icon;
        private String type; // "file" or "folder"
        private List<TreeNode> children = new ArrayList<>();
        private boolean expanded = true;
    }
}

