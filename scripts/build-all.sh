#!/usr/bin/env bash
# Builds every service's jar with Maven. Run from the repo root or from scripts/.
set -euo pipefail
cd "$(dirname "$0")/.."

SERVICES=(common/common-lib config-server service-discovery api-gateway \
          product-service order-service payment-service user-service \
          inventory-service notification-service)

echo "Installing shared common-lib first..."
(cd common/common-lib && mvn -q -DskipTests install)

for svc in "${SERVICES[@]}"; do
  if [ "$svc" = "common/common-lib" ]; then continue; fi
  echo "Building $svc..."
  (cd "$svc" && mvn -q -DskipTests package)
done

echo "All services built successfully."
