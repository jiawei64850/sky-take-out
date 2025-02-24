package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {
    @Select("select * from user where openid = #{openid}")
    User selectByOpenId(String openid);
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into user (openid, name, create_time) values (#{openid}, #{name}, #{createTime})")
    void insert(User user);

    /**
     * 通过userId查询用户
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User selectById(Long id);
}
