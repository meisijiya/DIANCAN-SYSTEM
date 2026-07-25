#!/usr/bin/env bash
# ============================================================
# scripts/deploy-pages.sh — 一键部署 docs/ 到 GitHub Pages
# ============================================================
# 前置条件：
#   1. 安装 gh CLI：apt install gh
#   2. 登录：gh auth login
#   3. 在 GitHub 创建同名仓库（如 DIANCAN-SYSTEM）
#
# 用法：
#   bash scripts/deploy-pages.sh
#   # 或者自定义用户名/仓库：
#   GITHUB_USER=alice REPO_NAME=blog bash scripts/deploy-pages.sh
#
# 这个脚本会：
#   1. 检查 gh CLI 已登录
#   2. 检查 docs/index.html 存在
#   3. 启用 GitHub Pages（从 master 分支 /docs 部署）
#   4. 触发部署（git push）
#   5. 等 60 秒 + 验证 4 个 URL
# ============================================================
set -euo pipefail

# ---------- 颜色输出 ----------
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---------- 配置 ----------
GITHUB_USER="${GITHUB_USER:-meisijiya}"
REPO_NAME="${REPO_NAME:-DIANCAN-SYSTEM}"
SOURCE_BRANCH="${SOURCE_BRANCH:-master}"
SOURCE_PATH="${SOURCE_PATH:-/docs}"

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ---------- 1. 前置检查 ----------
info "检查环境..."
command -v gh >/dev/null 2>&1 || error "缺 gh CLI。请运行：apt install gh"
gh auth status >/dev/null 2>&1 || error "gh CLI 未登录。请运行：gh auth login"

[ -d .git ] || error "当前目录不是 git 仓库"

[ -f docs/index.html ] || error "docs/index.html 不存在。请先创建入口页"
[ -d docs/learning ] || warn "docs/learning/ 不存在。教程会被忽略"

info "✅ gh CLI 已登录"
info "目标仓库：${GITHUB_USER}/${REPO_NAME}"
info "部署方式：${SOURCE_BRANCH} 分支的 ${SOURCE_PATH} 目录"

# ---------- 2. 启用 GitHub Pages ----------
info "🚀 启用 GitHub Pages..."

# 先尝试 POST（首次启用），失败回退到 PUT（更新现有配置）
$COMPOSE_CMD 2>/dev/null
ERROR_OUTPUT=$(gh api -X POST "repos/${GITHUB_USER}/${REPO_NAME}/pages" \
  -f "source[branch]=${SOURCE_BRANCH}" \
  -f "source[path]=${SOURCE_PATH}" \
  -f "build_type=legacy" 2>&1) || {
  warn "POST 失败（可能 Pages 已启用），尝试 PUT..."
  gh api -X PUT "repos/${GITHUB_USER}/${REPO_NAME}/pages" \
    -f "source[branch]=${SOURCE_BRANCH}" \
    -f "source[path]=${SOURCE_PATH}" \
    -f "build_type=legacy" >/dev/null || error "启用 Pages 失败：$ERROR_OUTPUT"
}

# ---------- 3. 触发部署 ----------
info "📤 触发部署（git push）..."
git add docs/ 2>/dev/null || true

# 如果 docs/ 有变更，提交；否则跳过
if ! git diff --cached --quiet 2>/dev/null; then
  git commit -m "docs: 触发 GitHub Pages 部署" >/dev/null
fi

git push origin "${SOURCE_BRANCH}" 2>&1 | tail -3

# ---------- 4. 等部署完成 ----------
info "⏳ 等 60 秒部署..."
sleep 60

# ---------- 5. 验证 ----------
PAGES_URL="https://${GITHUB_USER}.github.io/${REPO_NAME}/"
info "🔍 验证 ${PAGES_URL}"

PATHS=(
  "/"
  "/learning/diancan-system-tutorial.html"
  "/learning/code-exercises.html"
  "/learning/docker-guide.md"
)

ALL_OK=true
for path in "${PATHS[@]}"; do
  url="${PAGES_URL}${path}"
  STATUS=$(curl -sIL -o /dev/null -w "%{http_code}" --max-time 15 "$url" 2>/dev/null || echo "000")
  if [ "$STATUS" = "200" ]; then
    info "  ✅ $path → $STATUS"
  else
    warn "  ⚠️  $path → $STATUS"
    ALL_OK=false
  fi
done

echo ""
if [ "$ALL_OK" = true ]; then
  info "🎉 部署成功！"
  info "   访问：${PAGES_URL}"
else
  warn "⚠️  部分 URL 不通。可能是 CDN 缓存，等几分钟重试："
  warn "   bash scripts/deploy-pages.sh"
fi