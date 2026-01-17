#!/bin/bash
# Graphite-Forge + IronBucket Integration - Unified Spin-Up Script
# Purpose: Spin up all Graphite-Forge services with IronBucket integration
# Usage: ./spinup.sh [--ironbucket] [--rebuild] [--debug] [--logs]

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
STEEL_HAMMER_DIR="$IRONBUCKET_DIR/steel-hammer"
LOG_FILE="$PROJECT_ROOT/spinup-$(date +%Y%m%d-%H%M%S).log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Service URLs - Graphite-Forge services
GRAPHQL_URL="http://localhost:8083"
UI_URL="http://localhost:3000"

# Shared IronBucket service URLs (Gateway, Auth, Storage, Registry)
GATEWAY_URL="http://localhost:8080"           # Sentinel-Gear (IronBucket gateway)
KEYCLOAK_URL="http://localhost:7081"          # Identity & Auth
MINIO_URL="http://localhost:9000"             # S3 Object Storage
EUREKA_URL="http://localhost:8083/eureka"     # Service Registry

# Flags
START_IRONBUCKET=false
WITH_IRONBUCKET=false
REBUILD_SERVICES=false
DEBUG_MODE=false
SHOW_LOGS=false

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --ironbucket)
            START_IRONBUCKET=true
            shift
            ;;
        --with-ironbucket)
            WITH_IRONBUCKET=true
            START_IRONBUCKET=true
            shift
            ;;
        --rebuild)
            REBUILD_SERVICES=true
            shift
            ;;
        --debug)
            DEBUG_MODE=true
            shift
            ;;
        --logs)
            SHOW_LOGS=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./spinup.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --ironbucket         Start IronBucket infrastructure (Keycloak, MinIO, Gateway, etc.)"
            echo "                       + automatically setup alice/bob users for E2E"
            echo "  --with-ironbucket    Start Graphite-Forge services with IronBucket integration"
            echo "  --rebuild            Rebuild all services before starting"
            echo "  --debug              Enable debug logging"
            echo "  --logs               Show service logs after startup"
            echo "  -h, --help           Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
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

print_step() {
    echo ""
    echo -e "${YELLOW}▶ Step $1: $2${NC}"
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

check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 is not installed"
        return 1
    fi
    print_success "$1 installed"
    return 0
}

wait_for_service() {
    local url=$1
    local name=$2
    local timeout=${3:-60}
    local elapsed=0

    echo -n "Waiting for $name to be ready "
    
    while [ $elapsed -lt $timeout ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            echo ""
            print_success "$name is ready"
            return 0
        fi
        echo -n "."
        sleep 2
        elapsed=$((elapsed + 2))
    done

    echo ""
    print_warning "$name not ready after ${timeout}s (may still be starting)"
    return 1
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    print_header "     Graphite-Forge + IronBucket - Unified Spin-Up                 "
    echo ""
    echo -e "  ${MAGENTA}Timestamp: $TIMESTAMP${NC}"
    echo -e "  ${MAGENTA}Log file: $LOG_FILE${NC}"
    echo ""

    if [ "$START_IRONBUCKET" = true ]; then
        print_info "IronBucket integration: ENABLED"
    else
        print_info "IronBucket integration: DISABLED (use --ironbucket to enable)"
    fi
    echo ""

    # Step 1: Verify prerequisites
    print_step "1" "Verify Prerequisites"
    
    local all_checks_pass=true
    
    if ! check_command "docker"; then
        all_checks_pass=false
    fi
    
    if ! check_command "docker-compose"; then
        all_checks_pass=false
    fi
    
    if ! check_command "java"; then
        all_checks_pass=false
    fi
    
    if ! check_command "mvn"; then
        all_checks_pass=false
    fi
    
    if ! check_command "node"; then
        all_checks_pass=false
    fi
    
    if ! check_command "npm"; then
        all_checks_pass=false
    fi
    
    if ! check_command "jq"; then
        print_warning "jq not installed (optional but recommended)"
    fi
    
    if [ "$all_checks_pass" = false ]; then
        print_error "Prerequisites check failed"
        exit 1
    fi
    
    echo ""

    # Step 2: Clean up old containers (optional)
    print_step "2" "Clean Up Old Containers"
    
    cd "$PROJECT_ROOT"
    
    if docker-compose ps | grep -q "graphite-forge"; then
        print_info "Stopping old Graphite-Forge containers..."
        docker-compose down -v 2>/dev/null || true
        print_success "Old containers removed"
    else
        print_success "No old containers found"
    fi
    
    echo ""

    # Step 3: Start IronBucket services (if requested)
    if [ "$START_IRONBUCKET" = true ]; then
        print_step "3" "Start IronBucket Services"
        
        if [ ! -d "$STEEL_HAMMER_DIR" ]; then
            print_error "IronBucket steel-hammer directory not found: $STEEL_HAMMER_DIR"
            print_info "Please clone IronBucket repository first"
            exit 1
        fi
        
        cd "$STEEL_HAMMER_DIR"
        export DOCKER_FILES_HOMEDIR="."
        
        print_info "Starting IronBucket infrastructure (Keycloak, MinIO, PostgreSQL)..."
        docker-compose -f docker-compose-steel-hammer.yml up -d
        
        print_success "IronBucket services started"
        
        # Wait for Keycloak
        wait_for_service "$KEYCLOAK_URL/realms/dev/.well-known/openid-configuration" "Keycloak" 120
        
        # Wait for MinIO
        wait_for_service "$MINIO_URL/minio/health/live" "MinIO" 60
        
        # Step 3b: Set up Keycloak dev users (alice/bob) for E2E testing
        print_step "3b" "Configure Keycloak Development Users"
        cd "$PROJECT_ROOT"
        
        print_info "Setting up alice/bob users for direct grant flows..."
        if bash "$PROJECT_ROOT/scripts/setup-keycloak-dev-users.sh"; then
            print_success "Keycloak users configured (alice/bob ready)"
        else
            print_error "Keycloak user setup failed - E2E tests may fail"
            exit 1
        fi
        
        echo ""
    echo ""

    # Step 4: Start Graphite-Forge services via Docker Compose
    print_step "4" "Start Graphite-Forge Services (GraphQL + UI)"
    
    cd "$PROJECT_ROOT"
    
    # Validate docker-compose.yml exists
    if [ ! -f "docker-compose.yml" ]; then
        print_error "docker-compose.yml not found in $PROJECT_ROOT"
        exit 1
    fi
    
    # Validate docker-compose syntax
    print_info "Validating docker-compose.yml..."
    if ! docker-compose config > /dev/null 2>&1; then
        print_error "docker-compose.yml has configuration errors"
        print_warning "Run 'docker-compose config' to see details"
        print_info "Continuing anyway (some services may fail to start)..."
    else
        print_success "docker-compose.yml validated"
    fi
    
    print_info "Starting services via docker-compose..."
    
    # If --with-ironbucket, ensure services use IronBucket network
    if [ "$WITH_IRONBUCKET" = true ]; then
        print_info "Connecting Graphite-Forge services to IronBucket network..."
        
        # Check if IronBucket network exists
        IRONBUCKET_NETWORK="steel-hammer_steel-hammer-network"
        if ! docker network inspect "$IRONBUCKET_NETWORK" > /dev/null 2>&1; then
            print_error "IronBucket network not found: $IRONBUCKET_NETWORK"
            print_info "Start IronBucket services first: ./spinup.sh --ironbucket"
            exit 1
        fi
        
        print_success "Found IronBucket network: $IRONBUCKET_NETWORK"
        
        # Export network name for docker-compose
        export IRONBUCKET_NETWORK="$IRONBUCKET_NETWORK"
        
        # Create custom docker-compose override for network
        cat > "$PROJECT_ROOT/docker-compose.override.yml" <<EOF
version: '3.8'

services:
  config-server:
    networks:
      - ironbucket-network
  
  edge-gateway:
    networks:
      - ironbucket-network
  
  graphql-service:
    networks:
      - ironbucket-network
    environment:
      - KEYCLOAK_URL=http://steel-hammer-keycloak:7081
      - MINIO_URL=http://steel-hammer-minio:9000
      - EUREKA_URL=http://steel-hammer-buzzle-vane:8083/eureka

networks:
  ironbucket-network:
    external: true
    name: steel-hammer_steel-hammer-network
EOF
        
        print_success "Created docker-compose.override.yml for IronBucket network"
    fi
    
    if docker-compose up -d 2>&1 | tee -a "$LOG_FILE"; then
        print_success "Docker Compose services started"
    else
        print_error "Failed to start some services"
        print_warning "Check logs: $LOG_FILE"
        print_info "Continuing with health checks..."
    fi
    
    echo ""

    # Step 5: Wait for Graphite-Forge services to be ready
    print_step "5" "Wait for Services to Initialize"
    
    # GraphQL Service (primary Graphite-Forge service)
    if ! wait_for_service "$GRAPHQL_URL/actuator/health" "GraphQL Service" 120; then
        print_warning "GraphQL Service failed to start"
        print_info "Check logs: docker-compose logs graphql-service"
    fi
    
    echo ""

    # Step 6: Start UI (Next.js development server)
    print_step "6" "Start Next.js UI"
    
    cd "$PROJECT_ROOT/ui"
    
    if [ ! -d "node_modules" ]; then
        print_info "Installing Node.js dependencies..."
        npm install
        print_success "Dependencies installed"
    fi
    
    print_info "Starting Next.js development server..."
    
    # Kill any existing Next.js process on port 3000
    lsof -ti:3000 | xargs kill -9 2>/dev/null || true
    
    # Start Next.js in background
    npm run dev > "$PROJECT_ROOT/ui-$(date +%Y%m%d-%H%M%S).log" 2>&1 &
    UI_PID=$!
    
    echo $UI_PID > "$PROJECT_ROOT/.ui.pid"
    
    print_success "UI started (PID: $UI_PID)"
    
    # Wait for UI to be ready
    wait_for_service "$UI_URL" "Next.js UI" 60
    
    echo ""

    # Step 7: Service health checks
    print_step "7" "Service Health Checks"
    
    echo "Checking service endpoints..."
    echo ""
    
    # GraphQL Service
    if curl -sf "$GRAPHQL_URL/actuator/health" | grep -q "UP"; then
        print_success "GraphQL Service: UP ($GRAPHQL_URL)"
    else
        print_warning "GraphQL Service: DOWN ($GRAPHQL_URL)"
    fi
    
    # UI
    if curl -sf "$UI_URL" > /dev/null 2>&1; then
        print_success "Next.js UI: UP ($UI_URL)"
    else
        print_warning "Next.js UI: DOWN ($UI_URL)"
    fi
    
    # IronBucket services (if enabled)
    if [ "$START_IRONBUCKET" = true ]; then
        echo ""
        echo "Shared IronBucket Infrastructure:"
        
        if curl -sf "$GATEWAY_URL/actuator/health-check" > /dev/null 2>&1; then
            print_success "Sentinel-Gear (API Gateway): UP ($GATEWAY_URL)"
        else
            print_warning "Sentinel-Gear (API Gateway): DOWN ($GATEWAY_URL)"
        fi
        
        if curl -sf "$KEYCLOAK_URL/realms/dev/.well-known/openid-configuration" > /dev/null 2>&1; then
            print_success "Keycloak (Identity): UP ($KEYCLOAK_URL)"
        else
            print_warning "Keycloak (Identity): DOWN ($KEYCLOAK_URL)"
        fi
        
        if curl -sf "$MINIO_URL/minio/health/live" > /dev/null 2>&1; then
            print_success "MinIO (S3 Storage): UP ($MINIO_URL)"
        else
            print_warning "MinIO (S3 Storage): DOWN ($MINIO_URL)"
        fi
    fi
    
    echo ""

    # Step 8: Summary
    print_section "🎉 Graphite-Forge is Ready!"
    
    echo -e "${CYAN}Graphite-Forge Services:${NC}"
    echo ""
    echo -e "  📊 GraphQL Playground:    ${GREEN}http://localhost:8083/graphiql${NC}"
    echo -e "  🎨 Next.js UI:            ${GREEN}$UI_URL${NC}"
    
    if [ "$START_IRONBUCKET" = true ]; then
        echo ""
        echo -e "${CYAN}Shared IronBucket Infrastructure:${NC}"
        echo ""
        echo -e "  🚀 API Gateway (Sentinel-Gear):  ${GREEN}http://localhost:8080${NC}"
        echo -e "  🔐 Keycloak Admin:        ${GREEN}$KEYCLOAK_URL${NC}"
        echo -e "     Username: admin / Password: admin"
        echo -e "  💾 MinIO Console:         ${GREEN}http://localhost:9001${NC}"
        echo -e "     Username: minioadmin / Password: minioadmin"
        echo -e "  📡 Eureka Service Registry:     ${GREEN}http://localhost:8083/eureka${NC}"
    fi
    
    echo ""
    echo -e "${CYAN}Useful Commands:${NC}"
    echo ""
    echo -e "  View logs:                ${YELLOW}docker-compose logs -f [service]${NC}"
    echo -e "  Stop all services:        ${YELLOW}docker-compose down${NC}"
    echo -e "  Restart service:          ${YELLOW}docker-compose restart [service]${NC}"
    echo -e "  Run E2E tests:            ${YELLOW}./scripts/test-e2e.sh --in-container${NC}"
    
    if [ "$START_IRONBUCKET" = true ]; then
        echo -e "  Stop IronBucket:          ${YELLOW}cd IronBucket/steel-hammer && docker-compose -f docker-compose-steel-hammer.yml down${NC}"
    fi
    
    echo ""
    
    if [ "$SHOW_LOGS" = true ]; then
        echo -e "${YELLOW}Showing service logs (Ctrl+C to exit)...${NC}"
        echo ""
        docker-compose logs -f
    fi
}

# ============================================================================
# CLEANUP ON EXIT
# ============================================================================

cleanup() {
    echo ""
    print_warning "Interrupt received. Services are still running in background."
    echo ""
    echo "To stop all services, run:"
    echo "  docker-compose down"
    echo ""
    if [ "$START_IRONBUCKET" = true ]; then
        echo "To stop IronBucket services:"
        echo "  cd IronBucket/steel-hammer && docker-compose -f docker-compose-steel-hammer.yml down"
        echo ""
    fi
}

trap cleanup INT TERM

# ============================================================================
# RUN MAIN
# ============================================================================

main 2>&1 | tee "$LOG_FILE"
