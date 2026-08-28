package org.example.collect.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "collect.business")
public class CollectProperties {
    private int defaultPageSize = 10;
}
