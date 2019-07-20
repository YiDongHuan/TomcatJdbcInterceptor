package com.huan.test.logback.repo.mapper;

import com.huan.test.logback.repo.entity.Customer;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

@Mapper
public interface CustomerMapper {
    @Delete({
        "delete from customer",
        "where id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "<script>",
        "insert into customer",
        "<trim prefix='(' suffix=')' suffixOverrides=','>",
        "<if test='name != null'>name,</if>",
        "<if test='money != null'>money,</if>",
        "</trim>",
        "<trim prefix='values (' suffix=')' suffixOverrides=','>",
        "<if test='name != null'>#{name,jdbcType=VARCHAR},</if>",
        "<if test='money != null'>#{money,jdbcType=DOUBLE},</if>",
        "</trim>",
        "</script>"
    })
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="id", before=false, resultType=Integer.class)
    int insert(Customer record);

    @Select({
        "select",
        "id, name, money",
        "from customer",
        "where id = #{id,jdbcType=INTEGER}"
    })
    @ResultMap("all_column")
    Customer selectByPrimaryKey(Integer id);

    @Select({
        "select",
        "id, name, money",
        "from customer"
    })
    @Results(id = "all_column", value ={
        @Result(column="id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="money", property="money", jdbcType=JdbcType.DOUBLE)
    })
    List<Customer> selectAll();

    @Update({
        "update customer",
        "set name = #{name,jdbcType=VARCHAR},",
          "money = #{money,jdbcType=DOUBLE}",
        "where id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(Customer record);
}