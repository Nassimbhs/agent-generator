# Ollama Integration Guide

## Overview

This application integrates with Ollama running on your VPS to generate Spring Boot CRUD code and Angular TypeScript interfaces using the Qwen2.5-Coder model.

## Configuration

### 1. Update VPS Ollama URL

Edit `src/main/resources/application.properties`:

```properties
# Update this to your VPS IP or domain
ollama.api.url=http://your-vps-ip:11434
# Or if using domain:
# ollama.api.url=http://your-vps-domain.com:11434

ollama.model.name=qwen2.5-coder
ollama.timeout=300  # Timeout in seconds (5 minutes default)
```

### 2. Ensure Ollama is Running on VPS

SSH into your VPS and verify Ollama is running:

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running, start it:
ollama serve

# Verify Qwen2.5-Coder model is available:
ollama list
```

### 3. Network Configuration

**Important**: Make sure Ollama on your VPS is accessible:

- **Option 1**: Expose Ollama on VPS (for development)
  ```bash
  # On VPS, edit Ollama config or use environment variable
  export OLLAMA_HOST=0.0.0.0:11434
  ollama serve
  ```

- **Option 2**: Use SSH Tunnel (more secure)
  ```bash
  # On Windows, create SSH tunnel:
  ssh -L 11434:localhost:11434 user@your-vps-ip
  # Then use localhost:11434 in application.properties
  ```

- **Option 3**: Use VPN or private network

## API Endpoints

### Generate All Code (Backend + Frontend)
```
POST /api/projects/{id}/generate
```

Generates both Spring Boot CRUD code and Angular TypeScript interfaces.

### Generate Backend Code Only
```
POST /api/projects/{id}/generate/backend
```

Generates Spring Boot CRUD REST API code including:
- Entity classes with JPA annotations
- Repository interfaces
- Service interfaces and implementations
- REST Controllers
- DTOs
- Swagger annotations

### Generate Frontend Code Only
```
POST /api/projects/{id}/generate/frontend
```

Generates Angular TypeScript interface/model files.

## Usage Example

1. **Create a Project**:
```json
POST /api/projects
{
  "name": "User Management System",
  "description": "CRUD for users",
  "prompt": "Create a User entity with fields: id (Long), username (String), email (String), firstName (String), lastName (String), createdAt (LocalDateTime). Include full CRUD operations.",
  "projectType": "spring-boot-angular"
}
```

2. **Generate Code**:
```json
POST /api/projects/1/generate
```

3. **Get Generated Code**:
```json
GET /api/projects/1
```

Response includes:
- `backendCode`: Spring Boot CRUD code
- `frontendCode`: Angular TypeScript interfaces
- `generatedCode`: Legacy combined field

## Code Generation Prompts

The system uses optimized prompts for each type:

### Spring Boot Prompt
- Generates complete CRUD REST API
- Includes Entity, Repository, Service, Controller, DTOs
- Uses Lombok, JPA, Swagger annotations
- Spring Boot 4.0.1, Java 17, PostgreSQL

### Angular Prompt
- Generates TypeScript interface/model files
- Proper TypeScript types and optional properties
- Angular 17+, TypeScript 5+
- Export statements and documentation

## Troubleshooting

### Connection Issues

1. **Cannot connect to Ollama**:
   - Verify VPS IP/domain is correct
   - Check firewall rules (port 11434)
   - Ensure Ollama is running: `curl http://your-vps-ip:11434/api/tags`

2. **Timeout Errors**:
   - Increase `ollama.timeout` in application.properties
   - Check VPS resources (CPU/RAM)
   - Consider using smaller model or optimizing prompts

3. **Empty Responses**:
   - Check Ollama logs on VPS
   - Verify model is downloaded: `ollama list`
   - Check VPS memory availability

### Performance Tips

1. **For Faster Generation**:
   - Use smaller prompts
   - Generate backend and frontend separately
   - Consider caching generated code

2. **For Better Quality**:
   - Provide detailed prompts
   - Include specific requirements
   - Mention frameworks and versions

## Security Considerations

- **Production**: Use HTTPS and secure network
- **Development**: SSH tunnel recommended over exposing Ollama publicly
- **Authentication**: All endpoints require JWT authentication
- **Rate Limiting**: Consider implementing rate limits for code generation

## Architecture

```
ProjectController
    ↓
ProjectService
    ↓
CodeGenerationService
    ↓
OllamaClientService (HTTP Client)
    ↓
Ollama API (VPS)
    ↓
Qwen2.5-Coder Model
```

## Next Steps

1. Update `application.properties` with your VPS Ollama URL
2. Test connection: `curl http://your-vps-ip:11434/api/tags`
3. Create a project and generate code
4. Review generated code and refine prompts as needed

