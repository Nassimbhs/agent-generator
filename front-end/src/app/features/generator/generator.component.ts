import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { TabViewModule } from 'primeng/tabview';
import { ToastModule } from 'primeng/toast';
import { TreeModule } from 'primeng/tree';
import { MessageService } from 'primeng/api';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ProjectStructure, TreeNode } from '../../models/project-file.model';
import { ProjectStructureService } from '../../core/services/project-structure.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-generator',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    InputTextareaModule,
    ButtonModule,
    ProgressBarModule,
    TagModule,
    TabViewModule,
    ToastModule,
    TreeModule
  ],
  template: `
    <div class="w-full max-w-6xl mx-auto p-4">
      <p-card class="w-full">
        <ng-template pTemplate="header">
          <div class="p-4">
            <h1 class="text-3xl font-bold m-0">CRUD Code Generator</h1>
            <p class="text-color-secondary m-0 mt-2">Enter your requirements and watch the LLM generate code in real-time</p>
          </div>
        </ng-template>

        <form [formGroup]="generatorForm" (ngSubmit)="onGenerate()" class="flex flex-column gap-4">
          <div class="flex flex-column gap-2">
            <label for="prompt" class="font-semibold text-lg">Requirements Prompt *</label>
            <textarea 
              id="prompt"
              pInputTextarea 
              formControlName="prompt"
              placeholder="Example: Create a User entity with fields: id (Long), username (String), email (String), firstName (String), lastName (String), createdAt (LocalDateTime). Include full CRUD operations with Spring Boot and Angular TypeScript interfaces."
              rows="8"
              class="w-full"
              [class.ng-invalid]="isFieldInvalid('prompt')">
            </textarea>
            @if (isFieldInvalid('prompt')) {
              <small class="p-error">Prompt is required</small>
            }
            <small class="text-color-secondary">
              Be specific about entities, fields, relationships, and requirements. The more details you provide, the better the generated code will be.
            </small>
          </div>

          <div class="flex gap-2 flex-wrap">
            <p-button 
              type="submit"
              label="Generate Backend (Live)" 
              icon="pi pi-server"
              [loading]="streamingBackend"
              [disabled]="generatorForm.invalid || streamingFrontend || promptControl?.disabled"
              class="flex-1">
            </p-button>
            <p-button 
              type="button"
              label="Generate Frontend (Live)" 
              icon="pi pi-desktop"
              severity="secondary"
              [loading]="streamingFrontend"
              [disabled]="generatorForm.invalid || streamingBackend || promptControl?.disabled"
              (onClick)="onGenerateFrontend()"
              class="flex-1">
            </p-button>
            <p-button 
              type="button"
              label="Generate All (Live)" 
              icon="pi pi-code"
              severity="info"
              [loading]="streamingBackend || streamingFrontend"
              [disabled]="generatorForm.invalid || streamingBackend || streamingFrontend || promptControl?.disabled"
              (onClick)="onGenerateAll()"
              class="flex-1">
            </p-button>
            <p-button 
              type="button"
              label="Download Backend Template" 
              icon="pi pi-download"
              severity="help"
              [outlined]="true"
              (onClick)="downloadBackendTemplate()"
              class="flex-1">
            </p-button>
            <p-button 
              type="button"
              label="Download Frontend Template" 
              icon="pi pi-download"
              severity="help"
              [outlined]="true"
              (onClick)="downloadFrontendTemplate()"
              class="flex-1">
            </p-button>
          </div>
        </form>

        <!-- Estimated Time Display -->
        @if ((estimatedTime > 0 && (streamingBackend || streamingFrontend)) || elapsedTime > 0) {
          <div class="mt-4 p-4 bg-blue-50 border-round border-left-3 border-blue-500">
            <div class="flex flex-column gap-3">
              <div class="flex align-items-center justify-content-between">
                <div class="flex align-items-center gap-2">
                  <i class="pi pi-clock text-blue-500 text-xl"></i>
                  <span class="font-semibold text-lg">Estimated Time:</span>
                  <p-tag [value]="formatTime(estimatedTime)" severity="info"></p-tag>
                </div>
                @if (streamingBackend || streamingFrontend) {
                  <p-tag value="LIVE" severity="success" icon="pi pi-circle-fill"></p-tag>
                }
              </div>
              @if (streamingBackend || streamingFrontend) {
                <div>
                  <p-progressBar 
                    [value]="progressPercentage" 
                    [showValue]="false"
                    class="mb-2">
                  </p-progressBar>
                  <div class="flex justify-content-between text-sm text-color-secondary">
                    <span class="font-semibold">Progress: {{ progressPercentage.toFixed(0) }}%</span>
                    <span>Elapsed: {{ formatTime(elapsedTime) }} / Estimated: {{ formatTime(estimatedTime) }}</span>
                  </div>
                </div>
              }
            </div>
          </div>
        }

        <!-- Live Code Display with Tree -->
        @if (streamingBackend || streamingFrontend || backendCode || frontendCode) {
          <div class="mt-4">
            <p-tabView>
              @if (streamingBackend || backendCode) {
                <p-tabPanel header="Backend Code (Live)">
                  <div class="grid">
                    <!-- File Tree -->
                    <div class="col-12 lg:col-4 md:col-3 border-right-1 surface-border pr-3 tree-panel">
                      <div class="flex justify-content-between align-items-center mb-2 sticky top-0 bg-white z-1 pb-2">
                        <h4 class="m-0 text-sm font-semibold">Project Structure</h4>
                        @if (backendProjectStructure) {
                          <p-button 
                            label="ZIP" 
                            icon="pi pi-download"
                            size="small"
                            [text]="true"
                            pTooltip="Download as ZIP"
                            tooltipPosition="left"
                            (onClick)="downloadProjectZip(backendProjectStructure)">
                          </p-button>
                        }
                      </div>
                      @if (backendTreeNodes && backendTreeNodes.length > 0) {
                        <div class="tree-container">
                          <p-tree 
                            [value]="backendTreeNodes" 
                            selectionMode="single"
                            [(selection)]="selectedBackendFile"
                            (onNodeSelect)="onBackendFileSelect($event)"
                            styleClass="w-full text-sm project-tree">
                            <ng-template let-node pTemplate="default">
                              <span class="flex align-items-center gap-2 tree-node">
                                <i [class]="node.icon" class="tree-icon"></i>
                                <span class="tree-label">{{ node.label }}</span>
                              </span>
                            </ng-template>
                          </p-tree>
                        </div>
                      } @else {
                        <p class="text-color-secondary text-sm text-center p-4">No files parsed yet</p>
                      }
                    </div>
                    
                    <!-- Code Display -->
                    <div class="col-12 lg:col-8 md:col-9">
                      <div class="relative code-display-container">
                        <div class="flex flex-column md:flex-row justify-content-between align-items-center mb-2 sticky top-0 bg-white z-1 pb-2 gap-2">
                          <div class="flex align-items-center gap-2 flex-wrap">
                            @if (streamingBackend) {
                              <span class="text-xs bg-primary text-white border-round px-2 py-1">
                                <i class="pi pi-circle-fill mr-1" style="animation: blink 1s infinite;"></i>
                                LIVE
                              </span>
                            }
                            @if (selectedBackendFile) {
                              <span class="text-sm font-semibold text-color-secondary">FILE: {{ selectedBackendFile.data || 'generated-code.txt' }}</span>
                            } @else if (backendCode) {
                              <span class="text-sm font-semibold text-color-secondary">Generated Code ({{ backendCode.length }} chars)</span>
                            }
                          </div>
                          <p-button 
                            label="Copy" 
                            icon="pi pi-copy"
                            size="small"
                            [text]="true"
                            [disabled]="streamingBackend || (!selectedBackendFileContent && !backendCode)"
                            (onClick)="copyToClipboard(selectedBackendFileContent || backendCode)">
                          </p-button>
                        </div>
                        <div class="code-viewer">
                          <pre class="p-4 m-0 bg-gray-900 text-green-400 border-round text-sm line-height-3 font-mono code-content" 
                               style="white-space: pre-wrap; word-wrap: break-word; font-family: 'Courier New', monospace; min-height: 200px;"
                               id="backend-code">{{ selectedBackendFileContent || backendCode }}@if (streamingBackend) {<span class="text-white animate-blink">|</span>}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </p-tabPanel>
              }

              @if (streamingFrontend || frontendCode) {
                <p-tabPanel header="Frontend Code (Live)">
                  <div class="grid">
                    <!-- File Tree -->
                    <div class="col-12 lg:col-4 md:col-3 border-right-1 surface-border pr-3 tree-panel">
                      <div class="flex justify-content-between align-items-center mb-2 sticky top-0 bg-white z-1 pb-2">
                        <h4 class="m-0 text-sm font-semibold">Project Structure</h4>
                        @if (frontendProjectStructure) {
                          <p-button 
                            label="ZIP" 
                            icon="pi pi-download"
                            size="small"
                            [text]="true"
                            pTooltip="Download as ZIP"
                            tooltipPosition="left"
                            (onClick)="downloadProjectZip(frontendProjectStructure)">
                          </p-button>
                        }
                      </div>
                      @if (frontendTreeNodes && frontendTreeNodes.length > 0) {
                        <div class="tree-container">
                          <p-tree 
                            [value]="frontendTreeNodes" 
                            selectionMode="single"
                            [(selection)]="selectedFrontendFile"
                            (onNodeSelect)="onFrontendFileSelect($event)"
                            styleClass="w-full text-sm project-tree">
                            <ng-template let-node pTemplate="default">
                              <span class="flex align-items-center gap-2 tree-node">
                                <i [class]="node.icon" class="tree-icon"></i>
                                <span class="tree-label">{{ node.label }}</span>
                              </span>
                            </ng-template>
                          </p-tree>
                        </div>
                      } @else {
                        <p class="text-color-secondary text-sm text-center p-4">No files parsed yet</p>
                      }
                    </div>
                    
                    <!-- Code Display -->
                    <div class="col-12 lg:col-8 md:col-9">
                      <div class="relative code-display-container">
                        <div class="flex flex-column md:flex-row justify-content-between align-items-center mb-2 sticky top-0 bg-white z-1 pb-2 gap-2">
                          <div class="flex align-items-center gap-2 flex-wrap">
                            @if (streamingFrontend) {
                              <span class="text-xs bg-primary text-white border-round px-2 py-1">
                                <i class="pi pi-circle-fill mr-1" style="animation: blink 1s infinite;"></i>
                                LIVE
                              </span>
                            }
                            @if (selectedFrontendFile) {
                              <span class="text-sm font-semibold text-color-secondary">FILE: {{ selectedFrontendFile.data || 'generated-code.txt' }}</span>
                            } @else if (frontendCode) {
                              <span class="text-sm font-semibold text-color-secondary">Generated Code ({{ frontendCode.length }} chars)</span>
                            }
                          </div>
                          <p-button 
                            label="Copy" 
                            icon="pi pi-copy"
                            size="small"
                            [text]="true"
                            [disabled]="streamingFrontend || (!selectedFrontendFileContent && !frontendCode)"
                            (onClick)="copyToClipboard(selectedFrontendFileContent || frontendCode)">
                          </p-button>
                        </div>
                        <div class="code-viewer">
                          <pre class="p-4 m-0 bg-gray-900 text-blue-400 border-round text-sm line-height-3 font-mono code-content" 
                               style="white-space: pre-wrap; word-wrap: break-word; font-family: 'Courier New', monospace; min-height: 200px;"
                               id="frontend-code">{{ selectedFrontendFileContent || frontendCode }}@if (streamingFrontend) {<span class="text-white animate-blink">|</span>}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </p-tabPanel>
              }
            </p-tabView>
          </div>
        }
      </p-card>
    </div>

    <p-toast></p-toast>
  `,
  styles: [`
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50% { opacity: 0; }
    }
    .animate-blink {
      animation: blink 1s infinite;
    }
    
    /* Responsive Tree Panel */
    .tree-panel {
      max-height: 600px;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    
    .tree-container {
      flex: 1;
      overflow-y: auto;
      overflow-x: hidden;
      max-height: calc(600px - 60px);
      scrollbar-width: thin;
      scrollbar-color: #cbd5e0 #f7fafc;
    }
    
    .tree-container::-webkit-scrollbar {
      width: 6px;
    }
    
    .tree-container::-webkit-scrollbar-track {
      background: #f7fafc;
      border-radius: 3px;
    }
    
    .tree-container::-webkit-scrollbar-thumb {
      background: #cbd5e0;
      border-radius: 3px;
    }
    
    .tree-container::-webkit-scrollbar-thumb:hover {
      background: #a0aec0;
    }
    
    .project-tree ::ng-deep .p-tree {
      font-size: 0.875rem;
    }
    
    .project-tree ::ng-deep .p-tree .p-treenode {
      padding: 0.25rem 0;
    }
    
    .project-tree ::ng-deep .p-tree .p-treenode-content {
      padding: 0.375rem 0.5rem;
      border-radius: 4px;
      transition: background-color 0.2s;
      cursor: pointer;
    }
    
    .project-tree ::ng-deep .p-tree .p-treenode-content:hover {
      background-color: #f3f4f6;
    }
    
    .project-tree ::ng-deep .p-tree .p-treenode-content.p-highlight {
      background-color: #dbeafe;
      color: #1e40af;
    }
    
    .tree-node {
      width: 100%;
      overflow: hidden;
      min-width: 0;
    }
    
    .tree-icon {
      font-size: 0.875rem;
      min-width: 1rem;
      flex-shrink: 0;
    }
    
    .tree-label {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      min-width: 0;
    }
    
    /* Responsive Code Display */
    .code-display-container {
      height: 100%;
      display: flex;
      flex-direction: column;
      max-height: 600px;
    }
    
    .code-viewer {
      flex: 1;
      overflow-y: auto;
      overflow-x: auto;
      max-height: calc(600px - 80px);
      scrollbar-width: thin;
      scrollbar-color: #4a5568 #2d3748;
    }
    
    .code-viewer::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }
    
    .code-viewer::-webkit-scrollbar-track {
      background: #2d3748;
      border-radius: 4px;
    }
    
    .code-viewer::-webkit-scrollbar-thumb {
      background: #4a5568;
      border-radius: 4px;
    }
    
    .code-viewer::-webkit-scrollbar-thumb:hover {
      background: #718096;
    }
    
    .code-content {
      margin: 0;
      padding: 1rem;
    }
    
    /* Responsive Breakpoints */
    @media (max-width: 1024px) {
      .tree-panel {
        max-height: 400px;
      }
      
      .tree-container {
        max-height: calc(400px - 60px);
      }
      
      .code-display-container {
        max-height: 400px;
      }
      
      .code-viewer {
        max-height: calc(400px - 80px);
      }
    }
    
    @media (max-width: 768px) {
      .tree-panel {
        max-height: 300px;
        border-right: none;
        border-bottom: 1px solid #e5e7eb;
        margin-bottom: 1rem;
        padding-bottom: 1rem;
      }
      
      .tree-container {
        max-height: calc(300px - 60px);
      }
      
      .code-display-container .sticky {
        position: relative;
      }
      
      .code-display-container {
        max-height: 400px;
      }
      
      .code-viewer {
        max-height: calc(400px - 80px);
      }
      
      .code-content {
        font-size: 0.75rem;
        padding: 0.75rem;
      }
      
      .tree-label {
        font-size: 0.8125rem;
      }
    }
    
    @media (max-width: 480px) {
      .tree-panel {
        max-height: 250px;
      }
      
      .tree-container {
        max-height: calc(250px - 60px);
      }
      
      .code-content {
        font-size: 0.7rem;
        padding: 0.5rem;
        line-height: 1.5;
      }
    }
  `]
})
export class GeneratorComponent implements OnDestroy {
  generatorForm: FormGroup;
  streamingBackend = false;
  streamingFrontend = false;
  backendCode = '';
  frontendCode = '';
  estimatedTime = 0;
  elapsedTime = 0;
  progressPercentage = 0;
  
  // Project structure for tree display
  backendProjectStructure: ProjectStructure | null = null;
  frontendProjectStructure: ProjectStructure | null = null;
  backendTreeNodes: TreeNode[] = [];
  frontendTreeNodes: TreeNode[] = [];
  selectedBackendFile: TreeNode | null = null;
  selectedFrontendFile: TreeNode | null = null;
  selectedBackendFileContent = '';
  selectedFrontendFileContent = '';

  private progressInterval: any;
  private eventSource: EventSource | null = null;

  constructor(
    private fb: FormBuilder,
    private messageService: MessageService,
    private projectStructureService: ProjectStructureService,
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.generatorForm = this.fb.group({
      prompt: [{ value: '', disabled: false }, Validators.required]
    });
  }

  get promptControl() {
    return this.generatorForm.get('prompt');
  }

  ngOnDestroy(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
  }

  get streaming(): boolean {
    return this.streamingBackend || this.streamingFrontend;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.generatorForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onGenerate(): void {
    this.onGenerateBackend();
  }

  onGenerateBackend(): void {
    if (this.generatorForm.invalid) return;

    this.streamingBackend = true;
    this.backendCode = '';
    this.promptControl?.disable();
    this.startProgressTimer();

    const prompt = this.promptControl?.value;
    this.startBackendStreaming(prompt);
  }

  onGenerateFrontend(): void {
    if (this.generatorForm.invalid) return;

    this.streamingFrontend = true;
    this.frontendCode = '';
    this.promptControl?.disable();
    this.startProgressTimer();

    const prompt = this.promptControl?.value;
    this.startFrontendStreaming(prompt);
  }

  onGenerateAll(): void {
    this.onGenerateBackend();
    // Wait a bit then start frontend
    setTimeout(() => {
      if (!this.streamingBackend) {
        this.onGenerateFrontend();
      } else {
        // Wait for backend to complete
        const checkInterval = setInterval(() => {
          if (!this.streamingBackend) {
            clearInterval(checkInterval);
            this.onGenerateFrontend();
          }
        }, 1000);
      }
    }, 500);
  }

  private startBackendStreaming(prompt: string): void {
    const token = this.authService.getToken();
    if (!token) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Please login first to generate code'
      });
      this.streamingBackend = false;
      this.promptControl?.enable();
      return;
    }
    
    const url = `${environment.apiUrl}/api/generate/backend/stream?token=${encodeURIComponent(token)}&prompt=${encodeURIComponent(prompt)}`;
    
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('code-chunk', (event: MessageEvent) => {
      console.log('Received code-chunk event, data length:', event.data?.length);
      if (event.data) {
        this.backendCode += event.data;
        console.log('Total backend code length now:', this.backendCode.length);
        this.updateBackendProjectStructure();
        this.scrollToBottom('backend-code');
      }
    });

    let completed = false;
    
    this.eventSource.addEventListener('complete', () => {
      if (completed) return; // Prevent duplicate calls
      completed = true;
      
      console.log('Received complete event. Total code length:', this.backendCode.length);
      this.streamingBackend = false;
      this.promptControl?.enable();
      this.stopProgressTimer();
      this.updateBackendProjectStructure();
      
      // Wait a bit before closing to ensure all chunks are processed
      setTimeout(() => {
        if (this.eventSource) {
          this.eventSource.close();
        }
        if (this.backendCode.length > 0) {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Success', 
            detail: `Backend code generated successfully! (${this.backendCode.length} characters)` 
          });
        } else {
          this.messageService.add({ 
            severity: 'warn', 
            summary: 'Warning', 
            detail: 'Stream completed but no code was received.' 
          });
        }
      }, 500);
    });

    this.eventSource.addEventListener('error', (event: MessageEvent) => {
      if (event.data) {
        this.messageService.add({ 
          severity: 'error', 
          summary: 'Error', 
          detail: event.data 
        });
      }
    });

    this.eventSource.onmessage = (event) => {
      console.log('Received SSE message (no event name):', event.data?.substring(0, 100));
      // Fallback for messages without event name
      if (event.data && !event.data.includes('complete') && !event.data.includes('error')) {
        this.backendCode += event.data;
        this.updateBackendProjectStructure();
        this.scrollToBottom('backend-code');
      }
    };

    this.eventSource.onerror = (error: any) => {
      console.error('EventSource error:', error, 'ReadyState:', this.eventSource?.readyState);
      
      // EventSource.onerror fires multiple times during connection lifecycle
      // CONNECTING (0) = initial connection
      // OPEN (1) = connected
      // CLOSED (2) = closed
      
      if (this.eventSource) {
        const readyState = this.eventSource.readyState;
        
        if (readyState === EventSource.CLOSED && this.streamingBackend && !completed) {
          // Connection closed unexpectedly
          if (this.backendCode.length === 0) {
            // No data received - real error
            this.streamingBackend = false;
            this.promptControl?.enable();
            this.stopProgressTimer();
            this.eventSource.close();
            this.messageService.add({ 
              severity: 'error', 
              summary: 'Connection Error', 
              detail: 'Connection closed unexpectedly. Please check if the backend is running and try again.' 
            });
          } else {
            // We have some data - might be normal completion
            console.log('Connection closed but we have data:', this.backendCode.length, 'chars');
            // Don't show error, let the complete handler take care of it
          }
        } else if (readyState === EventSource.CONNECTING) {
          // Still connecting, wait
          console.log('EventSource still connecting...');
        } else if (readyState === EventSource.OPEN) {
          // Connection open, error might be temporary
          console.log('EventSource connection open, temporary error?');
        }
      }
    };
  }

  private startFrontendStreaming(prompt: string): void {
    const token = this.authService.getToken();
    if (!token) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Please login first to generate code'
      });
      this.streamingFrontend = false;
      this.promptControl?.enable();
      return;
    }
    
    const url = `${environment.apiUrl}/api/generate/frontend/stream?token=${encodeURIComponent(token)}&prompt=${encodeURIComponent(prompt)}`;
    
    const frontendEventSource = new EventSource(url);

    frontendEventSource.addEventListener('code-chunk', (event: MessageEvent) => {
      console.log('Received code-chunk event, data length:', event.data?.length);
      if (event.data) {
        this.frontendCode += event.data;
        console.log('Total frontend code length now:', this.frontendCode.length);
        this.updateFrontendProjectStructure();
        this.scrollToBottom('frontend-code');
      }
    });

    let frontendCompleted = false;
    
    frontendEventSource.addEventListener('complete', () => {
      if (frontendCompleted) return;
      frontendCompleted = true;
      
      console.log('Received complete event. Total code length:', this.frontendCode.length);
      this.streamingFrontend = false;
      this.promptControl?.enable();
      this.stopProgressTimer();
      this.updateFrontendProjectStructure();
      
      setTimeout(() => {
        frontendEventSource.close();
        if (this.frontendCode.length > 0) {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Success', 
            detail: `Frontend code generated successfully! (${this.frontendCode.length} characters)`
          });
        } else {
          this.messageService.add({ 
            severity: 'warn', 
            summary: 'Warning', 
            detail: 'Stream completed but no code was received.' 
          });
        }
      }, 500);
    });

    frontendEventSource.addEventListener('error', (event: MessageEvent) => {
      if (event.data) {
        this.messageService.add({ 
          severity: 'error', 
          summary: 'Error', 
          detail: event.data 
        });
      }
    });

    frontendEventSource.onmessage = (event) => {
      console.log('Received SSE message (no event name):', event.data?.substring(0, 100));
      // Fallback for messages without event name
      if (event.data && !event.data.includes('complete') && !event.data.includes('error')) {
        this.frontendCode += event.data;
        this.updateFrontendProjectStructure();
        this.scrollToBottom('frontend-code');
      }
    };

    frontendEventSource.onerror = (error) => {
      console.error('EventSource error:', error);
      // EventSource.onerror fires when connection is closed, even on success
      // Only treat as error if readyState is CLOSED (2) and we didn't receive complete event
      if (frontendEventSource.readyState === EventSource.CLOSED) {
        // Check if we received any data
        if (this.frontendCode.length === 0 && this.streamingFrontend) {
          frontendEventSource.close();
          this.streamingFrontend = false;
          this.promptControl?.enable();
          this.stopProgressTimer();
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Connection Error', 
            detail: 'Failed to connect to server. Please check if the backend is running and you are logged in.' 
          });
        }
      }
    };
  }

  private startProgressTimer(): void {
    // Estimate time based on prompt length (rough estimate: 30 seconds to 5 minutes)
    const promptLength = this.generatorForm.get('prompt')?.value?.length || 0;
    // Base time: 30 seconds, add 0.5 seconds per character (max 5 minutes)
    this.estimatedTime = Math.max(30, Math.min(300, 30 + Math.floor(promptLength * 0.5)));
    this.elapsedTime = 0;
    this.progressPercentage = 0;

    this.progressInterval = setInterval(() => {
      this.elapsedTime++;
      if (this.estimatedTime > 0) {
        this.progressPercentage = Math.min(95, (this.elapsedTime / this.estimatedTime) * 100);
      }
      
      // If exceeded estimated time, slowly increase estimate
      if (this.elapsedTime >= this.estimatedTime) {
        this.estimatedTime = Math.floor(this.elapsedTime * 1.2); // Increase estimate by 20%
        this.progressPercentage = Math.min(95, (this.elapsedTime / this.estimatedTime) * 100);
      }
    }, 1000);
  }

  formatTime(seconds: number): string {
    if (seconds < 60) {
      return `${seconds}s`;
    }
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return secs > 0 ? `${mins}m ${secs}s` : `${mins}m`;
  }

  private stopProgressTimer(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressPercentage = 100;
    }
  }

  private scrollToBottom(elementId: string): void {
    setTimeout(() => {
      const element = document.getElementById(elementId);
      if (element) {
        element.scrollTop = element.scrollHeight;
      }
    }, 100);
  }

  copyToClipboard(text: string): void {
    if (text) {
      navigator.clipboard.writeText(text).then(() => {
        this.messageService.add({ 
          severity: 'success', 
          summary: 'Copied', 
          detail: 'Code copied to clipboard' 
        });
      });
    }
  }

  private updateBackendProjectStructure(): void {
    if (this.backendCode) {
      const structure = this.projectStructureService.parseFilesFromCode(this.backendCode);
      if (structure && structure.root) {
        this.backendProjectStructure = structure;
        this.backendTreeNodes = [structure.root];
        
        // Expand all folders for PrimeNG tree
        if (structure.root) {
          this.expandAllFolders(structure.root);
        }
        
        // Auto-select first file if none selected
        if (!this.selectedBackendFile && structure.files.length > 0) {
          const firstFile = this.findFirstFile(structure.root);
          if (firstFile) {
            this.selectedBackendFile = firstFile;
            this.selectedBackendFileContent = firstFile.content || firstFile.file?.content || '';
          }
        }
      }
    }
  }

  private updateFrontendProjectStructure(): void {
    if (this.frontendCode) {
      const structure = this.projectStructureService.parseFilesFromCode(this.frontendCode);
      if (structure && structure.root) {
        this.frontendProjectStructure = structure;
        this.frontendTreeNodes = [structure.root];
        
        // Expand all folders for PrimeNG tree
        if (structure.root) {
          this.expandAllFolders(structure.root);
        }
        
        // Auto-select first file if none selected
        if (!this.selectedFrontendFile && structure.files.length > 0) {
          const firstFile = this.findFirstFile(structure.root);
          if (firstFile) {
            this.selectedFrontendFile = firstFile;
            this.selectedFrontendFileContent = firstFile.content || firstFile.file?.content || '';
          }
        }
      }
    }
  }

  private expandAllFolders(node: any): void {
    if (node.type === 'folder' && node.children) {
      node.expanded = true;
      node.children.forEach((child: any) => this.expandAllFolders(child));
    }
  }

  private findFirstFile(node: any): any {
    if (node.type === 'file') {
      return node;
    }
    if (node.children) {
      for (const child of node.children) {
        const file = this.findFirstFile(child);
        if (file) return file;
      }
    }
    return null;
  }

  onBackendFileSelect(event: any): void {
    const node = event.node;
    if (node && node.type === 'file') {
      this.selectedBackendFile = node;
      this.selectedBackendFileContent = node.content || node.file?.content || '';
      this.scrollToBottom('backend-code');
    }
  }

  onFrontendFileSelect(event: any): void {
    const node = event.node;
    if (node && node.type === 'file') {
      this.selectedFrontendFile = node;
      this.selectedFrontendFileContent = node.content || node.file?.content || '';
      this.scrollToBottom('frontend-code');
    }
  }

  downloadBackendTemplate(): void {
    this.projectStructureService.downloadBackendTemplate().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'spring-boot-template.zip';
        a.click();
        window.URL.revokeObjectURL(url);
        this.messageService.add({
          severity: 'success',
          summary: 'Downloaded',
          detail: 'Backend template downloaded successfully'
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to download backend template'
        });
      }
    });
  }

  downloadFrontendTemplate(): void {
    this.projectStructureService.downloadFrontendTemplate().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'frontend-template.zip';
        a.click();
        window.URL.revokeObjectURL(url);
        this.messageService.add({
          severity: 'success',
          summary: 'Downloaded',
          detail: 'Frontend template downloaded successfully'
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to download frontend template'
        });
      }
    });
  }

  downloadProjectZip(structure: ProjectStructure): void {
    if (!structure || !structure.files || structure.files.length === 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'No files to download. Please generate code first.'
      });
      return;
    }
    
    console.log('Downloading project ZIP with', structure.files.length, 'files');
    
    this.projectStructureService.downloadProject(structure).subscribe({
      next: (blob) => {
        if (blob && blob.size > 0) {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'project.zip';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
          this.messageService.add({
            severity: 'success',
            summary: 'Downloaded',
            detail: `Project ZIP downloaded successfully (${(blob.size / 1024).toFixed(2)} KB)`
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Downloaded file is empty. Please try again.'
          });
        }
      },
      error: (error) => {
        console.error('Download error:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: `Failed to download project ZIP: ${error.message || 'Unknown error'}`
        });
      }
    });
  }
}

