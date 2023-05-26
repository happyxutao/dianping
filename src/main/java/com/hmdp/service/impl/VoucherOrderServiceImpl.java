package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券的信息
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        //2.1秒杀没开始
        if(beginTime.isAfter(LocalDateTime.now())){
            return Result.fail("秒杀还未开始");
        }
        LocalDateTime endTime = seckillVoucher.getEndTime();
        //2.2秒杀已经结束
        if(endTime.isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }
        //3.库存是否充足
        Integer stock = seckillVoucher.getStock();
        if(stock<=0){
            return Result.fail("库存不足");
        }
        Long userId=UserHolder.getUser().getId();
        synchronized (userId.toString().intern()){
            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        //一人一单
        Long userId=UserHolder.getUser().getId();
        int count=query().eq("user_id",userId).eq("voucher_id", voucherId).count();
        if(count>0){
            return Result.fail("用户已经购买过一次了");
        }
        /**
         * 添上乐观锁，看库存是否发生变化，如果发生变化，则说明已经有人修改了，seckillVoucher.getStock()一开始的存储
         */
        //4.扣减库存
        boolean success = seckillVoucherService.update().setSql("stock=stock-1").eq("voucher_id", voucherId).
                gt("stock",0).update();
        //4.1如果成功，则说明当前执行过程中，没有受其它线程干扰，如果失败，则说明，有问题
        if(!success){
            return Result.fail("购买失败,请重试");
        }
        //5.创建订单
        VoucherOrder voucherOrder=new VoucherOrder();
        //5.1订单id
        voucherOrder.setId(redisIdWorker.nextId("voucher_order"));
        //5.2用户id
        UserDTO user = UserHolder.getUser();
        voucherOrder.setUserId(user.getId());
        //5.3代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(voucherOrder.getId());}

}
