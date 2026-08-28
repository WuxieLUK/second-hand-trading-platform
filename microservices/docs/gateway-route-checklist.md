# Gateway 路由清单

当前网关配置文件：

- `gateway/src/main/resources/application-route.yml`

已经配置好的路由如下。

## 1. 用户服务

- 网关前缀：`/api/user/**`
- 转发目标：`lb://service-user`
- 转发后路径：`/**`

示例：

- `/api/user/login -> service-user /login`
- `/api/user/info -> service-user /info`

## 2. 商品服务

- 网关前缀：`/api/product/**`
- 转发目标：`lb://service-product`
- 转发后路径：`/**`

示例：

- `/api/product/goodsopenlist -> service-product /goodsopenlist`
- `/api/product/detail/1 -> service-product /detail/1`

## 3. 订单服务

- 网关前缀：`/api/order/**`
- 转发目标：`lb://service-order`
- 转发后路径：`/**`

示例：

- `/api/order/create -> service-order /create`
- `/api/order/list -> service-order /list`

## 4. 评论服务

- 网关前缀：`/api/comment/**`
- 转发目标：`lb://service-comment`
- 转发后路径：`/**`

示例：

- `/api/comment/add -> service-comment /add`
- `/api/comment/commentList/1 -> service-comment /commentList/1`

## 5. 收藏服务

- 网关前缀：`/api/collect/**`
- 网关前缀别名：`/api/goodsCollect/**`
- 转发目标：`lb://service-collect`
- 转发后路径：`/**`

示例：

- `/api/collect/myList -> service-collect /myList`
- `/api/goodsCollect/add/1 -> service-collect /add/1`

## 6. 聊天服务

- 网关前缀：`/api/chat/**`
- 转发目标：`lb://service-chat`
- 转发后路径：`/**`

示例：

- `/api/chat/myList -> service-chat /myList`
- `/api/chat/send -> service-chat /send`

## 7. 鉴权约定

- 除登录、注册、商品公开查询、评论公开查询外，其余接口统一带 `Authorization` 请求头。
- 服务间 OpenFeign 调用会自动透传当前请求的 `Authorization`。
- 这意味着内部接口不再需要手工在拦截器里单独放开。
