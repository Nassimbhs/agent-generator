import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { LoginRequest } from '../../../models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessageModule
  ],
  template: `
    <div class="flex align-items-center justify-content-center min-h-screen p-4">
      <div class="w-full max-w-md">
        <p-card class="w-full">
          <ng-template pTemplate="header">
            <div class="text-center p-4">
              <h1 class="text-3xl font-bold m-0">Login</h1>
            </div>
          </ng-template>
          
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="flex flex-column gap-3">
            <div class="flex flex-column gap-2">
              <label for="username" class="font-semibold">Username</label>
              <input 
                id="username"
                type="text" 
                pInputText 
                formControlName="username"
                placeholder="Enter username"
                class="w-full"
                [class.ng-invalid]="isFieldInvalid('username')">
              @if (isFieldInvalid('username')) {
                <small class="p-error">Username is required</small>
              }
            </div>

            <div class="flex flex-column gap-2">
              <label for="password" class="font-semibold">Password</label>
              <p-password 
                id="password"
                formControlName="password"
                placeholder="Enter password"
                [feedback]="false"
                [toggleMask]="true"
                class="w-full"
                [inputStyleClass]="isFieldInvalid('password') ? 'ng-invalid' : ''">
              </p-password>
              @if (isFieldInvalid('password')) {
                <small class="p-error">Password is required</small>
              }
            </div>

            @if (errorMessage) {
              <p-message severity="error" [text]="errorMessage"></p-message>
            }

            <p-button 
              type="submit"
              label="Login"
              icon="pi pi-sign-in"
              [loading]="loading"
              [disabled]="loginForm.invalid"
              class="w-full">
            </p-button>

            <div class="text-center mt-2">
              <span class="text-sm">Don't have an account? </span>
              <a routerLink="/register" class="text-primary font-semibold no-underline">Register</a>
            </div>
          </form>
        </p-card>
      </div>
    </div>
  `
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.loading = true;
      this.errorMessage = '';
      
      const request: LoginRequest = this.loginForm.value;
      this.authService.login(request).subscribe({
        next: () => {
          this.router.navigate(['/projects']);
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Login failed. Please try again.';
          this.loading = false;
        }
      });
    }
  }
}

