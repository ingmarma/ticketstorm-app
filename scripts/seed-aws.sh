#!/usr/bin/env bash
# ============================================================
# TicketStorm — Seed Data for AWS Deployment
# Loads seed SQL into RDS and generates embeddings via Bedrock.
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TF_DIR="$PROJECT_ROOT/terraform/environments/demo"
SEED_SQL="$SCRIPT_DIR/seed-data.sql"
SEED_EMBED="$SCRIPT_DIR/seed-embeddings.py"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ------------------------------------------------------------
# 0. Pre-flight
# ------------------------------------------------------------
command -v psql >/dev/null 2>&1 || error "psql client not found. Install: postgresql-client"
command -v python3 >/dev/null 2>&1 || error "python3 not found"

cd "$TF_DIR"

# ------------------------------------------------------------
# 1. Get RDS endpoint from Terraform output
# ------------------------------------------------------------
info "Fetching RDS endpoint from Terraform output..."
RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null || \
    terraform output -json | python3 -c "import sys,json; print(json.load(sys.stdin)['rds_endpoint']['value'])" 2>/dev/null || \
    echo "")

if [[ -z "$RDS_ENDPOINT" ]]; then
    error "Could not retrieve rds_endpoint from Terraform output"
fi
info "RDS endpoint: $RDS_ENDPOINT"

# Extract host and port
RDS_HOST="${RDS_ENDPOINT%%:*}"
RDS_PORT="${RDS_ENDPOINT##*:}"
RDS_PORT="${RDS_PORT:-5432}"

DB_NAME="${DB_NAME:-ticketstorm}"
DB_USER="${DB_USER:-ticketstorm}"
DB_PASSWORD="${DB_PASSWORD:-$(terraform output -raw rds_password 2>/dev/null || echo 'ticketstorm')}"

# ------------------------------------------------------------
# 2. Load seed SQL into RDS
# ------------------------------------------------------------
if [[ ! -f "$SEED_SQL" ]]; then
    error "Seed SQL not found at $SEED_SQL"
fi

info "Waiting for RDS to accept connections..."
for i in $(seq 1 30); do
    if PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" &>/dev/null; then
        break
    fi
    if [[ $i -eq 30 ]]; then
        error "RDS not reachable after 30 attempts"
    fi
    info "  Attempt $i/30 — waiting 10s..."
    sleep 10
done

info "Loading seed data into RDS..."
PGPASSWORD="$DB_PASSWORD" psql \
    -h "$RDS_HOST" \
    -p "$RDS_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -f "$SEED_SQL" \
    --set ON_ERROR_STOP=on

info "Seed data loaded successfully"

# Verify counts
info "Verifying data..."
PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT 'venues' AS table_name, count(*) AS rows FROM venues
UNION ALL SELECT 'events', count(*) FROM events
UNION ALL SELECT 'event_sections', count(*) FROM event_sections
UNION ALL SELECT 'ticket_stock', count(*) FROM ticket_stock;
"

# ------------------------------------------------------------
# 3. Run embedding generation via Bedrock
# ------------------------------------------------------------
if [[ ! -f "$SEED_EMBED" ]]; then
    error "Embedding script not found at $SEED_EMBED"
fi

info "Checking Python dependencies..."
python3 -c "import psycopg2" 2>/dev/null || {
    info "Installing psycopg2-binary..."
    pip3 install psycopg2-binary --quiet
}
python3 -c "import boto3" 2>/dev/null || {
    info "Installing boto3..."
    pip3 install boto3 --quiet
}

info "Generating embeddings via Bedrock..."
DB_HOST="$RDS_HOST" DB_PORT="$RDS_PORT" DB_NAME="$DB_NAME" \
DB_USER="$DB_USER" DB_PASSWORD="$DB_PASSWORD" \
    python3 "$SEED_EMBED" --provider bedrock

# ------------------------------------------------------------
# 4. Done
# ------------------------------------------------------------
echo ""
info "============================================="
info " AWS seed complete!"
info "============================================="
info ""
info " Database: $RDS_HOST:$RDS_PORT/$DB_NAME"
info " Embeddings generated via Amazon Bedrock"
