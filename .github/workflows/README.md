# GitHub Actions Workflows

This directory contains the CI/CD pipeline workflows for Graphite-Forge with IronBucket integration.

## 🚀 Workflows Overview

### 1. `build-and-test.yml`
**Purpose:** Continuous Integration - Build and test all modules

**Triggers:**
- Push to `main` or `develop`
- Pull requests to `main` or `develop`

**What it does:**
- Builds all Spring Boot services (config-server, edge-gateway, graphql-service)
- Runs all tests (Sprint 1: 161 tests, Sprint 2+: implementation tests)
- Builds Next.js UI
- Uploads test results and artifacts

**Expected outcome:** All builds and tests passing ✅

---

### 2. `security-scan.yml`
**Purpose:** Security scanning and code quality

**Triggers:**
- Push to `main` or `develop`
- Pull requests to `main`
- Weekly schedule (Mondays)

**Scans:**
- **OWASP Dependency Check** - CVE scanning (Java & Node.js)
- **SpotBugs** - Static analysis for Java
- **ESLint** - Code quality for TypeScript/React
- **TruffleHog** - Secret detection
- **npm audit** - Node.js dependency vulnerabilities

**Expected outcome:** No critical vulnerabilities ✅

---

### 3. `docker-build.yml`
**Purpose:** Build and scan Docker images

**Triggers:**
- Push to `main` or `develop`
- Pull requests to `main`

**What it does:**
- Builds Docker images for all services
- Scans with Trivy
- Uploads scan results to GitHub Security
- Pushes images to GHCR (main only)

**Services:**
- `config-server`
- `edge-gateway`
- `graphql-service`
- `ui` (Next.js)

**Expected outcome:** Secure container images ✅

---

### 4. `e2e-integration-tests.yml`
**Purpose:** End-to-end integration tests with IronBucket

**Triggers:**
- Push to `main` or `develop`
- Pull requests to `main`
- Manual dispatch

**What it does:**
- Starts IronBucket services (Keycloak, MinIO, microservices)
- Builds Graphite-Forge services
- Runs containerized E2E tests
- Tests Alice & Bob multi-tenant scenario
- Validates GraphQL operations

**Expected outcome:** All integration tests passing ✅

---

### 5. `release.yml`
**Purpose:** Automated release pipeline

**Triggers:**
- Tag creation (`v*.*.*`)
- Manual dispatch with version

**Release process:**
1. ✅ Validate version and tests
2. ✅ Build release artifacts
3. ✅ Create GitHub Release
4. ✅ Build and push Docker images
5. ✅ Generate release notes

**Expected outcome:** Complete release with artifacts ✅

---

## 🔒 Security Features

### Dependency Scanning
- **Java**: OWASP Dependency Check for all Maven modules
- **Node.js**: npm audit for UI dependencies
- **Fail on**: CVSS >= 7.0 (HIGH or CRITICAL)

### Static Analysis
- **SpotBugs**: Java bug detection
- **ESLint**: TypeScript/React linting
- **Code style**: Automated checks

### Secret Detection
- **TruffleHog**: Scans for leaked credentials
- **Verified secrets only**: Reduces false positives

### Container Security
- **Trivy**: CVE scanning for Docker images
- **Security tab integration**: Results visible in GitHub Security

---

## 📊 Test Coverage

### Sprint 1 (TDD Phase)
- **161 tests written** (tests BEFORE implementation)
- Backend: 122 tests
- Frontend: 39 tests
- All tests expected to fail (no production code)

### Sprint 2+ (Implementation Phase)
- Tests gradually passing as code is implemented
- Target: 100% test passing rate
- Coverage reports uploaded as artifacts

---

## 🏷️ Workflow Status Badges

Add these to README.md:

```markdown
![Build Status](https://github.com/ZiggiZagga/Graphite-Forge/actions/workflows/build-and-test.yml/badge.svg)
![Security Scan](https://github.com/ZiggiZagga/Graphite-Forge/actions/workflows/security-scan.yml/badge.svg)
![Docker Build](https://github.com/ZiggiZagga/Graphite-Forge/actions/workflows/docker-build.yml/badge.svg)
![E2E Tests](https://github.com/ZiggiZagga/Graphite-Forge/actions/workflows/e2e-integration-tests.yml/badge.svg)
```

---

## 🔧 Manual Workflow Dispatch

Some workflows can be triggered manually via GitHub Actions UI:
- `e2e-integration-tests.yml` - Run E2E tests on demand
- `release.yml` - Create release manually

---

## 📝 Workflow Maintenance

### Adding New Services
1. Add build step to `build-and-test.yml`
2. Add Docker build to `docker-build.yml`
3. Add dependency scan to `security-scan.yml`

### Updating Java/Node Versions
- Java version: Update `java-version` in all workflows
- Node version: Update `node-version` in UI workflows

### Security Suppressions
- Edit `.github/dependency-check-suppressions.xml` for false positives
- Document each suppression with justification

---

## 🚀 Getting Started

### Local Development
```bash
# Run build locally (matches CI)
mvn clean test

# Run E2E tests locally
./scripts/test-e2e.sh --in-container --alice-bob

# Run security scans locally
mvn org.owasp:dependency-check-maven:check
```

### Testing Workflows Locally
Use [act](https://github.com/nektos/act) to test GitHub Actions locally:
```bash
act -j build-backend
```

---

## 📚 References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [TruffleHog](https://github.com/trufflesecurity/trufflehog)
- [Trivy](https://github.com/aquasecurity/trivy)
