package com.scienjus.mapper;

import com.scienjus.domain.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
public interface UserMapper {

    @Select("select * from user")
    List<User> all();

    @Select("select * from user where id = #{id}")
    User get(int id);

    @Insert("insert into user(name, follower_count) values(#{name}, #{followerCount}")
    void insert(User user);
}
