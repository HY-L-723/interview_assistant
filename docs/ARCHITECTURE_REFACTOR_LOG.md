# Architecture Refactor Log

## 本次改造目标

将当前项目从“能运行的教学/原型项目”提升为“更接近企业协作规范的前后端分离项目”。本次重点不是大拆大改业务，而是补齐配置治理、可观测性、环境隔离和架构文档。

## 已完成改造

1. 后端配置治理

   新增 `config/properties` 包，把 `app`、`jwt`、`llm`、`interview` 这几类配置建模为强类型配置类。这样做的好处是配置项集中、可校验、IDE 可提示，后续也更容易写测试。

2. 健康检查和监控入口

   引入 Spring Boot Actuator，并开放 `/actuator/health` 和 `/actuator/info`。这相当于给系统增加“体检接口”，上线后负载均衡或监控平台可以判断服务是否健康。

3. 请求追踪

   新增 `RequestIdFilter`，每个请求都会有 `X-Request-Id`。如果前端、网关或调用方传了这个头，后端会沿用；如果没有，后端自动生成。排查线上问题时，可以用同一个 requestId 串起日志。

4. 安全边界清理

   重写 `SecurityConfig`，明确只放行认证接口、上传资源和健康检查，其它请求默认需要登录。安全规则更直观，也减少后续新增接口时漏鉴权的风险。

5. 前端环境隔离

   新增 `frontend/src/config/app.js` 和 `frontend/.env.example`，把 API 基础路径、开发端口、代理目标改为环境变量控制。以后部署到测试环境或生产环境，不需要修改源码。

6. SSE 请求整理

   重写前端 `chat.js` 和 `interview.js` 中的 SSE 读取逻辑，去掉乱码提示和调试噪声，让流式通信代码更可读、更容易维护。

7. 配置文件清理

   重写 `application.yml`，统一 UTF-8、上传大小、Actuator、日志 requestId、dev/prod 数据库策略等配置。原文件中存在编码损坏的中文注释，本次已清理。

## 暂未改造但建议后续推进

- 数据库迁移：引入 Flyway 或 Liquibase。
- 多模型网关：将 DeepSeek 调用抽象为可插拔 Provider。
- 分布式限流：当前每日 50 次限流在数据库侧统计，生产建议迁移到 Redis 或网关。
- CI/CD：增加前端 lint/test/build、后端 test/package、安全扫描。
- 文件存储：生产建议从本地目录迁移到对象存储。
- 统一错误码：当前已有 `Result`，后续可引入业务错误码枚举和错误码文档。
