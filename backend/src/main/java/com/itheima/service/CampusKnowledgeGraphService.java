package com.itheima.service;

import com.itheima.pojo.CampusKnowledgeGraph;
import com.itheima.pojo.User;

import java.util.List;

/**
 * 校园知识图谱 Service 接口
 */
public interface CampusKnowledgeGraphService {

    /**
     * 查询所有知识图谱规则
     */
    List<CampusKnowledgeGraph> listAll();

    /**
     * 根据ID查询规则
     */
    CampusKnowledgeGraph getById(Integer id);

    /**
     * 新增规则
     */
    int add(CampusKnowledgeGraph campusKnowledgeGraph);

    /**
     * 修改规则
     */
    int update(CampusKnowledgeGraph campusKnowledgeGraph);

    /**
     * 删除规则
     */
    int delete(Integer id);

    /**
     * 同步规则到 Milvus 向量库
     */
    void syncKgToMilvus();

    /**
     * 生成用户复合向量（major+grade+scene+tags）
     */
    List<Double> generateUserCompositeVector(User user);

    /**
     * 根据用户信息匹配商品分类ID
     */
    List<Integer> matchCategoryByUser(User user);
}