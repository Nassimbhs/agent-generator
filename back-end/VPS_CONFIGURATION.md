# VPS Configuration Guide

## Your VPS Details
- **IP Address**: 102.211.210.197
- **Ports**:
  - Spring Boot API: 8080
  - Ollama API: 11434
  - PostgreSQL: 5432

## Configuration Files Updated

### Backend (Spring Boot)
**File**: `src/main/resources/application.properties`

#### Ollama Configuration
- **If Spring Boot runs on VPS** (recommended): `ollama.api.url=http://localhost:11434`
- **If Spring Boot runs on Windows**: `ollama.api.url=http://102.211.210.197:11434`

#### Database Configuration
- Currently configured for localhost PostgreSQL on VPS
- Update password in `application.properties` as needed

#### Server Port
- Spring Boot runs on port 8080
- Ensure firewall allows access to port 8080

### Frontend (Angular)
**File**: `src/environments/environment.ts`
- API URL: `http://102.211.210.197:8080`

**Production File**: `src/environments/environment.prod.ts`
- API URL: `http://102.211.210.197:8080`

## VPS Setup Checklist

### 1. Firewall Configuration
```bash
# Allow Spring Boot port
sudo ufw allow 8080/tcp

# Allow Ollama port (if exposing externally)
sudo ufw allow 11434/tcp

# Allow PostgreSQL port (if needed externally)
sudo ufw allow 5432/tcp

# Check status
sudo ufw status
```

### 2. Ollama Configuration
```bash
# On VPS, start Ollama (if not running as service)
ollama serve

# Or configure as service to start automatically
# Test Ollama API
curl http://localhost:11434/api/tags

# Verify Qwen2.5-Coder model
ollama list
```

### 3. Spring Boot Deployment
```bash
# Build the project
cd back-end
mvn clean package

# Run the JAR file
java -jar target/generator-0.0.1-SNAPSHOT.jar

# Or run with Maven
mvn spring-boot:run
```

### 4. Frontend Build and Deployment
```bash
# Build for production
cd front-end
npm install
npm run build

# The dist/ folder contains production build
# Deploy to Nginx, Apache, or serve with http-server
```

### 5. Nginx Configuration (Optional)
If using Nginx as reverse proxy:

```nginx
server {
    listen 80;
    server_name 102.211.210.197;

    # Frontend
    location / {
        root /var/www/crud-generator;
        try_files $uri $uri/ /index.html;
    }

    # Backend API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Swagger UI
    location /swagger-ui {
        proxy_pass http://localhost:8080;
    }
}
```

## Testing Connections

### Test Backend API
```bash
# From Windows or external machine
curl http://102.211.210.197:8080/api/auth/login

# Check Swagger UI
http://102.211.210.197:8080/swagger-ui.html
```

### Test Ollama API
```bash
# From VPS (localhost)
curl http://localhost:11434/api/tags

# From Windows (if exposing Ollama)
curl http://102.211.210.197:11434/api/tags
```

### Test Frontend
- Open browser: `http://102.211.210.197` (if using Nginx)
- Or: `http://102.211.210.197:4200` (if using Angular dev server)

## Security Recommendations

1. **Use HTTPS**: Configure SSL certificate (Let's Encrypt)
2. **Change Default Passwords**: Update PostgreSQL password
3. **JWT Secret**: Use strong, unique secret in production
4. **Firewall**: Only expose necessary ports
5. **Ollama**: Keep on localhost unless needed externally
6. **Database**: Don't expose PostgreSQL port publicly

## Troubleshooting

### Cannot Connect to API
- Check firewall: `sudo ufw status`
- Check if Spring Boot is running: `ps aux | grep java`
- Check port: `sudo netstat -tulpn | grep 8080`

### Ollama Connection Failed
- Verify Ollama is running: `ollama list`
- Check if accessible: `curl http://localhost:11434/api/tags`
- Review Spring Boot logs for connection errors

### CORS Issues
- Update `SecurityConfig.java` CORS configuration with frontend URL
- Add `http://102.211.210.197` to allowed origins

## Production Checklist

- [ ] Update database password
- [ ] Set strong JWT secret
- [ ] Configure HTTPS/SSL
- [ ] Set up Nginx reverse proxy
- [ ] Configure firewall properly
- [ ] Set up process manager (PM2, systemd)
- [ ] Configure logging
- [ ] Set up monitoring
- [ ] Backup database regularly
- [ ] Update application.properties for production

