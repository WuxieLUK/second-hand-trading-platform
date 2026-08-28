package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.pojo.CampusKnowledgeGraph;
import com.itheima.pojo.Result;
import com.itheima.service.CampusKnowledgeGraphService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 校园知识图谱 Controller 层
 * 提供规则的增删改查、同步 Milvus 等接口
 */
@Slf4j
@RestController
@RequestMapping("/kg")
public class CampusKnowledgeGraphController {

    @Resource
    private CampusKnowledgeGraphService campusKnowledgeGraphService;

    /**
     * 查询所有规则
     */
    @PreAuthorize("/kg/list")
    @GetMapping("/list")
    public Result<List<CampusKnowledgeGraph>> listAll() {
        try {
            List<CampusKnowledgeGraph> list = campusKnowledgeGraphService.listAll();
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询知识图谱规则失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询规则
     */
    @PreAuthorize("/kg/select/{id}")
    @GetMapping("/select/{id}")
    public Result<CampusKnowledgeGraph> getById(@PathVariable Integer id) {
        try {
            CampusKnowledgeGraph kg = campusKnowledgeGraphService.getById(id);
            return Result.success(kg);
        } catch (Exception e) {
            log.error("查询规则ID:{} 失败", id, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增规则
     */
    @PreAuthorize("/kg/add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody CampusKnowledgeGraph campusKnowledgeGraph) {
        try {
            int rows = campusKnowledgeGraphService.add(campusKnowledgeGraph);
            return Result.success("新增规则成功");
        } catch (Exception e) {
            log.error("新增知识图谱规则失败", e);
            return Result.error("新增失败：" + e.getMessage());
        }
    }

    /**
     * 修改规则
     */
    @PreAuthorize("/kg/update")
    @PutMapping("/update")
    public Result<String> update(@RequestBody CampusKnowledgeGraph campusKnowledgeGraph) {
        try {
            int rows = campusKnowledgeGraphService.update(campusKnowledgeGraph);
            return Result.success("修改规则成功");
        } catch (Exception e) {
            log.error("修改知识图谱规则失败", e);
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除规则
     */
    @PreAuthorize("/kg/delete/{id}")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        try {
            int rows = campusKnowledgeGraphService.delete(id);
            return Result.success("删除规则成功");
        } catch (Exception e) {
            log.error("删除规则ID:{} 失败", id, e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 同步所有规则到 Milvus
     */
    @PreAuthorize("/kg/sync")
    @PostMapping("/sync")
    public Result<String> syncKgToMilvus() {
        try {
            campusKnowledgeGraphService.syncKgToMilvus();
            return Result.success("同步成功");
        } catch (Exception e) {
            log.error("同步知识图谱到Milvus失败", e);
            return Result.error("同步失败：" + e.getMessage());
        }
    }
}