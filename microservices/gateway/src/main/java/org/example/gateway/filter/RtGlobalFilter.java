package org.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j//全局过滤时间
public class RtGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String uri = request.getURI().toString();
        long startTime = System.currentTimeMillis();
        log.info("uri:{},startTime:{}",uri,startTime);
        //前置逻辑
        Mono<Void> mono = chain.filter(exchange)
                .doFinally((result) -> {
                    //后置逻辑
                    long endTime = System.currentTimeMillis();
                    log.info("uri:{},endTime:{},costTime:{}",uri,endTime,endTime-startTime);
                });
        return mono;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
