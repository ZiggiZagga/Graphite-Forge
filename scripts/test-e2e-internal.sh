#!/bin/bash
# Graphite-Forge + IronBucket - E2E Tests (Container Internal)
# Purpose: Run E2E tests from inside Docker network (avoids localhost issues)
# Usage: Run via docker-compose or Docker run in same network

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration - Use Docker service names (not localhost)
GATEWAY_URL="${GATEWAY_URL:-http://edge-gateway:8080}"
GRAPHQL_URL="${GRAPHQL_URL:-http://graphql-service:8081}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:7081}"
MINIO_URL="${MINIO_URL:-http://minio:9000}"

# Test tracking
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Flags
RUN_ALICE_BOB_TEST=false
RUN_FULL_SUITE=false
SKIP_SETUP=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --alice-bob)
            RUN_ALICE_BOB_TEST=true
            shift
            ;;
        --full-suite)
            RUN_FULL_SUITE=true
            shift
            ;;
        --skip-setup)
            SKIP_SETUP=true
            shift
            ;;
        -h|--help)
            echo "Usage: test-e2e.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --alice-bob     Run Alice & Bob multi-tenant test"
            echo "  --full-suite    Run complete test suite"
            echo "  --skip-setup    Skip service health checks"
            echo "  -h, --help      Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

print_header() {
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}$1${BLUE}║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"
}

print_section() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
}

print_test() {
    echo -e "${YELLOW}TEST: $1${NC}"
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
}

assert_success() {
    local test_name=$1
    local result=$2
    
    if [ "$result" -eq 0 ]; then
        print_success "$test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        print_error "$test_name"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ  $1${NC}"
}

check_service() {
    local url=$1
    local name=$2
    local max_retries=${3:-10}
    local retry=0
    
    while [ $retry -lt $max_retries ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            print_success "$name is running ($url)"
            return 0
        fi
        retry=$((retry + 1))
        if [ $retry -lt $max_retries ]; then
            echo "  Waiting for $name... (attempt $retry/$max_retries)"
            sleep 3
        fi
    done
    
    print_error "$name is NOT running ($url)"
    return 1
}

# ============================================================================
# TEST SUITES
# ============================================================================

# Infrastructure health checks
test_infrastructure() {
    print_section "Infrastructure Health Checks (Docker Network)"
    
    print_info "Testing from inside Docker network"
    print_info "Using service names instead of localhost"
    echo ""
    
    local critical_failure=false
    
    print_test "Keycloak is accessible"
    if ! check_service "$KEYCLOAK_URL/realms/dev/.well-known/openid-configuration" "Keycloak" 20; then
        assert_success "Keycloak health" 1
        print_error "Keycloak is required for authentication"
        critical_failure=true
    else
        assert_success "Keycloak health" 0
    fi
    
    print_test "MinIO is accessible"
    if ! check_service "$MINIO_URL/minio/health/live" "MinIO" 15; then
        assert_success "MinIO health" 1
        print_warning "MinIO is required for S3 operations"
    else
        assert_success "MinIO health" 0
    fi
    
    print_test "Gateway is accessible"
    if ! check_service "$GATEWAY_URL/actuator/health" "Edge Gateway" 20; then
        assert_success "Gateway health" 1
        print_warning "Gateway is required for routing"
    else
        assert_success "Gateway health" 0
    fi
    
    print_test "GraphQL service is accessible"
    if ! check_service "$GRAPHQL_URL/actuator/health" "GraphQL Service" 20; then
        assert_success "GraphQL health" 1
        print_error "GraphQL service is required for tests"
        critical_failure=true
    else
        assert_success "GraphQL health" 0
    fi
    
    if [ "$critical_failure" = true ]; then
        print_error "Critical services are not running - cannot continue"
        exit 1
    fi
    
    echo ""
}

# Alice & Bob multi-tenant test
test_alice_bob() {
    print_section "Alice & Bob Multi-Tenant Test"
    
    # Step 1: Alice authenticates
    print_test "Alice authentication"
    
    ALICE_RESPONSE=$(curl -s -X POST \
        "$KEYCLOAK_URL/realms/dev/protocol/openid-connect/token" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -d 'client_id=dev-client' \
        -d 'client_secret=dev-secret' \
        -d 'username=alice' \
        -d 'password=aliceP@ss' \
        -d 'grant_type=password' \
        -d 'scope=openid profile email roles' 2>/dev/null)
    
    ALICE_TOKEN=$(echo "$ALICE_RESPONSE" | jq -r '.access_token // empty' 2>/dev/null)
    
    if [ -n "$ALICE_TOKEN" ] && [ "$ALICE_TOKEN" != "null" ]; then
        assert_success "Alice received JWT token" 0
        
        # Decode claims (base64 decode with padding fix)
        ALICE_PAYLOAD=$(echo "$ALICE_TOKEN" | cut -d'.' -f2)
        # Add padding if needed
        ALICE_PAYLOAD="${ALICE_PAYLOAD}$(printf '=' $(( (4 - ${#ALICE_PAYLOAD} % 4) % 4 )))"
        ALICE_CLAIMS=$(echo "$ALICE_PAYLOAD" | base64 -d 2>/dev/null || echo "{}")
        ALICE_USERNAME=$(echo "$ALICE_CLAIMS" | jq -r '.preferred_username // "unknown"')
        
        print_info "  Username: $ALICE_USERNAME"
        print_info "  Roles: $(echo "$ALICE_CLAIMS" | jq -r '.realm_access.roles[]' 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
    else
        assert_success "Alice authentication" 1
        print_error "Alice authentication failed - cannot continue"
        print_warning "Response: $ALICE_RESPONSE"
        return 1
    fi
    
    # Step 2: Bob authenticates
    print_test "Bob authentication"
    
    BOB_RESPONSE=$(curl -s -X POST \
        "$KEYCLOAK_URL/realms/dev/protocol/openid-connect/token" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -d 'client_id=dev-client' \
        -d 'client_secret=dev-secret' \
        -d 'username=bob' \
        -d 'password=bobP@ss' \
        -d 'grant_type=password' \
        -d 'scope=openid profile email roles' 2>/dev/null)
    
    BOB_TOKEN=$(echo "$BOB_RESPONSE" | jq -r '.access_token // empty' 2>/dev/null)
    
    if [ -n "$BOB_TOKEN" ] && [ "$BOB_TOKEN" != "null" ]; then
        assert_success "Bob received JWT token" 0
        
        BOB_PAYLOAD=$(echo "$BOB_TOKEN" | cut -d'.' -f2)
        BOB_PAYLOAD="${BOB_PAYLOAD}$(printf '=' $(( (4 - ${#BOB_PAYLOAD} % 4) % 4 )))"
        BOB_CLAIMS=$(echo "$BOB_PAYLOAD" | base64 -d 2>/dev/null || echo "{}")
        BOB_USERNAME=$(echo "$BOB_CLAIMS" | jq -r '.preferred_username // "unknown"')
        
        print_info "  Username: $BOB_USERNAME"
        print_info "  Roles: $(echo "$BOB_CLAIMS" | jq -r '.realm_access.roles[]' 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
    else
        assert_success "Bob authentication" 1
        print_error "Bob authentication failed"
        print_warning "Response: $BOB_RESPONSE"
        return 1
    fi
    
    # Step 3: Alice creates bucket via GraphQL
    print_test "Alice creates her bucket"
    
    CREATE_BUCKET_QUERY='mutation { createBucket(input: { name: "alice-bucket", region: "us-east-1" }) { name createdAt } }'
    
    ALICE_CREATE_RESPONSE=$(curl -s -X POST \
        "$GRAPHQL_URL/graphql" \
        -H "Authorization: Bearer $ALICE_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"query\":\"$CREATE_BUCKET_QUERY\"}" 2>/dev/null)
    
    if echo "$ALICE_CREATE_RESPONSE" | grep -q "alice-bucket"; then
        assert_success "Alice created bucket" 0
    else
        assert_success "Alice created bucket" 1
        print_warning "Response: $ALICE_CREATE_RESPONSE"
    fi
    
    # Step 4: Alice lists her buckets
    print_test "Alice lists her buckets"
    
    LIST_BUCKETS_QUERY='query { listBuckets { name owner createdAt } }'
    
    ALICE_LIST_RESPONSE=$(curl -s -X POST \
        "$GRAPHQL_URL/graphql" \
        -H "Authorization: Bearer $ALICE_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"query\":\"$LIST_BUCKETS_QUERY\"}" 2>/dev/null)
    
    if echo "$ALICE_LIST_RESPONSE" | grep -q "alice-bucket"; then
        assert_success "Alice sees her bucket" 0
    else
        assert_success "Alice sees her bucket" 1
    fi
    
    # Step 5: Bob lists buckets (should NOT see Alice's bucket)
    print_test "Bob lists buckets (tenant isolation)"
    
    BOB_LIST_RESPONSE=$(curl -s -X POST \
        "$GRAPHQL_URL/graphql" \
        -H "Authorization: Bearer $BOB_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"query\":\"$LIST_BUCKETS_QUERY\"}" 2>/dev/null)
    
    if echo "$BOB_LIST_RESPONSE" | grep -q "alice-bucket"; then
        assert_success "Bob cannot see Alice's bucket (tenant isolation)" 1
        print_error "SECURITY VIOLATION: Cross-tenant bucket visibility!"
    else
        assert_success "Bob cannot see Alice's bucket (tenant isolation)" 0
    fi
    
    # Step 6: Alice uploads object
    print_test "Alice uploads object"
    
    UPLOAD_MUTATION='mutation { uploadObject(input: { bucket: "alice-bucket", key: "test-file.txt", contentType: "text/plain" }) { bucket key size etag } }'
    
    ALICE_UPLOAD_RESPONSE=$(curl -s -X POST \
        "$GRAPHQL_URL/graphql" \
        -H "Authorization: Bearer $ALICE_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"query\":\"$UPLOAD_MUTATION\"}" 2>/dev/null)
    
    if echo "$ALICE_UPLOAD_RESPONSE" | grep -q "test-file.txt"; then
        assert_success "Alice uploaded object" 0
    else
        assert_success "Alice uploaded object" 1
        print_warning "Response: $ALICE_UPLOAD_RESPONSE"
    fi
    
    # Step 7: Cleanup - Alice deletes bucket
    print_test "Alice deletes her bucket"
    
    DELETE_BUCKET_MUTATION='mutation { deleteBucket(name: "alice-bucket", cascade: true) }'
    
    ALICE_DELETE_RESPONSE=$(curl -s -X POST \
        "$GRAPHQL_URL/graphql" \
        -H "Authorization: Bearer $ALICE_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"query\":\"$DELETE_BUCKET_MUTATION\"}" 2>/dev/null)
    
    if echo "$ALICE_DELETE_RESPONSE" | grep -q "true"; then
        assert_success "Alice deleted bucket" 0
    else
        assert_success "Alice deleted bucket" 1
    fi
    
    echo ""
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    print_header "   Graphite-Forge E2E Tests (Container Network)                   "
    echo ""
    print_info "Running from inside Docker network"
    echo ""

    # Pre-flight checks
    if [ "$SKIP_SETUP" = false ]; then
        test_infrastructure
    else
        print_info "Skipping infrastructure checks (--skip-setup)"
        echo ""
    fi

    # Run selected test suite
    if [ "$RUN_ALICE_BOB_TEST" = true ]; then
        test_alice_bob
    fi
    
    if [ "$RUN_FULL_SUITE" = true ]; then
        test_alice_bob
        # Add more test suites here
    fi
    
    # If no test suite specified, run default
    if [ "$RUN_ALICE_BOB_TEST" = false ] && [ "$RUN_FULL_SUITE" = false ]; then
        test_alice_bob
    fi

    # Test summary
    print_section "Test Summary"
    
    echo -e "${CYAN}Results:${NC}"
    echo ""
    echo -e "  Total Tests:     ${BLUE}$TESTS_TOTAL${NC}"
    echo -e "  Passed:          ${GREEN}$TESTS_PASSED${NC}"
    echo -e "  Failed:          ${RED}$TESTS_FAILED${NC}"
    echo ""
    
    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║                                        ║${NC}"
        echo -e "${GREEN}║   ✅ All Tests Passed Successfully!   ║${NC}"
        echo -e "${GREEN}║                                        ║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
        exit 0
    else
        echo -e "${RED}╔════════════════════════════════════════╗${NC}"
        echo -e "${RED}║                                        ║${NC}"
        echo -e "${RED}║   ❌ Some Tests Failed                ║${NC}"
        echo -e "${RED}║                                        ║${NC}"
        echo -e "${RED}╚════════════════════════════════════════╝${NC}"
        exit 1
    fi
}

# Run main
main
