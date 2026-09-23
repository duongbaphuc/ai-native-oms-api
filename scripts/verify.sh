#!/usr/bin/env bash
# Verify gate: tests green before commit (Phase 3).
set -e
mvn test
