# Setup Steps for CRUD Generator Project

## ⚠️ IMPORTANT: Development Environment Recommendation

### Recommended: Develop Directly on VPS (Ubuntu)

**For your setup (Windows with 8GB RAM, VPS with 8GB RAM Ubuntu), it is STRONGLY RECOMMENDED to develop directly on your VPS rather than on Windows.**

#### Why Develop on VPS?

1. **Memory Constraints**: 
   - Windows with 8GB RAM is insufficient for Qwen2.5-Coder LLM (requires 4-6GB)
   - Ubuntu is more memory-efficient, making 8GB RAM workable for development
   - Qwen2.5-Coder model needs significant RAM that Windows 8GB cannot comfortably provide

2. **Same Environment as Production**:
   - Avoids environment differences between development and production
   - Reduces deployment issues and "works on my machine" problems
   - Easier testing and debugging

3. **Better Resource Utilization**:
   - Ubuntu uses less base memory (~1-2GB) vs Windows (~2-3GB)
   - More RAM available for your application and LLM
   - Better performance for LLM inference

4. **No Deployment Step During Development**:
   - Test immediately with full LLM functionality
   - Faster iteration cycle
   - Direct access to production-like environment

#### Recommended Workflow: Remote Development

**Best Approach**: Use VS Code Remote SSH or similar tools
- Edit code on your Windows machine using VS Code
- Code runs and executes on your VPS with full resources
- Best of both worlds: comfortable IDE on Windows, powerful execution on VPS

**Steps**:
1. Set up project on VPS (Ubuntu) - see VPS setup section below
2. Install VS Code on Windows
3. Install "Remote - SSH" extension in VS Code
4. Connect to your VPS via SSH from VS Code
5. Develop directly on VPS filesystem
6. Run and test on VPS with full LLM support

#### Alternative: Hybrid Approach (Less Recommended)

If you prefer local editing:
- Develop Spring Boot code locally on Windows (no LLM needed for basic coding)
- Test LLM features only on VPS
- Deploy to VPS for final testing

**Note**: This approach is less efficient as you still need VPS for LLM testing.

---

## Windows Setup (For Limited Testing Without LLM)

**⚠️ WARNING**: The following Windows setup steps are provided for reference, but **Qwen2.5-Coder LLM will likely not work properly on Windows with 8GB RAM**. Use Windows setup only for:
- Learning the project structure
- Basic Spring Boot development (without LLM)
- Testing non-LLM features

**For full functionality with LLM, use the VPS setup instead.**

---

## Prerequisites Installation

### Step 1: Install Java Development Kit (JDK) 17
1. Download JDK 17 from Oracle or OpenJDK (Adoptium/Temurin recommended)
2. Visit: https://adoptium.net/temurin/releases/?version=17
3. Download the Windows x64 installer (.msi file)
4. Run the installer and follow the installation wizard
5. Check "Add to PATH" during installation
6. Verify installation by opening PowerShell/Command Prompt and running: `java -version`
7. Verify Java compiler: `javac -version`
8. Both commands should show version 17

### Step 2: Install Apache Maven
1. Download Maven from: https://maven.apache.org/download.cgi
2. Download the Binary zip archive (apache-maven-X.X.X-bin.zip)
3. Extract the zip file to a location like `C:\Program Files\Apache\maven`
4. Add Maven to system PATH:
   - Open System Properties → Advanced → Environment Variables
   - Under System Variables, find and select "Path", then click "Edit"
   - Click "New" and add: `C:\Program Files\Apache\maven\bin` (adjust path to your extraction location)
   - Click OK on all dialogs
5. Verify installation: Open new PowerShell/Command Prompt and run: `mvn -version`
6. You should see Maven version information

### Step 3: Install PostgreSQL Database
1. Download PostgreSQL from: https://www.postgresql.org/download/windows/
2. Download the PostgreSQL installer from EnterpriseDB
3. Run the installer and follow the setup wizard
4. During installation:
   - Choose installation directory (default is fine)
   - Select components: PostgreSQL Server, pgAdmin 4, Command Line Tools
   - Set a password for the postgres superuser (remember this password)
   - Set port to 5432 (default)
   - Choose locale (default is fine)
5. Complete the installation
6. Verify installation: Open PowerShell and run: `psql --version`
7. Note: You may need to add PostgreSQL bin directory to PATH if the command doesn't work

### Step 4: Install Git (if not already installed)
1. Download Git for Windows from: https://git-scm.com/download/win
2. Run the installer with default settings
3. Verify installation: Open PowerShell and run: `git --version`

### Step 5: Install Ollama (for Qwen2.5-Coder LLM)
**⚠️ WARNING**: Qwen2.5-Coder requires 4-6GB RAM. With Windows using 2-3GB base memory, your 8GB system will be severely constrained. The LLM may not work properly or may cause system instability. **Strongly consider using VPS instead.**

1. Download Ollama from: https://ollama.com/download
2. Download the Windows installer
3. Run the installer and follow the setup wizard
4. After installation, Ollama should start automatically
5. Verify installation: Open PowerShell and run: `ollama --version`
6. Download Qwen2.5-Coder model:
   - Open PowerShell/Command Prompt
   - Run: `ollama pull qwen2.5-coder`
   - Wait for the download to complete (this may take several minutes depending on your internet speed)
   - The model file will be several GB in size
   - **Note**: This may cause system slowdown or crashes on 8GB RAM Windows
7. Verify model installation: Run `ollama list` to see qwen2.5-coder in the list
8. **Memory Check**: Before running, close unnecessary applications and monitor memory usage

---

## Project Setup

### Step 6: Download/Clone the Project
1. If the project is in a Git repository:
   - Open PowerShell/Command Prompt
   - Navigate to your desired directory (e.g., `cd C:\Users\Nassim\Desktop`)
   - Run: `git clone <repository-url>`
   - Navigate into the project: `cd generator`
2. If you already have the project locally:
   - Navigate to the project directory: `cd C:\Users\Nassim\Desktop\generator`

### Step 7: Configure PostgreSQL Database
1. Open pgAdmin 4 (installed with PostgreSQL) or use command line
2. Connect to PostgreSQL server:
   - Server: localhost
   - Port: 5432
   - Username: postgres
   - Password: (the password you set during installation)
3. Create a new database for the project:
   - Right-click "Databases" → Create → Database
   - Name: `generator_db` (or your preferred name)
   - Click Save
4. Alternatively, use command line:
   - Open PowerShell
   - Run: `psql -U postgres`
   - Enter your postgres password when prompted
   - Run: `CREATE DATABASE generator_db;`
   - Run: `\q` to exit

### Step 8: Configure Application Properties
1. Navigate to the project directory
2. Open the file: `src\main\resources\application.properties`
3. Add database configuration (if not already present):
   - Add PostgreSQL connection settings
   - Database URL, username, password
   - JPA/Hibernate settings if needed
4. Add Ollama/Qwen2.5-Coder configuration:
   - Ollama API endpoint (typically: http://localhost:11434)
   - Model name: qwen2.5-coder
   - Any other LLM-related settings

### Step 9: Build the Project with Maven
1. Open PowerShell/Command Prompt
2. Navigate to the project root directory: `cd C:\Users\Nassim\Desktop\generator`
3. Clean previous builds (optional): `mvn clean`
4. Build the project: `mvn clean install`
5. Wait for Maven to download dependencies and compile the project
6. This may take several minutes on first build
7. Verify successful build: Look for "BUILD SUCCESS" message at the end
8. If build fails, check error messages and resolve any issues

### Step 10: Run the Application
1. Ensure PostgreSQL service is running:
   - Open Services (Win + R, type "services.msc")
   - Find "postgresql-x64-XX" service
   - Ensure it's running (if not, right-click and Start)
2. Ensure Ollama is running:
   - Check if Ollama service is running
   - If not, start it from Start Menu or run: `ollama serve` in a separate terminal
3. Run the Spring Boot application:
   - Option A: Using Maven: `mvn spring-boot:run`
   - Option B: Using the JAR file: `java -jar target\generator-0.0.1-SNAPSHOT.jar`
4. Wait for the application to start
5. Look for "Started GeneratorApplication" message in the console
6. The application should be accessible at: http://localhost:8080 (or the port configured in application.properties)

---

## Verification Steps

### Step 11: Verify Everything is Working
1. Check application logs for any errors
2. Verify database connection (check logs for successful connection)
3. Verify Ollama/Qwen2.5-Coder connection:
   - Test Ollama API: Open browser and visit: http://localhost:11434/api/tags
   - Should see qwen2.5-coder in the response
4. Test the application endpoints (if any are configured)
5. Test CRUD generation functionality with a sample prompt

---

## Troubleshooting Common Issues

### Java Not Found
- Ensure JDK 17 is installed and added to PATH
- Restart PowerShell/Command Prompt after adding to PATH
- Verify with: `java -version`

### Maven Not Found
- Ensure Maven is installed and bin directory is in PATH
- Restart terminal after adding to PATH
- Verify with: `mvn -version`

### PostgreSQL Connection Errors
- Verify PostgreSQL service is running
- Check database name, username, and password in application.properties
- Verify PostgreSQL is listening on port 5432
- Check firewall settings if connection fails

### Ollama/Qwen2.5-Coder Issues
- Ensure Ollama is running: `ollama serve` or check Windows services
- Verify model is downloaded: `ollama list`
- Check Ollama API is accessible: http://localhost:11434/api/tags
- If model not found, re-download: `ollama pull qwen2.5-coder`

### Build Failures
- Check internet connection (Maven needs to download dependencies)
- Verify Java version is 17: `java -version`
- Clean and rebuild: `mvn clean install`
- Check for error messages in the build output

### Port Already in Use
- If port 8080 is in use, change it in application.properties: `server.port=8081`
- If PostgreSQL port 5432 is in use, change PostgreSQL port or update application.properties

---

---

## VPS Setup (Ubuntu) - RECOMMENDED FOR DEVELOPMENT

### Prerequisites Installation on Ubuntu VPS

#### Step 1: Install Java Development Kit (JDK) 17
1. SSH into your VPS: `ssh user@your-vps-ip`
2. Update package list: `sudo apt update`
3. Install JDK 17: `sudo apt install openjdk-17-jdk -y`
4. Verify installation: `java -version` (should show version 17)
5. Verify Java compiler: `javac -version`

#### Step 2: Install Apache Maven
1. Install Maven: `sudo apt install maven -y`
2. Verify installation: `mvn -version`

#### Step 3: Install PostgreSQL Database
1. Install PostgreSQL: `sudo apt install postgresql postgresql-contrib -y`
2. Start PostgreSQL service: `sudo systemctl start postgresql`
3. Enable PostgreSQL to start on boot: `sudo systemctl enable postgresql`
4. Switch to postgres user: `sudo -u postgres psql`
5. Create database: `CREATE DATABASE generator_db;`
6. Create user (optional): `CREATE USER generator_user WITH PASSWORD 'your_password';`
7. Grant privileges: `GRANT ALL PRIVILEGES ON DATABASE generator_db TO generator_user;`
8. Exit: `\q`

#### Step 4: Install Git
1. Install Git: `sudo apt install git -y`
2. Verify installation: `git --version`

#### Step 5: Install Ollama (for Qwen2.5-Coder LLM)
1. Download and install Ollama:
   ```bash
   curl -fsSL https://ollama.com/install.sh | sh
   ```
2. Verify installation: `ollama --version`
3. Download Qwen2.5-Coder model: `ollama pull qwen2.5-coder`
4. Wait for download to complete (several GB, may take time)
5. Verify model: `ollama list` (should show qwen2.5-coder)

#### Step 6: Set Up Swap Space (IMPORTANT for 8GB RAM)
1. Check current swap: `free -h`
2. Create swap file (4GB recommended):
   ```bash
   sudo fallocate -l 4G /swapfile
   sudo chmod 600 /swapfile
   sudo mkswap /swapfile
   sudo swapon /swapfile
   ```
3. Make swap permanent:
   ```bash
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
4. Verify: `free -h` (should show swap)

### Project Setup on VPS

#### Step 7: Clone/Upload Project
1. If using Git: `git clone <repository-url>`
2. Navigate to project: `cd generator`
3. Or upload project files via SCP/SFTP from Windows

#### Step 8: Configure Application Properties
1. Edit: `nano src/main/resources/application.properties`
2. Add database configuration:
   ```
   spring.datasource.url=jdbc:postgresql://localhost:5432/generator_db
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Add Ollama configuration:
   ```
   ollama.api.url=http://localhost:11434
   ollama.model.name=qwen2.5-coder
   ```

#### Step 9: Build the Project
1. Navigate to project root: `cd ~/generator` (adjust path)
2. Build: `mvn clean install`
3. Wait for dependencies to download
4. Verify: Look for "BUILD SUCCESS"

#### Step 10: Run the Application
1. Ensure PostgreSQL is running: `sudo systemctl status postgresql`
2. Ensure Ollama is running: `ollama serve &` (runs in background)
3. Run application: `mvn spring-boot:run`
4. Or run JAR: `java -jar target/generator-0.0.1-SNAPSHOT.jar`
5. Application accessible at: `http://your-vps-ip:8080`

### Memory Management Tips for 8GB VPS

**Critical for successful operation:**

1. **Monitor Memory Usage**:
   - Install htop: `sudo apt install htop -y`
   - Monitor: `htop` or `free -h`
   - Keep at least 1-2GB free for system

2. **Optimize Ollama**:
   - Limit loaded models: Set `OLLAMA_MAX_LOADED_MODELS=1` in environment
   - Use smaller model variant if available (e.g., 7B instead of larger)
   - Restart Ollama if memory gets high: `pkill ollama && ollama serve &`

3. **Stop Unnecessary Services**:
   - Disable unused services: `sudo systemctl disable <service-name>`
   - Check running services: `sudo systemctl list-units --type=service --state=running`

4. **Resource Allocation** (8GB total):
   - Operating System: ~1-2GB
   - PostgreSQL: ~200-500MB
   - Spring Boot App: ~500MB-1GB
   - Qwen2.5-Coder: ~4-6GB (when active)
   - Swap: 4GB (safety buffer)
   - **Total: ~6-9GB** (swap helps when exceeding RAM)

5. **Storage Allocation** (160GB total):
   - Ubuntu OS: ~20GB
   - PostgreSQL data: ~5-10GB
   - Qwen2.5-Coder model: ~4-8GB
   - Application & dependencies: ~1-2GB
   - Generated projects: Variable
   - **Remaining: ~120GB+ available**

### Remote Development Setup (VS Code)

1. **On Windows**:
   - Install VS Code: https://code.visualstudio.com/
   - Install "Remote - SSH" extension

2. **On VPS**:
   - Ensure SSH is enabled: `sudo systemctl status ssh`
   - Note your VPS IP address

3. **Connect from VS Code**:
   - Press `F1` or `Ctrl+Shift+P`
   - Type "Remote-SSH: Connect to Host"
   - Enter: `user@your-vps-ip`
   - Enter password or use SSH key
   - Open project folder on VPS
   - Develop as if files were local!

---

---

## Next Steps After Setup
1. Configure application.properties with your specific settings
2. Set up any required API keys or authentication
3. Test the CRUD generation with sample prompts
4. Review and customize the generated code templates if needed
5. Set up logging and monitoring
6. Configure production settings for VPS deployment

---

## Summary Checklist

### For VPS Development (RECOMMENDED)
- [ ] VPS accessible via SSH
- [ ] JDK 17 installed on VPS and verified
- [ ] Maven installed on VPS and verified
- [ ] PostgreSQL installed, database created, and service running on VPS
- [ ] Git installed on VPS (if needed)
- [ ] Swap space configured (4GB recommended)
- [ ] Ollama installed and Qwen2.5-Coder model downloaded on VPS
- [ ] Project cloned/uploaded to VPS
- [ ] application.properties configured on VPS
- [ ] Project built successfully with Maven on VPS
- [ ] VS Code Remote SSH configured (optional but recommended)
- [ ] Application runs without errors on VPS
- [ ] Database connection verified
- [ ] Ollama/Qwen2.5-Coder connection verified
- [ ] Memory usage monitored and optimized
- [ ] Application accessible and functional

### For Windows Development (Limited - Not Recommended for LLM)
- [ ] JDK 17 installed and verified
- [ ] Maven installed and verified
- [ ] PostgreSQL installed, database created, and service running
- [ ] Git installed (if needed)
- [ ] Ollama installed (⚠️ May not work properly with 8GB RAM)
- [ ] Qwen2.5-Coder model downloaded (⚠️ May cause system issues)
- [ ] Project downloaded/cloned
- [ ] application.properties configured
- [ ] Project built successfully with Maven
- [ ] Application runs without errors (may be unstable with LLM)
- [ ] Database connection verified
- [ ] Ollama/Qwen2.5-Coder connection verified (if system allows)
- [ ] Application accessible and functional

---

## Final Recommendation

**For your setup (Windows 8GB RAM, VPS Ubuntu 8GB RAM):**

✅ **DO**: Set up and develop on VPS using VS Code Remote SSH
- Full LLM functionality
- Better performance
- Production-like environment
- Comfortable development experience

❌ **DON'T**: Try to run Qwen2.5-Coder on Windows 8GB RAM
- Insufficient memory
- System instability
- Poor performance
- Frequent crashes

**Use Windows setup only for learning project structure or basic Spring Boot development without LLM features.**

