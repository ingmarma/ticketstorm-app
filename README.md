# TicketStorm

**High-performance ticket reservation system designed for 50M+ DAU.**

TicketStorm is an event ticketing platform built for the Paraguayan market, capable of handling massive flash-sale scenarios with sub-second response times. It supports semantic event search via vector embeddings, real-time inventory management, and concurrent reservation handling with optimistic locking.

---

## Architecture

```
                         ┌─────────────────────┐
                         │      CloudFront      │
                         │     (CDN / WAF)      │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │     API Gateway       │
                         │   (Spring Cloud GW)   │
                         └──────────┬───────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
     ┌──────────▼────────┐ ┌───────▼────────┐ ┌───────▼────────┐
     │   Event Service   │ │Reservation Svc │ │  User Service  │
     │  (Java / Spring)  │ │ (Java / Spring)│ │ (Java / Spring)│
     └────────┬──────────┘ └───────┬────────┘ └───────┬────────┘
              │                    │                   │
     ┌────────▼──────────┐        │          ┌───────▼────────┐
     │   PostgreSQL       │◄───────┘          │   PostgreSQL    │
     │   (RDS / pgvector) │                  │   (RDS)         │
     └────────┬──────────┘                  └────────────────┘
              │
     ┌────────▼──────────┐
     │   Ollama / Bedrock │
     │  (Embeddings)      │
     └───────────────────┘
```

**Key Design Decisions:**
- Microservices architecture with independently scalable services
- PostgreSQL with pgvector for hybrid relational + vector search
- Optimistic locking (`version` column) for concurrent ticket reservations
- Spring Boot 4.x on Java 25 with virtual threads (Project Loom)
- Event-driven architecture with Kafka for reservation state propagation

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 25+ | Application runtime |
| Maven | 3.9+ | Build tool |
| Docker | 24+ | Containerization |
| Docker Compose | 2.20+ | Local orchestration |
| PostgreSQL client | 16+ | Database (via Docker) |
| Python | 3.11+ | Embedding scripts |
| AWS CLI | v2 | AWS deployment (optional) |
| Terraform | 1.5+ | IaC (optional) |
| kubectl | 1.30+ | Kubernetes (optional) |

---

## Quick Start

```bash
# Clone and start everything
git clone https://github.com/your-org/ticketstorm.git
cd ticketstorm

# Start infrastructure + application
docker compose up -d

# Wait for health checks
docker compose ps

# Seed the database
docker compose exec api psql -U ticketstorm -d ticketstorm -f /app/seed-data.sql

# Access the API
curl http://localhost:8080/api/events
```

### Using Make

```bash
make setup          # Build all services
make up             # Start docker compose
make down           # Stop everything
make test           # Run all tests
make seed           # Load seed data
make seed-embed     # Generate embeddings
```

---

## Running Tests

```bash
# Unit tests (per service)
cd services/event-service
mvn test

# Integration tests (requires Docker)
cd services/event-service
mvn verify -Pintegration

# All tests across all services
mvn test -pl services/*

# Load tests (requires k6)
k6 run tests/load/flash-sale.js
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.x |
| Build | Maven |
| API | REST + WebSocket |
| Database | PostgreSQL 16 + pgvector |
| Vector Search | pgvector / Ollama / Bedrock |
| Cache | Redis Cluster |
| Messaging | Apache Kafka |
| Container | Docker |
| Orchestration | Kubernetes (EKS) |
| IaC | Terraform |
| Monitoring | Prometheus + Grafana |
| CI/CD | GitHub Actions |

---

## AWS Deployment

### Prerequisites
```bash
aws configure                    # Set credentials
aws eks get-token --name ticketstorm  # Verify EKS access
```

### Deploy
```bash
# Provision infrastructure + deploy + seed
./scripts/aws-up.sh
```

The script will:
1. Initialize and apply Terraform (EKS, RDS, VPC, IAM)
2. Configure kubectl for the EKS cluster
3. Deploy all Kubernetes manifests via kustomize
4. Wait for pods to become ready
5. Load seed data into RDS and generate embeddings via Bedrock

### Tear Down
```bash
./scripts/aws-down.sh
```

Requires typing `destroy` to confirm — this is irreversible.

---

## API Endpoints

```
GET    /api/events              List events (paginated, filterable)
GET    /api/events/{id}         Get event details with sections
GET    /api/events/search?q=    Semantic search (vector embeddings)
POST   /api/reservations        Create reservation
GET    /api/reservations/{id}   Get reservation status
DELETE /api/reservations/{id}   Cancel reservation
POST   /api/v1/chat             AI assistant chat (RAG over indexed events)
GET    /api/v1/chat/suggestions Suggested questions for the AI assistant
GET    /api/health              Health check
GET    /api/metrics             Prometheus metrics
```

The AI assistant runs in `ai-discovery-service` on port **8087** (`/actuator/health`,
`/api/v1/chat`, `/api/v1/chat/suggestions`). It indexes events from `event-catalog`
into pgvector at startup and re-indexes every 10 minutes. Models are served by the
`ollama` container and pulled on first boot by `ollama-init`
(`llama3.2` for chat, `nomic-embed-text` for embeddings).

---

## Seed Data

The seed scripts populate the database with:

- **10 venues** across Asunción, Paraguay
- **20 events** (concerts, theater, sports, festivals) — Oct/Dec 2026
- **~70 ticket sections** with Guaraní (₲) pricing
- **Realistic scarcity** — some events at 95-98% sold out
- **3 demo users** (admin, regular, VIP)
- **Semantic embeddings** via Ollama (local) or Bedrock (AWS)

### Prices (Guaraníes)
| Section | Price Range |
|---------|-------------|
| VIP | ₲350,000 – ₲500,000 |
| Platea A | ₲180,000 – ₲250,000 |
| Platea B | ₲120,000 – ₲150,000 |
| General | ₲50,000 – ₲100,000 |
| Palcos | ₲350,000 – ₲500,000 |

---

## License

MIT License. See [LICENSE](LICENSE) for details.
