# Frontend Setup Guide

## Quick Start

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Update API URL** in `src/environments/environment.ts`:
   ```typescript
   apiUrl: 'http://localhost:8080'  // Change to your backend URL
   ```

3. **Start development server**:
   ```bash
   npm start
   ```

4. **Access application**:
   Open http://localhost:8090
   
   Note: Port is configured as 8090 in angular.json. The server is also configured to listen on 0.0.0.0 (all interfaces) so it can be accessed from external machines when running on VPS.

## Features Implemented

✅ **PrimeNG Components**:
- Menubar (navigation)
- Card (content containers)
- Button (actions)
- InputText, Password, Textarea (forms)
- DataView (project list with list/grid views)
- TabView (code display)
- Tag (labels)
- Message (error/success messages)
- Toast (notifications)
- ConfirmDialog (delete confirmation)

✅ **PrimeFlex Utilities**:
- Responsive layout: `flex`, `flex-column`, `flex-row`
- Spacing: `p-*`, `m-*`, `gap-*`
- Typography: `text-*`, `font-*`
- Responsive breakpoints: `md:`, `lg:`
- Colors: `text-primary`, `bg-gray-50`, etc.

✅ **Components**:
- Login/Register (authentication)
- Project List (with list/grid toggle)
- Project Form (create/edit)
- Project Detail (view code, generate)

✅ **Services**:
- AuthService (authentication)
- ProjectService (CRUD operations)

✅ **Guards & Interceptors**:
- AuthGuard (route protection)
- AuthInterceptor (JWT token injection)

## Responsive Design

The application is fully responsive using PrimeFlex utilities:

- **Mobile**: Single column layout, stacked elements
- **Tablet** (`md:`): Two columns, side-by-side elements
- **Desktop** (`lg:`): Multi-column grids, optimal spacing

Example responsive classes:
```html
<div class="flex flex-column md:flex-row gap-3">
  <!-- Stacks on mobile, side-by-side on tablet+ -->
</div>
```

## Custom CSS

Minimal custom CSS in `src/styles.css` - only root variables and base styles. All styling uses PrimeFlex utilities.

