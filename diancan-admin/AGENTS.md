# diancan-admin 后端知识库

> 生成日期：2026-07-24  
> 基线提交：`327dcb6`  
> 分支：`master`

## 技术栈

- **Java 17**、**Spring Boot 3.2.5**（`pom.xml` 第 10 行）
- **MyBatis-Plus 3.5.5** — ORM、分页、逻辑删除
- **Sa-Token 1.37.0** — RBAC 鉴权，集成 Redis 会话
- **RocketMQ 2.3.2** — 可靠消息（优惠券发放主链路）
- **Knife4j 4.4.0** — 接口文档 → `http://localhost:8080/api/doc.html`
- **MinIO 8.5.12** — 图片/文件上传
- **EasyExcel 3.3.4** — 报表导出
- **Hutool 5.8.26** — 通用工具（BCrypt、二维码等）
- **jqwik 1.8.3** — 属性测试（已声明，当前测试未使用）

## 模块布局

所有业务代码在 `com.scaffold` 包下，分为三个层级：

```
com.scaffold/
├── DiancanAdminApplication.java          # 启动入口 (@MapperScan, @EnableAsync)
├── common/
│   ├── result/                           # Result<T>, ResultCode, PageResult<T>
│   ├── exception/                        # BusinessException, GlobalExceptionHandler
│   ├── config/                           # CorsConfig, Knife4jConfig, WebSocketConfig, WechatProperties, etc.
│   ├── enums/                            # StatusEnum, WsEventType, MenuTypeEnum
│   └── constant/                         # CacheConstants
├── framework/
│   ├── mybatis/                          # MybatisPlusConfig (分页插件), MetaObjectHandlerImpl (审计自动填充)
│   ├── satoken/                          # SaTokenConfig (拦截器+PUBLIC_PATHS), StpInterfaceImpl, SessionUtils, RolePermissionDefaults
│   ├── websocket/                        # WsService (Redis Pub/Sub 广播), WsMessage, RedisMessageSubscriber
│   ├── redis/                            # RedisConfig, RedisUtils
│   └── aspectj/                          # OperationLog + OperationLogAspect
└── modules/                              # 16 个业务模块，每个模块内分 controller/dto/entity/mapper/service/vo
    ├── system/                           # 用户、角色、菜单、字典 — RBAC 核心
    ├── dish/                             # 菜品分类、菜品、规格组、规格选项
    ├── order/                            # 订单创建、状态流转 — 交易主链
    ├── payment/                          # 支付、退款、分账、回调 — 交易主链
    ├── table/                            # 桌台管理、扫码开台、换桌、清洁
    ├── cart/                             # 小程序购物车
    ├── kitchen/                          # 后厨接单/划单
    ├── coupon/                           # 优惠券模板、发放任务、用户领券（含 mq/ 子目录）
    ├── member/                           # 会员、积分、结算联动
    ├── mq/                               # 可靠消息持久化（ReliableMessageServiceImpl）
    ├── report/                           # 报表导出
    ├── review/                           # 菜品评价
    ├── banner/                           # 首页轮播
    ├── audit/                            # 审计日志
    ├── feedback/                         # 反馈
    └── print/                            # 打印任务
```

## API 设计约定

- 管理端 Controller → `Admin*Controller`，映射 `/admin/*`
- 小程序端 Controller → `App*Controller`，映射 `/app/*`
- 支付回调 → `PaymentCallbackController`，映射 `/wx/pay/**`（`SaTokenConfig.java PUBLIC_PATHS` 放行）
- 统一响应：所有接口返回 `Result<T>`（`code + message + data`），错误码定义在 `ResultCode` 枚举
- 业务异常：`throw new BusinessException(ResultCode.XXX)` → `GlobalExceptionHandler` 统一转换为 `Result.fail()`
- 分页：`IPage<T>` → `PageResult<T>.of(page)`
- `SaTokenConfig` 路由鉴权策略详见类内注释

## 持久化规则

- 实体继承 `BaseEntity`：自动获得 `id`（雪花算法 ASSIGN_ID）、`createBy/createTime/updateBy/updateTime`、`deleted`（逻辑删除，`@TableLogic`，0 未删除 / 1 已删除）
- `MetaObjectHandlerImpl` 自动从 `StpUtil.getLoginIdAsLong()` 填充审计字段（管理端登录态）
- Mapper XML 扫描路径：`classpath*:/mapper/**/*.xml`（`application.yml` 第 57 行），按模块分目录：`src/main/resources/mapper/{system,coupon,member,mq}/`
- 数据库主键：`id-type: assign_id`，下划线自动转驼峰
- 分页最大限制：500 条（`MybatisPlusConfig.java` 第 26 行）
- **没有 H2 测试数据库使用**：`pom.xml` 声明了 H2 依赖，但 `application-test.yml` 仍指向 MySQL（见"集成测试"小节）

## 鉴权与安全（Sa-Token）

- 入口：`SaTokenConfig.java` — 注册单一 `SaInterceptor`，`PUBLIC_PATHS` 数组定义公开接口（登录/注册、小程序浏览、支付回调、WebSocket、接口文档）
- 权限/角色加载：`StpInterfaceImpl.java`
- 默认角色-权限映射：`RolePermissionDefaults.java`
- 密码使用 BCrypt（`hutool-all`）哈希
- **项目没有 checkstyle / PMD 插件**，代码风格依赖人工审查

## RocketMQ 与 WebSocket

- **RocketMQ**：用于优惠券发放任务的可靠投递（`application.yml` 第 18-36 行，`coupon.grant.mq.*`），`modules/mq/` 独立模块管理消息持久化和重试
- **WebSocket**：通过 Redis Pub/Sub 中转实现多实例部署（非直连 STOMP），`WsService` 发布消息到 `ws:channel:*` → `RedisMessageSubscriber` 广播到客户端。事件类型枚举：`WsEventType`

## 热点业务路径

| 路径 | 关键类 | 说明 |
|------|--------|------|
| 订单交易主链 | `OrderServiceImpl`, `PaymentServiceImpl`, `KitchenServiceImpl` | 扫码→下单→支付→后厨→结台 |
| 支付 | `PaymentServiceImpl`, `PaymentCallbackController`, `PaymentReconciliationTask` | 支持微信支付、支付宝、现金、AA 制、分账 |
| 会员结算联动 | `MemberSettlementServiceImpl` | 支付后触发积分/等级变更 |
| 优惠券发放 | 客户发放: `CouponServiceImpl`; 消费后赠券: `UserActionNotifyMQListener`; 定时回收过期券: `CouponExpireTask` | 全手工配平优惠券金额 |
| RBAC 鉴权 | `SaTokenConfig`, `StpInterfaceImpl`, `SysUser` → `SysRole` → `SysMenu` | 管理端权限控制 |

## 集成测试

- 唯一集成测试：`src/test/java/com/scaffold/integration/DineInFlowIntegrationTest.java` — **这是理解业务全流程的最佳入口**
- 通过 `@SpringBootTest(classes = DiancanAdminApplication.class)` + `@ActiveProfiles("test")` 全链路启动
- 使用 `MockedStatic<StpUtil>` 注入测试用户登录态，不需要真实登录
- 依赖外部 MySQL（`application-test.yml` 指向 `diancan-system` 表），**不使用 H2 内存库**
- `PasswordTest.java` 是独立的 BCrypt 密码工具（`main` 方法，非测试用例）
- 覆盖率工件空缺 — 当前只有一个大流程测试

## 已验证命令

```bash
# 在 diancan-admin/ 目录执行
mvn clean compile        # 编译
mvn spring-boot:run      # 启动（默认 dev profile, 端口 8080, 上下文 /api）
mvn test                 # 运行集成测试（需要 MySQL）
```

## 反模式与坑

- **不要假设 H2 可用**：测试走的是 MySQL，`application-test.yml` 需要真实 MySQL 连接
- **不要直接操作 Session**：用 `SessionUtils` 和 `StpUtil`，不要手动读写 SaToken Session
- **不要忘记 Mapper XML**：自定义 SQL 在 `src/main/resources/mapper/` 下，MyBatis-Plus 的方法不需要 XML
- **不要修改审计字段**：`createBy/createTime/updateBy/updateTime/deleted` 由 `BaseEntity` + `MetaObjectHandlerImpl` 统一管理
- **不要暴露内部异常**：`GlobalExceptionHandler` 的兜底 `Exception` handler 返回"系统繁忙"，不泄露堆栈
- **不要假设 WebSocket 直连**：消息走 Redis Pub/Sub 中转，需要 Redis 运行中
- **不要在生产环境开启 SQL 日志**：`application.yml` 中 `mybatis-plus.configuration.log-impl: StdOutImpl` 仅适合开发
- **提交前不要暴露 credentials**：配置文件中有数据库、Redis、MinIO、微信支付的占位符，真机值走环境变量
