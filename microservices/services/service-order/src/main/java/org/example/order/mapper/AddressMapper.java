package org.example.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.address.POJO.Address;

@Mapper
public interface AddressMapper {

    @Select("select * from address where id = #{id}")
    Address findById(Integer id);
}
