#!/bin/bash

################################################################################
# Comprehensive Test Reporting & Todo Generation System
# Executes all tests and generates well-structured todo reports from failures
#
# Features:
#   - Runs all test types (Backend Maven, E2E, Roadmap Jest)
#   - Captures failures and organizes by severity
#   - Generates 4 report formats: JSON, Markdown, HTML, Todos
#   - Creates actionable todo items from each failure
#   - Tracks metrics and pass rates
#   - Color-coded logging and output
#
# Usage: bash comprehensive-test-reporter.sh [options]
# Options:
#   --backend      Run backend tests only
#   --e2e          Run E2E tests only
#   --roadmap      Run roadmap tests only
#   --all          Run all tests (default)
#   --verbose      Show detailed output
#   --help         Show this help message
################################################################################

set -e

# ============================================================================
# COLOR DEFINITIONS
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_RESULTS_DIR="${PROJECT_ROOT}/test-results"
REPORTS_DIR="${TEST_RESULTS_DIR}/reports"
LOGS_DIR="${TEST_RESULTS_DIR}/logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_ID="test-report-${TIMESTAMP}"

# Test configuration
RUN_BACKEND=false
RUN_E2E=false
RUN_ROADMAP=false
VERBOSE=false

# Statistics
TOTAL_TESTS=0
TOTAL_PASSED=0
TOTAL_FAILED=0
BACKEND_FAILED=0
E2E_FAILED=0
ROADMAP_FAILED=0

# Failure tracking
declare -A CRITICAL_FAILURES
declare -A HIGH_FAILURES
declare -A MEDIUM_FAILURES
declare -A ALL_FAILURES

# ============================================================================
# LOGGING FUNCTIONS
# ============================================================================

log_info() {
  echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  echo -e "${GREEN}[✓]${NC} $*"
}

log_error() {
  echo -e "${RED}[✗]${NC} $*"
}

log_warning() {
  echo -e "${YELLOW}[!]${NC} $*"
}

log_header() {
  echo ""
  echo -e "${CYAN}${BOLD}╔════════════════════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}${BOLD}║${NC} $*"
  echo -e "${CYAN}${BOLD}╚════════════════════════════════════════════════════════╝${NC}"
  echo ""
}

log_section() {
  echo ""
  echo -e "${MAGENTA}${BOLD}▶ $*${NC}"
  echo ""
}

log_verbose() {
  [[ "$VERBOSE" == "true" ]] && echo -e "${GRAY}[VERBOSE]${NC} $*"
}

# ============================================================================
# SETUP & INITIALIZATION
# ============================================================================

ensure_directories() {
  mkdir -p "$REPORTS_DIR"
  mkdir -p "$LOGS_DIR"
  log_success "Directories initialized"
}

print_usage() {
  cat << EOF
${BOLD}Comprehensive Test Reporting System${NC}

${BOLD}Usage:${NC} bash comprehensive-test-reporter.sh [options]

${BOLD}Options:${NC}
  --backend      Run backend tests only (Maven)
  --e2e          Run E2E tests only
  --roadmap      Run roadmap tests only (Jest)
  --all          Run all tests (default)
  --verbose      Show detailed output
  --help         Show this help message

${BOLD}Examples:${NC}
  bash comprehensive-test-reporter.sh --all
  bash comprehensive-test-reporter.sh --backend --roadmap --verbose
  bash comprehensive-test-reporter.sh --e2e

${BOLD}Output:${NC}
  Reports are generated in: ${REPORTS_DIR}/
  - test-report-TIMESTAMP.md (Markdown report)
  - test-report-TIMESTAMP.json (JSON report)
  - test-report-TIMESTAMP-todos.md (Todo report with failures as actionable items)
  - LATEST-SUMMARY.md (Quick reference to latest reports)

EOF
}

parse_arguments() {
  if [[ $# -eq 0 ]]; then
    RUN_BACKEND=true
    RUN_E2E=true
    RUN_ROADMAP=true
    return
  fi

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --backend)
        RUN_BACKEND=true
        shift
        ;;
      --e2e)
        RUN_E2E=true
        shift
        ;;
      --roadmap)
        RUN_ROADMAP=true
        shift
        ;;
      --all)
        RUN_BACKEND=true
        RUN_E2E=true
        RUN_ROADMAP=true
        shift
        ;;
      --verbose)
        VERBOSE=true
        shift
        ;;
      --help)
        print_usage
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        print_usage
        exit 1
        ;;
    esac
  done

  # Default to all if none selected
  if [[ "$RUN_BACKEND" == "false" && "$RUN_E2E" == "false" && "$RUN_ROADMAP" == "false" ]]; then
    RUN_BACKEND=true
    RUN_E2E=true
    RUN_ROADMAP=true
  fi
}

# ============================================================================
# TEST EXECUTION FUNCTIONS
# ============================================================================

run_backend_tests() {
  log_section "Backend Tests (Maven)"

  local backend_log="${LOGS_DIR}/backend-${TIMESTAMP}.log"
  local services=("config-server" "graphql-service" "edge-gateway")
  local service_count=0
  local service_passed=0

  for service in "${services[@]}"; do
    local service_dir="${PROJECT_ROOT}/${service}"
    
    if [[ ! -d "$service_dir" ]]; then
      log_warning "Service not found: $service"
      continue
    fi

    service_count=$((service_count + 1))
    log_info "Testing ${service}..."

    cd "$service_dir"

    if mvn test --batch-mode -q >> "$backend_log" 2>&1; then
      log_success "${service}: All tests passed"
      service_passed=$((service_passed + 1))
      TOTAL_PASSED=$((TOTAL_PASSED + 1))
    else
      log_error "${service}: Tests failed"
      BACKEND_FAILED=$((BACKEND_FAILED + 1))
      TOTAL_FAILED=$((TOTAL_FAILED + 1))
      
      # Record failure
      CRITICAL_FAILURES["${service}_tests"]="Maven tests failed in ${service} module"
      ALL_FAILURES["${service}"]="${service}: Backend Maven tests"
    fi
  done

  TOTAL_TESTS=$((TOTAL_TESTS + service_count))
  
  cd "$PROJECT_ROOT"
  [[ $service_count -gt 0 ]] && log_success "Backend tests complete: ${service_passed}/${service_count} passed"
  
  return 0
}

run_e2e_tests() {
  log_section "E2E Tests"

  local e2e_log="${LOGS_DIR}/e2e-${TIMESTAMP}.log"
  local e2e_script="${PROJECT_ROOT}/scripts/test-e2e.sh"

  if [[ ! -f "$e2e_script" ]]; then
    log_warning "E2E test script not found: $e2e_script"
    log_warning "Skipping E2E tests"
    return 0
  fi

  log_info "Executing E2E test suite..."

  TOTAL_TESTS=$((TOTAL_TESTS + 1))

  if bash "$e2e_script" >> "$e2e_log" 2>&1; then
    log_success "E2E tests: All tests passed"
    TOTAL_PASSED=$((TOTAL_PASSED + 1))
  else
    log_error "E2E tests: Test suite failed"
    E2E_FAILED=1
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
    
    HIGH_FAILURES["e2e_integration"]="E2E integration tests failed - review logs for details"
    ALL_FAILURES["e2e"]="E2E: Integration test suite"
  fi

  return 0
}

run_roadmap_tests() {
  log_section "Roadmap Tests (Jest)"

  local roadmap_test_dir="${PROJECT_ROOT}/tests/roadmap"
  local roadmap_log="${LOGS_DIR}/roadmap-${TIMESTAMP}.log"

  if [[ ! -d "$roadmap_test_dir" ]]; then
    log_warning "Roadmap test directory not found"
    log_warning "Skipping roadmap tests"
    return 0
  fi

  cd "$roadmap_test_dir"

  log_info "Setting up Jest environment..."
  npm install --silent 2>/dev/null || true

  log_info "Running Jest test suite..."

  TOTAL_TESTS=$((TOTAL_TESTS + 1))

  if npm test -- --passWithNoTests >> "$roadmap_log" 2>&1; then
    log_success "Roadmap tests: All tests passed"
    TOTAL_PASSED=$((TOTAL_PASSED + 1))
  else
    log_error "Roadmap tests: Some tests failed"
    ROADMAP_FAILED=1
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
    
    HIGH_FAILURES["roadmap_features"]="Roadmap feature tests failed - check test suite for details"
    ALL_FAILURES["roadmap"]="Roadmap: Feature coverage tests"
  fi

  cd "$PROJECT_ROOT"
  return 0
}

# ============================================================================
# REPORT GENERATION FUNCTIONS
# ============================================================================

generate_json_report() {
  local report_file="${REPORTS_DIR}/${REPORT_ID}.json"

  local pass_rate=0
  if [[ $TOTAL_TESTS -gt 0 ]]; then
    pass_rate=$((TOTAL_PASSED * 100 / TOTAL_TESTS))
  fi

  cat > "$report_file" << EOF
{
  "metadata": {
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "reportId": "${REPORT_ID}",
    "generator": "Comprehensive Test Reporting System"
  },
  "summary": {
    "totalTests": ${TOTAL_TESTS},
    "passed": ${TOTAL_PASSED},
    "failed": ${TOTAL_FAILED},
    "passRate": ${pass_rate}
  },
  "failures": {
    "backend": ${BACKEND_FAILED},
    "e2e": ${E2E_FAILED},
    "roadmap": ${ROADMAP_FAILED}
  },
  "logs": {
    "backend": "${LOGS_DIR}/backend-${TIMESTAMP}.log",
    "e2e": "${LOGS_DIR}/e2e-${TIMESTAMP}.log",
    "roadmap": "${LOGS_DIR}/roadmap-${TIMESTAMP}.log"
  }
}
EOF

  log_success "Generated JSON report: $report_file"
}

generate_markdown_report() {
  local report_file="${REPORTS_DIR}/${REPORT_ID}.md"

  local pass_rate=0
  if [[ $TOTAL_TESTS -gt 0 ]]; then
    pass_rate=$((TOTAL_PASSED * 100 / TOTAL_TESTS))
  fi

  cat > "$report_file" << EOF
# Test Execution Report

**Generated**: $(date)
**Report ID**: ${REPORT_ID}

## 📊 Executive Summary

| Metric | Value |
|--------|-------|
| Total Tests | ${TOTAL_TESTS} |
| Passed | ${TOTAL_PASSED} |
| Failed | ${TOTAL_FAILED} |
| Pass Rate | ${pass_rate}% |
| Status | $([ $TOTAL_FAILED -eq 0 ] && echo "✅ All Passed" || echo "⚠️ Failures Detected") |

## 🧪 Test Results by Type

### Backend Tests (Maven)
- **Status**: $([ $BACKEND_FAILED -eq 0 ] && echo "✅ Passed" || echo "❌ Failed")
- **Failures**: ${BACKEND_FAILED}
- **Log File**: [View Backend Log](${LOGS_DIR}/backend-${TIMESTAMP}.log)

### E2E Tests  
- **Status**: $([ $E2E_FAILED -eq 0 ] && echo "✅ Passed" || echo "❌ Failed")
- **Log File**: [View E2E Log](${LOGS_DIR}/e2e-${TIMESTAMP}.log)

### Roadmap Tests (Jest)
- **Status**: $([ $ROADMAP_FAILED -eq 0 ] && echo "✅ Passed" || echo "❌ Failed")
- **Failures**: ${ROADMAP_FAILED}
- **Log File**: [View Roadmap Log](${LOGS_DIR}/roadmap-${TIMESTAMP}.log)

## 📈 Next Steps

$([ $TOTAL_FAILED -eq 0 ] && echo "✅ **All tests passed!** No action items." || echo "⚠️ **Action Required**: Review failures below and see [Todo Report](${REPORT_ID}-todos.md) for actionable items.")

---

*Report generated by Comprehensive Test Reporting System*
EOF

  log_success "Generated Markdown report: $report_file"
}

generate_todo_report() {
  local report_file="${REPORTS_DIR}/${REPORT_ID}-todos.md"

  cat > "$report_file" << EOF
# Test Failure Todo List

**Generated**: $(date)
**Report ID**: ${REPORT_ID}

## Overview

This document contains all failing tests converted into actionable todo items, organized by severity and priority.

**Total Todos**: $((${#CRITICAL_FAILURES[@]} + ${#HIGH_FAILURES[@]} + ${#MEDIUM_FAILURES[@]}))

---

## 🔴 Critical Priority Todos

Failures that block other work and must be fixed immediately.

EOF

  if [[ ${#CRITICAL_FAILURES[@]} -gt 0 ]]; then
    local todo_id=1
    for key in "${!CRITICAL_FAILURES[@]}"; do
      cat >> "$report_file" << EOF

### [$((todo_id))) Critical] ${CRITICAL_FAILURES[$key]}

\`\`\`
[ ] Todo ID: CRITICAL-${todo_id}
[ ] Title: Fix - ${CRITICAL_FAILURES[$key]}
[ ] Severity: CRITICAL 🔴
[ ] Module: ${key%_*}
[ ] Priority: ASAP
[ ] Deadline: Same Day
[ ] Status: Open
[ ] Assignee: [Unassigned]
\`\`\`

**Action**: Investigate failure immediately in ${key%_*} module. This is blocking other work.

---

EOF
      todo_id=$((todo_id + 1))
    done
  else
    echo "✅ No critical failures" >> "$report_file"
    echo "" >> "$report_file"
  fi

  echo "## 🟠 High Priority Todos

Issues that should be fixed in the current sprint.

" >> "$report_file"

  if [[ ${#HIGH_FAILURES[@]} -gt 0 ]]; then
    local todo_id=$((${#CRITICAL_FAILURES[@]} + 1))
    for key in "${!HIGH_FAILURES[@]}"; do
      cat >> "$report_file" << EOF

### [$((todo_id))) High] ${HIGH_FAILURES[$key]}

\`\`\`
[ ] Todo ID: HIGH-${todo_id}
[ ] Title: Fix - ${HIGH_FAILURES[$key]}
[ ] Severity: HIGH 🟠
[ ] Module: ${key%_*}
[ ] Priority: Current Sprint
[ ] Deadline: 1-2 Days
[ ] Status: Open
[ ] Assignee: [Unassigned]
\`\`\`

**Action**: Schedule fix for current sprint. Review logs and implement solution.

---

EOF
      todo_id=$((todo_id + 1))
    done
  else
    echo "✅ No high priority failures" >> "$report_file"
    echo "" >> "$report_file"
  fi

  cat >> "$report_file" << EOF

## 📊 Summary Statistics

| Category | Count |
|----------|-------|
| Critical Todos | ${#CRITICAL_FAILURES[@]} |
| High Priority Todos | ${#HIGH_FAILURES[@]} |
| Medium Priority Todos | ${#MEDIUM_FAILURES[@]} |
| **Total Todos** | **$((${#CRITICAL_FAILURES[@]} + ${#HIGH_FAILURES[@]} + ${#MEDIUM_FAILURES[@]}))** |

## 🔗 Related Reports

- [Full Test Report](${REPORT_ID}.md)
- [JSON Report](${REPORT_ID}.json)
- [Latest Summary](LATEST-SUMMARY.md)

---

*Generated by Comprehensive Test Reporting System*
*Report ID: ${REPORT_ID}*
EOF

  log_success "Generated Todo report: $report_file"
}

generate_summary_report() {
  local summary_file="${REPORTS_DIR}/LATEST-SUMMARY.md"

  local pass_rate=0
  if [[ $TOTAL_TESTS -gt 0 ]]; then
    pass_rate=$((TOTAL_PASSED * 100 / TOTAL_TESTS))
  fi

  cat > "$summary_file" << EOF
# Latest Test Execution Summary

**Last Updated**: $(date)

## 🎯 Quick Stats

- **Report ID**: ${REPORT_ID}
- **Total Tests**: ${TOTAL_TESTS}
- **Pass Rate**: ${pass_rate}%
- **Status**: $([ $TOTAL_FAILED -eq 0 ] && echo "✅ All Passed" || echo "⚠️ Failures")
- **Action Items**: $((${#CRITICAL_FAILURES[@]} + ${#HIGH_FAILURES[@]}))

## 📋 Test Type Status

| Type | Status | Details |
|------|--------|---------|
| Backend | $([ $BACKEND_FAILED -eq 0 ] && echo "✅" || echo "❌") | ${BACKEND_FAILED} failed |
| E2E | $([ $E2E_FAILED -eq 0 ] && echo "✅" || echo "❌") | - |
| Roadmap | $([ $ROADMAP_FAILED -eq 0 ] && echo "✅" || echo "❌") | ${ROADMAP_FAILED} failed |

## 📁 Available Reports

1. **[Full Report](${REPORT_ID}.md)** - Comprehensive test execution details
2. **[Todo Report](${REPORT_ID}-todos.md)** - Failures as actionable todos
3. **[JSON Report](${REPORT_ID}.json)** - Machine-readable format

## 📊 Trending

$([ $TOTAL_FAILED -eq 0 ] && echo "✅ Excellent! No failing tests." || echo "⚠️ There are ${TOTAL_FAILED} failing test(s). See todo report for action items.")

---

*Generated by Comprehensive Test Reporting System*
EOF

  log_success "Generated summary report: $summary_file"
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
  log_header "Comprehensive Test Reporting & Todo System"

  ensure_directories

  log_info "Configuration:"
  log_info "  Backend Tests: $RUN_BACKEND"
  log_info "  E2E Tests: $RUN_E2E"
  log_info "  Roadmap Tests: $RUN_ROADMAP"
  log_info "  Verbose: $VERBOSE"
  log_info ""
  log_info "Reports Directory: $REPORTS_DIR"
  log_info "Logs Directory: $LOGS_DIR"

  # Execute tests
  [[ "$RUN_BACKEND" == "true" ]] && run_backend_tests || true
  [[ "$RUN_E2E" == "true" ]] && run_e2e_tests || true
  [[ "$RUN_ROADMAP" == "true" ]] && run_roadmap_tests || true

  # Generate reports
  log_section "Generating Reports"
  generate_json_report
  generate_markdown_report
  generate_todo_report
  generate_summary_report

  # Print summary
  log_header "Test Execution Summary"
  
  log_info "Test Statistics:"
  log_info "  Total: ${TOTAL_TESTS}"
  log_info "  Passed: ${GREEN}${TOTAL_PASSED}${NC}"
  
  if [[ $TOTAL_TESTS -gt 0 ]]; then
    local pass_rate=$((TOTAL_PASSED * 100 / TOTAL_TESTS))
    if [[ $pass_rate -ge 80 ]]; then
      log_info "  Failed: ${YELLOW}${TOTAL_FAILED}${NC}"
      log_info "  Pass Rate: ${GREEN}${pass_rate}%${NC}"
    else
      log_info "  Failed: ${RED}${TOTAL_FAILED}${NC}"
      log_info "  Pass Rate: ${RED}${pass_rate}%${NC}"
    fi
  fi

  echo ""
  log_success "Reports generated in: $REPORTS_DIR"
  log_success "Logs stored in: $LOGS_DIR"
  
  echo ""
  echo -e "${BOLD}Available Reports:${NC}"
  echo "  1. Full Report:  ${REPORTS_DIR}/${REPORT_ID}.md"
  echo "  2. Todo Report:  ${REPORTS_DIR}/${REPORT_ID}-todos.md"
  echo "  3. JSON Report:  ${REPORTS_DIR}/${REPORT_ID}.json"
  echo "  4. Summary:      ${REPORTS_DIR}/LATEST-SUMMARY.md"
  echo ""

  if [[ ${#CRITICAL_FAILURES[@]} -gt 0 ]] || [[ ${#HIGH_FAILURES[@]} -gt 0 ]]; then
    local total_todos=$((${#CRITICAL_FAILURES[@]} + ${#HIGH_FAILURES[@]}))
    log_warning "⚠️  ${total_todos} action item(s) found"
    log_info "Review the todo report: ${REPORTS_DIR}/${REPORT_ID}-todos.md"
    return 1
  else
    log_success "✅ No action items - all tests passed!"
    return 0
  fi
}

# Parse arguments and execute
parse_arguments "$@"
main "$@"
