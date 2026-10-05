#!/usr/bin/env bash
# ============================================================
# TicketStorm — AWS Infrastructure Destroy
# Tears down EC2 and all AWS resources via Terraform.
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TF_DIR="$PROJECT_ROOT/terraform"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ------------------------------------------------------------
# 0. Pre-flight checks
# ------------------------------------------------------------
command -v terraform >/dev/null 2>&1 || error "terraform not found"
command -v aws      >/dev/null 2>&1 || error "aws CLI not found"

if ! aws sts get-caller-identity &>/dev/null; then
    error "AWS credentials not configured"
fi

# ------------------------------------------------------------
# 1. Confirmation prompt
# ------------------------------------------------------------
echo ""
echo -e "${RED}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║         DESTRUCTIVE ACTION                      ║${NC}"
echo -e "${RED}║                                                  ║${NC}"
echo -e "${RED}║  This will permanently destroy:                  ║${NC}"
echo -e "${RED}║    • EC2 instance (ticketstorm-server)           ║${NC}"
echo -e "${RED}║    • Elastic IP                                  ║${NC}"
echo -e "${RED}║    • VPC, subnet, security groups                ║${NC}"
echo -e "${RED}║    • IAM role and policies                       ║${NC}"
echo -e "${RED}║    • SSH key pair                                ║${NC}"
echo -e "${RED}║    • All data on the server                      ║${NC}"
echo -e "${RED}║                                                  ║${NC}"
echo -e "${RED}║  This action is IRREVERSIBLE.                    ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════╝${NC}"
echo ""

read -p "Type 'destroy' to confirm destruction: " CONFIRM
if [[ "$CONFIRM" != "destroy" ]]; then
    info "Aborted — no resources destroyed."
    exit 0
fi

# ------------------------------------------------------------
# 2. Terraform destroy
# ------------------------------------------------------------
info "Destroying AWS infrastructure..."
cd "$TF_DIR"

if [[ -d ".terraform" ]]; then
    terraform destroy -auto-approve -input=false
else
    terraform init -input=false
    terraform destroy -auto-approve -input=false
fi

# ------------------------------------------------------------
# 3. Cleanup local files
# ------------------------------------------------------------
info "Cleaning up local files..."
rm -f "$TF_DIR/ticketstorm-key.pem"
rm -f "$TF_DIR/tfplan"

# ------------------------------------------------------------
# 4. Done
# ------------------------------------------------------------
echo ""
info "============================================="
info " All AWS resources destroyed successfully!"
info "============================================="
info ""
info " Resources removed:"
info "   • EC2 instance + EBS volume"
info "   • Elastic IP"
info "   • VPC + subnet + security groups"
info "   • IAM role + instance profile"
info "   • SSH key pair"
info ""
info " To re-deploy: ./scripts/aws-up.sh"
