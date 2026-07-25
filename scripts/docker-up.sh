#!/usr/bin/env bash
# ============================================================
# scripts/docker-up.sh — 一键启动中间件
# ============================================================
# 流程：
#   1. 检查 Docker / docker compose
#   2. 预处理 SQL 文件（库名 digital_ordering_system → diancan-system）
#   3. 复制 .env.docker.example → .env（如不存在）
#   4. docker compose up -d
#   5. 等待 MySQL 初始化完成
# ============================================================
set -euo pipefail

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# ---------- 1. 前置检查 ----------
info "检查 Docker 环境..."
if ! command -v docker >/dev/null 2>&1; then
  error "未找到 docker 命令。请先安装 Docker Desktop (WSL2 用户推荐) 或 docker-ce。"
fi

if ! docker info >/dev/null 2>&1; then
  error "Docker daemon 未运行。请先启动 Docker Desktop 或执行 'sudo systemctl start docker'。"
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  error "未找到 docker compose。请升级到 Docker Desktop 或安装 docker-compose-plugin。"
fi
info "使用命令: $COMPOSE_CMD"

# ---------- 2. 复制 .env（如不存在）----------
if [ ! -f .env ]; then
  info "创建 .env 文件..."
  cp .env.docker.example .env
else
  warn ".env 已存在，跳过复制。如需重置请手动修改或删除。"
fi

# ---------- 3. 预处理 SQL 文件 ----------
# AGENTS.md 已知坑：SQL 导出库名 digital_ordering_system，与 application-dev.yml 的 diancan-system 不一致
# 我们在本地生成一份修正后的 SQL 到 .docker/initdb/，MySQL 首次启动会自动执行
info "预处理 SQL 文件（库名修正）..."
mkdir -p .docker/initdb
sed 's/digital_ordering_system/diancan-system/g' \
  db/diancan-system.sql \
  > .docker/initdb/01-init.sql
info "✅ 已生成 .docker/initdb/01-init.sql（库名：digital_ordering_system → diancan-system）"

# ---------- 4. 启动容器 ----------
info "启动中间件容器（首次启动约 60~90 秒）..."
$COMPOSE_CMD up -d

# ---------- 5. 等待 MySQL 就绪 ----------
info "等待 MySQL 就绪..."
RETRIES=30
INTERVAL=3
for i in $(seq 1 $RETRIES); do
  if docker exec diancan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-123456}" \
       -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='diancan-system'" 2>/dev/null | grep -q '[0-9]'; then
    TABLE_COUNT=$(docker exec diancan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-123456}" -N -B \
      -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='diancan-system'" 2>/dev/null)
    info "✅ MySQL 就绪，diancan-system 库共 $TABLE_COUNT 张表"
    break
  fi
  echo -n "."
  sleep $INTERVAL
  if [ "$i" -eq "$RETRIES" ]; then
    error "MySQL 启动超时。请检查 docker logs diancan-mysql"
  fi
done

# ---------- 6. 打印连接信息 ----------
cat <<EOF

${GREEN}╔══════════════════════════════════════════════════════════╗
║              🎉 中间件启动成功                            ║
╚══════════════════════════════════════════════════════════╝${NC}

服务地址：
  MySQL       localhost:${MYSQL_PORT:-3307}   root / 123456  库: diancan-system
  Redis       localhost:6379   密码: 123456
  RocketMQ    localhost:9876   (NameServer)
  MinIO API   http://localhost:9000   minioadmin / minioadmin
  MinIO Web   http://localhost:9001   (Web 控制台)

下一步：
  ① 启动后端（自动连接到上述中间件）
     cd diancan-admin
     mvn spring-boot:run

  ② 启动管理端
     cd diancan-admin-web
     pnpm install && pnpm dev

  ③ 启动小程序（微信开发者工具导入 diancan-miniapp）

查看日志：$COMPOSE_CMD logs -f
停止服务：bash scripts/docker-down.sh
重置数据：bash scripts/docker-reset.sh
EOF