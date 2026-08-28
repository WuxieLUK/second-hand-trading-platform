package com.itheima.mapper;

import com.itheima.DTO.GoodsQueryDTO;
import com.itheima.pojo.Goods;
import com.itheima.pojo.GoodsImage;
import com.itheima.pojo.PageBean;
import com.itheima.vo.BuyerViewSellerVO;
import com.itheima.vo.GoodsVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品Mapper接口
 */
public interface GoodsMapper {

    // 1. 新增：批量插入商品图片
    void insertGoodsImages(@Param("goodsId") Integer goodsId, @Param("imageList") List<GoodsImage> imageList);

    // 2. 新增：根据商品ID删除所有图片
    @Delete("DELETE FROM goods_image WHERE goods_id = #{goodsId}")
    void deleteGoodsImagesByGoodsId(Integer goodsId);

    // 3. 新增：根据商品ID查询图片列表
    @Select("SELECT * FROM goods_image WHERE goods_id = #{goodsId} ORDER BY id ASC")
    List<GoodsImage> findGoodsImagesByGoodsId(Integer goodsId);

    // 新增商品
    @Insert("INSERT INTO goods (goods_name, goods_desc, goods_pic, category_id, original_price, sell_price, seller_id, goods_status, is_new,stock, create_time, update_time) " +
            "VALUES (#{goodsName}, #{goodsDesc}, #{goodsPic}, #{categoryId}, #{originalPrice}, #{sellPrice}, #{sellerId}, #{goodsStatus}, #{isNew}, #{stock}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Goods goods);

    /**
     * 分页查询商品（支持分类筛选）
     * @param queryDTO 查询条件
     * @param categoryIds 目标分类ID列表（子ID）
     * @param limit 数量限制
     * @return 商品列表
     */
    List<GoodsVO> list(
            @Param("queryDTO") GoodsQueryDTO queryDTO,
            @Param("categoryIds") List<Integer> categoryIds,
            @Param("limit") Integer limit
    );


    // 根据ID查询商品
    @Select("SELECT * FROM goods WHERE id = #{id}")
    Goods findById(Integer id);

    // 修改商品
    void update(Goods goods);

    // 删除商品
    @Delete("DELETE FROM goods WHERE id = #{id}")
    void delete(Integer id);

    // 更新商品状态
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);


    BuyerViewSellerVO findSellerByUserId(Integer id);

    /**
     * 查询数据库中所有未删除的商品ID
     */
    @Select("SELECT id FROM goods")
    List<Long> listAllValidGoodsIds();

    /**
     * 根据商品ID列表查询商品列表
     * @param goodsIdList 商品ID列表
     * @return 商品列表
     */
    List<GoodsVO> listByIds(List<Integer> goodsIdList);

    /**
     * 根据分类ID列表查询商品列表
     * @param categoryIds 分类ID列表
     * @return 商品列表
     */
    List<GoodsVO> listByCategoryIds(List<Integer> categoryIds, Integer limit);

    /**
     * 查询所有商品
     * @param queryDTO 查询条件
     * @return 商品列表
     */
    List<GoodsVO> allList(GoodsQueryDTO queryDTO);


    /**
     * 批量查询多个商品的图片
     * @param goodsIds 商品ID列表
     * @return 图片列表
     */
    List<GoodsImage> findGoodsImagesByGoodsIds(@Param("goodsIds") List<Integer> goodsIds);

    // GoodsMapper中新增
    List<Goods> findAllOnSale(); // 查询所有在售商品，用于初始化库存缓存

    @Select("SELECT goods_name, is_new, sell_price, original_price, stock, goods_status FROM goods WHERE goods_status = 1")
    List<Goods> listOnSaleGoods();
}