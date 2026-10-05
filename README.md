# TicketStorm

**Sistema de reserva de entradas de alto rendimiento diseñado para 50M+ DAU con cero sobreventa.**

TicketStorm es una plataforma de ticketing para eventos construida con microservicios en Java 25 y Spring Boot 4.1. Integra búsqueda semántica con Spring AI, gestión de inventario en tiempo real con Redis, y procesamiento de reservas concurrentes con cuatro capas anti-sobreventa. Diseñada para el mercado paraguayo con precios en Guaraníes (₲).

> Presentado en [AWS Community Day Paraguay 2026](https://awscommunitydaypyco.com) — *"Spring AI + AWS Bedrock: Integrando Modelos de Lenguaje en Microservicios Java en Producción"*

---

## Arquitectura

```
                    ┌──────────────────────────┐
                    │     React 19 + Vite 6     │
                    │   (TanStack Query, Zustand)│
                    └────────────┬──────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                   │
   ┌──────────▼────────┐ ┌──────▼───────┐ ┌────────▼────────┐
   │  Event Catalog    │ │ AI Discovery │ │  Virtual Queue  │
   │    (port 8081)    │ │  (port 8087) │ │   (port 8083)   │
   └──────────┬────────┘ └──────┬───────┘ └────────┬────────┘
              │                 │                   │
   ┌──────────▼────────┐ ┌─────▼────────┐ ┌───────▼─────────┐
   │    Inventory      │ │  Reservation │ │     Payment     │
   │    (port 8082)    │ │  (port 8084) │ │   (port 8085)   │
   └──────────┬────────┘ └──────┬───────┘ └────────┬────────┘
              │                 │                   │
              │          ┌──────▼───────┐           │
              │          │ Notification │           │
              │          │  (port 8086) │           │
              │          └──────────────┘           │
              │                                     │
   ┌──────────▼──────────────────────────────────────▼──────┐
   │                    Infrastructure                      │
   │  PostgreSQL (pgvector) · Redis · Kafka · Ollama/Bedrock│
   │  Prometheus · Grafana                                  │
   └────────────────────────────────────────────────────────┘
```

### 7 Microservicios con DDD Bounded Contexts

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| **Event Catalog** | 8081 | CRUD de eventos, búsqueda, secciones de tickets |
| **Inventory** | 8082 | Stock atómico, reserva de asientos, control de disponibilidad |
| **Virtual Queue** | 8083 | Cola virtual con Redis Sorted Set + WebSocket |
| **Reservation** | 8084 | Orquestación de reservas, Saga pattern |
| **Payment** | 8085 | Procesamiento de pagos (simulado para demo) |
| **Notification** | 8086 | Notificaciones por email/push vía Kafka |
| **AI Discovery** | 8087 | Chatbot RAG con Spring AI, búsqueda semántica |

### Patrones de Diseño

- **CQRS + Event Sourcing** — separación de comandos y consultas
- **Choreographed Saga** sobre Kafka + Outbox Pattern
- **Anti-sobreventa en 4 capas**: Lua atómico → Redisson Redlock → `SELECT FOR UPDATE` → `UNIQUE` constraint
- **Virtual Queue**: Redis Sorted Set O(log N) + WebSocket + Virtual Threads

---

## AI Discovery — Spring AI + RAG

El servicio estrella del proyecto. Implementa un chatbot con Retrieval-Augmented Generation:

- **Spring AI 2.0 GA** con `ChatClient` + vector store
- **RAG Pipeline**: indexa eventos desde Event Catalog → embeddings en pgvector → búsqueda semántica
- **ChatIntentResolver**: detección determinística de intents (BUY, VIEW_SECTIONS, VIEW_EVENT) con matching accent-insensitive
- **Modelos**: Ollama (local) con `llama3.2` / `nomic-embed-text`, o **AWS Bedrock** (producción) con Claude Haiku 4.5 + Titan Embed Text V2

```
Usuario: "¿Qué conciertos hay en noviembre?"
    ↓
ChatIntentResolver → detecta intent VIEW_EVENT
    ↓
VectorStore.similaritySearch() → busca en pgvector
    ↓
ChatClient.prompt() → genera respuesta con contexto RAG
    ↓
ChatResponse { events, sections, action, sessionId }
```

---

## Tech Stack

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 25 (Virtual Threads) |
| Framework | Spring Boot 4.1.0 + Spring AI 2.0.1 |
| Frontend | React 19, Vite 6, TanStack Query v5, Zustand v5, Tailwind CSS v4 |
| Base de datos | PostgreSQL 16 + pgvector |
| Cache | Redis 7 |
| Mensajería | Apache Kafka 3.8 (KRaft mode) |
| AI/ML | Ollama (local) / AWS Bedrock (producción): Claude Haiku 4.5 + Titan Embed Text V2 |
| Monitoreo | Prometheus + Grafana (3 dashboards pre-provisioned) |
| Contenedores | Docker + Docker Compose |
| IaC | Terraform |

---

## Inicio Rápido

### Prerrequisitos

- Java 25+ (recomendado via [SDKMAN](https://sdkman.io/))
- Maven 3.9+
- Docker 24+ con Docker Compose v2
- Node.js 20+ con npm

### Levantar todo

```bash
# Clonar
git clone https://github.com/ingmarma/ticketstorm-app.git
cd ticketstorm-app

# Compilar los 7 servicios
mvn package -DskipTests -q

# Levantar infraestructura + servicios
docker compose up -d

# Verificar que todo esté healthy
docker compose ps

# Seed de datos (10 eventos + 51 secciones)
docker compose exec -T postgres psql -U ticketstorm -d ticketstorm < scripts/seed-data.sql
docker compose exec -T postgres psql -U ticketstorm -d ticketstorm < scripts/seed-sections.sql
```

### Con Ollama (chatbot AI)

```bash
# Levantar con el profile de Ollama
docker compose --profile ollama up -d

# Primera vez: Ollama descarga los modelos (~2GB)
# Verificar que AI Discovery esté healthy
curl http://localhost:8087/actuator/health
```

### Acceder

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:5173 |
| Grafana | http://localhost:3001 (admin/admin) |
| Prometheus | http://localhost:9090 |
| API Eventos | http://localhost:8081/api/v1/events |
| AI Chat | http://localhost:8087/api/v1/chat |

---

## API Endpoints

### Event Catalog (8081)
```
GET    /api/v1/events                  Listar eventos (paginado)
GET    /api/v1/events/{id}             Detalle de evento
GET    /api/v1/events/{id}/sections    Secciones de tickets
GET    /api/v1/events/search?q=        Búsqueda por texto
```

### AI Discovery (8087)
```
POST   /api/v1/chat                    Chat con el asistente AI (RAG)
GET    /api/v1/chat/suggestions        Preguntas sugeridas
GET    /actuator/health                Health check
GET    /actuator/prometheus            Métricas Prometheus
```

---

## Datos de Ejemplo

10 eventos reales del mercado paraguayo con 51 secciones de tickets:

| Evento | Categoría | Venue | Capacidad | Precio desde |
|--------|-----------|-------|-----------|-------------|
| Coldplay - Music of the Spheres | CONCERT | Estadio Defensores del Chaco | 42,000 | ₲120.000 |
| Bad Bunny - Most Wanted Tour | CONCERT | Arena SAP | 8,500 | ₲120.000 |
| Paraguay vs Argentina - Eliminatorias | SPORTS | Estadio Defensores del Chaco | 42,000 | ₲100.000 |
| Romeo Santos - Fórmula Vol. 4 Tour | CONCERT | Centro de Exposiciones | 12,000 | ₲250.000 |
| Festival Asunciónico 2026 | FESTIVAL | Jockey Club Paraguay | 5,000 | ₲200.000 |
| El Fantasma de la Ópera | THEATER | Teatro Municipal de Asunción | 2,200 | ₲80.000 |
| Rock del Paraguay | CONCERT | Arena Vila Morra | 8,500 | ₲65.000 |
| Copa América de Básquetbol | SPORTS | Polideportivo CND | 15,000 | ₲70.000 |
| Festival Gastronómico Internacional | FESTIVAL | Centro de Exposiciones | 12,000 | ₲70.000 |
| Reveillon Asunción 2027 | FESTIVAL | Anfiteatro Ñandutí | 3,500 | ₲80.000 |

---

## Monitoreo — Grafana Dashboards

3 dashboards pre-provisioned en la carpeta "TicketStorm":

1. **Overview** — health de los 7 servicios, request rate, P99 latency, error rate, JVM memory
2. **AI Discovery** — métricas del chatbot, Spring AI latency, RAG pipeline, token usage
3. **Queue & Reservations** — cola virtual, pipeline de reservas, métricas de Redis/Kafka

---

## Despliegue en AWS

### Arquitectura Cloud

El despliegue en AWS utiliza una instancia EC2 con Docker Compose, manteniendo la misma topología que desarrollo local pero reemplazando Ollama por AWS Bedrock para inferencia AI.

| Componente | Detalle |
|-----------|---------|
| **EC2** (m7i-flex.large) | Instancia con 8GB RAM, 2 vCPUs — ejecuta los 7 microservicios + infra via Docker Compose |
| **AWS Bedrock** | Claude Haiku 4.5 (chat vía Converse API) + Titan Embed Text V2 (embeddings 1024d) |
| **IAM Role** | Rol de instancia con política `AmazonBedrockFullAccess` para acceso a modelos |
| **Terraform** | IaC completo: VPC, Security Groups, EC2, IAM, Elastic IP |
| **Docker Compose** | Producción usa overlay: `docker-compose.yml` + `docker-compose.prod.yml` |

### Perfiles Spring

El servicio AI Discovery usa `SPRING_PROFILES_ACTIVE=aws` en producción, lo que activa:
- `bedrock-converse` como proveedor de chat (Claude Haiku 4.5 con inference profile `us.`)
- `bedrock-titan` como proveedor de embeddings (Titan Embed Text V2, 1024 dimensiones)
- Desactiva Ollama automáticamente

### Deploy

```bash
# Provisionar infraestructura + desplegar todo
./scripts/aws-up.sh

# El script ejecuta:
# 1. Terraform init + apply (VPC, EC2, IAM, Elastic IP)
# 2. Espera SSH + user-data setup (Docker, Java, Maven)
# 3. Docker Compose build + up (con overlay prod)
# 4. Seed de datos (10 eventos + 51 secciones)

# Destruir infraestructura
./scripts/aws-down.sh
```

---

## Estructura del Proyecto

```
ticketstorm/
├── frontend/                    # React 19 + Vite 6
│   └── src/
│       ├── features/            # Páginas por feature
│       │   ├── events/          # EventListPage, EventDetailPage
│       │   ├── chat/            # ChatPanel (AI Assistant)
│       │   ├── checkout/        # CheckoutPage (pago simulado)
│       │   ├── queue/           # QueuePage
│       │   └── admin/           # AdminDashboard
│       ├── components/          # Componentes compartidos
│       ├── hooks/               # Custom hooks (useEventSearch, useQueue)
│       ├── services/            # API client
│       ├── store/               # Zustand store
│       └── types/               # TypeScript types
├── services/
│   ├── common-lib/              # DTOs, excepciones, utilidades compartidas
│   ├── event-lib/               # Modelos de dominio de eventos
│   ├── test-lib/                # Utilidades de testing
│   ├── event-catalog-service/   # Catálogo de eventos + búsqueda
│   ├── inventory-service/       # Control de stock atómico
│   ├── virtual-queue-service/   # Cola virtual Redis + WebSocket
│   ├── reservation-service/     # Orquestación de reservas
│   ├── payment-service/         # Procesamiento de pagos
│   ├── notification-service/    # Notificaciones async
│   └── ai-discovery-service/    # Chatbot RAG con Spring AI
├── infrastructure/
│   ├── prometheus/              # Configuración Prometheus
│   └── grafana/                 # Dashboards + provisioning
├── scripts/                     # SQL seeds, deploy scripts (aws-up.sh, aws-down.sh)
├── terraform/                   # IaC: VPC, EC2, IAM, Security Groups
├── docker-compose.yml           # Orquestación local
├── docker-compose.prod.yml      # Override para AWS (Bedrock, nginx frontend)
└── pom.xml                      # Parent POM (Maven multi-module)
```

---

## Autor

**Matías Martínez** — SRE & Backend Engineer

- GitHub: [@ingmarma](https://github.com/ingmarma)

---

## Licencia

MIT License. Ver [LICENSE](LICENSE) para detalles.
