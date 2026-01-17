# Changelog

All notable changes to Graphite-Forge are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- User management dashboard (in progress)
- Bucket-level policies and access control (in progress)

### Changed
- Documentation restructured for clarity

### Fixed
- TBD

## [2.0.0] - January 17, 2026

### Added
- **Core GraphQL API** for bucket and object management
  - CreateBucket, DeleteBucket, ListBuckets mutations
  - UploadObject, DownloadObject, DeleteObject mutations
  - Full CRUD operations with subscription support
  
- **Multi-Tenancy & RBAC**
  - Complete tenant isolation
  - Role-based access control (Admin, Developer)
  - Per-user bucket access restrictions
  
- **OAuth 2.0 Authentication**
  - Keycloak integration for identity management
  - Token validation and refresh
  - User profile management
  
- **S3-Compatible Storage**
  - MinIO backend integration
  - Full S3 API compatibility
  - Bucket and object operations
  
- **Service Discovery**
  - Eureka service registry integration
  - Automatic service registration and discovery
  - Health check endpoints
  
- **Next.js React UI**
  - Modern responsive interface
  - Apollo Client for GraphQL integration
  - Real-time updates with WebSocket subscriptions
  - Tailwind CSS styling
  
- **Docker-Based Development**
  - docker-compose.yml for local development
  - Automated service health checks
  - IronBucket infrastructure integration
  
- **Testing Suite**
  - Comprehensive E2E test suite
  - Unit tests for GraphQL service
  - Test utilities and fixtures
  - Cypress for UI testing
  
- **Documentation**
  - Architecture documentation
  - Contributing guidelines
  - Development roadmap
  - Comprehensive README

### Changed
- **Architecture Consolidation**
  - Removed duplicate edge-gateway service
  - Removed duplicate config-server service
  - Consolidated to use IronBucket shared infrastructure
  - Simplified service dependencies
  
- **Startup Process**
  - Enhanced spinup.sh with intelligent IronBucket detection
  - Added interactive prompts for service startup
  - Improved error logging and diagnostics
  
- **Configuration**
  - Simplified docker-compose.yml
  - Environment-based configuration
  - Service container name standardization

### Fixed
- Port exposure security issue (tests run in container network)
- IronBucket service discovery via correct container names
- GraphQL health check response format
- Docker network configuration for service-to-service communication

### Technical Details
- **Java Version**: 17+ (built with Java 25)
- **Spring Boot**: 4.0.0-RC2
- **Node.js**: 18+
- **GraphQL**: Spring GraphQL with subscriptions
- **Database**: PostgreSQL with Flyway migrations
- **Container Platform**: Docker & Docker Compose

---

## Release Notes

### v2.0.0 Highlights
This is the first major release of Graphite-Forge as a consolidated, production-ready platform.

**Key Achievements**:
- ✅ Complete architecture consolidation with IronBucket
- ✅ Multi-tenant support with full RBAC
- ✅ Scalable microservices design
- ✅ Comprehensive test coverage
- ✅ Production-ready security and error handling

**Migration Guide**: First release, no migration needed.

---

## Version Support

| Version | Status | Release Date | End of Support |
|---------|--------|--------------|----------------|
| 2.0.x | Active | Jan 2026 | May 2026 |
| 1.x | Deprecated | N/A | Jan 2026 |

---

## Contributing

For information about contributing to Graphite-Forge, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Roadmap

For planned features and future releases, see [ROADMAP.md](ROADMAP.md).

---

**Maintained by**: ZiggiZagga  
**Repository**: https://github.com/ZiggiZagga/Graphite-Forge
