package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_TYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        //1.先查询redis，看有无该结果
        String typeJsonList = stringRedisTemplate.opsForValue().get(CACHE_TYPE_KEY);
        //1.1有的话，返回该结果
        if(!StrUtil.isBlank(typeJsonList)){
            //将string类型转list
            String[] typeShops = typeJsonList.split(",,");
            ArrayList<ShopType> shopTypes = new ArrayList<>();
            for(String s:typeShops){
                //将s转为对象
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                shopTypes.add(shopType);
            }
            //更新时间
            stringRedisTemplate.expire(CACHE_TYPE_KEY,CACHE_TYPE_TTL,TimeUnit.MINUTES);
            return Result.ok(shopTypes);
        }
        //1.2无的话，查数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();

        //2.数据库有的话，返回数据库的结果，加入缓存
        if(typeList!=null){
            //将list转为string
            StringBuilder stringBuilder = new StringBuilder();
            for (ShopType shopType:typeList) {
                String s = JSONUtil.toJsonStr(shopType);
                stringBuilder.append(s).append(",,");
            }
            //去掉逗号
            int length = stringBuilder.length();
            if(length>0){
                stringBuilder.setLength(length-2);
            }
            String strType=stringBuilder.toString();
            stringRedisTemplate.opsForValue().set(CACHE_TYPE_KEY,strType,CACHE_TYPE_TTL, TimeUnit.MINUTES);
            return Result.ok(typeList);
        }
        //2.1无的话，报异常
        return Result.fail("查询错误");
    }
}
