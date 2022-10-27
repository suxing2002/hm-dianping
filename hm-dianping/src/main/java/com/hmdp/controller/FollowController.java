package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource(name = "followServiceImpl")
    private IFollowService followService;
    @GetMapping("/or/not/{userId}")
    public Result isFollow(@PathVariable("userId") String userId){
        return followService.isFollow(userId);
    }
    @PutMapping("/{userId}/{follow}")
    public Result follow(@PathVariable(name = "follow") String follow, @PathVariable(name = "userId") String authId){
        boolean action = "true".equalsIgnoreCase(follow) ? true : false;
        return followService.follow(authId , action);
    }
    /**
     * 请求 URL: http://localhost:8080/api/follow/common/2
     * 请求方法: GET
     */
    @GetMapping("/common/{targetId}")
    public Result getIntersectFollow(@PathVariable String targetId){
        return followService.intersectFollow(targetId);
    }
}
