package com.lee.service;

import com.lee.dto.Result;
import com.lee.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlog(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result queryBlogByUserId(Integer current, Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
