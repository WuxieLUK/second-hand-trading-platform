package org.example.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "chat.business")
public class ChatProperties {
    private int defaultPageSize = 20;
}
