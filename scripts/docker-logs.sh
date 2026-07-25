#!/usr/bin/env bash
# ============================================================
# scripts/docker-logs.sh — 跟踪所有容器日志
# ============================================================
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  echo "未找到 docker compose"
  exit 1
fi

SERVICE="${1:-}"
if [ -n "$SERVICE" ]; then
  echo "查看 $SERVICE 日志（Ctrl+C 退出）..."
  $COMPOSE_CMD logs -f "$SERVICE"
else
  echo "查看所有容器日志（Ctrl+C 退出）..."
  $COMPOSE_CMD logs -f
fi