# 末端驿站包裹管理系统 — 设计文档

> **版本**: v1.0 | **日期**: 2026-06-20 | **基于**: DDD 六边形架构技术标准 v2.0

---

## 目录

1. [需求概述](#1-需求概述)
2. [系统架构](#2-系统架构)
3. [数据模型 & 数据库设计](#3-数据模型--数据库设计)
4. [API 接口设计](#4-api-接口设计)
5. [前端设计](#5-前端设计)
6. [后端模块设计](#6-后端模块设计)
7. [测试用例设计](#7-测试用例设计)
8. [部署架构](#8-部署架构)

---

## 1. 需求概述

### 1.1 业务背景

在快递物流的"最后一公里"环节，末端驿站（菜鸟驿站、丰巢等）承担着包裹的接收、存放和交付工作。本系统为末端驿站提供轻量级包裹管理能力，提高操作效率。

### 1.2 功能需求

| 编号 | 功能 | 描述 |
|------|------|------|
| F1 | 包裹入库 | 录入运单号、收件人手机号、快递公司、货架位置 |
| F2 | 包裹查询与取件 | 按手机号查询待取件包裹，确认取件后变更状态并记录取件时间 |
| F3 | 滞留标识 | 入库超 48 小时的包裹在列表中以红色背景/警告标签突出显示 |
| F4 | 编辑入库信息 | 对处于待取件状态的包裹，支持修改运单号、快递公司、货架位置、手机号 |
| F5 | 统计概览 | 仪表盘展示今日入库数、待取件总数、滞留数、今日取件数 |
| F6 | 登录认证 | 简单 JWT 登录，单管理员账号 |

### 1.3 技术栈

| 层 | 技术 | 版本 |
|-----|------|------|
| 后端框架 | Spring Boot | 2.7.12 |
| 语言 | Java | 8 |
| ORM | MyBatis | 2.1.4 |
| 数据库 | MySQL | 8.x |
| 构建 | Maven | 3.x |
| 前端框架 | Vue 3 (Composition API) + TypeScript | 3.x |
| 构建工具 | Vite | 5.x |
| UI 组件库 | Element Plus | 2.x |
| HTTP 客户端 | axios | 1.x |
| 认证 | JWT (jjwt) | 0.9.1 |

---

## 2. 系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────┐
│                    Frontend                      │
│              Vue 3 + Vite + Element Plus         │
│                  (nginx 部署)                     │
└─────────────────────┬───────────────────────────┘
                      │ REST API (JSON)
                      │ JWT Bearer Token
┌─────────────────────▼───────────────────────────┐
│               Spring Boot (app)                  │
│                                                    │
│  ┌──────────┐  ┌────────────┐  ┌──────────────┐ │
│  │ trigger  │  │   domain   │  │infrastructure│ │
│  │ (入站)    │  │  (领域核心) │  │  (出站)       │ │
│  │          │  │            │  │              │ │
│  │Package   │  │ package    │  │ Repository   │ │
│  │Controller│──│ Entity     │──│ Impl + DAO   │ │
│  │Auth      │  │ Service    │  │ MyBatis      │ │
│  │Controller│  │ Repository │  │ MySQL        │ │
│  └──────────┘  └────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────┘
```

### 2.2 模块依赖关系

```
app ──→ trigger ──→ domain ←── infrastructure
  │        │                    │
  └────────┼────────────────────┘
           ▼
       types, api
```

### 2.3 各模块职责

| 模块 | 职责 | 涉及内容 |
|------|------|----------|
| `packageManager-types` | 共享内核 | 错误码枚举 `ResponseCode`、业务异常 `AppException`、常量 |
| `packageManager-api` | 接口契约 | 外部 API 接口 `IPackageManageService`、Request/Response DTO、统一 `Response` |
| `packageManager-domain` | 领域核心 | `package` 限界上下文：Entity、Enum、Repository 接口、领域服务 |
| `packageManager-infrastructure` | 出站适配器 | Repository 实现、MyBatis DAO、PO、数据库映射 |
| `packageManager-trigger` | 入站适配器 | `PackageController`、`AuthController` |
| `packageManager-app` | 启动引导 | Spring Boot 启动类、全局配置 |

### 2.4 限界上下文 `package` 内部结构

```
domain/package/
├── adapter/
│   └── repository/
│       └── IPackageRepository.java      ← 仓储接口（只接受/返回领域对象）
├── model/
│   ├── aggregate/
│   │   └── PackageAggregate.java        ← 聚合根
│   ├── entity/
│   │   └── PackageEntity.java           ← 领域实体
│   └── valobj/
│       ├── PackageStatusEnum.java        ← 状态枚举
│       └── CourierCompanyEnum.java       ← 快递公司枚举
└── service/
    ├── IPackageService.java             ← 领域服务接口
    └── PackageService.java              ← 领域服务实现（无事务注解）
```

### 2.5 基础设施对应结构

```
infrastructure/
├── adapter/
│   └── repository/
│       └── PackageRepository.java       ← 仓储实现（@Repository, @Transactional）
├── dao/
│   ├── IPackageDao.java                ← MyBatis Mapper 接口
│   └── po/
│       └── PackagePO.java              ← 持久化对象（1:1 对应 package 表）
```

### 2.6 入站适配器

```
trigger/
└── http/
    ├── AuthController.java              ← 登录端点
    └── PackageController.java           ← 包裹 CRUD 端点（implements IPackageManageService）
```

### 2.7 API 契约层

```
api/
├── dto/
│   ├── LoginRequestDTO.java
│   ├── CheckinRequestDTO.java
│   ├── PickupRequestDTO.java
│   ├── EditRequestDTO.java
│   ├── PackageResponseDTO.java
│   └── DashboardStatsResponseDTO.java
├── response/
│   └── Response.java                    ← 已有，统一 {code, info, data}
└── IPackageManageService.java           ← 外部 API 接口
```

### 2.8 数据流向

```
请求 → PackageController (implements IPackageManageService)
     → PackageService (领域服务, 无事务)
     → IPackageRepository (领域接口)
     → PackageRepository (基础设施实现, @Transactional)
     → IPackageDao (MyBatis Mapper)
     → MySQL
```

---

## 3. 数据模型 & 数据库设计

### 3.1 领域模型

```
PackageAggregate（聚合根）
├── PackageEntity
│   ├── id: String              // 业务主键（Snowflake 风格）
│   ├── waybillNo: String       // 运单号
│   ├── recipientPhone: String  // 收件人手机号
│   ├── courierCompany: String  // 快递公司编码
│   ├── shelfLocation: String   // 货架位置
│   ├── status: PackageStatusEnum  // PENDING / PICKED_UP
│   ├── checkinTime: LocalDateTime // 入库时间
│   └── pickupTime: LocalDateTime  // 取件时间（可空）
```

### 3.2 状态枚举

| 状态 | code | info |
|------|------|------|
| `PENDING` | 0 | 待取件 |
| `PICKED_UP` | 1 | 已取件 |

> 滞留不是独立状态，通过 `checkinTime > 48h AND status == PENDING` 动态计算得出。

### 3.3 快递公司枚举

| code | info |
|------|------|
| SF | 顺丰 |
| YTO | 圆通 |
| ZTO | 中通 |
| STO | 申通 |
| YD | 韵达 |
| JD | 京东 |
| DB | 德邦 |
| OTHER | 其他 |

### 3.4 数据库 DDL — 包裹表

```sql
CREATE TABLE package (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    biz_id       VARCHAR(32)    NOT NULL UNIQUE COMMENT '业务ID（对外使用）',
    waybill_no   VARCHAR(64)    NOT NULL COMMENT '运单号',
    phone        VARCHAR(20)    NOT NULL COMMENT '收件人手机号',
    courier      VARCHAR(32)    NOT NULL COMMENT '快递公司编码',
    shelf        VARCHAR(64)    NOT NULL COMMENT '货架位置',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0待取件 1已取件',
    checkin_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    pickup_time  DATETIME       NULL COMMENT '取件时间',
    create_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_waybill_no (waybill_no),
    INDEX idx_checkin_time (checkin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包裹表';
```

### 3.5 数据库 DDL — 用户表

```sql
CREATE TABLE sys_user (
    id          BIGINT         AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)    NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128)   NOT NULL COMMENT 'BCrypt加密密码',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化默认管理员账号: admin / admin123
INSERT INTO sys_user (username, password) VALUES ('admin', '$2a$10$...BCrypt哈希...');
```

### 3.6 关键设计决策

| 决策 | 说明 |
|------|------|
| 业务 ID 与自增 ID 分离 | `biz_id` 对外使用，`id` 仅数据库内部使用 |
| 软计算滞留 | 不在数据库存滞留字段，查询时动态判断（`NOW() - checkin_time > 48h AND status = 0`） |
| 手机号索引 | 高频查询字段，加普通索引 |
| 密码 BCrypt 加密 | 不存明文，使用 BCryptPasswordEncoder |

---

## 4. API 接口设计

### 4.1 接口总览

| 序号 | 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|------|
| 1 | POST | `/api/v1/auth/login` | 登录 | 否 |
| 2 | POST | `/api/v1/package/checkin` | 包裹入库 | 是 |
| 3 | GET | `/api/v1/package/list` | 包裹列表/搜索 | 是 |
| 4 | PUT | `/api/v1/package/pickup/{id}` | 确认取件 | 是 |
| 5 | PUT | `/api/v1/package/edit` | 编辑入库信息 | 是 |
| 6 | GET | `/api/v1/dashboard/stats` | 统计概览 | 是 |

### 4.2 接口详情

#### 4.2.1 登录

```
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
  "username": "admin",
  "password": "admin123"
}

Response 200:
{
  "code": "0000",
  "info": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}

Response 401:
{
  "code": "E0101",
  "info": "用户名或密码错误"
}
```

- 验证用户名密码，返回 JWT token（有效期 24h）
- 后续请求在 Header 携带 `Authorization: Bearer <token>`

#### 4.2.2 包裹入库

```
POST /api/v1/package/checkin
Authorization: Bearer <token>

Request:
{
  "waybillNo": "SF1234567890",
  "phone": "13800138000",
  "courier": "SF",
  "shelf": "A-01"
}

Response 200:
{
  "code": "0000",
  "info": "入库成功",
  "data": {
    "id": "PKG202606201234560001"
  }
}
```

- 校验规则：运单号不为空、手机号 11 位、courier 必须为合法枚举值、货架不为空
- 同运单号已有待取件包裹 → 返回 `E0202: 该运单号已存在待取件包裹`

#### 4.2.3 包裹列表/搜索

```
GET /api/v1/package/list?phone=&keyword=&status=&page=1&size=20
Authorization: Bearer <token>

Response 200:
{
  "code": "0000",
  "info": "成功",
  "data": {
    "total": 100,
    "list": [
      {
        "id": "PKG202606201234560001",
        "waybillNo": "SF1234567890",
        "phone": "13800138000",
        "courier": "SF",
        "courierDesc": "顺丰",
        "shelf": "A-01",
        "status": 0,
        "statusDesc": "待取件",
        "checkinTime": "2026-06-19 10:00:00",
        "pickupTime": null,
        "stale": false
      }
    ]
  }
}
```

- `phone`：按手机号精确查询（取件场景）
- `keyword`：模糊搜索运单号/手机号/快递公司
- `status`：0 = 待取件，1 = 已取件，不传则全部
- 滞留字段 `stale` 由后端实时计算（`NOW() - checkinTime > 48h`）
- 分页返回，默认每页 20 条

#### 4.2.4 确认取件

```
PUT /api/v1/package/pickup/{id}
Authorization: Bearer <token>

Request: (无 body)

Response 200:
{
  "code": "0000",
  "info": "取件成功"
}

Response 400:
{
  "code": "E0203",
  "info": "该包裹已被取走"
}
```

- 校验包裹存在且状态为 PENDING
- 更新 status → PICKED_UP，pickup_time → NOW()

#### 4.2.5 编辑入库信息

```
PUT /api/v1/package/edit
Authorization: Bearer <token>

Request:
{
  "id": "PKG202606201234560001",
  "waybillNo": "SF1234567890",
  "phone": "13800138000",
  "courier": "SF",
  "shelf": "B-03"
}

Response 200:
{
  "code": "0000",
  "info": "修改成功"
}
```

- 仅允许修改 PENDING 状态的包裹
- 已取件包裹编辑 → 返回 `E0204: 已取件包裹不可编辑`

#### 4.2.6 统计概览

```
GET /api/v1/dashboard/stats
Authorization: Bearer <token>

Response 200:
{
  "code": "0000",
  "info": "成功",
  "data": {
    "todayCheckin": 25,
    "pendingTotal": 48,
    "staleTotal": 3,
    "todayPickup": 18
  }
}
```

- `todayCheckin`：今日入库数（`checkin_time` 在今日范围内）
- `pendingTotal`：当前待取件总数（`status = 0`）
- `staleTotal`：当前滞留数（`status = 0 AND checkin_time < NOW() - 48h`）
- `todayPickup`：今日取件数（`pickup_time` 在今日范围内）

### 4.3 统一响应格式

```json
{
  "code": "0000",
  "info": "成功",
  "data": { ... }
}
```

### 4.4 错误码定义

| 错误码 | 说明 |
|--------|------|
| `0000` | 成功 |
| `0001` | 未知错误 |
| `0002` | 非法参数 |
| `0003` | 唯一索引冲突 |
| `0004` | 更新影响行数为0 |
| `E0101` | 用户名或密码错误 |
| `E0102` | 未登录或 Token 已过期 |
| `E0201` | 包裹不存在 |
| `E0202` | 该运单号已存在待取件包裹 |
| `E0203` | 该包裹已被取走 |
| `E0204` | 已取件包裹不可编辑 |

---

## 5. 前端设计

### 5.1 目录结构

```
package-manager-web/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── router/
│   │   └── index.ts              ← 路由配置 + 导航守卫
│   ├── utils/
│   │   └── request.ts            ← axios 实例 + 请求/响应拦截
│   ├── views/
│   │   ├── LoginView.vue         ← 登录页
│   │   ├── LayoutView.vue        ← 布局框架
│   │   ├── DashboardView.vue     ← 统计概览页
│   │   ├── CheckinView.vue       ← 包裹入库页
│   │   └── PackageListView.vue   ← 包裹列表页
│   ├── components/
│   │   └── StaleTag.vue          ← 滞留标签组件
│   └── styles/
│       └── global.css
```

### 5.2 路由设计

| 路径 | 页面 | 认证 | 说明 |
|------|------|------|------|
| `/login` | LoginView | 否 | 登录页，已登录自动跳转 `/` |
| `/` | LayoutView → 重定向 `/dashboard` | 是 | 布局外壳 |
| `/dashboard` | DashboardView | 是 | 统计概览 |
| `/checkin` | CheckinView | 是 | 包裹入库 |
| `/packages` | PackageListView | 是 | 包裹列表（默认 tab: 待取件） |

**路由守卫逻辑**：
- `router.beforeEach` 检查 localStorage 中 token
- 无 token 且目标非 `/login` → 重定向到 `/login`
- 有 token 且目标为 `/login` → 重定向到 `/`

### 5.3 axios 封装

```typescript
// utils/request.ts
import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
});

// 请求拦截器 — 注入 JWT
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  response => {
    const { code, info, data } = response.data;
    if (code === '0000') return data;
    ElMessage.error(info || '请求失败');
    return Promise.reject(new Error(info));
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      router.push('/login');
    }
    ElMessage.error('网络错误');
    return Promise.reject(error);
  }
);

export default request;
```

### 5.4 页面设计

#### 5.4.1 LoginView — 登录页

- 居中卡片式登录表单（`el-card` + `el-form`）
- 用户名 + 密码输入框，登录按钮
- 调用 `POST /api/v1/auth/login`，成功存 token → 跳转 `/dashboard`
- 失败显示 Element Plus `ElMessage` 错误提示

#### 5.4.2 LayoutView — 布局框架

```
┌──────────┬──────────────────────────────┐
│  侧边栏   │  顶栏: 包裹管理系统 | admin    │
│          ├──────────────────────────────┤
│  📊 统计  │                              │
│  📦 入库  │       <router-view />         │
│  📋 列表  │                              │
└──────────┴──────────────────────────────┘
```

- `el-container` + `el-aside` + `el-header` + `el-main`
- 侧边栏 `el-menu` 三个菜单项，`router` 模式自动关联路由
- 顶栏右侧显示用户名 + 退出按钮

#### 5.4.3 DashboardView — 统计概览

- 4 个统计卡片，使用 `el-card` 或 `el-statistic`
- 布局：`el-row` + `el-col`，span 各 6
- 滞留数卡片使用红色字体/背景突出
- `onMounted` 时调用 `GET /api/v1/dashboard/stats`

#### 5.4.4 CheckinView — 包裹入库

- `el-form` + `el-card` 包裹
- 运单号 (`el-input`) + 手机号 (`el-input`, 11位校验) + 快递公司 (`el-select` 下拉) + 货架位置 (`el-input`)
- 提交按钮调用 `POST /api/v1/package/checkin`
- 成功 → 清空表单，`ElMessage.success('入库成功')`
- 失败 → `ElMessage.error` 显示具体错误

#### 5.4.5 PackageListView — 包裹列表

```
┌──────────────────────────────────────────────────┐
│  手机号: [__________] 关键字: [__________]  [搜索] │
│  ┌──────────────┬──────────────┐                  │
│  │  ● 待取件 (48) │  ○ 已取件     │                  │
│  └──────────────┴──────────────┘                  │
│  ┌──────────────────────────────────────────────┐ │
│  │ 运单号│手机号│快递│货架│入库时间│状态│操作    │ │
│  ├──────────────────────────────────────────────┤ │
│  │SF12345│138..│顺丰│A-01│06-18 10│待取│[取件]  │ │
│  │YTO6789│139..│圆通│B-02│06-17 15│🔴滞留│[取件]│ │
│  │ZTO... │136..│中通│A-03│06-20 09│已取│-      │ │
│  └──────────────────────────────────────────────┘ │
│                         < 1  2  3 ... >          │
└──────────────────────────────────────────────────┘
```

- 上方搜索区：手机号精确筛选 + 关键字模糊搜索 + 搜索按钮
- `el-tabs` 切换"待取件"/"已取件"
- `el-table` 渲染列表
- **滞留行**：`:row-class-name` 动态添加 `stale-row` class，设置浅红色背景
- **取件按钮**：待取件行显示 `el-button`，点击触发 `el-popconfirm` 二次确认
- 已取件行不显示操作按钮
- 确认取件后调 `PUT /api/v1/package/pickup/{id}` → 刷新列表
- `el-pagination` 分页

#### 5.4.6 StaleTag 组件

- `el-tag` type="danger"，内容 "滞留" 或 "⚠ 滞留"
- 接收 `stale: boolean` prop，`false` 时不渲染

---

## 6. 后端模块设计

### 6.1 核心类关系

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  Controller │ ──→ │  IPackageService │ ──→ │ IPackageRepository│
│  (trigger)  │     │  (domain)        │     │  (domain)          │
└─────────────┘     └──────────────────┘     └─────────┬─────────┘
                                                       │ implements
                                              ┌────────▼──────────┐
                                              │ PackageRepository │
                                              │ (infrastructure)  │
                                              └────────┬──────────┘
                                                       │
                                              ┌────────▼──────────┐
                                              │   IPackageDao     │
                                              │   (MyBatis)       │
                                              └───────────────────┘
```

### 6.2 关键设计约束

| 规则 | 说明 |
|------|------|
| DI 使用 `@Resource` | 不用 `@Autowired` |
| 领域服务不加 `@Transactional` | 事务是基础设施关注点，只在 Repository 实现加 |
| 领域层零框架依赖 | 不导入 Spring/MyBatis 注解 |
| 手动参数校验 | Controller 层入口手动 `if` 校验，不使用 Bean Validation |
| 三层 catch | Controller 中 `BusinessException` → 业务错误，`Exception` → 未知错误 |
| PO ↔ Entity 内联转换 | 使用 Builder，不创建单独的 Converter 类 |
| 枚举 valueOf 抛异常 | 未知 code → `IllegalArgumentException`，不返回 null |

### 6.3 登录认证流程

```
LoginRequest → AuthController
  → 查 sys_user 表验证用户名/密码 (BCryptPasswordEncoder)
  → 生成 JWT Token (claims: username, exp: 24h)
  → 返回 token

后续请求:
  → JwtInterceptor (在 app/config 中注册)
  → 从 Header 取 token → 解析验证 → 放行 / 401
```

JWT 拦截器在 `app` 模块通过 `WebMvcConfigurer.addInterceptors()` 注册，放行 `/api/v1/auth/login`。

### 6.4 滞留计算方式

不依赖定时任务批量更新状态，而是在查询包裹列表时动态计算：

```java
// PackageService 中
boolean isStale = entity.getStatus() == PackageStatusEnum.PENDING
    && ChronoUnit.HOURS.between(entity.getCheckinTime(), LocalDateTime.now()) >= 48;
```

统计概览中的滞留总数使用 SQL 实时查询：

```sql
SELECT COUNT(*) FROM package
WHERE status = 0 AND checkin_time < DATE_SUB(NOW(), INTERVAL 48 HOUR);
```

---

## 7. 测试用例设计

### 7.1 后端 API 测试用例

| 编号 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| T1 | 正常入库 | 运单号 SF001，手机号 13800138000，快递 SF，货架 A-01 | code=0000, 返回业务ID |
| T2 | 重复运单号入库 | T1 已入库，再次用同一运单号入库 | code=E0202, 提示已存在 |
| T3 | 入库参数缺失 | 运单号为空 | code=0002, 非法参数 |
| T4 | 入库手机号格式错误 | 手机号 12345 | code=0002, 非法参数 |
| T5 | 按手机号查询 | 手机号 13800138000 | 返回该手机号所有待取件包裹 |
| T6 | 确认取件 | T1 的包裹 id | code=0000, 状态变为已取件 |
| T7 | 重复取件 | T6 已取件的包裹再次取件 | code=E0203, 已被取走 |
| T8 | 取件后不可编辑 | T6 的包裹调用编辑接口 | code=E0204, 不可编辑 |
| T9 | 统计概览 | 调用 dashboard/stats | 返回正确的入库数/待取数/滞留数/取件数 |
| T10 | 未登录访问 | 不带 token 调入库接口 | 返回 401 |

### 7.2 前端测试场景

| 编号 | 场景 | 操作 | 预期 |
|------|------|------|------|
| T11 | 登录成功 | 输入正确账号密码登录 | 跳转 Dashboard |
| T12 | 登录失败 | 输入错误密码 | 提示"用户名或密码错误" |
| T13 | 未登录拦截 | 直接在地址栏输入 /dashboard | 重定向到 /login |
| T14 | 入库表单 | 填写完整信息提交 | 提示"入库成功"，表单清空 |
| T15 | 滞留标签展示 | 查看包含 >48h 包裹的列表 | 对应行背景红色 + 滞留标签 |
| T16 | 取件确认 | 点击取件 → 二次确认 → 确定 | 列表刷新，该包裹进入已取件 tab |

---

## 8. 部署架构

```
┌─────────────────────────────────────────┐
│                Server                    │
│                                          │
│  ┌──────────┐     ┌──────────────────┐  │
│  │  nginx   │ ──→ │  Spring Boot     │  │
│  │  (端口80) │     │  (端口8080)       │  │
│  │          │     │                  │  │
│  │ /        │     │  JAR 部署         │  │
│  │ 静态文件  │     │  -Dspring.profiles│  │
│  │          │     │   =release       │  │
│  │ /api/v1/ │     │                  │  │
│  │ 代理转发  │     │                  │  │
│  └──────────┘     └────────┬─────────┘  │
│                            │            │
│                   ┌────────▼─────────┐  │
│                   │      MySQL       │  │
│                   │    (端口3306)     │  │
│                   └──────────────────┘  │
└─────────────────────────────────────────┘
```

- 前端 `dist/` 静态文件由 nginx 直接 serve
- `/api/v1/*` 由 nginx 反代到 Spring Boot 8080 端口
- MySQL 通过 Spring DataSource 连接
- 支持多环境 profile：dev / test / prod（沿用项目已有 pom.xml 配置）

---

## 附录：待实现文件清单

### 后端（按依赖顺序）

| 序号 | 模块 | 文件 |
|------|------|------|
| 1 | types | `ResponseCode.java`（扩展错误码） |
| 2 | api | `dto/LoginRequestDTO.java` |
| 3 | api | `dto/CheckinRequestDTO.java` |
| 4 | api | `dto/EditRequestDTO.java` |
| 5 | api | `dto/PackageResponseDTO.java` |
| 6 | api | `dto/DashboardStatsResponseDTO.java` |
| 7 | api | `IPackageManageService.java` |
| 8 | domain | `package/model/valobj/PackageStatusEnum.java` |
| 9 | domain | `package/model/valobj/CourierCompanyEnum.java` |
| 10 | domain | `package/model/entity/PackageEntity.java` |
| 11 | domain | `package/model/aggregate/PackageAggregate.java` |
| 12 | domain | `package/adapter/repository/IPackageRepository.java` |
| 13 | domain | `package/service/IPackageService.java` |
| 14 | domain | `package/service/PackageService.java` |
| 15 | infrastructure | `dao/po/PackagePO.java` |
| 16 | infrastructure | `dao/IPackageDao.java` |
| 17 | infrastructure | `adapter/repository/PackageRepository.java` |
| 18 | app | `config/JwtInterceptor.java` |
| 19 | app | `config/WebMvcConfig.java`（注册拦截器） |
| 20 | trigger | `http/AuthController.java` |
| 21 | trigger | `http/PackageController.java` |
| 22 | resources | `mapper/package_mapper.xml`（MyBatis SQL） |
| 23 | resources | `db/schema.sql`（DDL） |
| 24 | resources | `application-dev.yml`（数据库连接等） |

### 前端

| 序号 | 文件 |
|------|------|
| 1 | `package.json` / `vite.config.ts` / `tsconfig.json` |
| 2 | `src/main.ts` / `src/App.vue` |
| 3 | `src/router/index.ts` |
| 4 | `src/utils/request.ts` |
| 5 | `src/views/LoginView.vue` |
| 6 | `src/views/LayoutView.vue` |
| 7 | `src/views/DashboardView.vue` |
| 8 | `src/views/CheckinView.vue` |
| 9 | `src/views/PackageListView.vue` |
| 10 | `src/components/StaleTag.vue` |
| 11 | `src/styles/global.css` |
