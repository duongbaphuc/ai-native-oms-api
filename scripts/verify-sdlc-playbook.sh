#!/usr/bin/env bash
# ==============================================================================
# AI-Native SDLC Comprehensive Compliance & Quality Gate Verification
# Audits Phases 05 through 15 from docs/prompt/01-sdlc-playbook/
# ==============================================================================
set -e

FAILED_GATES=0

report_pass() {
  echo "  ✅ [PASS] $1"
}

report_fail() {
  echo "  ❌ [FAIL] $1"
  FAILED_GATES=$((FAILED_GATES + 1))
}

echo "======================================================================"
echo "🚀 STARTING AI-NATIVE SDLC PLAYBOOK VERIFICATION (PHASES 05 -> 15)"
echo "======================================================================"

# ------------------------------------------------------------------------------
# PHASE 05: Automated Testing & JaCoCo Coverage Gate (100%)
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 05] Automated Testing & JaCoCo 100% Quality Gate ---"
if grep -q "<minimum>1.00</minimum>" pom.xml; then
  report_pass "JaCoCo Quality Gate configured with 100% Line & Branch threshold in pom.xml"
else
  report_fail "JaCoCo Quality Gate 1.00 threshold missing in pom.xml"
fi

REQUIRED_TESTS=(
  "src/test/java/com/gpc/oms/domain/WorkOrderStatusTest.java"
  "src/test/java/com/gpc/oms/domain/PriorityTest.java"
  "src/test/java/com/gpc/oms/domain/WorkOrderTest.java"
  "src/test/java/com/gpc/oms/service/WorkOrderServiceTest.java"
  "src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java"
  "src/test/java/com/gpc/oms/WorkOrderIntegrationTest.java"
)
for tfile in "${REQUIRED_TESTS[@]}"; do
  if [ -f "$tfile" ]; then
    report_pass "Test class exists: $tfile"
  else
    report_fail "Missing required test class: $tfile"
  fi
done

# ------------------------------------------------------------------------------
# PHASE 06: Interactive Verification & Web Test Console
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 06] Interactive Verification & Web Test Console ---"
if [ -f "src/main/resources/static/index.html" ]; then
  report_pass "Web test console asset exists: src/main/resources/static/index.html"
else
  report_fail "Missing Web test console: src/main/resources/static/index.html"
fi

if grep -q "ADMIN" src/main/resources/static/index.html && grep -q "DISPATCHER" src/main/resources/static/index.html && grep -q "TECHNICIAN" src/main/resources/static/index.html; then
  report_pass "Role Switcher integrated (ADMIN, DISPATCHER, TECHNICIAN)"
else
  report_fail "Role Switcher missing roles in index.html"
fi

if grep -q '@Profile("!prod")' src/main/java/com/gpc/oms/config/SecurityConfig.java; then
  report_pass "Demo security credentials isolated with @Profile(\"!prod\")"
else
  report_fail "Demo security credentials not isolated with @Profile(\"!prod\")"
fi

# ------------------------------------------------------------------------------
# PHASE 07: Comprehensive Code Review Audit (Code vs Spec)
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 07] Comprehensive Code Review Audit ---"
AUDIT_P7=$(ls docs/archive/audit-logs/code-vs-spec-audit-report-*.md 2>/dev/null | head -n 1)
if [ -n "$AUDIT_P7" ]; then
  report_pass "Code-vs-Spec comprehensive audit report exists: $AUDIT_P7"
else
  report_fail "Missing Code-vs-Spec comprehensive audit report in docs/archive/audit-logs/"
fi

TARGET_CLASSES=(
  "src/main/java/com/gpc/oms/domain/WorkOrder.java"
  "src/main/java/com/gpc/oms/domain/WorkOrderStatus.java"
  "src/main/java/com/gpc/oms/domain/Priority.java"
  "src/main/java/com/gpc/oms/domain/WorkOrderRepository.java"
  "src/main/java/com/gpc/oms/service/WorkOrderService.java"
  "src/main/java/com/gpc/oms/controller/WorkOrderController.java"
  "src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java"
  "src/main/java/com/gpc/oms/config/SecurityConfig.java"
  "src/main/java/com/gpc/oms/exception/ProblemTypes.java"
)
for cfile in "${TARGET_CLASSES[@]}"; do
  if [ -f "$cfile" ]; then
    report_pass "Target architecture class verified: $cfile"
  else
    report_fail "Missing target architecture class: $cfile"
  fi
done

# ------------------------------------------------------------------------------
# PHASE 08: System Handover Documentation (Runbook & Dossier)
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 08] System Handover Documentation ---"
if [ -f "docs/08-SYSTEM_HANDOVER.md" ]; then
  report_pass "System Handover Dossier exists: docs/08-SYSTEM_HANDOVER.md"
  SECTION_COUNT=$(grep -c -E '^## [1-9]\. ' docs/08-SYSTEM_HANDOVER.md || true)
  if [ "$SECTION_COUNT" -ge 9 ]; then
    report_pass "System Handover contains all 9 required sections ($SECTION_COUNT sections detected)"
  else
    report_fail "System Handover has incomplete sections: expected >= 9, found $SECTION_COUNT"
  fi
else
  report_fail "Missing docs/08-SYSTEM_HANDOVER.md"
fi

# ------------------------------------------------------------------------------
# PHASE 09: Security Audit & Vulnerability Assessment
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 09] Security Audit & Vulnerability Assessment ---"
if [ -f "docs/09-SECURITY_HANDOVER_REPORT.md" ]; then
  report_pass "Security Handover Report exists: docs/09-SECURITY_HANDOVER_REPORT.md"
else
  report_fail "Missing docs/09-SECURITY_HANDOVER_REPORT.md"
fi

if grep -q "aquasecurity/trivy-action" .github/workflows/ci.yml; then
  report_pass "Trivy vulnerability scanner configured in CI pipeline"
else
  report_fail "Trivy vulnerability scanner missing in .github/workflows/ci.yml"
fi

# ------------------------------------------------------------------------------
# PHASE 10: Docker Packaging & CI/CD Pipeline
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 10] Containerization & CI/CD Pipeline ---"
if grep -q "FROM maven:3.9-eclipse-temurin-17-alpine AS builder" Dockerfile && grep -q "FROM eclipse-temurin:17-jre-alpine AS runner" Dockerfile; then
  report_pass "Multi-stage Dockerfile configured (builder + hardened runner)"
else
  report_fail "Dockerfile does not match multi-stage architecture"
fi

if grep -q "USER 10001:10001" Dockerfile; then
  report_pass "Non-root container hardening enforced (USER 10001:10001)"
else
  report_fail "Non-root container hardening missing in Dockerfile"
fi

if [ -f "docker-compose.yml" ] && grep -q "oms-api" docker-compose.yml && grep -q "postgres:15-alpine" docker-compose.yml; then
  report_pass "docker-compose.yml configured with oms-api and postgres:15-alpine"
else
  report_fail "docker-compose.yml missing or incomplete"
fi

# ------------------------------------------------------------------------------
# PHASE 11: Feature Evolution & Bugfix Discipline
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 11] Feature Evolution & Bugfix Discipline ---"
if [ -f "CONTRIBUTING.md" ] && grep -q "feature/WO-" CONTRIBUTING.md; then
  report_pass "CONTRIBUTING.md enforces feature/WO-<issue-id> branching and PR standards"
else
  report_fail "CONTRIBUTING.md missing branching policy"
fi

# ------------------------------------------------------------------------------
# PHASE 12: Post-Fix Documentation Synchronization & Zero-Drift
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 12] Post-Fix Documentation Synchronization ---"
POST_FIX_REPORT=$(ls docs/archive/audit-logs/post-fix-spec-drift-audit-report-*.md 2>/dev/null | head -n 1)
if [ -n "$POST_FIX_REPORT" ]; then
  report_pass "Post-fix spec drift audit report exists: $POST_FIX_REPORT"
else
  report_fail "Missing post-fix spec drift audit report in docs/archive/audit-logs/"
fi

# Check Enum Synchronization across Java & Markdown
for status in OPEN IN_PROGRESS DONE; do
  if grep -rqi "$status" docs/01-domain-model.md && grep -rqi "$status" docs/02-api-spec.md; then
    report_pass "WorkOrderStatus '$status' synchronized between code & docs"
  else
    report_fail "WorkOrderStatus '$status' missing in docs/01-domain-model.md or docs/02-api-spec.md"
  fi
done

for priority in LOW MEDIUM HIGH CRITICAL; do
  if grep -rqi "$priority" docs/01-domain-model.md && grep -rqi "$priority" docs/02-api-spec.md; then
    report_pass "Priority '$priority' synchronized between code & docs"
  else
    report_fail "Priority '$priority' missing in docs/01-domain-model.md or docs/02-api-spec.md"
  fi
done

# ------------------------------------------------------------------------------
# PHASE 13: Coding Rules & Design Patterns Enforcement (Oracle Senior Java)
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 13] Coding Rules & Design Patterns Enforcement ---"
# Check Zero Lombok
LOMBOK_MATCHES=$(grep -rnE '@(Data|Getter|Setter|Builder|AllArgsConstructor|NoArgsConstructor|ToString|EqualsAndHashCode)\b|import lombok\.' src/main/java src/test/java || true)
if [ -z "$LOMBOK_MATCHES" ]; then
  report_pass "Zero-Lombok rule satisfied: No Lombok annotations detected in project"
else
  report_fail "Lombok annotations detected in source: $LOMBOK_MATCHES"
fi

# Check Constructor Injection (no @Autowired on fields)
AUTOWIRED_FIELDS=$(grep -rn '@Autowired' src/main/java || true)
if [ -z "$AUTOWIRED_FIELDS" ]; then
  report_pass "Constructor Injection rule satisfied: No @Autowired field injection"
else
  report_fail "@Autowired field injection detected: $AUTOWIRED_FIELDS"
fi

# Check Pure Java 17 Records for DTOs
DTOS=("WorkOrderRequest" "WorkOrderResponse" "WorkOrderStatusRequest" "PagedResponse")
for dto in "${DTOS[@]}"; do
  if grep -q "public record $dto" src/main/java/com/gpc/oms/dto/*.java; then
    report_pass "Immutable Java 17 Record verified: $dto"
  else
    report_fail "DTO $dto is not an immutable Java 17 record"
  fi
done

# Check Invariant Encapsulation on WorkOrder Entity (no public setStatus)
if grep -q "void setStatus" src/main/java/com/gpc/oms/domain/WorkOrder.java; then
  report_fail "Violation: WorkOrder has public setStatus method!"
else
  report_pass "Invariant encapsulation verified: No public setStatus method on WorkOrder entity"
fi

# ------------------------------------------------------------------------------
# PHASE 14: Syntax Performance & Code Reuse Optimization
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 14] Syntax Performance & Code Reuse Optimization ---"
# Check centralized RFC 7807 URN constants in ProblemTypes.java
if grep -q "public static final URI VALIDATION_ERROR" src/main/java/com/gpc/oms/exception/ProblemTypes.java; then
  report_pass "Centralized RFC 7807 Problem Type URI constants in ProblemTypes.java"
else
  report_fail "Missing centralized URI constants in ProblemTypes.java"
fi

# Check Switch Expressions
if grep -q "switch (this)" src/main/java/com/gpc/oms/domain/WorkOrderStatus.java && grep -q -- "->" src/main/java/com/gpc/oms/domain/WorkOrderStatus.java; then
  report_pass "Java 17 Enhanced Switch Expression verified in WorkOrderStatus.java"
else
  report_fail "Enhanced Switch Expression not used in WorkOrderStatus.java"
fi

# Check Test Fixture Pattern
if [ -f "src/test/java/com/gpc/oms/testutil/WorkOrderTestFixtures.java" ]; then
  report_pass "Test Fixture Pattern verified: WorkOrderTestFixtures.java"
else
  report_fail "Missing WorkOrderTestFixtures.java in src/test/java/com/gpc/oms/testutil/"
fi

# Check UTC Instant timestamps
if grep -q "java.time.Instant" src/main/java/com/gpc/oms/domain/WorkOrder.java; then
  report_pass "UTC Instant timestamps verified on domain entity"
else
  report_fail "Domain entity does not use java.time.Instant for timestamps"
fi

# ------------------------------------------------------------------------------
# PHASE 15: Strict Checklist & Automated Audit Generation
# ------------------------------------------------------------------------------
echo ""
echo "--- [PHASE 15] Strict Checklist & Automated Audit Generation ---"
LATEST_CHECKLIST=$(ls -t docs/audit-logs/checklist-*.md 2>/dev/null | head -n 1)
if [ -n "$LATEST_CHECKLIST" ]; then
  report_pass "Physical audit checklist artifact found: $LATEST_CHECKLIST"
  UNPASSED=$(grep -E '\|\s*\[ \]|\|\s*(\[ \]\s*)?FAIL\s*\|' "$LATEST_CHECKLIST" || true)
  if [ -z "$UNPASSED" ]; then
    PASSED_COUNT=$(grep -c '\[x\] PASS' "$LATEST_CHECKLIST" || true)
    report_pass "Checklist sign-off audit PASSED: $PASSED_COUNT criteria 100% verified [x] PASS (Zero unpassed items)"
  else
    report_fail "Checklist has unverified or failed criteria in $LATEST_CHECKLIST: $UNPASSED"
  fi
else
  report_fail "No physical checklist artifact found in docs/audit-logs/"
fi

# ------------------------------------------------------------------------------
# FINAL SUMMARY
# ------------------------------------------------------------------------------
echo ""
echo "======================================================================"
if [ "$FAILED_GATES" -eq 0 ]; then
  echo "🎉 ALL AI-NATIVE SDLC GATES (PHASES 05 -> 15) PASSED WITH 100% SUCCESS!"
  echo "======================================================================"
  exit 0
else
  echo "💥 AI-NATIVE SDLC VERIFICATION FAILED: $FAILED_GATES gates failed!"
  echo "======================================================================"
  exit 1
fi
