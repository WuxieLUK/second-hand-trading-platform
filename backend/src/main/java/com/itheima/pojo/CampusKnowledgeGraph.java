package com.itheima.pojo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园知识图谱规则实体类
 * 对应表：campus_knowledge_graph
 */
@Data
public class CampusKnowledgeGraph implements Serializable {
    // 规则ID（主键）
    private Integer id;
    // 场景类型：major(专业)/grade(年级)/scene(场景)/tag(标签)
    private String sceneType;
    // 场景值：如“计算机”“大四”“毕业季”“编程”
    private String sceneValue;
    // 关联分类ID，逗号分隔：如“2,5”
    private String categoryIds;
    // 权重（影响推荐优先级）
    private BigDecimal weight;
    // 创建时间
    private LocalDateTime createTime;
    // 修改时间
    private LocalDateTime updateTime;

    // 序列化版本号（避免反序列化异常）
    private static final long serialVersionUID = 1L;
}