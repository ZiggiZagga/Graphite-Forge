# Changelog

All notable changes to Graphite-Forge are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Comprehensive Test Reporting System** - Automatic failure tracking and todo generation
  - Single command test execution (`./scripts/comprehensive-test-reporter.sh --all`)
  - Automatic conversion of test failures to structured todos
  - Severity-based prioritization (CRITICAL, HIGH, MEDIUM, LOW)
  - Auto-assigned deadlines based on severity
  - 4 report formats: Markdown, JSON, HTML, Summary
  - CI/CD integration ready (GitHub Actions, GitLab CI, Jenkins examples)
  - TypeScript reporting classes for advanced features
  - Jest custom reporter integration
  - 8 comprehensive documentation files (50+ pages)
  - [Full Documentation](docs/TEST-REPORTING-SYSTEM.md)
- User management dashboard (planned for v2.1)
- Bucket-level policies and access control (planned for v2.1)

### Changed
- **Documentation Updates** - Enhanced README, CONTRIBUTING, ROADMAP, ARCHITECTURE with test reporting system
- Test execution workflow now features comprehensive reporter as primary method

### Fixed
- (No unreleased fixes)

## [2.0.1] - January 17, 2026

### Added
- **Comprehensive Community Roadmap** - Transparent 6-month feature roadmap, contributor tiers, and voting mechanism
- **Intelligent IronBucket Startup Detection** - Automatic detection of IronBucket services with interactive prompts for user convenience
- **Enhanced Error Logging** - Comprehensive error diagnostics when services fail to start

### Changed
- **Documentation Consolidation** - Restructured from 30+ redundant files to 5 focused documents:
  - README.md - Quick start and high-level overview
  - ARCHITECTURE.md - Complete system design and technical deep-dive
  - CONTRIBUTING.md - Developer guidelines and contribution process
  - ROADMAP.md - Feature roadmap with community priorities
  - COMMUNITY_ROADMAP.md - Community engagement strategy and contributor recognition
- **spinup.sh Script** - Enhanced with IronBucket detection, improved error handling, and better user experience
- **Startup Process** - Now intelligently detects IronBucket and offers interactive startup options

### Fixed
- Improved error messages when services fail to start
- Better diagnostics for Docker container issues

### Removed
- Removed 30+ redundant documentation files (eliminated documentation debt)
- Cleaned up duplicate documentation that caused confusion

### Statistics
- 48 files changed, 1,370 insertions(+), 12,991 deletions(-)
- Net reduction of -11,621 lines through aggressive documentation consolidation

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
