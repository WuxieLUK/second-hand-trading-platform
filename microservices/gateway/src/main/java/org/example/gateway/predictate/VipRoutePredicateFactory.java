package org.example.gateway.predictate;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Component
public class VipRoutePredicateFactory extends AbstractRoutePredicateFactory<VipRoutePredicateFactory.Config> {
    public VipRoutePredicateFactory() {
        super(Config.class);
    }


    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return new GatewayPredicate() {
            @Override
            public boolean test(ServerWebExchange serverWebExchange) {
                ServerHttpRequest request = serverWebExchange.getRequest();
                String param = request.getQueryParams().getFirst(config.param);
                if (StringUtils.hasText(param) && param.equals(config.value)){
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("param", "value");
    }


    //可以配置的参数
    @Validated
    public static class Config {
        @NotEmpty
        private String param;
        @NotEmpty
        private String value;
        public @NotEmpty String getParam(){
            return param;
        }

        public void setParam(@NotEmpty String param){
            this.param = param;
        }
        public @NotEmpty String getValue(){
            return value;
        }
        public void setValue(@NotEmpty String value){
            this.value = value;
        }
    }
}
