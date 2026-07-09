# 点餐系统

![logo](./diancan-admin-web/public/favicon.svg)

> 一个包含后端、管理端、微信小程序端的点餐业务系统。

![JDK](https://img.shields.io/badge/JDK-17-3776AB)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3-42B883)
![Vite](https://img.shields.io/badge/Vite-7-646CFF)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)
![Redis](https://img.shields.io/badge/Redis-6%2F7-DC382D)
![License](https://img.shields.io/badge/Status-Local%20Project-orange)

## 项目亮点

| 模块化 | 三端协同 | 实时消息 | 业务闭环 |
| --- | --- | --- | --- |
| 后端按业务域拆分，结构清晰 | 后端 + 管理端 + 小程序端完整联动 | 支持 WebSocket / STOMP 实时推送 | 覆盖点餐、订单、会员、优惠券、评价等场景 |

## 快速导航

| 我想做什么 | 建议先看 |
| --- | --- |
| 快速了解项目组成 | [项目简介](#项目简介) / [项目结构](#项目结构) |
| 准备本地环境 | [环境要求](#环境要求) |
| 直接启动项目 | [快速开始](#快速开始) / [本地运行步骤](#本地运行步骤) |
| 看三个端分别做什么 | [三端说明](#三端说明) |
| 排查启动问题 | [注意事项](#注意事项) / [常见问题](#常见问题) |

## 项目概览

点餐系统面向门店堂食场景，核心目标是把顾客自助点餐、门店履约处理、会员营销沉淀放到一套三端协同体系里完成。

| 维度 | 概览 |
| --- | --- |
| 系统定位 | 一个围绕“扫码入桌 -> 点餐下单 -> 后厨履约 -> 支付结算 -> 会员沉淀 -> 评价复盘”构建的门店点餐系统 |
| 终端组成 | `diancan-miniapp` 顾客端小程序、`diancan-admin-web` 门店管理端、`diancan-admin` 统一业务后端 |
| 使用角色 | 顾客、服务员、收银员、后厨、店长、运营 |
| 核心能力 | 菜品与桌台管理、购物车与订单、支付、后厨与打印、会员积分成长、优惠券营销、评价反馈、经营报表 |
| 架构形态 | 当前仓库是真实的三端协同项目，后端采用 Spring Boot 单体应用按业务域模块化组织，而不是演示型单页项目 |

| 三端协作 | 作用 |
| --- | --- |
| 小程序端 | 承接顾客扫码入桌、浏览菜单、加购、下单、支付、查单、会员、优惠券、评价反馈 |
| 管理端 | 承接桌区桌台、下单台、收银台、订单处理、后厨处理、打印机、会员营销、评价报表 |
| 后端服务 | 统一提供认证、交易、会员、营销、消息、报表和中间件集成能力 |

## 目录

- [项目简介](#项目简介)
- [项目结构](#项目结构)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
- [系统架构](#系统架构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [本地运行前需要确认的配置](#本地运行前需要确认的配置)
- [三端说明](#三端说明)
- [本地运行步骤](#本地运行步骤)
- [启动顺序建议](#启动顺序建议)
- [注意事项](#注意事项)
- [常见问题](#常见问题)
- [补充说明](#补充说明)

## 项目简介

点餐系统是一个包含后端、管理端、小程序端的完整项目，主要面向门店点餐、桌台管理、订单处理、会员运营等业务场景。

当前仓库包含 3 个子项目：

| 模块 | 目录 | 说明 |
| --- | --- | --- |
| 后端服务 | `diancan-admin` | Spring Boot 3 后端，负责认证、菜品、桌台、订单、支付、会员、优惠券、打印、评价、报表等业务 |
| 管理端 | `diancan-admin-web` | Vue 3 管理后台，面向运营、服务员、收银、后厨等角色 |
| 微信小程序端 | `diancan-miniapp` | 顾客端小程序，提供点餐、购物车、订单、会员、优惠券、评价、反馈等能力 |

## 管理端页面截图

### 管理端登录页

![管理端登录页](./docs/images/admin-login.png)



### 桌台看板页

![桌台看板页](./docs/images/admin-zhuotai.png)

### 管理端主题设置页

![管理端主题设置页](./docs/images/admin-theme.png)

## 管理端完整菜单截图

以下截图均为系统主体内容区截图，已去掉浏览器地址栏、系统侧栏与顶部菜单，只保留当前菜单对应的页面内容。

### 首页

![首页](./docs/images/admin-menus/35-home.png)

### 菜品管理

| 分类管理 | 菜品列表 |
| --- | --- |
| ![分类管理](./docs/images/admin-menus/01-dish_category.png) | ![菜品列表](./docs/images/admin-menus/02-dish_list.png) |

### 桌台管理

| 桌台列表 | 区域管理 |
| --- | --- |
| ![桌台列表](./docs/images/admin-menus/03-table_manage.png) | ![区域管理](./docs/images/admin-menus/04-table_area.png) |

### 订单中心

| 订单列表 | 支付管理 |
| --- | --- |
| ![订单列表](./docs/images/admin-menus/05-order_list.png) | ![支付管理](./docs/images/admin-menus/06-device_payment.png) |

| 评价管理 | 反馈管理 |
| --- | --- |
| ![评价管理](./docs/images/admin-menus/07-device_review.png) | ![反馈管理](./docs/images/admin-menus/08-device_feedback.png) |

### 数据报表

| 营业额统计 | 菜品排行 | 翻台率 |
| --- | --- | --- |
| ![营业额统计](./docs/images/admin-menus/09-report_revenue.png) | ![菜品排行](./docs/images/admin-menus/10-report_dish-ranking.png) | ![翻台率](./docs/images/admin-menus/11-report_table-turnover.png) |

### 设备与日志

| 打印机管理 | 审计日志 |
| --- | --- |
| ![打印机管理](./docs/images/admin-menus/12-device_printer.png) | ![审计日志](./docs/images/admin-menus/13-device_audit-log.png) |

### 服务收银

| 桌台看板 | 服务员点单 |
| --- | --- |
| ![桌台看板](./docs/images/admin-menus/14-service_table-board.png) | ![服务员点单](./docs/images/admin-menus/15-service_place-order.png) |

| 订单操作 | 结账 |
| --- | --- |
| ![订单操作](./docs/images/admin-menus/16-service_order-ops.png) | ![结账](./docs/images/admin-menus/17-service_checkout.png) |

| 后厨 |
| --- |
| ![后厨](./docs/images/admin-menus/18-service_kitchen.png) |

### 营销中心

| 优惠券管理 | 轮播图管理 |
| --- | --- |
| ![优惠券管理](./docs/images/admin-menus/19-marketing_coupon.png) | ![轮播图管理](./docs/images/admin-menus/20-marketing_banner.png) |

| 会员列表 | 会员等级 |
| --- | --- |
| ![会员列表](./docs/images/admin-menus/21-marketing_member.png) | ![会员等级](./docs/images/admin-menus/22-marketing_member-level.png) |

| 积分流水 | 成长流水 |
| --- | --- |
| ![积分流水](./docs/images/admin-menus/23-marketing_member-points-record.png) | ![成长流水](./docs/images/admin-menus/24-marketing_member-growth-record.png) |

### 消息监控

![消息管理](./docs/images/admin-menus/25-monitor_message.png)

### 系统管理

| 用户管理 | 角色管理 |
| --- | --- |
| ![用户管理](./docs/images/admin-menus/26-manage_user.png) | ![角色管理](./docs/images/admin-menus/27-manage_role.png) |

| 菜单管理 | 字典管理 |
| --- | --- |
| ![菜单管理](./docs/images/admin-menus/28-manage_menu.png) | ![字典管理](./docs/images/admin-menus/29-manage_dict.png) |

| 配置管理 | 主题设置 |
| --- | --- |
| ![配置管理](./docs/images/admin-menus/30-manage_config.png) | ![主题设置](./docs/images/admin-menus/31-manage_theme.png) |

| 会员用户管理 |
| --- |
| ![会员用户管理](./docs/images/admin-menus/32-manage_member-user.png) |

### 日志管理

| 登录日志 | 操作日志 |
| --- | --- |
| ![登录日志](./docs/images/admin-menus/33-log_login.png) | ![操作日志](./docs/images/admin-menus/34-log_operation.png) |

## 小程序页面截图

<p align="center">
  <img src="./docs/images/xiaochengxu-shouye.png" alt="小程序首页" width="24%">
  <img src="./docs/images/xiaochengxu-diancan.png" alt="小程序点餐页" width="24%">
  <img src="./docs/images/xiaochengxu-dingdan.png" alt="小程序订单页" width="24%">
  <img src="./docs/images/xiaochengxu-wode.png" alt="小程序我的页" width="24%">
</p>

## 三端一览

| 端 | 目录 | 面向对象 | 主要职责 |
| --- | --- | --- | --- |
| 后端服务 | `diancan-admin` | 系统核心服务 | 提供业务接口、权限认证、订单流转、会员体系、消息推送 |
| 管理端 | `diancan-admin-web` | 门店后台人员 | 管理菜品、桌台、订单、营销、设备、日志、报表 |
| 微信小程序端 | `diancan-miniapp` | 顾客 | 承载扫码点餐、购物车、订单、会员、优惠券、评价、反馈 |

## 内置能力

基于当前仓库代码结构，项目已包含以下核心能力：

- 后台账号登录、角色、菜单、日志管理
- 菜品分类、菜品、规格管理
- 桌区、桌台、桌台二维码管理
- 购物车、下单、加菜、催菜、订单处理
- 微信支付相关后端能力
- 会员、积分、成长、权益体系
- 优惠券模板、发券任务、用户优惠券
- 打印机管理
- 评价、反馈
- 经营报表
- WebSocket 实时消息推送
- RocketMQ 可靠消息处理

## 适用场景

当前仓库更适合以下场景使用：

- 本地点餐系统课程设计 / 毕设项目参考
- 门店业务系统原型搭建
- 三端协同项目结构学习
- Spring Boot + Vue 3 + 微信小程序综合项目实践
- 订单、会员、营销一体化业务流梳理

## 项目结构

```text
diancan-system
├─ db
│  └─ diancan-system.sql
├─ diancan-admin
│  ├─ pom.xml
│  └─ src
├─ diancan-admin-web
│  ├─ package.json
│  ├─ vite.config.ts
│  ├─ .env
│  ├─ .env.dev
│  ├─ .env.test
│  ├─ .env.prod
│  └─ src
└─ diancan-miniapp
   ├─ app.json
   ├─ project.config.json
   ├─ config
   ├─ api
   ├─ pages
   └─ components
```

## 技术栈

### 后端

- JDK 17
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- Sa-Token 1.37.0
- MySQL 8
- Redis
- RocketMQ
- MinIO
- WebSocket + STOMP + SockJS
- Knife4j / OpenAPI

### 管理端

- Node.js `>= 20.19.0`
- pnpm `>= 10.5.0`
- Vue 3
- Vite 7
- TypeScript
- Naive UI
- UnoCSS
- Pinia
- ECharts

### 小程序端

- 微信开发者工具
- 原生微信小程序

## 功能模块

### 后端模块

根据 `diancan-admin/src/main/java/com/scaffold/modules` 目录，当前已包含以下业务模块：

- `audit`：审计日志导出
- `banner`：轮播图
- `cart`：购物车
- `coupon`：优惠券与发券任务
- `dish`：菜品、分类、规格
- `feedback`：用户反馈
- `kitchen`：后厨业务
- `member`：会员、积分、成长、权益
- `mq`：可靠消息
- `order`：订单业务
- `payment`：支付业务
- `print`：打印机管理
- `report`：报表统计
- `review`：评价业务
- `system`：系统管理、认证、角色、菜单、日志
- `table`：桌台与桌区

### 管理端页面

根据 `diancan-admin-web/src/views` 目录，当前主要页面分组包括：

- `home`
- `dish`
- `table`
- `order`
- `service`
- `device`
- `marketing`
- `report`
- `manage`
- `log`

### 小程序页面

根据 `diancan-miniapp/app.json`，当前小程序页面包括：

- 首页
- 桌台页
- 点餐页
- 购物车
- 订单页
- 支付页
- 结果页
- 我的
- 会员中心
- 积分记录
- 成长记录
- 优惠券
- 评价
- 我的评价
- 反馈

## 系统架构

下面这张图是参考 `docs/系统架构图.png` 的层次表达方式，结合当前点餐系统的真实实现重新绘制的，不再直接套用数据中台的术语。

![点餐系统架构图](./docs/images/ordering-system-architecture.png)

图中的分层和本项目代码的对应关系如下：

| 架构层 | 在本项目中的落地 |
| --- | --- |
| 业务应用层 | 顾客小程序、门店管理端、服务收银页面、经营报表与营销运营页面 |
| 统一业务服务体系 | `diancan-admin` 作为 Spring Boot 单体后端，对外提供认证鉴权、REST API、WebSocket 推送 |
| 核心交易服务 | `dish`、`table`、`cart`、`order`、`payment` 等交易主链路模块 |
| 会员营销服务 | `member`、`coupon`、`banner`、`review`、`feedback` 等会员与营销相关模块 |
| 履约运营服务 | `kitchen`、`print`、`report`、`audit` 以及订单操作相关能力 |
| 系统治理能力 | `system` 模块下的用户、角色、菜单、字典、配置，以及登录/操作日志能力 |
| 集成支撑能力 | RocketMQ、MinIO、微信小程序、微信支付等外部依赖与支撑组件 |
| 基础设施 / 存储层 | MySQL、Redis、RocketMQ、MinIO 是当前仓库实际依赖的底层设施 |

## 页面与业务关系

这一部分不再讲技术层次，而是把上面的系统架构落到实际业务链路里，说明顾客端页面、门店端页面、后端模块分别参与哪一段业务。

```mermaid
flowchart LR
    A[扫码入桌] --> B[菜单浏览 / 购物车]
    B --> C[提交订单 / 支付结算]
    C --> D[接单 / 后厨 / 打印 / 出餐]
    D --> E[订单完成 / 评价反馈]
    C -.营销激励.-> F[会员 / 积分 / 优惠券]
    E -.数据沉淀.-> G[报表分析 / 运营复盘]
```

| 业务阶段 | 顾客端页面 | 管理端页面 | 支撑后端模块 |
| --- | --- | --- | --- |
| 入桌与引流 | `pages/index`、`pages/table` | `table/area`、`table/manage` | `table`、`banner` |
| 点餐与交易 | `pages/menu`、`pages/cart`、`pages/payment` | `service/place-order`、`service/order-ops`、`service/checkout`、`order/list` | `dish`、`cart`、`order`、`payment` |
| 履约与出餐 | 顾客侧主要感知订单状态 | `service/kitchen`、`service/table-board`、`device/printer` | `kitchen`、`print`、`mq` |
| 会员与营销 | `pages/member`、`pages/member-points`、`pages/member-growth`、`pages/coupon`、`pages/profile` | `marketing/member`、`marketing/member-level`、`marketing/coupon`、`marketing/banner` | `member`、`coupon`、`banner` |
| 评价与复盘 | `pages/review`、`pages/my-review`、`pages/feedback`、`pages/order` | `device/review`、`device/feedback`、`report/revenue`、`report/dish-ranking`、`report/table-turnover` | `review`、`feedback`、`report`、`audit` |

## 环境要求

如果需要完整跑起三个端，建议准备以下环境：

| 环境 | 版本要求 | 是否必须 | 说明 |
| --- | --- | --- | --- |
| JDK | 17 | 是 | 后端运行必须 |
| Maven | 3.9+ | 是 | 后端构建与启动 |
| MySQL | 8.x | 是 | 业务数据库 |
| Redis | 6.x / 7.x | 是 | 登录态、缓存、消息转发 |
| Node.js | 20.19+ | 管理端必须 | 管理端运行 |
| pnpm | 10.5+ | 管理端必须 | 管理端依赖安装 |
| 微信开发者工具 | 最新稳定版 | 小程序必须 | 小程序调试 |
| RocketMQ | 5.x | 建议 | 当前项目包含 RocketMQ 监听器 |
| MinIO | 8.x+ | 可选 | 菜品图片等文件能力依赖 |

## 快速开始

如果你只想先把项目跑起来，建议按下面步骤执行：

> 推荐路径：先跑通 `后端 + 管理端`，确认基础链路可用后，再联调 `微信小程序端`。

### 1. 准备基础环境

- 安装 JDK 17
- 安装 Maven 3.9+
- 安装 MySQL 8
- 安装 Redis
- 安装 Node.js 20.19+
- 安装 pnpm 10.5+
- 安装微信开发者工具

### 2. 导入数据库

执行 `db/diancan-system.sql`，并确认后端配置的数据库名与实际一致。

### 3. 启动后端

```bash
cd diancan-admin
mvn clean compile
mvn spring-boot:run
```

### 4. 启动管理端

```bash
cd diancan-admin-web
pnpm install
pnpm dev
```

### 5. 启动小程序端

- 使用微信开发者工具导入 `diancan-miniapp`
- 修改 `config/env.js` 为你本机的局域网地址
- 编译运行

### 6. 验证访问

- 后端接口文档：`http://localhost:8080/api/swagger-ui.html`
- 管理端：`http://localhost:9527`

### 7. 默认尝试账号

管理端登录页当前默认预填：

- 用户名：`admin`
- 密码：`123456`

## 推荐阅读顺序

如果你是第一次接手这个仓库，建议按这个顺序阅读和启动：

1. 先看“项目结构”，了解三端分别在哪
2. 再看“环境要求”，准备基础依赖
3. 接着看“本地运行前需要确认的配置”，先把数据库、Redis、端口统一
4. 然后看“本地运行步骤”，先跑后端，再跑管理端
5. 最后再联调小程序端与微信相关能力

## 本地运行前需要确认的配置

### 1. 数据库

SQL 文件位于：

```text
db/diancan-system.sql
```

需要注意：

- `application-dev.yml` 中开发库名写的是 `diancan-system`
- SQL 文件头部导出源库名显示的是 `digital_ordering_system`

也就是说，当前仓库里数据库命名存在不一致。

建议你本地统一成一个名字后再启动，避免导入成功但配置连错库。比如统一为：

```text
digital_ordering_system
```

然后同步修改后端 `application-dev.yml` 的 JDBC 地址。

### 2. 后端配置

后端配置文件位于：

- `diancan-admin/src/main/resources/application.yml`
- `diancan-admin/src/main/resources/application-dev.yml`
- `diancan-admin/src/main/resources/application-test.yml`
- `diancan-admin/src/main/resources/application-prod.yml`

开发环境默认配置重点如下：

### 服务配置

- 端口：`8080`
- 上下文路径：`/api`

接口基础地址：

```text
http://localhost:8080/api
```

接口文档地址：

```text
http://localhost:8080/api/swagger-ui.html
```

### MySQL

默认开发配置为：

- host：`localhost`
- port：`3306`
- username：`root`
- password：`123456`

### Redis

默认开发配置为：

- host：`localhost`
- port：`6379`
- password：`123456`
- database：`0`

如果你的本地 Redis 没有密码，需要自行调整配置。

### RocketMQ

默认配置：

- NameServer：`127.0.0.1:9876`

当前项目中存在固定的 RocketMQ 消费监听器，因此如果你要完整联调优惠券、可靠消息等功能，建议本地直接准备 RocketMQ 环境。

### MinIO

开发环境默认配置为：

- endpoint：`http://127.0.0.1:9000`
- access-key：`minioadmin`
- secret-key：`minioadmin`
- bucket：`dish-images`

如果你暂时不使用文件上传能力，可以关闭 MinIO 相关配置。

### 微信小程序与微信支付

后端开发配置中已经包含：

- 小程序配置
- 微信支付配置

这部分能力依赖真实微信环境。如果你只做本地基础开发，不一定需要先打通。

### 3. 管理端配置

管理端目录：

```text
diancan-admin-web
```

关键文件：

- `package.json`
- `.env`
- `.env.dev`
- `.env.test`
- `.env.prod`
- `vite.config.ts`

开发环境默认配置重点如下：

- `VITE_SERVICE_BASE_URL=http://localhost:8080/api`
- `VITE_HTTP_PROXY=Y`
- Vite 本地端口：`9527`

管理端本地访问地址：

```text
http://localhost:9527
```

登录页当前默认预填：

- 用户名：`admin`
- 密码：`123456`

同时数据库初始化数据中也存在后台账号：

- `admin`
- `waiter01`
- `waiter02`
- `cashier01`
- `chef01`
- `chef02`

### 4. 小程序端配置

小程序目录：

```text
diancan-miniapp
```

关键文件：

- `project.config.json`
- `app.json`
- `config/env.js`

当前 `config/env.js` 中已经写了一个局域网地址示例，实际本地联调时需要改成你自己电脑可被手机访问的 IP。

例如：

```js
apiHost: 'http://192.168.x.x:8080'
```

注意：

- 小程序联调**不要使用** `localhost`
- 小程序登录接口依赖微信 `code` 和 `phoneCode`
- 小程序支付能力依赖真实微信环境

## 三端说明

### 1. 后端 `diancan-admin`

后端是整个系统的核心服务，负责：

- 认证与权限
- 菜品、分类、规格
- 桌台、桌区、二维码
- 购物车、订单、催菜、加菜
- 支付、退款、支付状态
- 会员、积分、成长、权益
- 优惠券、发券任务
- 打印机与消息推送
- 报表、评价、反馈、日志

默认启动信息：

- 端口：`8080`
- 上下文：`/api`
- 文档地址：`http://localhost:8080/api/swagger-ui.html`

核心价值：

- 统一承载业务逻辑
- 提供后台与小程序两侧接口
- 负责消息推送、会员体系、订单状态流转

适合先启动它的原因：

- 所有前端都依赖后端接口
- 数据库、Redis、MQ、文件存储配置问题都会先在这里暴露

### 2. 管理端 `diancan-admin-web`

管理端主要用于门店后台运营，当前代码中已覆盖：

- 首页概览
- 菜品管理
- 桌台管理
- 订单管理
- 服务台
- 设备管理
- 营销管理
- 报表统计
- 系统管理
- 日志管理

默认启动信息：

- 本地端口：`9527`
- 访问地址：`http://localhost:9527`

核心价值：

- 提供门店运营后台入口
- 承接菜品、桌台、订单、设备、营销等管理功能
- 作为实时消息处理与可视化操作端

适合优先联调的原因：

- 比小程序更容易调试
- 不依赖真实微信登录与支付环境

### 3. 小程序端 `diancan-miniapp`

小程序端主要面向顾客，当前包含：

- 首页
- 点餐
- 购物车
- 订单
- 支付
- 我的
- 会员中心
- 优惠券
- 评价
- 反馈

联调重点：

- 需要局域网 IP
- 需要微信开发者工具
- 部分功能依赖真实微信环境

核心价值：

- 面向顾客完成扫码点餐与订单操作
- 承接会员、优惠券、评价、反馈等用户侧功能

联调顺序建议：

- 先确认后端接口可用
- 再确认管理端基础功能可用
- 最后再处理小程序真机、微信登录、支付等问题

## 本地运行步骤

### 1. 导入数据库

先创建数据库，然后导入脚本：

```bash
mysql -uroot -p <你的数据库名> < db/diancan-system.sql
```

导入完成后，确认后端配置中的数据库名与实际一致。

### 2. 启动基础依赖

建议至少先启动：

1. MySQL
2. Redis

如果需要完整联调，再启动：

3. RocketMQ
4. MinIO

### 3. 启动后端

```bash
cd diancan-admin
mvn clean compile
mvn spring-boot:run
```

启动成功后可访问：

```text
http://localhost:8080/api/swagger-ui.html
```

### 4. 启动管理端

```bash
cd diancan-admin-web
pnpm install
pnpm dev
```

启动成功后访问：

```text
http://localhost:9527
```

### 5. 启动小程序

1. 打开微信开发者工具
2. 导入 `diancan-miniapp`
3. 修改 `config/env.js`
4. 编译运行

## 开发环境访问地址

| 模块 | 地址 |
| --- | --- |
| 后端接口根地址 | `http://localhost:8080/api` |
| 后端接口文档 | `http://localhost:8080/api/swagger-ui.html` |
| 管理端 | `http://localhost:9527` |
| 小程序接口前缀 | `http://<局域网IP>:8080/api/app` |

## 推荐启动路径

### 方式一：先熟悉项目

1. 导入数据库
2. 启动后端
3. 打开 Swagger
4. 启动管理端
5. 使用 `admin / 123456` 登录后台

### 方式二：完整三端联调

1. 准备 MySQL、Redis、RocketMQ、MinIO
2. 启动后端
3. 启动管理端
4. 修改小程序 `config/env.js`
5. 使用微信开发者工具运行小程序

## 当前仓库特点

从现有代码结构来看，这个仓库有几个比较明显的特点：

- 不是单页单体 demo，而是明确拆分成三端
- 后端业务模块比较完整，已经覆盖订单、会员、营销、支付、打印等领域
- 管理端页面分组较清晰，适合继续扩展
- 小程序端不是简单展示页，已经包含订单、会员、评价、反馈等页面
- 对中间件有一定依赖，适合做接近真实业务链路的本地联调

## 开发部署说明

开发环境建议按“中间件 -> 后端 -> 管理端 -> 小程序端”的顺序逐步启动。

推荐流程：

1. 先完成 MySQL、Redis 配置
2. 先启动后端，确认接口文档可访问
3. 再启动管理端，确认后台可登录
4. 最后再联调小程序、微信登录、支付等能力

## 生产部署说明

当前仓库中：

- 后端已提供 `application-prod.yml`
- 管理端已提供 `.env.prod`

但仓库中**没有提供完整的生产部署文档、脚本或演示环境地址**，因此本 README 仅说明当前已有的生产配置入口，不扩展未落地的信息。

如果后续需要补充生产部署文档，建议单独增加：

- Nginx 配置说明
- JAR 部署方式
- MySQL / Redis / RocketMQ / MinIO 生产配置说明
- 微信小程序与支付正式环境参数说明
- 日志、监控、备份策略说明

## 启动顺序建议

建议本地按以下顺序启动：

1. MySQL
2. Redis
3. RocketMQ
4. MinIO
5. `diancan-admin`
6. `diancan-admin-web`
7. `diancan-miniapp`

## 注意事项

### 1. 当前仓库没有演示地址

README 不提供演示地址说明，因为当前仓库中没有现成的在线演示环境信息。

### 2. 数据库名需要你自行统一

当前配置与 SQL 文件之间存在命名不一致的问题，启动前建议先统一。

### 3. 小程序不要直接使用 localhost

真机或开发者工具访问本地后端时，应使用你电脑的局域网 IP。

### 4. RocketMQ 建议本地准备

当前项目包含 RocketMQ 监听器，完整联调建议直接准备 RocketMQ 环境。

### 5. 微信能力不等于纯前端本地调试

以下能力通常需要真实微信环境支持：

- 手机号登录
- 微信支付
- 支付回调
- 小程序相关能力

## 常见问题

### 1. 数据库导入了但项目起不来

优先检查：

- 后端配置的数据库名是否和你实际创建的一致
- MySQL 用户名密码是否一致

### 2. Redis 连接失败

优先检查：

- 端口是否正确
- 是否配置了密码
- 后端配置是否和本地 Redis 一致

### 3. 管理端接口请求失败

优先检查：

- 后端是否已经启动
- `VITE_SERVICE_BASE_URL` 是否正确
- 本地是否存在端口占用

### 4. 小程序请求不到本地后端

优先检查：

- `config/env.js` 是否写成了 `localhost`
- 是否改成了你电脑局域网 IP
- 手机和电脑是否在同一网络

### 5. 与支付、手机号登录相关的功能跑不通

这类能力依赖真实微信环境，包括但不限于：

- 微信登录
- 手机号授权
- 微信支付
- 支付回调

如果只是本地熟悉项目，建议先以“后端 + 管理端 + 基础接口”跑通为主。

## 补充说明

当前 README 主要用于：

- 帮助首次接手项目的人快速了解仓库结构
- 说明三个端分别需要什么环境
- 说明本地如何把项目跑起来

如果后续你希望继续补充，我建议下一步可以再单独补这几块：

- 数据库表结构与核心业务表说明
- 后端接口分组说明
- 管理端菜单与权限说明
- 小程序联调注意事项
- Docker 本地环境方案
