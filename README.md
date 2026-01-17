# Graphite-Forge

A modern **S3-compatible object storage management platform** with **GraphQL API**, **Next.js UI**, and **multi-tenancy support**. Built on Java 25, Spring Boot 4, and integrated with IronBucket infrastructure.

## Quick Start

```bash
# Start all services (GraphQL + UI)
./scripts/spinup.sh

# Access the application
UI:       http://localhost:3000
GraphQL:  http://localhost:8083/graphql
Health:   http://localhost:8083/actuator/health
```

## What You Get

✅ **GraphQL API** - Flexible, type-safe queries for bucket and object management  
✅ **React UI** - Modern Next.js interface with real-time updates  
✅ **Multi-Tenancy** - Complete tenant isolation with RBAC  
✅ **OAuth 2.0** - Secure authentication with Keycloak  
✅ **S3-Compatible** - Direct integration with MinIO object storage  
✅ **Service Discovery** - Automatic service registry with Eureka  
✅ **Production Ready** - Health checks, monitoring, security built-in  

## Architecture

Graphite-Forge leverages **IronBucket** as shared infrastructure:

**Graphite-Forge Services:**
- GraphQL API (Port 8083) - Java Spring Boot GraphQL service
- Next.js UI (Port 3000) - React-based user interface

**Shared IronBucket Services:**
- Keycloak (7081) - Identity & Access Management
- MinIO (9000) - S3-Compatible Object Storage
- Sentinel-Gear (8080) - API Gateway
- Buzzle-Vane (8083) - Eureka Service Registry
- PostgreSQL (5432) - Persistent Database

All services run in Docker containers on the same network for seamless service discovery.

## Development

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 18+
- Maven 3.9+

### Setup

```bash
# Start the full stack
./scripts/spinup.sh

# Run E2E tests
./scripts/test-e2e.sh

# Run unit tests
cd graphql-service && mvn test
cd ../ui && npm test
```

## Documentation

| Document | Purpose |
|----------|---------|
| [**ARCHITECTURE.md**](ARCHITECTURE.md) | System design, component interactions, data flow |
| [**CONTRIBUTING.md**](CONTRIBUTING.md) | Development guidelines, coding standards, PR process |
| [**ROADMAP.md**](ROADMAP.md) | Feature roadmap and community priorities |

## Key Features

### Multi-Tenancy
- Complete isolation between tenants
- Separate namespaces for buckets, objects, and policies
- Role-based access control (RBAC) with Admin and Developer roles

### Real-Time Capabilities
- GraphQL subscriptions for live updates
- WebSocket support for instant notifications
- Event-driven architecture for state synchronization

### Security
- OAuth 2.0 with Keycloak integration
- Role-based access control (Admin, Developer)
- Encrypted data transmission
- Comprehensive audit logging

### Developer Experience
- GraphQL playground for API exploration
- Comprehensive error messages and logging
- E2E tests for workflow validation
- Docker-based local development

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Code style and standards
- Pull request process
- Testing requirements
- Commit conventions

## Support

- 📖 Check [Architecture Docs](ARCHITECTURE.md) for system design
- 🚀 See [Roadmap](ROADMAP.md) for planned features
- 🤝 Review [Contributing Guide](CONTRIBUTING.md) for development
- 🐛 [Report Issues](https://github.com/ZiggiZagga/Graphite-Forge/issues)

## License

MIT License - See LICENSE file for details

---

**Maintained by**: ZiggiZagga | **Status**: Active Development
