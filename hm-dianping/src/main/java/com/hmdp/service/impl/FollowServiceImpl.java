package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.ConcreteUser;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @Resource(name = "followMapper")
    private FollowMapper followMapper;
    @Resource
    private IUserService userServiceImpl;
    /**
     * 关注博客作者,存储在数据库中并在缓存中存储
     * @param authId
     * @param action
     * @return
     */
    @Override
    public Result follow(String authId, Boolean action) {
        Integer auth = userServiceImpl.query().eq("id", authId).count();
        if(auth != 1){
            return Result.fail("用户不存在");
        }
        Long userId = ConcreteUser.get().getId();
        String followKey = RedisConstants.SET_FOLLOW_KEY + userId;
        if(action){
            Follow follow = new Follow();
            follow.setFollowUserId(Long.valueOf(authId));
            follow.setUserId(userId);
            boolean isSuccess = save(follow);
            if(isSuccess){
                redisTemplate.opsForSet().add(followKey , authId);
                return Result.ok("关注成功");
            }else {
                return Result.fail("关注失败");
            }
        }
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                        .eq(Follow::getFollowUserId, authId);
        int count = followMapper.delete(wrapper);
        if(count == 0){
            return Result.fail("取消关注失败");
        }
        redisTemplate.opsForSet().remove(followKey, authId);
        return Result.ok("取消关注成功");
    }

    @Override
    public Result isFollow(String followUserId) {
        Long userId = ConcreteUser.get().getId();
        String followKey = RedisConstants.SET_FOLLOW_KEY + userId;
        Boolean member = redisTemplate.opsForSet().isMember(followKey, followUserId);
        return Result.ok(Boolean.TRUE.equals(member));
    }

    @Override
    public Result intersectFollow(String targetId) {
        String userId = String.valueOf(ConcreteUser.get().getId());
        Set<String> intersectFollow = redisTemplate.opsForSet().intersect(RedisConstants.SET_FOLLOW_KEY + userId, RedisConstants.SET_FOLLOW_KEY + targetId);
        return Result.ok(intersectFollow);
    }
}
