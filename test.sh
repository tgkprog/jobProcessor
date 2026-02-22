#!/bin/bash

# End-to-End API Test Script for Job Processor Engine
# Author: Tushar Kapila
# Usage: ./test.sh
#
# This script:
#   1. Kills any existing jobProc process on port 8087
#   2. Cleans and rebuilds the project
#   3. Starts the server in background
#   4. Waits for it to be ready
#   5. Runs API tests against real H2 database
#   6. Reports results
#   7. Stops the server

PORT=8087
JAR="target/jobProc-1.0.0-fat.jar"
PASS=0
FAIL=0
TOTAL=0

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() { echo -e "${YELLOW}[TEST]${NC} $1"; }
pass() { echo -e "  ${GREEN}✔ PASS${NC}: $1"; ((PASS++)); ((TOTAL++)); }
fail() { echo -e "  ${RED}✘ FAIL${NC}: $1 — got: $2"; ((FAIL++)); ((TOTAL++)); }

# ============================================
# Step 1: Kill existing process on port
# ============================================
log "Killing any existing process on port $PORT..."
PID=$(lsof -ti :$PORT 2>/dev/null)
if [ -n "$PID" ]; then
    kill -9 $PID 2>/dev/null
    sleep 1
    echo "  Killed PID $PID"
else
    echo "  No existing process found."
fi

# ============================================
# Step 2: Clean old DB and rebuild
# ============================================
log "Cleaning old test DB..."
rm -f data/jobprocdb.mv.db data/jobprocdb.trace.db

log "Building project..."
mvn clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo -e "${RED}BUILD FAILED${NC}"
    exit 1
fi
log "Build OK."

# ============================================
# Step 3: Start server in background
# ============================================
log "Starting server..."
java -jar $JAR > /tmp/jobproc_test.log 2>&1 &
SERVER_PID=$!
echo "  Server PID: $SERVER_PID"

# ============================================
# Step 4: Wait for server to be ready
# ============================================
log "Waiting for server to start (max 60s)..."
for i in $(seq 1 60); do
    if curl -s http://localhost:$PORT/api/job/status > /dev/null 2>&1; then
        echo "  Server ready after ${i}s"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}Server failed to start in 60s${NC}"
        cat /tmp/jobproc_test.log | tail -20
        kill $SERVER_PID 2>/dev/null
        exit 1
    fi
    sleep 1
done

echo ""
echo "========================================"
echo " Running API Tests"
echo "========================================"
echo ""

# ============================================
# Test 1: GET /api/job/status — empty initially
# ============================================
log "Test 1: GET /api/job/status (should be empty)"
RESULT=$(curl -s http://localhost:$PORT/api/job/status)
if [ "$RESULT" = "[]" ]; then
    pass "Job status returns empty list on fresh DB"
else
    fail "Expected [], got" "$RESULT"
fi

# ============================================
# Test 2: GET /api/job/listAll — no processors
# ============================================
log "Test 2: GET /api/job/listAll (should be empty)"
RESULT=$(curl -s http://localhost:$PORT/api/job/listAll)
if [ "$RESULT" = "[]" ]; then
    pass "Processor list returns empty on fresh DB"
else
    fail "Expected [], got" "$RESULT"
fi

# ============================================
# Test 3: POST /api/job/add — register a processor
# ============================================
log "Test 3: POST /api/job/add (register processor)"
RESULT=$(curl -s -X POST http://localhost:$PORT/api/job/add \
    -H "Content-Type: application/json" \
    -d '{"className":"com.sel2in.jobProc.processors.DateFileTxn","jarPath":"/data/jobProc/dateFileTxn.jar"}')
if echo "$RESULT" | grep -q "saved successfully"; then
    pass "Processor registered"
else
    fail "Expected success message" "$RESULT"
fi

# ============================================
# Test 4: GET /api/job/listAll — should have 1
# ============================================
log "Test 4: GET /api/job/listAll (should have 1 processor)"
RESULT=$(curl -s http://localhost:$PORT/api/job/listAll)
COUNT=$(echo "$RESULT" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
if [ "$COUNT" = "1" ]; then
    pass "Processor list has 1 entry"
else
    fail "Expected count=1" "$RESULT"
fi

# ============================================
# Test 5: Verify processor fields
# ============================================
log "Test 5: Verify processor className"
CLASS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['className'])" 2>/dev/null)
if [ "$CLASS" = "com.sel2in.jobProc.processors.DateFileTxn" ]; then
    pass "Processor className matches"
else
    fail "Expected DateFileTxn class" "$CLASS"
fi

# ============================================
# Test 6: POST /api/job/schedule — schedule a job
# ============================================
log "Test 6: POST /api/job/schedule (schedule a job)"
RESULT=$(curl -s -X POST "http://localhost:$PORT/api/job/schedule?jobName=TestAuditQ1&processorClassName=com.sel2in.jobProc.processors.DateFileTxn&comment=API+Test")
STATUS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
JOB_ID=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
if [ "$STATUS" = "SCHEDULED" ]; then
    pass "Job scheduled with status=SCHEDULED, id=$JOB_ID"
else
    fail "Expected status=SCHEDULED" "$RESULT"
fi

# ============================================
# Test 7: GET /api/job/status — should have 1 job
# ============================================
log "Test 7: GET /api/job/status (should have 1 job)"
RESULT=$(curl -s http://localhost:$PORT/api/job/status)
COUNT=$(echo "$RESULT" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
if [ "$COUNT" = "1" ]; then
    pass "Job status has 1 entry"
else
    fail "Expected count=1" "$RESULT"
fi

# ============================================
# Test 8: Schedule a second job
# ============================================
log "Test 8: POST /api/job/schedule (second job)"
RESULT=$(curl -s -X POST "http://localhost:$PORT/api/job/schedule?jobName=TestAuditQ2&processorClassName=com.sel2in.jobProc.processors.DateFileTxn&comment=Second+Job")
STATUS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
if [ "$STATUS" = "SCHEDULED" ]; then
    pass "Second job scheduled"
else
    fail "Expected status=SCHEDULED" "$RESULT"
fi

# ============================================
# Test 9: Verify 2 jobs exist
# ============================================
log "Test 9: GET /api/job/status (should have 2 jobs)"
RESULT=$(curl -s http://localhost:$PORT/api/job/status)
COUNT=$(echo "$RESULT" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
if [ "$COUNT" = "2" ]; then
    pass "Job status has 2 entries"
else
    fail "Expected count=2" "$RESULT"
fi

# ============================================
# Test 10: DELETE /api/job/remove — delete processor
# ============================================
log "Test 10: DELETE /api/job/remove (delete processor)"
RESULT=$(curl -s -X DELETE "http://localhost:$PORT/api/job/remove/com.sel2in.jobProc.processors.DateFileTxn")
if echo "$RESULT" | grep -q "removed"; then
    pass "Processor removed"
else
    fail "Expected removed message" "$RESULT"
fi

# ============================================
# Test 11: Verify processor deleted
# ============================================
log "Test 11: GET /api/job/listAll (should be empty after delete)"
RESULT=$(curl -s http://localhost:$PORT/api/job/listAll)
if [ "$RESULT" = "[]" ]; then
    pass "Processor list empty after removal"
else
    fail "Expected [], got" "$RESULT"
fi

# ============================================
# Test 12: H2 Console accessible
# ============================================
log "Test 12: H2 Console accessible"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/h2)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ]; then
    pass "H2 console responds (HTTP $HTTP_CODE)"
else
    fail "Expected 200 or 302" "HTTP $HTTP_CODE"
fi

# ============================================
# Test 13: Static HTML served
# ============================================
log "Test 13: Static jobs.html accessible"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/jobs.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "jobs.html served (HTTP 200)"
else
    fail "Expected 200" "HTTP $HTTP_CODE"
fi

log "Test 14: Static jobsProcs.html accessible"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/jobsProcs.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "jobsProcs.html served (HTTP 200)"
else
    fail "Expected 200" "HTTP $HTTP_CODE"
fi

# ============================================
# Summary
# ============================================
echo ""
echo "========================================"
echo " Test Results"
echo "========================================"
echo -e " Total:  $TOTAL"
echo -e " ${GREEN}Passed: $PASS${NC}"
echo -e " ${RED}Failed: $FAIL${NC}"
echo "========================================"
echo ""

# ============================================
# Cleanup: Stop server
# ============================================
log "Stopping server (PID $SERVER_PID)..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null
echo "  Server stopped."

# Exit code based on failures
if [ $FAIL -gt 0 ]; then
    exit 1
fi
exit 0
