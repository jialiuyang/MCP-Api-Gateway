#!/usr/bin/env sh
# Minimal Maven wrapper shim. Replace with the official wrapper
# (`mvn wrapper:wrapper`) once the project has a stable build.
set -e
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is not on PATH. Please install Apache Maven 3.9 or later." >&2
  exit 1
fi
exec mvn "$@"
