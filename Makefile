.PHONY: up down build test logs seed aws-up aws-down

up:
	docker compose up -d

down:
	docker compose down

build:
	mvn clean package -DskipTests

test:
	mvn verify

logs:
	docker compose logs -f

seed:
	docker exec ticketstorm-postgres-1 psql -U ticketstorm -d ticketstorm -f /seed-data.sql

aws-up:
	docker compose -f docker-compose.yml -f docker-compose.aws.yml up -d

aws-down:
	docker compose -f docker-compose.yml -f docker-compose.aws.yml down
