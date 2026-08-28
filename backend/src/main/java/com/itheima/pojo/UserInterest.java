package com.itheima.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 用户兴趣实体
@Data
public class UserInterest {
    private Long id;
    private Long userId;
    private Integer interestType;
    private Long interestId;
    private BigDecimal interestScore;
    private LocalDateTime updateTime;
}
