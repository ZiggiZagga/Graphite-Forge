# Graphite-Forge + IronBucket - Consolidated Architecture

## Overview

Graphite-Forge and IronBucket now operate as a unified system where:
- **Graphite-Forge** provides specialized microservices (GraphQL API, UI)
- **IronBucket** provides shared infrastructure (Authentication, Gateway, Storage, Service Registry)

This eliminates duplication and simplifies operations.

## Service Architecture

### Graphite-Forge Services (Dedicated)

| Service | Container | Port | Role |
|---------|-----------|------|------|
| **GraphQL Service** | `graphql-service` | 8083 | Core API for bucket/object operations, policy management, audit logs |
| **Next.js UI** | Host process | 3000 | Web UI for Graphite-Forge |

### Shared IronBucket Infrastructure

| Service | Container | Port | Role |
|---------|-----------|------|------|
| **Sentinel-Gear** (API Gateway) | `steel-hammer-sentinel-gear` | 8080, 8081 | Routes requests to GraphQL, handles cross-cutting concerns |
| **Keycloak** (Identity) | `steel-hammer-keycloak` | 7081 | OAuth2/OIDC provider, user/role management |
| **MinIO** (S3 Storage) | `steel-hammer-minio` | 9000 | S3-compatible object storage backend |
| **Buzzle-Vane** (Eureka) | `steel-hammer-buzzle-vane` | 8083 | Service discovery/registry |
| **PostgreSQL** | `steel-hammer-postgres` | 5432 | Database for Keycloak, audit logs, metadata |

### Removed Services

❌ `edge-gateway` - Replaced by `steel-hammer-sentinel-gear`  
❌ `config-server` - No longer needed; services load config from environment

## Network Topology

```
┌─────────────────────────────────────────────────────────────┐
│                   Graphite-Forge Stack                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                   Host (Localhost)                   │   │
│  │  Port 3000: Next.js UI  Port 8083: GraphQL Service  │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│                  graphite-forge-network                      │
│                  (docker bridge)                             │
└─────────────────────────────────────────────────────────────┘
              │
              ├──────── Cross-Network Connection
              │
┌─────────────────────────────────────────────────────────────┐
│                    IronBucket Stack                          │
│  (steel-hammer in /workspaces/Graphite-Forge/IronBucket/)   │
│                                                              │
│    steel-hammer_steel-hammer-network (docker network)       │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Sentinel-Gear (8080) ← Routes to GraphQL, Auth, etc. │   │
│  │ Keycloak (7081)      ← OAuth2/OIDC Provider         │   │
│  │ MinIO (9000)         ← S3 Storage                     │   │
│  │ Buzzle-Vane (8083)   ← Service Registry              │   │
│  │ PostgreSQL (5432)    ← Persistent Data               │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Network Connectivity

**When `--with-ironbucket` is used in spinup.sh:**
1. A `docker-compose.override.yml` is generated
2. GraphQL service is connected to the `steel-hammer_steel-hammer-network`
3. Environment variables point services to each other via service names:
   - `KEYCLOAK_URL=http://steel-hammer-keycloak:7081`
   - `MINIO_URL=http://steel-hammer-minio:9000`
   - `EUREKA_URL=http://steel-hammer-buzzle-vane:8083/eureka`

## Communication Flow

### User Creates a Bucket (Web UI)

```
1. User → Next.js UI (http://localhost:3000)
2. UI → GraphQL Service (http://localhost:8083/graphql)
   - Authorization header with JWT from Keycloak
3. GraphQL Service → MinIO (http://steel-hammer-minio:9000)
   - Creates bucket via S3 API
4. GraphQL Service → PostgreSQL (http://steel-hammer-postgres:5432)
   - Records audit log
```

### Authentication Flow

```
1. User → Keycloak (http://localhost:7081)
   - Credentials: alice/aliceP@ss or bob/bobP@ss
   - Receives: JWT token with roles (adminrole, devrole)
2. UI/Client → GraphQL Service
   - Sends Authorization header: "Bearer {JWT_TOKEN}"
3. GraphQL Service validates JWT with Keycloak
   - JWT claims include user roles (adminrole, devrole)
   - Multi-tenant isolation based on user identity
```

### E2E Testing Flow

```
Host Terminal:
  ./scripts/spinup.sh --ironbucket
    ↓
    Start IronBucket (Keycloak, MinIO, etc.)
    ↓
    Setup alice/bob users in Keycloak (setup-keycloak-dev-users.sh)
    ↓
    Start Graphite-Forge services

Test Execution:
  ./scripts/test-e2e.sh --in-container
    ↓
    Build E2E test container
    ↓
    Run tests in Docker (same network as services)
    ↓
    Authenticate alice/bob against Keycloak
    ↓
    Test bucket operations via GraphQL
    ↓
    Verify multi-tenant isolation
```

## Key Improvements

✅ **Eliminates Duplication**
- Single Keycloak instance for all services
- Single API Gateway for routing
- Single S3-compatible storage backend

✅ **Simplified Operations**
- Fewer services to manage
- Clear separation of concerns
- Graphite-Forge focuses on GraphQL/bucket operations

✅ **Better Scalability**
- Shared infrastructure can handle multiple apps
- Service registry enables dynamic discovery
- Centralized authentication simplifies SSO

✅ **Production-Ready**
- Multi-tenant isolation via JWT roles
- Audit trail in PostgreSQL
- Service-to-service authentication via Keycloak

## Configuration

### Environment Variables

All services automatically configured when started with `--ironbucket`:

```bash
# In Graphite-Forge services
KEYCLOAK_URL=http://steel-hammer-keycloak:7081
MINIO_URL=http://steel-hammer-minio:9000
EUREKA_URL=http://steel-hammer-buzzle-vane:8083/eureka
```

### Keycloak Realm

- **Realm**: `dev` (production would use different realms)
- **Clients**:
  - `dev-client` (confidential, for service-to-service auth)
- **Users**:
  - `alice` (adminrole) - Can create, read, update, delete
  - `bob` (devrole) - Limited access for testing
- **Roles**:
  - `adminrole` - Full access
  - `devrole` - Developer/test access

### MinIO Setup

- **Root Credentials**: `minioadmin / minioadmin`
- **Endpoint**: `http://steel-hammer-minio:9000`
- **Console**: `http://localhost:9001`

## Startup Workflow

```bash
# Full stack with IronBucket integration
./scripts/spinup.sh --ironbucket

# Graphite-Forge only (requires running IronBucket separately)
./scripts/spinup.sh

# View logs
docker-compose logs -f graphql-service
```

## Testing

```bash
# E2E tests in Docker (recommended)
./scripts/test-e2e.sh --in-container

# Specific test suite
./scripts/test-e2e.sh --in-container --alice-bob

# Full suite (when available)
./scripts/test-e2e.sh --in-container --full-suite
```

## Troubleshooting

### GraphQL Service Cannot Connect to Keycloak

**Check**: Is `--with-ironbucket` being used in spinup?
```bash
# Ensure docker-compose.override.yml exists
ls -la docker-compose.override.yml

# Check GraphQL environment variables
docker inspect graphql-service | grep -A 10 '"Env"'
```

### Keycloak Users Not Configured

**Run**: User setup script manually
```bash
bash scripts/setup-keycloak-dev-users.sh
```

### E2E Tests Still Reference Old Gateway

**Update**: GraphQL_URL and GATEWAY_URL in test scripts
```bash
# Should be (NEW):
GRAPHQL_URL=http://localhost:8083
GATEWAY_URL=http://localhost:8080  # or sentinel-gear in container

# NOT (OLD):
GRAPHQL_URL=http://localhost:8081
GATEWAY_URL=http://localhost:8080/graphql
```

## Future Enhancements

- [ ] Add multi-region support (replicate MinIO buckets)
- [ ] Implement complete audit trail in PostgreSQL
- [ ] Add policy dry-run capability
- [ ] Implement bucket quotas and usage tracking
- [ ] Add S3 SELECT support for querying objects
- [ ] Implement versioning and WORM (Write Once Read Many)

## References

- [IronBucket Architecture](../IronBucket/README.md)
- [Keycloak Admin Console](http://localhost:7081)
- [MinIO Console](http://localhost:9001)
- [Sentinel-Gear API Gateway](http://localhost:8080)
