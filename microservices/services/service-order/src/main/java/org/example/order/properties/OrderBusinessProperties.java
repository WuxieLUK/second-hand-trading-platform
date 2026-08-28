package org.example.order.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "order.business")
public class OrderBusinessProperties {
    private long lockTimeoutSeconds = 30;
    private long acquireTimeoutMillis = 5000;
}
