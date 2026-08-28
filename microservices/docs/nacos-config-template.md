# Nacos 配置模板

下面这套模板对应当前项目已经拆好的 6 个核心微服务和 1 个网关：

- `service-user`
- `service-product`
- `service-order`
- `service-comment`
- `service-collect`
- `service-chat`
- `gateway`

建议在 Nacos 中按以下方式准备配置。

## 1. 推荐命名方式

每个服务准备一个独立 `Data ID`：

- `service-user.yml`
- `service-product.yml`
- `service-order.yml`
- `service-comment.yml`
- `service-collect.yml`
- `service-chat.yml`
- `gateway.yml`

分组建议统一使用：

- `DEFAULT_GROUP`

命名空间如果你没有特别区分开发、测试、生产，可以先使用：

- `public`

## 2. service-user.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: service-user
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8719
      eager: true

feign:
  sentinel:
    enabled: true
```

## 3. service-product.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: service-product
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8721
      eager: true

feign:
  sentinel:
    enabled: true
```

## 4. service-order.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: service-order
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8720
      eager: true

feign:
  sentinel:
    enabled: true
```

## 5. service-comment.yml

```yaml
server:
  port: 8084

spring:
  application:
    name: service-comment
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8723
      eager: true

feign:
  sentinel:
    enabled: true
```

## 6. service-collect.yml

```yaml
server:
  port: 8085

spring:
  application:
    name: service-collect
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8724
      eager: true

feign:
  sentinel:
    enabled: true
```

## 7. service-chat.yml

```yaml
server:
  port: 8086

spring:
  application:
    name: service-chat
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8725
      eager: true

feign:
  sentinel:
    enabled: true
```

## 8. gateway.yml

```yaml
server:
  port: 9005

spring:
  application:
    name: gateway
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8722
      eager: true
```

## 9. 启动顺序建议

1. 启动 MySQL
2. 启动 Redis
3. 启动 Nacos
4. 启动 Sentinel Dashboard
5. 启动 `service-user`
6. 启动 `service-product`
7. 启动 `service-order`
8. 启动 `service-comment`
9. 启动 `service-collect`
10. 启动 `service-chat`
11. 启动 `gateway`
