#!/bin/bash
# Graphite-Forge + IronBucket - End-to-End Test Suite
# Purpose: Run complete E2E tests for IronBucket integration
# Usage: ./test-e2e.sh [--alice-bob] [--full-suite] [--skip-setup]

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
PROJECT_ROOT="/workspaces/Graphite-Forge"
IRONBUCKET_DIR="/workspaces/Graphite-Forge/IronBucket"
STEEL_HAMMER_DIR="/workspaces/Graphite-Forge/IronBucket/steel-hammer"
LOG_FILE="$PROJECT_ROOT/e2e-test-$(date +%Y%m%d-%H%M%S).log"

# Test URLs (use IronBucket shared infrastructure when available)
GATEWAY_URL="http://localhost:8080"           # Sentinel-Gear API Gateway
GRAPHQL_URL="http://localhost:8083"           # GraphQL Service (Graphite-Forge)
KEYCLOAK_URL="http://localhost:7081"          # Keycloak (IronBucket)
MINIO_URL="http://localhost:9000"             # MinIO (IronBucket)

# Test tracking
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Flags
RUN_ALICE_BOB_TEST=false
RUN_FULL_SUITE=false
SKIP_SETUP=false
CLEAN_START=false
RUN_IN_CONTAINER=false

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
        --clean-start)
            CLEAN_START=true
            shift
            ;;
        --in-container)
            RUN_IN_CONTAINER=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./test-e2e.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --alice-bob        Run Alice & Bob multi-tenant test"
            echo "  --full-suite       Run complete test suite"
            echo "  --skip-setup       Skip service health checks"
            echo "  --clean-start      Stop all services, clean up, and start fresh"
            echo "  --in-container     Run tests in Docker container (avoids network issues)"
            echo "  -h, --help         Show this help message"
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
    
    if curl -sf "$url" > /dev/null 2>&1; then
        print_success "$name is running ($url)"
        return 0
    else
        print_error "$name is NOT running ($url)"
        return 1
    fi
}

# ============================================================================
# TEST SUITES
# ============================================================================

# Infrastructure health checks
test_infrastructure() {
    print_section "Shared Infrastructure Health Checks"
    
    local critical_failure=false
    
    print_test "Keycloak (Identity Provider) is accessible"
    if ! check_service "$KEYCLOAK_URL/realms/dev/.well-known/openid-configuration" "Keycloak"; then
        assert_success "Keycloak health" 1
        print_error "Keycloak is required for authentication"
        print_info "Start IronBucket services: ./scripts/spinup.sh --ironbucket"
        critical_failure=true
    else
        assert_success "Keycloak health" 0
    fi
    
    print_test "MinIO (S3 Storage) is accessible"
    if ! check_service "$MINIO_URL/minio/health/live" "MinIO"; then
        assert_success "MinIO health" 1
        print_warning "MinIO is required for S3 operations"
    else
        assert_success "MinIO health" 0
    fi
    
    print_test "Sentinel-Gear (API Gateway) is accessible"
    if ! check_service "$GATEWAY_URL/actuator/health-check" "Sentinel-Gear"; then
        assert_success "Gateway health" 1
        print_warning "Sentinel-Gear is required for API routing"
    else
        assert_success "Gateway health" 0
    fi
    
    print_test "GraphQL service (Graphite-Forge) is accessible"
    if ! check_service "$GRAPHQL_URL/actuator/health" "GraphQL Service"; then
        assert_success "GraphQL health" 1
        print_error "GraphQL service is required for tests"
        print_info "Start services: ./scripts/spinup.sh --ironbucket"
        critical_failure=true
    else
        assert_success "GraphQL health" 0
    fi
    
    if [ "$critical_failure" = true ]; then
        print_error "Critical services are not running - cannot continue"
        print_info "Run './scripts/spinup.sh --ironbucket' to start all services"
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
        
        # Decode claims
        ALICE_CLAIMS=$(echo "$ALICE_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null || echo "{}")
        ALICE_USERNAME=$(echo "$ALICE_CLAIMS" | jq -r '.preferred_username // "unknown"')
        
        print_info "  Username: $ALICE_USERNAME"
        print_info "  Roles: $(echo "$ALICE_CLAIMS" | jq -r '.realm_access.roles[]' 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
    else
        assert_success "Alice authentication" 1
        print_error "Alice authentication failed - cannot continue"
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
        
        BOB_CLAIMS=$(echo "$BOB_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null || echo "{}")
        BOB_USERNAME=$(echo "$BOB_CLAIMS" | jq -r '.preferred_username // "unknown"')
        
        print_info "  Username: $BOB_USERNAME"
        print_info "  Roles: $(echo "$BOB_CLAIMS" | jq -r '.realm_access.roles[]' 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
    else
        assert_success "Bob authentication" 1
        print_error "Bob authentication failed"
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

# Full GraphQL API test suite
test_full_graphql_api() {
    print_section "Full GraphQL API Test Suite"
    
    print_warning "Full test suite not implemented yet"
    print_info "This will include:"
    print_info "  - All S3 operations (bucket CRUD, object CRUD)"
    print_info "  - Policy management (create, update, delete, dry-run)"
    print_info "  - Audit log queries"
    print_info "  - Error handling"
    print_info "  - Performance tests"
    
    echo ""
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    print_header "     Graphite-Forge + IronBucket - E2E Test Suite                 "
    echo ""
    
    # Handle clean start
    if [ "$CLEAN_START" = true ]; then
        print_section "Clean Start: Stopping and Cleaning Services"
        
        print_info "Stopping IronBucket services..."
        cd "$STEEL_HAMMER_DIR" || {
            print_error "Steel Hammer directory not found: $STEEL_HAMMER_DIR"
            exit 1
        }
        docker-compose -f docker-compose-steel-hammer.yml down -v || print_warning "Failed to stop some services"
        
        print_info "Cleaning up Docker resources..."
        cd "$PROJECT_ROOT"
        docker system prune -f > /dev/null 2>&1 || true
        
        print_info "Removing test containers..."
        docker rm -f graphite-forge-e2e steel-hammer-test 2>/dev/null || true
        
        print_success "Cleanup complete"
        echo ""
        
        print_section "Starting Services Fresh"
        
        print_info "Starting IronBucket services..."
        cd "$STEEL_HAMMER_DIR"
        docker-compose -f docker-compose-steel-hammer.yml up -d || {
            print_error "Failed to start IronBucket services"
            exit 1
        }
        
        print_info "Waiting for services to be ready (60 seconds)..."
        sleep 60
        
        cd "$PROJECT_ROOT"
        print_success "Services started"
        echo ""
    fi
    
    # Check if should run in container
    if [ "$RUN_IN_CONTAINER" = true ]; then
        print_info "Running tests in Docker container (same network as services)"
        print_info "This avoids localhost/networking issues"
        echo ""
        
        cd "$PROJECT_ROOT"
        
        # Check if IronBucket network exists
        local network_name="steel-hammer_steel-hammer-network"
        if ! docker network inspect "$network_name" > /dev/null 2>&1; then
            print_warning "IronBucket network '$network_name' not found"
            
            # Try alternative names
            local alt_network="steel-hammer_ironbucket-network"
            if docker network inspect "$alt_network" > /dev/null 2>&1; then
                network_name="$alt_network"
                print_success "Found alternative network: $network_name"
            else
                print_info "Checking for any steel-hammer network..."
                
                # Try to find any steel-hammer network
                local steel_network=$(docker network ls --format '{{.Name}}' | grep -E 'steel-hammer' | head -1)
                
                if [ -n "$steel_network" ]; then
                    network_name="$steel_network"
                    print_success "Using network: $network_name"
                else
                    print_warning "No IronBucket network found"
                    print_info "Creating temporary test network..."
                    docker network create graphite-forge-test-network || {
                        print_error "Failed to create network"
                        exit 1
                    }
                    network_name="graphite-forge-test-network"
                    print_success "Created network: $network_name"
                    print_warning "Note: Start IronBucket services for full integration tests"
                    print_info "Run: ./scripts/spinup.sh --ironbucket"
                fi
            fi
        else
            print_success "Found network: $network_name"
        fi
        
        # Ensure Keycloak users are fully set up for direct grant (alice/bob)
        print_section "Keycloak User Setup"
        print_info "Ensuring Keycloak dev users are initialized..."
        
        if ! bash "$PROJECT_ROOT/scripts/setup-keycloak-dev-users.sh"; then
            print_warning "Keycloak user setup failed - continuing may cause auth failures"
        fi
        
        echo ""
        # Build E2E test image
        print_info "Building E2E test container..."
        docker build -f scripts/Dockerfile.e2e -t graphite-forge-e2e:latest scripts/ || {
            print_error "Failed to build E2E test image"
            exit 1
        }
        
        # Run tests in container
        print_info "Running tests in container..."
        
        local test_args=""
        if [ "$RUN_ALICE_BOB_TEST" = true ]; then
            test_args="--alice-bob"
        elif [ "$RUN_FULL_SUITE" = true ]; then
            test_args="--full-suite"
        fi
        
        if [ "$SKIP_SETUP" = true ]; then
            test_args="$test_args --skip-setup"
        fi
        
        docker run --rm \
            --network "$network_name" \
            -e GATEWAY_URL=http://steel-hammer-sentinel-gear:8080 \
            -e GRAPHQL_URL=http://graphql-service:8083 \
            -e KEYCLOAK_URL=http://steel-hammer-keycloak:7081 \
            -e MINIO_URL=http://steel-hammer-minio:9000 \
            graphite-forge-e2e:latest $test_args
        
        local exit_code=$?
        
        # Cleanup temporary network if created
        if [ "$network_name" = "graphite-forge-test-network" ]; then
            print_info "Cleaning up temporary network..."
            docker network rm graphite-forge-test-network 2>/dev/null || true
        fi
        
        exit $exit_code
    fi
    
    echo -e "  ${MAGENTA}Log file: $LOG_FILE${NC}"
    echo ""
    print_warning "Running tests from host (localhost) - use --in-container for better network isolation"
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
        test_full_graphql_api
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
main 2>&1 | tee "$LOG_FILE"
