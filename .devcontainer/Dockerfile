FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

# Install additional development tools
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        curl \
        wget \
        unzip \
        tree \
        htop \
        vim \
    && apt-get autoremove -y && apt-get clean -y && rm -rf /var/lib/apt/lists/*

# Ensure Gradle wrapper permissions
RUN echo 'alias ll="ls -la"' >> /home/vscode/.bashrc \
    && echo 'alias gradle="./gradlew"' >> /home/vscode/.bashrc

# Set proper permissions for the vscode user
USER vscode

# Verify Java installation
RUN java -version && javac -version 