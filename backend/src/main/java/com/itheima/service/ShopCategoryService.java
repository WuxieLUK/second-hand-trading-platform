package com.itheima.service;

import com.itheima.pojo.Category;

import java.util.List;
import java.util.Map;

// 先完善Service接口（原缺失，补充）
public interface ShopCategoryService {
    void add(Category category);

    List<Category> list();

    // 新增：查询一级分类
    List<Category> listParentCategories();

    // 新增：根据父ID查二级分类
    List<Category> listChildCategoriesByParentId(Integer parentId);

    Category findById(Integer id);

    void update(Category category);

    // 优化删除：先校验是否有子分类
    void delete(Integer id);

    List<Category> controllerList();

    String findCategoryNameById(Integer id);

    // 新增：树形结构查询（一级+二级分类组合）
    List<Map<String, Object>> listCategoryTree();

    Integer findParentIdByChildId(Integer childId);
}