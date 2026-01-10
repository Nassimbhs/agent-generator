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
    // Expected format:
    // FILE: path/to/file.ext
    // ```language
    // content here
    // ```
    // 
    // OR (during streaming, without closing backticks):
    // FILE: path/to/file.ext
    // ```language
    // content here (incomplete)
    
    const files: any[] = [];
    
    // More precise pattern that matches:
    // FILE: path (on same line, until newline - MUST end at newline, not at ```)
    // Then optional ```language on new line
    // Then content until closing ``` or next FILE:
    // CRITICAL: Path must end with newline, and ``` must be on new line
    // Pattern: FILE: path\n```language\ncontent\n```
    const filePattern = /(?:^|\r?\n)\s*FILE:\s*([^\n\r```]+?)\s*(?:\r?\n)\s*(?:```(\w+)?\s*\r?\n)?([\s\S]*?)(?:```(?:\s*\r?\n|$)|(?=(?:^|\r?\n)\s*FILE:)|$)/gim;
    
    let match;
    let lastMatchIndex = 0;

    while ((match = filePattern.exec(code)) !== null) {
      // Group 1: File path (everything after FILE: until newline, must not contain ```)
      let path = (match[1] || '').trim();
      
      // CRITICAL: Remove any code block markers that might have leaked into path
      path = path.replace(/```/g, '').trim();
      
      // Remove any invalid characters from path (but keep valid path chars)
      // Path should only contain: letters, numbers, dots, slashes, hyphens, underscores, spaces
      path = path.replace(/[<>:"|?*\x00-\x1f```]/g, '').trim();
      
      // Skip if path contains suspicious patterns that suggest it captured content
      if (path.includes('xmlns') || path.includes('package') || path.includes('import') || 
          path.length > 200 || path.match(/\n|\r/)) {
        console.warn('Skipping invalid path (likely captured content):', path.substring(0, 50));
        continue;
      }
      
      // Group 2: Language (optional, from ```language)
      let language = (match[2] || '').trim();
      
      // Group 3: Content (everything after language marker)
      let content = (match[3] || '').trim();
      
      // Remove closing backticks if present in content
      content = content.replace(/```\s*$/gm, '').trim();
      
      // If no language was detected from code block, detect from file extension
      if (!language && path) {
        language = this.detectLanguage(path);
      }
      
      // Extract filename from path (last segment)
      const pathParts = path.split('/').filter((p: string) => p && p.trim().length > 0 && !p.includes('```'));
      const name = pathParts.length > 0 ? pathParts[pathParts.length - 1] : path || 'unknown';
      
      // Clean name - remove any code block markers
      const cleanName = name.replace(/```/g, '').trim();

      // Only add if we have a valid path that looks like a file path
      if (path && path.length > 0 && path.length < 500 && 
          (path.includes('/') || path.includes('.') || path.match(/^[\w\.-]+$/))) {
        
        const normalizedPath = path.replace(/\\/g, '/');
        
        // Check if this file already exists (during streaming, same file might be parsed multiple times)
        const existingIndex = files.findIndex(f => f.path === normalizedPath);
        const fileData = {
          path: normalizedPath,
          name: cleanName,
          content: content,
          language: language || 'text',
          type: 'file'
        };
        
        if (existingIndex >= 0) {
          // Update existing file with more complete content (only if new content is longer)
          if (content.length > files[existingIndex].content.length) {
            files[existingIndex] = fileData;
            console.log('Updated file:', normalizedPath, 'Content length:', content.length);
          }
        } else {
          files.push(fileData);
          console.log('Parsed file:', normalizedPath, 'Name:', cleanName, 'Language:', language || 'auto', 'Content length:', content.length);
        }
      } else {
        console.warn('Skipping file with invalid path:', path.substring(0, 100));
      }
      
      lastMatchIndex = match.index + match[0].length;
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
      
      const parts = file.path.split('/').filter((p: string) => p && p.trim().length > 0);
      
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

