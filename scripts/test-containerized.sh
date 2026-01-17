#!/bin/bash
# Graphite-Forge - Containerized Test Runner
# Purpose: Run all tests inside Docker containers (isolated environment)
# Usage: ./test-containerized.sh [--backend] [--frontend] [--e2e]

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
LOG_FILE="$PROJECT_ROOT/test-containerized-$(date +%Y%m%d-%H%M%S).log"

# Test types
RUN_BACKEND_TESTS=false
RUN_FRONTEND_TESTS=false
RUN_E2E_TESTS=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --backend)
            RUN_BACKEND_TESTS=true
            shift
            ;;
        --frontend)
            RUN_FRONTEND_TESTS=true
            shift
            ;;
        --e2e)
            RUN_E2E_TESTS=true
            shift
            ;;
        --all)
            RUN_BACKEND_TESTS=true
            RUN_FRONTEND_TESTS=true
            RUN_E2E_TESTS=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./test-containerized.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --backend       Run backend JUnit tests"
            echo "  --frontend      Run frontend Jest tests"
            echo "  --e2e           Run E2E integration tests"
            echo "  --all           Run all test suites"
            echo "  -h, --help      Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Default: run all tests
if [ "$RUN_BACKEND_TESTS" = false ] && [ "$RUN_FRONTEND_TESTS" = false ] && [ "$RUN_E2E_TESTS" = false ]; then
    RUN_BACKEND_TESTS=true
    RUN_FRONTEND_TESTS=true
    RUN_E2E_TESTS=true
fi

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

# ============================================================================
# TEST RUNNERS
# ============================================================================

run_backend_tests() {
    print_section "Backend Tests (JUnit + Mockito)"
    
    cd "$PROJECT_ROOT"
    
    # Run tests in config-server
    print_info "Testing config-server..."
    
    if [ ! -d "$PROJECT_ROOT/config-server" ]; then
        print_error "config-server directory not found"
        return 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/config-server/pom.xml" ]; then
        print_error "config-server/pom.xml not found"
        return 1
    fi
    
    cd "$PROJECT_ROOT/config-server"
    
    if ! docker run --rm \
        -v "$PWD":/workspace \
        -w /workspace \
        maven:3.9-eclipse-temurin-25 \
        mvn clean test 2>&1 | tee -a "$LOG_FILE"; then
        print_error "config-server tests failed"
        local config_exit=1
    else
        print_success "config-server tests passed"
        local config_exit=0
    fi
    
    # Run tests in graphql-service
    print_info "Testing graphql-service..."
    
    if [ ! -d "$PROJECT_ROOT/graphql-service" ]; then
        print_error "graphql-service directory not found"
        return 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/graphql-service/pom.xml" ]; then
        print_error "graphql-service/pom.xml not found"
        return 1
    fi
    
    cd "$PROJECT_ROOT/graphql-service"
    
    if ! docker run --rm \
        -v "$PWD":/workspace \
        -w /workspace \
        maven:3.9-eclipse-temurin-25 \
        mvn clean test 2>&1 | tee -a "$LOG_FILE"; then
        print_error "graphql-service tests failed"
        local graphql_exit=1
    else
        print_success "graphql-service tests passed"
        local graphql_exit=0
    fi
    
    # Run tests in edge-gateway
    print_info "Testing edge-gateway..."
    
    if [ ! -d "$PROJECT_ROOT/edge-gateway" ]; then
        print_error "edge-gateway directory not found"
        return 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/edge-gateway/pom.xml" ]; then
        print_error "edge-gateway/pom.xml not found"
        return 1
    fi
    
    cd "$PROJECT_ROOT/edge-gateway"
    
    if ! docker run --rm \
        -v "$PWD":/workspace \
        -w /workspace \
        maven:3.9-eclipse-temurin-25 \
        mvn clean test 2>&1 | tee -a "$LOG_FILE"; then
        print_error "edge-gateway tests failed"
        local gateway_exit=1
    else
        print_success "edge-gateway tests passed"
        local gateway_exit=0
    fi
    
    echo ""
    
    # Return failure if any test suite failed
    if [ $config_exit -ne 0 ] || [ $graphql_exit -ne 0 ] || [ $gateway_exit -ne 0 ]; then
        return 1
    fi
    
    return 0
}

run_frontend_tests() {
    print_section "Frontend Tests (Jest + React Testing Library)"
    
    if [ ! -d "$PROJECT_ROOT/ui" ]; then
        print_error "ui directory not found at $PROJECT_ROOT/ui"
        return 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/ui/package.json" ]; then
        print_error "ui/package.json not found"
        return 1
    fi
    
    cd "$PROJECT_ROOT/ui"
    
    print_info "Running Jest tests..."
    
    if ! docker run --rm \
        -v "$PWD":/workspace \
        -w /workspace \
        node:22-alpine \
        sh -c "npm ci && npm test -- --passWithNoTests" 2>&1 | tee -a "$LOG_FILE"; then
        print_error "Frontend tests failed"
        local exit_code=1
    else
        print_success "Frontend tests passed"
        local exit_code=0
    fi
    
    echo ""
    
    return $exit_code
}

run_e2e_tests() {
    print_section "E2E Integration Tests"
    
    print_info "Starting services for E2E tests..."
    
    cd "$PROJECT_ROOT"
    
    # Start services in detached mode
    docker-compose up -d
    
    # Wait for services to be ready
    print_info "Waiting for services to be ready..."
    sleep 30
    
    # Run E2E test script
    if [ -f "$PROJECT_ROOT/scripts/test-e2e.sh" ]; then
        "$PROJECT_ROOT/scripts/test-e2e.sh" --skip-setup
        local exit_code=$?
    else
        print_warning "E2E test script not found"
        exit_code=1
    fi
    
    # Stop services
    print_info "Stopping services..."
    docker-compose down
    
    if [ $exit_code -eq 0 ]; then
        print_success "E2E tests passed"
    else
        print_error "E2E tests failed"
    fi
    
    echo ""
    
    return $exit_code
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    print_header "     Graphite-Forge - Containerized Test Suite                    "
    echo ""
    echo -e "  ${MAGENTA}Log file: $LOG_FILE${NC}"
    echo ""

    local all_passed=true

    # Run selected test suites
    if [ "$RUN_BACKEND_TESTS" = true ]; then
        if ! run_backend_tests; then
            all_passed=false
        fi
    fi
    
    if [ "$RUN_FRONTEND_TESTS" = true ]; then
        if ! run_frontend_tests; then
            all_passed=false
        fi
    fi
    
    if [ "$RUN_E2E_TESTS" = true ]; then
        if ! run_e2e_tests; then
            all_passed=false
        fi
    fi

    # Final summary
    print_section "Test Summary"
    
    if [ "$all_passed" = true ]; then
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
