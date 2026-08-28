package com.itheima.mapper;

import com.itheima.pojo.CampusKnowledgeGraph;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 校园知识图谱 Mapper 层
 * 负责数据库交互
 */
@Mapper
@Repository
public interface CampusKnowledgeGraphMapper {

    /**
     * 查询所有知识图谱规则
     */
    @Select("SELECT id, scene_type, scene_value, category_ids, weight, create_time, update_time " +
            "FROM campus_knowledge_graph ORDER BY weight DESC")
    List<CampusKnowledgeGraph> listAll();

    /**
     * 根据ID查询规则
     */
    @Select("SELECT id, scene_type, scene_value, category_ids, weight, create_time, update_time " +
            "FROM campus_knowledge_graph WHERE id = #{id}")
    CampusKnowledgeGraph getById(Integer id);

    /**
     * 根据场景类型+场景值查询规则
     */
    @Select("SELECT id, scene_type, scene_value, category_ids, weight, create_time, update_time " +
            "FROM campus_knowledge_graph WHERE scene_type = #{sceneType} AND scene_value = #{sceneValue}")
    CampusKnowledgeGraph getByScene(String sceneType, String sceneValue);

    /**
     * 新增规则
     */
    @Insert("INSERT INTO campus_knowledge_graph (scene_type, scene_value, category_ids, weight, create_time, update_time) " +
            "VALUES (#{sceneType}, #{sceneValue}, #{categoryIds}, #{weight}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 回填主键ID
    int add(CampusKnowledgeGraph campusKnowledgeGraph);

    /**
     * 修改规则
     */
    @Update("UPDATE campus_knowledge_graph SET scene_type = #{sceneType}, scene_value = #{sceneValue}, " +
            "category_ids = #{categoryIds}, weight = #{weight}, update_time = NOW() WHERE id = #{id}")
    int update(CampusKnowledgeGraph campusKnowledgeGraph);

    /**
     * 删除规则（物理删除）
     */
    @Delete("DELETE FROM campus_knowledge_graph WHERE id = #{id}")
    int delete(Integer id);
}