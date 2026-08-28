package org.example.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.goods.POJO.Category;

import java.util.List;

@Mapper
public interface ShopCategoryMapper {

    @Select("select * from shop_category order by parent_id asc, id asc")
    List<Category> list();

    @Select("select * from shop_category where id = #{id}")
    Category findById(Integer id);

    @Select("select id from shop_category where parent_id = #{parentId}")
    List<Integer> listChildCategoryIdsByParentId(Integer parentId);

    @Select("select id from shop_category where category_name like concat('%', #{keyword}, '%')")
    List<Integer> findCategoryIdsByNameLike(String keyword);
}
