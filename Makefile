# Keycloak BulkGate SMS OTP authenticator — common tasks.
# Run `make` or `make help` to list targets.

.DEFAULT_GOAL := help
GRADLE := ./gradlew

.PHONY: help build jar test e2e verify up down clean

help: ## Show this help
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Compile, run unit/integration tests, build the provider jar
	$(GRADLE) clean build

jar: ## Build only the provider jar (build/libs/*.jar)
	$(GRADLE) shadowJar

test: ## Run fast unit + WireMock integration tests (no Docker)
	$(GRADLE) test

e2e: ## Run Docker-backed end-to-end tests (Keycloak + mocked BulkGate)
	$(GRADLE) e2eTest

verify: ## Run everything: clean build + e2e
	$(GRADLE) clean build e2eTest

up: jar ## Build the jar and start the demo stack (Keycloak + WireMock)
	docker compose up

down: ## Stop and remove the demo stack
	docker compose down

clean: ## Remove build outputs
	$(GRADLE) clean
