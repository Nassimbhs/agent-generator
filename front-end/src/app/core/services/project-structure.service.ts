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
    // Handles multiple formats:
    // 1. Normal: FILE: path\n```language\ncontent\n```
    // 2. Malformed (no newlines): FILE:path```languagecontent```
    // 3. Concatenated: FILE:path1```...```FILE:path2```...
    
    const files: any[] = [];
    
    // Split code by FILE: markers (case-insensitive)
    // This handles all formats including concatenated ones
    const fileSections = code.split(/(?=FILE:)/gi).filter((s: string) => s.trim().length > 0 && /^FILE:/i.test(s.trim()));
    
    for (const section of fileSections) {
      // Extract path: from FILE: until ``` (or newline then ```)
      // Pattern: FILE: path```language or FILE: path\n```language
      const pathMatch = section.match(/^FILE:\s*([^\n\r```]+?)(?:```|(?:\r?\n)\s*```)/i);
      if (!pathMatch) continue;
      
      let path = (pathMatch[1] || '').trim();
      path = path.replace(/```/g, '').replace(/[<>:"|?*\x00-\x1f]/g, '').trim();
      
      // Skip invalid paths (contain code keywords or too long)
      if (path.includes('package') || path.includes('import') || path.includes('xmlns') ||
          path.includes('public') || path.includes('class') || path.includes('interface') ||
          path.length > 300 || !path.match(/^[\w\/\.\-\s]+$/)) {
        continue;
      }
      
      // Find first ``` after FILE: to extract language and content
      const codeBlockStart = section.indexOf('```');
      if (codeBlockStart === -1) continue;
      
      // Extract language: after ``` until newline or space or next ```
      const afterBackticks = section.substring(codeBlockStart + 3);
      const langMatch = afterBackticks.match(/^(\w+)/);
      const language = langMatch ? langMatch[1].trim() : '';
      
      // Content starts after language marker (skip language + any whitespace/newline)
      let contentStartPos = codeBlockStart + 3 + (langMatch ? langMatch[0].length : 0);
      // Skip whitespace/newlines after language
      while (contentStartPos < section.length && /\s/.test(section[contentStartPos])) {
        contentStartPos++;
      }
      
      // Find content end: closing ``` (look for next ``` that's not at start)
      let contentEnd = section.indexOf('```', contentStartPos + 3);
      // Also check for next FILE: marker
      const nextFile = section.indexOf('FILE:', contentStartPos);
      if (nextFile !== -1 && (contentEnd === -1 || nextFile < contentEnd)) {
        contentEnd = nextFile;
      }
      if (contentEnd === -1) {
        contentEnd = section.length;
      }
      
      let content = section.substring(contentStartPos, contentEnd).trim();
      // Remove trailing ```
      content = content.replace(/```\s*$/gm, '').trim();
      
      // Detect language from extension if not found
      const detectedLang = language || this.detectLanguage(path);
      
      // Extract filename
      const pathParts = path.split('/').filter((p: string) => p && p.trim().length > 0);
      const name = pathParts.length > 0 ? pathParts[pathParts.length - 1] : path || 'unknown';
      
      // Validate and add file
      if (path && path.length > 0 && path.length < 500 && (path.includes('/') || path.includes('.'))) {
        const normalizedPath = path.replace(/\\/g, '/');
        const existingIndex = files.findIndex(f => f.path === normalizedPath);
        
        const fileData = {
          path: normalizedPath,
          name: name.trim(),
          content: content,
          language: detectedLang,
          type: 'file'
        };
        
        if (existingIndex >= 0) {
          if (content.length > files[existingIndex].content.length) {
            files[existingIndex] = fileData;
          }
        } else {
          files.push(fileData);
          console.log('Parsed file:', normalizedPath, 'Name:', name, 'Language:', detectedLang, 'Content length:', content.length);
        }
      }
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

