# ComModule

一套按“地基 → 抽象 → 业务 → 分布式 → 进阶”路线搭建的 Spring Boot 组件化练习工程。

每个模块都遵循同一套约束：

- 独立 Starter，可单独引入。
- 先定义扩展点，再提供开箱即用的默认实现。
- 默认实现不依赖外部中间件，测试可直接运行。
- 用配置或自定义 Bean 切换实现，业务代码不感知具体存储/消息/缓存实现。
- 目标是“能用、可切换、有测试、有示例”，不追求生产级完备。

当前技术栈：Java 17、Spring Boot 3.3.7、Maven、JJWT 0.12.6。

## 模块地图

| 模块 | 扩展点 | 默认实现 | 已覆盖能力 |
| --- | --- | --- | --- |
| `commodule-web-spring-boot-starter` | 全局异常处理、响应包装、trace 配置 | Servlet 过滤器 + `ResponseBodyAdvice` | 统一出参、业务异常、参数校验错误、404、traceId |
| `commodule-file-storage-spring-boot-starter` | `FileStorage` | `LocalFileStorage`、`InMemoryFileStorage` | 上传、读取、删除、元数据、路径穿越防护、大小限制、原子写入 |
| `commodule-auth-spring-boot-starter` | `AuthUserService`、`JwtTokenService`、`RefreshTokenStore`、`TokenRevocationStore`、`SecurityFilterChain` | JWT + Spring Security + BCrypt + 内存 Refresh Token + 内存访问令牌撤销 | 登录、JWT 校验、Refresh Token 轮换/撤销、访问令牌即时撤销、无状态安全链、角色鉴权、CORS、401/403 统一返回 |
| `commodule-cache-spring-boot-starter` | `CacheStore` | `MemoryCacheStore` | TTL、容量上限、Spring Cache 适配、按命名空间清理、单飞回源 |
| `commodule-mq-spring-boot-starter` | `MessageBroker`、`MessageListener`、`DeadLetterQueue` | `InMemoryMessageBroker` | 发布/订阅、异步投递、重试、有界队列、死信队列 |
| `commodule-data-permission-spring-boot-starter` | `DataPermissionRule`、`DataFieldReader` | 管理员全量、普通用户仅本人 | 注解式数据过滤、结果集过滤、`DataPage`、SQL 上下文 |
| `commodule-code-generator-spring-boot-starter` | `CodeTemplate`、`CodeGenerator` | 实体、Request、Service、SQL 模板 | 元数据校验、代码预览、ZIP 下载、自定义模板 |
| `commodule-project-template` | 业务示例应用 | 全部模块装配 | 登录、RBAC、数据权限、代码生成、文件、缓存、消息、集成测试 |

核心组件和两个进阶模块都已完成第一版。后续接数据库时，可以继续补 SQL 级数据权限、MyBatis Mapper 模板和真实工程落盘。

## 工程结构

```text
ComModule/
├── commodule-web-spring-boot-starter/
├── commodule-file-storage-spring-boot-starter/
├── commodule-cache-spring-boot-starter/
├── commodule-mq-spring-boot-starter/
├── commodule-auth-spring-boot-starter/
├── commodule-data-permission-spring-boot-starter/
├── commodule-code-generator-spring-boot-starter/
├── commodule-project-template/
└── pom.xml
```

`project-template` 是最终装配示例，依赖关系为：

```text
project-template
├── auth-starter ──> web-starter
├── data-permission-starter ──> auth-starter
├── code-generator-starter
├── file-storage-starter
├── cache-starter
└── mq-starter
```

## 快速开始

```bash
mvn clean test
mvn clean install
mvn -pl commodule-project-template spring-boot:run
```

默认端口为 `8080`。示例账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | `ADMIN`、`USER` |
| `user` | `user123` | `USER` |

示例账号只用于演示，密码在启动时用 BCrypt 编码后放入内存。

## 统一响应与 traceId

所有普通 JSON 接口默认被包装为：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "username": "admin"
  },
  "traceId": "7bfcd0e6299f4f8d8da7d5d5cfbcff62",
  "timestamp": "2026-11-26T00:00:00Z"
}
```

错误响应示例：

```json
{
  "code": 40300,
  "message": "无权访问该资源",
  "data": null,
  "traceId": "a0a5e7fbe9454e498f4e45311c5c2175",
  "timestamp": "2026-11-26T00:00:00Z"
}
```

常用错误码：

| code | HTTP 状态 | 含义 |
| --- | --- | --- |
| `0` | `200` | 成功 |
| `40000` | `400` | 请求参数或格式错误 |
| `40100` | `401` | 未认证或凭证失效 |
| `40300` | `403` | 无权限访问资源 |
| `40301` | `403` | 跨域请求被拒绝 |
| `40400` | `404` | 资源不存在 |
| `40500` | `405` | 请求方法不支持 |
| `40900` | `409` | 资源状态冲突 |
| `41500` | `415` | 请求内容类型不支持 |
| `50000` | `500` | 服务器内部错误 |

请求可携带 `X-Trace-Id`。合法 traceId 会透传，否则自动生成，并写入响应头与 MDC。

文件下载接口返回 `ResponseEntity<Resource>`，不会套统一响应；普通 DTO、`Map`、`List` 会自动包装。

## 常用接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | 公开 | 登录并获取 JWT |
| `POST` | `/api/auth/refresh` | 公开 | 用 Refresh Token 轮换新令牌 |
| `POST` | `/api/auth/logout` | 公开 | 撤销 Refresh Token；携带 Bearer 时同时撤销当前访问令牌 |
| `GET` | `/api/auth/me` | 已登录 | 当前用户信息 |
| `GET` | `/api/demo/me` | 已登录 | 示例受保护接口 |
| `GET` | `/api/demo/admin` | `ADMIN` | 方法级角色鉴权示例 |
| `GET` | `/api/demo/cache/{id}` | 已登录 | 缓存命中示例 |
| `POST` | `/api/demo/events/{eventName}` | 已登录 | 发布内存消息 |
| `GET` | `/api/demo/events` | 已登录 | 查看已消费消息 |
| `GET` | `/api/demo/orders` | 已登录 | 管理员全量、普通用户仅本人 |
| `GET` | `/api/demo/orders/page` | 已登录 | 数据权限与分页结果示例 |
| `POST` | `/api/generator/preview` | `ADMIN` | 预览生成文件 |
| `POST` | `/api/generator/zip` | `ADMIN` | 下载生成结果 ZIP |
| `POST` | `/api/files` | 已登录 | multipart 上传 |
| `GET` | `/api/files?key=...` | 已登录 | 下载文件 |
| `DELETE` | `/api/files?key=...` | `ADMIN` | 删除文件 |

登录：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

访问受保护接口：

```bash
curl http://localhost:8080/api/demo/admin \
  -H "Authorization: Bearer ${TOKEN}"
```

跨域预检：

```bash
curl -i -X OPTIONS http://localhost:8080/api/demo/me \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
```

上传与下载：

```bash
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@README.md"

curl "http://localhost:8080/api/files?key=${KEY}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -o downloaded.md
```

可在 IntelliJ HTTP Client 或 VS Code REST Client 中直接执行 `commodule-project-template/requests.http` 的请求示例。

## 配置说明

### Web

```yaml
commodule:
  web:
    trace-id-header: X-Trace-Id
    max-trace-id-length: 128
```

### 文件存储

```yaml
commodule:
  file-storage:
    provider: local
    base-dir: ${java.io.tmpdir}/commodule-files
    max-file-size: 10MB
```

`provider` 可选：

- `local`：本地磁盘，默认值。
- `memory`：进程内内存，适合测试或演示。

自定义实现时声明 `FileStorage` Bean 即可覆盖默认实现。

### 认证

```yaml
commodule:
  auth:
    secret: ${COMMODULE_AUTH_SECRET:commodule-change-me-32-byte-secret-key}
    token-ttl: 2h
    issuer: commodule-project-template
    base-path: /api/auth
    refresh-token-ttl: 7d
    refresh-token-max-per-user: 10
    cors-allowed-origins:
      - http://localhost:3000
      - http://localhost:5173
    cors-allowed-methods:
      - GET
      - POST
      - PUT
      - PATCH
      - DELETE
      - OPTIONS
    cors-allowed-headers:
      - "*"
    cors-exposed-headers:
      - X-Trace-Id
    cors-allow-credentials: false
    cors-max-age: 1h
```

`secret` 至少 32 字节。业务侧实现 `AuthUserService` 后，Starter 会自动装配登录接口、JWT 过滤器、方法级鉴权和默认安全链。

登录响应同时返回访问令牌和 Refresh Token。Refresh Token 是随机 opaque token，默认实现只保存 SHA-256 哈希，不保存明文；刷新时旧 token 会被原子消费并轮换出新 token，登出会撤销当前 Refresh Token。`refresh-token-max-per-user` 限制单用户保留的活跃 Refresh Token 数量，超出时淘汰最旧的 token。

访问令牌包含唯一 `jti`。登出请求携带 `Authorization: Bearer <token>` 时，当前访问令牌会按 `jti` 写入撤销存储，后续携带同一 token 的请求立即返回 `401/40100`。撤销记录保留到 token 的 `exp`，过期后自动清理；默认实现是进程内存储，重启或多节点部署时应替换为 Redis 等共享实现。

CORS 默认不信任任何跨域来源；配置 `cors-allowed-origins` 后才会放行，支持精确来源和 `https://*.example.com` 这类通配模式。预检请求不需要 JWT，非法来源返回统一的 `40301` JSON 响应并携带 traceId。浏览器默认只能读取简单响应头，因此 `cors-exposed-headers` 默认暴露 `X-Trace-Id`，方便前端关联后端日志。

需要接管 CORS 时，声明一个名为 `corsConfigurationSource` 的 Bean 即可。Spring MVC 自带的 `HandlerMappingIntrospector` 也实现了 `CorsConfigurationSource`，Starter 按 Bean 名称判断和装配，避免与它发生类型冲突。

替换 Refresh Token 存储时声明 `RefreshTokenStore` Bean：

```java
@Bean
RefreshTokenStore refreshTokenStore() {
    return new RedisRefreshTokenStore();
}
```

实现需要提供签发、一次性消费、单 token 撤销和按用户批量撤销。默认实现读取 `refresh-token-ttl` 与 `refresh-token-max-per-user`，自定义实现可以自行选择持久化方式、过期策略和并发控制。

替换访问令牌撤销存储时声明 `TokenRevocationStore` Bean：

```java
@Bean
TokenRevocationStore tokenRevocationStore() {
    return new RedisTokenRevocationStore();
}
```

实现只需要按 `jti` 撤销并保留到 `expiresAt`，同时支持查询是否已撤销。多节点部署必须共享该存储，否则一个节点上的登出不会影响其他节点。

### 缓存

```yaml
commodule:
  cache:
    provider: memory
    default-ttl: 5m
    max-entries: 1000
```

Starter 会把 `CacheStore` 适配成 Spring `CacheManager`，因此可以直接使用 `@Cacheable`、`@CacheEvict`、`@CachePut`。

替换 Redis 等实现时，只需提供新的 `CacheStore` Bean，并实现：

- `get`
- `put`
- `evict`
- `evictByPrefix`
- `clear`

### 消息

```yaml
commodule:
  mq:
    provider: memory
    max-attempts: 3
    retry-backoff: 100ms
    max-dead-letters: 100
    worker-threads: 2
    queue-capacity: 1000
```

自定义消息实现时，实现 `MessageBroker` 与 `MessageListener` 即可。业务监听器声明为 Spring Bean 后会自动订阅。

队列满时消息不会继续无界堆积，而是写入死信队列；消费失败达到 `max-attempts` 后同样进入死信队列。

### 数据权限

```yaml
commodule:
  data-permission:
    enabled: true
    admin-role: ADMIN
```

在需要过滤结果的方法上声明：

```java
@DataPermission(resource = "order", ownerField = "ownerId")
public List<Order> listOrders() {
    return orders;
}
```

默认规则：

- 拥有 `admin-role` 的用户可以看到全部数据。
- 其他用户只能看到 `ownerId` 等于当前 JWT 用户 id 的数据。
- 未认证、缺少用户 id 或读取字段失败时返回空结果或明确异常，避免越权。

支持自动过滤 `List` 与 `DataPage`。如果要在 SQL/DAO 层拼接条件，可在方法内部读取：

```java
DataPermissionDecision decision = DataPermissionContext.current()
        .orElseThrow(() -> new IllegalStateException("缺少数据权限上下文"))
        .decision();
```

自定义规则时声明 `DataPermissionRule` Bean；自定义字段读取方式时声明 `DataFieldReader` Bean。

### 代码生成器

```yaml
commodule:
  code-generator:
    enabled: true
```

请求示例：

```json
{
  "basePackage": "com.example.demo",
  "className": "UserProfile",
  "tableName": "user_profile",
  "description": "用户资料",
  "fields": [
    {"name": "nickname", "javaType": "String", "required": true},
    {"name": "age", "javaType": "Integer", "required": false}
  ]
}
```

默认生成：

- `domain/{ClassName}.java`
- `web/{ClassName}Request.java`
- `service/{ClassName}Service.java`
- `db/{table_name}.sql`

生成器会校验包名、类名、表名、字段名和 Java 类型，避免把非法标识符直接拼进源码。字段名 `id` 保留给自动主键，不能重复声明。

## 扩展方式

### 替换文件存储

```java
@Bean
FileStorage fileStorage() {
    return new MyObjectStorage();
}
```

### 替换缓存

```java
@Bean
CacheStore cacheStore() {
    return new RedisCacheStore();
}
```

### 接入业务用户

```java
@Service
class DatabaseUserService implements AuthUserService {
    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new AuthUser(
                        user.id(),
                        user.username(),
                        user.passwordHash(),
                        user.roles(),
                        user.enabled()
                ));
    }
}
```

### 刷新与登出

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"${REFRESH_TOKEN}"}'

curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d '{"refreshToken":"${REFRESH_TOKEN}"}'
```

### 接管安全配置

声明自定义 `SecurityFilterChain` 后，Starter 的默认安全链会退让。JWT 过滤器、登录控制器、`AuthUserService`、`corsConfigurationSource` 和 `ApiCorsProcessor` 仍可复用；自定义链需要自行加入 `CorsFilter` 和 `JwtAuthenticationFilter`。构造 `JwtAuthenticationFilter` 时应注入当前的 `TokenRevocationStore`，并保证它与 `/api/auth/logout` 使用同一实例或同一共享存储。

### 新增消息监听

```java
@Component
class OrderCreatedListener implements MessageListener {
    @Override
    public String topic() {
        return "demo.orders";
    }

    @Override
    public void onMessage(Message message) {
    }
}
```

### 自定义数据权限规则

```java
@Bean
DataPermissionRule dataPermissionRule() {
    return (request, principal) -> {
        if (principal == null) {
            return DataPermissionDecision.deny();
        }
        return DataPermissionDecision.self(principal.id());
    };
}
```

### 自定义代码模板

```java
@Bean
CodeTemplate readmeTemplate() {
    return new CodeTemplate() {
        @Override
        public String name() {
            return "readme";
        }

        @Override
        public List<GeneratedFile> render(GenerationRequest request) {
            return List.of(new GeneratedFile(
                    "README.generated.md",
                    "# " + request.className()
            ));
        }
    };
}
```

自定义模板会与默认模板共同参与生成；如果需要完全接管生成流程，声明自定义 `CodeGenerator` Bean 即可。

## 可靠性优化

- 本地文件先写临时文件，再原子移动到目标路径；超限或异常会清理半成品。
- 下载时对非法 `Content-Type` 回退为 `application/octet-stream`，避免异常元数据导致 500。
- 缓存 `get(key, loader)` 使用分段锁实现单飞，同一热点 key 并发回源只执行一次。
- MQ 使用有界队列，队列满时拒绝任务并写入死信，避免无界内存增长。
- Web 边界统一处理参数校验、404、405、415 和未预期异常，响应仍带 traceId。
- CORS 拒绝使用统一 `40301` JSON 响应，不再返回 Spring 默认纯文本；405/415 也使用标准错误码。
- JWT 认证方案按 HTTP 规范忽略大小写，`Bearer` 与 `bearer` 均可使用。
- JWT 过滤器不会覆盖或清空其他认证机制已经建立的安全上下文。
- Refresh Token 只存哈希、一次性消费、刷新轮换，并支持登出撤销和单用户数量上限。
- 访问令牌携带唯一 `jti`；登出可即时撤销当前 token，撤销记录随 `exp` 到期清理。
- 代码生成器对包名、类名、表名、字段名和 Java 类型做白名单校验。

## 测试

```bash
mvn test
```

测试覆盖：

- traceId 透传与非法值替换。
- 统一响应中的 traceId。
- 本地/内存文件存储、路径穿越、大小限制和临时文件清理。
- 文件下载对非法元数据 `Content-Type` 的安全回退。
- 缓存 TTL、类型校验、Spring Cache 适配、命名空间清理和并发单飞。
- MQ 异步投递、重试、死信容量和队列满拒绝。
- JWT 签发/解析、篡改拒绝、短密钥拒绝、密码校验。
- Refresh Token 一次性消费、过期、批量撤销、单用户上限和刷新轮换。
- 访问令牌 `jti`、登出即时撤销、撤销记录过期清理。
- 数据权限规则、字段读取、上下文嵌套恢复和自动装配开关。
- 代码生成校验、默认模板、ZIP 归档、自定义模板和自动装配开关。
- 模板应用端到端：登录、401、403、Bearer 大小写兼容、Refresh Token 轮换/登出、登出后访问令牌失效、参数校验、404、405、415、数据权限、代码生成、文件上传下载删除、缓存命中、MQ 消费。
- CORS 预检放行、非法来源统一 JSON 拒绝、实际响应暴露 traceId。
- JWT 过滤器保留已有安全上下文。

## 刻意保留的边界

这是一套练习框架，不是开箱即用的生产级基础设施。当前明确不包含：

- Redis、Kafka、RocketMQ、对象存储、数据库等外部中间件客户端。
- 分布式令牌撤销与集群同步、全局强制下线、多设备会话管理、登录限流、验证码、多因素认证。
- 分布式缓存一致性、消息幂等表、事务消息、跨节点死信补偿。
- 文件分片、断点续传、病毒扫描、对象存储签名 URL。
- 部门/组织架构级数据权限、MyBatis/JPA SQL 拦截器、代码落盘、工程级模板组合、网关、服务注册发现。

这些能力都设计成可替换实现，当前工程已经覆盖了从框架地基到进阶组件的完整练习链路。

