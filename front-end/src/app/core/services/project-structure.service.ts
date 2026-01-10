import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProjectStructure } from '../../models/project-file.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProjectStructureService {
  private apiUrl = `${environment.apiUrl}/api/project`;

  constructor(private http: HttpClient) {}

  parseProjectStructure(generatedCode: string): Observable<ProjectStructure> {
    return this.http.post<ProjectStructure>(`${this.apiUrl}/structure`, generatedCode);
  }

  downloadProject(structure: ProjectStructure): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/download`, structure, {
      responseType: 'blob'
    });
  }

  downloadBackendTemplate(): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/templates/backend/empty`, {
      responseType: 'blob'
    });
  }

  downloadFrontendTemplate(): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/templates/frontend/empty`, {
      responseType: 'blob'
    });
  }

  parseFilesFromCode(code: string): ProjectStructure | null {
    // Parse FILE: patterns from generated code
    const filePattern = /FILE:\s*(.+?)\s*(?:```(\w+)?\s*)?\n((?:[^`]|`(?!``))*?)(?=```|FILE:|$)/gs;
    const files: any[] = [];
    let match;

    while ((match = filePattern.exec(code)) !== null) {
      const path = match[1].trim();
      const language = match[2]?.trim() || this.detectLanguage(path);
      const content = match[3].trim();
      const name = path.split('/').pop() || path;

      if (path && content) {
        files.push({
          path,
          name,
          content,
          language,
          type: 'file'
        });
      }
    }

    if (files.length === 0) {
      return null;
    }

    // Build tree structure
    const root: any = {
      label: 'Project',
      type: 'folder',
      icon: 'pi pi-folder',
      expanded: true,
      children: [],
      key: 'root' // Unique key for PrimeNG tree
    };

    const folderMap = new Map<string, any>();
    folderMap.set('', root);

    files.sort((a, b) => a.path.localeCompare(b.path));

    for (const file of files) {
      const parts = file.path.split('/');
      let currentPath = '';
      let parent = root;

      // Create folder nodes
      for (let i = 0; i < parts.length - 1; i++) {
        if (i > 0) currentPath += '/';
        currentPath += parts[i];
        const folderPath = currentPath;

        if (!folderMap.has(folderPath)) {
          const folder: any = {
            label: parts[i],
            type: 'folder',
            icon: 'pi pi-folder',
            expanded: true,
            children: [],
            key: folderPath // Unique key for PrimeNG tree
          };
          folderMap.set(folderPath, folder);
          parent.children.push(folder);
        }
        parent = folderMap.get(folderPath)!;
      }

      // Create file node
      const fileName = parts[parts.length - 1];
      const fileNode: any = {
        label: fileName,
        data: file.path,
        type: 'file',
        icon: this.getFileIcon(file.language),
        expanded: false,
        children: undefined,
        file: file,
        content: file.content, // Direct access to content
        language: file.language,
        key: file.path
      };
      parent.children.push(fileNode);
    }

    return {
      files,
      root
    };
  }

  private detectLanguage(path: string): string {
    const ext = path.split('.').pop()?.toLowerCase();
    const langMap: { [key: string]: string } = {
      'html': 'html',
      'css': 'css',
      'js': 'javascript',
      'json': 'json',
      'md': 'markdown',
      'java': 'java',
      'ts': 'typescript',
      'tsx': 'typescript',
      'py': 'python',
      'xml': 'xml',
      'yml': 'yaml',
      'yaml': 'yaml',
      'properties': 'properties'
    };
    return langMap[ext || ''] || 'text';
  }

  private getFileIcon(language: string): string {
    const iconMap: { [key: string]: string } = {
      'html': 'pi pi-code',
      'css': 'pi pi-palette',
      'javascript': 'pi pi-code',
      'typescript': 'pi pi-code',
      'json': 'pi pi-file-edit',
      'java': 'pi pi-file',
      'markdown': 'pi pi-file-edit',
      'python': 'pi pi-file',
      'xml': 'pi pi-file',
      'properties': 'pi pi-cog'
    };
    return iconMap[language.toLowerCase()] || 'pi pi-file';
  }
}

