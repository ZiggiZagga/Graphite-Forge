
# Graphite-Forge

## Why Graphite-Forge?

**Are you tired of monolithic backends, slow feature delivery, and integration headaches?**

Graphite-Forge is your launchpad for modern, cloud-native microservices. It eliminates the pain of:
- Complex, hard-to-scale legacy architectures
- Tedious API gateway setup and brittle routing
- Slow, blocking APIs that can't keep up with real-time needs
- Feature toggles that require code changes and redeploys
- Difficult local development and onboarding

**With Graphite-Forge you unlock:**
- Lightning-fast, reactive GraphQL APIs for any frontend
- A ready-to-go Spring Cloud Gateway for secure, dynamic routing
- Feature toggles managed via config, not code
- Seamless local development with Docker Compose
- A foundation for rapid prototyping, scaling, and cloud migration

**Stop fighting your stack. Start building features.**

---

## Overview
Graphite-Forge is a microservices-based system built with Spring Boot, featuring a GraphQL CRUD service and an Edge Gateway. The architecture is designed for extensibility, cloud-native deployment, and modern development practices.

---

## Architecture
- **Edge Gateway**: Acts as the main entry point, routing and securing traffic to backend services.
- **GraphQL CRUD Service**: Provides a reactive GraphQL API for managing `Item` entities with full CRUD support and feature toggles.
- **Service Discovery & Config**: (Optional) Integrates with Spring Cloud Eureka and Config Server for dynamic service registration and configuration.
- **Docker Compose**: Orchestrates all services for local development and testing.

---

## Features
- Reactive, non-blocking GraphQL API
- Feature toggles for CRUD operations
- Spring Boot 3.x, Spring Cloud, Spring GraphQL, WebFlux, R2DBC, H2
- Test-first (TDD) approach with JUnit, Mockito, Spring Boot Test
- Extensible and production-ready configuration

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (for local orchestration)

### Running Locally
1. **Build all services:**
	 ```bash
	 mvn clean install
	 ```
2. **Start with Docker Compose:**
	 ```bash
	 docker-compose up --build
	 ```
3. **Access Services:**
	 - Edge Gateway: http://localhost:8080
	 - GraphQL Playground: http://localhost:8083/graphiql (if enabled)

### Running Tests
From the `graphql-service` directory:
```bash
mvn test
```

---

## Project Structure
```
edge-gateway/           # API Gateway (Spring Cloud Gateway)
graphql-service/        # GraphQL CRUD microservice
docker-compose.yml      # Local orchestration
```

---

## Example GraphQL Queries

**Get all items:**
```graphql
query {
	items {
		id
		name
		description
	}
}
```

**Create an item:**
```graphql
mutation {
	createItem(name: "Sample", description: "Demo item") {
		id
		name
		description
	}
}
```

---

## Roadmap
See [Project Roadmap](#roadmap) in this README for planned improvements and tasks.

---

## Contributing
1. Fork the repo and create your branch from `main`.
2. Make your changes and add tests.
3. Ensure all tests pass (`mvn test`).
4. Submit a pull request with a clear description.

---

## License
MIT License

---

## Maintainers
- ZiggiZagga

---

## Contact
For questions or support, open an issue or contact the maintainer.
# Graphite-Forge