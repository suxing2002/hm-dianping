package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.ConcreteUser;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisUtils;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_BLOG_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource(name = "userServiceImpl")
    private IUserService userService;
    @Resource
    private RedisUtils redisUtils;
    @Resource(name = "followServiceImpl")
    private IFollowService followService;
    @Override
    public Result queryBlogById(String blogId) {
//        Blog blog = redisUtils.queryCacheForBean(blogId, CACHE_BLOG_KEY, Blog.class);
//        if (blog == null) {
//            blog = redisUtils.createCache(id -> query().eq("id", id).one(),
//                    blogId, CACHE_BLOG_KEY, Duration.ofMinutes(RedisConstants.CACHE_BLOG_TTL));
//        }
        Blog blog = query().eq("id", blogId).one();
        Double score = null;
        if(ConcreteUser.get() != null){
            score = stringRedisTemplate.opsForZSet().score(RedisConstants.BLOG_LIKED_KEY + blogId, String.valueOf(ConcreteUser.get().getId()));
        }
        blog.setIsLike(score != null);
        return Result.ok(blog);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        Long userId = ConcreteUser.get().getId();
        blog.setUserId(userId);
        // 保存探店博文//同时将博客的id推送给关注博主的人
        //feed流
        //拉模式:读推送 用户读取时拉取博主博主写信箱内容 读取频繁,并且实现复杂(粉丝能够看到所有的关注者信息,所以对于一个用户,
        // 需要维护他关注人数量的读取索引) 实现复杂
        //推模式:写推送 博主写博客时将博客信息发送至粉丝信箱 写频繁,如果粉丝数量大(粉丝基数大,僵尸粉多,推送没有意义),但是实现简单
        //推-拉结合:两者结合,具体情况具体分析 实现复杂
        //使用推模式
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("博客发布失败");
        }
        String blogKey = RedisConstants.ZSET_BLOG_LIKE + blog.getId();
        //建立点赞用户队列
        stringRedisTemplate.opsForZSet().add(blogKey , RedisConstants.BLOG_LIKE_DEFAULT , Long.MAX_VALUE);
        //获取粉丝id
        List<Follow> follows = followService.query().select("user_id").eq("follow_user_id", userId).list();
        if(!follows.isEmpty()){
            //将博客id推送至粉丝收件箱
            follows.forEach(i -> stringRedisTemplate.opsForZSet().add(RedisConstants.ZSET_BLOG_MAILBOX_KEY + String.valueOf(i.getUserId()), String.valueOf(blog.getId()), System.currentTimeMillis()));
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result getHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        });
        return Result.ok(records);
    }

    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = ConcreteUser.get();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        //根据点赞的用户确定,将点赞的状态翻转
        Long userId = ConcreteUser.get().getId();
        String blogKey = RedisConstants.ZSET_BLOG_LIKE + id;
        //如果用户点赞,加入点赞队列,取消则从队列中删除
//        Boolean member = stringRedisTemplate.opsForSet().isMember(blogKey, String.valueOf(userId));
        //创建对应的点赞榜单
        Boolean member = stringRedisTemplate.opsForZSet().add(blogKey, String.valueOf(userId), System.currentTimeMillis());
        if (member) {
            //用户点赞
            update().setSql("liked = liked + 1").eq("id", id).update();
            return Result.ok(true);
        } else {
            //用户取消点赞
            update().setSql("liked = liked - 1").eq("id", id).update();
            stringRedisTemplate.opsForZSet().remove(blogKey, String.valueOf(userId));
        }
        return Result.ok(false);
    }

    @Override
    public Result getBlogLikeCount(Long id) {
        String blogKey = RedisConstants.ZSET_BLOG_LIKE + id;
        Set<String> users = stringRedisTemplate.opsForZSet().rangeByScore(blogKey, RedisConstants.BLOG_LIKE_ZSET_START, System.currentTimeMillis(), RedisConstants.BLOG_LIKE_OFFSET, RedisConstants.BLOG_LIKE_COUNT);
        if(users.size() == 0){
            return Result.ok();
        }
        String joinStr = String.join(",", users);
        List<User> userList = userService.query().in("id", users).last("order by field(id ," + joinStr + ")").list();
        List<UserDTO> dtos = userList.stream()
                .map(i -> BeanUtil.copyProperties(i, UserDTO.class, "")).collect(Collectors.toList());
        return Result.ok(dtos);
    }

    @Override
    public Result getBlogPage(Long userId, Integer currentPage) {
        Page<Blog> blogPage = query().eq("user_id", userId)
                .page(new Page<Blog>(currentPage, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(blogPage.getRecords());
    }

    /**
     * 当存在2个以上score相同时,会出现重复拉取得情况,目前没有好的方法解决
     * (保证score不会重复可以解决 score = time + index , 这种解决方法需要分离特定位数已得到时间)
     * @param lastId
     * @param offset
     * @return
     */
    @Override
    public Result getFollowBlog(Long lastId, Integer offset) {
        Long userId = ConcreteUser.get().getId();
        String userBlogMailBoxKey = RedisConstants.ZSET_BLOG_MAILBOX_KEY + userId;
        ScrollResult result = new ScrollResult();
        Set<ZSetOperations.TypedTuple<String>> blogSet = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(userBlogMailBoxKey, 0, lastId, offset, SystemConstants.SCROLL_PAGE_COUNT);
        if(blogSet == null || blogSet.isEmpty()){
            return Result.ok();
        }
        ArrayList<Blog> blogs = new ArrayList<>(blogSet.size());
        //List<Blog> + minTime + offset
        long minTime = 0;
        int newOffset = 1;
        for (ZSetOperations.TypedTuple<String> blog : blogSet) {
            blogs.add(query().eq("id", blog.getValue()).one());
            long score = blog.getScore().longValue();
            if(minTime == score){
                newOffset++;
            }else {
                minTime = score;
                newOffset = 1;
            }
        }
        result.setList(blogs);
        result.setMinTime(minTime);
        result.setOffset(newOffset);
        return Result.ok(result);
    }
}
