package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import org.example.goods.POJO.Category;
import org.example.common.Result;
import com.itheima.service.ShopCategoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shopcategory")
@Slf4j
public class ShopCategoryController {

    @Resource
    private ShopCategoryService categoryService;

    // 添加商品分类（管理员）
    @PreAuthorize("/shopcategory/add")
    @PostMapping("/add")
    public Result add(@RequestBody @Validated(Category.Add.class) Category category) {
        try {
            categoryService.add(category);
            return Result.success("分类添加成功");
        } catch (Exception e) {
            log.error("添加分类失败", e);
            return Result.error("添加分类失败：" + e.getMessage());
        }
    }

    // 管理员查询所有分类（全量）
    @PreAuthorize("/shopcategory/admin/list")
    @GetMapping("/admin/list")
    public Result<List<Category>> adminList() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }

    // 根据id查询分类信息（管理员）
    @PreAuthorize("/shopcategory/detail")
    @GetMapping("/detail")
    public Result<Category> detail(@RequestParam Integer id) {
        if (id == null || id <= 0) {
            return Result.error("分类ID不能为空");
        }
        Category c = categoryService.findById(id);
        if (c == null) {
            return Result.error("分类不存在");
        }
        return Result.success(c);
    }

    // 修改商品分类信息（管理员）
    @PreAuthorize("/shopcategory/update")
    @PutMapping("/update")
    public Result update(@RequestBody @Validated(Category.Update.class) Category category) {
        try {
            categoryService.update(category);
            return Result.success("分类修改成功");
        } catch (Exception e) {
            log.error("修改分类失败", e);
            return Result.error("修改分类失败：" + e.getMessage());
        }
    }

    // 删除商品分类（管理员）
    @PreAuthorize("/shopcategory/delete")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        try {
            categoryService.delete(id);
            return Result.success("分类删除成功");
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return Result.error("删除分类失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Category>> openList() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }


    // 前端通用：查询所有一级分类（无权限限制）
    @GetMapping("/parent/list")
    public Result<List<Category>> parentList() {
        List<Category> parentList = categoryService.listParentCategories();
        return Result.success(parentList);
    }

    // 前端通用：根据父ID查询二级分类（无权限限制）
    @GetMapping("/child/list")
    public Result<List<Category>> childList(@RequestParam Integer parentId) {
        if (parentId == null || parentId <= 0) {
            return Result.error("父分类ID不能为空");
        }
        List<Category> childList = categoryService.listChildCategoriesByParentId(parentId);
        return Result.success(childList);
    }

    // 前端通用：查询树形分类结构（一级+二级，无权限限制）
    @GetMapping("/tree/list")
    public Result<List<Map<String, Object>>> treeList() {
        List<Map<String, Object>> treeList = categoryService.listCategoryTree();
        return Result.success(treeList);
    }
}