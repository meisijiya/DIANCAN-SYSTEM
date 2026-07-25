#!/usr/bin/env bash
# ============================================================
# scripts/docker-down.sh — 停止中间件（保留数据）
# ============================================================
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

GREEN='\033[0;32m'
NC='\033[0m'

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  echo "未找到 docker compose"
  exit 1
fi

echo -e "${GREEN}[INFO]${NC} 停止中间件容器（数据卷保留）..."
$COMPOSE_CMD down

echo -e "${GREEN}✅ 已停止。下次启动：bash scripts/docker-up.sh"