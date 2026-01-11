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
import { RegisterRequest } from '../../../models/user.model';

@Component({
  selector: 'app-register',
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
              <h1 class="text-3xl font-bold m-0">Register</h1>
            </div>
          </ng-template>
          
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="flex flex-column gap-3">
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
                <small class="p-error">Username is required (min 3 characters)</small>
              }
            </div>

            <div class="flex flex-column gap-2">
              <label for="email" class="font-semibold">Email</label>
              <input 
                id="email"
                type="email" 
                pInputText 
                formControlName="email"
                placeholder="Enter email"
                class="w-full"
                [class.ng-invalid]="isFieldInvalid('email')">
              @if (isFieldInvalid('email')) {
                <small class="p-error">Valid email is required</small>
              }
            </div>

            <div class="flex flex-column gap-2">
              <label for="password" class="font-semibold">Password</label>
              <p-password 
                id="password"
                formControlName="password"
                placeholder="Enter password"
                [feedback]="true"
                [toggleMask]="true"
                class="w-full"
                [inputStyleClass]="isFieldInvalid('password') ? 'ng-invalid' : ''">
              </p-password>
              @if (isFieldInvalid('password')) {
                <small class="p-error">Password is required (min 6 characters)</small>
              }
            </div>

            <div class="flex flex-column gap-2">
              <label for="firstName" class="font-semibold">First Name (Optional)</label>
              <input 
                id="firstName"
                type="text" 
                pInputText 
                formControlName="firstName"
                placeholder="Enter first name"
                class="w-full">
            </div>

            <div class="flex flex-column gap-2">
              <label for="lastName" class="font-semibold">Last Name (Optional)</label>
              <input 
                id="lastName"
                type="text" 
                pInputText 
                formControlName="lastName"
                placeholder="Enter last name"
                class="w-full">
            </div>

            @if (errorMessage) {
              <p-message severity="error" [text]="errorMessage"></p-message>
            }

            <p-button 
              type="submit"
              label="Register"
              icon="pi pi-user-plus"
              [loading]="loading"
              [disabled]="registerForm.invalid"
              class="w-full">
            </p-button>

            <div class="text-center mt-2">
              <span class="text-sm">Already have an account? </span>
              <a routerLink="/login" class="text-primary font-semibold no-underline">Login</a>
            </div>
          </form>
        </p-card>
      </div>
    </div>
  `
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      firstName: [''],
      lastName: ['']
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.loading = true;
      this.errorMessage = '';
      
      const request: RegisterRequest = this.registerForm.value;
      this.authService.register(request).subscribe({
        next: () => {
          this.router.navigate(['/projects']);
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
          this.loading = false;
        }
      });
    }
  }
}


