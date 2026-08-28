package com.itheima.service.impl;

import com.itheima.mapper.ShopCategoryMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Category;
import com.itheima.service.ShopCategoryService;
import com.itheima.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShopCategoryServiceImpl implements ShopCategoryService {

    @Autowired
    private ShopCategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Category category) {
        // 补充属性：parent_id默认0（一级分类）
        if (category.getParentId() == null) {
            category.setParentId(0);
        }
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        // 获取当前登录用户（创建人）
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        String username = userMapper.findUsernameById(userId);
        category.setCreateUser(username);

        categoryMapper.add(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> list() {
        return categoryMapper.list();
    }

    // 新增：查询所有一级分类
    @Override
    @Transactional(readOnly = true)
    public List<Category> listParentCategories() {
        return categoryMapper.listParentCategories();
    }

    // 新增：根据父ID查二级分类
    @Override
    @Transactional(readOnly = true)
    public List<Category> listChildCategoriesByParentId(Integer parentId) {
        if (parentId == null || parentId <= 0) {
            return new ArrayList<>();
        }
        return categoryMapper.listChildCategoriesByParentId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return categoryMapper.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Category category) {
        if (category.getId() == null || category.getId() <= 0) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        // 校验：如果是父分类，先删除所有子分类
        int childCount = categoryMapper.countChildByParentId(id);
        if (childCount > 0) {
            // 方式1：级联删除子分类（推荐）
            List<Category> childCategories = categoryMapper.listChildCategoriesByParentId(id);
            for (Category child : childCategories) {
                categoryMapper.delete(child.getId());
            }
        }
        // 删除当前分类
        categoryMapper.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> controllerList() {
        return categoryMapper.list();
    }

    @Override
    @Transactional(readOnly = true)
    public String findCategoryNameById(Integer id) {
        if (id == null || id <= 0) {
            return "";
        }
        return categoryMapper.findCategoryNameById(id);
    }

    // 新增：树形结构查询（前端下拉菜单专用）
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategoryTree() {
        List<Map<String, Object>> treeList = new ArrayList<>();
        // 1. 查询所有一级分类
        List<Category> parentList = categoryMapper.listParentCategories();
        for (Category parent : parentList) {
            Map<String, Object> parentMap = new HashMap<>();
            parentMap.put("id", parent.getId());
            parentMap.put("categoryName", parent.getCategoryName());
            parentMap.put("categoryAlias", parent.getCategoryAlias());
            // 2. 查询当前一级分类的二级分类
            List<Category> childList = categoryMapper.listChildCategoriesByParentId(parent.getId());
            parentMap.put("children", childList);
            treeList.add(parentMap);
        }
        return treeList;
    }

    @Override
    public Integer findParentIdByChildId(Integer childId) {
        return categoryMapper.findParentIdByChildId(childId);
    }
}