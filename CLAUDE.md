# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

PackageManager is a courier station package management system (末端驿站包裹管理系统) with a Java 8 / Spring Boot 2.7.12 backend and a Vue 3 / TypeScript / Element Plus frontend. It handles package check-in, pickup confirmation, multi-field search, stale-package alerts, and a statistics dashboard.

## Build & run

### Infrastructure (MySQL, Redis)

```bash
docker-compose -f docs/dev-ops/docker-compose-environment.yml up -d
# MySQL on 127.0.0.1:3306, password: 523Hust.985
```

### Backend (Maven, Java 8, Spring Boot 2.7.12)

```bash
mvn clean package -DskipTests          # build all modules
java -jar packageManager-app/target/packageManager-app.jar   # runs on :8091
java -jar -Dspring.profiles.active=prod packageManager-app/target/packageManager-app.jar  # prod profile

mvn test                               # run all tests
mvn test -pl packageManager-app        # run only app module tests
```

### Frontend (Vue 3, Vite, Element Plus)

```bash
cd package-manager-web
npm install
npm run dev       # dev server on :3000, proxies /api → localhost:8091
npm run build     # type-check + production build
```

### Database setup

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p523Hust.985 < docs/dev-ops/mysql/sql/schema.sql
```

Creates database `package_manager` with tables `package`, `daily_sequence`, `sys_user`, and default admin user `admin / admin123`.

## Architecture

The backend follows **DDD hexagonal architecture** with strict top-down dependency:

```
packageManager-app           ← Spring Boot entry, config, MyBatis XML
    ↓
packageManager-trigger       ← HTTP controllers + JWT interceptor
    ↓
packageManager-api           ← Interface definitions + DTOs
    ↓
packageManager-domain         ← Domain entities, aggregates, repository interfaces, domain services
    ↓                           ↑
packageManager-infrastructure ← Repository implementations (MyBatis DAOs)
    ↓
packageManager-types          ← Shared enums, exceptions, constants
```

Dependency direction is always downward — upper layers know about lower layers, never the reverse. The domain layer defines repository **interfaces**; infrastructure implements them via MyBatis.

The frontend is a separate module (`package-manager-web/`) with no build dependency on the backend. It communicates purely over REST.

## Key patterns

### Pickup code generation

Pickup codes follow format `SHELF-SERIAL` (e.g., `A-13-0001`). The shelf portion comes from user input; the serial number is per-shelf, per-date, atomically allocated via `daily_sequence` table using MySQL `INSERT ... ON DUPLICATE KEY UPDATE`. See `PackageRepository.createPickupCode()`.

### Stale package detection

Packages not picked up within 48 hours of check-in are considered "stale" (滞留). This is computed in `PackageEntity.isStale()` (Java-level) and also used in SQL queries when the `stale` filter is active. The frontend applies `.stale-row` CSS class (red border) and renders `StaleTag` for such packages.

### JWT authentication flow

- All `/api/v1/**` requests pass through `JwtInterceptor` except `/api/v1/auth/login`
- Login uses `AuthController` with BCrypt password verification against `sys_user` table via raw JDBC
- Tokens are HS256-signed (jjwt 0.9.1), 24-hour expiry
- The `WebMvcConfig` in `packageManager-app` registers the interceptor and exclusion patterns
- Frontend: `utils/request.ts` Axios interceptor injects `Authorization: Bearer <token>` header and redirects to `/login` on 401

### Duplicate check-in prevention

When checking in a package, `PackageService.checkin()` queries for an existing package with the same waybill number and PENDING status. If found, it throws `AppException` with code `E0202`.

### Response format

All API responses use `Response<T>` wrapper with fields `code` (string error code), `info` (message), `data` (generic payload). Success code is `"0000"`. Error codes are defined in `ResponseCode.java`.

### MyBatis query param passing

Both `PackageDao.listAllPackages()` and `PackageDao.countPackages()` accept individual parameters that map to `#{}` placeholders in the XML mapper. The `PackagePO` object carries query-only fields (offset, limit, stale, sortOrder) in addition to table columns.

## Test conventions

Tests live in `packageManager-app/src/test/java/com/huster/test/ApiTest.java`. They use JUnit 4 with `SpringRunner` and `TestRestTemplate` against a random port. Test methods run in alphabetic order (`@FixMethodOrder(MethodSorters.NAME_ASCENDING)`) — the naming convention is `testNN_description` where NN controls execution order. Tests depend on prior tests (e.g., login must run before authenticated endpoints, checkin must run before pickup).

## Configuration files

| File | Purpose |
|------|---------|
| `pom.xml` (root) | Parent POM, module list, Maven profiles (dev/test/prod) |
| `packageManager-app/src/main/resources/application.yml` | Default config, sets `spring.profiles.active: dev` |
| `packageManager-app/src/main/resources/application-test.yml` | Test profile — datasource, thread pool (20 core / 50 max) |
| `packageManager-app/src/main/resources/application-prod.yml` | Prod profile — datasource, 6GB heap JVM args |
| `packageManager-app/src/main/resources/mybatis/mapper/package_mapper.xml` | All MyBatis SQL mappings |
| `package-manager-web/vite.config.ts` | Vite config — port 3000, proxy `/api` → `:8091`, `@` → `./src` |
