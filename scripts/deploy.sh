#!/usr/bin/env bash
# Deploys the platform to Kubernetes via the Helm chart in infrastructure/k8s/helm.
set -euo pipefail
cd "$(dirname "$0")/.."

NAMESPACE=${1:-ecommerce}

echo "Applying namespaces, configmaps and secrets..."
kubectl apply -f infrastructure/k8s/namespaces.yaml
kubectl apply -f infrastructure/k8s/configmaps.yaml
kubectl apply -f infrastructure/k8s/secrets.yaml

echo "Installing/upgrading Helm release 'ecommerce' in namespace $NAMESPACE..."
helm upgrade --install ecommerce infrastructure/k8s/helm \
  --namespace "$NAMESPACE" \
  --set namespace="$NAMESPACE"

echo "Deployment triggered. Watch rollout with:"
echo "  kubectl -n $NAMESPACE get pods -w"
