# Architecture

## System Overview

Graphite-Forge is a microservices-based S3-compatible storage management platform. It consists of two main layers:

### Application Layer (Graphite-Forge)
- **GraphQL Service** (Port 8083) - Java Spring Boot microservice providing GraphQL API
- **Next.js UI** (Port 3000) - React-based user interface for bucket and object management

### Infrastructure Layer (IronBucket - Shared)
- **Keycloak** (Port 7081) - OAuth 2.0 identity provider for authentication
- **MinIO** (Port 9000) - S3-compatible object storage backend
- **Sentinel-Gear** (Port 8080) - API Gateway for routing and load balancing
- **Buzzle-Vane** (Port 8083) - Eureka service registry for service discovery
- **PostgreSQL** (Port 5432) - Persistent database for metadata and configuration

## Network Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  graphite-forge-network                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │         Graphite-Forge Services                    │ │
│  │  ┌──────────────────┐    ┌──────────────────────┐  │ │
│  │  │ GraphQL Service  │    │  Next.js UI          │  │ │
│  │  │ Spring Boot      │◄───┤  React + Apollo      │  │ │
│  │  │ (8083)           │    │  (3000)              │  │ │
│  │  └──────────────────┘    └──────────────────────┘  │ │
│  └────────────────────────────────────────────────────┘ │
│         │                                                │
│         │ (Service Discovery via Container Names)       │
│         └──────────────────┬──────────────────┐         │
└────────────────────────────┼──────────────────┼─────────┘
                             │                  │
              ┌──────────────┼──────────────────┤──────────┐
              │              │                  │          │
         (steel-hammer-network - external, shared with IronBucket)
              │              │                  │          │
              ▼              ▼                  ▼          ▼
        ┌──────────┐  ┌──────────┐      ┌──────────┐ ┌──────────┐
        │ Keycloak │  │  MinIO   │      │Sentinel- │ │ Buzzle-  │
        │ (7081)   │  │ (9000)   │      │ Gear     │ │ Vane     │
        │          │  │          │      │ (8080)   │ │ (8083)   │
        └──────────┘  └──────────┘      └──────────┘ └──────────┘
              │              │                            │
              └──────────────┬────────────────────────────┘
                             │
                       ┌─────▼──────┐
                       │ PostgreSQL  │
                       │ (5432)      │
                       └─────────────┘
```

## Service Descriptions

### GraphQL Service
**Language**: Java 25 | **Framework**: Spring Boot 4 | **Location**: `/graphql-service`

Provides a GraphQL API for managing buckets, objects, and user permissions.

**Key Components**:
- `GraphQLController` - GraphQL endpoint handler
- `ConfigResolver` - Schema type definitions
- `BucketService` - Bucket operations (create, delete, list)
- `ObjectService` - Object operations (upload, download, delete)
- `UserService` - User and permission management
- `ConfigRepository` - Data access layer

**Technologies**:
- GraphQL (Spring GraphQL)
- Hibernate JPA for data access
- Spring Security for authorization
- Spring Cloud for service discovery

### Next.js UI
**Language**: TypeScript | **Framework**: Next.js 14 | **Location**: `/ui`

Modern React-based user interface for interacting with Graphite-Forge.

**Key Features**:
- Apollo Client for GraphQL integration
- Real-time subscriptions via WebSocket
- Responsive design with Tailwind CSS
- Protected routes with OAuth 2.0

### Keycloak (IronBucket)
OAuth 2.0 identity provider for authentication and authorization.

**Configuration**:
- Realm: `master`
- Admin User: `admin`
- Client: `graphite-forge-client` (configured for direct grant OAuth flow)

**Users** (created by `setup-keycloak-dev-users.sh`):
- `alice:password` - Admin role
- `bob:password` - Developer role

### MinIO (IronBucket)
S3-compatible object storage backend.

**Features**:
- Full S3 API compatibility
- Multi-tenant bucket isolation
- Object versioning and lifecycle policies
- Access control lists (ACLs)

### Sentinel-Gear (IronBucket)
API Gateway providing:
- Request routing to appropriate services
- Rate limiting
- Authentication/authorization enforcement
- Load balancing

### Buzzle-Vane (IronBucket)
Eureka service registry for dynamic service discovery.

**Service Registration**:
- GraphQL Service registers at startup
- Services discover each other via Eureka URI: `http://steel-hammer-buzzle-vane:8083/eureka`

### PostgreSQL (IronBucket)
Shared persistent database for:
- User profiles and permissions
- Bucket metadata
- Object metadata
- Configuration data

## Data Model

### Bucket Entity
```
Bucket
├── id: UUID
├── name: String
├── owner: String (Keycloak user)
├── created_at: Timestamp
├── updated_at: Timestamp
└── metadata: JSON
```

### Object Entity
```
Object
├── id: UUID
├── bucket_id: UUID (FK)
├── key: String
├── size: Long
├── content_type: String
├── created_at: Timestamp
├── updated_at: Timestamp
└── metadata: JSON
```

### User Entity
```
User
├── id: UUID
├── username: String
├── email: String
├── roles: List<String>
├── created_at: Timestamp
└── updated_at: Timestamp
```

## API Flows

### Authentication Flow
1. User logs in on UI with Keycloak credentials
2. Keycloak issues OAuth 2.0 access token
3. UI includes token in GraphQL requests
4. GraphQL Service validates token with Keycloak
5. GraphQL Service authorizes based on user roles

### Bucket Creation Flow
1. UI sends `CreateBucket` GraphQL mutation
2. GraphQL Service validates user permissions
3. GraphQL Service calls MinIO S3 API to create bucket
4. GraphQL Service stores bucket metadata in PostgreSQL
5. GraphQL Service publishes `BucketCreated` subscription event
6. UI receives real-time update

### Object Upload Flow
1. UI sends `UploadObject` GraphQL mutation with file
2. GraphQL Service validates user has bucket access
3. GraphQL Service uploads to MinIO via S3 API
4. GraphQL Service stores object metadata in PostgreSQL
5. GraphQL Service publishes `ObjectUploaded` subscription event
6. UI receives real-time update

## Deployment Architecture

### Docker Compose
Local development uses `docker-compose.yml`:
- GraphQL Service container (Java application)
- E2E test container (Cypress for integration testing)
- Network connections to IronBucket services

### Container Networking
- **graphite-forge-network**: Internal bridge network for Graphite-Forge services
- **steel-hammer-network**: External network shared with IronBucket (managed by IronBucket)

Both networks are configured in `docker-compose.yml` to enable inter-service communication.

## Service Discovery

Services discover each other using:
1. **Eureka** - Primary service registry
2. **Container Names** - Docker DNS resolution (e.g., `steel-hammer-keycloak:7081`)

**Environment Variables** (set in `docker-compose.yml`):
```
EUREKA_URI=http://steel-hammer-buzzle-vane:8083/eureka
KEYCLOAK_URI=http://steel-hammer-keycloak:7081
MINIO_URI=http://steel-hammer-minio:9000
```

## Security

### Authentication
- OAuth 2.0 via Keycloak
- Access tokens validated on each GraphQL request
- Token refresh handled by Apollo Client on UI

### Authorization
- Role-based access control (RBAC)
- Users with `admin` role can manage all buckets
- Users with `developer` role can only access assigned buckets
- Policies enforced at GraphQL resolver level

### Data Protection
- TLS/SSL for in-transit encryption (in production)
- Data at rest encrypted by MinIO
- Database passwords stored in Docker secrets (production)

## Performance Considerations

### Caching
- Apollo Client caches GraphQL results on frontend
- PostgreSQL query optimization with indexes
- MinIO S3 API caching headers respected

### Scalability
- Stateless GraphQL Service allows horizontal scaling
- Eureka enables load balancing across instances
- MinIO supports distributed deployments

### Monitoring
- Spring Boot Actuator health checks (`/actuator/health`)
- Application metrics exposed at `/actuator/metrics`
- Logs collected from Docker containers

## Development Workflow

### Local Development
1. Run `./scripts/spinup.sh` to start all services
2. Verify health at `http://localhost:8083/actuator/health`
3. Access GraphQL Playground at `http://localhost:8083/graphql`
4. Make changes to source code
5. Rebuild service: `cd graphql-service && mvn clean package`
6. Restart service: `docker-compose restart graphql-service`

### Testing
- **Unit Tests**: `cd graphql-service && mvn test`
- **E2E Tests**: `./scripts/test-e2e.sh`
- Tests run in Docker containers on same network as services

---

**For questions about architecture, see [Contributing Guide](CONTRIBUTING.md) or open an issue.**
