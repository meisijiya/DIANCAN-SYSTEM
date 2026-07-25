#!/usr/bin/env bash
# ============================================================
# scripts/start-backend.sh — 从 WSL 启动 Spring Boot 直连 docker MySQL
# ============================================================
# 为什么需要这个脚本：
#   - WSL 的 localhost:3306 通过 Windows 主机转发到 Windows 上的 MySQL 8.0.40
#   - 但 docker 容器里是 MySQL 8.0.46，库名 diancan-system
#   - 用容器内部 IP 172.20.0.2:3306 直连 docker 容器，绕过 Windows 转发
#
# 用法：
#   bash scripts/start-backend.sh
# ============================================================
set -euo pipefail

source /home/ljh2923/.sdkman/bin/sdkman-init.sh 2>/dev/null || true
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# 获取 MySQL 容器 IP
MYSQL_IP=$(docker inspect diancan-mysql --format='{{.NetworkSettings.IPAddress}}' 2>/dev/null || echo "")
if [ -z "$MYSQL_IP" ]; then
  echo -e "${RED}❌ MySQL 容器没启动或没拿到 IP，请先跑 docker-up.sh${NC}"
  exit 1
fi

echo -e "${GREEN}✅ MySQL 容器 IP: $MYSQL_IP${NC}"

# 验证容器 IP 能直连
docker exec diancan-mysql mysql -uroot -p123456 -e "SELECT '容器内 MySQL 8.0.46 OK'" 2>&1 | grep -v "Using a password" || {
  echo -e "${RED}❌ 容器内 MySQL 访问失败${NC}"
  exit 1
}

# 检查 mvn 是否可用
if ! command -v mvn >/dev/null 2>&1; then
  echo -e "${RED}❌ mvn 未安装。请先: source ~/.sdkman/bin/sdkman-init.sh && sdk install maven 3.9.6${NC}"
  exit 1
fi

# 用环境变量覆盖 Spring Boot 的 datasource URL
export SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_IP}:3306/diancan-system?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=123456
export SPRING_DATA_REDIS_HOST=$MYSQL_IP  # 临时设错，让 spring 启动时报清楚
# 实际 redis 也得直连容器 IP
REDIS_IP=$(docker inspect diancan-redis --format='{{.NetworkSettings.IPAddress}}' 2>/dev/null || echo "")
if [ -n "$REDIS_IP" ]; then
  export SPRING_DATA_REDIS_HOST=$REDIS_IP
  echo -e "${GREEN}✅ Redis 容器 IP: $REDIS_IP${NC}"
fi

ROCKETMQ_IP=$(docker inspect diancan-rocketmq-namesrv --format='{{.NetworkSettings.IPAddress}}' 2>/dev/null || echo "")
if [ -n "$ROCKETMQ_IP" ]; then
  export ROCKETMQ_NAME_SERVER="${ROCKETMQ_IP}:9876"
  echo -e "${GREEN}✅ RocketMQ NameServer IP: $ROCKETMQ_IP${NC}"
fi

MINIO_IP=$(docker inspect diancan-minio --format='{{.NetworkSettings.IPAddress}}' 2>/dev/null || echo "")
if [ -n "$MINIO_IP" ]; then
  export STORAGE_MINIO_ENDPOINT="http://${MINIO_IP}:9000"
  echo -e "${GREEN}✅ MinIO IP: $MINIO_IP${NC}"
fi

echo ""
echo -e "${GREEN}🚀 启动 Spring Boot（环境变量已配置直连 docker 容器）...${NC}"
echo "  MySQL URL: $SPRING_DATASOURCE_URL"
echo "  Redis Host: $SPRING_DATA_REDIS_HOST"
echo "  RocketMQ: $ROCKETMQ_NAME_SERVER"
echo "  MinIO: $STORAGE_MINIO_ENDPOINT"
echo ""

cd diancan-admin
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev