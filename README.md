<div align="center">

# ⚡ TicketStorm

### Sistema de Reservaciones de Alta Concurrencia

**Arquitectura backend a gran escala para venta de tickets en eventos masivos**

[![Java](https://img.shields.io/badge/Java-21%20LTS-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-GKE-326CE5?style=flat-square&logo=kubernetes&logoColor=white)](https://cloud.google.com/kubernetes-engine)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-231F20?style=flat-square&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.x-F778BA?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

**[🌐 Ver landing](https://TU-USUARIO.github.io/ticketstorm/) · [🏗️ System Design](https://TU-USUARIO.github.io/ticketstorm/docs/01_system_design_architecture.html) · [📖 Cuestionario](https://TU-USUARIO.github.io/ticketstorm/docs/06_cuestionario_evaluacion.html) · [🎥 Video](https://youtu.be/TU-VIDEO)**

</div>

---

## 📋 Sobre el proyecto

**TicketStorm** es el proyecto final del **Bootcamp de Arquitecturas Backend a Gran Escala con Java** de [Código Facilito](https://codigofacilito.com/).

Diseño arquitectónico completo de un sistema de reservación de tickets para eventos masivos, capaz de soportar picos de tráfico como los que ocurren en aperturas de venta de conciertos populares (el clásico *"Taylor Swift effect"*).

### Requisitos de negocio

| Métrica | Valor | Descripción |
|---------|:-----:|-------------|
| 👥 **DAU** | 50M | Usuarios diarios activos |
| 🔥 **Concurrentes** | 5M | Pico durante apertura de venta |
| 🎫 **Overbooking** | 0 | Restricción no-negociable del sistema |
| ⏱️ **Reserva** | 10 min | TTL de reserva temporal antes del pago |
| ⚡ **P99 Search** | <200ms | SLO de latencia de búsqueda |
| 💳 **P99 Checkout** | <500ms | SLO de latencia de reserva + pago |

---

## 🎯 Objetivos arquitectónicos

Este no es un CRUD de tickets. El diseño resuelve tres desafíos técnicos fundamentales:

1. **Anti-overbooking absoluto** — un asiento nunca puede venderse dos veces, ni bajo condiciones de race condition extrema con 5M usuarios compitiendo por el mismo asiento.
2. **Absorber picos de 5M concurrentes** — el sistema crítico nunca debe verse afectado por el spike; los usuarios esperan en una fila virtual desacoplada.
3. **Balancear consistencia y disponibilidad** — CP donde el negocio lo exige (reservas, pagos); AP donde el usuario prefiere velocidad (browse, search, notificaciones).

---

## 📚 Entregables

El proyecto consta de **6 documentos HTML interactivos**, más la presentación en video.

| # | Documento | Descripción |
|:-:|-----------|-------------|
| 01 | **[System Design Architecture](./docs/01_system_design_architecture.html)** | Diagrama completo de 7 capas con componentes clickeables y análisis de trade-offs. |
| 02 | **[DDD Bounded Contexts + Context Map](./docs/02_ddd_bounded_contexts_context_map.html)** | Descomposición Domain-Driven, ubiquitous language, Saga flow. |
| 03 | **[Data Model](./docs/03_data_model.html)** | Schemas PostgreSQL, estructuras Redis, Event Store, pgvector. |
| 04 | **[API Specification](./docs/04_api_spec.html)** | OpenAPI 3.0 con 11 endpoints core, WebSocket, request/response detallados. |
| 05 | **[Tech Stack & Trade-offs](./docs/05_tech_stack_tradeoffs.html)** | Justificación arquitectónica de cada decisión, alternativas comparadas. |
| 06 | **[Cuestionario de Evaluación](./docs/06_cuestionario_evaluacion.html)** | Respuestas técnicas a las 5 preguntas oficiales del bootcamp. |

> 💡 **Cómo navegar**: empezá por el [landing page](./index.html) para una vista general, o directo por el [System Design](./docs/01_system_design_architecture.html) si preferís entrar por la arquitectura.

---

## 🏗️ Vista rápida de la arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│                     CLIENTES (Web · Mobile · API)                │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTPS + WebSocket
┌────────────────────────────▼─────────────────────────────────────┐
│  EDGE   ·  Cloud Armor (WAF/DDoS)  ·  CDN  ·  Load Balancer      │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│  API GATEWAY  ·  Spring Cloud Gateway  ·  Keycloak  ·  Istio     │
└────────────────────────────┬─────────────────────────────────────┘
                             │ gRPC + REST + mTLS
┌────────────────────────────▼─────────────────────────────────────┐
│                    MICROSERVICIOS (Java 21)                      │
│                                                                  │
│  Catalog  ·  Queue  ·  Inventory  ·  Reservation                 │
│  Payment  ·  Notification  ·  AI Discovery                       │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│              EVENT BACKBONE  ·  Apache Kafka                     │
│      (particionado por eventId · exactly-once · Streams)         │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│                          DATA LAYER                              │
│                                                                  │
│  PostgreSQL  ·  Redis Cluster  ·  Elasticsearch  ·  pgvector     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔑 Decisiones arquitectónicas destacadas

### 🛡️ Anti-overbooking en 4 capas
Defense in depth: Lua script atómico en Redis → Redisson Redlock → `SELECT FOR UPDATE` en PostgreSQL → `UNIQUE` constraint sobre el Event Store. El overbooking es imposible por diseño, no por convención.

### 🚦 Fila virtual para 5M concurrentes
Redis Sorted Set con complejidad O(log N) + WebSocket streaming con **Virtual Threads de Java 21** (~1KB por conexión). El sistema crítico nunca ve el spike completo; el usuario espera sin hacer polling.

### 🎯 CAP híbrido por bounded context
- **CP** en Inventory, Reservation y Payment (consistencia no-negociable).
- **AP** en Catalog, Queue, Notification y AI Discovery (disponibilidad prioritaria).
- La *boundary CP/AP* está colocada deliberadamente en el momento de la reserva.

### 🔄 Saga coreografiado + Event Sourcing
Consistencia distribuida sin orquestador central. Cada servicio reacciona a eventos Kafka; las compensaciones son automáticas si un paso falla. Inventory y Reservation son event stores puros — el estado es una proyección del log.

### 🤖 Spring AI arquitectónico
- **Búsqueda semántica** con `text-embedding-004` + pgvector + índice HNSW.
- **Chatbot RAG multi-turn** con `ChatClient` + `QuestionAnswerAdvisor` + Gemini 1.5 Pro.
- **Fraud detection** en tiempo real sobre Vertex AI Custom Model.

### 📊 SRE-first observability
- **Prometheus + Grafana** con SLOs definidos y burn rate alerts multi-window.
- **OpenTelemetry** con W3C TraceContext propagado end-to-end.
- **Cloud Logging** con structured JSON y correlation IDs.

---

## 🛠️ Stack tecnológico

<details>
<summary><b>Runtime &amp; Framework</b></summary>

- Java 21 LTS con Virtual Threads (Project Loom)
- Spring Boot 3.x
- Spring WebFlux (solo Virtual Queue Service para WebSocket streaming)
- Structured Concurrency (JEP 453)

</details>

<details>
<summary><b>Persistencia</b></summary>

- PostgreSQL 16 (Cloud SQL) — write-side, Event Stores
- Redis Cluster 7 (Memorystore) — bitmaps, sorted sets, distributed locks
- Elasticsearch 8 — full-text search + faceted queries
- pgvector — embeddings semánticos (768 dim, HNSW)

</details>

<details>
<summary><b>Mensajería</b></summary>

- Apache Kafka (Confluent Platform en GKE) — event backbone principal
- GCP Pub/Sub — fan-out masivo de notificaciones
- gRPC + Protobuf — llamadas síncronas inter-servicios

</details>

<details>
<summary><b>Infraestructura</b></summary>

- GKE Autopilot (multi-zona `us-central1`)
- Istio Service Mesh (mTLS, tracing, canary)
- Spring Cloud Gateway (API Gateway)
- OCI (Oracle Cloud) como DR multi-cloud

</details>

<details>
<summary><b>Seguridad</b></summary>

- Keycloak (OIDC + OAuth2 + JWT RS256)
- OPA (Open Policy Agent) — authorization declarativa
- Cloud Armor (WAF + DDoS)
- GCP Secret Manager

</details>

<details>
<summary><b>Observabilidad</b></summary>

- Prometheus + Grafana + Alertmanager
- OpenTelemetry Java Agent → Google Cloud Trace
- Cloud Logging (structured JSON) → BigQuery

</details>

<details>
<summary><b>AI / ML</b></summary>

- Spring AI 1.x
- Vertex AI Gemini 1.5 Pro (LLM)
- text-embedding-004 (embeddings)
- Vertex AI Custom Model (fraud scoring)

</details>

<details>
<summary><b>Resiliencia</b></summary>

- Resilience4j (Circuit Breaker, Retry, Bulkhead, TimeLimiter)
- Outbox Pattern (transactional consistency Kafka ↔ DB)
- Saga coreografiado con transacciones compensatorias
- Dead Letter Topics + Idempotency Keys

</details>

---

## 📂 Estructura del repositorio

```
ticketstorm/
├── README.md                   ← Este documento
├── index.html                  ← Landing page navegable
├── docs/
│   ├── 01_system_design_architecture.html
│   ├── 02_ddd_bounded_contexts_context_map.html
│   ├── 03_data_model.html
│   ├── 04_api_spec.html
│   ├── 05_tech_stack_tradeoffs.html
│   └── 06_cuestionario_evaluacion.html
└── LICENSE
```

---

## 🚀 Cómo explorar el proyecto

### Opción 1 — Landing web
Abrí la [**página del proyecto**](https://TU-USUARIO.github.io/ticketstorm/) para una experiencia de navegación completa entre los 6 entregables.

### Opción 2 — Directo a los HTMLs
Cada documento es interactivo y auto-contenido:

```bash
git clone https://github.com/TU-USUARIO/ticketstorm.git
cd ticketstorm
open docs/01_system_design_architecture.html
```

### Opción 3 — Video de justificación
Ver el [**video de 18 minutos**](https://youtu.be/TU-VIDEO) donde recorro el diseño completo, con foco en las decisiones arquitectónicas clave y el análisis desde la perspectiva del CAP Theorem.

---

## 👤 Autor

**Matías Martínez** — SRE &amp; Backend Engineer en NFD S.A. (Paraguay 🇵🇾)

Este proyecto forma parte de mi camino hacia posiciones Senior/Arquitecto. Cubre los tópicos del bootcamp y agrega valor con:

- Integración arquitectónica de **Spring AI** como componente justificado (no como gimmick).
- Diseño **multi-cloud** (GCP + OCI) alineado con el stack real de NFD S.A.
- Enfoque **SRE-first** en observabilidad, SLOs y error budgets.
- Análisis de **PACELC** como complemento al CAP Theorem.

---

## 🎓 Sobre el Bootcamp

**Bootcamp de Arquitecturas Backend a Gran Escala con Java** — [Código Facilito](https://codigofacilito.com/)

Instructor: **Javier Ramírez** ([@javierbenek](https://twitter.com/javierbenek))

Fecha de entrega: 27 de julio de 2026.

---

## 📄 Licencia

Este proyecto está publicado bajo licencia [MIT](LICENSE). Sos libre de estudiarlo, forkearlo, adaptarlo y usarlo como referencia.

---

<div align="center">

**⭐ Si el proyecto te resultó útil, dejá una estrella al repo — ayuda a que llegue a más personas.**

</div>