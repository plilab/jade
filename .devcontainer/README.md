# Dev Container Setup for Jade Project

This Dev Container provides a complete Java development environment for the Jade project with:

## What's Included

- **Java 17** (Eclipse Temurin JDK)
- **Gradle 8.11.1** (matching your project version)
- **Kotlin support** for VS Code
- **Git** and **GitHub CLI**
- **Essential VS Code extensions** for Java/Kotlin development

## Getting Started

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [VS Code](https://code.visualstudio.com/)
- [Dev Containers Extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

### Setup Steps

1. **Open the project in VS Code**
2. **Open Command Palette** (`Ctrl+Shift+P` / `Cmd+Shift+P`)
3. **Run**: "Dev Containers: Reopen in Container"
4. **Wait** for the container to build (first time only)

### Verify Setup

Once the container is running, open the integrated terminal and run:

```bash
# Check Java version
java -version
# Should show: openjdk version "17.x.x"

# Check Gradle version  
./gradlew --version
# Should show: Gradle 8.11.1

# Test your playground
./gradlew playground:run
```

## Running Your Playground

```bash
# Run the playground application
./gradlew playground:run

# Run playground tests
./gradlew playground:test

# Build everything
./gradlew build
```

## Environment Details

- **JAVA_HOME**: `/usr/local/sdkman/candidates/java/current`
- **Gradle User Home**: `/workspace/.gradle` (persisted)
- **User**: `vscode`

## Troubleshooting

### Container won't start
- Ensure Docker Desktop is running
- Try "Dev Containers: Rebuild Container" from Command Palette

### Gradle permissions error
- The container automatically sets executable permissions on `gradlew`
- If issues persist, run: `chmod +x gradlew`

### Extensions not loading
- Reload VS Code window: "Developer: Reload Window"
- Check if Dev Containers extension is installed and enabled

## Benefits

✅ **No local Java/Gradle setup required**  
✅ **Consistent environment across team members**  
✅ **Isolated from host system**  
✅ **Automatic tooling and extension setup**  
✅ **Gradle cache persistence for faster builds** 