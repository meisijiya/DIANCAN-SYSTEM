# 🐳 Docker 一键启动指南（WSL2 / Ubuntu）

> 配套学习手册：[diancan-system-tutorial.html](./diancan-system-tutorial.html)
> 本指南教你：**WSL2 装 Docker → 一键拉起中间件 → 跑通后端**。

---

## 📚 你将学到什么

1. WSL2 上装 Docker Desktop 的两种方式
2. 理解 Docker Compose 的"一键启动"原理
3. 学会 docker 命令的最小集合
4. 排查容器启动失败的常见问题

---

## 🪜 步骤 0 · WSL2 装 Docker

### 方案 A · Docker Desktop（最省心，强烈推荐）

Windows 用户首选，Docker Desktop for Windows 自带 WSL2 集成。

```powershell
# Windows PowerShell（管理员）
# 1. 开启 WSL2 功能
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# 2. 重启电脑

# 3. 设置 WSL2 为默认版本
wsl --set-default-version 2

# 4. 安装 Ubuntu 24.04（如果还没装）
# Microsoft Store 搜索 Ubuntu 24.04 安装

# 5. 下载 Docker Desktop
# https://www.docker.com/products/docker-desktop/
# 安装时勾选 "Use WSL 2 based engine"

# 6. 启动 Docker Desktop，等右下角图标变绿

# 7. 验证
wsl
docker --version
docker run hello-world
```

### 方案 B · 直接在 WSL2 里装 docker-ce（进阶用户）

```bash
# Ubuntu 24.04 (WSL2) 内执行
sudo apt update
sudo apt install -y ca-certificates curl gnupg

# 添加 Docker 官方 GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# 添加仓库
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 允许当前用户免 sudo 用 docker
sudo usermod -aG docker $USER
newgrp docker

# 验证
docker --version
docker run hello-world
```

> ⚠️ WSL2 内 docker-ce 启动 daemon 需要 `sudo service docker start` 或在 `/etc/wsl.conf` 加 `[boot] command="service docker start"`。

---

## 🪜 步骤 1 · 启动中间件

```bash
cd /home/ljh2923/opencode-project/diancan-system
bash scripts/docker-up.sh
```

**输出示例**：
```
[INFO] 检查 Docker 环境...
[INFO] 使用命令: docker compose
[INFO] 预处理 SQL 文件（库名修正）...
✅ 已生成 .docker/initdb/01-init.sql
[INFO] 启动中间件容器（首次启动约 60~90 秒）...
[+] Running 6/6
 ✔ Network diancan-system_default       Created
 ✔ Container diancan-mysql              Started
 ✔ Container diancan-redis              Started
 ✔ Container diancan-rocketmq-namesrv   Started
 ✔ Container diancan-rocketmq-broker    Started
 ✔ Container diancan-minio              Started
[INFO] 等待 MySQL 就绪...
✅ MySQL 就绪，diancan-system 库共 102 张表
```

---

## 🪜 步骤 2 · 启动后端

中间件起来后：

```bash
cd diancan-admin
mvn spring-boot:run
```

打开浏览器：`http://localhost:8080/api/doc.html`（Knife4j 接口文档）

---

## 🪜 步骤 3 · 启动管理端 + 小程序

```bash
# 另开终端
cd diancan-admin-web
pnpm install
pnpm dev
# → http://localhost:9527
# → 登录：admin / 123456
```

小程序用微信开发者工具导入 `diancan-miniapp/`。

---

## 🔧 常用 Docker 命令（最小集合）

```bash
# 查看运行中的容器
docker ps

# 查看所有容器（包括已停止）
docker ps -a

# 查看某个容器日志
docker logs -f diancan-mysql       # Ctrl+C 退出
bash scripts/docker-logs.sh mysql  # 等价命令

# 进入容器内部
docker exec -it diancan-mysql bash
docker exec -it diancan-mysql mysql -uroot -p123456    # 直接进 MySQL CLI

# 停止 / 启动容器
docker stop diancan-mysql
docker start diancan-mysql

# 重启某个服务
docker compose restart mysql

# 查看资源占用
docker stats

# 清理无用镜像（慎用）
docker system prune
```

---

## 🔍 故障排查

### 1. `docker compose` 命令找不到

**原因**：Docker Desktop 没装 compose v2，或 docker-compose-plugin 没装。

**解决**：
- Docker Desktop 用户：升级到最新版本
- docker-ce 用户：`sudo apt install docker-compose-plugin`

### 2. 端口被占用

```bash
# 看谁占了 3306
sudo lsof -i :3306
# 或
sudo ss -tlnp | grep 3306
```

**解决**：停掉旧 MySQL 服务或修改 `.env` 里 `MYSQL_PORT=3307`（但要同步改 `application-dev.yml`）。

### 3. MySQL 容器一直 restarting

```bash
docker logs diancan-mysql | tail -50
```

常见原因：
- 数据卷冲突：先 `bash scripts/docker-reset.sh` 清掉
- 内存不够：MySQL 至少需要 512MB，WSL2 默认 4GB 内存通常够

### 4. RocketMQ broker 起不来

```bash
docker logs diancan-rocketmq-broker | tail -30
```

常见原因：
- namesrv 没就绪就启动了 broker：等待 30 秒自动恢复
- broker.conf 配置错：检查 `scripts/rocketmq/broker.conf` 路径

### 5. SQL 没自动导入

**原因**：数据卷已存在但 SQL 没跑过（MySQL 只在 volume 首次创建时跑 init 脚本）。

**解决**：
```bash
bash scripts/docker-reset.sh   # 删掉数据卷
bash scripts/docker-up.sh       # 重新跑
```

### 6. 镜像拉取 429 Too Many Requests

**症状**：
```
Error response from daemon: unknown: failed to resolve reference "..."
unexpected status from GET request to ... 429 Too Many Requests
```

**原因**：Docker 配置的镜像加速器对特定 tag 触发限流（常见于第三方镜像代理）。

**解决**：
1. **重试一次**——429 通常是临时限流，等 30 秒再跑
   ```bash
   bash scripts/docker-up.sh
   ```
2. **检查镜像加速器配置**——`docker info | grep -A 5 "Registry Mirrors"`
3. **改用官方 Docker Hub**——临时禁用加速器：
   ```bash
   # 编辑 /etc/docker/daemon.json（需要 sudo）
   sudo nano /etc/docker/daemon.json
   # 把 registry-mirrors 改成空数组 []，然后 sudo systemctl restart docker
   ```
4. **本项目已规避**——`minio/mc` 是第三方工具已被移除，bucket 由后端自动创建

### 7. 后端连不上中间件

**症状**：Spring Boot 启动报 `Unknown database 'diancan-system'`，但 `docker exec diancan-mysql mysql` 看库是存在的。

**根因**：本机（WSL2 / Windows）有另一个 MySQL 占用了 3306 端口，Docker Desktop 把容器端口 publish 到 3306 失败。你的应用连到的不是 docker 里的 MySQL，而是本机的"野 MySQL"。

**解决**：
1. 改 `.env` 把 MYSQL_PORT 改成空闲端口（3307 / 3308 / 13306 等）
2. 跑 `docker compose up -d mysql` 让容器用新端口
3. 用 `bash scripts/start-backend.sh` 启动后端——脚本会用 `SPRING_DATASOURCE_URL` 环境变量覆盖默认的 localhost:3306

**原理图**：
```
[WSL2 Ubuntu] 应用 localhost:3307  → Docker Desktop NAT  → [容器 mysql 3306]  ✅
[WSL2 Ubuntu] 应用 localhost:3306  → Windows 主机 MySQL 8.0.40（不是你 docker 那个） ❌
```

### 8. Windows 主机有 MySQL / Redis 占用了端口

**检查清单**：
- [ ] 容器都 running：`docker ps`
- [ ] 后端 `application-dev.yml` 用的是 `localhost:3306`（容器和宿主机共享 localhost）
- [ ] MySQL 密码是 `123456`（和 `.env` 里一致）

---

## 📂 文件位置

| 文件 | 说明 |
|---|---|
| `docker-compose.yml` | 中间件编排（A 方案：仅中间件） |
| `.env.docker.example` | 环境变量模板（复制为 `.env` 后修改） |
| `.docker/initdb/01-init.sql` | 自动生成，库名已修正 |
| `scripts/docker-up.sh` | 一键启动 |
| `scripts/docker-down.sh` | 停止（保留数据） |
| `scripts/docker-reset.sh` | 重置（删数据） |
| `scripts/docker-logs.sh` | 日志查看 |
| `scripts/rocketmq/broker.conf` | RocketMQ broker 配置 |

---

## 🎯 学习路径建议

配合 `diancan-system-tutorial.html`：

1. ✅ **已完成** U2 后端骨架的环境准备
2. → **继续** U3 管理端启动
3. → **继续** U5 订单状态机调试（用 Swagger 调真实接口）

## 🪜 步骤 1.5 · 启动后端（WSL2 Ubuntu）

中间件起来后，**用专门的脚本启动后端**：

```bash
cd /home/ljh2923/opencode-project/diancan-system
bash scripts/start-backend.sh
```

为什么需要这个脚本：
- WSL2 的 `localhost:3306` 实际上通过 Docker Desktop 转发表被 Windows 主机的某个 MySQL 占了（如果你机器上有）
- docker 容器的 MySQL 改用 3307 端口
- 这个脚本会用 `SPRING_DATASOURCE_URL` 环境变量覆盖默认配置，强制应用走 docker 容器

**如果你用 VSCode Spring Boot Dashboard 启动**：直接启动就行，`application-dev.yml` 已经写死了 `localhost:3307`（不用环境变量）。

打开浏览器：`http://localhost:8080/api/doc.html`（Knife4j 接口文档）

---

启动后端后，到 Swagger UI 调一下 `/api/admin/dish/list`，看到返回数据说明三端链路通了。

### 9. RocketMQ broker 启动崩溃（5.x 版本 bug）

**症状**：`diancan-rocketmq-broker` 一直 `Restarting (255)`，日志里有 `java.lang.NullPointerException at ScheduleMessageService.configFilePath`。

**根因**：RocketMQ 5.x 在 `apache/rocketmq:5.x` 镜像启动时 `ScheduleMessageService.configFilePath` 触发 NPE。

**解决**：用 4.9.7，并在 `docker-compose.yml` 用绝对路径启动：
```yaml
rocketmq-broker:
  image: apache/rocketmq:4.9.7
  command: >
    /home/rocketmq/rocketmq-4.9.7/bin/mqbroker
      -n rocketmq-namesrv:9876
  volumes:
    - ./scripts/rocketmq/broker.conf:/home/rocketmq/rocketmq-4.9.7/conf/broker.conf:ro
```

注意：4.9.7 镜像的 mqbroker 不在 PATH，必须用绝对路径。