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
import { MessageService } from 'primeng/api';
import { environment } from '../../../environments/environment';

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
    ToastModule
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
              [class.ng-invalid]="isFieldInvalid('prompt')"
              [disabled]="streaming">
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
              [disabled]="generatorForm.invalid || streamingFrontend"
              class="flex-1">
            </p-button>
            <p-button 
              type="button"
              label="Generate Frontend (Live)" 
              icon="pi pi-desktop"
              severity="secondary"
              [loading]="streamingFrontend"
              [disabled]="generatorForm.invalid || streamingBackend"
              (onClick)="onGenerateFrontend()"
              class="flex-1">
            </p-button>
            <p-button 
              type="button"
              label="Generate All (Live)" 
              icon="pi pi-code"
              severity="info"
              [loading]="streamingBackend || streamingFrontend"
              [disabled]="generatorForm.invalid || streamingBackend || streamingFrontend"
              (onClick)="onGenerateAll()"
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

        <!-- Live Code Display -->
        @if (streamingBackend || streamingFrontend || backendCode || frontendCode) {
          <div class="mt-4">
            <p-tabView>
              @if (streamingBackend || backendCode) {
                <p-tabPanel header="Backend Code (Live)">
                  <div class="relative">
                    @if (streamingBackend) {
                      <div class="absolute top-0 right-0 z-1 p-2">
                        <span class="text-xs bg-primary text-white border-round px-2 py-1">
                          <i class="pi pi-circle-fill mr-1" style="animation: blink 1s infinite;"></i>
                          LIVE
                        </span>
                      </div>
                    }
                    <div class="flex justify-content-end mb-2">
                      <p-button 
                        label="Copy" 
                        icon="pi pi-copy"
                        size="small"
                        [text]="true"
                        [disabled]="streamingBackend"
                        (onClick)="copyToClipboard(backendCode)">
                      </p-button>
                    </div>
                    <pre class="p-4 m-0 bg-gray-900 text-green-400 border-round text-sm line-height-3 overflow-auto font-mono" 
                         style="max-height: 600px; white-space: pre-wrap; word-wrap: break-word; font-family: 'Courier New', monospace;"
                         id="backend-code">{{ backendCode }}@if (streamingBackend) {<span class="text-white animate-blink">|</span>}</pre>
                  </div>
                </p-tabPanel>
              }

              @if (streamingFrontend || frontendCode) {
                <p-tabPanel header="Frontend Code (Live)">
                  <div class="relative">
                    @if (streamingFrontend) {
                      <div class="absolute top-0 right-0 z-1 p-2">
                        <span class="text-xs bg-primary text-white border-round px-2 py-1">
                          <i class="pi pi-circle-fill mr-1" style="animation: blink 1s infinite;"></i>
                          LIVE
                        </span>
                      </div>
                    }
                    <div class="flex justify-content-end mb-2">
                      <p-button 
                        label="Copy" 
                        icon="pi pi-copy"
                        size="small"
                        [text]="true"
                        [disabled]="streamingFrontend"
                        (onClick)="copyToClipboard(frontendCode)">
                      </p-button>
                    </div>
                    <pre class="p-4 m-0 bg-gray-900 text-blue-400 border-round text-sm line-height-3 overflow-auto font-mono" 
                         style="max-height: 600px; white-space: pre-wrap; word-wrap: break-word; font-family: 'Courier New', monospace;"
                         id="frontend-code">{{ frontendCode }}@if (streamingFrontend) {<span class="text-white animate-blink">|</span>}</pre>
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
  private progressInterval: any;
  private eventSource: EventSource | null = null;

  constructor(
    private fb: FormBuilder,
    private messageService: MessageService
  ) {
    this.generatorForm = this.fb.group({
      prompt: ['', Validators.required]
    });
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
    this.startProgressTimer();

    const prompt = this.generatorForm.get('prompt')?.value;
    this.startBackendStreaming(prompt);
  }

  onGenerateFrontend(): void {
    if (this.generatorForm.invalid) return;

    this.streamingFrontend = true;
    this.frontendCode = '';
    this.startProgressTimer();

    const prompt = this.generatorForm.get('prompt')?.value;
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
    const token = localStorage.getItem('token');
    const url = `${environment.apiUrl}/api/generate/backend/stream?token=${encodeURIComponent(token || '')}&prompt=${encodeURIComponent(prompt)}`;
    
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('code-chunk', (event: MessageEvent) => {
      this.backendCode += event.data;
      this.scrollToBottom('backend-code');
    });

    this.eventSource.addEventListener('complete', () => {
      this.eventSource?.close();
      this.streamingBackend = false;
      this.stopProgressTimer();
      this.messageService.add({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Backend code generated successfully!' 
      });
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
      this.backendCode += event.data;
      this.scrollToBottom('backend-code');
    };

    this.eventSource.onerror = () => {
      this.eventSource?.close();
      this.streamingBackend = false;
      this.stopProgressTimer();
      this.messageService.add({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Streaming connection failed. Please try again.' 
      });
    };
  }

  private startFrontendStreaming(prompt: string): void {
    const token = localStorage.getItem('token');
    const url = `${environment.apiUrl}/api/generate/frontend/stream?token=${encodeURIComponent(token || '')}&prompt=${encodeURIComponent(prompt)}`;
    
    const frontendEventSource = new EventSource(url);

    frontendEventSource.addEventListener('code-chunk', (event: MessageEvent) => {
      this.frontendCode += event.data;
      this.scrollToBottom('frontend-code');
    });

    frontendEventSource.addEventListener('complete', () => {
      frontendEventSource.close();
      this.streamingFrontend = false;
      this.stopProgressTimer();
      this.messageService.add({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Frontend code generated successfully!' 
      });
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
      this.frontendCode += event.data;
      this.scrollToBottom('frontend-code');
    };

    frontendEventSource.onerror = () => {
      frontendEventSource.close();
      this.streamingFrontend = false;
      this.stopProgressTimer();
      this.messageService.add({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Streaming connection failed. Please try again.' 
      });
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
}

