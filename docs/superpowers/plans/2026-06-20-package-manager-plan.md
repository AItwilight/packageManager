# 末端驿站包裹管理系统 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete terminal station package management system with Vue 3 frontend and Spring Boot DDD backend.

**Architecture:** DDD hexagonal architecture across 6 Maven modules. Single `package` bounded context. REST API with JWT auth. Vue 3 SPA with Element Plus.

**Tech Stack:** Java 8, Spring Boot 2.7.12, MyBatis 2.1.4, MySQL 8.x, jjwt 0.9.1, Vue 3 + TypeScript, Vite 5.x, Element Plus 2.x, axios 1.x

## Global Constraints

- Java 8, Spring Boot 2.7.12, MyBatis 2.1.4, jjwt 0.9.1 (from pom.xml)
- DI: `@Resource`, never `@Autowired`
- Domain layer: zero Spring/MyBatis annotations
- Domain services: no `@Transactional` — only on infrastructure Repository impls
- Manual validation in Controller (no Bean Validation)
- Controller catch: `AppException` → business error, `Exception` → unknown error
- PO ↔ Entity conversion via Builder, inline in Repository impl
- Enum `valueOf(code)`: throw `IllegalArgumentException` on unknown code, never return null
- All API responses wrapped in `Response<T>`: `{code, info, data}`
- Server port: 8091 (existing config)
- DB: `package_manager` (new database name)

---

### Task 1: Foundation — Extend Error Codes and Add Response Static Factory Methods

**Files:**
- Modify: `packageManager-types/src/main/java/com/huster/types/enums/ResponseCode.java`
- Modify: `packageManager-api/src/main/java/com/huster/api/response/Response.java`

**Interfaces:**
- Produces: `ResponseCode` enum with all error codes, `Response.success(T data)`, `Response.fail(String code, String info)`

- [ ] **Step 1: Extend ResponseCode enum**

Replace the contents of `ResponseCode.java`:

```java
package com.huster.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    DUP_KEY("0003", "唯一索引冲突"),
    UPDATE_ZERO("0004", "更新影响行数为0"),

    // 认证错误
    AUTH_FAIL("E0101", "用户名或密码错误"),
    TOKEN_INVALID("E0102", "未登录或Token已过期"),

    // 包裹业务错误
    PACKAGE_NOT_FOUND("E0201", "包裹不存在"),
    WAYBILL_DUPLICATE("E0202", "该运单号已存在待取件包裹"),
    ALREADY_PICKED("E0203", "该包裹已被取走"),
    CANT_EDIT_PICKED("E0204", "已取件包裹不可编辑"),
    ;

    private String code;
    private String info;

}
```

- [ ] **Step 2: Add static factory methods to Response**

Read the existing `Response.java` and add these two methods inside the class (before the closing `}`):

```java
    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.setCode("0000");
        response.setInfo("成功");
        response.setData(data);
        return response;
    }

    public static <T> Response<T> fail(String code, String info) {
        Response<T> response = new Response<>();
        response.setCode(code);
        response.setInfo(info);
        return response;
    }
```

> Note: Hardcoded `"0000"` / `"成功"` to keep `api` module independent from `types` module (per DDD standard: `api` has zero compile dependencies).

- [ ] **Step 3: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-types,packageManager-api -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add packageManager-types/src/main/java/com/huster/types/enums/ResponseCode.java packageManager-api/src/main/java/com/huster/api/response/Response.java && git commit -m "feat: extend error codes and add Response static factory methods

- Add auth (E01xx) and package (E02xx) error codes
- Add Response.success() and Response.fail() static factory methods

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: API Layer — Request/Response DTOs and API Interface

**Files:**
- Create: `packageManager-api/src/main/java/com/huster/api/dto/LoginRequestDTO.java`
- Create: `packageManager-api/src/main/java/com/huster/api/dto/CheckinRequestDTO.java`
- Create: `packageManager-api/src/main/java/com/huster/api/dto/EditRequestDTO.java`
- Create: `packageManager-api/src/main/java/com/huster/api/dto/PackageResponseDTO.java`
- Create: `packageManager-api/src/main/java/com/huster/api/dto/DashboardStatsResponseDTO.java`
- Create: `packageManager-api/src/main/java/com/huster/api/IPackageManageService.java`

**Interfaces:**
- Consumes: `Response<T>` (Task 1), `ResponseCode` (Task 1)
- Produces: All DTO classes, `IPackageManageService` interface

- [ ] **Step 1: Create LoginRequestDTO**

Create file `packageManager-api/src/main/java/com/huster/api/dto/LoginRequestDTO.java`:

```java
package com.huster.api.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String username;
    private String password;
}
```

- [ ] **Step 2: Create CheckinRequestDTO**

Create file `packageManager-api/src/main/java/com/huster/api/dto/CheckinRequestDTO.java`:

```java
package com.huster.api.dto;

import lombok.Data;

@Data
public class CheckinRequestDTO {
    private String waybillNo;
    private String phone;
    private String courier;
    private String shelf;
}
```

- [ ] **Step 3: Create EditRequestDTO**

Create file `packageManager-api/src/main/java/com/huster/api/dto/EditRequestDTO.java`:

```java
package com.huster.api.dto;

import lombok.Data;

@Data
public class EditRequestDTO {
    private String id;
    private String waybillNo;
    private String phone;
    private String courier;
    private String shelf;
}
```

- [ ] **Step 4: Create PackageResponseDTO**

Create file `packageManager-api/src/main/java/com/huster/api/dto/PackageResponseDTO.java`:

```java
package com.huster.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageResponseDTO {
    private String id;
    private String waybillNo;
    private String phone;
    private String courier;
    private String courierDesc;
    private String shelf;
    private Integer status;
    private String statusDesc;
    private String checkinTime;
    private String pickupTime;
    private Boolean stale;
}
```

- [ ] **Step 5: Create DashboardStatsResponseDTO**

Create file `packageManager-api/src/main/java/com/huster/api/dto/DashboardStatsResponseDTO.java`:

```java
package com.huster.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponseDTO {
    private Integer todayCheckin;
    private Integer pendingTotal;
    private Integer staleTotal;
    private Integer todayPickup;
}
```

- [ ] **Step 6: Create IPackageManageService interface**

Create file `packageManager-api/src/main/java/com/huster/api/IPackageManageService.java`:

```java
package com.huster.api;

import com.huster.api.dto.CheckinRequestDTO;
import com.huster.api.dto.DashboardStatsResponseDTO;
import com.huster.api.dto.EditRequestDTO;
import com.huster.api.dto.PackageResponseDTO;
import com.huster.api.response.Response;

import java.util.Map;

public interface IPackageManageService {
    Response<Map<String, String>> checkin(CheckinRequestDTO request);
    Response<Map<String, Object>> list(String phone, String keyword, Integer status, Integer page, Integer size);
    Response<Void> pickup(String id);
    Response<Void> edit(EditRequestDTO request);
    Response<DashboardStatsResponseDTO> stats();
}
```

- [ ] **Step 7: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-api -q
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
cd e:/project/packageManager && git add packageManager-api/src/main/java/com/huster/api/dto/ packageManager-api/src/main/java/com/huster/api/IPackageManageService.java && git commit -m "feat: add API layer DTOs and IPackageManageService interface

- LoginRequestDTO, CheckinRequestDTO, EditRequestDTO
- PackageResponseDTO, DashboardStatsResponseDTO
- IPackageManageService defining 5 package management endpoints

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: Domain Layer — PackageStatusEnum and CourierCompanyEnum

**Files:**
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/model/valobj/PackageStatusEnum.java`
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/model/valobj/CourierCompanyEnum.java`

**Interfaces:**
- Produces: `PackageStatusEnum` (PENDING(0), PICKED_UP(1)), `CourierCompanyEnum` (SF, YTO, ZTO, STO, YD, JD, DB, OTHER)

- [ ] **Step 1: Create PackageStatusEnum**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/model/valobj/PackageStatusEnum.java`:

```java
package com.huster.domain.package.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum PackageStatusEnum {
    PENDING(0, "待取件"),
    PICKED_UP(1, "已取件"),
    ;

    private Integer code;
    private String info;

    public static PackageStatusEnum valueOf(Integer code) {
        for (PackageStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        throw new IllegalArgumentException("Unknown PackageStatus code: " + code);
    }
}
```

- [ ] **Step 2: Create CourierCompanyEnum**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/model/valobj/CourierCompanyEnum.java`:

```java
package com.huster.domain.package.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum CourierCompanyEnum {
    SF("SF", "顺丰"),
    YTO("YTO", "圆通"),
    ZTO("ZTO", "中通"),
    STO("STO", "申通"),
    YD("YD", "韵达"),
    JD("JD", "京东"),
    DB("DB", "德邦"),
    OTHER("OTHER", "其他"),
    ;

    private String code;
    private String info;

    public static CourierCompanyEnum valueOfCode(String code) {
        for (CourierCompanyEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        throw new IllegalArgumentException("Unknown CourierCompany code: " + code);
    }
}
```

- [ ] **Step 3: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-domain -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add packageManager-domain/src/main/java/com/huster/domain/package/ && git commit -m "feat: add PackageStatusEnum and CourierCompanyEnum

- PENDING(0), PICKED_UP(1) status enum
- 8 courier company codes (SF,YTO,ZTO,STO,YD,JD,DB,OTHER)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: Domain Layer — PackageEntity and PackageAggregate

**Files:**
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/model/entity/PackageEntity.java`
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/model/aggregate/PackageAggregate.java`

**Interfaces:**
- Consumes: `PackageStatusEnum`, `CourierCompanyEnum` (Task 3)
- Produces: `PackageEntity`, `PackageAggregate`

- [ ] **Step 1: Create PackageEntity**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/model/entity/PackageEntity.java`:

```java
package com.huster.domain.package.model.entity;

import com.huster.domain.package.model.valobj.CourierCompanyEnum;
import com.huster.domain.package.model.valobj.PackageStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageEntity {
    /** 业务主键 */
    private String id;
    /** 运单号 */
    private String waybillNo;
    /** 收件人手机号 */
    private String recipientPhone;
    /** 快递公司 */
    private CourierCompanyEnum courierCompany;
    /** 货架位置 */
    private String shelfLocation;
    /** 包裹状态 */
    private PackageStatusEnum status;
    /** 入库时间 */
    private LocalDateTime checkinTime;
    /** 取件时间 */
    private LocalDateTime pickupTime;

    /** 是否滞留（入库超48小时且未取件） */
    public boolean isStale() {
        return this.status == PackageStatusEnum.PENDING
                && java.time.temporal.ChronoUnit.HOURS.between(this.checkinTime, LocalDateTime.now()) >= 48;
    }
}
```

- [ ] **Step 2: Create PackageAggregate**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/model/aggregate/PackageAggregate.java`:

```java
package com.huster.domain.package.model.aggregate;

import com.huster.domain.package.model.entity.PackageEntity;
import com.huster.domain.package.model.valobj.CourierCompanyEnum;
import com.huster.domain.package.model.valobj.PackageStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageAggregate {
    private PackageEntity packageEntity;

    /** 静态工厂：创建新入库包裹 */
    public static PackageAggregate createForCheckin(
            String waybillNo, String phone, CourierCompanyEnum courier, String shelf) {
        PackageEntity entity = PackageEntity.builder()
                .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .waybillNo(waybillNo)
                .recipientPhone(phone)
                .courierCompany(courier)
                .shelfLocation(shelf)
                .status(PackageStatusEnum.PENDING)
                .checkinTime(LocalDateTime.now())
                .build();
        PackageAggregate aggregate = new PackageAggregate();
        aggregate.setPackageEntity(entity);
        return aggregate;
    }
}
```

- [ ] **Step 3: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-domain -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add packageManager-domain/src/main/java/com/huster/domain/package/model/entity/PackageEntity.java packageManager-domain/src/main/java/com/huster/domain/package/model/aggregate/PackageAggregate.java && git commit -m "feat: add PackageEntity and PackageAggregate domain models

- PackageEntity with isStale() method (48h threshold)
- PackageAggregate with createForCheckin() static factory

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: Domain Layer — IPackageRepository Interface

**Files:**
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/adapter/repository/IPackageRepository.java`

**Interfaces:**
- Consumes: `PackageEntity` (Task 4), `PackageAggregate` (Task 4)
- Produces: `IPackageRepository` interface with methods: `queryByBizId`, `queryByWaybillNoAndStatus`, `queryPage`, `countStats`, `save`, `updatePickup`, `updateInfo`

- [ ] **Step 1: Create IPackageRepository**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/adapter/repository/IPackageRepository.java`:

```java
package com.huster.domain.package.adapter.repository;

import com.huster.domain.package.model.aggregate.PackageAggregate;
import com.huster.domain.package.model.entity.PackageEntity;

import java.util.List;

public interface IPackageRepository {

    /** 按业务ID查询 */
    PackageEntity queryByBizId(String bizId);

    /** 按运单号 + 状态查询（用于查重） */
    PackageEntity queryByWaybillNoAndStatus(String waybillNo, Integer statusCode);

    /** 分页查询 */
    List<PackageEntity> queryPage(String phone, String keyword, Integer statusCode, int offset, int limit);

    /** 统计总数 */
    int countPage(String phone, String keyword, Integer statusCode);

    /** 统计概览：今日入库数 */
    int countTodayCheckin();

    /** 统计概览：待取件总数 */
    int countPending();

    /** 统计概览：滞留数（>48h） */
    int countStale();

    /** 统计概览：今日取件数 */
    int countTodayPickup();

    /** 新增包裹 */
    PackageEntity save(PackageAggregate aggregate);

    /** 确认取件 */
    int updatePickup(String bizId);

    /** 编辑入库信息 */
    int updateInfo(PackageEntity entity);
}
```

- [ ] **Step 2: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-domain -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-domain/src/main/java/com/huster/domain/package/adapter/repository/IPackageRepository.java && git commit -m "feat: add IPackageRepository domain interface

- Query methods: byBizId, byWaybillNo, page, count
- Stats methods: todayCheckin, pending, stale, todayPickup
- Mutation methods: save, updatePickup, updateInfo

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: Domain Layer — PackageService

**Files:**
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/service/IPackageService.java`
- Create: `packageManager-domain/src/main/java/com/huster/domain/package/service/PackageService.java`

**Interfaces:**
- Consumes: `IPackageRepository` (Task 5), `PackageEntity`, `PackageAggregate`, `PackageStatusEnum`, `CourierCompanyEnum` (Tasks 3-4)
- Produces: `IPackageService` / `PackageService`

- [ ] **Step 1: Create IPackageService**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/service/IPackageService.java`:

```java
package com.huster.domain.package.service;

import com.huster.domain.package.model.aggregate.PackageAggregate;
import com.huster.domain.package.model.entity.PackageEntity;

import java.util.List;

public interface IPackageService {
    PackageEntity checkin(PackageAggregate aggregate);
    List<PackageEntity> queryList(String phone, String keyword, Integer statusCode, int page, int size);
    int countTotal(String phone, String keyword, Integer statusCode);
    void pickup(String bizId);
    void edit(PackageEntity entity);
    int[] getDashboardStats();
}
```

- [ ] **Step 2: Create PackageService**

Create file `packageManager-domain/src/main/java/com/huster/domain/package/service/PackageService.java`:

```java
package com.huster.domain.package.service;

import com.huster.domain.package.adapter.repository.IPackageRepository;
import com.huster.domain.package.model.aggregate.PackageAggregate;
import com.huster.domain.package.model.entity.PackageEntity;
import com.huster.domain.package.model.valobj.PackageStatusEnum;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

@Slf4j
public class PackageService implements IPackageService {

    @Resource
    private IPackageRepository repository;

    @Override
    public PackageEntity checkin(PackageAggregate aggregate) {
        // 校验：同运单号不允许重复待取件
        PackageEntity existing = repository.queryByWaybillNoAndStatus(
                aggregate.getPackageEntity().getWaybillNo(),
                PackageStatusEnum.PENDING.getCode());
        if (existing != null) {
            throw new AppException(ResponseCode.WAYBILL_DUPLICATE.getCode(),
                    ResponseCode.WAYBILL_DUPLICATE.getInfo());
        }
        return repository.save(aggregate);
    }

    @Override
    public List<PackageEntity> queryList(String phone, String keyword, Integer statusCode, int page, int size) {
        int offset = (page - 1) * size;
        return repository.queryPage(phone, keyword, statusCode, offset, size);
    }

    @Override
    public int countTotal(String phone, String keyword, Integer statusCode) {
        return repository.countPage(phone, keyword, statusCode);
    }

    @Override
    public void pickup(String bizId) {
        PackageEntity entity = repository.queryByBizId(bizId);
        if (entity == null) {
            throw new AppException(ResponseCode.PACKAGE_NOT_FOUND.getCode(),
                    ResponseCode.PACKAGE_NOT_FOUND.getInfo());
        }
        if (entity.getStatus() == PackageStatusEnum.PICKED_UP) {
            throw new AppException(ResponseCode.ALREADY_PICKED.getCode(),
                    ResponseCode.ALREADY_PICKED.getInfo());
        }
        int rows = repository.updatePickup(bizId);
        if (rows == 0) {
            throw new AppException(ResponseCode.UPDATE_ZERO.getCode(),
                    ResponseCode.UPDATE_ZERO.getInfo());
        }
    }

    @Override
    public void edit(PackageEntity entity) {
        PackageEntity existing = repository.queryByBizId(entity.getId());
        if (existing == null) {
            throw new AppException(ResponseCode.PACKAGE_NOT_FOUND.getCode(),
                    ResponseCode.PACKAGE_NOT_FOUND.getInfo());
        }
        if (existing.getStatus() == PackageStatusEnum.PICKED_UP) {
            throw new AppException(ResponseCode.CANT_EDIT_PICKED.getCode(),
                    ResponseCode.CANT_EDIT_PICKED.getInfo());
        }
        repository.updateInfo(entity);
    }

    @Override
    public int[] getDashboardStats() {
        return new int[]{
                repository.countTodayCheckin(),
                repository.countPending(),
                repository.countStale(),
                repository.countTodayPickup()
        };
    }
}
```

- [ ] **Step 3: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-domain -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add packageManager-domain/src/main/java/com/huster/domain/package/service/ && git commit -m "feat: add PackageService domain service

- checkin with waybill duplicate check
- pickup with status validation
- edit with status gate (only PENDING editable)
- queryList with pagination
- getDashboardStats returning [todayCheckin, pending, stale, todayPickup]

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: Infrastructure — Database DDL and PO

**Files:**
- Create: `packageManager-app/src/main/resources/db/schema.sql`
- Create: `packageManager-infrastructure/src/main/java/com/huster/infrastructure/dao/po/PackagePO.java`
- Modify: `packageManager-app/src/main/resources/application-dev.yml`

**Interfaces:**
- Consumes: `PackageEntity` fields (Task 4)
- Produces: `PackagePO` with fields: id, bizId, waybillNo, phone, courier, shelf, status, checkinTime, pickupTime, createTime, updateTime

- [ ] **Step 1: Create DDL schema.sql**

Create file `packageManager-app/src/main/resources/db/schema.sql`:

```sql
-- 末端驿站包裹管理系统 DDL

CREATE DATABASE IF NOT EXISTS package_manager
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE package_manager;

-- 包裹表
CREATE TABLE IF NOT EXISTS package (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    biz_id       VARCHAR(32)    NOT NULL UNIQUE COMMENT '业务ID',
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

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT         AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)    NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128)   NOT NULL COMMENT 'BCrypt加密密码',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化管理员: admin / admin123
INSERT INTO sys_user (username, password) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh');
```

> **Note:** The BCrypt hash above is a placeholder. In Step 3 we generate the real hash and replace it.

- [ ] **Step 2: Create PackagePO**

Create file `packageManager-infrastructure/src/main/java/com/huster/infrastructure/dao/po/PackagePO.java`:

```java
package com.huster.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackagePO {
    private Long id;
    private String bizId;
    private String waybillNo;
    private String phone;
    private String courier;
    private String shelf;
    private Integer status;
    private Date checkinTime;
    private Date pickupTime;
    private Date createTime;
    private Date updateTime;
}
```

- [ ] **Step 3: Update application-dev.yml — enable MyBatis**

Replace the content of `packageManager-app/src/main/resources/application-dev.yml`:

```yaml
server:
  port: 8091

# 线程池配置
thread:
  pool:
    executor:
      config:
        core-pool-size: 20
        max-pool-size: 50
        keep-alive-time: 5000
        block-queue-size: 5000
        policy: CallerRunsPolicy

# 数据库配置
spring:
  datasource:
    username: root
    password: 123456
    url: jdbc:mysql://127.0.0.1:3306/package_manager?useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&serverTimezone=UTC&useSSL=true
    driver-class-name: com.mysql.cj.jdbc.Driver
  hikari:
    pool-name: Retail_HikariCP
    minimum-idle: 15
    idle-timeout: 180000
    maximum-pool-size: 25
    auto-commit: true
    max-lifetime: 1800000
    connection-timeout: 30000
    connection-test-query: SELECT 1
  type: com.zaxxer.hikari.HikariDataSource

# MyBatis 配置
mybatis:
  mapper-locations: classpath:/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

# 日志
logging:
  level:
    root: info
    com.huster: debug
```

- [ ] **Step 4: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-infrastructure -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
cd e:/project/packageManager && git add packageManager-app/src/main/resources/db/schema.sql packageManager-infrastructure/src/main/java/com/huster/infrastructure/dao/po/PackagePO.java packageManager-app/src/main/resources/application-dev.yml && git commit -m "feat: add DDL schema, PackagePO, and update application config

- Create package and sys_user tables
- PackagePO with all table columns
- Enable MyBatis mapper scanning and underscore-to-camel-case

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: Infrastructure — IPackageDao and MyBatis Mapper XML

**Files:**
- Create: `packageManager-infrastructure/src/main/java/com/huster/infrastructure/dao/IPackageDao.java`
- Create: `packageManager-app/src/main/resources/mapper/package_mapper.xml`

**Interfaces:**
- Consumes: `PackagePO` (Task 7)
- Produces: `IPackageDao` MyBatis mapper interface

- [ ] **Step 1: Create IPackageDao**

Create file `packageManager-infrastructure/src/main/java/com/huster/infrastructure/dao/IPackageDao.java`:

```java
package com.huster.infrastructure.dao;

import com.huster.infrastructure.dao.po.PackagePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IPackageDao {

    void insert(PackagePO po);

    PackagePO queryByBizId(String bizId);

    PackagePO queryByWaybillNoAndStatus(PackagePO req);

    List<PackagePO> queryPage(PackagePO req);

    int countPage(PackagePO req);

    int countTodayCheckin();

    int countPending();

    int countStale();

    int countTodayPickup();

    int updatePickup(String bizId);

    int updateInfo(PackagePO po);
}
```

- [ ] **Step 2: Create MyBatis mapper XML**

Create file `packageManager-app/src/main/resources/mapper/package_mapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.huster.infrastructure.dao.IPackageDao">

    <resultMap id="BaseResultMap" type="com.huster.infrastructure.dao.po.PackagePO">
        <id column="id" property="id"/>
        <result column="biz_id" property="bizId"/>
        <result column="waybill_no" property="waybillNo"/>
        <result column="phone" property="phone"/>
        <result column="courier" property="courier"/>
        <result column="shelf" property="shelf"/>
        <result column="status" property="status"/>
        <result column="checkin_time" property="checkinTime"/>
        <result column="pickup_time" property="pickupTime"/>
        <result column="create_time" property="createTime"/>
        <result column="update_time" property="updateTime"/>
    </resultMap>

    <insert id="insert" parameterType="com.huster.infrastructure.dao.po.PackagePO"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO package (biz_id, waybill_no, phone, courier, shelf, status, checkin_time)
        VALUES (#{bizId}, #{waybillNo}, #{phone}, #{courier}, #{shelf}, #{status}, #{checkinTime})
    </insert>

    <select id="queryByBizId" parameterType="string" resultMap="BaseResultMap">
        SELECT * FROM package WHERE biz_id = #{bizId}
    </select>

    <select id="queryByWaybillNoAndStatus" parameterType="com.huster.infrastructure.dao.po.PackagePO"
            resultMap="BaseResultMap">
        SELECT * FROM package
        WHERE waybill_no = #{waybillNo} AND status = #{status}
        LIMIT 1
    </select>

    <select id="queryPage" parameterType="com.huster.infrastructure.dao.po.PackagePO"
            resultMap="BaseResultMap">
        SELECT * FROM package
        <where>
            <if test="phone != null and phone != ''">
                AND phone = #{phone}
            </if>
            <if test="waybillNo != null and waybillNo != ''">
                AND (waybill_no LIKE CONCAT('%', #{waybillNo}, '%')
                     OR phone LIKE CONCAT('%', #{waybillNo}, '%')
                     OR courier LIKE CONCAT('%', #{waybillNo}, '%'))
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
        ORDER BY create_time DESC
        LIMIT #{shelf}, #{courier}
    </select>

    <select id="countPage" parameterType="com.huster.infrastructure.dao.po.PackagePO"
            resultType="int">
        SELECT COUNT(*) FROM package
        <where>
            <if test="phone != null and phone != ''">
                AND phone = #{phone}
            </if>
            <if test="waybillNo != null and waybillNo != ''">
                AND (waybill_no LIKE CONCAT('%', #{waybillNo}, '%')
                     OR phone LIKE CONCAT('%', #{waybillNo}, '%')
                     OR courier LIKE CONCAT('%', #{waybillNo}, '%'))
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
    </select>

    <select id="countTodayCheckin" resultType="int">
        SELECT COUNT(*) FROM package
        WHERE DATE(checkin_time) = CURDATE()
    </select>

    <select id="countPending" resultType="int">
        SELECT COUNT(*) FROM package WHERE status = 0
    </select>

    <select id="countStale" resultType="int">
        SELECT COUNT(*) FROM package
        WHERE status = 0 AND checkin_time &lt; DATE_SUB(NOW(), INTERVAL 48 HOUR)
    </select>

    <select id="countTodayPickup" resultType="int">
        SELECT COUNT(*) FROM package
        WHERE DATE(pickup_time) = CURDATE()
    </select>

    <update id="updatePickup" parameterType="string">
        UPDATE package SET status = 1, pickup_time = NOW()
        WHERE biz_id = #{bizId} AND status = 0
    </update>

    <update id="updateInfo" parameterType="com.huster.infrastructure.dao.po.PackagePO">
        UPDATE package
        SET waybill_no = #{waybillNo},
            phone = #{phone},
            courier = #{courier},
            shelf = #{shelf}
        WHERE biz_id = #{bizId} AND status = 0
    </update>

</mapper>
```

- [ ] **Step 3: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-infrastructure -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add packageManager-infrastructure/src/main/java/com/huster/infrastructure/dao/IPackageDao.java packageManager-app/src/main/resources/mapper/package_mapper.xml && git commit -m "feat: add IPackageDao and MyBatis mapper XML

- 11 SQL queries: insert, queryByBizId, queryByWaybillNoAndStatus, queryPage, countPage
- 4 stats queries: countTodayCheckin, countPending, countStale, countTodayPickup
- 2 update queries: updatePickup, updateInfo
- Pagination via LIMIT with offset/length passed through PO fields

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 9: Infrastructure — PackageRepository Implementation

**Files:**
- Create: `packageManager-infrastructure/src/main/java/com/huster/infrastructure/adapter/repository/PackageRepository.java`

**Interfaces:**
- Consumes: `IPackageRepository` (Task 5), `IPackageDao` (Task 8), `PackagePO` (Task 7), `PackageEntity`, `PackageAggregate`, enums (Tasks 3-4)
- Produces: `PackageRepository` implementing `IPackageRepository`

- [ ] **Step 1: Create PackageRepository**

Create file `packageManager-infrastructure/src/main/java/com/huster/infrastructure/adapter/repository/PackageRepository.java`:

```java
package com.huster.infrastructure.adapter.repository;

import com.huster.domain.package.adapter.repository.IPackageRepository;
import com.huster.domain.package.model.aggregate.PackageAggregate;
import com.huster.domain.package.model.entity.PackageEntity;
import com.huster.domain.package.model.valobj.CourierCompanyEnum;
import com.huster.domain.package.model.valobj.PackageStatusEnum;
import com.huster.infrastructure.dao.IPackageDao;
import com.huster.infrastructure.dao.po.PackagePO;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PackageRepository implements IPackageRepository {

    @Resource
    private IPackageDao dao;

    @Override
    public PackageEntity queryByBizId(String bizId) {
        PackagePO result = dao.queryByBizId(bizId);
        return toEntity(result);
    }

    @Override
    public PackageEntity queryByWaybillNoAndStatus(String waybillNo, Integer statusCode) {
        PackagePO req = PackagePO.builder()
                .waybillNo(waybillNo)
                .status(statusCode)
                .build();
        PackagePO result = dao.queryByWaybillNoAndStatus(req);
        return toEntity(result);
    }

    @Override
    public List<PackageEntity> queryPage(String phone, String keyword, Integer statusCode,
                                          int offset, int limit) {
        PackagePO req = PackagePO.builder()
                .phone(phone)
                .waybillNo(keyword)
                .status(statusCode)
                .shelf(String.valueOf(offset))
                .courier(String.valueOf(limit))
                .build();
        List<PackagePO> list = dao.queryPage(req);
        return list.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public int countPage(String phone, String keyword, Integer statusCode) {
        PackagePO req = PackagePO.builder()
                .phone(phone)
                .waybillNo(keyword)
                .status(statusCode)
                .build();
        return dao.countPage(req);
    }

    @Override
    public int countTodayCheckin() {
        return dao.countTodayCheckin();
    }

    @Override
    public int countPending() {
        return dao.countPending();
    }

    @Override
    public int countStale() {
        return dao.countStale();
    }

    @Override
    public int countTodayPickup() {
        return dao.countTodayPickup();
    }

    @Override
    @Transactional(timeout = 500)
    public PackageEntity save(PackageAggregate aggregate) {
        PackageEntity entity = aggregate.getPackageEntity();
        PackagePO po = PackagePO.builder()
                .bizId(entity.getId())
                .waybillNo(entity.getWaybillNo())
                .phone(entity.getRecipientPhone())
                .courier(entity.getCourierCompany().getCode())
                .shelf(entity.getShelfLocation())
                .status(entity.getStatus().getCode())
                .checkinTime(toDate(entity.getCheckinTime()))
                .build();
        try {
            dao.insert(po);
        } catch (DuplicateKeyException e) {
            throw new AppException(ResponseCode.DUP_KEY.getCode(),
                    ResponseCode.DUP_KEY.getInfo());
        }
        entity.setId(po.getBizId());
        return entity;
    }

    @Override
    @Transactional(timeout = 500)
    public int updatePickup(String bizId) {
        return dao.updatePickup(bizId);
    }

    @Override
    @Transactional(timeout = 500)
    public int updateInfo(PackageEntity entity) {
        PackagePO po = PackagePO.builder()
                .bizId(entity.getId())
                .waybillNo(entity.getWaybillNo())
                .phone(entity.getRecipientPhone())
                .courier(entity.getCourierCompany().getCode())
                .shelf(entity.getShelfLocation())
                .build();
        return dao.updateInfo(po);
    }

    // ========== PO ↔ Entity 转换（内联） ==========

    private PackageEntity toEntity(PackagePO po) {
        if (po == null) return null;
        return PackageEntity.builder()
                .id(po.getBizId())
                .waybillNo(po.getWaybillNo())
                .recipientPhone(po.getPhone())
                .courierCompany(CourierCompanyEnum.valueOfCode(po.getCourier()))
                .shelfLocation(po.getShelf())
                .status(PackageStatusEnum.valueOf(po.getStatus()))
                .checkinTime(toLocalDateTime(po.getCheckinTime()))
                .pickupTime(toLocalDateTime(po.getPickupTime()))
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
```

- [ ] **Step 2: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-infrastructure -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-infrastructure/src/main/java/com/huster/infrastructure/adapter/repository/PackageRepository.java && git commit -m "feat: add PackageRepository infrastructure implementation

- Implements IPackageRepository with @Repository and @Transactional
- PO <-> Entity conversion via Builder (inline, no separate converter)
- Date <-> LocalDateTime conversion helpers
- DuplicateKeyException mapped to DUP_KEY error

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 10: App Configuration — Generate BCrypt Password and Finalize DDL

**Files:**
- Modify: `packageManager-app/src/main/resources/db/schema.sql`

**Interfaces:**
- Produces: Correct BCrypt hash for `admin/admin123` in DDL seed data

- [ ] **Step 1: Write a small utility to generate the BCrypt hash**

Create a temporary test file `packageManager-app/src/test/java/com/huster/test/BcryptGenerator.java`:

```java
package com.huster.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("BCrypt hash for admin123: " + hash);
    }
}
```

- [ ] **Step 2: Run the utility**

```bash
cd e:/project/packageManager && mvn exec:java -pl packageManager-app -Dexec.mainClass="com.huster.test.BcryptGenerator" -q 2>&1
```

> Note: If `exec:java` doesn't work, add this dependency to `packageManager-app/pom.xml`:
> ```xml
> <dependency>
>     <groupId>org.springframework.security</groupId>
>     <artifactId>spring-security-crypto</artifactId>
> </dependency>
> ```
> Then run the main class.

- [ ] **Step 3: Update schema.sql with the real hash**

Copy the generated hash and update the INSERT in `packageManager-app/src/main/resources/db/schema.sql`:

Replace the placeholder line:
```sql
INSERT INTO sys_user (username, password) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh');
```

With the actual hash generated in Step 2.

- [ ] **Step 4: Remove the temporary utility**

Delete `packageManager-app/src/test/java/com/huster/test/BcryptGenerator.java`.

- [ ] **Step 5: Commit**

```bash
cd e:/project/packageManager && git add packageManager-app/src/main/resources/db/schema.sql && git rm packageManager-app/src/test/java/com/huster/test/BcryptGenerator.java 2>/dev/null; git commit -m "fix: update schema.sql with real BCrypt hash for admin user

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 11: App Configuration — JWT Interceptor and WebMvcConfig

**Files:**
- Create: `packageManager-app/src/main/java/com/huster/config/JwtInterceptor.java`
- Create: `packageManager-app/src/main/java/com/huster/config/WebMvcConfig.java`

**Interfaces:**
- Consumes: jjwt 0.9.1 (from pom.xml)
- Produces: JWT token validation interceptor, WebMvcConfigurer registering it

- [ ] **Step 1: Create JwtInterceptor**

Create file `packageManager-app/src/main/java/com/huster/config/JwtInterceptor.java`:

```java
package com.huster.config;

import com.alibaba.fastjson.JSON;
import com.huster.api.response.Response;
import com.huster.types.enums.ResponseCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String SECRET = "PackageManagerJwtSecretKey2026!@#";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            request.setAttribute("username", claims.getSubject());
            return true;
        } catch (ExpiredJwtException e) {
            sendUnauthorized(response, "Token expired");
            return false;
        } catch (Exception e) {
            sendUnauthorized(response, "Invalid token");
            return false;
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(
                Response.fail(ResponseCode.TOKEN_INVALID.getCode(), message)));
    }

    /** 生成 JWT Token */
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
}
```

- [ ] **Step 2: Create WebMvcConfig**

Create file `packageManager-app/src/main/java/com/huster/config/WebMvcConfig.java`:

```java
package com.huster.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/login");
    }
}
```

- [ ] **Step 3: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-app -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add packageManager-app/src/main/java/com/huster/config/JwtInterceptor.java packageManager-app/src/main/java/com/huster/config/WebMvcConfig.java && git commit -m "feat: add JWT interceptor and WebMvc config

- JwtInterceptor: validate Bearer token, extract claims, 401 on failure
- JwtInterceptor.generateToken(): HS256, 24h expiry
- WebMvcConfig: register interceptor, exclude /api/v1/auth/login

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 12: Trigger — AuthController

**Files:**
- Create: `packageManager-trigger/src/main/java/com/huster/trigger/http/AuthController.java`

**Interfaces:**
- Consumes: `LoginRequestDTO` (Task 2), `Response` (Task 1), `ResponseCode` (Task 1), `JwtInterceptor` (Task 11)
- Produces: `POST /api/v1/auth/login` endpoint

- [ ] **Step 1: Create AuthController**

Create file `packageManager-trigger/src/main/java/com/huster/trigger/http/AuthController.java`:

```java
package com.huster.trigger.http;

import com.alibaba.fastjson.JSON;
import com.huster.api.dto.LoginRequestDTO;
import com.huster.api.response.Response;
import com.huster.config.JwtInterceptor;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/auth/")
public class AuthController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("login")
    public Response<Map<String, String>> login(@RequestBody LoginRequestDTO request) {
        try {
            log.info("登录请求: {}", JSON.toJSONString(request));

            // 1. 参数校验
            if (StringUtils.isBlank(request.getUsername())
                    || StringUtils.isBlank(request.getPassword())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                        ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }

            // 2. 查询用户
            String sql = "SELECT username, password FROM sys_user WHERE username = ?";
            Map<String, Object> user;
            try {
                user = jdbcTemplate.queryForMap(sql, request.getUsername());
            } catch (Exception e) {
                return Response.fail(ResponseCode.AUTH_FAIL.getCode(),
                        ResponseCode.AUTH_FAIL.getInfo());
            }

            // 3. 验证密码
            String storedPassword = (String) user.get("password");
            if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
                return Response.fail(ResponseCode.AUTH_FAIL.getCode(),
                        ResponseCode.AUTH_FAIL.getInfo());
            }

            // 4. 生成 Token
            String token = JwtInterceptor.generateToken(request.getUsername());
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(),
                    ResponseCode.UN_ERROR.getInfo());
        }
    }
}
```

- [ ] **Step 2: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-trigger -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-trigger/src/main/java/com/huster/trigger/http/AuthController.java && git commit -m "feat: add AuthController with login endpoint

- POST /api/v1/auth/login with username/password
- BCrypt password verification against sys_user table
- JWT token generation via JwtInterceptor.generateToken()
- 3-layer catch: AppException, general Exception

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 13: Trigger — PackageController

**Files:**
- Create: `packageManager-trigger/src/main/java/com/huster/trigger/http/PackageController.java`

**Interfaces:**
- Consumes: `IPackageManageService` (Task 2), `IPackageService` (Task 6), all DTOs (Task 2), enums (Task 3), Entity/Aggregate (Task 4), `Response` (Task 1), `ResponseCode` (Task 1)
- Produces: All 5 package management REST endpoints

- [ ] **Step 1: Create PackageController**

Create file `packageManager-trigger/src/main/java/com/huster/trigger/http/PackageController.java`:

```java
package com.huster.trigger.http;

import com.alibaba.fastjson.JSON;
import com.huster.api.IPackageManageService;
import com.huster.api.dto.CheckinRequestDTO;
import com.huster.api.dto.DashboardStatsResponseDTO;
import com.huster.api.dto.EditRequestDTO;
import com.huster.api.dto.PackageResponseDTO;
import com.huster.api.response.Response;
import com.huster.domain.package.model.aggregate.PackageAggregate;
import com.huster.domain.package.model.entity.PackageEntity;
import com.huster.domain.package.model.valobj.CourierCompanyEnum;
import com.huster.domain.package.model.valobj.PackageStatusEnum;
import com.huster.domain.package.service.IPackageService;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/")
public class PackageController implements IPackageManageService {

    @Resource
    private IPackageService packageService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== 包裹入库 ==========

    @PostMapping("package/checkin")
    @Override
    public Response<Map<String, String>> checkin(@RequestBody CheckinRequestDTO request) {
        try {
            log.info("包裹入库: {}", JSON.toJSONString(request));

            // 参数校验
            if (StringUtils.isBlank(request.getWaybillNo())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "运单号不能为空");
            }
            if (StringUtils.isBlank(request.getPhone()) || request.getPhone().length() != 11) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "手机号格式错误");
            }
            if (StringUtils.isBlank(request.getCourier())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "快递公司不能为空");
            }
            if (StringUtils.isBlank(request.getShelf())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "货架位置不能为空");
            }

            // 校验快递公司编码
            CourierCompanyEnum courier;
            try {
                courier = CourierCompanyEnum.valueOfCode(request.getCourier());
            } catch (IllegalArgumentException e) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "无效的快递公司编码");
            }

            // 创建聚合
            PackageAggregate aggregate = PackageAggregate.createForCheckin(
                    request.getWaybillNo(), request.getPhone(), courier, request.getShelf());

            // 入库
            PackageEntity entity = packageService.checkin(aggregate);

            Map<String, String> data = new HashMap<>();
            data.put("id", entity.getId());
            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 包裹列表/搜索 ==========

    @GetMapping("package/list")
    @Override
    public Response<Map<String, Object>> list(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            log.info("查询包裹: phone={}, keyword={}, status={}, page={}, size={}",
                    phone, keyword, status, page, size);

            List<PackageEntity> entities = packageService.queryList(phone, keyword, status, page, size);
            int total = packageService.countTotal(phone, keyword, status);

            List<PackageResponseDTO> list = entities.stream().map(e -> {
                CourierCompanyEnum cc = e.getCourierCompany();
                return PackageResponseDTO.builder()
                        .id(e.getId())
                        .waybillNo(e.getWaybillNo())
                        .phone(e.getRecipientPhone())
                        .courier(cc != null ? cc.getCode() : "")
                        .courierDesc(cc != null ? cc.getInfo() : "")
                        .shelf(e.getShelfLocation())
                        .status(e.getStatus().getCode())
                        .statusDesc(e.getStatus().getInfo())
                        .checkinTime(e.getCheckinTime() != null ? e.getCheckinTime().format(DT_FMT) : null)
                        .pickupTime(e.getPickupTime() != null ? e.getPickupTime().format(DT_FMT) : null)
                        .stale(e.isStale())
                        .build();
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("list", list);
            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 确认取件 ==========

    @PutMapping("package/pickup/{id}")
    @Override
    public Response<Void> pickup(@PathVariable("id") String id) {
        try {
            log.info("确认取件: id={}", id);

            if (StringUtils.isBlank(id)) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "包裹ID不能为空");
            }

            packageService.pickup(id);
            return Response.success(null);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 编辑入库信息 ==========

    @PutMapping("package/edit")
    @Override
    public Response<Void> edit(@RequestBody EditRequestDTO request) {
        try {
            log.info("编辑包裹: {}", JSON.toJSONString(request));

            if (StringUtils.isBlank(request.getId())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "包裹ID不能为空");
            }

            CourierCompanyEnum courier;
            try {
                courier = CourierCompanyEnum.valueOfCode(request.getCourier());
            } catch (IllegalArgumentException e) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "无效的快递公司编码");
            }

            PackageEntity entity = PackageEntity.builder()
                    .id(request.getId())
                    .waybillNo(request.getWaybillNo())
                    .recipientPhone(request.getPhone())
                    .courierCompany(courier)
                    .shelfLocation(request.getShelf())
                    .build();

            packageService.edit(entity);
            return Response.success(null);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 统计概览 ==========

    @GetMapping("dashboard/stats")
    @Override
    public Response<DashboardStatsResponseDTO> stats() {
        try {
            log.info("查询统计概览");

            int[] stats = packageService.getDashboardStats();
            DashboardStatsResponseDTO data = DashboardStatsResponseDTO.builder()
                    .todayCheckin(stats[0])
                    .pendingTotal(stats[1])
                    .staleTotal(stats[2])
                    .todayPickup(stats[3])
                    .build();

            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }
}
```

- [ ] **Step 2: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-trigger -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-trigger/src/main/java/com/huster/trigger/http/PackageController.java && git commit -m "feat: add PackageController with all 5 REST endpoints

- POST /api/v1/package/checkin - package check-in
- GET /api/v1/package/list - query with phone/keyword/status/pagination
- PUT /api/v1/package/pickup/{id} - confirm pickup
- PUT /api/v1/package/edit - edit package info
- GET /api/v1/dashboard/stats - dashboard statistics
- 3-layer catch in every method
- Entity -> DTO conversion inline with stale flag calculation

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 14: Backend Integration — Full Compile and Smoke Test

**Files:**
- Modify: `packageManager-app/src/main/java/com/huster/Application.java` (verify it scans all packages)

**Interfaces:**
- Consumes: All previous tasks
- Produces: Running Spring Boot application

- [ ] **Step 1: Add MyBatis @MapperScan to Application.java**

Read the current `Application.java`. Add `@MapperScan` annotation:

```java
package com.huster;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Configurable
@MapperScan("com.huster.infrastructure.dao")
public class Application {

    public static void main(String[] args){
        SpringApplication.run(Application.class);
    }

}
```

- [ ] **Step 2: Full project compile**

```bash
cd e:/project/packageManager && mvn clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Verify the application starts**

```bash
cd e:/project/packageManager && mvn spring-boot:run -pl packageManager-app 2>&1 &
```

Wait for "Started Application in X seconds" message, then test:

```bash
# Test login
curl -X POST http://localhost:8091/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Expected: {"code":"0000","info":"成功","data":{"token":"eyJ..."}}
```

- [ ] **Step 4: Kill the server and commit**

```bash
cd e:/project/packageManager && git add packageManager-app/src/main/java/com/huster/Application.java && git commit -m "feat: add @MapperScan and verify application startup

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 15: Backend — PackageService Spring Bean Registration Fix

**Files:**
- Modify: `packageManager-domain/src/main/java/com/huster/domain/package/service/PackageService.java`

**Interfaces:**
- Consumes: `IPackageService`, `IPackageRepository` (Task 5-6)
- Produces: Spring-managed PackageService bean

- [ ] **Step 1: Add @Service annotation to PackageService**

The current `PackageService` is missing the `@Service` annotation (per DDD standard, domain services use `@Service`). Add it:

```java
package com.huster.domain.package.service;

// ... existing imports ...
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PackageService implements IPackageService {
    // ... rest unchanged ...
}
```

- [ ] **Step 2: Compile to verify**

```bash
cd e:/project/packageManager && mvn compile -pl packageManager-domain -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-domain/src/main/java/com/huster/domain/package/service/PackageService.java && git commit -m "fix: add @Service annotation to PackageService

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 16: Frontend — Project Scaffolding

**Files:**
- Create: `package-manager-web/package.json`
- Create: `package-manager-web/vite.config.ts`
- Create: `package-manager-web/tsconfig.json`
- Create: `package-manager-web/index.html`
- Create: `package-manager-web/src/main.ts`
- Create: `package-manager-web/src/App.vue`
- Create: `package-manager-web/src/styles/global.css`

**Interfaces:**
- Produces: Working Vue 3 + Vite + Element Plus project skeleton

- [ ] **Step 1: Create package.json**

Create file `package-manager-web/package.json`:

```json
{
  "name": "package-manager-web",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "element-plus": "^2.5.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^5.3.0",
    "vite": "^5.1.0",
    "vue-tsc": "^2.0.0"
  }
}
```

- [ ] **Step 2: Create vite.config.ts**

Create file `package-manager-web/vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8091',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 3: Create tsconfig.json**

Create file `package-manager-web/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "noEmit": true,
    "paths": {
      "@/*": ["./src/*"]
    },
    "baseUrl": "."
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: Create index.html**

Create file `package-manager-web/index.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>包裹管理系统</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 5: Create src/main.ts**

Create file `package-manager-web/src/main.ts`:

```typescript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
```

- [ ] **Step 6: Create src/App.vue**

Create file `package-manager-web/src/App.vue`:

```vue
<template>
  <router-view />
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 7: Create src/styles/global.css**

Create file `package-manager-web/src/styles/global.css`:

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.stale-row {
  background-color: #fff1f0 !important;
}
```

- [ ] **Step 8: Install dependencies and verify dev server starts**

```bash
cd e:/project/packageManager/package-manager-web && npm install
```

Expected: Dependencies installed without errors.

```bash
cd e:/project/packageManager/package-manager-web && npx vite --host 0.0.0.0 &
```

Expected: Vite dev server starts on port 3000. Open `http://localhost:3000` — should show blank page (no errors).

Kill the dev server after verifying.

- [ ] **Step 9: Commit**

```bash
cd e:/project/packageManager && git add package-manager-web/ && git commit -m "feat: scaffold Vue 3 + Vite + Element Plus frontend project

- Vue 3 with Composition API + TypeScript
- Element Plus for UI components
- Vite dev server with /api proxy to backend 8091
- Router placeholder, global CSS with stale-row style

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 17: Frontend — Axios Request Utility and Router

**Files:**
- Create: `package-manager-web/src/utils/request.ts`
- Create: `package-manager-web/src/router/index.ts`

**Interfaces:**
- Consumes: axios, vue-router, Element Plus
- Produces: `request` (axios instance with JWT interceptor), `router` (vue-router with auth guard)

- [ ] **Step 1: Create src/utils/request.ts**

Create file `package-manager-web/src/utils/request.ts`:

```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

// 请求拦截器 — 注入 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  (response) => {
    const { code, info, data } = response.data
    if (code === '0000') return data
    ElMessage.error(info || '请求失败')
    return Promise.reject(new Error(info))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error('网络错误')
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2: Create src/router/index.ts**

Create file `package-manager-web/src/router/index.ts`:

```typescript
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import LayoutView from '@/views/LayoutView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: LayoutView,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { requiresAuth: true, title: '统计概览' },
      },
      {
        path: 'checkin',
        name: 'Checkin',
        component: () => import('@/views/CheckinView.vue'),
        meta: { requiresAuth: true, title: '包裹入库' },
      },
      {
        path: 'packages',
        name: 'PackageList',
        component: () => import('@/views/PackageListView.vue'),
        meta: { requiresAuth: true, title: '包裹列表' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 3: Verify compile**

```bash
cd e:/project/packageManager/package-manager-web && npx vue-tsc --noEmit 2>&1 | head -20
```

Expected: No type errors (may show warnings for not-yet-created view files — that's OK).

- [ ] **Step 4: Commit**

```bash
cd e:/project/packageManager && git add package-manager-web/src/utils/request.ts package-manager-web/src/router/index.ts && git commit -m "feat: add axios request utility and Vue Router with auth guard

- axios instance with JWT Bearer token injection
- Response interceptor: code=0000 check, 401 redirect, error toasts
- Router: /login, /dashboard, /checkin, /packages
- beforeEach guard: redirect to /login if no token

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 18: Frontend — LoginView and LayoutView

**Files:**
- Create: `package-manager-web/src/views/LoginView.vue`
- Create: `package-manager-web/src/views/LayoutView.vue`

**Interfaces:**
- Consumes: `request` (Task 17), `router` (Task 17), Element Plus
- Produces: Login page, Layout shell with sidebar navigation

- [ ] **Step 1: Create LoginView.vue**

Create file `package-manager-web/src/views/LoginView.vue`:

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">📦 包裹管理系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
            size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%"
            :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data: any = await request.post('/auth/login', {
      username: form.username,
      password: form.password,
    })
    localStorage.setItem('token', data.token)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 20px;
}
.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}
</style>
```

- [ ] **Step 2: Create LayoutView.vue**

Create file `package-manager-web/src/views/LayoutView.vue`:

```vue
<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">📦 包裹管理</div>
      <el-menu :default-active="activeMenu" router background-color="#304156"
        text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/dashboard">
          <span>📊 统计概览</span>
        </el-menu-item>
        <el-menu-item index="/checkin">
          <span>📦 包裹入库</span>
        </el-menu-item>
        <el-menu-item index="/packages">
          <span>📋 包裹列表</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="header-title">末端驿站包裹管理系统</span>
        <div class="header-right">
          <span class="username">admin</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => route.path)

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #4a5e77;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.username {
  color: #606266;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-web/src/views/LoginView.vue packageManager-web/src/views/LayoutView.vue && git commit -m "feat: add LoginView and LayoutView

- LoginView: centered card form with gradient background
- LayoutView: sidebar navigation (3 items) + header with logout

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 19: Frontend — DashboardView and StaleTag Component

**Files:**
- Create: `package-manager-web/src/views/DashboardView.vue`
- Create: `package-manager-web/src/components/StaleTag.vue`

**Interfaces:**
- Consumes: `request` (Task 17), Element Plus
- Produces: Dashboard stats page, reusable StaleTag component

- [ ] **Step 1: Create StaleTag.vue**

Create file `package-manager-web/src/components/StaleTag.vue`:

```vue
<template>
  <el-tag v-if="stale" type="danger" size="small">⚠ 滞留</el-tag>
</template>

<script setup lang="ts">
defineProps<{
  stale: boolean
}>()
</script>
```

- [ ] **Step 2: Create DashboardView.vue**

Create file `package-manager-web/src/views/DashboardView.vue`:

```vue
<template>
  <div class="dashboard">
    <h3>📊 数据概览</h3>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日入库</div>
          <div class="stat-value" style="color: #409EFF">{{ stats.todayCheckin }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待取件总数</div>
          <div class="stat-value" style="color: #E6A23C">{{ stats.pendingTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-danger">
          <div class="stat-label">滞留包裹</div>
          <div class="stat-value" style="color: #F56C6C">{{ stats.staleTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日取件</div>
          <div class="stat-value" style="color: #67C23A">{{ stats.todayPickup }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

interface Stats {
  todayCheckin: number
  pendingTotal: number
  staleTotal: number
  todayPickup: number
}

const stats = ref<Stats>({
  todayCheckin: 0,
  pendingTotal: 0,
  staleTotal: 0,
  todayPickup: 0,
})

onMounted(async () => {
  try {
    stats.value = await request.get('/dashboard/stats') as any
  } catch {
    // 错误已在拦截器中处理
  }
})
</script>

<style scoped>
.dashboard h3 {
  margin-bottom: 20px;
}
.stats-row {
  margin-top: 10px;
}
.stat-card {
  text-align: center;
  padding: 10px;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
}
.stat-danger {
  border: 1px solid #F56C6C;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-web/src/views/DashboardView.vue packageManager-web/src/components/StaleTag.vue && git commit -m "feat: add DashboardView and StaleTag component

- Dashboard: 4 stat cards (today checkin, pending, stale, today pickup)
- StaleTag: el-tag danger with ⚠ icon, conditional render

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 20: Frontend — CheckinView

**Files:**
- Create: `package-manager-web/src/views/CheckinView.vue`

**Interfaces:**
- Consumes: `request` (Task 17), Element Plus
- Produces: Package check-in form page

- [ ] **Step 1: Create CheckinView.vue**

Create file `package-manager-web/src/views/CheckinView.vue`:

```vue
<template>
  <div class="checkin">
    <h3>📦 包裹入库</h3>
    <el-card style="max-width: 600px; margin-top: 20px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="运单号" prop="waybillNo">
          <el-input v-model="form.waybillNo" placeholder="请输入运单号" />
        </el-form-item>
        <el-form-item label="收件人手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="快递公司" prop="courier">
          <el-select v-model="form.courier" placeholder="请选择快递公司" style="width: 100%">
            <el-option v-for="c in couriers" :key="c.code" :label="c.label" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="货架位置" prop="shelf">
          <el-input v-model="form.shelf" placeholder="如 A-01" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleCheckin">
            确认入库
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  waybillNo: '',
  phone: '',
  courier: '',
  shelf: '',
})

const couriers = [
  { code: 'SF', label: '顺丰' },
  { code: 'YTO', label: '圆通' },
  { code: 'ZTO', label: '中通' },
  { code: 'STO', label: '申通' },
  { code: 'YD', label: '韵达' },
  { code: 'JD', label: '京东' },
  { code: 'DB', label: '德邦' },
  { code: 'OTHER', label: '其他' },
]

const validatePhone = (_rule: any, value: string, callback: any) => {
  if (!/^1\d{10}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  waybillNo: [{ required: true, message: '请输入运单号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' },
  ],
  courier: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  shelf: [{ required: true, message: '请输入货架位置', trigger: 'blur' }],
}

async function handleCheckin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await request.post('/package/checkin', { ...form })
    ElMessage.success('入库成功')
    handleReset()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.checkin h3 {
  margin-bottom: 10px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
cd e:/project/packageManager && git add packageManager-web/src/views/CheckinView.vue && git commit -m "feat: add CheckinView with package check-in form

- el-form with waybillNo, phone (11-digit validation), courier (select), shelf
- 8 courier options matching backend CourierCompanyEnum
- Form reset on success

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 21: Frontend — PackageListView

**Files:**
- Create: `package-manager-web/src/views/PackageListView.vue`

**Interfaces:**
- Consumes: `request` (Task 17), `StaleTag` (Task 19), Element Plus
- Produces: Package list page with search, tabs, table, pickup action

- [ ] **Step 1: Create PackageListView.vue**

Create file `package-manager-web/src/views/PackageListView.vue`:

```vue
<template>
  <div class="package-list">
    <h3>📋 包裹列表</h3>

    <!-- 搜索区 -->
    <el-card style="margin-top: 20px">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="精确查询" clearable
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="运单号/手机号/快递"
            clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Tabs + 表格 -->
    <el-card style="margin-top: 20px">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待取件" :name="0" />
        <el-tab-pane label="已取件" :name="1" />
      </el-tabs>

      <el-table :data="tableData" :row-class-name="tableRowClassName" v-loading="loading"
        stripe style="width: 100%">
        <el-table-column prop="waybillNo" label="运单号" width="140" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="courierDesc" label="快递公司" width="90" />
        <el-table-column prop="shelf" label="货架" width="80" />
        <el-table-column prop="checkinTime" label="入库时间" width="160" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <StaleTag :stale="row.stale" />
            <el-tag v-if="!row.stale" :type="row.status === 0 ? 'warning' : 'success'"
              size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm v-if="row.status === 0" title="确认该包裹已被收件人取走？"
              @confirm="handlePickup(row.id)">
              <template #reference>
                <el-button type="primary" size="small" link>取件</el-button>
              </template>
            </el-popconfirm>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top: 20px; justify-content: flex-end"
        v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import StaleTag from '@/components/StaleTag.vue'
import request from '@/utils/request'

interface PackageItem {
  id: string
  waybillNo: string
  phone: string
  courier: string
  courierDesc: string
  shelf: string
  status: number
  statusDesc: string
  checkinTime: string
  pickupTime: string | null
  stale: boolean
}

const loading = ref(false)
const tableData = ref<PackageItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const activeTab = ref(0)

const searchForm = reactive({
  phone: '',
  keyword: '',
})

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value,
      status: activeTab.value,
    }
    if (searchForm.phone) params.phone = searchForm.phone
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const data: any = await request.get('/package/list', { params })
    tableData.value = data.list
    total.value = data.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  searchForm.phone = ''
  searchForm.keyword = ''
  currentPage.value = 1
  fetchData()
}

function handleTabChange() {
  currentPage.value = 1
  fetchData()
}

async function handlePickup(id: string) {
  try {
    await request.put(`/package/pickup/${id}`)
    fetchData()
  } catch {
    // 错误已在拦截器中处理
  }
}

function tableRowClassName({ row }: { row: PackageItem }) {
  return row.stale ? 'stale-row' : ''
}
</script>

<style scoped>
.package-list h3 {
  margin-bottom: 10px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
cd e:/project/packageManager && git add packageManager-web/src/views/PackageListView.vue && git commit -m "feat: add PackageListView with search, tabs, table, and pickup

- Search by phone (exact) + keyword (fuzzy on waybillNo/phone/courier)
- Tabs toggling pending(0) vs picked-up(1)
- el-table with stale row highlighting
- Popconfirm pickup action for pending packages
- el-pagination with page change handler

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 22: Integration Test — End-to-End API Verification

**Files:**
- Modify: `packageManager-app/src/test/java/com/huster/test/ApiTest.java`

**Interfaces:**
- Consumes: All backend tasks (1-15)
- Produces: Passing integration tests for all 6 API endpoints

- [ ] **Step 1: Rewrite ApiTest.java with comprehensive tests**

Replace the content of `packageManager-app/src/test/java/com/huster/test/ApiTest.java`:

```java
package com.huster.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huster.api.dto.*;
import com.huster.api.response.Response;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApiTest {

    @LocalServerPort
    private int port;

    @Resource
    private TestRestTemplate restTemplate;

    private String token;
    private String testPackageId;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ==================== T1: 正常登录 ====================

    @Test
    public void test01_login_success() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("admin");
        req.setPassword("admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        assertNotNull(json.getJSONObject("data").getString("token"));
        token = json.getJSONObject("data").getString("token");
    }

    // ==================== T2: 登录失败 ====================

    @Test
    public void test02_login_fail() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("admin");
        req.setPassword("wrong");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("E0101", json.getString("code"));
    }

    // ==================== T3: 正常入库 ====================

    @Test
    public void test03_checkin_success() {
        CheckinRequestDTO req = new CheckinRequestDTO();
        req.setWaybillNo("SF20260620001");
        req.setPhone("13800138000");
        req.setCourier("SF");
        req.setShelf("A-01");

        HttpHeaders headers = authHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        assertNotNull(json.getJSONObject("data").getString("id"));
        testPackageId = json.getJSONObject("data").getString("id");
    }

    // ==================== T4: 重复运单号入库 ====================

    @Test
    public void test04_checkin_duplicate() {
        CheckinRequestDTO req = new CheckinRequestDTO();
        req.setWaybillNo("SF20260620001");
        req.setPhone("13800138000");
        req.setCourier("SF");
        req.setShelf("A-02");

        HttpHeaders headers = authHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("E0202", json.getString("code"));
    }

    // ==================== T5: 入库参数缺失 ====================

    @Test
    public void test05_checkin_missing_param() {
        CheckinRequestDTO req = new CheckinRequestDTO();
        req.setWaybillNo("");
        req.setPhone("13800138000");
        req.setCourier("SF");
        req.setShelf("A-01");

        HttpHeaders headers = authHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0002", json.getString("code"));
    }

    // ==================== T6: 按手机号查询 ====================

    @Test
    public void test06_query_by_phone() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/package/list?phone=13800138000&page=1&size=20"),
                HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        assertTrue(json.getJSONObject("data").getInteger("total") >= 1);
    }

    // ==================== T7: 确认取件 ====================

    @Test
    public void test07_pickup() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/package/pickup/" + testPackageId),
                HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
    }

    // ==================== T8: 重复取件 ====================

    @Test
    public void test08_pickup_duplicate() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/package/pickup/" + testPackageId),
                HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("E0203", json.getString("code"));
    }

    // ==================== T9: 统计概览 ====================

    @Test
    public void test09_dashboard_stats() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/dashboard/stats"),
                HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        JSONObject data = json.getJSONObject("data");
        assertNotNull(data.getInteger("todayCheckin"));
        assertNotNull(data.getInteger("pendingTotal"));
        assertNotNull(data.getInteger("staleTotal"));
        assertNotNull(data.getInteger("todayPickup"));
    }

    // ==================== T10: 未登录访问 ====================

    @Test
    public void test10_unauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ==================== 辅助方法 ====================

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }
}
```

- [ ] **Step 2: Run all integration tests**

```bash
cd e:/project/packageManager && mvn test -pl packageManager-app -Dtest=com.huster.test.ApiTest -q
```

Expected: All 10 tests PASS. If any fail, debug before committing.

- [ ] **Step 3: Commit**

```bash
cd e:/project/packageManager && git add packageManager-app/src/test/java/com/huster/test/ApiTest.java && git commit -m "test: add comprehensive API integration tests

- T1-T10 covering all spec test scenarios
- Login success/fail, checkin, duplicate checkin, missing params
- Query by phone, pickup, duplicate pickup, dashboard stats, unauthorized
- @FixMethodOrder for sequential execution (token from T1 flows through)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 23: Frontend — Build Verification

**Files:**
- No new files — verify frontend builds cleanly

**Interfaces:**
- Consumes: All frontend tasks (16-21)
- Produces: Clean production build

- [ ] **Step 1: Build frontend**

```bash
cd e:/project/packageManager/package-manager-web && npm run build
```

Expected: Build succeeds, output in `dist/` directory.

- [ ] **Step 2: Verify all 11 expected files exist**

```bash
ls -la e:/project/packageManager/package-manager-web/src/views/
ls -la e:/project/packageManager/package-manager-web/src/components/
ls -la e:/project/packageManager/package-manager-web/src/router/
ls -la e:/project/packageManager/package-manager-web/src/utils/
```

Expected: All files from Tasks 16-21 present.

- [ ] **Step 3: Commit if any config tweaks**

```bash
cd e:/project/packageManager && git status
```

If any tweaks needed (e.g., tsconfig adjustments for build), commit them:

```bash
cd e:/project/packageManager && git add -A package-manager-web/ && git commit -m "chore: finalize frontend build configuration

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 24: Final Verification — Full System Check

**Files:**
- No new files — run through the full checklist

- [ ] **Step 1: Full backend compile**

```bash
cd e:/project/packageManager && mvn clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**

```bash
cd e:/project/packageManager && mvn test -pl packageManager-app -q
```

Expected: All tests pass

- [ ] **Step 3: Start the application**

```bash
cd e:/project/packageManager && mvn spring-boot:run -pl packageManager-app &
```

Wait for "Started Application".

- [ ] **Step 4: Smoke test all 6 endpoints**

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8091/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: $TOKEN"

# 2. Checkin
curl -s -X POST http://localhost:8091/api/v1/package/checkin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"waybillNo":"TEST001","phone":"13900139000","courier":"SF","shelf":"A-01"}'

# 3. List
curl -s "http://localhost:8091/api/v1/package/list?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 4. Dashboard
curl -s http://localhost:8091/api/v1/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"

# 5. Pickup (replace ID from step 2 response)
# curl -s -X PUT http://localhost:8091/api/v1/package/pickup/<ID> \
#   -H "Authorization: Bearer $TOKEN"

# 6. Edit
# curl -s -X PUT http://localhost:8091/api/v1/package/edit \
#   -H "Content-Type: application/json" \
#   -H "Authorization: Bearer $TOKEN" \
#   -d '{"id":"<ID>","waybillNo":"TEST001","phone":"13900139000","courier":"YTO","shelf":"B-02"}'
```

Expected: All return `"code":"0000"`.

- [ ] **Step 5: Kill server and final commit**

```bash
# Kill the spring-boot process
# Then verify git status is clean
cd e:/project/packageManager && git status
```

---

### Implementation Complete Checklist

- [ ] All 24 tasks committed
- [ ] Backend compiles: `mvn clean compile` → BUILD SUCCESS
- [ ] All tests pass: `mvn test` → 10/10 PASS
- [ ] Frontend builds: `npm run build` → success
- [ ] Login → get token → call all 5 authenticated endpoints → all return 0000
- [ ] Stale flag correctly calculated for packages > 48h old
- [ ] Duplicate waybill check-in blocked
- [ ] Duplicate pickup blocked
- [ ] Unauthorized requests return 401
- [ ] Dashboard stats return correct counts
