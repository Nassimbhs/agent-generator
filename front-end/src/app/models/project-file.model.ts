export interface ProjectFile {
  path: string;
  name: string;
  content: string;
  language: string;
  type: string;
}

export interface TreeNode {
  label: string;
  data?: string;
  icon?: string;
  type?: string;
  expanded?: boolean;
  children?: TreeNode[];
}

export interface ProjectStructure {
  files: ProjectFile[];
  root: TreeNode;
}


