# IronBucket Integration Roadmap

**Project:** Graphite-Forge + IronBucket Integration  
**Version:** 1.0  
**Date:** January 17, 2026  
**Status:** Phase 1 Complete - Architecture Analysis Done

---

## Executive Summary

This roadmap outlines the complete integration of IronBucket's identity-aware S3 proxy system into Graphite-Forge's microservices platform. The integration will enable full S3-compatible object storage with JWT-based authentication, policy-driven access control, and comprehensive audit logging—all accessible through Graphite-Forge's existing GraphQL API and Next.js UI.

**Key Benefits:**
- 🔐 Identity-aware object storage with fine-grained access control
- 📊 Unified GraphQL API for both item management and S3 operations
- 🎨 Single UI for all platform features
- 🔍 Complete audit trail for all S3 operations
- 📦 S3-compatible API for standard AWS SDK usage

---

## Technology Compatibility Analysis

| Component | Graphite-Forge | IronBucket | Status |
|-----------|----------------|------------|--------|
| **Java** | 25 | 25 | ✅ Perfect Match |
| **Spring Boot** | 4.0.0 | 4.0.1 | ✅ Compatible |
| **Spring Cloud** | 2025.0.0 | 2025.1.0 | ✅ Compatible |
| **Reactive** | WebFlux | WebFlux | ✅ Perfect Match |
| **Auth** | OAuth2 JWT (Keycloak) | OAuth2 JWT (Keycloak) | ✅ Perfect Match |
| **Service Discovery** | Eureka | Eureka (Buzzle-Vane) | ✅ Perfect Match |
| **Database** | PostgreSQL/H2 | PostgreSQL | ✅ Compatible |
| **Container** | Docker Compose | Docker Compose | ✅ Perfect Match |
| **Frontend** | Next.js 15, React 19 | Next.js 15, React 19 | ✅ Perfect Match |

**Assessment:** Excellent technology alignment with minimal integration friction.

---

## Architecture Overview

### Current Graphite-Forge
```
User → Edge Gateway (8080) → GraphQL Service (8083) → PostgreSQL
                            → Config Server (8888)
       ↓
    Next.js UI (3000)
```

### IronBucket Services
```
Client → Brazz-Nossel (S3 Proxy, 8082)
         ↓
       Sentinel-Gear (JWT Validator, 8080)
         ↓
       Claimspindel (Policy Router, 8081)
         ↓
       MinIO (S3 Storage, 9000) → PostgreSQL (Audit)
```

### Integrated Architecture
```
┌─────────────────────────────────────────────────────────────┐
│              Graphite-Forge UI (Next.js)                    │
│                    Port: 3000                                │
│  • Item Management (existing)                               │
│  • S3 Bucket Browser (NEW)                                  │
│  • Policy Admin Console (NEW)                               │
│  • Audit Log Viewer (NEW)                                   │
└────────────────────────┬────────────────────────────────────┘
                         │ GraphQL + JWT
                         ↓
┌─────────────────────────────────────────────────────────────┐
│          Edge Gateway (Spring Cloud Gateway)                │
│                    Port: 8080                                │
│  Routes:                                                     │
│    /graphql → GraphQL Service (8083)                        │
│    /s3/** → Brazz-Nossel (8082)                            │
│    /sentinel/** → Sentinel-Gear (8080)                      │
│    /policy/** → Claimspindel (8081)                        │
└───────┬────────────────────┬────────────────────┬───────────┘
        │                    │                    │
        ↓                    ↓                    ↓
┌──────────────────┐  ┌─────────────────┐  ┌────────────────┐
│ GraphQL Service  │  │ Brazz-Nossel    │  │ Claimspindel   │
│ Port: 8083       │  │ (S3 Proxy)      │  │ (Policy Router)│
│                  │  │ Port: 8082      │  │ Port: 8081     │
│ • Item CRUD      │  └────────┬────────┘  └────────┬───────┘
│ • S3 Resolvers ★ │           │                    │
│ • Policy API ★   │           │ JWT Validation     │
│ • Audit Logs ★   │           ↓                    │
└────────┬─────────┘  ┌─────────────────┐           │
         │            │ Sentinel-Gear   │           │
         │            │ (JWT Validator) │           │
         │            │ Port: 8080      │           │
         │            └────────┬────────┘           │
         │                     │                    │
         │                     ↓                    ↓
         ↓              ┌────────────────────────────────┐
┌─────────────────┐    │     MinIO S3 Storage           │
│ PostgreSQL      │    │     Port: 9000                 │
│ • Items         │    │ • Buckets & Objects            │
│ • Config        │    └──────────────┬─────────────────┘
└─────────────────┘                   │
                                      ↓
                         ┌────────────────────────────────┐
                         │ PostgreSQL (IronBucket)        │
                         │ • Audit Logs                   │
                         │ • Policy Metadata              │
                         └────────────────────────────────┘
```

★ = New components to be implemented

---

## Phase 1: Architecture Analysis ✅ COMPLETE

**Duration:** Completed January 17, 2026  
**Status:** ✅ Done

### Deliverables Completed
- [x] Analyzed Graphite-Forge architecture (edge-gateway, graphql-service, ui)
- [x] Analyzed IronBucket architecture (6 microservices + infrastructure)
- [x] Identified integration points
- [x] Assessed technology compatibility
- [x] Documented current state
- [x] Created this roadmap

### Key Findings
1. **Excellent Technology Alignment:** Both projects use Java 25, Spring Boot 4, WebFlux, Keycloak
2. **Compatible Architectures:** Both use microservices + Eureka + Docker Compose
3. **Shared Auth Model:** Both use OAuth2 JWT tokens from Keycloak
4. **Integration Strategy:** GraphQL federation + gateway routing
5. **IronBucket Code Location:** Services in `/temp/` subdirectory (231 passing tests)

---

## Phase 2: Infrastructure Setup

**Duration:** Week 1 (5 days)  
**Status:** 🔜 Ready to Start

### 2.1 Docker Compose Integration (Day 1-2)
**Objective:** Create unified docker-compose.yml with all services

**Tasks:**
- [ ] Merge Graphite-Forge and IronBucket docker-compose files
- [ ] Resolve port conflicts (Eureka on 8083 in both systems)
- [ ] Configure shared network for all services
- [ ] Add MinIO service (port 9000, 9001)
- [ ] Add PostgreSQL for IronBucket audit logs
- [ ] Configure Keycloak with proper realm settings
- [ ] Test full stack startup

**Files:**
- `docker-compose-integrated.yml` (new)
- `docker-compose.yml` (update)

**Acceptance Criteria:**
- All services start successfully
- Service discovery works across all services
- Keycloak JWT tokens accepted by all services

### 2.2 Gateway Routing Configuration (Day 3-4)
**Objective:** Add IronBucket routes to edge-gateway

**Tasks:**
- [ ] Add route: `/s3/**` → Brazz-Nossel (8082)
- [ ] Add route: `/sentinel/**` → Sentinel-Gear (8080)
- [ ] Add route: `/policy/**` → Claimspindel (8081)
- [ ] Configure JWT propagation to all routes
- [ ] Add CORS configuration for new routes
- [ ] Add rate limiting for S3 operations
- [ ] Add circuit breakers for resilience
- [ ] Update Actuator health checks

**Files:**
- `edge-gateway/src/main/resources/application.yml`
- `edge-gateway/src/main/java/com/example/gateway/SecurityConfig.java`

**Acceptance Criteria:**
- Routes accessible via gateway
- JWT tokens properly forwarded
- Circuit breakers work on service failures

### 2.3 Service Discovery Integration (Day 5)
**Objective:** Register IronBucket services with Eureka

**Tasks:**
- [ ] Configure Brazz-Nossel Eureka client
- [ ] Configure Sentinel-Gear Eureka client
- [ ] Configure Claimspindel Eureka client
- [ ] Test dynamic service resolution
- [ ] Verify health check propagation
- [ ] Test service failover

**Acceptance Criteria:**
- All IronBucket services visible in Eureka dashboard
- Gateway can discover services dynamically
- Health checks report correctly

---

## Phase 3: GraphQL API Extension

**Duration:** Week 2-3 (10 days)  
**Status:** 🔜 Pending Phase 2

### 3.1 Schema Definition (Day 6-7)
**Objective:** Define GraphQL types for IronBucket operations

**Tasks:**
- [ ] Define S3 types (Bucket, Object, Metadata)
- [ ] Define Policy types (PolicyRule, Tenant, Role)
- [ ] Define Audit types (AuditLog, AccessRecord)
- [ ] Define input types for mutations
- [ ] Define result types for operations
- [ ] Add scalar types (Upload, DateTime, JSON)

**Files:**
- `graphql-service/src/main/resources/graphql/ironbucket-schema.graphqls` (new)

**Schema Components:**
```graphql
# S3 Operations
type S3Bucket { name, creationDate, owner, tenant }
type S3Object { key, bucket, size, contentType, metadata }
type Query { listBuckets, listObjects, getObject, getPresignedUrl }
type Mutation { createBucket, uploadObject, deleteObject, setMetadata }

# Policy Operations
type PolicyRule { id, tenant, roles, allowedBuckets, operations }
type Query { policies, policyById, policiesByTenant }
type Mutation { createPolicy, updatePolicy, deletePolicy, dryRunPolicy }

# Audit Operations
type AuditLogEntry { id, timestamp, user, action, bucket, result }
type Query { auditLogs, auditLogsByUser, auditLogsByBucket }
```

**Acceptance Criteria:**
- Schema compiles without errors
- All operations have proper documentation
- Input validation rules defined

### 3.2 Service Layer Implementation (Day 8-10)
**Objective:** Create services to interact with IronBucket APIs

**Tasks:**
- [ ] Create `IronBucketS3Service` with WebClient
- [ ] Create `PolicyManagementService`
- [ ] Create `AuditLogService`
- [ ] Implement JWT claim extraction from context
- [ ] Add error mapping (IronBucket errors → GraphQL errors)
- [ ] Add response caching for frequently accessed data
- [ ] Add retry logic with exponential backoff
- [ ] Implement connection pooling

**Files:**
- `graphql-service/src/main/java/com/example/graphql/ironbucket/IronBucketS3Service.java` (new)
- `graphql-service/src/main/java/com/example/graphql/ironbucket/PolicyManagementService.java` (new)
- `graphql-service/src/main/java/com/example/graphql/ironbucket/AuditLogService.java` (new)

**Acceptance Criteria:**
- All HTTP calls use reactive WebClient
- Errors mapped to GraphQL error format
- JWT claims extracted and passed to IronBucket
- Unit tests for each service method

### 3.3 Resolver Implementation (Day 11-13)
**Objective:** Implement GraphQL resolvers

**Tasks:**
- [ ] Create `S3BucketResolver` (queries + mutations)
- [ ] Create `S3ObjectResolver` (queries + mutations)
- [ ] Create `PolicyResolver` (queries + mutations)
- [ ] Create `AuditLogResolver` (queries only)
- [ ] Implement field resolvers for nested data
- [ ] Add DataLoader for batch operations
- [ ] Add pagination support
- [ ] Add filtering and sorting

**Files:**
- `graphql-service/src/main/java/com/example/graphql/ironbucket/S3BucketResolver.java` (new)
- `graphql-service/src/main/java/com/example/graphql/ironbucket/S3ObjectResolver.java` (new)
- `graphql-service/src/main/java/com/example/graphql/ironbucket/PolicyResolver.java` (new)
- `graphql-service/src/main/java/com/example/graphql/ironbucket/AuditLogResolver.java` (new)

**Acceptance Criteria:**
- All resolvers return proper types
- Errors handled gracefully
- DataLoader prevents N+1 queries
- GraphiQL playground shows all operations

### 3.4 Testing (Day 14-15)
**Objective:** Comprehensive testing of GraphQL layer

**Tasks:**
- [ ] Unit tests for services (mock HTTP clients)
- [ ] Unit tests for resolvers (mock services)
- [ ] Integration tests (GraphQL → IronBucket → MinIO)
- [ ] Error scenario tests (401, 403, 404, 500)
- [ ] Performance tests (response times, throughput)
- [ ] Load tests (concurrent requests)

**Test Coverage Target:** 80%+ for new code

**Acceptance Criteria:**
- All unit tests passing
- Integration tests verify E2E flow
- Error scenarios handled correctly
- Performance meets requirements (<200ms p95)

---

## Phase 4: Frontend Client UI

**Duration:** Week 4 (5 days)  
**Status:** 🔜 Pending Phase 3

### 4.1 GraphQL Client Setup (Day 16)
**Objective:** Configure Apollo Client for IronBucket operations

**Tasks:**
- [ ] Add IronBucket GraphQL queries to Apollo cache
- [ ] Configure cache policies for S3 data
- [ ] Add optimistic updates for mutations
- [ ] Configure error handling
- [ ] Add retry logic
- [ ] Create custom hooks for common operations

**Files:**
- `ui/lib/apollo.tsx` (update)
- `ui/graphql/ironbucket-queries.ts` (new)
- `ui/graphql/ironbucket-mutations.ts` (new)
- `ui/hooks/useS3Buckets.ts` (new)
- `ui/hooks/useS3Objects.ts` (new)

**Acceptance Criteria:**
- Apollo Client configured for IronBucket queries
- Cache policies prevent stale data
- Error handling shows user-friendly messages

### 4.2 S3 Bucket Browser (Day 17-18)
**Objective:** Build UI for browsing and managing S3 buckets

**Tasks:**
- [ ] Create BucketList component (list all buckets)
- [ ] Create BucketDetails component (bucket info)
- [ ] Create ObjectList component (list objects with pagination)
- [ ] Create ObjectViewer component (view/download object)
- [ ] Add search/filter functionality
- [ ] Add breadcrumb navigation
- [ ] Add loading skeletons
- [ ] Add empty states

**Files:**
- `ui/components/ironbucket/BucketList.tsx` (new)
- `ui/components/ironbucket/BucketDetails.tsx` (new)
- `ui/components/ironbucket/ObjectList.tsx` (new)
- `ui/components/ironbucket/ObjectViewer.tsx` (new)
- `ui/app/buckets/page.tsx` (new)
- `ui/app/buckets/[name]/page.tsx` (new)

**Acceptance Criteria:**
- Users can browse all accessible buckets
- Users can navigate folder structures
- Users can view object metadata
- Users can download objects

### 4.3 Upload/Download Flows (Day 19)
**Objective:** Build UI for uploading and downloading files

**Tasks:**
- [ ] Create UploadDialog component (single & multiple files)
- [ ] Add drag-and-drop support
- [ ] Add upload progress indicator
- [ ] Add file type validation
- [ ] Add file size validation
- [ ] Create download confirmation dialog
- [ ] Add download progress (for large files)
- [ ] Handle upload errors gracefully

**Files:**
- `ui/components/ironbucket/UploadDialog.tsx` (new)
- `ui/components/ironbucket/UploadProgress.tsx` (new)
- `ui/components/ironbucket/DownloadDialog.tsx` (new)

**Acceptance Criteria:**
- Users can upload files via drag-and-drop or file picker
- Upload progress visible in real-time
- Large files handled via multipart upload
- Downloads work for all file types

### 4.4 Policy-Aware UI (Day 20)
**Objective:** Implement identity-aware UI behavior

**Tasks:**
- [ ] Extract user claims from JWT (tenant, roles)
- [ ] Hide buckets user doesn't have access to
- [ ] Disable operations based on policy rules
- [ ] Show access level indicators (read-only, read-write)
- [ ] Add "Access Denied" messages
- [ ] Show tenant-specific views

**Files:**
- `ui/lib/auth-context.tsx` (new)
- `ui/hooks/useUserClaims.ts` (new)
- `ui/utils/policy-checker.ts` (new)

**Acceptance Criteria:**
- UI adapts to user's permissions
- Users only see what they can access
- Clear feedback when access is denied

---

## Phase 5: Admin UI

**Duration:** Week 5 (5 days)  
**Status:** 🔜 Pending Phase 4

### 5.1 Policy Management UI (Day 21-22)
**Objective:** Build admin interface for policy management

**Tasks:**
- [ ] Create PolicyList component (all policies)
- [ ] Create PolicyEditor component (YAML/JSON editor)
- [ ] Add syntax highlighting for policy files
- [ ] Add validation before save
- [ ] Create PolicyDryRun component (test policies)
- [ ] Add policy versioning display
- [ ] Add policy comparison (diff view)

**Files:**
- `ui/components/ironbucket/admin/PolicyList.tsx` (new)
- `ui/components/ironbucket/admin/PolicyEditor.tsx` (new)
- `ui/components/ironbucket/admin/PolicyDryRun.tsx` (new)
- `ui/app/admin/policies/page.tsx` (new)

**Acceptance Criteria:**
- Admins can view all policies
- Admins can create/edit/delete policies
- Policies validated before save
- Dry-run shows expected results

### 5.2 User & Claims Inspector (Day 23)
**Objective:** Build UI for inspecting user claims and roles

**Tasks:**
- [ ] Create UserList component (all users)
- [ ] Create UserDetails component (claims viewer)
- [ ] Create RoleAssignment component
- [ ] Add claim search/filter
- [ ] Show effective permissions per user
- [ ] Add user activity summary

**Files:**
- `ui/components/ironbucket/admin/UserList.tsx` (new)
- `ui/components/ironbucket/admin/UserDetails.tsx` (new)
- `ui/components/ironbucket/admin/ClaimsViewer.tsx` (new)
- `ui/app/admin/users/page.tsx` (new)

**Acceptance Criteria:**
- Admins can view all users
- Admins can inspect JWT claims
- Clear display of user permissions

### 5.3 Audit Log Viewer (Day 24)
**Objective:** Build UI for viewing audit logs

**Tasks:**
- [ ] Create AuditLogList component (paginated list)
- [ ] Create AuditLogDetails component (full record)
- [ ] Add filtering by user, action, bucket, date
- [ ] Add export functionality (CSV, JSON)
- [ ] Add visualization (activity timeline)
- [ ] Add search functionality

**Files:**
- `ui/components/ironbucket/admin/AuditLogList.tsx` (new)
- `ui/components/ironbucket/admin/AuditLogDetails.tsx` (new)
- `ui/app/admin/audit/page.tsx` (new)

**Acceptance Criteria:**
- Admins can view all audit logs
- Logs filterable by multiple criteria
- Export works for filtered results

### 5.4 Observability Dashboard (Day 25)
**Objective:** Build monitoring dashboard for IronBucket services

**Tasks:**
- [ ] Create ServiceHealthDashboard component
- [ ] Display service status (up/down)
- [ ] Show metrics (requests/sec, latency, errors)
- [ ] Create charts for key metrics
- [ ] Add real-time updates (WebSocket or polling)
- [ ] Add alerting indicators

**Files:**
- `ui/components/ironbucket/admin/ServiceHealthDashboard.tsx` (new)
- `ui/components/ironbucket/admin/MetricsChart.tsx` (new)
- `ui/app/admin/observability/page.tsx` (new)

**Acceptance Criteria:**
- Dashboard shows all service health
- Metrics updated in real-time
- Charts visualize trends

---

## Phase 6: Testing & Validation

**Duration:** Week 6 (5 days)  
**Status:** 🔜 Pending Phase 5

### 6.1 Integration Testing (Day 26-27)
**Objective:** End-to-end integration tests

**Tasks:**
- [ ] Test: Upload file via UI → verify in MinIO
- [ ] Test: Create bucket → verify in GraphQL query
- [ ] Test: Delete object → verify removed
- [ ] Test: Policy enforcement (allow/deny scenarios)
- [ ] Test: JWT authentication (401 on invalid token)
- [ ] Test: Multi-tenant isolation
- [ ] Test: Audit log recording

**Tools:**
- Playwright for E2E tests
- Jest for component tests
- Postman/curl for API tests

**Acceptance Criteria:**
- All E2E scenarios passing
- No false positives/negatives

### 6.2 Performance Testing (Day 28)
**Objective:** Validate performance requirements

**Tasks:**
- [ ] Load test: 100 concurrent users
- [ ] Stress test: Find breaking point
- [ ] Measure response times (p50, p95, p99)
- [ ] Test large file uploads (>1GB)
- [ ] Test batch operations
- [ ] Profile memory usage
- [ ] Profile CPU usage

**Tools:**
- Apache JMeter or k6
- Spring Boot Actuator metrics

**Performance Targets:**
- GraphQL queries: <200ms (p95)
- S3 operations: <500ms (p95)
- Upload throughput: >10MB/s per user
- Support: 100+ concurrent users

**Acceptance Criteria:**
- Performance targets met
- No memory leaks
- Graceful degradation under load

### 6.3 Security Audit (Day 29)
**Objective:** Comprehensive security review

**Tasks:**
- [ ] Verify JWT validation on all endpoints
- [ ] Test authorization bypass attempts
- [ ] Test SQL injection vectors
- [ ] Test XSS vulnerabilities
- [ ] Test CSRF protection
- [ ] Test rate limiting
- [ ] Review CORS configuration
- [ ] Review secrets management
- [ ] Test multi-tenant isolation

**Tools:**
- OWASP ZAP
- Manual testing
- Code review

**Acceptance Criteria:**
- No critical vulnerabilities
- All endpoints properly secured
- Secrets not in code or logs

### 6.4 Documentation (Day 30)
**Objective:** Complete documentation

**Tasks:**
- [ ] Update README.md with IronBucket features
- [ ] Create IRONBUCKET_USER_GUIDE.md
- [ ] Create IRONBUCKET_ADMIN_GUIDE.md
- [ ] Update API documentation
- [ ] Create deployment guide for integrated system
- [ ] Create troubleshooting guide
- [ ] Record demo video

**Acceptance Criteria:**
- All features documented
- User guides easy to follow
- Admin guides comprehensive

---

## Phase 7: Deployment & Production Readiness

**Duration:** Week 7 (5 days)  
**Status:** 🔜 Pending Phase 6

### 7.1 Production Configuration (Day 31-32)
**Objective:** Configure for production deployment

**Tasks:**
- [ ] Configure external PostgreSQL
- [ ] Configure external MinIO cluster
- [ ] Set up TLS/HTTPS for all services
- [ ] Configure production Keycloak realm
- [ ] Set up log aggregation (ELK/Loki)
- [ ] Set up metrics collection (Prometheus)
- [ ] Set up distributed tracing (Jaeger)
- [ ] Configure backup strategy

**Acceptance Criteria:**
- All services use production configs
- TLS enabled everywhere
- Monitoring and logging operational

### 7.2 Kubernetes Deployment (Day 33-34)
**Objective:** Create Kubernetes manifests

**Tasks:**
- [ ] Create Kubernetes deployments for all services
- [ ] Create Services and Ingress
- [ ] Configure resource limits (CPU, memory)
- [ ] Set up horizontal pod autoscaling
- [ ] Configure liveness/readiness probes
- [ ] Create ConfigMaps and Secrets
- [ ] Create Helm chart
- [ ] Test deployment on staging cluster

**Files:**
- `k8s/graphite-forge-deployment.yaml` (new)
- `k8s/ironbucket-deployment.yaml` (new)
- `k8s/ingress.yaml` (new)
- `helm/graphite-forge/` (new chart)

**Acceptance Criteria:**
- Full stack deploys to Kubernetes
- Services auto-scale under load
- Health checks work correctly

### 7.3 CI/CD Pipeline (Day 35)
**Objective:** Automate build, test, deploy

**Tasks:**
- [ ] Create GitHub Actions workflow
- [ ] Add automated testing on PR
- [ ] Add security scanning (SAST, SCA)
- [ ] Add container image scanning
- [ ] Add automated deployment to staging
- [ ] Add manual approval for production
- [ ] Configure rollback strategy

**Files:**
- `.github/workflows/ci-cd.yml` (new)
- `.github/workflows/security-scan.yml` (new)

**Acceptance Criteria:**
- CI/CD runs on every commit
- Tests must pass before merge
- Automated deployment to staging

---

## Success Criteria

### Technical Requirements ✅
- [ ] All IronBucket services integrated and functional
- [ ] GraphQL API covers 100% of IronBucket operations
- [ ] UI supports all client and admin features
- [ ] All tests passing (unit, integration, E2E)
- [ ] Performance targets met
- [ ] Security audit passed
- [ ] Documentation complete

### User Experience ✅
- [ ] Users can browse S3 buckets in UI
- [ ] Users can upload/download files
- [ ] Admins can manage policies
- [ ] Admins can view audit logs
- [ ] UI is responsive and accessible
- [ ] Error messages are clear and actionable

### Operational ✅
- [ ] System deploys via docker-compose
- [ ] System deploys to Kubernetes
- [ ] Monitoring and alerting operational
- [ ] Log aggregation working
- [ ] Backup/restore tested
- [ ] Disaster recovery plan documented

---

## Risk Management

### High Priority Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| IronBucket services not compatible with integration | High | Phase 1 analysis showed good compatibility; proceed with caution |
| Performance degradation with additional services | Medium | Load testing in Phase 6; optimize as needed |
| Complex multi-tenant policy logic | Medium | Thorough testing; start with simple policies |
| Large file upload handling | Medium | Implement chunking and resumable uploads |

### Technical Debt Considerations
- Monitor GraphQL query complexity (depth limits)
- Cache frequently accessed S3 metadata
- Consider CDN for object downloads
- Plan for schema versioning

---

## Milestones & Timeline

| Milestone | Target Date | Status |
|-----------|-------------|--------|
| **Phase 1: Architecture Analysis** | ✅ Jan 17, 2026 | Complete |
| **Phase 2: Infrastructure Setup** | Jan 24, 2026 | 🔜 Next |
| **Phase 3: GraphQL API Extension** | Feb 7, 2026 | Pending |
| **Phase 4: Frontend Client UI** | Feb 14, 2026 | Pending |
| **Phase 5: Admin UI** | Feb 21, 2026 | Pending |
| **Phase 6: Testing & Validation** | Feb 28, 2026 | Pending |
| **Phase 7: Production Deployment** | Mar 7, 2026 | Pending |
| **🚀 Go-Live** | Mar 10, 2026 | Target |

**Total Duration:** 7-8 weeks

---

## Resource Requirements

### Development Team
- **Backend Developer** (Java/Spring Boot): 1 FTE
- **Frontend Developer** (React/Next.js): 1 FTE  
- **DevOps Engineer**: 0.5 FTE
- **QA Engineer**: 0.5 FTE

### Infrastructure
- Development environment (Docker Compose)
- Staging Kubernetes cluster
- Production Kubernetes cluster
- PostgreSQL instances
- MinIO S3 storage
- Keycloak identity provider

### Tools & Services
- GitHub (version control, CI/CD)
- Docker Hub or private registry
- Kubernetes (EKS, GKE, or AKS)
- Monitoring stack (Prometheus, Grafana)
- Log aggregation (ELK or Loki)

---

## Next Steps

### Immediate Actions (Next 48 Hours)
1. ✅ Add IronBucket to .gitignore
2. ✅ Create and commit this roadmap
3. 🔜 Set up development environment with all services
4. 🔜 Begin Phase 2: Infrastructure Setup

### Week 1 Focus
- Resolve IronBucket service code location
- Create integrated docker-compose.yml
- Configure gateway routing
- Test full stack startup

---

## Questions & Decisions Pending

### Architecture Decisions
1. **Database Strategy:**
   - Option A: Shared PostgreSQL with separate schemas ✅ RECOMMENDED
   - Option B: Separate PostgreSQL instances
   - **Decision:** Use shared instance for simplicity

2. **UI Integration:**
   - Option A: Unified UI (all features in Graphite-Forge UI) ✅ RECOMMENDED
   - Option B: Separate admin portal
   - **Decision:** Unified UI with role-based visibility

3. **S3 API Access:**
   - Option A: GraphQL-only for all operations
   - Option B: GraphQL for management, direct S3 API for data operations ✅ RECOMMENDED
   - **Decision:** Hybrid approach - keep S3 proxy passthrough for AWS SDK compatibility

4. **Service Deployment:**
   - Option A: Monorepo - merge all services ✅ RECOMMENDED
   - Option B: Polyrepo - keep separate repositories
   - **Decision:** Monorepo for easier development and testing

### Technical Questions
- Should we implement GitOps policy workflow? (mentioned in IronBucket docs)
- What file size limits for uploads?
- Support for S3 versioning and lifecycle policies?
- CDN integration for object downloads?

---

## Appendix: IronBucket Services Reference

### Service Details

| Service | Port | Purpose | Lines of Code | Tests |
|---------|------|---------|---------------|-------|
| **Brazz-Nossel** | 8082 | S3 Proxy Gateway | ~2,000 | 47 ✅ |
| **Sentinel-Gear** | 8080 | JWT Validator | ~1,500 | 44 ✅ |
| **Claimspindel** | 8081 | Policy Router | ~2,500 | 72 ✅ |
| **Buzzle-Vane** | 8083 | Service Discovery | ~1,000 | 58 ✅ |
| **Storage-Conductor** | N/A | S3 Orchestration | ~500 | 10 ✅ |
| **Vault-Smith** | N/A | Secrets Management | ~300 | Ready |

**Total:** ~8,000 LOC, 231 tests passing

### Technology Stack (IronBucket)
- Java 25
- Spring Boot 4.0.1
- Spring Cloud 2025.1.0
- Spring Cloud Gateway
- Spring Security OAuth2
- R2DBC (Reactive Database)
- PostgreSQL 16
- MinIO (S3-compatible storage)
- Keycloak (OIDC provider)
- Docker Compose

---

## Document Control

**Version:** 1.0  
**Author:** GitHub Copilot (AI Pair Programmer)  
**Date Created:** January 17, 2026  
**Last Updated:** January 17, 2026  
**Status:** Active  
**Next Review:** After Phase 2 completion

---

**Ready to begin Phase 2!** 🚀
