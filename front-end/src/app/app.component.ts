import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterModule } from '@angular/router';
import { MenubarModule } from 'primeng/menubar';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { AuthService } from './core/services/auth.service';
import { User } from './models/user.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterModule, MenubarModule, ButtonModule, ToastModule],
  template: `
    <div class="min-h-screen flex flex-column">
      <!-- Header -->
      <p-menubar [model]="menuItems" class="w-full">
        <ng-template pTemplate="end">
          <div class="flex align-items-center gap-2">
            @if (currentUser) {
              <span class="text-sm">{{ currentUser.username }}</span>
              <p-button 
                label="Logout" 
                icon="pi pi-sign-out" 
                (onClick)="logout()"
                severity="secondary"
                [text]="true"
                size="small">
              </p-button>
            } @else {
              <p-button 
                label="Login" 
                icon="pi pi-sign-in" 
                routerLink="/login"
                [text]="true"
                size="small">
              </p-button>
            }
          </div>
        </ng-template>
      </p-menubar>

      <!-- Main Content -->
      <main class="flex-1 p-4 md:p-6">
        <router-outlet></router-outlet>
      </main>
    </div>
    <p-toast></p-toast>
  `
})
export class AppComponent {
  currentUser: User | null = null;
  menuItems: any[] = [];

  constructor(private authService: AuthService) {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.updateMenu();
    });
  }

  private updateMenu(): void {
    if (this.currentUser) {
      this.menuItems = [
        {
          label: 'Generator',
          icon: 'pi pi-fw pi-code',
          routerLink: '/generator'
        },
        {
          label: 'Projects',
          icon: 'pi pi-fw pi-folder',
          routerLink: '/projects'
        }
      ];
    } else {
      this.menuItems = [];
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
