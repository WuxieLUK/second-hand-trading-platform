package com.itheima.mapper;

import com.itheima.DTO.AddressQueryDTO;
import com.itheima.pojo.Address;
import com.itheima.vo.AddressVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressMapper {
    // 新增地址
    @Insert("INSERT INTO address(address_name, province, city, district, detail_addr, zip_code, is_default, create_time, update_time, user_id) " +
            "VALUES(#{addressName}, #{province}, #{city}, #{district}, #{detailAddr}, #{zipCode}, #{isDefault}, #{createTime}, #{updateTime}, #{userId})")
    void add(Address address);

    // 分页查询地址列表
    List<AddressVO> list(AddressQueryDTO queryDTO);

    // 根据ID查询地址
    @Select("SELECT * FROM address WHERE id = #{id}")
    Address findById(Integer id);

    // 修改地址
    @Update("UPDATE address SET address_name=#{addressName}, province=#{province}, city=#{city}, district=#{district}, " +
            "detail_addr=#{detailAddr}, zip_code=#{zipCode}, is_default=#{isDefault}, update_time=#{updateTime} " +
            "WHERE id = #{id}")
    void update(Address address);

    // 删除地址
    @Delete("DELETE FROM address WHERE id = #{id}")
    void delete(Integer id);

    // 根据用户ID查询默认地址
    @Select("SELECT * FROM address WHERE user_id = #{userId} AND is_default = 1")
    Address findDefaultAddressByUserId(Integer userId);
}