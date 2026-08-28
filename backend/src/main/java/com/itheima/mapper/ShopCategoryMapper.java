package com.itheima.mapper;

import com.itheima.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShopCategoryMapper {
    // 新增分类：补充parent_id字段
    @Insert("insert into shop_category (category_name,category_alias,parent_id,create_user,create_time,update_time)" +
            "values(#{categoryName},#{categoryAlias},#{parentId},#{createUser},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Category category);

    // 查询所有分类（保留原接口）
    @Select("select * from shop_category order by parent_id asc, id asc")
    List<Category> list();

    // 新增：查询所有一级分类（parent_id=0）
    @Select("select * from shop_category where parent_id = 0 order by id asc")
    List<Category> listParentCategories();

    // 新增：根据父ID查询二级分类
    @Select("select * from shop_category where parent_id = #{parentId} order by id asc")
    List<Category> listChildCategoriesByParentId(Integer parentId);

    // 根据id查询分类（保留）
    @Select("select * from shop_category where id = #{id}")
    Category findById(Integer id);

    // 更新分类：补充parent_id字段
    @Update("update shop_category set category_name = #{categoryName},category_alias = #{categoryAlias}," +
            "parent_id = #{parentId},update_time = #{updateTime} where id = #{id}")
    void update(Category category);

    // 删除分类：先删子分类，再删父分类（避免脏数据）
    @Delete("delete from shop_category where id = #{id}")
    void delete(Integer id);

    // 新增：查询分类下是否有子分类（删除父分类前校验）
    @Select("select count(*) from shop_category where parent_id = #{parentId}")
    int countChildByParentId(Integer parentId);

    // 查询分类名称：修正表名（原category改为shop_category）
    @Select("select category_name from shop_category where id = #{id}")
    String findCategoryNameById(Integer id);

    //通过子id查找父id
    @Select("select parent_id from shop_category where id = #{id}")
    Integer findParentIdByChildId(Integer id);

    /**
     * 根据分类ID查询其parent_id（判断是父/子分类）
     * @param categoryId 分类ID
     * @return parent_id（0=父分类，非0=子分类）
     */
    @Select("select parent_id from shop_category where id = #{categoryId}")
    Integer findParentIdById(Integer categoryId);

    /**
     * 根据父ID查询所有子分类ID列表
     * @param parentId 父分类ID
     * @return 子分类ID集合
     */
    @Select("select id from shop_category where parent_id = #{parentId}")
    List<Integer> listChildCategoryIdsByParentId(Integer parentId);
}