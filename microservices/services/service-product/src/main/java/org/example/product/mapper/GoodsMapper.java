package org.example.product.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.POJO.Goods;
import org.example.goods.POJO.GoodsImage;
import org.example.goods.VO.GoodsVO;
import org.example.user.VO.BuyerViewSellerVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GoodsMapper {

    void insertGoodsImages(@Param("goodsId") Integer goodsId, @Param("imageList") List<GoodsImage> imageList);

    @Delete("delete from goods_image where goods_id = #{goodsId}")
    void deleteGoodsImagesByGoodsId(Integer goodsId);

    @Select("select * from goods_image where goods_id = #{goodsId} order by id asc")
    List<GoodsImage> findGoodsImagesByGoodsId(Integer goodsId);

    @Insert("insert into goods (goods_name, goods_desc, goods_pic, category_id, original_price, sell_price, seller_id, goods_status, is_new, stock, create_time, update_time) values (#{goodsName}, #{goodsDesc}, #{goodsPic}, #{categoryId}, #{originalPrice}, #{sellPrice}, #{sellerId}, #{goodsStatus}, #{isNew}, #{stock}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Goods goods);

    List<GoodsVO> list(@Param("queryDTO") GoodsQueryDTO queryDTO, @Param("categoryIds") List<Integer> categoryIds, @Param("limit") Integer limit);

    @Select("select * from goods where id = #{id}")
    Goods findById(Integer id);

    void update(Goods goods);

    @Delete("delete from goods where id = #{id}")
    void delete(Integer id);

    void updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    BuyerViewSellerVO findSellerByUserId(Integer id);

    List<GoodsVO> allList(GoodsQueryDTO queryDTO);

    List<GoodsVO> searchByKeyword(GoodsQueryDTO queryDTO);

    List<GoodsVO> listByIds(@Param("ids") List<Integer> ids);

    @Select("select * from goods where goods_status = 1 order by create_time desc")
    List<Goods> listOnSaleGoods();

    @Select("select * from goods where goods_status = 1")
    List<Goods> findAllOnSale();

    /** 查询所有在售商品（含卖家信息），用于热门排序 */
    List<GoodsVO> findAllOnSaleWithSeller();

    /** 按分类查询热门商品（按浏览次数降序） */
    List<GoodsVO> findHotByCategoryIds(@Param("categoryIds") List<Integer> categoryIds, @Param("limit") Integer limit);

    /** 获取商品浏览次数 */
    @Select("select view_count from goods where id = #{id}")
    Integer getViewCount(Integer id);

    /** 增加商品浏览次数 */
    @Update("update goods set view_count = view_count + 1 where id = #{id}")
    void incrementViewCount(Integer id);
}
