#!/bin/bash
# Applies every migration to a fresh database on a local PostgreSQL (15+) and runs the tests.
# Uses the usual libpq settings, e.g.:  PGHOST=localhost PGUSER=postgres supabase/tests/run.sh
set -uo pipefail
cd "$(dirname "$0")"
DB=enkan_haban_test
P="psql -X -q -v ON_ERROR_STOP=1"
$P -d postgres -c "drop database if exists $DB" -c "create database $DB" >/dev/null || exit 1
for f in supabase_stub.sql ../migrations/*.sql; do
  $P -d $DB -f "$f" 2>&1 | grep -v -e wal_level -e 'HINT:' 
  [ "${PIPESTATUS[0]}" -ne 0 ] && { echo "MIGRATION FAILED: $f"; exit 1; }
done
out=$($P -d $DB -f security_test.sql 2>&1 | grep -E '^ (PASS|FAIL)|passed|ERROR')
echo "$out"
! grep -q -e '^ FAIL' -e ERROR <<<"$out"
