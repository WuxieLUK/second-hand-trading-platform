package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

// 用户行为实体
@Data
public class UserBehavior {
    private Long id;
    private Long userId;
    private Long articleId;
    private Integer behaviorType;
    private LocalDateTime behaviorTime;
    private Integer stayDuration;
}
