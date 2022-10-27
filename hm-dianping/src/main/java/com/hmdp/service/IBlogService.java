package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 查询日志
     * @param blogId
     * @return
     */
    Result queryBlogById(String blogId);

    Result saveBlog(Blog blog);

    Result getHotBlog(Integer current);

    Result queryMyBlog(Integer current);

    Result likeBlog(Long id);

    Result getBlogLikeCount(Long id);

    Result getBlogPage(Long userId, Integer currentPage);

    Result getFollowBlog(Long lastId, Integer offset);
}
