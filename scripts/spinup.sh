#!/bin/bash
# Graphite-Forge Spin-Up Script
# Purpose: Start Graphite-Forge microservices (assumes IronBucket is already running)
# Usage: ./spinup.sh [--rebuild] [--logs]
#
# Prerequisites: IronBucket infrastructure must be running separately
#   Start IronBucket: cd IronBucket/steel-hammer && docker-compose -f docker-compose-steel-hammer.yml up -d

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
PROJECT_ROOT="/workspaces/Graphite-Forge"
LOG_FILE="$PROJECT_ROOT/spinup-$(date +%Y%m%d-%H%M%S).log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Service URLs
GRAPHQL_URL="http://localhost:8083"
UI_URL="http://localhost:3000"

# IronBucket expected URLs (used for health checks)
KEYCLOAK_URL="http://localhost:7081"
MINIO_URL="http://localhost:9000"
GATEWAY_URL="http://localhost:8080"

# Flags
REBUILD_SERVICES=false
SHOW_LOGS=false

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --rebuild)
            REBUILD_SERVICES=true
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
            echo "  --rebuild    Rebuild all services before starting (Maven clean install)"
            echo "  --logs       Show service logs after startup"
            echo "  -h, --help   Show this help message"
            echo ""
            echo "How it works:"
            echo "  1. Checks if IronBucket infrastructure is running"
            echo "  2. If not found, offers to start it using IronBucket's own scripts"
            echo "  3. Starts Graphite-Forge services (GraphQL API + Next.js UI)"
            echo "  4. Performs health checks and displays service URLs"
            echo ""
            echo "Service URLs:"
            echo "  GraphQL Service:      $GRAPHQL_URL/graphiql"
            echo "  Next.js UI:           $UI_URL"
            echo "  Keycloak (IronBucket): $KEYCLOAK_URL"
            echo "  MinIO (IronBucket):    $MINIO_URL"
            echo "  Gateway (IronBucket):  $GATEWAY_URL"
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
    print_header "     Graphite-Forge - Spin-Up Script                           "
    echo ""
    echo -e "  ${CYAN}Timestamp: $TIMESTAMP${NC}"
    echo -e "  ${CYAN}Log file: $LOG_FILE${NC}"
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
    
    if [ "$all_checks_pass" = false ]; then
        print_error "Prerequisites check failed"
        exit 1
    fi
    
    echo ""

    # Step 2: Check if IronBucket is running, offer to start if needed
    print_step "2" "Verify IronBucket Infrastructure is Running"
    
    print_info "Checking for required IronBucket services..."
    echo ""
    
    local ironbucket_ok=true
    local keycloak_up=false
    local minio_up=false
    local gateway_up=false
    
    # Check Docker containers (not HTTP - ports aren't exposed to host)
    if docker ps --filter "name=steel-hammer-keycloak" --filter "status=running" --format '{{.Names}}' | grep -q "steel-hammer-keycloak"; then
        print_success "Keycloak (IronBucket) container is running"
        keycloak_up=true
    else
        print_warning "Keycloak container is not running"
        ironbucket_ok=false
    fi
    
    if docker ps --filter "name=steel-hammer-minio" --filter "status=running" --format '{{.Names}}' | grep -q "steel-hammer-minio"; then
        print_success "MinIO (IronBucket) container is running"
        minio_up=true
    else
        print_warning "MinIO container is not running"
        ironbucket_ok=false
    fi
    
    if docker ps --filter "name=steel-hammer-sentinel-gear" --filter "status=running" --format '{{.Names}}' | grep -q "steel-hammer-sentinel-gear"; then
        print_success "Sentinel-Gear (IronBucket) container is running"
        gateway_up=true
    else
        print_warning "Sentinel-Gear container is not running"
        ironbucket_ok=false
    fi
    
    echo ""
    if [ "$ironbucket_ok" = false ]; then
        print_error "IronBucket infrastructure is not fully running"
        print_info "Checking for IronBucket startup script..."
        
        if [ -f "$PROJECT_ROOT/IronBucket/steel-hammer/docker-compose-steel-hammer.yml" ]; then
            print_success "Found IronBucket startup script"
            echo ""
            read -p "Would you like to start IronBucket? (y/n) " -n 1 -r
            echo ""
            
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_info "Starting IronBucket services..."
                cd "$PROJECT_ROOT/IronBucket/steel-hammer"
                export DOCKER_FILES_HOMEDIR="."
                
                if docker-compose -f docker-compose-steel-hammer.yml up -d > /dev/null 2>&1; then
                    print_success "IronBucket services started"
                    
                    # Wait for services to be ready
                    print_info "Waiting for IronBucket services to initialize (120 seconds)..."
                    local waited=0
                    while [ $waited -lt 120 ]; do
                        if [ "$keycloak_up" = false ] && curl -sf "$KEYCLOAK_URL/realms/dev/.well-known/openid-configuration" > /dev/null 2>&1; then
                            print_success "  ✓ Keycloak is ready"
                            keycloak_up=true
                        fi
                        if [ "$minio_up" = false ] && curl -sf "$MINIO_URL/minio/health/live" > /dev/null 2>&1; then
                            print_success "  ✓ MinIO is ready"
                            minio_up=true
                        fi
                        if [ "$gateway_up" = false ] && curl -sf "$GATEWAY_URL/actuator/health-check" > /dev/null 2>&1; then
                            print_success "  ✓ Sentinel-Gear is ready"
                            gateway_up=true
                        fi
                        
                        if [ "$keycloak_up" = true ] && [ "$minio_up" = true ] && [ "$gateway_up" = true ]; then
                            print_success "All IronBucket services are ready!"
                            ironbucket_ok=true
                            break
                        fi
                        
                        sleep 2
                        waited=$((waited + 2))
                        echo -n "."
                    done
                    
                    if [ "$ironbucket_ok" = false ]; then
                        print_warning "Some IronBucket services may still be initializing..."
                    fi
                    echo ""
                else
                    print_error "Failed to start IronBucket services"
                    echo ""
                    print_warning "Showing IronBucket service status and logs..."
                    echo ""
                    docker-compose -f docker-compose-steel-hammer.yml ps 2>&1 | tail -20
                    echo ""
                    print_info "Recent logs from IronBucket services:"
                    docker-compose -f docker-compose-steel-hammer.yml logs --tail=50 2>&1 | tail -100
                    exit 1
                fi
                
                cd "$PROJECT_ROOT"
            else
                print_error "IronBucket infrastructure is required to run Graphite-Forge"
                print_info "Please start IronBucket manually:"
                echo ""
                echo "  cd /workspaces/Graphite-Forge/IronBucket/steel-hammer"
                echo "  docker-compose -f docker-compose-steel-hammer.yml up -d"
                echo ""
                exit 1
            fi
        else
            print_error "IronBucket startup script not found at: $PROJECT_ROOT/IronBucket/steel-hammer/docker-compose-steel-hammer.yml"
            print_info "Please start IronBucket manually or clone it:"
            echo ""
            echo "  cd /workspaces/Graphite-Forge"
            echo "  git submodule update --init --recursive IronBucket"
            echo ""
            exit 1
        fi
    else
        print_success "All IronBucket services verified"
    fi
    
    echo ""

    # Step 3: Clean up old Graphite-Forge containers
    print_step "3" "Clean Up Old Graphite-Forge Containers"
    
    cd "$PROJECT_ROOT"
    
    if docker-compose ps | grep -q "graphql-service\|graphite-forge"; then
        print_info "Stopping old Graphite-Forge containers..."
        docker-compose down 2>/dev/null || true
        print_success "Old containers removed"
    else
        print_success "No old containers found"
    fi
    
    echo ""

    # Step 4: Build services (if requested)
    if [ "$REBUILD_SERVICES" = true ]; then
        print_step "4" "Build Graphite-Forge Services"
        
        print_info "Running Maven build (this may take 2-5 minutes)..."
        mvn clean install -DskipTests 2>&1 | tail -20
        
        print_success "All services built successfully"
        echo ""
    else
        print_step "4" "Skip Maven Build"
        print_info "Using existing builds (use --rebuild to force rebuild)"
        echo ""
    fi

    # Step 5: Start Graphite-Forge services
    print_step "5" "Start Graphite-Forge Services"
    
    cd "$PROJECT_ROOT"
    
    # Validate docker-compose.yml
    if [ ! -f "docker-compose.yml" ]; then
        print_error "docker-compose.yml not found in $PROJECT_ROOT"
        exit 1
    fi
    
    print_info "Validating docker-compose.yml..."
    if ! docker-compose config > /dev/null 2>&1; then
        print_error "docker-compose.yml has configuration errors"
        exit 1
    fi
    print_success "docker-compose.yml is valid"
    
    # Remove any old override file
    rm -f "$PROJECT_ROOT/docker-compose.override.yml"
    
    print_info "Starting Graphite-Forge services via docker-compose..."
    if docker-compose up -d 2>&1 | tee /tmp/docker-compose-up.log | grep -E 'Starting|Created|Done'; then
        print_success "Graphite-Forge services started"
    else
        print_error "Failed to start Graphite-Forge services"
        echo ""
        print_warning "Docker Compose output:"
        cat /tmp/docker-compose-up.log
        echo ""
        print_warning "Showing container status:"
        docker-compose ps 2>&1
        echo ""
        print_info "Recent logs:"
        docker-compose logs --tail=50 2>&1 | tail -100
        exit 1
    fi
    
    echo ""

    # Step 6: Wait for Graphite-Forge services
    print_step "6" "Wait for Graphite-Forge Services to Initialize"
    
    if ! wait_for_service "$GRAPHQL_URL/actuator/health" "GraphQL Service" 120; then
        print_error "GraphQL Service failed to start or become healthy"
        echo ""
        print_warning "Container status:"
        docker-compose ps graphql-service 2>&1
        echo ""
        print_info "Recent logs from GraphQL Service:"
        docker-compose logs --tail=100 graphql-service 2>&1
        echo ""
        print_info "To continue debugging:"
        echo "  docker-compose logs -f graphql-service"
        echo "  docker-compose exec graphql-service bash"
        exit 1
    fi
    
    echo ""

    # Step 7: Start Next.js UI
    print_step "7" "Start Next.js UI Development Server"
    
    cd "$PROJECT_ROOT/ui"
    
    if [ ! -d "node_modules" ]; then
        print_info "Installing Node.js dependencies..."
        npm install > /dev/null 2>&1
        print_success "Dependencies installed"
    else
        print_success "Dependencies already installed"
    fi
    
    # Kill any existing Next.js process
    lsof -ti:3000 | xargs kill -9 2>/dev/null || true
    
    print_info "Starting Next.js development server..."
    npm run dev > "$PROJECT_ROOT/ui-$(date +%Y%m%d-%H%M%S).log" 2>&1 &
    UI_PID=$!
    
    echo $UI_PID > "$PROJECT_ROOT/.ui.pid"
    print_success "UI started (PID: $UI_PID)"
    
    wait_for_service "$UI_URL" "Next.js UI" 60
    
    echo ""

    # Step 8: Service health checks
    print_step "8" "Service Health Checks"
    
    echo "Checking Graphite-Forge services..."
    echo ""
    
    if curl -sf "$GRAPHQL_URL/actuator/health" > /dev/null 2>&1; then
        print_success "GraphQL Service: UP ($GRAPHQL_URL)"
    else
        print_warning "GraphQL Service: DOWN"
    fi
    
    if curl -sf "$UI_URL" > /dev/null 2>&1; then
        print_success "Next.js UI: UP ($UI_URL)"
    else
        print_warning "Next.js UI: DOWN"
    fi
    
    echo ""
    echo "Checking IronBucket shared infrastructure..."
    echo ""
    
    if curl -sf "$KEYCLOAK_URL/realms/dev/.well-known/openid-configuration" > /dev/null 2>&1; then
        print_success "Keycloak (Identity): UP ($KEYCLOAK_URL)"
    else
        print_warning "Keycloak: DOWN"
    fi
    
    if curl -sf "$GATEWAY_URL/actuator/health-check" > /dev/null 2>&1; then
        print_success "Sentinel-Gear (API Gateway): UP ($GATEWAY_URL)"
    else
        print_warning "Sentinel-Gear: DOWN"
    fi
    
    if curl -sf "$MINIO_URL/minio/health/live" > /dev/null 2>&1; then
        print_success "MinIO (S3 Storage): UP ($MINIO_URL)"
    else
        print_warning "MinIO: DOWN"
    fi
    
    echo ""

    # Step 9: Summary
    print_section "🎉 Graphite-Forge is Ready!"
    
    echo -e "${CYAN}Graphite-Forge Services:${NC}"
    echo ""
    echo -e "  📊 GraphQL Playground:    ${GREEN}$GRAPHQL_URL/graphiql${NC}"
    echo -e "  🎨 Next.js UI:            ${GREEN}$UI_URL${NC}"
    
    echo ""
    echo -e "${CYAN}Shared IronBucket Infrastructure:${NC}"
    echo ""
    echo -e "  🚀 API Gateway (Sentinel-Gear):  ${GREEN}$GATEWAY_URL${NC}"
    echo -e "  🔐 Keycloak (Identity Provider): ${GREEN}$KEYCLOAK_URL${NC}"
    echo -e "  💾 MinIO (S3 Storage):           ${GREEN}$MINIO_URL${NC}"
    echo -e "     Console: ${GREEN}http://localhost:9001${NC}"
    echo -e "     Credentials: minioadmin / minioadmin"
    
    echo ""
    echo -e "${CYAN}Useful Commands:${NC}"
    echo ""
    echo -e "  View Graphite-Forge logs:       ${YELLOW}docker-compose logs -f [service]${NC}"
    echo -e "  Stop Graphite-Forge services:   ${YELLOW}docker-compose down${NC}"
    echo -e "  Restart a service:              ${YELLOW}docker-compose restart [service]${NC}"
    echo -e "  Run E2E tests:                  ${YELLOW}./scripts/test-e2e.sh --in-container${NC}"
    echo ""
    echo -e "${CYAN}Setup Keycloak Users (if needed):${NC}"
    echo ""
    echo -e "  ${YELLOW}bash ./scripts/setup-keycloak-dev-users.sh${NC}"
    echo ""
    
    if [ "$SHOW_LOGS" = true ]; then
        echo -e "${YELLOW}Showing Graphite-Forge service logs (Ctrl+C to exit)...${NC}"
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
    echo "To stop Graphite-Forge:"
    echo "  docker-compose down"
    echo ""
    echo "To stop IronBucket (if needed):"
    echo "  cd IronBucket/steel-hammer"
    echo "  docker-compose -f docker-compose-steel-hammer.yml down"
    echo ""
}

trap cleanup INT TERM

# ============================================================================
# RUN MAIN
# ============================================================================

main 2>&1 | tee "$LOG_FILE"
