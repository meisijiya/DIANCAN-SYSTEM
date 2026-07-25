#!/usr/bin/env bash
# ============================================================
# scripts/docker-reset.sh — 重置中间件（删除所有数据）
# ============================================================
# 警告：会删除所有数据库表、上传的文件、缓存数据
# 用法：bash scripts/docker-reset.sh
# ============================================================
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

RED='\033[0;31m'
YELLOW='\033[1;33m'
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

echo -e "${RED}⚠️  警告：即将删除所有 MySQL / Redis / MinIO / RocketMQ 数据${NC}"
echo -e "${RED}    这意味着你将丢失所有数据库修改、上传文件、消息队列数据${NC}"
echo ""
read -p "确认要重置吗？输入 YES 继续：" CONFIRM

if [ "$CONFIRM" != "YES" ]; then
  echo -e "${GREEN}已取消${NC}"
  exit 0
fi

echo -e "${YELLOW}[INFO]${NC} 停止并删除容器 + 数据卷..."
$COMPOSE_CMD down -v

echo -e "${YELLOW}[INFO]${NC} 清理预处理目录..."
rm -rf .docker/initdb

echo -e "${GREEN}✅ 数据已重置。下次启动：bash scripts/docker-up.sh${NC}"