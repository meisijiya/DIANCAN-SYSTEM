# 项目知识库

> 生成日期：2026-07-24  
> 基线提交：`327dcb6`  
> 分支：`master`

## 项目概览

这是一个**手工协调的三项目仓库**，没有根级构建编排器。三个子项目独立启动，通过同一个 Spring Boot 后端契约协作：

- `diancan-admin`：Java 17、Spring Boot 3.2.5 单体后端，默认 `8080`，上下文路径 `/api`。
- `diancan-admin-web`：Vue 3、Vite 7、TypeScript 管理端，默认 `9527`。
- `diancan-miniapp`：原生微信小程序，顾客点餐端。
- 业务主链：扫码入桌 → 菜单与购物车 → 下单支付 → 后厨履约 → 会员营销 → 评价报表。

根文档只描述跨项目关系。后续 `diancan-admin/AGENTS.md`、`diancan-admin-web/AGENTS.md`、`diancan-miniapp/AGENTS.md` 分别负责栈内细节，不在此重复。

## 目录结构

```text
diancan-system/
├── db/                         # 基线库与按日期追加的升级 SQL
│   ├── diancan-system.sql      # 新环境全量初始化
│   └── upgrade/                # 已有环境增量迁移
├── diancan-admin/              # Spring Boot 后端，统一 API 与业务编排
│   ├── pom.xml                 # Maven 依赖与 Java 17 基线
│   └── src/main/               # Java 模块、配置、MyBatis XML
├── diancan-admin-web/          # Vue 管理端，pnpm workspace
│   ├── package.json            # 脚本、Node 与 pnpm 版本约束
│   └── src/                    # 页面、服务、路由、状态
├── diancan-miniapp/            # 原生微信小程序
│   ├── project.config.json     # 微信开发者工具项目入口
│   ├── api/                    # 顾客端接口封装
│   ├── pages/                  # 页面
│   └── utils/                  # 请求、认证、桌台绑定、Socket
├── docs/images/                # README 架构图与界面截图
└── readme.md                   # 环境、启动、功能与联调说明
```

## 去哪里找

| 需求 | 首选路径 | 说明 |
| --- | --- | --- |
| 后端启动与端口 | `diancan-admin/src/main/resources/application.yml` | `8080`、`/api`、默认 `dev` profile |
| 环境依赖配置 | `diancan-admin/src/main/resources/application-*.yml` | MySQL、Redis、RocketMQ、MinIO 等 |
| 后端业务入口 | `diancan-admin/src/main/java/com/scaffold/modules/*/controller/` | 按领域区分 Admin 与 App Controller |
| 核心业务实现 | `diancan-admin/src/main/java/com/scaffold/modules/*/service/impl/` | 订单、支付、会员、优惠券等事务编排 |
| 自定义 SQL | `diancan-admin/src/main/resources/mapper/` | MyBatis XML，不要只查 Java Mapper |
| 管理端接口 | `diancan-admin-web/src/service/api/` | 与后端 `/admin/*` Controller 对照 |
| 管理端页面 | `diancan-admin-web/src/views/` | 页面按 dish、order、service、marketing 等分组 |
| 管理端请求链 | `diancan-admin-web/src/service/request/` | baseURL、鉴权与响应处理 |
| 小程序接口 | `diancan-miniapp/api/` | 相对路径由请求层补齐 `/api/app` |
| 小程序请求链 | `diancan-miniapp/utils/request.js`、`config/env.js` | Host、前缀、Token、错误归一化 |
| 页面注册 | `diancan-miniapp/app.json` | 小程序页面与 tabBar 清单 |
| 数据库初始化 | `db/diancan-system.sql` | 新库基线 |
| 数据库升级 | `db/upgrade/` | 已有库按文件日期顺序执行 |

## 跨端接口契约

后端 `server.servlet.context-path=/api`，Controller 再声明端侧前缀：

| 调用方 | 客户端路径 | 后端映射 | 典型领域 |
| --- | --- | --- | --- |
| 管理端 | `src/service/api/*` 中 `/admin/*` | `/api/admin/*` | 菜品、桌台、订单、支付、报表、系统管理 |
| 小程序 | `api/*` 中 `/order` 等相对路径 | `/api/app/*` | 登录、菜单、购物车、订单、会员、优惠券、评价 |
| 支付平台 | 外部回调地址 | `/api` 下独立回调 Controller | 微信支付通知与验签 |

修改契约时必须同步检查 Controller、对应客户端 API 封装、请求/响应类型和页面调用。管理端路径已包含 `/admin`，小程序由 `config/env.js` 的 `apiPrefix: '/api/app'` 统一补前缀，禁止在单个小程序 API 中重复写前缀。

## CODE MAP

- `DiancanAdminApplication.java`：后端进程入口。
- `modules/order/service/impl/OrderServiceImpl.java`：交易主链热点，连接桌台、菜品、支付与履约。
- `modules/payment/service/impl/PaymentServiceImpl.java`：支付状态与退款热点。
- `modules/member/service/impl/MemberSettlementServiceImpl.java`：订单结算后的会员权益联动。
- `modules/coupon/service/impl/CouponServiceImpl.java`：优惠券领取、核销与订单优惠。
- `modules/mq/service/impl/ReliableMessageServiceImpl.java`：可靠消息与重试状态。
- `diancan-admin-web/src/service/api/`：管理端契约集中层。
- `diancan-admin-web/src/views/service/`：门店点单、结账、后厨、桌台看板主流程。
- `diancan-miniapp/utils/request.js`：小程序所有 HTTP 请求汇合点。
- `diancan-miniapp/pages/menu/`、`pages/cart/`、`pages/order/`：顾客交易主流程。

当前未安装 LSP，codegraph 也未建立索引，因此上述 CODE MAP 角色来自目录、命名、端点和调用职责证据，不是中心性度量结果。

## 项目约定

- 后端按 `com.scaffold.modules.<domain>` 组织，同一领域内保持 controller、service、mapper、entity、dto 分层。
- 管理端 API 调用集中在 `src/service/api/`，页面不直接拼接服务地址。
- 小程序 API 集中在 `api/`，公共鉴权、Host 和错误处理走 `utils/request.js`。
- 数据库变更保留 `db/diancan-system.sql` 作为新环境基线，同时为已有环境在 `db/upgrade/` 增加按日期命名的增量脚本。
- 三端没有根级统一命令，必须进入对应子项目执行命令。

## 启动与迁移顺序

1. 新环境导入 `db/diancan-system.sql`。已有环境按日期顺序执行 `db/upgrade/*.sql`。
2. 启动 MySQL、Redis。完整联调再启动 RocketMQ、MinIO。
3. 启动 `diancan-admin`，先确认 `http://localhost:8080/api/swagger-ui.html`。
4. 启动 `diancan-admin-web`，访问 `http://localhost:9527`。
5. 最后用微信开发者工具导入 `diancan-miniapp`，再处理真机、登录和支付联调。

## 已确认命令

```bash
# 后端，在 diancan-admin/ 执行
mvn clean compile
mvn spring-boot:run

# 管理端，在 diancan-admin-web/ 执行
pnpm install
pnpm dev
pnpm typecheck
pnpm lint
pnpm build
```

小程序没有仓库内 CLI 构建命令，使用微信开发者工具导入 `diancan-miniapp` 后编译。

## 反模式与坑

- 不要假设根目录能一键构建或启动，仓库没有根编排器、CI、Docker 配置。
- 不要混淆 SQL 导出库名与 `application-dev.yml` 的开发库名，启动前需人工统一。
- 不要让真机访问 `127.0.0.1` 或 `localhost`，真机 Host 应指向电脑可达地址。
- 不要把管理端 `/admin` 与小程序 `/app` 契约混用。
- 不要只改页面或 Controller，跨端字段与状态枚举必须成对核对。
- 不要提交真实数据库、微信支付、对象存储或消息队列凭据。
- 小程序默认关闭 Socket，因为后端当前提供 STOMP 端点，未直接适配原生微信 WebSocket。

## 备注

- 生产入口仅见 `application-prod.yml` 与 `.env.prod`，仓库没有完整生产部署脚本。
- README 是当前环境与业务说明来源，代码与配置冲突时以实际运行路径为准并同步修正文档。
- 子项目栈内规则应写入各自的子级 `AGENTS.md`，根文件只维护跨端契约、启动顺序和共享风险。
