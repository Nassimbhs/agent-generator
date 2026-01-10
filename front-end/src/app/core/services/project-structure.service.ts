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
    // Ensure structure has required fields
    if (!structure || !structure.files || structure.files.length === 0) {
      throw new Error('Project structure is empty');
    }
    
    console.log('Sending download request with', structure.files.length, 'files');
    
    // Only send the files array to avoid circular reference issues
    // The tree structure has circular references (children -> parent -> children)
    const payload = {
      files: structure.files.map(file => ({
        path: file.path,
        name: file.name,
        content: file.content,
        language: file.language,
        type: file.type
      }))
    };
    
    console.log('Serialized payload:', payload);
    
    return this.http.post(`${this.apiUrl}/download`, payload, {
      responseType: 'blob',
      headers: {
        'Content-Type': 'application/json'
      }
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
    if (!code || code.trim().length === 0) {
      return null;
    }
    
    // Parse FILE: patterns from generated code
    // Pattern matches: FILE: path/to/file.ext\n```language\ncontent\n```
    // or FILE: path/to/file.ext\ncontent (without code blocks)
    // Improved pattern to correctly capture file paths
    // FILE: followed by path (non-greedy until newline), then optional code block
    const filePattern = /FILE:\s*([^\n\r]+?)(?:\s*\r?\n)(?:```(\w+)?\s*\r?\n)?([\s\S]*?)(?:```\s*(?:\r?\n|$)|(?=\s*FILE:)|$)/gi;
    const files: any[] = [];
    let match;
    let lastIndex = 0;

    while ((match = filePattern.exec(code)) !== null) {
      // Group 1: File path (everything after FILE: until newline)
      let path = match[1]?.trim();
      // Group 2: Language (optional, from ```language)
      const language = match[2]?.trim() || this.detectLanguage(path || '');
      // Group 3: Content (everything between code blocks or until next FILE:)
      let content = (match[3] || '').trim();
      
      // Clean up path - remove any trailing whitespace or invalid characters
      if (path) {
        path = path.replace(/[^\w\s\/\\\.\-_]/g, '').trim();
      }
      
      // Extract filename from path
      const name = path ? (path.split('/').pop() || path.split('\\').pop() || path) : 'unknown';

      // Only add if we have a valid path and some content
      if (path && path.length > 0 && content.length > 0) {
        files.push({
          path: path.replace(/\\/g, '/'), // Normalize path separators
          name: name || path, // Use path as fallback if name extraction fails
          content,
          language,
          type: 'file'
        });
        console.log('Parsed file:', path, 'Name:', name, 'Language:', language, 'Content length:', content.length);
      } else {
        console.warn('Skipping invalid file entry. Path:', path, 'Content length:', content.length);
      }
      lastIndex = match.index + match[0].length;
    }
    
    // If no FILE: patterns found but we have code, treat entire code as single file
    if (files.length === 0 && code.trim().length > 50) {
      console.log('No FILE: patterns found, treating entire code as single file');
      files.push({
        path: 'generated-code.txt',
        name: 'generated-code.txt',
        content: code.trim(),
        language: 'text',
        type: 'file'
      });
    }

    if (files.length === 0) {
      console.log('No files parsed from code');
      return null;
    }
    
    console.log('Parsed', files.length, 'files from code');

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
      // Ensure we have a valid path
      if (!file.path || file.path.trim().length === 0) {
        console.warn('Skipping file with empty path:', file);
        continue;
      }
      
      const parts = file.path.split('/').filter(p => p && p.trim().length > 0);
      
      // If no parts, skip this file
      if (parts.length === 0) {
        console.warn('Skipping file with no valid path parts:', file);
        continue;
      }
      
      let currentPath = '';
      let parent = root;

      // Create folder nodes (all parts except the last one)
      for (let i = 0; i < parts.length - 1; i++) {
        if (i > 0) currentPath += '/';
        currentPath += parts[i];
        const folderPath = currentPath;

        if (!folderMap.has(folderPath)) {
          const folder: any = {
            label: parts[i], // Use the folder name, not just "s"
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

      // Create file node - use the actual file name with extension
      const fileName = parts[parts.length - 1] || file.name || 'unknown';
      const fileNode: any = {
        label: fileName, // Full filename with extension
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
      
      console.log('Added file node:', fileName, 'to path:', file.path);
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

