#!/usr/bin/env bash
# ============================================================
# TicketStorm — AWS Deploy (EC2 + Docker Compose + Bedrock)
# Provisions EC2 via Terraform, builds services, deploys.
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TF_DIR="$PROJECT_ROOT/terraform"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step()  { echo -e "\n${CYAN}━━━ $* ━━━${NC}"; }

# ------------------------------------------------------------
# 0. Pre-flight checks
# ------------------------------------------------------------
step "Pre-flight checks"

command -v aws       >/dev/null 2>&1 || error "aws CLI not found. Install: https://aws.amazon.com/cli/"
command -v terraform >/dev/null 2>&1 || error "terraform not found. Install: https://terraform.io"

if ! aws sts get-caller-identity &>/dev/null; then
    error "AWS credentials not configured. Run: aws configure"
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region 2>/dev/null || echo "us-east-2")
info "AWS Account: $ACCOUNT_ID | Region: $REGION"

# ------------------------------------------------------------
# 1. Terraform — provision EC2 + VPC + IAM
# ------------------------------------------------------------
step "Step 1/5 — Terraform: provisioning infrastructure"

cd "$TF_DIR"
terraform init -input=false

info "Planning infrastructure..."
terraform plan -out=tfplan -input=false

info "Applying Terraform (this takes ~2-3 minutes)..."
terraform apply -auto-approve tfplan
rm -f tfplan

PUBLIC_IP=$(terraform output -raw public_ip)
SSH_KEY="$TF_DIR/ticketstorm-key.pem"

info "EC2 instance created: $PUBLIC_IP"

# ------------------------------------------------------------
# 2. Wait for EC2 to be ready
# ------------------------------------------------------------
step "Step 2/5 — Waiting for EC2 instance to boot"

info "Waiting for SSH to become available..."
for i in $(seq 1 60); do
    if ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@"$PUBLIC_IP" "echo ok" 2>/dev/null; then
        info "SSH is ready!"
        break
    fi
    if [ "$i" -eq 60 ]; then
        error "Timeout waiting for SSH. Check EC2 console."
    fi
    echo -n "."
    sleep 10
done

# ------------------------------------------------------------
# 3. Wait for user-data to complete
# ------------------------------------------------------------
step "Step 3/5 — Waiting for server setup (Docker, Java, Maven)"

SSH_CMD="ssh -i $SSH_KEY -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP"

info "This may take 5-10 minutes on first deploy..."
for i in $(seq 1 120); do
    if $SSH_CMD "test -f /var/log/user-data.log && tail -1 /var/log/user-data.log | grep -q 'setup complete'" 2>/dev/null; then
        info "Server setup complete!"
        break
    fi
    if [ "$i" -eq 120 ]; then
        warn "Setup taking longer than expected. Checking logs..."
        $SSH_CMD "tail -20 /var/log/user-data.log" 2>/dev/null || true
        error "Timeout waiting for user-data. SSH in and check /var/log/user-data.log"
    fi
    echo -n "."
    sleep 10
done

# ------------------------------------------------------------
# 4. Start services with Docker Compose
# ------------------------------------------------------------
step "Step 4/5 — Starting TicketStorm services"

$SSH_CMD "cd /home/ubuntu/ticketstorm-app && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build" 2>&1

info "Waiting for services to be healthy (up to 3 minutes)..."
for i in $(seq 1 36); do
    HEALTHY=$($SSH_CMD "cd /home/ubuntu/ticketstorm-app && docker compose ps --format json 2>/dev/null | grep -c '\"running\"'" 2>/dev/null || echo "0")
    if [ "$HEALTHY" -ge 10 ]; then
        info "All services are running!"
        break
    fi
    if [ "$i" -eq 36 ]; then
        warn "Some services may still be starting. Checking status..."
        $SSH_CMD "cd /home/ubuntu/ticketstorm-app && docker compose ps" 2>/dev/null || true
    fi
    echo -n "."
    sleep 5
done

# ------------------------------------------------------------
# 5. Seed data
# ------------------------------------------------------------
step "Step 5/5 — Seeding database"

$SSH_CMD "cd /home/ubuntu/ticketstorm-app && \
  docker compose exec -T postgres psql -U ticketstorm -d ticketstorm -f /seed/seed-data.sql 2>/dev/null && \
  docker compose exec -T postgres psql -U ticketstorm -d ticketstorm -f /seed/seed-sections.sql 2>/dev/null" 2>&1 || \
  warn "Seed failed — run manually: docker compose exec postgres psql -U ticketstorm -d ticketstorm -f /seed/seed-data.sql"

# ------------------------------------------------------------
# Done!
# ------------------------------------------------------------
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║     TicketStorm deployed successfully! 🎉       ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║  App:     http://$PUBLIC_IP               ║${NC}"
echo -e "${GREEN}║  Grafana: http://$PUBLIC_IP:3001           ║${NC}"
echo -e "${GREEN}║  Region:  $REGION                          ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║  SSH: ssh -i terraform/ticketstorm-key.pem \\    ║${NC}"
echo -e "${GREEN}║       ubuntu@$PUBLIC_IP                    ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║  AI: Spring AI + Bedrock (Claude Haiku 4.5)     ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║  Tear down: ./scripts/aws-down.sh               ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
