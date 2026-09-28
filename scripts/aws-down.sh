#!/usr/bin/env bash
# ============================================================
# TicketStorm — AWS Infrastructure Destroy
# Tears down all AWS resources provisioned by Terraform.
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TF_DIR="$PROJECT_ROOT/terraform/environments/demo"

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
command -v aws >/dev/null 2>&1 || error "aws CLI not found"

if ! aws sts get-caller-identity &>/dev/null; then
    error "AWS credentials not configured"
fi

# ------------------------------------------------------------
# 1. Confirmation prompt
# ------------------------------------------------------------
echo ""
echo -e "${RED}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║         ⚠️  DESTRUCTIVE ACTION                  ║${NC}"
echo -e "${RED}║                                                  ║${NC}"
echo -e "${RED}║  This will permanently destroy:                  ║${NC}"
echo -e "${RED}║    • EKS cluster (ticketstorm)                   ║${NC}"
echo -e "${RED}║    • RDS database and all data                   ║${NC}"
echo -e "${RED}║    • VPC, subnets, security groups               ║${NC}"
echo -e "${RED}║    • All Kubernetes workloads                    ║${NC}"
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
# 2. Delete Kubernetes resources first (graceful cleanup)
# ------------------------------------------------------------
info "Removing Kubernetes workloads..."
if command -v kubectl &>/dev/null; then
    if kubectl cluster-info &>/dev/null 2>&1; then
        kubectl delete -k "$PROJECT_ROOT/k8s/overlays/demo" --ignore-not-found --timeout=120s 2>/dev/null || \
            warn "Kubernetes cleanup skipped (cluster may be unreachable)"
    else
        warn "Cannot reach cluster — skipping K8s cleanup"
    fi
else
    warn "kubectl not found — skipping K8s cleanup"
fi

# ------------------------------------------------------------
# 3. Terraform destroy
# ------------------------------------------------------------
info "Destroying Terraform infrastructure..."
cd "$TF_DIR"

if [[ -f ".terraform" ]]; then
    terraform init -input=false 2>/dev/null || warn "Terraform init had warnings"
    terraform destroy -auto-approve -input=false
else
    error "Terraform directory not initialized at $TF_DIR"
fi

# ------------------------------------------------------------
# 4. Cleanup kubeconfig
# ------------------------------------------------------------
info "Cleaning up kubeconfig entry for ticketstorm..."
kubectl config delete-context ticketstorm 2>/dev/null || true
kubectl config delete-cluster ticketstorm 2>/dev/null || true
kubectl config unset users.ticketstorm 2>/dev/null || true

# ------------------------------------------------------------
# 5. Done
# ------------------------------------------------------------
echo ""
info "============================================="
info " All AWS resources destroyed successfully!"
info "============================================="
info ""
info " The following AWS resources were removed:"
info "   • EKS cluster and node groups"
info "   • RDS PostgreSQL instance"
info "   • VPC and networking"
info "   • Security groups and IAM roles"
info "   • S3 buckets and CloudWatch logs"
info ""
info " Note: Some resources (e.g. CloudWatch log groups)"
info " may take a few minutes to fully disappear from AWS."
