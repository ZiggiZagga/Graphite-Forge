# Bash Scripts - Testing & Troubleshooting Guide

## 🧪 Script Testing Summary

### ✅ Tested Scenarios

| Script | Test Scenario | Result | Fehlermeldung |
|--------|--------------|--------|---------------|
| `spinup.sh` | Missing prerequisites | ✅ | "❌ [tool] is not installed" |
| `spinup.sh` | Invalid docker-compose.yml | ✅ | "❌ docker-compose.yml has configuration errors" |
| `spinup.sh` | Service startup failure | ✅ | "⚠️ [Service] failed to start - Check logs" |
| `spinup.sh` | Missing IronBucket dir | ✅ | "❌ IronBucket steel-hammer directory not found" |
| `test-e2e.sh` | Services not running | ✅ | "❌ [Service] is NOT running" + "Run ./scripts/spinup.sh" |
| `test-e2e.sh` | Authentication failure | ✅ | "❌ Alice authentication FAILED" |
| `test-e2e.sh` | GraphQL query error | ✅ | "❌ [Operation] failed" + Response ausgabe |
| `test-containerized.sh` | Missing project directory | ✅ | "❌ [service] directory not found" |
| `test-containerized.sh` | Missing pom.xml | ✅ | "❌ [service]/pom.xml not found" |
| `test-containerized.sh` | Docker not available | ✅ | Docker error ausgegeben |

---

## 🔧 Error Handling Features

### Network Isolation with Containers

**Problem:** Tests from host (localhost) can have network issues

**Solution:** Run tests in container (same network as services):

```bash
# Tests in container (recommended)
./scripts/test-e2e.sh --in-container --alice-bob
```

**What happens:**
1. Script checks for IronBucket network (`steel-hammer_ironbucket-network`)
2. If not found, creates temporary network
3. Builds Alpine-based test container (curl, jq, bash)
4. Runs tests using Docker service names (not localhost)
5. Cleans up temporary network on exit

**Advantages:**
- ✅ No localhost/port issues
- ✅ Works when services are in Docker network
- ✅ Automatic network fallback

---

### 1. `spinup.sh`

**Prerequisites-Check:**
```bash
./scripts/spinup.sh

# Fehlende Tools werden erkannt:
❌ docker is not installed
❌ mvn is not installed
```

**Docker-Compose Validation:**
```bash
# Ungültige docker-compose.yml wird erkannt:
❌ docker-compose.yml has configuration errors
⚠️  Run 'docker-compose config' to see details
ℹ  Continuing anyway (some services may fail to start)...
```

**Service Health-Check:**
```bash
# Timeout wenn Service nicht startet:
Waiting for Config Server to be ready ..................
⚠️  Config Server not ready after 120s (may still be starting)
ℹ  Check logs: docker-compose logs config-server
```

**IronBucket Integration:**
```bash
./scripts/spinup.sh --ironbucket

# Fehlende IronBucket Installation:
❌ IronBucket steel-hammer directory not found: /workspaces/Graphite-Forge/IronBucket/steel-hammer
ℹ  Please clone IronBucket repository first
```

---

### 2. `test-e2e.sh`

**Two Execution Modes:**

#### Mode 1: Host-based (localhost)
```bash
./scripts/test-e2e.sh --alice-bob
# Uses localhost URLs (may have network issues)
```

#### Mode 2: Container-based (recommended)
```bash
./scripts/test-e2e.sh --in-container --alice-bob

# Automatically:
# 1. Checks for IronBucket Docker network
# 2. Creates temporary network if needed
# 3. Builds test container
# 4. Runs tests using service names
# 5. Cleans up on exit
```

**Network Detection:**
```bash
# Script checks for these networks (in order):
1. steel-hammer_ironbucket-network (IronBucket)
2. Any network matching "ironbucket" or "graphite"
3. Creates graphite-forge-test-network (temporary)
```

**Infrastructure Check:**
```bash
./scripts/test-e2e.sh --alice-bob

# Services nicht erreichbar:
TEST: Keycloak is accessible
❌ Keycloak is NOT running (http://localhost:7081/...)
❌ Keycloak health
❌ Keycloak is required for authentication
ℹ  Start IronBucket services: ./scripts/spinup.sh --ironbucket

❌ Critical services are not running - cannot continue
ℹ  Run './scripts/spinup.sh --ironbucket' to start all services
```

**Authentication Failure:**
```bash
TEST: Alice authentication
❌ Alice received JWT token
❌ Alice authentication
❌ Alice authentication failed - cannot continue
```

**GraphQL Errors:**
```bash
TEST: Alice creates her bucket
❌ Alice created bucket
⚠️  Response: {"errors":[{"message":"401 Unauthorized"}]}
```

**Tenant Isolation:**
```bash
TEST: Bob lists buckets (tenant isolation)
❌ Bob cannot see Alice's bucket (tenant isolation)
❌ SECURITY VIOLATION: Cross-tenant bucket visibility!
```

---

### 3. `test-containerized.sh`

**Missing Project Structure:**
```bash
./scripts/test-containerized.sh --backend

ℹ  Testing config-server...
❌ config-server directory not found
```

**Missing pom.xml:**
```bash
ℹ  Testing graphql-service...
❌ graphql-service/pom.xml not found
```

**Docker Issues:**
```bash
# Docker nicht installiert:
docker: command not found
❌ Backend tests failed
```

**Test Failures:**
```bash
# Maven Tests fehlgeschlagen:
[ERROR] Tests run: 60, Failures: 60, Errors: 0, Skipped: 0
❌ graphql-service tests failed

# Frontend Tests fehlgeschlagen:
FAIL ui/__tests__/components/ironbucket/client/BucketList.test.tsx
❌ Frontend tests failed
```

---

## 📋 Troubleshooting Guide

### Problem: "docker-compose.yml has configuration errors"

**Ursache:** Services hängen von nicht-existierenden Services ab

**Lösung:**
```bash
# docker-compose.yml prüfen
docker-compose config

# Problem: edge-gateway hängt von eureka-server ab (nicht vorhanden)
# Lösung: Entferne oder kommentiere die depends_on Zeile aus
```

---

### Problem: "Keycloak is NOT running"

**Ursache:** IronBucket Services nicht gestartet

**Lösung:**
```bash
# IronBucket Services starten
./scripts/spinup.sh --ironbucket

# Oder manuell:
cd IronBucket/steel-hammer
export DOCKER_FILES_HOMEDIR="."
docker-compose -f docker-compose-steel-hammer.yml up -d
```

---

### Problem: "Config Server not ready after 120s"

**Ursache:** Config Server startet nicht

**Lösung:**
```bash
# Logs prüfen
docker-compose logs config-server

# Häufige Probleme:
# 1. Port 8888 bereits belegt
lsof -i:8888

# 2. Build-Fehler
cd config-server
mvn clean install

# 3. Fehlende Dependencies
docker-compose down -v
docker-compose up -d --build
```

---

### Problem: "All tests should FAIL (no production code exists)"

**Ursache:** Das ist das **erwartete Verhalten** in Sprint 1 (TDD)

**Lösung:** Dies ist korrekt! Tests wurden zuerst geschrieben (TDD).

```bash
# Backend Tests (erwartetes Ergebnis: FAIL)
cd graphql-service
mvn test

[ERROR] Failures: 60, Errors: 0, Skipped: 0
# ✅ Das ist korrekt! Keine Production-Code existiert noch.

# Frontend Tests (erwartetes Ergebnis: FAIL)
cd ui
npm test

● Cannot find module '@/components/ironbucket/client/BucketList'
# ✅ Das ist korrekt! Komponenten existieren noch nicht.
```

---

## 🎯 Expected Behavior Matrix

| Phase | Command | Expected Result | Meaning |
|-------|---------|----------------|---------|
| **Sprint 1** (Test Creation) | `./scripts/test-containerized.sh` | ❌ ALL FAIL | ✅ Correct - no code yet |
| **Sprint 2** (Implementation) | `./scripts/test-containerized.sh` | 🔄 MIXED | ⏳ In progress |
| **Sprint Complete** | `./scripts/test-containerized.sh` | ✅ ALL PASS | 🎉 Ready! |

---

## 🚀 Quick Reference

### Check if Scripts Work

```bash
# Test help output (should succeed)
./scripts/spinup.sh --help
./scripts/test-e2e.sh --help
./scripts/test-containerized.sh --help

# Test prerequisites check (should list installed tools)
timeout 10 ./scripts/spinup.sh 2>&1 | head -50

# Test E2E without services (should fail with helpful message)
./scripts/test-e2e.sh --alice-bob 2>&1 | head -30
```

### Verify Error Handling

```bash
# Test with missing IronBucket
./scripts/spinup.sh --ironbucket
# Expected: ❌ IronBucket steel-hammer directory not found

# Test E2E without services
./scripts/test-e2e.sh --alice-bob
# Expected: ❌ Critical services are not running

# Test containerized without project structure
cd /tmp && /workspaces/Graphite-Forge/scripts/test-containerized.sh --backend
# Expected: ❌ config-server directory not found
```

---

## ✅ Summary

**Alle Scripts sind getestet und haben:**
- ✅ Hilfreiche Fehlermeldungen
- ✅ Prerequisites-Checks
- ✅ Validierung von Konfigurationsdateien
- ✅ Health-Checks für Services
- ✅ Farbige Ausgabe (Grün/Rot/Gelb/Blau)
- ✅ Log-Dateien mit Timestamps
- ✅ Exit bei kritischen Fehlern
- ✅ Cleanup bei Interrupts (Ctrl+C)
- ✅ Detaillierte Anleitungen bei Fehlern

**Sprint 1 Status:**
- 🎯 Tests sind **absichtlich rot** (TDD-Ansatz)
- 🔧 Scripts funktionieren und zeigen aussagekräftige Fehler
- 📝 Dokumentation ist vollständig
- ✅ Bereit für Sprint 2 (Implementation)
