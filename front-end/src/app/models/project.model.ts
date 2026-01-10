export interface Project {
  id: number;
  name: string;
  description?: string;
  prompt: string;
  backendCode?: string;
  frontendCode?: string;
  generatedCode?: string;
  projectType?: string;
  userId: number;
  username: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  description?: string;
  prompt: string;
  projectType?: string;
}

