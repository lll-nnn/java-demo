package com.lee.community.dao;

import com.lee.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User selectByName(String username);

}
