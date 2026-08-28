# 微服务架构改造方案 — 变更说明文档

## 概述

本次改造针对校园二手交易平台后端的微服务架构进行系统性重构，消除原有的"共享数据库反模式""未死亡单体""跨切面重复代码"三大核心问题，并引入领域驱动设计（DDD）的分层理念。

---

## 一、改造前后架构对比

### 改造前（问题架构）

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3)                          │
└──┬─────────────────────┬───────────────────────────────┘
   │ /api/backAll         │ /api/* (gateway)
   ▼                     ▼
┌──────────────┐   ┌──────────┐
│ service-main │   │ Gateway  │ ← 绕过网关，直连
│  (胖单体)     │   │ (9005)   │
│  8088        │   └────┬─────┘
│ ·商品/用户/订单 │        │
│ ·AI/Milvus   │        ├──────────┬──────────┬──────────┐
│              │        ▼          ▼          ▼          ▼
│ ·文件上传    │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ ·WebSocket   │  │ user │ │product│ │ order│ │comment│
│ ·权限/角色   │  │ 8081 │ │ 8083  │ │ 8082 │ │ 8084 │
└──────┬───────┘  └──────┘ └──┬───┘ └──┬───┘ └──────┘
       │                      │        │
       │              ┌───────┴──┐ ┌───┴────────┐
       │              │ ❌ user  │ │ ❌ goods   │
       │              │   表直连  │ │   表直连    │
       │              └──────────┘ └────────────┘
       │              ┌───────┐    ┌───────────┐
       │              │ ❌    │    │ ❌ user   │
       │              │ order│    │   表直连    │
       │              │ 表直连 │   └───────────┘
       ▼              └───────┘
   (数据库) ← 所有服务共享同一数据库
```

### 改造后（目标架构）

```
┌────────────────────────────────────────────────────────────┐
│                    前端 (Vue 3)                            │
└──┬──────────────────────────┬─────────────────────────────┘
   │ 经 Gateway (9005)         │ 轻量 BFF 层
   ▼                          ▼
┌──────────┐          ┌──────────────┐
│ Gateway  │          │ service-main │ ← 只做聚合编排
│ 路由/鉴权 │          │ (BFF)        │
│ 限流/日志 │          │ ·Dashboard   │
└────┬─────┘          │ ·FileUpload  │
     │                │ ·WebSocket   │
     │                │ ·角色/权限   │
     │                └──────┬───────┘
     │                       │ Feign调用
     ├──────────┬──────────┬─┼────┬──────────┐
     ▼          ▼          ▼ ▼     ▼          ▼
  ┌──────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ ┌──────┐
  │service│ │ service  │ │ service  │ │service│ │service│
  │ user  │ │ product  │ │  order   │ │comment│ │collect│
  │ 8081  │ │  8083    │ │  8082    │ │ 8084  │ │ 8085  │
  └──┬───┘ └──┬───────┘ └──┬───────┘ └──┬───┘ └──┬───┘
     │        │            │            │        │
     ▼        ▼            ▼            ▼        ▼
  ┌──────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ ┌──────┐
  │用户库 │ │ 商品库    │ │ 订单库    │ │评论库 │ │收藏库 │
  │ 私有  │ │ +Milvus  │ │ 私有      │ │ 私有 │ │ 私有  │
  └──────┘ └──────────┘ └──────────┘ └──────┘ └──────┘
                        │
                        │ Feign 调用（不再直连数据库）
                        ▼
                 ┌──────────┐
                 │ service  │
                 │  chat    │
                 │  8086    │
                 └──────────┘

所有服务共享 ──→ common 模块（JwtUtil, ThreadLocalUtil 等）
```

---

## 二、新增模块说明

### 1. `common` 公共基础设施模块

| 文件 | 说明 |
|------|------|
| `util/JwtUtil.java` | JWT 令牌解析（统一密钥 `itheima`） |
| `util/ThreadLocalUtil.java` | 用户上下文管理 |
| `util/Md5Util.java` | MD5 加密 |
| `util/RedisDistributedLock.java` | Redis 分布式锁模板 |
| `interceptor/LoginInterceptor.java` | 登录拦截器模板（参考实现） |
| `config/FeignAuthConfig.java` | Feign Token 传播配置模板 |
| `exception/GlobalExceptionHandler.java` | 全局异常处理模板 |
| `exception/SentinelBlockHandler.java` | Sentinel 限流降级统一处理 |

**设计原则**：common 模块提供**静态工具类和代码模板**，不包含可扫描的 Spring Bean（避免各服务间的 bean 冲突）。各服务按需复制或引用。

---

## 三、各服务改造详情

### 3.1 `service-product`（商品服务）

#### 变更点

| 类别 | 变更内容 |
|------|---------|
| ❌ 删除 | `mapper/UserMapper.java` — 跨域数据库访问 |
| ❌ 删除 | `mapper/OrderTraceMapper.java` — 跨域数据库访问 |
| 🆕 新增 | `feign/UserFeignClient.java` — 通过 Feign 调用 service-user |
| 🆕 新增 | `controller/ProductInternalController.java` — 内部 API 端点 |
| 🆕 新增 | `infrastructure/milvus/` — Milvus 向量检索（从 service-main 迁移） |
| 🆕 新增 | `infrastructure/ai/` — AI Embedding + QwenChatUtil（从 service-main 迁移） |
| 🆕 新增 | `service/AiService.java` + `impl/` — AI 对话服务（从 service-main 迁移，模型改为 qwen3:4b） |
| 🆕 新增 | `controller/AIController.java` — AI 控制器（从 service-main 迁移） |
| 🆕 新增 | `config/MilvusInitConfig.java` — Milvus 启动初始化 |
| 🆕 新增 | `agent/` — CampusAssistant 智能体包（从 service-main 迁移） |
| 🆕 新增 | `agent/config/LangChain4jConfig.java` — LangChain4j + Ollama 配置（模型统一 qwen3:4b） |
| 🆕 新增 | `agent/tool/GoodsSearchTool.java` — 商品搜索工具（使用本地 GoodsMapper） |
| 🆕 新增 | `agent/tool/OrderQueryTool.java` — 订单查询工具（使用 Feign → service-order） |
| 🆕 新增 | `agent/tool/PlatformRuleTool.java` — 平台规则工具 |
| 🆕 新增 | `feign/OrderFeignClient.java` — 调用 service-order 获取订单详情 |
| 🆕 新增 | `mapper/ShopCategoryMapper.list()` — 添加查询全部分类方法（供工具使用） |
| 🔄 修改 | `ProductDomainServiceImpl` — 使用 Feign 替代 UserMapper |
| 🔄 修改 | `ProductDomainService` 接口 — 新增 `updateStock()`、`findAllOnSale()`、`ragSearchWithSummary()` |
| 🔄 修改 | `GoodsMapper.java` — 新增 `listOnSaleGoods()` 方法 |
| 🔄 修改 | `AiServiceImpl` — 模型从 `llama3.2-vision` 改为 `qwen3:4b` |
| 🔄 修改 | `ragSearch()` — 从空实现改为完整 Milvus 向量搜索（Embedding → Milvus → DB 查询） |
| 🔄 修改 | `AIController` — 新增 `/ai/agent/chat` 和 `/ai/agent/chat/stream` 智能体端点 |

#### AI 端点一览

| 端点 | 类型 | 模型 | 说明 |
|------|------|------|------|
| `POST /ai/chat` | SSE 流式 | qwen3:4b (Ollama) | 纯对话（无工具），原 AiServiceImpl |
| `POST /ai/generateGoodsDesc` | 同步 | qwen3:4b (Ollama) | AI 商品描述生成 |
| `POST /ai/agent/chat` | 同步 | qwen3:4b (Ollama/LangChain4j) | 智能体对话（含商品搜索/订单查询/规则工具） |
| `POST /ai/agent/chat/stream` | SSE 流式 | qwen3:4b (Ollama/LangChain4j) | 智能体流式对话 |
| `POST /internal/goods/ragSearch` | 同步 | text-embedding-v3 (DashScope) | RAG 向量搜索（Milvus） |
| `POST /internal/goods/ragSearchWithSummary` | 同步 | text-embedding-v3 + qwen-turbo | RAG 搜索 + AI 摘要一站式 |

#### 内部 API 端点

| 端点 | 方法 | 说明 | 调用方 |
|------|------|------|--------|
| `/internal/goods/{id}` | GET | 获取商品详情 | service-order, service-comment, service-collect |
| `/internal/goods/listByIds` | POST | 批量查询商品 | service-order, service-collect |
| `/internal/goods/allOnSale` | GET | 所有在售商品 | service-order（库存缓存初始化） |
| `/internal/goods/stock` | PUT | 更新库存 | service-order（下单/取消） |
| `/internal/goods/ragSearch` | POST | RAG 向量搜索 | service-main BFF |
| `/internal/goods/ragSearchWithSummary` | POST | RAG 搜索 + AI 摘要 | service-main BFF |

---

### 3.2 `service-order`（订单服务）

#### 变更点

| 类别 | 变更内容 |
|------|---------|
| ❌ 删除 | `mapper/GoodsMapper.java` — 跨域数据库访问 |
| ❌ 删除 | `mapper/UserMapper.java` — 跨域数据库访问 |
| 🆕 新增 | `feign/ProductFeignClient.java` — 调用 service-product |
| 🆕 新增 | `feign/UserFeignClient.java` — 调用 service-user |
| 🔄 修改 | `OrderDomainServiceImpl` — 全面使用 Feign 替代 GoodsMapper/UserMapper |
| 🔄 修改 | `OrderWebConfig` — 新增 `/order/internal/**` 排除路径 |

#### 数据访问方式变更

```
改造前: OrderService → GoodsMapper → goods 表 (直接SQL)
改造后: OrderService → ProductFeignClient → service-product HTTP API
```

```
改造前: OrderService → UserMapper → user 表 (直接SQL)
改造后: OrderService → UserFeignClient → service-user HTTP API
```

---

### 3.3 `service-main`（BFF 聚合层）

#### 变更点

| 类别 | 变更内容 |
|------|---------|
| 🆕 新增 | `feign/ProductFeignClient.java` — 调用 service-product 全部 API |
| 🆕 新增 | `feign/UserFeignClient.java` — 调用 service-user 全部 API |
| 🆕 新增 | `feign/OrderFeignClient.java` — 调用 service-order 全部 API |
| 🆕 新增 | `config/FeignAuthConfig.java` — 转发 Authorization 头到下游 Feign 调用 |
| 🔄 修改 | `@EnableFeignClients(basePackages = {"com.itheima.feign"})` — 限制扫描范围 |
| 🔄 重写 | `service/impl/GoodsServiceImpl.java` — 完全使用 ProductFeignClient 替代本地 Mapper |
| 🔄 重写 | `service/impl/UserServiceImpl.java` — 完全使用 UserFeignClient 替代本地 Mapper |
| ❌ 删除 | AI/Milvus 相关文件迁移到 service-product |
| ❌ 删除 | `mapper/CommentMapper.java` — 评论数据访问已迁移到 service-comment |
| ❌ 删除 | `mapper/GoodsCollectMapper.java` — 收藏数据访问已迁移到 service-collect |
| ❌ 删除 | `mapper/ChatMessageMapper.java` + `mapper/ChatSessionMapper.java` — 消息/会话迁移到 service-chat |
| ❌ 删除 | 智能体包 `agent/`（CampusAssistant + LangChain4jConfig + 3 个 Tool + Controller）— 全部迁移到 service-product |
| ❌ 删除 | `util/QwenChatUtil.java` — RAG 摘要功能迁移到 service-product，合并入 ragSearchWithSummary() |
| ↩️ 保留 | Dashboard、FileUpload、WebSocket、权限/角色等独特功能（仍使用本地 Mapper） |

**定位变化**：从"包含全部业务逻辑的胖单体" → "轻量 BFF 聚合层"
- 领域业务逻辑 → 下沉到 domain service
- 跨服务数据聚合 → Feign 调用 + 本地组装
- 独特能力 → 保留（Dashboard 统计、文件上传、WebSocket 推送、权限管理等）

#### BFF 层 Feign 调用链路

```
service-main (BFF)
  ├── GoodsServiceImpl → ProductFeignClient → service-product (商品CRUD、RAG搜索)
  ├── UserServiceImpl  → UserFeignClient    → service-user    (用户CRUD、注册、密码)
  ├── (待迁移) AddressServiceImpl → 本地 AddressMapper
  ├── (待迁移) DashboardServiceImpl → 本地 DashboardMapper
  ├── (待迁移) PermissionServiceImpl → 本地 PermissionsMapper
  ├── (待迁移) RolesServiceImpl → 本地 RolesMapper
  └── (待迁移) ShopCategoryServiceImpl → 本地 ShopCategoryMapper + UserMapper
```

---

### 3.4 其他服务

| 服务 | 变更 |
|------|------|
| `service-user` | 已有 `/user/internal/{id}` 和 `/user/internal/listByIds` 内部端点 ✅ |
| `service-comment` | 已使用 Feign 调用 service-product ✅ |
| `service-collect` | 已使用 Feign 调用 service-product ✅ |
| `service-chat` | 独立聊天域，无跨库访问 ✅ |

---

## 四、POM 依赖变更

### 根 POM (`cloud-demo/pom.xml`)
- 新增模块: `<module>common</module>`

### services 聚合 POM (`services/pom.xml`)
- 新增依赖: `org.example:common:0.0.1-SNAPSHOT`

### service-product POM (`services/service-product/pom.xml`)
- 新增依赖: dashscope-sdk-java, openai-java, milvus-sdk-java, langchain4j, reactor-core（AI 能力迁移）

---

## 五、模块依赖关系图

```
cloud-demo
  ├── model          ← 共享 POJO/DTO/VO（计划后续拆分为各服务私有）
  ├── common         ← 公共工具类（无Spring Bean扫描，纯静态引用）
  │   ├── util/      ← JwtUtil, ThreadLocalUtil, Md5Util
  │   └── exception/ ← 异常处理模板
  ├── gateway        ← Spring Cloud Gateway（路由、鉴权、限流）
  ├── services/
  │   ├── service-main  → Feign→ {product, user, order}
  │   ├── service-user  → 内部API供 {product, order, main} 调用
  │   ├── service-product → 内部API供 {order, comment, collect} 调用
  │   │                   → Feign→ user
  │   │                   → Feign→ order
  │   │                   └── 内嵌 Milvus + AI Agent
  │   ├── service-order  → Feign→ {product, user}
  │   ├── service-comment → Feign→ product ✅
  │   ├── service-collect → Feign→ product ✅
  │   └── service-chat    → Feign→ user
```

---

## 六、Gateway 路由规划

| 前端路径 | 后端服务 | 说明 |
|----------|----------|------|
| `/api/user/**` | `lb://service-user` | 用户相关 |
| `/api/product/**` | `lb://service-product` | 商品相关 |
| `/api/ai/**` | `lb://service-product` | AI 客服/智能体 |
| `/api/order/**` | `lb://service-order` | 订单相关 |
| `/api/comment/**` | `lb://service-comment` | 评论相关 |
| `/api/collect/**` | `lb://service-collect` | 收藏相关 |
| `/api/chat/**` | `lb://service-chat` | 聊天相关 |
| `/api/bff/**` | `lb://service-backAll` | BFF 聚合（仪表盘/文件上传等） |
| `/api/backAll/**` | `lb://service-backAll` | 旧版 BFF 路径（兼容） |

**注意**：需在前端 `vite.config.js` 中将 `/api/backAll` → service-main 的直连代理改为统一走 gateway，推荐逐步迁移到 `/api/bff/**` 路径。

---

## 七、遗留问题和后续建议

### 🔴 P0 - 待处理

| 问题 | 说明 | 建议方案 |
|------|------|---------|
| model 模块过胖 | 所有域共享同一个 model JAR，变更影响大 | 按域拆分为 `product-api`、`order-api` 等独立模块 |
| Seata 分布式事务 | 依赖已注释，订单创建跨 product/order 无事务保证 | 引入 Seata AT 模式或采用 Saga/TCC 模式 |
| 剩余本地 Mapper | service-main 仍有 Address/Dashboard/Permission/Roles/ShopCategory/Goods/User/Order Mapper 被本地服务使用 | 逐步迁移为 Feign 调用对应微服务 |

### 🟡 P1 - 建议优化

| 问题 | 说明 | 建议方案 |
|------|------|---------|
| 重复的跨切面代码 | 每个服务仍有自己的 JwtUtil/LoginInterceptor | 逐步迁移到 common 模块（需要解决 Spring Bean 冲突） |
| 缺少熔断降级 | Feign 调用只有简单 try-catch | 统一配置 Sentinel Feign 降级 |
| Address 表归属 | 订单服务直接读取 address 表 | 下个迭代将地址管理归入 user 服务或独立 address 服务 |
| 前端直连 service-main | `/api/backAll` 绕过 gateway | 更新 vite.config.js，路由统一走 gateway |

### 🟢 P2 - 远期规划

| 项目 | 说明 |
|------|------|
| DDD 完整分层 | 为每个服务引入 application/domain/infrastructure 包结构 |
| 防腐层（ACL） | Feign 调用处定义专用 DTO，通过 Assembler 转换 |
| CI/CD 流水线 | 独立构建和部署每个微服务 |
| 容器化 | 为每个服务编写 Dockerfile + docker-compose |

---

*文档版本：2.2.0*
*生成日期：2026-06-06*
