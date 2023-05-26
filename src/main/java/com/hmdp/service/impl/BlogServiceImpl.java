package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result getBlog(Integer id) {
        Blog blog=this.getById(id);
        if(blog==null){
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        //存入是否点赞
        isBlogLinked(blog);
        return Result.ok(blog);
    }

    private void isBlogLinked(Blog blog) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2.判读登录用户是否点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Boolean isMembet = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        //3.将是否点赞信息set到blog中
        blog.setIsLike(BooleanUtil.isTrue(isMembet));
    }


    /**
     * 点赞功能实现及判读逻辑
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2.判读登录用户是否点赞
        String key = BLOG_LIKED_KEY + id;
        Boolean isMembet = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        //3.若未点赞，可以点赞
        if (BooleanUtil.isFalse(isMembet)){
            //3.1 数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            //3.2 保存用户到redis的set集合中
            if (isSuccess){
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        }else {
            //4.若已点赞，取消点赞
            //4.1 数据库点赞数-1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            //4.2 清除redis的set集合里的userId
            if (isSuccess){
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }



    private void queryBlogUser(Blog blog) {
        Long useId= blog.getUserId();
        User user=userService.getById(useId);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }
}
