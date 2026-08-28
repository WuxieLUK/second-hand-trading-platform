# 校易通 · 前端（Vue 3）

校园二手交易平台的前端应用，基于 **Vue 3 + Vite + Element Plus + Pinia** 构建。

## 技术栈

- Vue 3（组合式 API）· Vue Router · Pinia（含持久化插件）
- Element Plus · ECharts（数据统计大屏）· Axios · WebSocket
- 富文本 / Markdown 编辑器（md-editor-v3、vue-quill 等）

## 快速开始

```bash
npm install
npm run dev      # 开发模式，默认 http://localhost:5173
npm run build    # 生产构建，产物输出到 dist/
npm run preview  # 预览构建产物
```

## 后端指向配置

后端地址通过 [`frontend/.env.development`](.env.development) 配置：

- **微服务模式（默认）**：`VITE_API_BASE_URL="http://localhost:9005"`，请求直连 Spring Cloud Gateway；
- **单体模式**：`VITE_API_BASE_URL=""` 且 `VITE_API_PROXY_TARGET="http://localhost:8080"`，由 Vite 开发代理将 `/api/backAll` 前缀剥离后转发到单体后端。

## 目录结构

```
src/
├── api/        # 接口封装（user / goods / order / chat / AI ...）
├── views/      # 页面（首页、商品、订单、聊天、AI、后台管理等）
├── stores/     # Pinia 状态（token、用户信息等）
├── components/ # 公共组件
├── utils/      # 请求封装、工具函数
└── router/     # 路由配置
```
