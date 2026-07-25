# diancan-miniapp · 微信小程序子项目

> 基线：`diancan-system/master`，无根级编排器，使用微信开发者工具导入 `diancan-miniapp` 后编译。
> 跨端契约、启动顺序、共享风险在根 `AGENTS.md`；本文件只覆盖小程序栈内。

## 入口与全局

- `app.js`（22 行）：仅 `onLaunch` 调用 `restoreSession()`，已登录时尝试 `connectSocket()`。
- `app.json`（60 行）：15 个页面，4 项 tabBar（首页/点餐/订单/我的），`style:"v2"`，`lazyCodeLoading:"requiredComponents"`。
- `project.config.json`（41 行）：`appid` 在第 2 行，禁止复制到文档或日志；ES6 + postcss + minified。

## 配置（`config/env.js`，96 行）

所有运行时变量通过 `env` 对象只读暴露，调用方统一 `require('./config/env').env`。

| 字段 | 值 | 备注 |
| --- | --- | --- |
| `apiPrefix` | `/api/app` | 唯一前缀来源；`api/*.js` 写相对路径（如 `/order`），`request.js` 拼前缀 |
| `enableSocket` | `false` | 后端是 STOMP 端点，未适配小程序原生 ws，强制关闭 |
| `loginPath` | `/api/app/auth/phone-login` | 唯一特例：登录接口在 `auth.js` 用 `withPrefix:false` 直接走全路径 |
| `apiHost` | devtools→`127.0.0.1:8080`，真机→cpolar 隧道 | 优先级：storage 覆盖 > 平台判断 > 默认 |
| `wsUrl` | `ws://<apiHost>/ws` | 当前不可用，随 `enableSocket` 失效 |

Host 切换：开发期写 `wx.setStorageSync('diancan.devApiHost','http://192.168.x.x:8080')` 即可临时覆盖真机地址，键定义在 `DEV_API_HOST_STORAGE_KEY`。

## 工具层（`utils/`）

- `request.js`（79）：`wx.request` 封装，10s 超时，错误归一化为 `Error`；`request` 默认返回 `payload.data`，`requestRaw` 返回完整 payload。`Authorization: <token>` 取自 `KEYS.TOKEN`。
- `storage.js`（71）：`KEYS` 是 storage key 字符串的唯一来源（`appToken` / `currentTable` / `mockPaidOrderIds` 等共 9 项）。`setCurrentTable` 在桌号身份变化时清空 `ORDER_ID` / `ORDERED_DISH_IDS` / `PERSON_COUNT`。
- `auth.js`（63）：`wxLogin()` 拿临时 `code`；`phoneLogin(code, phoneCode)` 调 `env.loginPath`（`withPrefix:false`），回填 `KEYS.TOKEN`。
- `table-binding.js`（96）：`bindTableByCode` 是入桌主入口，根据后端 `status`（0 空闲 / 1 在用 / 2,3 收尾）决定 `entryMode`：`open` / `resume` / `join`；登录态下走 `ensureCurrentUserTableBinding` 走 `bindCurrentUser`。
- `socket.js`（42）：仅当 `env.enableSocket=true` 时连接，提供 `addSocketListener` 订阅。默认关闭，不要在页面里直接 `wx.connectSocket`。
- `debounce.js`（11）、`format.js`（17）：通用小工具。

## 接口封装（`api/`）

10 个文件，**全部使用相对路径**，由 `request.js` 加 `apiPrefix`：

```
api/banner.js    api/cart.js     api/coupon.js   api/dish.js
api/feedback.js  api/member.js   api/order.js    api/payment.js
api/review.js    api/table.js
```

样例：`api/order.js` 暴露 `/order`、`/order/:id/add-item`、`/order/:orderId/rush/:itemId`、`/order/:id`、`/order/table/:tableId`；`api/cart.js` 用 `params: { tableId }` 走 query 串。修改契约时同步改三端：Controller、此处、调用页面。

## 页面与组件

- 页面 15 个，按业务分组在 `pages/<domain>/index.{js,wxml,wxss,json}`：
  - 交易主链：`index`、`table`、`menu`、`cart`、`order`、`payment`、`result`
  - 会员营销：`member`、`member-points`、`member-growth`、`coupon`
  - 评价：`review`、`my-review`、`feedback`
  - 个人：`profile`
- 组件 5 个（`components/`）：`app-navbar`（自定义导航栏，全局替代默认 navbar）、`dish-card`、`cart-bar`、`star-rating`、`empty-state`。
- `app-navbar` 的存在意味着 `window.navigationStyle` 在各页面 `json` 里通常为 `"custom"`，新增页面要显式声明。

## 运行主链

```
pages/table (扫码/输入桌号)
  └─ utils/table-binding.bindTableByCode → api/table.getTableByCode / openTable / bindCurrentUser
        └─ pages/menu (扫台后跳转)
              └─ api/dish + api/cart 读写
                    └─ pages/cart → api/order.createOrder → pages/payment
                          └─ api/payment.wechat / getPaymentStatus
                                └─ pages/result → pages/review (KEYS.REVIEWED_ORDER_IDS 防止重复)
```

`globalData` 只放 `user` / `table` / `currentOrderId` 三个引用，业务数据走 storage 或接口。`KEYS.MOCK_PAID_ORDER_IDS` 用于在没有真实微信支付回调时模拟已付订单（开发调试用）。

## 约定

- 无 npm、无 `package.json`、无构建；CommonJS `require` 即可。
- storage key 字符串只允许出现在 `utils/storage.js` 的 `KEYS` 对象里，其它地方用 `KEYS.X` 引用。
- 所有 HTTP 必须经 `utils/request.js`，不要在页面里直接 `wx.request`。
- 接口路径写相对路径（不带 `/api/app`），前缀唯一来源是 `env.apiPrefix`。
- 调试地址：开发工具走 `127.0.0.1:8080`；真机先确认电脑可达地址，或通过 `wx.setStorageSync('diancan.devApiHost', ...)` 临时覆盖。
- 真机不要访问 `127.0.0.1` / `localhost`。

## 热点

- `pages/menu/index.js`（960 行）、`pages/menu/index.wxss`（1444 行）：菜单页是体量最大的页面，分类/菜品/购物车联动都集中在这两个文件，改动前先确认对 `cart-bar` 组件事件的影响。
- 无测试栈（无 jest / miniprogram-automator），修改后用微信开发者工具真机预览验收。

## 禁止

- 不要把 `appid` 或 `appSecret` 写进代码、文档、注释或 commit message。
- 不要在 `api/*.js` 里写 `/api/app` 前缀，前缀统一由 `request.js` 拼。
- 不要在页面里直接 `wx.request` / `wx.connectSocket` / `wx.setStorageSync('appToken', ...)`，分别走 `request.js` / `socket.js` / `storage.js`。
- 不要改 `env.js` 里的 `enableSocket` 强行启用 ws，STOMP 不匹配会反复断连。
