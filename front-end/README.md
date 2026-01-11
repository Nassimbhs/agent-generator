# CRUD Generator Frontend

Angular 17+ frontend application using PrimeNG and PrimeFlex for responsive UI/UX design.

## Features

- **PrimeNG Components**: Rich UI components library
- **PrimeFlex**: Utility-first CSS framework (minimal custom CSS)
- **Responsive Design**: Mobile-first approach with PrimeFlex utilities
- **Authentication**: Login and registration with JWT
- **Project Management**: Create, edit, view, and delete projects
- **Code Generation**: Generate Spring Boot CRUD and Angular TypeScript interfaces

## Installation

```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

## Configuration

Update `src/environments/environment.ts` with your backend API URL:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

## PrimeFlex Usage

This project uses PrimeFlex utility classes for styling, minimizing custom CSS:

- **Layout**: `flex`, `grid`, `block`, `inline-block`
- **Spacing**: `p-*`, `m-*`, `gap-*`
- **Typography**: `text-*`, `font-*`
- **Colors**: `text-primary`, `bg-gray-50`, etc.
- **Responsive**: `md:`, `lg:` prefixes for breakpoints

Example:
```html
<div class="flex flex-column gap-3 p-4 md:p-6">
  <h1 class="text-3xl font-bold">Title</h1>
</div>
```

## Project Structure

```
src/
├── app/
│   ├── core/
│   │   ├── guards/          # Route guards
│   │   ├── interceptors/     # HTTP interceptors
│   │   └── services/         # Core services
│   ├── features/
│   │   ├── auth/            # Authentication components
│   │   └── projects/        # Project management components
│   ├── models/              # TypeScript interfaces
│   └── app.component.ts     # Root component
└── environments/            # Environment configuration
```

## Development

The application uses:
- Angular 17+ (standalone components)
- PrimeNG 17+ for UI components
- PrimeFlex 3+ for utility classes
- RxJS for reactive programming
- TypeScript 5+

## Responsive Breakpoints

- Mobile: default (< 768px)
- Tablet: `md:` prefix (≥ 768px)
- Desktop: `lg:` prefix (≥ 992px)


