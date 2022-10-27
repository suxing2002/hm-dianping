package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.ConcreteUser;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
       return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
       return blogService.likeBlog(id);
    }
    @GetMapping("/likes/{id}")
    public Result getLikeBlog(@PathVariable("id") Long id) {
       return blogService.getBlogLikeCount(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
       return blogService.queryMyBlog(current);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.getHotBlog(current);
    }
    @GetMapping("/{blogId}")
    public Result getBlog(@PathVariable(name = "blogId") String blogId){
        return blogService.queryBlogById(blogId);
    }
    @GetMapping("/of/user")
    public Result getBolg(@RequestParam(name = "id") Long userId ,@RequestParam(name = "current") Integer currentPage){
        return blogService.getBlogPage(userId , currentPage);
    }
    /**
     * 请求 URL: http://localhost:8080/api/blog/of/follow?&lastId=1666779943015
     * 请求方法: GET
     */
    @GetMapping("/of/follow")
    public Result getFollowBlog(@RequestParam("lastId") Long lastId ,
                                @RequestParam(name = "offset" , defaultValue = "0") Integer offset){
        return blogService.getFollowBlog(lastId , offset);
    }
}
