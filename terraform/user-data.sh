#!/bin/bash
set -euxo pipefail

# Log everything to /var/log/user-data.log
exec > >(tee /var/log/user-data.log) 2>&1

echo "=== TicketStorm EC2 Setup ==="

# ─────────────────────────────────────────────
# System updates
# ─────────────────────────────────────────────
apt-get update -y
apt-get upgrade -y

# ─────────────────────────────────────────────
# Docker
# ─────────────────────────────────────────────
apt-get install -y ca-certificates curl gnupg lsb-release

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

# ─────────────────────────────────────────────
# Java 25 (Eclipse Temurin)
# ─────────────────────────────────────────────
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" \
  > /etc/apt/sources.list.d/adoptium.list

apt-get update -y
apt-get install -y temurin-25-jdk || {
  # Fallback: install via SDKMAN
  curl -s "https://get.sdkman.io" | bash
  source /root/.sdkman/bin/sdkman-init.sh
  sdk install java 25-tem
}

# ─────────────────────────────────────────────
# Maven
# ─────────────────────────────────────────────
apt-get install -y maven || {
  MAVEN_VERSION=3.9.9
  curl -fsSL "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" \
    | tar xz -C /opt
  ln -sf /opt/apache-maven-$MAVEN_VERSION/bin/mvn /usr/local/bin/mvn
}

# ─────────────────────────────────────────────
# Node.js 20 (for frontend build)
# ─────────────────────────────────────────────
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs

# ─────────────────────────────────────────────
# Git + utilities
# ─────────────────────────────────────────────
apt-get install -y git jq htop

# ─────────────────────────────────────────────
# Swap (safety net for builds)
# ─────────────────────────────────────────────
fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# ─────────────────────────────────────────────
# Docker resource limits
# ─────────────────────────────────────────────
cat > /etc/docker/daemon.json <<'DOCKEREOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 65536,
      "Soft": 65536
    }
  }
}
DOCKEREOF

systemctl restart docker

# ─────────────────────────────────────────────
# Clone and build TicketStorm
# ─────────────────────────────────────────────
cd /home/ubuntu
git clone https://github.com/ingmarma/ticketstorm-app.git
cd ticketstorm-app

# Build all Java services
mvn package -DskipTests -q -T 2C 2>&1 || mvn package -DskipTests -q 2>&1

# Build frontend
cd frontend
npm ci
npm run build
cd ..

# Fix ownership
chown -R ubuntu:ubuntu /home/ubuntu/ticketstorm-app

echo "=== TicketStorm setup complete ==="
echo "Run: cd /home/ubuntu/ticketstorm-app && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
