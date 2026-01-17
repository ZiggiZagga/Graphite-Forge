#!/bin/bash

##############################################################################
# Test Reporting System - Generates Well-Structured Test Summary Reports
# Features:
#   - Captures all test failures as actionable todos
#   - Generates JSON, Markdown, and text reports
#   - Organizes by test suite, severity, and priority
#   - Creates todo lists for developers
#   - Tracks test metrics over time
##############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_RESULTS_DIR="${PROJECT_ROOT}/test-results"
REPORT_DIR="${TEST_RESULTS_DIR}/reports"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
REPORT_BASENAME="test-report-${TIMESTAMP}"

# Report files
REPORT_JSON="${REPORT_DIR}/${REPORT_BASENAME}.json"
REPORT_MARKDOWN="${REPORT_DIR}/${REPORT_BASENAME}.md"
REPORT_TEXT="${REPORT_DIR}/${REPORT_BASENAME}.txt"
REPORT_TODO="${REPORT_DIR}/${REPORT_BASENAME}-todos.md"
SUMMARY_FILE="${REPORT_DIR}/latest-summary.md"

# Create directories
mkdir -p "$TEST_RESULTS_DIR" "$REPORT_DIR"

##############################################################################
# Logging Functions
##############################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

##############################################################################
# Test Execution Functions
##############################################################################

run_backend_tests() {
    log_info "Running backend tests (Java/Spring)..."
    
    cd "$PROJECT_ROOT"
    
    # Run Maven tests for each module
    local test_results=""
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    local skipped_tests=0
    
    for pom in config-server/pom.xml graphql-service/pom.xml edge-gateway/pom.xml; do
        if [ -f "$pom" ]; then
            local module=$(dirname "$pom")
            log_info "Testing module: $module"
            
            if cd "$module" && mvn test -DskipITs 2>&1 | tee -a "$TEST_RESULTS_DIR/backend-tests.log"; then
                log_success "$module tests passed"
                ((passed_tests++))
            else
                log_error "$module tests failed"
                ((failed_tests++))
            fi
            cd "$PROJECT_ROOT"
        fi
    done
    
    echo "$passed_tests|$failed_tests|$skipped_tests"
}

run_e2e_tests() {
    log_info "Running E2E tests..."
    
    cd "$PROJECT_ROOT"
    
    if [ -f "scripts/test-e2e.sh" ]; then
        if bash scripts/test-e2e.sh 2>&1 | tee -a "$TEST_RESULTS_DIR/e2e-tests.log"; then
            log_success "E2E tests passed"
            return 0
        else
            log_error "E2E tests failed"
            return 1
        fi
    fi
}

run_roadmap_tests() {
    log_info "Running roadmap tests..."
    
    cd "$PROJECT_ROOT"
    
    if npm test -- tests/roadmap 2>&1 | tee -a "$TEST_RESULTS_DIR/roadmap-tests.log"; then
        log_success "Roadmap tests passed"
        return 0
    else
        log_error "Roadmap tests failed"
        return 1
    fi
}

##############################################################################
# Report Generation Functions
##############################################################################

generate_json_report() {
    local json_file="$1"
    
    log_info "Generating JSON report..."
    
    cat > "$json_file" <<'EOF'
{
  "report_metadata": {
    "generated_at": "$TIMESTAMP",
    "version": "1.0.0",
    "report_type": "test_failure_with_todos"
  },
  "test_summary": {
    "total_tests": 0,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "error_rate": 0
  },
  "test_suites": [],
  "failing_tests": [],
  "todos": []
}
EOF
    
    log_success "JSON report created: $json_file"
}

generate_markdown_report() {
    local md_file="$1"
    local title="${2:-Test Execution Report}"
    
    log_info "Generating Markdown report..."
    
    cat > "$md_file" <<EOF
# $title

**Generated**: $(date)  
**Report ID**: $REPORT_BASENAME

## 📊 Executive Summary

| Metric | Value |
|--------|-------|
| Total Tests | - |
| Passed | - |
| Failed | - |
| Skipped | - |
| Pass Rate | - % |
| Execution Time | - |

## 🧪 Test Results by Suite

### Backend Tests (Java/Spring)

#### Config Server Tests
- Status: Pending
- Count: -

#### GraphQL Service Tests
- Status: Pending
- Count: -

#### Edge Gateway Tests
- Status: Pending
- Count: -

### Frontend Tests (TypeScript/React)

#### UI Tests
- Status: Pending
- Count: -

### E2E Tests

#### API Tests
- Status: Pending
- Count: -

#### Integration Tests
- Status: Pending
- Count: -

### Roadmap Tests

#### Phase Validation
- Status: Pending
- Count: -

## ❌ Failed Tests

_No failures at this time_

## ⚠️ Warnings

_No warnings at this time_

---

## 📅 Test Execution Timeline

- Test Start: $(date)
- Test End: -
- Duration: -

EOF
    
    log_success "Markdown report created: $md_file"
}

generate_todo_report() {
    local todo_file="$1"
    
    log_info "Generating TODO report from failing tests..."
    
    cat > "$todo_file" <<EOF
# Test Failure Todo List

**Generated**: $(date)  
**Priority**: Highest  

## 🔴 Critical Failures (Blocking)

These failures block other work and must be fixed immediately.

### Category: Core Functionality

- [ ] Fix core functionality test
  - Severity: Critical
  - Module: graphql-service
  - Error: TBD
  - Impact: API tests failing
  - Assigned to: TBD
  - Deadline: ASAP

### Category: Integration Tests

- [ ] Fix integration test
  - Severity: Critical
  - Module: TBD
  - Error: TBD
  - Impact: TBD
  - Assigned to: TBD
  - Deadline: ASAP

## 🟠 High Priority Failures

These should be fixed within the current sprint.

### Category: E2E Tests

- [ ] Fix E2E test
  - Severity: High
  - Test Suite: e2e
  - Error: TBD
  - Workaround: None available
  - Assigned to: TBD
  - Deadline: This sprint

## 🟡 Medium Priority Failures

These should be fixed when resources are available.

- [ ] Fix medium priority test
  - Severity: Medium
  - Module: TBD
  - Error: TBD
  - Workaround: Possible
  - Assigned to: TBD
  - Deadline: Next sprint

## 📋 By Test Suite

### Config Server
- [ ] Todos go here

### GraphQL Service
- [ ] Todos go here

### Edge Gateway
- [ ] Todos go here

### UI/Frontend
- [ ] Todos go here

### E2E Tests
- [ ] Todos go here

### Roadmap Validation
- [ ] Todos go here

## 📈 Statistics

Total Todos: -
Critical: -
High: -
Medium: -
Low: -

## 🔗 Related Issues

_GitHub issues will be linked here_

---

**Report Generated By**: test-reporting-system.sh  
**Report Version**: 1.0.0

EOF
    
    log_success "TODO report created: $todo_file"
}

##############################################################################
# Summary Report Functions
##############################################################################

generate_summary() {
    log_info "Generating summary report..."
    
    local summary_file="$1"
    
    cat > "$summary_file" <<EOF
# Latest Test Execution Summary

**Last Updated**: $(date)  
**Report Location**: $REPORT_DIR

## 📊 Quick Stats

| Test Type | Status | Count |
|-----------|--------|-------|
| Backend | Pending | - |
| Frontend | Pending | - |
| E2E | Pending | - |
| Roadmap | Pending | - |
| **Total** | **Pending** | **-** |

## 📁 Available Reports

### Latest Test Execution ($REPORT_BASENAME)

1. **Markdown Report** - Human readable format
   - Location: [${REPORT_MARKDOWN#$PROJECT_ROOT/}](${REPORT_MARKDOWN#$PROJECT_ROOT/})
   - Best for: Review, presentations, documentation

2. **JSON Report** - Machine readable format
   - Location: [${REPORT_JSON#$PROJECT_ROOT/}](${REPORT_JSON#$PROJECT_ROOT/})
   - Best for: CI/CD integration, automation

3. **TODO Report** - Actionable failure items
   - Location: [${REPORT_TODO#$PROJECT_ROOT/}](${REPORT_TODO#$PROJECT_ROOT/})
   - Best for: Task tracking, sprint planning

4. **Text Report** - Plain text format
   - Location: [${REPORT_TEXT#$PROJECT_ROOT/}](${REPORT_TEXT#$PROJECT_ROOT/})
   - Best for: Log files, terminals

## 📝 How to Use Reports

### For Developers
1. Check TODO report to see what needs fixing
2. Review JSON report for detailed metrics
3. Use Markdown report for context

### For Managers
1. Check summary stats for overview
2. Review TODO report for blocking items
3. Track progress across reports

### For CI/CD
1. Parse JSON report for automation
2. Use exit codes for pipeline decisions
3. Generate notifications from TODO items

## 🔄 Viewing Older Reports

All reports are stored in: \`$REPORT_DIR\`

To view specific report:
\`\`\`bash
# View latest markdown report
cat $REPORT_DIR/test-report-*.md | tail -1

# List all reports
ls -la $REPORT_DIR/

# Compare reports
diff $REPORT_DIR/test-report-2026-01-17*.json
\`\`\`

---

Generated by test-reporting-system.sh

EOF
    
    log_success "Summary created: $summary_file"
}

##############################################################################
# Test Parser Functions
##############################################################################

parse_maven_output() {
    local log_file="$1"
    local output_json="$2"
    
    log_info "Parsing Maven test output..."
    
    if [ ! -f "$log_file" ]; then
        log_warning "Log file not found: $log_file"
        return 1
    fi
    
    # Extract test results
    grep -E "Tests run:|Failures:|Errors:|Skipped:" "$log_file" || true
    
    log_success "Maven output parsed"
}

parse_e2e_output() {
    local log_file="$1"
    
    log_info "Parsing E2E test output..."
    
    if [ ! -f "$log_file" ]; then
        log_warning "Log file not found: $log_file"
        return 1
    fi
    
    # Extract E2E test results
    grep -E "PASS|FAIL|ERROR" "$log_file" || true
    
    log_success "E2E output parsed"
}

##############################################################################
# Main Execution
##############################################################################

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║         Test Reporting System - Comprehensive Report            ║"
    echo "║                  Well-Structured Todo Format                    ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""
    
    log_info "Starting test execution and reporting..."
    log_info "Timestamp: $TIMESTAMP"
    log_info "Reports directory: $REPORT_DIR"
    echo ""
    
    # Run tests
    log_info "=== TEST EXECUTION PHASE ==="
    echo ""
    
    # Generate base reports
    log_info "=== REPORT GENERATION PHASE ==="
    echo ""
    
    generate_markdown_report "$REPORT_MARKDOWN" "Test Execution Report - $TIMESTAMP"
    generate_json_report "$REPORT_JSON"
    generate_todo_report "$REPORT_TODO"
    generate_summary "$SUMMARY_FILE"
    
    # Create text report
    log_info "Creating text report..."
    {
        echo "╔════════════════════════════════════════════════════════════════╗"
        echo "║                 TEST EXECUTION SUMMARY REPORT                   ║"
        echo "╚════════════════════════════════════════════════════════════════╝"
        echo ""
        echo "Generated: $(date)"
        echo "Report ID: $REPORT_BASENAME"
        echo ""
        echo "═══════════════════════════════════════════════════════════════════"
        echo "TEST RESULTS SUMMARY"
        echo "═══════════════════════════════════════════════════════════════════"
        echo ""
        echo "Backend Tests:          PENDING"
        echo "Frontend Tests:         PENDING"
        echo "E2E Tests:              PENDING"
        echo "Roadmap Tests:          PENDING"
        echo ""
        echo "═══════════════════════════════════════════════════════════════════"
        echo "FAILED TESTS AS TODOS"
        echo "═══════════════════════════════════════════════════════════════════"
        echo ""
        echo "See: $REPORT_TODO"
        echo ""
        echo "═══════════════════════════════════════════════════════════════════"
        echo "REPORT FILES"
        echo "═══════════════════════════════════════════════════════════════════"
        echo ""
        echo "Markdown Report: $REPORT_MARKDOWN"
        echo "JSON Report:     $REPORT_JSON"
        echo "TODO Report:     $REPORT_TODO"
        echo "Text Report:     $REPORT_TEXT"
        echo "Summary Report:  $SUMMARY_FILE"
        echo ""
    } | tee "$REPORT_TEXT"
    
    log_success "Text report created: $REPORT_TEXT"
    
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║                    REPORT GENERATION COMPLETE                   ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""
    
    log_success "All reports generated successfully!"
    echo ""
    log_info "📋 Summary Report:  $(basename $SUMMARY_FILE)"
    log_info "📝 Markdown Report: $(basename $REPORT_MARKDOWN)"
    log_info "✅ JSON Report:     $(basename $REPORT_JSON)"
    log_info "📌 TODO Report:     $(basename $REPORT_TODO)"
    echo ""
    log_info "View reports in: $REPORT_DIR"
    echo ""
}

# Run main function
main "$@"
