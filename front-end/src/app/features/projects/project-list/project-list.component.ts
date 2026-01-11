import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { TagModule } from 'primeng/tag';
import { ProjectService } from '../../../core/services/project.service';
import { Project } from '../../../models/project.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    ButtonModule,
    DataViewModule,
    TagModule
  ],
  template: `
    <div class="w-full">
      <div class="flex justify-content-between align-items-center mb-4">
        <h1 class="text-3xl font-bold m-0">My Projects</h1>
        <p-button 
          label="New Project" 
          icon="pi pi-plus"
          routerLink="/projects/new">
        </p-button>
      </div>

      @if (loading) {
        <div class="flex justify-content-center p-6">
          <i class="pi pi-spin pi-spinner text-4xl text-primary"></i>
        </div>
      } @else if (projects.length === 0) {
        <p-card>
          <div class="text-center p-6">
            <i class="pi pi-folder text-6xl text-300 mb-3"></i>
            <h2 class="text-2xl font-semibold mb-2">No projects yet</h2>
            <p class="text-color-secondary mb-4">Create your first CRUD project to get started</p>
            <p-button 
              label="Create Project" 
              icon="pi pi-plus"
              routerLink="/projects/new">
            </p-button>
          </div>
        </p-card>
      } @else {
        <p-dataView [value]="projects" [layout]="layout">
          <ng-template pTemplate="header">
            <div class="flex justify-content-between align-items-center mb-3">
              <span class="text-lg font-semibold">{{ projects.length }} project(s)</span>
              <div class="flex gap-2">
                <p-button 
                  icon="pi pi-list" 
                  [text]="true"
                  [outlined]="layout === 'grid'"
                  (onClick)="layout = 'list'">
                </p-button>
                <p-button 
                  icon="pi pi-th-large" 
                  [text]="true"
                  [outlined]="layout === 'list'"
                  (onClick)="layout = 'grid'">
                </p-button>
              </div>
            </div>
          </ng-template>

          <ng-template let-project pTemplate="listItem">
            <div class="col-12 p-3">
              <p-card class="h-full">
                <div class="flex flex-column md:flex-row justify-content-between gap-3">
                  <div class="flex-1">
                    <h3 class="text-xl font-semibold m-0 mb-2">{{ project.name }}</h3>
                    @if (project.description) {
                      <p class="text-color-secondary m-0 mb-2">{{ project.description }}</p>
                    }
                    <div class="flex flex-wrap gap-2 align-items-center">
                      <p-tag [value]="project.projectType || 'N/A'" severity="info"></p-tag>
                      <span class="text-sm text-color-secondary">
                        <i class="pi pi-calendar mr-1"></i>
                        {{ formatDate(project.createdAt) }}
                      </span>
                    </div>
                  </div>
                  <div class="flex gap-2">
                    <p-button 
                      label="View" 
                      icon="pi pi-eye"
                      [outlined]="true"
                      (onClick)="viewProject(project.id)">
                    </p-button>
                    <p-button 
                      label="Edit" 
                      icon="pi pi-pencil"
                      [outlined]="true"
                      severity="secondary"
                      (onClick)="editProject(project.id)">
                    </p-button>
                  </div>
                </div>
              </p-card>
            </div>
          </ng-template>

          <ng-template let-project pTemplate="gridItem">
            <div class="col-12 md:col-6 lg:col-4 p-2">
              <p-card class="h-full">
                <div class="flex flex-column gap-3">
                  <div>
                    <h3 class="text-xl font-semibold m-0 mb-2">{{ project.name }}</h3>
                    @if (project.description) {
                      <p class="text-color-secondary m-0 mb-2 line-height-3" 
                         [style.max-height.px]="60"
                         style="overflow: hidden; text-overflow: ellipsis;">
                        {{ project.description }}
                      </p>
                    }
                  </div>
                  <div class="flex flex-column gap-2">
                    <p-tag [value]="project.projectType || 'N/A'" severity="info"></p-tag>
                    <span class="text-sm text-color-secondary">
                      <i class="pi pi-calendar mr-1"></i>
                      {{ formatDate(project.createdAt) }}
                    </span>
                  </div>
                  <div class="flex gap-2 mt-auto">
                    <p-button 
                      label="View" 
                      icon="pi pi-eye"
                      [outlined]="true"
                      class="flex-1"
                      (onClick)="viewProject(project.id)">
                    </p-button>
                    <p-button 
                      icon="pi pi-pencil"
                      [outlined]="true"
                      severity="secondary"
                      (onClick)="editProject(project.id)">
                    </p-button>
                  </div>
                </div>
              </p-card>
            </div>
          </ng-template>
        </p-dataView>
      }
    </div>
  `
})
export class ProjectListComponent implements OnInit {
  projects: Project[] = [];
  loading = false;
  layout: 'list' | 'grid' = 'grid';

  constructor(
    private projectService: ProjectService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  viewProject(id: number): void {
    this.router.navigate(['/projects', id]);
  }

  editProject(id: number): void {
    this.router.navigate(['/projects', id, 'edit']);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString();
  }
}


