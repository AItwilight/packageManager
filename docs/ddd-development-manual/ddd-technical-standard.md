# 六边形 DDD 技术标准

> **版本**: v2.0 | **适用范围**: 基于 Spring Boot 的微服务后端项目 | **架构**: 六边形架构 + DDD 战术设计

---

## 目录

1. [架构概览](#1-架构概览)
2. [项目结构规范](#2-项目结构规范)
3. [核心开发工作流](#3-核心开发工作流)
   - [3.1 新增 API 接口](#31-新增-api-接口)
   - [3.2 新增限界上下文](#32-新增限界上下文)
   - [3.3 新增领域模型](#33-新增领域模型)
   - [3.4 新增持久化操作](#34-新增持久化操作)
   - [3.5 新增定时任务](#35-新增定时任务)
   - [3.6 新增消息监听](#36-新增消息监听)
   - [3.7 新增外部服务调用](#37-新增外部服务调用)
4. [编码规范速查](#4-编码规范速查)
5. [设计模式应用指南](#5-设计模式应用指南)
6. [反模式警示](#6-反模式警示)
7. [模板库（Java / Spring Boot 版）](#7-模板库)

---

## 1. 架构概览

### 1.1 分层架构

```
                      ┌─────────────────┐
                      │    app           │
                      │   启动 & 配置     │
                      └────────┬────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
   ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐
   │  trigger     │   │   domain     │   │  infrastructure  │
   │  (入站适配器) │   │   (领域核心)  │   │  (出站适配器)     │
   │              │   │              │   │                  │
   │ HTTP Controller│  │ 实体/值对象  │   │ 仓储实现          │
   │ 定时任务      │   │ 聚合         │   │ 端口实现          │
   │ 消息监听      │   │ 仓储接口      │   │ ORM/DAO          │
   │              │   │ 端口接口      │   │ 外部网关          │
   │              │   │ 领域服务      │   │ 缓存/消息/事件    │
   └──────┬───────┘   └──────┬───────┘   └────────┬─────────┘
          │                  │                     │
          └──────────────────┼─────────────────────┘
                             │
                    ┌────────┴────────┐
                    │   共享内核       │
                    │  (types/common) │
                    │  枚举 · 异常 ·   │
                    │  常量 · 事件基类  │
                    └─────────────────┘
```

**依赖方向**: `app → trigger + domain + infrastructure`，`trigger → domain ← infrastructure`，`domain` 作为核心不依赖任何框架。

### 1.2 核心原则

| 原则 | 说明 |
|------|------|
| **依赖倒置** | 领域层定义接口（仓储、端口），基础设施层实现它们 |
| **领域纯粹** | 领域层不依赖任何框架（Spring、MyBatis、JPA），只使用 JDK + 通用工具 |
| **端口隔离** | 入站（trigger）和出站（infrastructure）通过领域定义的端口隔离 |
| **聚合边界** | 聚合是事务一致性边界，一个事务只操作一个聚合 |
| **统一契约** | 所有外部 API 返回统一响应格式 `{code, info, data}` |

### 1.3 数据流向

```
请求 → Controller → 领域服务 → 仓储接口(domain) → 仓储实现(infra) → ORM/DAO → DB
                                         ↓
                                  端口接口(domain) → 端口实现(infra) → 外部系统
```

---

## 2. 项目结构规范

### 2.1 模块划分（Maven/Gradle 多模块）

| 模块 | 层 | 包含内容 | 编译依赖 |
|------|-----|----------|----------|
| `{project}-api` | 接口层 | 外部服务接口 `I*Service`、请求/响应 DTO、统一响应包装类 | 无 |
| `{project}-types` | 共享内核 | 错误码枚举、业务异常类、领域事件基类、全局常量 | 无 |
| `{project}-domain` | **领域核心** | 限界上下文、实体、值对象、聚合、仓储接口、端口接口、领域服务 | types |
| `{project}-infrastructure` | 出站适配器 | 仓储实现、ORM 映射/DAO、端口实现、网关、缓存、消息队列 | domain, types |
| `{project}-trigger` | 入站适配器 | HTTP Controller、定时任务、MQ 监听器 | api, domain, types |
| `{project}-app` | 启动引导 | 主启动类、全局配置（线程池、连接池、过滤器等） | 全部 |

**模块依赖关系**:

```
app ──→ trigger ──→ domain ←── infrastructure
  │        │                    │
  └────────┼────────────────────┘
           ▼
       types, api
```

### 2.2 限界上下文内部结构

```
domain/{context}/
├── adapter/
│   ├── repository/        # 仓储接口 I{Context}Repository
│   └── port/              # 出站端口接口 I{Context}Port
├── model/
│   ├── aggregate/         # 聚合根
│   ├── entity/            # 领域实体
│   └── valobj/            # 值对象、领域枚举
└── service/               # 领域服务
    ├── I{Context}Service
    ├── {Context}Service
    └── {功能子域}/         # 策略/责任链等子领域实现
```

### 2.3 基础设施对应结构

```
infrastructure/
├── adapter/
│   ├── repository/        # 仓储实现 (implements domain 仓储接口)
│   └── port/              # 端口实现 (implements domain 端口接口)
├── dao/                   # ORM 映射器接口 (@Mapper / @Repository)
│   └── po/                # 持久化对象（PO = 1:1 对应数据库表）
├── gateway/               # HTTP/RPC 外部服务调用客户端
├── redis/                 # 缓存服务
├── event/                 # 事件发布器
└── dcc/                   # 动态配置中心
```

### 2.4 入站适配器结构

```
trigger/
├── http/                  # REST Controller
├── job/                   # 定时任务
└── listener/              # MQ 消息监听器
```

### 2.5 API 接口层结构

```
api/
├── dto/                   # 请求/响应 DTO
├── response/              # 统一响应包装 Response<T>
└── I*Service.java         # 外部 API 接口定义
```

---

## 3. 核心开发工作流

### 3.1 新增 API 接口

> **场景**: 新增一个 HTTP 端点，从接口层→领域层→持久化层全链路贯通。

#### 检查清单（10步）

- [ ] **Step 1**: 在 `types` 模块定义所需错误码
- [ ] **Step 2**: 在 `api` 模块创建 Request/Response DTO
- [ ] **Step 3**: 在 `api` 模块定义服务接口 `I{功能}Service`
- [ ] **Step 4**: 在 `domain` 模块定义领域实体/值对象（如需要新模型）
- [ ] **Step 5**: 在 `domain` 模块定义仓储接口 `I{Context}Repository`
- [ ] **Step 6**: 在 `domain` 模块实现领域服务
- [ ] **Step 7**: 在 `infrastructure` 模块实现仓储
- [ ] **Step 8**: 在 `infrastructure` 模块创建/扩展 ORM 映射器和 PO
- [ ] **Step 9**: 在 `trigger` 模块创建 Controller
- [ ] **Step 10**: 端到端测试

#### 决策树

```
需要调用外部服务或发布领域事件？
  ├─ 是 → 使用 Port 模式（接口在 domain，实现在 infrastructure）
  └─ 否 → 直接调用 Repository

操作涉及多个实体的原子性更新？
  ├─ 是 → 定义 Aggregate 作为事务边界
  └─ 否 → 使用单个 Entity

承载只读的业务计算数据？
  ├─ 是 → 使用 Value Object（@Getter，不可变）
  └─ 否 → 使用 Entity（@Data，可变）
```

#### 文件创建顺序（依赖关系图）

```
types/（错误码）
  └─→ api/（DTO + 接口）
      └─→ domain/（实体 + 仓储接口 + 领域服务）
          └─→ infrastructure/（仓储实现 + PO + ORM）
              └─→ trigger/（Controller）
```

---

### 3.2 新增限界上下文

> **场景**: 引入一个全新的业务子领域。

#### 检查清单（5步）

- [ ] **Step 1**: 在 `domain` 模块按标准结构创建新包

```
domain/{new-context}/
├── adapter/
│   ├── repository/       # I{Context}Repository.java
│   └── port/             # I{Context}Port.java（如需出站调用）
├── model/
│   ├── aggregate/
│   ├── entity/
│   └── valobj/
└── service/
```

- [ ] **Step 2**: 在 `infrastructure` 模块创建对应实现

```
infrastructure/
├── adapter/repository/    # {Context}Repository.java
├── adapter/port/          # {Context}Port.java
├── dao/                   # I{Entity}Dao.java + po/{Entity}PO.java
```

- [ ] **Step 3**: 在 `api` 模块定义接口契约和 DTO（如需对外暴露）
- [ ] **Step 4**: 在 `trigger` 模块创建 Controller / Job / Listener（如需触发入口）
- [ ] **Step 5**: 注册 ORM 映射和 Spring Bean 扫描

---

### 3.3 新增领域模型

#### 3.3.1 构造型速查

| 构造型 | 特征 | Lombok 组合 | 位置 |
|--------|------|------------|------|
| **Entity** | 有唯一标识 | `@Data @Builder @AllArgsConstructor @NoArgsConstructor` | `domain/{context}/model/entity/` |
| **Value Object** | 无标识，不可变 | `@Getter @Builder @AllArgsConstructor @NoArgsConstructor` | `domain/{context}/model/valobj/` |
| **Domain Enum** | 业务枚举 (code + description) | `@Getter @AllArgsConstructor @NoArgsConstructor` | `domain/{context}/model/valobj/` |
| **Aggregate** | 实体组，事务边界 | `@Data @Builder @AllArgsConstructor @NoArgsConstructor` | `domain/{context}/model/aggregate/` |

#### 3.3.2 Entity 设计准则

```java
// 特征:
// 1. 唯一业务标识（非数据库自增ID）
// 2. 使用领域枚举而非原始 int/String
// 3. 可变（@Data 包含 setter）
// 4. 不含框架注解

public class OrderEntity {
    private String orderId;           // 业务主键
    private BigDecimal amount;        // 金额
    private OrderStatusEnum status;    // 领域枚举，不是 int
}
```

#### 3.3.3 Value Object 设计准则

```java
// 特征:
// 1. 无独立标识，通过值判断相等
// 2. 不可变（@Getter，无 setter）
// 3. 通过 Builder 或静态工厂方法构造

public class ProgressVO {
    private Integer targetCount;
    private Integer completeCount;
}
```

#### 3.3.4 Enum VO 设计准则

```java
// 特征:
// 1. code + description 模式
// 2. 提供 valueOf(Integer code) 静态工厂方法
// 3. 未知 code 必须抛异常（而非返回 null）

public enum OrderStatusEnum {
    CREATED(0, "已创建"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款");

    private Integer code;
    private String description;

    public static OrderStatusEnum valueOf(Integer code) {
        for (OrderStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }
}
```

#### 3.3.5 Aggregate 设计准则

```java
// 特征:
// 1. 组合多个实体和值对象
// 2. 是事务一致性边界（一个事务只更新一个聚合）
// 3. 提供静态工厂方法表达创建意图
// 4. 通过 Repository 整体加载和持久化

public class OrderAggregate {
    private UserEntity user;
    private ActivityEntity activity;
    private DiscountEntity discount;
    private Integer participationCount;  // 标量值

    // 静态工厂：表达不同场景的创建
    public static OrderAggregate createForNewTeam(UserEntity user, ...) { ... }
    public static OrderAggregate createForExistingTeam(UserEntity user, ...) { ... }
}
```

---

### 3.4 新增持久化操作

#### 3.4.1 仓储模式

```
┌──────────────────────────────────────────┐
│  domain 层                                │
│  I{Context}Repository                    │  ← 接口：纯 Java，无框架注解
│  - queryXxx(domainEntity)                │    只接受/返回领域对象
│  - saveXxx(aggregate)                    │
└────────────────┬─────────────────────────┘
                 │ implements
┌────────────────▼─────────────────────────┐
│  infrastructure 层                        │
│  {Context}Repository                     │  ← 实现：@Repository，注入 DAO
│  - PO → Entity (Builder 转换)             │    方法上 @Transactional
│  - Entity → PO (Builder 转换)             │
│  - 调用 DAO (MyBatis/JPA)                │
└──────────────────────────────────────────┘
```

#### 3.4.2 对象转换规范

```
请求/响应 ←→ DTO ←→ 领域对象 ←→ PO ←→ 数据库表
  (Controller)    (Service)    (Repository)   (ORM)
```

| 转换方向 | 位置 | 方式 |
|----------|------|------|
| DTO → Entity | Controller / Domain Service | Builder 构建 |
| Entity → DTO | Controller | Builder 构建 |
| Entity → PO | Repository Impl | Builder 构建 |
| PO → Entity | Repository Impl | Builder 构建（int → 枚举） |

> **转换原则**: 不使用单独的 Assembler/Converter 工具类。转换逻辑内联在数据源旁边（Repository impl 中 PO↔Entity，Controller 中 DTO↔Entity）。

#### 3.4.3 事务管理

| 位置 | 是否加 @Transactional | 原因 |
|------|----------------------|------|
| Repository 实现（infrastructure） | ✅ 是 | 事务是持久化关注点 |
| 领域服务（domain） | ❌ 否 | 领域层不应感知事务 |
| Controller（trigger） | ❌ 否 | 事务应在服务层处理 |

#### 3.4.4 缓存穿透模式

```java
// 基类提供缓存-through能力
public abstract class AbstractRepository {
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback) {
        // 1. 检查缓存开关（由配置中心控制）
        // 2. 命中缓存 → 直接返回
        // 3. 未命中 → 查 DB → 写缓存 → 返回
    }
}
```

---

### 3.5 新增定时任务

#### 设计模板

```java
public class XxxJob {
    // 1. 注入 RedissonClient（或其他分布式锁实现）
    // 2. @Scheduled(cron = "...") 定义执行频率
    // 3. tryLock(waitTime, leaseTime) 获取分布式锁
    // 4. 未获锁 → 跳过本次执行
    // 5. 执行业务逻辑
    // 6. finally 块释放锁
}
```

#### 关键约束

| 约束 | 说明 |
|------|------|
| 必须使用分布式锁 | 多实例部署防止重复执行 |
| leaseTime > 执行时间 | 防止锁提前释放 |
| finally 释放锁 | 确保异常时也释放 |
| 判断 `isLocked && isHeldByCurrentThread` | 只释放自己持有的锁 |

---

### 3.6 新增消息监听

```java
public class XxxListener {
    // 1. @RabbitListener 绑定队列/交换机/路由键
    // 2. 解析消息 → 处理业务
    // 3. 处理失败 → 抛出 RuntimeException → 触发 MQ 重投
    // 4. 业务逻辑必须幂等（分布式锁 / DB 唯一约束）
}
```

**幂等策略优先级**: DB 唯一索引 > 分布式锁 > 状态机检查

---

### 3.7 新增外部服务调用

#### Port / Adapter 模式

```
domain/adapter/port/ISomePort.java     ← 端口接口（业务语义）
infrastructure/adapter/port/SomePort.java ← 端口实现（技术细节）
infrastructure/gateway/SomeGateway.java   ← 网关（HTTP/RPC 客户端）
infrastructure/event/EventPublisher.java  ← 事件发布器（MQ）
```

| 组件 | 位置 | 职责 |
|------|------|------|
| Port 接口 | `domain/{context}/adapter/port/` | 领域需要的出站调用声明 |
| Port 实现 | `infrastructure/adapter/port/` | 使用具体技术完成调用，含分布式锁 |
| Gateway | `infrastructure/gateway/` | 封装 HTTP/RPC 客户端（OkHttp、gRPC 等） |

**Repository vs Port 决策**:

| 场景 | 使用 |
|------|------|
| 数据持久化（DB、缓存） | Repository |
| 调用外部系统（第三方 API、其他微服务） | Port |
| 发布领域事件（MQ） | Port 或 EventPublisher |

---

## 4. 编码规范速查

### 4.1 命名约定

| 构造型 | 模式 | 示例 | 所在模块 |
|--------|------|------|----------|
| 领域实体 | `{名词}Entity` | `OrderEntity`, `UserEntity` | domain |
| 值对象 | `{名词}VO` | `ProgressVO`, `ConfigVO` | domain |
| 领域枚举 | `{名词}Enum` 或 `{名词}EnumVO` | `OrderStatusEnum` | domain |
| 聚合根 | `{名词}Aggregate` | `OrderAggregate` | domain |
| 仓储接口 | `I{Context}Repository` | `IOrderRepository` | domain |
| 仓储实现 | `{Context}Repository` | `OrderRepository` | infrastructure |
| 端口接口 | `I{Context}Port` | `INotifyPort` | domain |
| 端口实现 | `{Context}Port` | `NotifyPort` | infrastructure |
| 领域服务接口 | `I{Context}{操作}Service` | `ILockOrderService` | domain |
| 领域服务实现 | `{Context}{操作}Service` | `LockOrderService` | domain |
| ORM 映射器 | `I{表名}Dao` / `{表名}Mapper` | `IOrderDao` | infrastructure |
| 持久化对象 | `{表名}` 或 `{表名}PO` | `Order`, `OrderPO` | infrastructure |
| API 接口 | `I{功能}Service` | `ITradeService` | api |
| 请求 DTO | `{操作}RequestDTO` | `CreateOrderRequestDTO` | api |
| 响应 DTO | `{操作}ResponseDTO` | `CreateOrderResponseDTO` | api |
| Controller | `{资源}Controller` | `OrderController` | trigger |
| 定时任务 | `{功能}Job` | `TimeoutCheckJob` | trigger |
| 消息监听器 | `{功能}Listener` | `PaymentCallbackListener` | trigger |

### 4.2 DI 与注解规范

| 规则 | 说明 |
|------|------|
| 使用 `@Resource` 而非 `@Autowired` | 避免 by-type 歧义，按名称装配更稳定 |
| 使用 `@Slf4j`（Lombok） | 统一日志方式 |
| `@Service` → 领域服务、端口实现 | Spring Bean |
| `@Repository` → 仓储实现 | Spring Bean + ORM 异常转换 |
| `@Component` → 消息监听器、事件发布器 | Spring Bean |
| `@RestController` + `@CrossOrigin("*")` → Controller | REST 端点 |
| `@Transactional(timeout=N)` → 仓储写方法 | 仅 infrastructure 层 |

### 4.3 异常处理规范

**三层 catch 模型（Controller 层）**:

```java
try {
    // 正常业务
} catch (BusinessException e) {
    // 层1: 已知业务异常 → 返回具体错误码
    return Response.fail(e.getCode(), e.getMessage());
} catch (Exception e) {
    // 层2: 未知异常 → 返回通用错误码
    return Response.fail(ErrorCode.UNKNOWN);
}
```

**异常抛出规则**:

| 场景 | 抛什么 | 示例 |
|------|--------|------|
| 业务规则违反 | `BusinessException(code, msg)` | 活动未生效、库存不足 |
| 数据更新为0行 | `BusinessException(ErrorCode.UPDATE_ZERO)` | 乐观锁冲突 |
| 唯一索引冲突 | `BusinessException(ErrorCode.DUP_KEY)` | 重复下单 |
| 外部调用失败 | `BusinessException(ErrorCode.EXT_CALL_FAIL)` | HTTP 超时 |

### 4.4 ID 生成

| 策略 | 场景 | 说明 |
|------|------|------|
| 随机数字字符串 | 对外业务 ID | 分布式友好，可读性好 |
| Snowflake ID | 高并发场景 | 有序、高性能 |
| 组合 bizId | 幂等键 | `{partitionKey}_{userId}_{counter}` |

### 4.5 统一响应格式

```json
{
  "code": "0000",
  "info": "成功",
  "data": { ... }
}
```

- `0000` 系列：系统通用码
- `E0xxx` 系列：业务错误码，按领域分段

### 4.6 参数校验

采用**手动校验**模式（不使用 Bean Validation）：

```java
if (StringUtils.isBlank(request.getUserId())
        || null == request.getAmount()) {
    return Response.fail(ErrorCode.ILLEGAL_PARAM);
}
```

**校验位置**: Controller 层入口，尽早返回。

### 4.7 Lombok 使用规则

| 类型 | 注解 | 原因 |
|------|------|------|
| Entity | `@Data @Builder @AllArgsConstructor @NoArgsConstructor` | 可变对象 |
| VO | `@Getter @Builder @AllArgsConstructor @NoArgsConstructor` | 不可变 |
| Enum | `@Getter @AllArgsConstructor @NoArgsConstructor` | 只读 |
| Aggregate | 同 Entity | 可变聚合 |
| DTO | `@Data` (请求) / `@Data @Builder` (响应) | 数据载体 |
| PO | 同 Entity | 数据库映射 |

### 4.8 包导入约束

```
domain 层禁止导入:
  ✗ org.springframework.*
  ✗ infrastructure 模块的任何类
  ✗ ORM 相关类（MyBatis、JPA）
  ✗ 数据库驱动类

domain 层允许导入:
  ✓ java.*, javax.*
  ✓ lombok
  ✓ org.slf4j
  ✓ org.apache.commons.lang3
  ✓ types 模块
```

---

## 5. 设计模式应用指南

### 5.1 策略模式

**场景**: 同一接口有多种实现，运行时根据条件选择。

**实现模式**:
1. 定义策略接口 `IStrategy`
2. 可选用模板抽象类 `AbstractStrategy` 提供公共逻辑
3. 具体实现用 `@Service("name")` 或 `@Component("name")` 注册
4. 调用方通过 `Map<String, IStrategy>` 注入并选择

**何时使用**: 多种算法变体、多种计费规则、多种优惠计算方式。

### 5.2 责任链模式

**场景**: 请求需经过多道规则校验，任一不通过则中断返回。

**实现模式**:
1. 每个规则实现同一接口 `IFilter<Input, Context, Output>`
2. 使用 `BusinessLinkedList` 或自定义 `ChainBuilder` 组装链
3. 通过 `DynamicContext` 在节点间传递中间状态
4. 在工厂类中使用 `@Bean` 方法组装并注入

**何时使用**: 多条件校验、审批流程、风控规则链。

### 5.3 模板方法模式

**场景**: 算法骨架固定，某些步骤留给子类差异化实现。

**实现模式**:
1. 抽象类中 `public final` 方法定义骨架
2. `protected abstract` 定义钩子方法
3. 子类实现钩子方法

**何时使用**: 同一流程有多个变体（如退款/发货/对账等）。

### 5.4 工厂模式

**场景**: 集中创建复杂对象或组装对象图。

**实现模式**:
- 责任链的 `*FilterFactory` 使用 `@Bean` 方法组装链
- 策略选择器的 `*StrategyFactory` 使用 `@Bean` 方法返回策略 Map
- Aggregate 的静态工厂方法表达创建意图

### 5.5 状态/策略组合

**场景**: 行为依赖多个状态维度的组合。

**实现模式**:
1. 定义状态枚举矩阵
2. 每种状态组合映射到对应策略
3. 在枚举中提供 `getStrategy()` 工厂方法

**何时使用**: 订单状态流转、退款场景、工作流引擎。

### 5.6 聚合模式

**场景**: 确保多个关联对象在同一个事务边界内保持一致。

**设计要点**:
- 一个事务只修改一个聚合实例
- 聚合根通过 ID 引用其他聚合，不持有对象引用
- 通过 Repository 整体加载、整体持久化
- 小聚合优于大聚合（按一致性边界划分，非领域概念边界）

---

## 6. 反模式警示

### 6.1 十条禁止事项

| # | 反模式 | 正确做法 | 原因 |
|---|--------|----------|------|
| 1 | 领域服务上加事务注解 | 在仓储实现层加 | 事务是基础设施关注点 |
| 2 | 使用 `@Autowired` | 使用 `@Resource` | 按名称装配更稳定 |
| 3 | 使用 Bean Validation 注解 | 手动校验 | 保持校验逻辑显式可控 |
| 4 | 数据库自增 ID 作为业务标识 | 业务层生成 ID | 分布式兼容 |
| 5 | 创建单独的 Converter/Assembler 类 | Repository 中内联 Builder 转换 | 避免过度抽象 |
| 6 | 枚举 valueOf 返回 null | 抛出异常 | 尽早暴露数据错误 |
| 7 | 领域实体引入框架注解 | 纯 POJO | 领域层零框架依赖 |
| 8 | 领域模块导入基础设施类 | 遵循依赖倒置 | 六边形架构的铁律 |
| 9 | 绕过统一 Response 返回原始类型 | 始终使用 Response 包装 | 统一 API 契约 |
| 10 | 大聚合（包含过多实体） | 按一致性边界拆分 | 避免性能问题和并发冲突 |

### 6.2 代码审查检查清单

- [ ] Controller 是否有完整的三层 catch？
- [ ] 写操作的 `@Transactional` 是否在 Repository 实现层面？
- [ ] 领域服务是否不含事务注解？
- [ ] PO ↔ Entity 转换是否用 Builder 且无字段遗漏？
- [ ] 枚举字段是否正确转换（PO 用 int/code，Entity 用枚举实例）？
- [ ] 定时任务是否有分布式锁且 finally 释放？
- [ ] MQ 监听器是否保证幂等性？
- [ ] 所有 DI 是否使用 `@Resource`？
- [ ] 领域实体是否不含 Spring 或 ORM 注解？

---

## 7. 模板库

> 以下模板基于 Java + Spring Boot + Lombok + MyBatis 技术栈。使用时替换 `{...}` 占位符。

### 7.1 错误码枚举

```java
package {base}.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ErrorCode {
    // 系统通用: 0xxx
    SUCCESS("0000", "成功"),
    UNKNOWN("0001", "未知错误"),
    ILLEGAL_PARAM("0002", "非法参数"),
    DUP_KEY("0003", "唯一索引冲突"),
    UPDATE_ZERO("0004", "更新影响行数为0"),
    EXT_CALL_FAIL("0005", "外部调用异常"),
    RATE_LIMITED("0006", "请求频率限制"),

    // 业务错误: Exxxx（按领域分段）
    E0101("E0101", "业务规则描述"),
    ;

    private String code;
    private String info;
}
```

### 7.2 业务异常

```java
package {base}.types.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private String code;
    private String info;

    public BusinessException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.info = errorCode.getInfo();
    }

    public BusinessException(String code, String info) {
        this.code = code;
        this.info = info;
    }
}
```

### 7.3 统一响应

```java
package {base}.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private String code;
    private String info;
    private T data;

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code("0000").info("成功").data(data).build();
    }

    public static <T> Response<T> fail(String code, String info) {
        return Response.<T>builder().code(code).info(info).build();
    }
}
```

### 7.4 请求/响应 DTO

```java
// 请求 DTO
package {base}.api.dto;

import lombok.Data;

@Data
public class {Operation}RequestDTO {
    private String userId;
    // ... 其他字段
}
```

```java
// 响应 DTO
package {base}.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Operation}ResponseDTO {
    private String resultField;
}
```

### 7.5 API 接口

```java
package {base}.api;

import {base}.api.dto.{Operation}RequestDTO;
import {base}.api.dto.{Operation}ResponseDTO;
import {base}.api.response.Response;

public interface I{Feature}Service {
    Response<{Operation}ResponseDTO> {operation}({Operation}RequestDTO request);
}
```

### 7.6 领域实体

```java
package {base}.domain.{context}.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {EntityName}Entity {
    private String id;               // 业务主键
    private Long relatedId;
    private BigDecimal amount;
    private {StatusEnum}Enum status;  // 领域枚举
}
```

### 7.7 值对象

```java
package {base}.domain.{context}.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Name}VO {
    private Integer targetCount;
    private Integer completeCount;
}
```

### 7.8 领域枚举

```java
package {base}.domain.{context}.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum {Name}Enum {
    STATE_A(0, "状态A"),
    STATE_B(1, "状态B");

    private Integer code;
    private String info;

    public static {Name}Enum valueOf(Integer code) {
        for ({Name}Enum e : values()) {
            if (e.code.equals(code)) return e;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
```

### 7.9 聚合根

```java
package {base}.domain.{context}.model.aggregate;

import {base}.domain.{context}.model.entity.EntityA;
import {base}.domain.{context}.model.entity.EntityB;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Name}Aggregate {
    private EntityA entityA;
    private EntityB entityB;
    private Integer scalarValue;

    /** 静态工厂方法 */
    public static {Name}Aggregate createScenarioA(EntityA a, EntityB b) {
        {Name}Aggregate aggregate = new {Name}Aggregate();
        aggregate.setEntityA(a);
        aggregate.setEntityB(b);
        return aggregate;
    }
}
```

### 7.10 仓储接口（domain 层）

```java
package {base}.domain.{context}.adapter.repository;

import {base}.domain.{context}.model.aggregate.{Name}Aggregate;
import {base}.domain.{context}.model.entity.{Name}Entity;

public interface I{Context}Repository {
    {Name}Entity queryById(String id);
    {Name}Entity save({Name}Aggregate aggregate);
}
```

### 7.11 仓储实现（infrastructure 层）

```java
package {base}.infrastructure.adapter.repository;

import {base}.domain.{context}.adapter.repository.I{Context}Repository;
import {base}.domain.{context}.model.aggregate.{Name}Aggregate;
import {base}.domain.{context}.model.entity.{Name}Entity;
import {base}.domain.{context}.model.valobj.{Name}Enum;
import {base}.infrastructure.dao.I{Name}Dao;
import {base}.infrastructure.dao.po.{Name}PO;
import {base}.types.enums.ErrorCode;
import {base}.types.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;

@Slf4j
@Repository
public class {Context}Repository implements I{Context}Repository {

    @Resource
    private I{Name}Dao dao;

    @Override
    public {Name}Entity queryById(String id) {
        {Name}PO req = new {Name}PO();
        req.setId(Long.valueOf(id));
        {Name}PO result = dao.queryById(req);
        if (null == result) return null;

        // PO → Entity
        return {Name}Entity.builder()
                .id(String.valueOf(result.getId()))
                .amount(result.getAmount())
                .status({Name}Enum.valueOf(result.getStatus()))
                .build();
    }

    @Override
    @Transactional(timeout = 500)
    public {Name}Entity save({Name}Aggregate aggregate) {
        {Name}Entity entity = aggregate.getEntityA();

        // Entity → PO
        {Name}PO po = {Name}PO.builder()
                .amount(entity.getAmount())
                .status(entity.getStatus().getCode())
                .build();

        try {
            dao.insert(po);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DUP_KEY);
        }

        return {Name}Entity.builder()
                .id(String.valueOf(po.getId()))
                .amount(po.getAmount())
                .status({Name}Enum.valueOf(po.getStatus()))
                .build();
    }
}
```

### 7.12 ORM 映射器与持久化对象

```java
// DAO 接口
package {base}.infrastructure.dao;

import {base}.infrastructure.dao.po.{Name}PO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface I{Name}Dao {
    void insert({Name}PO po);
    {Name}PO queryById({Name}PO req);
    int update({Name}PO po);
}
```

```java
// PO
package {base}.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Name}PO {
    private Long id;           // 自增主键
    private String bizId;      // 业务标识
    private Integer status;     // 状态（int）
    private BigDecimal amount;
    private Date createTime;
    private Date updateTime;
}
```

### 7.13 领域服务

```java
package {base}.domain.{context}.service;

import {base}.domain.{context}.adapter.repository.I{Context}Repository;
import {base}.domain.{context}.model.entity.{Name}Entity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

@Slf4j
@Service
public class {Context}Service implements I{Context}Service {

    @Resource
    private I{Context}Repository repository;

    @Override
    public {Name}Entity execute({Name}Entity input) {
        log.info("执行业务操作: {}", input.getId());
        // 1. 业务规则校验
        // 2. 调用仓储
        // 3. 返回结果
        return repository.queryById(input.getId());
    }
}
```

### 7.14 Controller

```java
package {base}.trigger.http;

import {base}.api.I{Feature}Service;
import {base}.api.dto.{Operation}RequestDTO;
import {base}.api.dto.{Operation}ResponseDTO;
import {base}.api.response.Response;
import {base}.domain.{context}.service.I{Context}Service;
import {base}.types.enums.ErrorCode;
import {base}.types.exception.BusinessException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/{resource}/")
public class {Name}Controller implements I{Feature}Service {

    @Resource
    private I{Context}Service service;

    @PostMapping("{action}")
    @Override
    public Response<{Operation}ResponseDTO> {action}(@RequestBody {Operation}RequestDTO request) {
        try {
            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId())) {
                return Response.fail(ErrorCode.ILLEGAL_PARAM.getCode(),
                                     ErrorCode.ILLEGAL_PARAM.getInfo());
            }
            log.info("{操作}: {}", JSON.toJSONString(request));

            // 2. 业务处理
            // ...

            // 3. 返回
            return Response.success(...);
        } catch (BusinessException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ErrorCode.UNKNOWN.getCode(),
                                 ErrorCode.UNKNOWN.getInfo());
        }
    }
}
```

### 7.15 定时任务

```java
package {base}.trigger.job;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class {Name}Job {

    @Resource
    private RedissonClient redissonClient;

    @Scheduled(cron = "0 */5 * * * ?")
    public void exec() {
        RLock lock = redissonClient.getLock("{job_key}");
        try {
            if (!lock.tryLock(3, 60, TimeUnit.SECONDS)) {
                log.info("获取锁失败，跳过");
                return;
            }
            // 业务逻辑
        } catch (Exception e) {
            log.error("任务异常", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 7.16 消息监听器

```java
package {base}.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class {Name}Listener {

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue("${mq.queue.{name}}"),
        exchange = @Exchange(value = "${mq.exchange}", type = ExchangeTypes.TOPIC),
        key = "${mq.routing-key.{name}}"
    ))
    public void onMessage(String message) {
        log.info("收到消息: {}", message);
        try {
            // 幂等处理
        } catch (Exception e) {
            log.error("处理失败: {}", message, e);
            throw new RuntimeException(e); // 触发重投
        }
    }
}
```


