#!/usr/bin/env python3
"""
TicketStorm — Seed Embeddings Script
Generates semantic embeddings for events via Ollama (local) or Bedrock (AWS).
Inserts into the event_embeddings table in PostgreSQL.
"""

import sys
import json
import time
import argparse
import logging
from typing import Optional

import requests
import psycopg2
from psycopg2.extras import execute_values

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("seed-embeddings")

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

OLLAMA_URL = "http://localhost:11434"
BEDROCK_REGION = "us-east-1"
EMBED_MODEL = "nomic-embed-text"
EMBED_DIMENSION = 768

DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "dbname": "ticketstorm",
    "user": "ticketstorm",
    "password": "ticketstorm",
}


# ---------------------------------------------------------------------------
# Ollama embedding provider
# ---------------------------------------------------------------------------

def get_embedding_ollama(text: str) -> list[float]:
    """Call Ollama API to generate an embedding."""
    resp = requests.post(
        f"{OLLAMA_URL}/api/embed",
        json={"model": EMBED_MODEL, "input": text},
        timeout=30,
    )
    resp.raise_for_status()
    data = resp.json()
    return data["embeddings"][0]


def wait_for_ollama(max_retries: int = 15, delay: float = 2.0) -> None:
    """Wait until Ollama is reachable and the model is available."""
    for attempt in range(1, max_retries + 1):
        try:
            r = requests.get(f"{OLLAMA_URL}/api/tags", timeout=5)
            if r.status_code == 200:
                models = [m["name"] for m in r.json().get("models", [])]
                if any(EMBED_MODEL in m for m in models):
                    log.info("Ollama ready — model '%s' found", EMBED_MODEL)
                    return
                log.warning("Ollama reachable but model not pulled yet (attempt %d/%d)", attempt, max_retries)
            else:
                log.warning("Ollama responded %d (attempt %d/%d)", r.status_code, attempt, max_retries)
        except requests.ConnectionError:
            log.warning("Cannot reach Ollama (attempt %d/%d)", attempt, max_retries)
        time.sleep(delay)
    log.error("Ollama not available after %d attempts — exiting", max_retries)
    sys.exit(1)


# ---------------------------------------------------------------------------
# Bedrock embedding provider (AWS)
# ---------------------------------------------------------------------------

def get_embedding_bedrock(text: str, bedrock_client) -> list[float]:
    """Call Amazon Bedrock to generate an embedding using Titan Embed Text."""
    body = json.dumps({"inputText": text})
    resp = bedrock_client.invoke_model(
        modelId="amazon.titan-embed-text-v1",
        body=body,
        contentType="application/json",
        accept="application/json",
    )
    result = json.loads(resp["body"].read())
    return result["embedding"]


def get_bedrock_client():
    """Create a Bedrock runtime client."""
    try:
        import boto3
        return boto3.client("bedrock-runtime", region_name=BEDROCK_REGION)
    except ImportError:
        log.error("boto3 not installed — run: pip install boto3")
        sys.exit(1)


# ---------------------------------------------------------------------------
# Database helpers
# ---------------------------------------------------------------------------

def get_db_connection():
    """Open a PostgreSQL connection using DB_CONFIG."""
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        return conn
    except psycopg2.OperationalError as e:
        log.error("Cannot connect to PostgreSQL: %s", e)
        sys.exit(1)


def fetch_events(conn) -> list[dict]:
    """Fetch all active events with venue info."""
    query = """
        SELECT e.id, e.title, e.description, e.event_type,
               e.event_date, v.name AS venue_name, v.city
        FROM events e
        JOIN venues v ON e.venue_id = v.id
        WHERE e.status = 'ACTIVE'
        ORDER BY e.event_date;
    """
    with conn.cursor() as cur:
        cur.execute(query)
        cols = [desc[0] for desc in cur.description]
        return [dict(zip(cols, row)) for row in cur.fetchall()]


def build_embedding_text(event: dict) -> str:
    """Build a rich text string to embed for semantic search."""
    return (
        f"{event['title']}. "
        f"{event['description']}. "
        f"Tipo: {event['event_type']}. "
        f"Lugar: {event['venue_name']}, {event['city']}. "
        f"Fecha: {event['event_date'].isoformat()}."
    )


def insert_embeddings(conn, rows: list[tuple]) -> int:
    """Batch-insert embeddings into event_embeddings."""
    query = """
        INSERT INTO event_embeddings (event_id, embedding, model)
        VALUES %s
        ON CONFLICT (event_id) DO UPDATE
          SET embedding = EXCLUDED.embedding,
              model = EXCLUDED.model,
              updated_at = now();
    """
    with conn.cursor() as cur:
        execute_values(cur, query, rows, page_size=50)
    return len(rows)


def embeddings_exist(conn) -> set:
    """Return set of event_ids that already have embeddings."""
    with conn.cursor() as cur:
        cur.execute("SELECT event_id FROM event_embeddings")
        return {row[0] for row in cur.fetchall()}


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Seed event embeddings")
    parser.add_argument("--provider", choices=["ollama", "bedrock"], default="ollama",
                        help="Embedding provider (default: ollama)")
    parser.add_argument("--skip-existing", action="store_true", default=True,
                        help="Skip events that already have embeddings")
    parser.add_argument("--force", action="store_true",
                        help="Regenerate all embeddings (overrides --skip-existing)")
    args = parser.parse_args()

    log.info("Provider: %s | Model: %s", args.provider, EMBED_MODEL)

    # Connect to database
    conn = get_db_connection()
    log.info("Connected to PostgreSQL")

    # Fetch events
    events = fetch_events(conn)
    log.info("Found %d active events", len(events))

    if not events:
        log.warning("No events found — nothing to do")
        conn.close()
        return

    # Determine which events need embeddings
    existing = set()
    if args.skip_existing and not args.force:
        existing = embeddings_exist(conn)
        log.info("Events with existing embeddings: %d / %d", len(existing), len(events))

    # Set up embedding provider
    bedrock_client = None
    if args.provider == "bedrock":
        bedrock_client = get_bedrock_client()
        log.info("Bedrock client initialized (region: %s)", BEDROCK_REGION)
    else:
        wait_for_ollama()

    # Generate embeddings
    rows = []
    skipped = 0
    errors = 0

    for i, event in enumerate(events, 1):
        if event["id"] in existing and not args.force:
            skipped += 1
            continue

        text = build_embedding_text(event)
        log.info("[%d/%d] Embedding: %s", i, len(events), event["title"][:60])

        try:
            if args.provider == "bedrock":
                embedding = get_embedding_bedrock(text, bedrock_client)
            else:
                embedding = get_embedding_ollama(text)

            # Validate dimension
            if len(embedding) != EMBED_DIMENSION:
                log.error("  Unexpected dimension %d (expected %d)", len(embedding), EMBED_DIMENSION)
                errors += 1
                continue

            rows.append((event["id"], embedding, f"{args.provider}:{EMBED_MODEL}"))

            # Rate-limit for Ollama (local)
            if args.provider == "ollama":
                time.sleep(0.1)

        except Exception as e:
            log.error("  Failed: %s", e)
            errors += 1

    # Insert into database
    if rows:
        try:
            inserted = insert_embeddings(conn, rows)
            conn.commit()
            log.info("Inserted %d embeddings into database", inserted)
        except Exception as e:
            conn.rollback()
            log.error("Database insert failed: %s", e)
            sys.exit(1)
    else:
        log.info("No new embeddings to insert")

    conn.close()

    log.info(
        "Done — total: %d | inserted: %d | skipped: %d | errors: %d",
        len(events), len(rows), skipped, errors,
    )


if __name__ == "__main__":
    main()
