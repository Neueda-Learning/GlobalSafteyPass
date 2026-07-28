# Travel Assistant – Mobile Banking Travel Support

一个可运行的 Java 21 / Spring Boot 3 课程与作品集项目。它模拟手机银行中的旅行助手：出发前检查银行卡，旅途中追踪预算、解释拒付，并用内部规则与可替换的外部评分服务识别可疑境外交易。

> 本项目只处理模拟数据，不执行真实资金交易。外部汇率仅用于预算展示估算；最终入账金额始终应以银行交易系统提供的 `billingAmount` 为准。

## 功能

- 旅行 CRUD、严格日期/预算/卡片归属校验
- 10 条独立 Travel Readiness 规则与 0–100 分准备度
- 境外支付、冻结/解冻、支付及取现限额快捷操作
- 交易事件幂等处理、旅行自动匹配、预算仪表盘
- Frankfurter 汇率 API（3 秒超时、内存缓存、1.0 安全降级）
- 12 条可配置反欺诈规则 + 外部评分 + 银行历史客户画像三层风险决策
- 外部反欺诈抽象与本地 Mock 实现（可替换 Sift、Feedzai 或银行内部系统）
- 可读的拒付原因与处理建议
- 告警确认、盗刷上报、一键冻卡和不可由 API 删除的审计日志
- Trusted-device / App PIN / SMS OTP 多方式认证、短期 Bearer 会话与客户数据隔离
- Swagger、H2 开发环境、MySQL 生产环境、Docker Compose
- 响应式手机银行网页

## 架构

```text
Card Management
       │
       ├── CardCapabilityService
       │       ├── supported currencies
       │       ├── overseas-payment eligibility
       │       └── card/account/payment capability check
       │
       ├── ExchangeRateService + JourneyExchangeRateService
       │       ├── real-time exchange rate
       │       ├── FX calculation and fallback
       │       └── persisted card FX rates
       │
       ├── TravelCardRecommendationService + readiness rules
       │       ├── destination and currency matching
       │       ├── eligible-card filtering
       │       └── explainable card recommendation
       │
       └── PaymentRecoveryService
               ├── decline classification and explanation
               ├── retry on the original transaction
               ├── alternate-card authorization
               └── recovery status and timeline
```

Controller 只负责认证上下文和 HTTP contract；业务判断集中在上述服务，Repository 负责 MySQL 持久化。`recovery_status` 使用可扩展 `VARCHAR`，避免工作流新增状态时受 MySQL ENUM 限制。

主要源码位于 `src/main/java/com/travelassistant`，按 `controller`、`service`、`repository`、`model`、`dto`、`rule`、`integration`、`exception`、`config`、`security` 分层。金额一律使用 `BigDecimal`，日期使用 `LocalDate`，事件时间使用 `Instant`。系统不会存储 CVV、完整卡号或密码。

## 数据库

实体表包括 `trips`、`cards`、`accounts`、`card_fx_rates`、`travel_transactions`、`fraud_alerts`、`support_cases`、`readiness_assessments`、`audit_logs`。本地默认 profile 和 Docker 都使用 MySQL 8.4，种子数据由 `DataSeeder` 直接写入 MySQL。H2 仅供自动化测试以及显式指定 `dev` profile 时使用。

演示数据聚焦 Tokyo 支付恢复、Paris 正常旅行与实时汇率、Nice 卡片到期检查三个典型场景。默认演示客户为 `customer-001`。

## 本地运行

要求 Java 21、Maven 3.9+ 和正在运行的 MySQL。先启动 MySQL：

```bash
docker compose up -d mysql
```

再启动应用：

```bash
mvn spring-boot:run
```

打开：

- 手机银行页面：<http://localhost:8080>
- Swagger：<http://localhost:8080/swagger-ui.html>
默认 MySQL 数据库为 `travel_assistant`，用户名 `travel_user`，密码 `travel_password`。这些值可以通过 `DATABASE_URL`、`DATABASE_USERNAME` 和 `DATABASE_PASSWORD` 修改。

如需临时使用 H2：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

运行测试：

```bash
mvn test
```

## Docker + MySQL

```bash
docker compose up --build
```

Compose 启动 `travel-assistant-app` 和 `mysql`，数据库为 `travel_assistant`，应用端口为 `8080`。首次构建会下载 Maven 与 Docker 依赖。

## API 示例

先创建认证挑战并完成验证。系统默认推荐无需短信的 trusted-device 流程，App PIN 为离线备用方式，SMS OTP 仍可由用户主动选择。演示设备断言为 `trusted-device-demo`，App PIN 为 `2580`，SMS code 为 `246810`；生产环境应替换为 WebAuthn/Passkey、真实设备绑定与短信供应商。

```bash
curl -X POST http://localhost:8080/api/auth/start \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer-001"}'
```

使用返回的 `challengeId`：

```bash
curl -X POST http://localhost:8080/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"challengeId":"CHALLENGE_ID","method":"TRUSTED_DEVICE","credential":"trusted-device-demo"}'
```

后续 API 使用返回的短期 token：`Authorization: Bearer ACCESS_TOKEN`。

创建旅行：

```bash
curl -X POST http://localhost:8080/api/travel/trips \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ACCESS_TOKEN" \
  -d '{"destinationCountry":"Japan","destinationCity":"Osaka","startDate":"2026-10-01","endDate":"2026-10-08","budget":2200,"budgetCurrency":"USD","preferredCardId":"card-001"}'
```

提交可疑交易：

```bash
curl -X POST http://localhost:8080/api/travel/transactions/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ACCESS_TOKEN" \
  -d '{"transactionId":"txn-demo-risk","customerId":"customer-001","cardId":"card-002","merchantName":"Paris Luxury","merchantCountry":"France","merchantCity":"Paris","merchantCategory":"SHOPPING","originalAmount":1800,"originalCurrency":"USD","transactionTime":"2026-08-12T13:30:00Z","transactionType":"PURCHASE","status":"APPROVED"}'
```

## 外部服务配置

```text
FX_API_BASE_URL
FX_API_ENABLED
FX_API_KEY
FRAUD_API_BASE_URL
FRAUD_API_KEY
FRAUD_API_MODE
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

`ExchangeRateProvider` 隔离汇率供应商。失败时先使用同一 key（如 `FX:USD:JPY:2026-08-12`）的最近缓存；仍无缓存则使用 1.0 并标记 `estimated=true`。`FraudDetectionClient` 隔离外部反欺诈服务；默认 `FRAUD_API_MODE=mock` 保证离线演示，将其设为 `http` 后会向 `FRAUD_API_BASE_URL/score` 发起带可选 Bearer API key 的 3 秒超时 HTTP 请求。外部不可用时交易仍会保存，并完全使用内部规则分数。

## 评分说明

准备度从 100 分开始扣分，结果限制在 0–100：80–100 为 `READY`，50–79 为 `ACTION_REQUIRED`，0–49 为 `NOT_READY`。

反欺诈最终评分由内部规则 50%、外部风险 API 30%、银行历史客户画像 20% 组成。客户画像从历史交易动态计算平均消费、常用国家和常用商户类别。外部服务不可用时，系统自动使用内部规则 70% + 客户画像 30%。0–24 允许，25–49 监控，50–74 要求确认，75–100 阻断并告警。权重和规则分值都可在 `application.yml` 调整。

实时汇率接口：

```bash
curl "http://localhost:8080/api/travel/exchange-rates/live?base=USD&quote=JPY" \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

首页的 “Demo suspicious payment” 会提交一笔发生在 France 的 1,800 USD 高金额交易，与已登记的 Japan 旅程冲突。系统会综合内部规则、外部评分和客户历史画像生成可解释告警。

种子数据库还包含退款、撤销、重复扣款、旅行日期外交易、目的地不一致、高金额交易、连续拒付、网络错误和境外 ATM 等场景。

## 后续改进

- 将内存汇率缓存替换为 Redis 并增加 TTL
- 接入 OAuth2/OIDC 与真实银行客户上下文
- 使用 Flyway 管理 MySQL schema
- 将交易事件入口改为 Kafka，并增加 outbox
- 对敏感审计日志增加 WORM 存储和签名
- 接入真实卡核心、Feedzai/Sift 和卡组织风险信号
