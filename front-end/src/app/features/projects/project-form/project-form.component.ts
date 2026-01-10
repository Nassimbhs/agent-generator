import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { DropdownModule } from 'primeng/dropdown';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectRequest } from '../../../models/project.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    InputTextModule,
    InputTextareaModule,
    ButtonModule,
    MessageModule,
    DropdownModule
  ],
  template: `
    <div class="w-full max-w-4xl mx-auto">
      <p-card>
        <ng-template pTemplate="header">
          <div class="p-4">
            <h1 class="text-3xl font-bold m-0">
              {{ isEditMode ? 'Edit Project' : 'Create New Project' }}
            </h1>
          </div>
        </ng-template>

        <form [formGroup]="projectForm" (ngSubmit)="onSubmit()" class="flex flex-column gap-4">
          <div class="flex flex-column gap-2">
            <label for="name" class="font-semibold">Project Name *</label>
            <input 
              id="name"
              type="text" 
              pInputText 
              formControlName="name"
              placeholder="e.g., User Management System"
              class="w-full"
              [class.ng-invalid]="isFieldInvalid('name')">
            @if (isFieldInvalid('name')) {
              <small class="p-error">Project name is required</small>
            }
          </div>

          <div class="flex flex-column gap-2">
            <label for="description" class="font-semibold">Description</label>
            <textarea 
              id="description"
              pInputTextarea 
              formControlName="description"
              placeholder="Brief description of your project"
              rows="3"
              class="w-full">
            </textarea>
          </div>

          <div class="flex flex-column gap-2">
            <label for="projectType" class="font-semibold">Project Type</label>
            <p-dropdown 
              id="projectType"
              formControlName="projectType"
              [options]="projectTypes"
              placeholder="Select project type"
              class="w-full">
            </p-dropdown>
          </div>

          <div class="flex flex-column gap-2">
            <label for="prompt" class="font-semibold">Requirements Prompt *</label>
            <textarea 
              id="prompt"
              pInputTextarea 
              formControlName="prompt"
              placeholder="Describe what CRUD operations you need. Example: Create a User entity with fields: id (Long), username (String), email (String), firstName (String), lastName (String), createdAt (LocalDateTime). Include full CRUD operations."
              rows="8"
              class="w-full"
              [class.ng-invalid]="isFieldInvalid('prompt')">
            </textarea>
            @if (isFieldInvalid('prompt')) {
              <small class="p-error">Prompt is required</small>
            }
            <small class="text-color-secondary">
              Be specific about entities, fields, and relationships. The more details you provide, the better the generated code will be.
            </small>
          </div>

          @if (errorMessage) {
            <p-message severity="error" [text]="errorMessage"></p-message>
          }

          <div class="flex gap-2 justify-content-end">
            <p-button 
              label="Cancel" 
              severity="secondary"
              [outlined]="true"
              routerLink="/projects">
            </p-button>
            <p-button 
              type="submit"
              [label]="isEditMode ? 'Update' : 'Create'"
              [icon]="isEditMode ? 'pi pi-check' : 'pi pi-plus'"
              [loading]="loading"
              [disabled]="projectForm.invalid">
            </p-button>
          </div>
        </form>
      </p-card>
    </div>
  `
})
export class ProjectFormComponent implements OnInit {
  projectForm: FormGroup;
  loading = false;
  errorMessage = '';
  isEditMode = false;
  projectId: number | null = null;
  projectTypes = [
    { label: 'Spring Boot + Angular', value: 'spring-boot-angular' },
    { label: 'Spring Boot Only', value: 'spring-boot' },
    { label: 'Angular Only', value: 'angular' }
  ];

  constructor(
    private fb: FormBuilder,
    private projectService: ProjectService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.projectForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      prompt: ['', Validators.required],
      projectType: ['spring-boot-angular']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.projectId = +id;
      this.loadProject();
    }
  }

  loadProject(): void {
    if (this.projectId) {
      this.loading = true;
      this.projectService.getProjectById(this.projectId).subscribe({
        next: (project) => {
          this.projectForm.patchValue({
            name: project.name,
            description: project.description || '',
            prompt: project.prompt,
            projectType: project.projectType || 'spring-boot-angular'
          });
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.projectForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.projectForm.valid) {
      this.loading = true;
      this.errorMessage = '';
      
      const request: ProjectRequest = this.projectForm.value;
      
      const operation = this.isEditMode && this.projectId
        ? this.projectService.updateProject(this.projectId, request)
        : this.projectService.createProject(request);

      operation.subscribe({
        next: (project) => {
          this.router.navigate(['/projects', project.id]);
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Operation failed. Please try again.';
          this.loading = false;
        }
      });
    }
  }
}

