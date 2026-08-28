package com.itheima.service.impl;

import com.itheima.mapper.CampusKnowledgeGraphMapper;
import com.itheima.pojo.CampusKnowledgeGraph;
import com.itheima.pojo.User;
import com.itheima.service.CampusKnowledgeGraphService;
import com.itheima.util.MilvusCollectionUtil;
import com.itheima.util.MilvusVectorUtil;
import com.itheima.util.QwenEmbeddingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 校园知识图谱服务实现类（完整实现）
 * 复用现有Milvus工具+融合用户标签，补全所有接口方法
 * 新增异步执行Milvus同步操作，解决接口超时问题
 */
@Slf4j
@Service
public class CampusKnowledgeGraphServiceImpl implements CampusKnowledgeGraphService {

    @Autowired
    private CampusKnowledgeGraphMapper campusKnowledgeGraphMapper;
    @Autowired
    private MilvusCollectionUtil milvusCollectionUtil;
    @Autowired
    private MilvusVectorUtil milvusVectorUtil;
    @Autowired
    private QwenEmbeddingUtil qwenEmbeddingUtil;

    // 创建线程池处理异步任务（推荐使用线程池而非直接new Thread）
    private final ExecutorService milvusExecutor = Executors.newFixedThreadPool(5);

    /**
     * 查询所有知识图谱规则
     * @return 规则列表
     */
    @Override
    public List<CampusKnowledgeGraph> listAll() {
        try {
            List<CampusKnowledgeGraph> kgList = campusKnowledgeGraphMapper.listAll();
            log.info("查询到校园知识图谱规则总数：{}", kgList.size());
            return kgList;
        } catch (Exception e) {
            log.error("查询所有知识图谱规则失败", e);
            throw new RuntimeException("查询知识图谱规则失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询规则
     * @param id 规则ID
     * @return 规则详情
     */
    @Override
    public CampusKnowledgeGraph getById(Integer id) {
        // 参数校验
        if (id == null || id <= 0) {
            log.error("根据ID查询规则失败：ID不能为空且必须大于0");
            throw new IllegalArgumentException("规则ID不能为空且必须大于0");
        }

        try {
            CampusKnowledgeGraph kg = campusKnowledgeGraphMapper.getById(id);
            if (kg == null) {
                log.warn("未查询到ID为{}的知识图谱规则", id);
                return null;
            }
            log.info("成功查询到ID为{}的知识图谱规则：{}", id, kg);
            return kg;
        } catch (Exception e) {
            log.error("根据ID:{}查询知识图谱规则失败", id, e);
            throw new RuntimeException("查询规则失败：" + e.getMessage());
        }
    }

    /**
     * 新增知识图谱规则
     * @param campusKnowledgeGraph 规则信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(CampusKnowledgeGraph campusKnowledgeGraph) {
        // 参数校验
        validateKgParam(campusKnowledgeGraph);
        int rows = campusKnowledgeGraphMapper.add(campusKnowledgeGraph);

        // 数据库新增成功后，异步同步Milvus（失败仅日志，不回滚数据库）
        if (rows > 0) {
            // ========== 关键修改1：异步执行Milvus同步 ==========
            milvusExecutor.submit(() -> {
                try {
                    syncSingleKgToMilvus(campusKnowledgeGraph);
                    log.info("新增知识图谱规则成功，ID：{}，场景类型：{}，场景值：{}，Milvus同步完成",
                            campusKnowledgeGraph.getId(), campusKnowledgeGraph.getSceneType(), campusKnowledgeGraph.getSceneValue());
                } catch (Exception e) {
                    log.error("新增规则ID:{} 同步Milvus失败（数据库已成功），后续可手动调用/sync接口同步",
                            campusKnowledgeGraph.getId(), e);
                    // 不抛异常，避免影响主线程
                }
            });

            // 主线程仅记录日志，不等待Milvus同步完成
            log.info("新增知识图谱规则成功，ID：{}，场景类型：{}，场景值：{}，已提交Milvus异步同步任务",
                    campusKnowledgeGraph.getId(), campusKnowledgeGraph.getSceneType(), campusKnowledgeGraph.getSceneValue());
        }
        return rows;
    }


    /**
     * 修改知识图谱规则
     * @param campusKnowledgeGraph 规则信息
     * @return 影响行数
     */
    @Override
    public int update(CampusKnowledgeGraph campusKnowledgeGraph) {
        // 1. 基础参数校验
        if (campusKnowledgeGraph.getId() == null || campusKnowledgeGraph.getId() <= 0) {
            log.error("修改规则失败：ID不能为空且必须大于0");
            throw new IllegalArgumentException("规则ID不能为空且必须大于0");
        }

        // 2. 业务参数校验
        validateKgParam(campusKnowledgeGraph);

        try {
            // 3. 检查规则是否存在
            CampusKnowledgeGraph existKg = getById(campusKnowledgeGraph.getId());
            if (existKg == null) {
                log.error("修改规则失败：未找到ID为{}的规则", campusKnowledgeGraph.getId());
                throw new RuntimeException("未找到ID为" + campusKnowledgeGraph.getId() + "的规则");
            }

            // 4. 更新数据库
            int rows = campusKnowledgeGraphMapper.update(campusKnowledgeGraph);
            if (rows > 0) {
                log.info("修改知识图谱规则成功，ID：{}，更新后信息：{}",
                        campusKnowledgeGraph.getId(), campusKnowledgeGraph);

                // ========== 关键修改2：异步执行Milvus同步 ==========
                milvusExecutor.submit(() -> {
                    try {
                        syncSingleKgToMilvus(campusKnowledgeGraph);
                        log.info("修改规则ID:{} Milvus同步完成", campusKnowledgeGraph.getId());
                    } catch (Exception e) {
                        log.error("修改规则ID:{} 同步Milvus失败", campusKnowledgeGraph.getId(), e);
                    }
                });
            } else {
                log.warn("修改知识图谱规则失败，数据库影响行数为0");
            }
            return rows;
        } catch (IllegalArgumentException e) {
            log.error("修改知识图谱规则参数校验失败：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("修改知识图谱规则失败", e);
            throw new RuntimeException("修改规则失败：" + e.getMessage());
        }
    }

    /**
     * 删除知识图谱规则
     * @param id 规则ID
     * @return 影响行数
     */
    @Override
    public int delete(Integer id) {
        // 1. 参数校验
        if (id == null || id <= 0) {
            log.error("删除规则失败：ID不能为空且必须大于0");
            throw new IllegalArgumentException("规则ID不能为空且必须大于0");
        }

        try {
            // 2. 检查规则是否存在
            CampusKnowledgeGraph existKg = getById(id);
            if (existKg == null) {
                log.error("删除规则失败：未找到ID为{}的规则", id);
                throw new RuntimeException("未找到ID为" + id + "的规则");
            }

            // 3. 删除数据库记录
            int rows = campusKnowledgeGraphMapper.delete(id);
            if (rows > 0) {
                log.info("删除知识图谱规则成功，ID：{}", id);

                // ========== 可选：异步删除Milvus中的数据 ==========
                milvusExecutor.submit(() -> {
                    try {
                        // 如需实现Milvus删除，可在此处添加逻辑
                        log.info("已提交删除规则ID:{} 的Milvus清理任务（暂未实现具体逻辑）", id);
                    } catch (Exception e) {
                        log.error("删除规则ID:{} Milvus清理失败", id, e);
                    }
                });

                // 注：Milvus删除可根据实际需求实现，此处仅提交异步任务
            } else {
                log.warn("删除知识图谱规则失败，数据库影响行数为0");
            }
            return rows;
        } catch (Exception e) {
            log.error("删除ID:{}的知识图谱规则失败", id, e);
            throw new RuntimeException("删除规则失败：" + e.getMessage());
        }
    }

    /**
     * 初始化：创建知识图谱集合并同步所有规则到Milvus
     */
    @Override
    public void syncKgToMilvus() {
        // ========== 关键修改3：异步执行批量同步 ==========
        milvusExecutor.submit(() -> {
            try {
                // 1. 初始化 Milvus 知识图谱集合
                milvusCollectionUtil.initKgCollection();

                // 2. 查询所有规则
                List<CampusKnowledgeGraph> kgList = campusKnowledgeGraphMapper.listAll();
                if (CollectionUtils.isEmpty(kgList)) {
                    log.warn("【知识图谱同步】无规则数据，跳过同步");
                    return;
                }

                // 3. 批量同步到 Milvus
                int successCount = 0;
                for (CampusKnowledgeGraph kg : kgList) {
                    try {
                        syncSingleKgToMilvus(kg);
                        successCount++;
                    } catch (Exception e) {
                        log.error("【知识图谱同步】规则ID:{} 同步失败", kg.getId(), e);
                    }
                }
                log.info("【知识图谱同步】完成，总数：{}，成功：{}，失败：{}",
                        kgList.size(), successCount, kgList.size() - successCount);
            } catch (Exception e) {
                log.error("【知识图谱批量同步】整体执行失败", e);
            }
        });

        // 立即返回，不等待批量同步完成
        log.info("【知识图谱批量同步】任务已提交到后台执行，可查看日志了解进度");
    }

    /**
     * 生成用户复合向量（融合major+grade+scene+tags）
     * @param user 用户信息
     * @return 1024维用户复合向量
     */
    @Override
    public List<Double> generateUserCompositeVector(User user) {
        // 参数校验
        if (user == null) {
            log.error("生成用户复合向量失败：用户信息不能为空");
            throw new IllegalArgumentException("用户信息不能为空");
        }

        try {
            // 1. 拼接用户特征文本（处理空值）
            StringBuilder userText = new StringBuilder();
            userText.append("专业:").append(StringUtils.hasText(user.getMajor()) ? user.getMajor() : "未知").append(",");
            userText.append("年级:").append(StringUtils.hasText(user.getGrade()) ? user.getGrade() : "未知").append(",");
            userText.append("场景:").append(StringUtils.hasText(user.getCampusScene()) ? user.getCampusScene() : "未知").append(",");
            userText.append("标签:").append(StringUtils.hasText(user.getTags()) ? user.getTags() : "无");

            log.info("生成用户复合向量的特征文本：{}", userText);

            // 2. 调用Embedding工具生成1024维向量
            List<Double> embedding = qwenEmbeddingUtil.getEmbedding(userText.toString());

            // 3. 校验向量维度
            if (embedding.size() != MilvusCollectionUtil.VECTOR_DIMENSION) {
                log.error("用户复合向量维度异常 | 期望：{}维 | 实际：{}维",
                        MilvusCollectionUtil.VECTOR_DIMENSION, embedding.size());
                throw new RuntimeException("向量维度异常，期望1024维，实际" + embedding.size() + "维");
            }

            log.info("成功生成用户ID:{}的1024维复合向量", user.getId());
            return embedding;
        } catch (Exception e) {
            log.error("生成用户复合向量失败", e);
            throw new RuntimeException("生成用户向量失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户信息匹配商品分类（融合知识图谱+用户标签）
     * @param user 用户信息
     * @return 匹配的分类ID列表（去重）
     */
    @Override
    public List<Integer> matchCategoryByUser(User user) {
        try {
            // 1. 生成用户复合向量
            List<Double> userVector = generateUserCompositeVector(user);

            // 2. 调用Milvus检索匹配的分类ID（取前5条）
            List<Integer> categoryIds = milvusVectorUtil.searchKgCategory(userVector, 5);

            // 3. 去重并打印结果
            List<Integer> distinctCategoryIds = categoryIds.stream().distinct().collect(Collectors.toList());
            log.info("用户ID:{}匹配到的商品分类ID：{}", user.getId(), distinctCategoryIds);

            return distinctCategoryIds;
        } catch (Exception e) {
            log.error("根据用户信息匹配商品分类失败", e);
            throw new RuntimeException("匹配商品分类失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验知识图谱规则参数
     */
    private void validateKgParam(CampusKnowledgeGraph campusKnowledgeGraph) {
        if (campusKnowledgeGraph == null) {
            throw new IllegalArgumentException("规则信息不能为空");
        }
        if (!StringUtils.hasText(campusKnowledgeGraph.getSceneType())) {
            throw new IllegalArgumentException("场景类型不能为空");
        }
        if (!StringUtils.hasText(campusKnowledgeGraph.getSceneValue())) {
            throw new IllegalArgumentException("场景值不能为空");
        }
        if (!StringUtils.hasText(campusKnowledgeGraph.getCategoryIds())) {
            throw new IllegalArgumentException("关联分类ID不能为空");
        }
        if (campusKnowledgeGraph.getWeight() == null) {
            campusKnowledgeGraph.setWeight(BigDecimal.valueOf(1.0)); // 默认权重1.0
        }
    }

    /**
     * 同步单条规则到 Milvus
     */
    private void syncSingleKgToMilvus(CampusKnowledgeGraph kg) {
        // 构建规则文本
        String kgText = String.format("%s:%s,分类:%s,权重:%s",
                kg.getSceneType(), kg.getSceneValue(), kg.getCategoryIds(), kg.getWeight());
        // 生成 1024 维向量
        List<Double> embedding = null;
        try {
            embedding = qwenEmbeddingUtil.getEmbedding(kgText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 校验向量维度
        if (embedding.size() != MilvusCollectionUtil.VECTOR_DIMENSION) {
            throw new RuntimeException("向量维度异常，期望：" + MilvusCollectionUtil.VECTOR_DIMENSION +
                    "，实际：" + embedding.size());
        }
        // 插入 Milvus
        milvusVectorUtil.insertKgVector(
                Long.valueOf(kg.getId()),
                kg.getSceneType(),
                kg.getSceneValue(),
                kg.getCategoryIds(),
                kg.getWeight().doubleValue(),
                embedding
        );
    }
}