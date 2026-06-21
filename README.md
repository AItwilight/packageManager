# PackageManager — 末端驿站包裹管理系统

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-blue.svg)](https://vuejs.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](LICENSE)

面向末端快递驿站的包裹管理 Web 应用，支持包裹入库、取件确认、多条件搜索、滞留预警及数据看板。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端框架 | Spring Boot 2.7.12 (Java 8) |
| ORM | MyBatis 2.1.4 + MyBatis-Spring-Boot-Starter |
| 数据库 | MySQL 8.0 (HikariCP 连接池) |
| 认证 | JWT (io.jsonwebtoken 0.9.1) |
| 密码加密 | BCrypt (Spring Security Crypto) |
| 缓存 | Guava Cache |
| 前端 | Vue 3 + TypeScript + Vite 8 |
| UI 库 | Element Plus 2.5 |
| HTTP 客户端 | Axios |
| 容器化 | Docker + Docker Compose |

## 架构

采用 DDD 六边形架构，模块分层如下：

```
packageManager-app           ← Spring Boot 启动层、配置类
    ↓
packageManager-trigger       ← HTTP Controller + JWT 拦截器
    ↓
packageManager-api           ← API 接口定义 + DTO
    ↓
packageManager-domain         ← 领域模型（实体、聚合、仓储接口、领域服务）
    ↓                           ↑
packageManager-infrastructure ← 仓储实现（MyBatis DAO + PO）
    ↓
packageManager-types          ← 共享类型（枚举、异常、常量）
```

```
package-manager-web           ← Vue 3 SPA 前端（独立模块）
```

## 功能

- **包裹入库** — 录入运单号、手机号、快递公司、货架位置，自动生成取件码（`货架-流水号`，如 `A-13-0001`）。同运单号重复待取件自动拦截
- **取件确认** — 扫描包裹业务 ID 一键取件，记录取件时间
- **高级搜索** — 手机号后四位精确匹配 + 多字段模糊搜索（运单号 / 手机号 / 快递公司 / 取件码），支持状态筛选、滞留筛选、入库时间排序、分页
- **滞留预警** — 入库超过 48 小时未取件自动标记为滞留（stale），前端列表红框标记 + Danger 标签 + 看板独立统计
- **数据看板** — 今日入库数 / 待取件总数 / 滞留数 / 今日取件数，四项实时统计
- **信息编辑** — 支持修改包裹信息（运单号、手机号、快递公司、货架），已取件包裹禁止编辑，货架变更自动同步取件码前缀
- **JWT 认证** — 登录 / 登出，Token 24 小时有效期，拦截器校验

## 快速开始

### 1. 启动基础设施

```bash
docker-compose -f docs/dev-ops/docker-compose-environment.yml up -d
```

自动启动 MySQL 8.0（`127.0.0.1:3306`）、phpMyAdmin（`localhost:8899`）、Redis 6.2。

### 2. 初始化数据库

连接 MySQL 执行建表脚本：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p523Hust.985 < docs/dev-ops/mysql/sql/schema.sql
```

脚本自动创建库 `package_manager`、表 `package` / `daily_sequence` / `sys_user`，并初始化默认管理员 `admin / admin123`。

### 3. 启动后端

```bash
# 编译
mvn clean package -DskipTests

# 启动（默认 dev profile，端口 8091）
java -jar packageManager-app/target/packageManager-app.jar

# 或指定 profile
java -jar -Dspring.profiles.active=prod packageManager-app/target/packageManager-app.jar
```

### 4. 启动前端

```bash
cd package-manager-web
npm install
npm run dev
```

前端运行在 `http://localhost:3000`，API 请求通过 Vite proxy 自动转发到后端 `localhost:8091`。

### 5. 登录

浏览器打开 `http://localhost:3000`，使用默认账号登录：

- 用户名：`admin`
- 密码：`admin123`

## API 接口

Base URL: `http://localhost:8091/api/v1`

除登录外所有接口需携带 `Authorization: Bearer <token>`。

### 认证

| Method | Path | 说明 |
|--------|------|------|
| POST | `/auth/login` | 登录，返回 JWT token |

请求体：`{ "username": "admin", "password": "admin123" }`

### 包裹管理

| Method | Path | 说明 |
|--------|------|------|
| POST | `/package/checkin` | 包裹入库 |
| GET | `/package/list` | 分页查询包裹列表 |
| PUT | `/package/pickup/{id}` | 确认取件 |
| PUT | `/package/edit` | 编辑包裹信息 |

### 看板

| Method | Path | 说明 |
|--------|------|------|
| GET | `/dashboard/stats` | 看板统计（今日入库/待取件/滞留/今日取件） |

### 查询参数（GET `/package/list`）

| 参数 | 类型 | 说明 |
|------|------|------|
| phone | string | 手机号后四位精确匹配 |
| keyword | string | 模糊搜索（运单号 / 手机号 / 快递 / 取件码） |
| status | int | 0 = 待取件，1 = 已取件 |
| stale | boolean | 仅查滞留包裹（入库超 48h） |
| sortOrder | string | asc / desc，按入库时间排序，默认 desc |
| page | int | 页码，默认 1 |
| size | int | 每页条数，默认 20 |

### 响应格式

```json
{
  "code": "0000",
  "info": "成功",
  "data": {}
}
```

## 数据库

### package（包裹表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT PK | 自增主键 |
| biz_id | VARCHAR(32) UNIQUE | 业务 UUID |
| waybill_no | VARCHAR(64) | 运单号 |
| phone | VARCHAR(20) | 收件人手机号 |
| courier | VARCHAR(32) | 快递公司编码 |
| shelf | VARCHAR(4) | 货架位置（如 `A-13`） |
| status | TINYINT | 0 = 待取件，1 = 已取件 |
| checkin_time | DATETIME | 入库时间 |
| pickup_time | DATETIME | 取件时间 |
| pickup_code | VARCHAR(10) | 取件码（如 `A-13-0001`） |
| phone_suffix | VARCHAR(4) | 手机号后四位（生成列，自动提取） |
| user_id | BIGINT | 入库操作人 ID |

### daily_sequence（每日流水号表）

保证取件码流水号原子递增，单日最大 9999。

### sys_user（系统用户表）

BCrypt 加密存储密码。

## 快递公司编码

| 编码 | 名称 | 编码 | 名称 |
|------|------|------|------|
| SF | 顺丰速运 | YD | 韵达快递 |
| YTO | 圆通速递 | JD | 京东物流 |
| ZTO | 中通快递 | DB | 德邦快递 |
| STO | 申通快递 | OTHER | 其他 |

## 项目结构

```
packageManager/
├── packageManager-app/              # Spring Boot 入口 + MyBatis XML + 配置
│   └── src/main/resources/
│       ├── application.yml          # 默认配置
│       ├── application-dev.yml      # 开发环境配置
│       └── mybatis/mapper/          # MyBatis SQL 映射文件
├── packageManager-trigger/          # HTTP Controller + JWT 拦截器
│   └── trigger/http/
│       ├── AuthController.java      # 登录接口
│       └── PackageController.java   # 包裹 CRUD + 看板接口
├── packageManager-api/              # API 接口 + DTO
│   └── api/
│       ├── IPackageManageService.java
│       └── dto/                     # Request / Response DTO
├── packageManager-domain/           # 领域模型
│   └── domain/pkg/
│       ├── model/
│       │   ├── aggregate/           # 聚合根（PackageAggregate）
│       │   ├── entity/              # 实体（PackageEntity）
│       │   └── valobj/              # 值对象（枚举）
│       ├── adapter/repository/      # 仓储接口
│       └── service/                 # 领域服务
├── packageManager-infrastructure/   # 基础设施层
│   └── infrastructure/
│       ├── dao/                     # MyBatis Mapper 接口 + PO
│       └── adapter/repository/      # 仓储实现
├── packageManager-types/            # 共享类型
│   └── types/
│       ├── enums/ResponseCode.java  # 错误码枚举
│       └── exception/               # 自定义异常
├── package-manager-web/             # Vue 3 + TypeScript 前端
│   └── src/
│       ├── router/                  # 路由配置 + 导航守卫
│       ├── utils/request.ts         # Axios 封装（JWT 注入 / 错误拦截）
│       └── views/                   # 页面组件
│           ├── LoginView.vue        # 登录页
│           ├── LayoutView.vue       # 主布局（侧边栏 + 顶栏）
│           ├── DashboardView.vue    # 数据看板
│           ├── CheckinView.vue      # 包裹入库
│           └── PackageListView.vue  # 包裹列表 / 搜索
├── docs/dev-ops/
│   ├── docker-compose-environment.yml  # 基础设施 Docker Compose
│   ├── docker-compose-app.yml          # 应用 Docker Compose
│   └── mysql/sql/schema.sql            # 建表脚本
└── pom.xml                          # 根 POM
```

## 错误码

| 编码 | 说明 |
|------|------|
| 0000 | 成功 |
| 0001 | 未知错误 |
| 0002 | 参数无效 |
| 0003 | 业务 ID 重复（唯一键冲突） |
| 0004 | 更新影响 0 行（并发冲突或数据不存在） |
| E0101 | 用户名或密码错误 |
| E0102 | Token 无效或已过期 |
| E0201 | 包裹不存在 |
| E0202 | 同运单号已有待取件包裹（不允许重复入库） |
| E0203 | 包裹已被取走（不允许重复取件） |
| E0204 | 已取件包裹不可编辑 |
| E0205 | 货架格式无效（应为 大写字母-两位数字，如 A-13） |
| E0206 | 当日流水号超限（最大 9999） |


## License

[Apache License 2.0](LICENSE)
