package org.example.comment.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "comment.business")
public class CommentProperties {
    private int defaultPageSize = 10;
}
