# Back-blog 微服务拆分说明

本次将 `services/Back-blog` 中的二手交易平台核心能力，按业务域拆成 3 个独立微服务：

## 1. `service-user`

- 负责用户注册、登录、资料维护、头像、密码、用户列表。
- 已整理的标准结构：
  - `config`
  - `controller`
  - `exception`
  - `feign`
  - `interceptor`
  - `mapper`
  - `properties`
  - `service`
  - `util`
  - `resources`

## 2. `service-order`

- 负责订单创建、订单列表、详情、状态流转、退款、发货、取消。
- 已整理的标准结构：
  - `config`
  - `controller`
  - `exception`
  - `feign`
  - `interceptor`
  - `mapper`
  - `properties`
  - `service`
  - `util`
  - `resources`

## 3. `service-product`

- 负责商品发布、分页查询、详情、修改、删除、上下架、卖家信息查询。
- 已整理的标准结构：
  - `config`
  - `controller`
  - `exception`
  - `feign`
  - `interceptor`
  - `mapper`
  - `properties`
  - `service`
  - `resources`

## 当前处理策略

- 保留 `Back-blog` 作为原始单体参考，不主动删除，避免影响你现有代码。
- 新增的 3 个服务以更清晰的业务域边界承接原有核心能力。
- 复杂的 AI / Chat / 搜索 / 评论 / 收藏等能力暂未继续拆入本轮三个核心服务，避免把本次目标范围拉散。

## 下一步建议

1. 继续把 `comment / collect / chat / search` 拆成独立服务。
2. 把通用 JWT、异常、返回体、鉴权逻辑抽到 `common-starter`。
3. 再统一 Nacos、Gateway 路由和 OpenFeign 调用链。
