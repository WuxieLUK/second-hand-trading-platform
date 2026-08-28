package org.example.product.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "product.business")
public class ProductBusinessProperties {
    private int defaultPageSize = 10;
}
