# 管理端项目知识库

## 适用范围

本文件只描述 `diancan-admin-web` 的前端结构与约定。跨端接口前缀、后端启动顺序和数据库事项见根级 `AGENTS.md`。

## 技术与工作区

- Vue 3.5、Vite 7、TypeScript 5.9、Naive UI、UnoCSS、Pinia 3、Vue Router 4。
- 要求 Node `>=20.19.0`、pnpm `>=10.5.0`。
- 当前目录既是应用根，也是 pnpm workspace 根。`pnpm-workspace.yaml` 只纳入 `packages/*`。
- 应用通过 `workspace:*` 使用本地 `@sa/*` 包，不要把这些包替换成重复的应用内工具。
- `@sa/axios`：Axios 实例、平铺响应、重试、请求取消与后端错误钩子。
- `@sa/hooks`：加载态、布尔状态、表格和上下文等组合式函数。
- `@sa/color`：主题调色板与颜色转换。
- `@sa/materials`：布局、滚动容器、页签等通用界面材料。
- `@sa/utils`：存储、克隆、ID 等基础工具。
- `@sa/scripts`：提供 `sa` CLI，驱动提交、路由生成、清理、发布和依赖更新。
- `@sa/uno-preset`：项目 UnoCSS 预设。
- workspace 还包含 `@sa/alova`，但主应用 `package.json` 没有直接依赖它。

## 启动入口与 Vite

- `src/main.ts` 是应用入口，顺序为全局资源与插件初始化、`createApp`、Pinia、Router、i18n、版本通知、根节点校验，最后挂载 `#app`。
- Router 初始化是异步的，入口会等待 `router.isReady()`，不要提前挂载应用。
- `vite.config.ts` 提供 `~` 到项目根、`@` 到 `src` 的别名。
- 开发服务器监听 `0.0.0.0:9527`，预览端口为 `9725`。
- SCSS 自动注入 `src/styles/scss/global.scss`，公共变量不需要重复导入。
- `VITE_HTTP_PROXY=Y` 时，开发服务按 `build/config` 的规则创建代理。
- 注意：`pnpm dev` 实际执行 `vite --mode test`，不会读取 `.env.dev`。需要 dev mode 时应先确认命令意图，不能只改 `.env.dev` 后假定已生效。

## 路由

- `src/router/index.ts` 初始只注册 builtin routes，并按 `VITE_ROUTER_HISTORY_MODE` 选择 history、hash 或 memory history。
- 全局守卫位于 `src/router/guard/`，依次安装进度条、鉴权路由和页面标题守卫。
- `src/router/elegant/routes.ts` 与 `imports.ts` 由 Elegant Router 生成。新增或调整页面后优先运行 `pnpm gen-route`，不要手改生成文件。
- `src/router/routes/index.ts` 将 Elegant Router 路由转换为 Vue Router 路由；`customRoutes` 仅用于生成器无法表达的例外。
- `VITE_AUTH_ROUTE_MODE` 在 `.env` 中切换 `static` 与 `dynamic` 两种鉴权路由模式。
- static 模式使用生成路由，并按用户角色过滤；`VITE_STATIC_SUPER_ROLE` 可绕过角色过滤。
- dynamic 模式从服务端获取常量路由、用户路由和首页，并按需检查路由是否存在。
- 路由守卫先初始化常量路由，再按登录状态初始化鉴权路由；未登录跳转登录页并保留 `redirect`。
- `meta.constant` 表示无需登录，`meta.roles` 控制角色访问；已登录但无权访问时进入 403。
- 动态路由加载前命中 not-found 时，守卫会在初始化后重放原地址，修改此流程时要保留该恢复逻辑。

## Pinia

- `src/store/index.ts` 创建 Pinia，并安装 `resetSetupStore`，为 setup store 提供统一重置能力。
- `modules/auth`：登录、用户信息、Token、登出清理和静态超级角色判断。
- `modules/route`：常量与鉴权路由、菜单、面包屑、路由缓存、static/dynamic 分流。
- `modules/tab`：首页页签、多页签、固定页签、缓存和路由切换。
- `modules/theme`：主题方案、Naive UI 覆盖、CSS 变量、水印和本地持久化。
- `modules/app`：响应式布局、侧栏、全屏内容、语言和页面重载。
- Store 间已有明确协作关系，新增全局状态前先检查现有模块，页面局部状态继续留在页面内。

## 请求层与错误码

- 业务接口集中在 `src/service/api/`，统一经 `src/service/request/index.ts` 发出；页面不要自行创建 Axios 实例。
- `request` 使用 `@sa/axios` 的 `createFlatRequest`，调用结果是 `{ data, error, response }`，响应数据会统一格式化日期时间字段。
- `src/service/request/shared.ts` 从本地存储读取 Token。当前 Sa-Token 合同要求 `Authorization` 直接传 Token，不加 `Bearer`。
- `.env` 定义成功码 `200`、直接登出码 `401`、弹窗登出码 `7777,7778`、过期码 `9999,9998,3333`。
- 改后端业务码时，要同步核对 `VITE_SERVICE_SUCCESS_CODE`、`VITE_SERVICE_LOGOUT_CODES`、`VITE_SERVICE_MODAL_LOGOUT_CODES` 和 `VITE_SERVICE_EXPIRED_TOKEN_CODES`。
- `@sa/axios` 的刷新合同位于 `onBackendFail`：过期码触发刷新，成功后更新请求头并重放原 Axios config。
- `refreshTokenPromise` 会合并并发刷新请求；刷新完成约 1 秒后清空，不能绕过该共享状态自行并发刷新。
- `/auth/refreshToken` 接收 `refreshToken`，成功必须返回新的 `token` 与 `refreshToken`。
- 刷新接口失败时不能再次返回过期码，否则会递归重试；应返回直接登出码或弹窗登出码。
- 错误消息通过 `errMsgStack` 去重，弹窗登出和刷新中的错误不会重复弹普通消息。
- `.env.dev` 给出本地服务地址和支付回跳地址，但是否加载取决于 Vite mode。

## WebSocket

- `src/service/websocket.ts` 使用 STOMP 7 和 SockJS，连接地址复用 HTTP baseURL 规则并追加 `/ws`。
- 客户端启用 5 秒重连、双向 10 秒心跳，并在重连后恢复全部 topic。
- `subscribe(topic, handler)` 返回取消订阅函数。页面必须在卸载时调用它，避免处理器残留。
- 当前实时消费者是后厨页的 `/topic/kitchen` 和桌台看板的 `/topic/table-status`。
- `connectWebSocket` 是幂等入口，不要在页面内另建 STOMP Client。

## 页面布局与热点

- `src/views/` 按业务域分为 `_builtin`、`home`、`dish`、`table`、`order`、`service`、`marketing`、`report`、`manage`、`device`、`monitor`、`log`。
- 页面接口类型集中在全局 `Api.*` 类型，路由类型来自 Elegant Router 生成声明。
- `service/place-order`：堂食选桌、菜品筛选、本地购物车和离线订单队列，约 749 行。
- `service/order-ops`：加菜、催菜、赠送、退菜、换菜和折扣，约 685 行。
- `service/checkout`：现金与二维码支付、拆单、支付轮询和语音提示，约 769 行。
- `service/kitchen`：后厨任务、自动接单、沽清、轮询、STOMP 更新和语音播报，约 730 行。
- `service/table-board`：桌台状态、开台点餐、订单详情、结账、清台和实时更新，是当前最大热点，共 2807 行。
- 修改上述热点前先定位 script、template、style 三段边界。`table-board/index.vue` 的 style 从约 1130 行开始，局部改动不要顺手重排整文件。

## 命令、测试与提交钩子

```bash
pnpm dev          # Vite test mode
pnpm build        # prod mode 构建
pnpm build:test   # test mode 构建
pnpm typecheck    # vue-tsc
pnpm lint         # ESLint 自动修复
pnpm gen-route    # 生成 Elegant Router 文件
pnpm commit       # sa git-commit
pnpm commit:zh    # 中文交互式提交
```

- 当前没有 `test` 脚本，也没有 Vitest 或 Jest 依赖与测试栈。改动至少执行 `pnpm typecheck`、`pnpm lint` 和对应构建。
- `prepare` 安装 simple-git-hooks。
- `commit-msg` 执行 `pnpm sa git-commit-verify`，提交信息应走 `pnpm commit` 或满足同一规范。
- `pre-commit` 执行 `pnpm typecheck && pnpm lint && git diff --exit-code`。lint 会自动修复，若产生未暂存差异，钩子会失败。
- `.editorconfig` 要求 UTF-8、2 空格缩进、LF、删除行尾空白并保留文件末尾换行。
