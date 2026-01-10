import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ProjectService } from '../../../core/services/project.service';
import { Project } from '../../../models/project.model';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    ButtonModule,
    TabViewModule,
    TagModule,
    MessageModule,
    ConfirmDialogModule,
    ToastModule
  ],
  providers: [ConfirmationService, MessageService],
  template: `
    <div class="w-full">
      @if (loading) {
        <div class="flex justify-content-center p-6">
          <i class="pi pi-spin pi-spinner text-4xl text-primary"></i>
        </div>
      } @else if (project) {
        <!-- Project Header -->
        <div class="flex flex-column md:flex-row justify-content-between align-items-start md:align-items-center gap-3 mb-4">
          <div class="flex-1">
            <h1 class="text-3xl font-bold m-0 mb-2">{{ project.name }}</h1>
            @if (project.description) {
              <p class="text-color-secondary m-0 mb-2">{{ project.description }}</p>
            }
            <div class="flex flex-wrap gap-2 align-items-center">
              <p-tag [value]="project.projectType || 'N/A'" severity="info"></p-tag>
              <span class="text-sm text-color-secondary">
                <i class="pi pi-calendar mr-1"></i>
                Created: {{ formatDate(project.createdAt) }}
              </span>
            </div>
          </div>
          <div class="flex gap-2">
            <p-button 
              label="Edit" 
              icon="pi pi-pencil"
              [outlined]="true"
              (onClick)="editProject()">
            </p-button>
            <p-button 
              label="Delete" 
              icon="pi pi-trash"
              severity="danger"
              [outlined]="true"
              (onClick)="confirmDelete()">
            </p-button>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex flex-wrap gap-2 mb-4">
          <p-button 
            label="Generate All Code" 
            icon="pi pi-code"
            (onClick)="generateCode()"
            [loading]="generating">
          </p-button>
          <p-button 
            label="Generate Backend" 
            icon="pi pi-server"
            severity="secondary"
            [outlined]="true"
            (onClick)="generateBackendCode()"
            [loading]="generatingBackend">
          </p-button>
          <p-button 
            label="Generate Frontend" 
            icon="pi pi-desktop"
            severity="secondary"
            [outlined]="true"
            (onClick)="generateFrontendCode()"
            [loading]="generatingFrontend">
          </p-button>
        </div>

        <!-- Generated Code Tabs -->
        <p-tabView>
          <p-tabPanel header="Prompt">
            <p-card>
              <pre class="p-3 m-0 bg-gray-50 border-round text-sm line-height-3">{{ project.prompt }}</pre>
            </p-card>
          </p-tabPanel>

          <p-tabPanel header="Backend Code" [disabled]="!project.backendCode">
            @if (project.backendCode) {
              <p-card>
                <div class="flex justify-content-end mb-2">
                  <p-button 
                    label="Copy" 
                    icon="pi pi-copy"
                    size="small"
                    [text]="true"
                    (onClick)="copyToClipboard(project.backendCode!)">
                  </p-button>
                </div>
                <pre class="p-3 m-0 bg-gray-50 border-round text-sm line-height-3 overflow-auto" 
                     style="max-height: 600px;">{{ project.backendCode }}</pre>
              </p-card>
            } @else {
              <p-card>
                <div class="text-center p-6">
                  <i class="pi pi-code text-4xl text-300 mb-3"></i>
                  <p class="text-color-secondary">No backend code generated yet. Click "Generate Backend" to create Spring Boot CRUD code.</p>
                </div>
              </p-card>
            }
          </p-tabPanel>

          <p-tabPanel header="Frontend Code" [disabled]="!project.frontendCode">
            @if (project.frontendCode) {
              <p-card>
                <div class="flex justify-content-end mb-2">
                  <p-button 
                    label="Copy" 
                    icon="pi pi-copy"
                    size="small"
                    [text]="true"
                    (onClick)="copyToClipboard(project.frontendCode!)">
                  </p-button>
                </div>
                <pre class="p-3 m-0 bg-gray-50 border-round text-sm line-height-3 overflow-auto" 
                     style="max-height: 600px;">{{ project.frontendCode }}</pre>
              </p-card>
            } @else {
              <p-card>
                <div class="text-center p-6">
                  <i class="pi pi-desktop text-4xl text-300 mb-3"></i>
                  <p class="text-color-secondary">No frontend code generated yet. Click "Generate Frontend" to create Angular TypeScript interfaces.</p>
                </div>
              </p-card>
            }
          </p-tabPanel>
        </p-tabView>
      }

      <p-confirmDialog></p-confirmDialog>
      <p-toast></p-toast>
    </div>
  `
})
export class ProjectDetailComponent implements OnInit {
  project: Project | null = null;
  loading = false;
  generating = false;
  generatingBackend = false;
  generatingFrontend = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProject(+id);
    }
  }

  loadProject(id: number): void {
    this.loading = true;
    this.projectService.getProjectById(id).subscribe({
      next: (project) => {
        this.project = project;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  generateCode(): void {
    if (this.project) {
      this.generating = true;
      this.projectService.generateCode(this.project.id).subscribe({
        next: (project) => {
          this.project = project;
          this.generating = false;
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Code generated successfully!' });
        },
        error: () => {
          this.generating = false;
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to generate code' });
        }
      });
    }
  }

  generateBackendCode(): void {
    if (this.project) {
      this.generatingBackend = true;
      this.projectService.generateBackendCode(this.project.id).subscribe({
        next: (project) => {
          this.project = project;
          this.generatingBackend = false;
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Backend code generated successfully!' });
        },
        error: () => {
          this.generatingBackend = false;
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to generate backend code' });
        }
      });
    }
  }

  generateFrontendCode(): void {
    if (this.project) {
      this.generatingFrontend = true;
      this.projectService.generateFrontendCode(this.project.id).subscribe({
        next: (project) => {
          this.project = project;
          this.generatingFrontend = false;
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Frontend code generated successfully!' });
        },
        error: () => {
          this.generatingFrontend = false;
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to generate frontend code' });
        }
      });
    }
  }

  editProject(): void {
    if (this.project) {
      this.router.navigate(['/projects', this.project.id, 'edit']);
    }
  }

  confirmDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this project?',
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.deleteProject();
      }
    });
  }

  deleteProject(): void {
    if (this.project) {
      this.projectService.deleteProject(this.project.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Project deleted successfully' });
          this.router.navigate(['/projects']);
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete project' });
        }
      });
    }
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.messageService.add({ severity: 'success', summary: 'Copied', detail: 'Code copied to clipboard' });
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString();
  }
}

