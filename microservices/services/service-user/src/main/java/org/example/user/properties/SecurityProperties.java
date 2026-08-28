package org.example.user.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.token")
public class SecurityProperties {
    private String header = "Authorization";
    private long expireHours = 12;
}
