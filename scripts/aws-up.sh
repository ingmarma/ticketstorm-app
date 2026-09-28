#!/usr/bin/env bash
# ============================================================
# TicketStorm — AWS Infrastructure Deploy
# Provisions EKS, RDS, networking, deploys K8s workloads,
# and seeds data.
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TF_DIR="$PROJECT_ROOT/terraform/environments/demo"
K8S_OVERLAY="$PROJECT_ROOT/k8s/overlays/demo"

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
info "Checking prerequisites..."

command -v aws   >/dev/null 2>&1 || error "aws CLI not found. Install: https://aws.amazon.com/cli/"
command -v terraform >/dev/null 2>&1 || error "terraform not found. Install: https://terraform.io"
command -v kubectl  >/dev/null 2>&1 || error "kubectl not found. Install: https://kubernetes.io/docs/tasks/tools/"

# Verify AWS credentials
if ! aws sts get-caller-identity &>/dev/null; then
    error "AWS credentials not configured. Run: aws configure"
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region 2>/dev/null || echo "us-east-1")
info "AWS Account: $ACCOUNT_ID | Region: $REGION"

# ------------------------------------------------------------
# 1. Terraform — apply infrastructure
# ------------------------------------------------------------
info "Initializing Terraform..."
cd "$TF_DIR"
terraform init -input=false

info "Planning infrastructure changes..."
terraform plan -out=tfplan -input=false

info "Applying Terraform changes (this may take 15-20 minutes)..."
terraform apply -auto-approve tfplan
rm -f tfplan

# ------------------------------------------------------------
# 2. Configure kubectl for EKS
# ------------------------------------------------------------
info "Configuring kubectl for EKS cluster 'ticketstorm'..."
CLUSTER_NAME=$(terraform output -raw cluster_name 2>/dev/null || echo "ticketstorm")
aws eks update-kubeconfig \
    --name "$CLUSTER_NAME" \
    --region "$REGION" \
    --alias ticketstorm

# Verify connectivity
info "Verifying cluster connectivity..."
kubectl cluster-info || error "Cannot reach EKS cluster"
kubectl get nodes -o wide

# ------------------------------------------------------------
# 3. Deploy Kubernetes workloads
# ------------------------------------------------------------
info "Applying Kubernetes manifests (kustomize overlay: demo)..."
kubectl apply -k "$K8S_OVERLAY"

# ------------------------------------------------------------
# 4. Wait for pods to be ready
# ------------------------------------------------------------
info "Waiting for pods to become ready (timeout: 300s)..."
kubectl -n ticketstorm rollout status deployment/api-gateway   --timeout=300s 2>/dev/null || warn "api-gateway not found, skipping"
kubectl -n ticketstorm rollout status deployment/event-service --timeout=300s 2>/dev/null || warn "event-service not found, skipping"
kubectl -n ticketstorm rollout status deployment/reservation-service --timeout=300s 2>/dev/null || warn "reservation-service not found, skipping"

info "Pod status:"
kubectl -n ticketstorm get pods -o wide

# ------------------------------------------------------------
# 5. Seed data
# ------------------------------------------------------------
info "Running seed scripts against AWS..."
bash "$SCRIPT_DIR/seed-aws.sh"

# ------------------------------------------------------------
# 6. Print access info
# ------------------------------------------------------------
INGRESS_HOST=$(kubectl -n ticketstorm get ingress api-gateway -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
INGRESS_IP=$(kubectl -n ticketstorm get ingress api-gateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")

echo ""
info "============================================="
info " TicketStorm deployed successfully!"
info "============================================="
info ""
info " API Endpoint: http://${INGRESS_HOST:-$INGRESS_IP}"
info " EKS Cluster:  $CLUSTER_NAME"
info " Region:       $REGION"
info ""
info " Useful commands:"
info "   kubectl -n ticketstorm get pods"
info "   kubectl -n ticketstorm logs -f deployment/api-gateway"
info "   terraform -C $TF_DIR output"
info ""
info " To tear down:  ./scripts/aws-down.sh"
