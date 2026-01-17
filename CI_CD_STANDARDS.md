# CI/CD and Security Standards

## Overview

Graphite-Forge implements comprehensive CI/CD and security workflows that comply with IronBucket's standards. This ensures consistent quality, security, and reliability across all deployments.

## GitHub Actions Workflows

### 1. Build and Test ([ci.yml](.github/workflows/ci.yml))

**Purpose:** Continuous integration for all services.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

**Jobs:**
- **build-backend**: Builds and tests all 3 Spring Boot services
  - config-server
  - graphql-service  
  - edge-gateway
- **build-ui**: Builds and tests Next.js frontend
- **test-report**: Generates Sprint 1 TDD progress report

**Artifacts:**
- Backend test results (Surefire reports)
- JAR files for all services
- UI build artifacts (.next/ directory)
- Test summary in GitHub Actions summary

**Standards Met:**
- ✅ Java 25 with Temurin distribution
- ✅ Maven dependency caching
- ✅ Test result parsing and reporting
- ✅ Build artifact retention (90 days)

### 2. Security Scan ([security-scan.yml](.github/workflows/security-scan.yml))

**Purpose:** Comprehensive security vulnerability scanning.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Weekly schedule (Monday 9:00 AM UTC)

**Jobs:**
- **owasp-dependency-check**: Scan Java dependencies for known vulnerabilities
- **spotbugs-analysis**: Static analysis for Java code quality issues
- **checkstyle-analysis**: Code style and quality checks
- **secret-scanning**: Detect committed secrets (TruffleHog)
- **npm-audit**: Scan frontend dependencies
- **eslint-analysis**: Frontend code quality checks
- **security-summary**: Aggregate all security scan results

**Security Standards:**
- ✅ CVSS >= 7.0 threshold for build failures
- ✅ Verified secrets only (TruffleHog)
- ✅ Weekly automated scans
- ✅ Suppression file for false positives ([dependency-check-suppressions.xml](dependency-check-suppressions.xml))

**Artifacts:**
- OWASP Dependency Check reports (HTML, JSON)
- SpotBugs reports (XML, HTML)
- Checkstyle reports (XML)
- npm audit report (JSON)
- ESLint report (JSON)
- All retained for 30 days

### 3. Docker Build and Scan ([docker-build.yml](.github/workflows/docker-build.yml))

**Purpose:** Build, scan, and publish Docker images.

**Triggers:**
- Push to `main` or `develop` branches
- Version tags (`v*`)
- Pull requests to `main` or `develop`

**Jobs:**
- **build-and-scan**: Matrix build for all 4 services
  - config-server
  - graphql-service
  - edge-gateway
  - ui
- **build-summary**: Aggregate build results

**Container Security:**
- ✅ Trivy vulnerability scanning
- ✅ SARIF reports uploaded to GitHub Security
- ✅ Critical/High/Medium severity tracking
- ✅ Multi-platform builds with Buildx
- ✅ Layer caching for faster builds

**Image Registry:**
- Images pushed to GitHub Container Registry (ghcr.io)
- Automatic tagging:
  - Branch name
  - Commit SHA
  - Semantic version (for tags)
  - `latest` (for releases)

### 4. E2E Integration Tests ([e2e-integration-tests.yml](.github/workflows/e2e-integration-tests.yml))

**Purpose:** End-to-end integration testing with IronBucket.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Nightly schedule (2:00 AM UTC)

**Jobs:**
- **e2e-ironbucket**: Full integration test suite
  - Start IronBucket services (MinIO, Keycloak, 6 microservices)
  - Build and start Graphite-Forge services
  - Run scripts/test-e2e.sh
  - Collect test results and logs
- **containerized-tests**: Run scripts/test-containerized.sh
- **test-report-generation**: Generate TDD progress report
- **integration-summary**: Aggregate all test results

**Test Infrastructure:**
- ✅ IronBucket develop branch checkout
- ✅ Docker network: steel-hammer_steel-hammer-network
- ✅ Health checks for all services
- ✅ Phase-based test execution (Infrastructure → Alice → Bob → Validation)
- ✅ JSON test result parsing
- ✅ Log collection and upload

**Artifacts:**
- E2E test results (JSON)
- Service logs
- TDD progress report
- All retained for 30 days

### 5. Release ([release.yml](.github/workflows/release.yml))

**Purpose:** Automated release process for version tags.

**Triggers:**
- Push tags matching `v*.*.*` (semantic versioning)

**Jobs:**
- **validate-tag**: Ensure tag follows semantic versioning
- **build-artifacts**: Build JAR files and UI tarball
- **build-docker-images**: Build and push versioned Docker images
- **generate-changelog**: Auto-generate changelog from git history
- **create-release**: Create GitHub Release with artifacts
- **release-summary**: Display release details

**Release Artifacts:**
- config-server-{version}.jar
- graphql-service-{version}.jar
- edge-gateway-{version}.jar
- ui-build-{version}.tar.gz
- SHA256 checksums for all artifacts
- Automated changelog

**Docker Images:**
- Tagged with semantic version (e.g., `v1.2.3`)
- Tagged with major.minor (e.g., `1.2`)
- Tagged with major (e.g., `1`)
- Tagged with `latest`

## Security Standards Compliance

### OWASP Dependency Check

- **Frequency:** Every push, PR, and weekly
- **Threshold:** CVSS >= 7.0 fails the build
- **Suppressions:** Managed in [dependency-check-suppressions.xml](dependency-check-suppressions.xml)
- **Formats:** JSON and HTML reports

**Usage:**
```bash
cd config-server
mvn org.owasp:dependency-check-maven:check \
  -DfailBuildOnCVSS=7 \
  -DsuppressionFile=../dependency-check-suppressions.xml \
  -Dformat=JSON,HTML
```

### SpotBugs

- **Effort:** Max
- **Threshold:** Low (catches more issues)
- **Purpose:** Static analysis for Java code quality

**Usage:**
```bash
cd graphql-service
mvn compile spotbugs:check -Dspotbugs.effort=Max -Dspotbugs.threshold=Low
```

### Checkstyle

- **Config:** Google Java Style Guide
- **Purpose:** Code style consistency

**Usage:**
```bash
cd edge-gateway
mvn checkstyle:check -Dcheckstyle.config.location=google_checks.xml
```

### TruffleHog

- **Mode:** Verified secrets only
- **Scope:** Git history from base to HEAD
- **Action:** Fails build on detected secrets

### Trivy

- **Targets:** Docker images
- **Severities:** CRITICAL, HIGH, MEDIUM
- **Formats:** SARIF (for GitHub Security) and JSON
- **Integration:** Results appear in GitHub Security tab

### npm audit

- **Threshold:** High severity and above
- **Scope:** UI dependencies (Next.js, React, etc.)
- **Frequency:** Every push and PR

## TDD Progress Tracking

### Sprint 1 Status

- **Tests Written:** 161 (122 backend + 39 frontend)
- **Tests Passing:** 0 (no production code yet - expected)
- **Implementation:** Sprint 2 in progress

### Test Suite Breakdown

**Backend (122 tests):**
- IronBucketS3ServiceTest: 60 tests
- PolicyManagementServiceTest: 70 tests
- AuditLogServiceTest: 50 tests
- S3BucketResolverTest: 20 tests
- S3ObjectResolverTest: 15 tests
- PolicyResolverTest: 15 tests
- IronBucketIntegrationTest: 15 tests

**Frontend (39 tests):**
- BucketList.test.tsx: 35 tests
- UploadDialog.test.tsx: 35 tests
- PolicyEditor.test.tsx: 40 tests

### Progress Reporting

Run the test report script:
```bash
./scripts/test-report.sh
```

This generates:
- Test count by category
- Pass/fail status
- Implementation progress
- Visual checkmarks for completed features

## Local Development

### Running Tests Locally

**Unit Tests:**
```bash
# Backend
cd config-server && mvn test
cd graphql-service && mvn test
cd edge-gateway && mvn test

# Frontend
cd ui && npm test
```

**E2E Tests:**
```bash
# Start IronBucket first
cd ../IronBucket
docker-compose up -d

# Then run E2E tests
cd ../Graphite-Forge
./scripts/test-e2e.sh
```

**Containerized Tests:**
```bash
./scripts/test-containerized.sh
```

### Running Security Scans Locally

**OWASP Dependency Check:**
```bash
cd graphql-service
mvn org.owasp:dependency-check-maven:check
```

**SpotBugs:**
```bash
cd config-server
mvn compile spotbugs:check
```

**npm audit:**
```bash
cd ui
npm audit
```

### Building Docker Images Locally

```bash
# Build all images
docker build -t graphite-forge-config-server:dev config-server/
docker build -t graphite-forge-graphql-service:dev graphql-service/
docker build -t graphite-forge-edge-gateway:dev edge-gateway/
docker build -t graphite-forge-ui:dev ui/

# Scan with Trivy
trivy image graphite-forge-graphql-service:dev
```

## Workflow Artifacts

All workflows upload artifacts for debugging and audit purposes:

| Workflow | Artifact | Retention |
|----------|----------|-----------|
| Build and Test | Test results, JARs, UI build | 90 days |
| Security Scan | OWASP, SpotBugs, Checkstyle, npm audit reports | 30 days |
| Docker Build | Trivy scan reports | 30 days |
| E2E Integration | Test results, service logs | 30 days |
| Release | JAR files, UI tarball, checksums | 90 days |

## Monitoring and Observability

### GitHub Actions Summary

Each workflow generates a summary visible in the GitHub Actions UI:

- **Build and Test:** Test counts, module status, TDD progress
- **Security Scan:** Vulnerability counts by severity
- **Docker Build:** Image tags, scan results
- **E2E Integration:** Test phase results, service health
- **Release:** Artifact list, changelog, Docker image tags

### Notifications

Configure GitHub Actions notifications:
- **Email:** Settings → Notifications → Actions
- **Slack:** Use GitHub Actions Slack integration
- **Custom:** Use webhook actions in workflows

## Best Practices

### Branch Protection

Configure branch protection rules for `main` and `develop`:

1. Require status checks to pass:
   - Build and Test
   - Security Scan
   - Docker Build (for main)
2. Require pull request reviews (1+ approver)
3. Require up-to-date branches
4. Require signed commits (optional)

### Pull Request Workflow

1. Create feature branch from `develop`
2. Implement changes with tests
3. Push to GitHub (triggers CI)
4. Create PR to `develop`
5. Wait for all checks to pass
6. Request review
7. Merge after approval
8. Delete feature branch

### Release Process

1. Ensure all tests pass on `develop`
2. Update version in relevant files
3. Commit and push to `develop`
4. Create and push semantic version tag:
   ```bash
   git tag v1.2.3
   git push origin v1.2.3
   ```
5. Release workflow runs automatically
6. Verify release on GitHub Releases page
7. Merge `develop` to `main`

### Handling Security Vulnerabilities

1. **Review OWASP report:** Check workflow artifacts
2. **Assess risk:** Determine if vulnerability affects your usage
3. **Update dependencies:** Upgrade to patched versions
4. **If false positive:** Add to [dependency-check-suppressions.xml](dependency-check-suppressions.xml)
5. **Document suppression:** Include clear rationale
6. **Rerun security scan:** Verify issue is resolved

### Suppressing False Positives

Edit [dependency-check-suppressions.xml](dependency-check-suppressions.xml):

```xml
<suppress>
    <notes><![CDATA[
        CVE-2023-12345 does not affect our usage because we do not use
        the vulnerable feature (e.g., XML parsing).
    ]]></notes>
    <packageUrl regex="true">^pkg:maven/com\.example/vulnerable\-lib@.*$</packageUrl>
    <cve>CVE-2023-12345</cve>
</suppress>
```

## Troubleshooting

### Workflow Failures

**Build fails on dependency download:**
- Check Maven Central or npm registry status
- Verify `pom.xml` and `package.json` syntax
- Clear dependency cache and retry

**Tests fail unexpectedly:**
- Check if IronBucket services are required
- Verify test data and fixtures
- Review test logs in workflow artifacts

**Security scan fails on false positive:**
- Review OWASP report in artifacts
- Add suppression to [dependency-check-suppressions.xml](dependency-check-suppressions.xml)
- Rerun workflow

**Docker build fails:**
- Check Dockerfile syntax
- Verify base image availability
- Review build logs for missing files

**E2E tests fail:**
- Verify IronBucket checkout succeeded
- Check service health checks
- Review service logs in artifacts

### Common Issues

**"No tests found":**
- Ensure test files match naming convention (`*Test.java`, `*.test.tsx`)
- Verify test annotations (`@Test`, `test()`, `it()`)
- Check Maven Surefire configuration

**"Dependency not found":**
- Run `mvn dependency:resolve` locally
- Check repository configuration in `pom.xml`
- Verify dependency coordinates (groupId, artifactId, version)

**"Out of memory during build":**
- Increase Maven heap size: `MAVEN_OPTS=-Xmx2048m`
- Use parallel builds cautiously: `mvn -T1C`
- Split large modules if necessary

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [OWASP Dependency Check](https://jeremylong.github.io/DependencyCheck/)
- [SpotBugs](https://spotbugs.github.io/)
- [Checkstyle](https://checkstyle.sourceforge.io/)
- [TruffleHog](https://github.com/trufflesecurity/trufflehog)
- [Trivy](https://github.com/aquasecurity/trivy)
- [IronBucket CI/CD Standards](../IronBucket/.github/workflows/README.md)

## Support

For issues or questions:
1. Check workflow logs and artifacts
2. Review this documentation
3. Search [GitHub Issues](https://github.com/YOUR_ORG/Graphite-Forge/issues)
4. Create new issue with workflow run URL
