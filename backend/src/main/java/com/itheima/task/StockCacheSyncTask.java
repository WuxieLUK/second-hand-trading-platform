package com.itheima.task;

import com.itheima.mapper.GoodsMapper;
import com.itheima.pojo.Goods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品库存缓存同步定时任务
 * 负责初始化和定期刷新Redis中的库存缓存，保证缓存与数据库一致性
 */
@Slf4j
@Component // 必须加@Component让Spring扫描到
public class StockCacheSyncTask {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 库存缓存前缀（和OrderServiceImpl中保持一致）
    private static final String STOCK_CACHE_KEY = "goods:stock:";

    /**
     * 项目启动时初始化库存缓存
     * @PostConstruct 注解会在Spring容器初始化该Bean后立即执行
     */
    // @PostConstruct
    public void initStockCache() {
        log.info("开始初始化商品库存缓存...");
        try {
            // 查询所有在售商品（需确保GoodsMapper中有该方法）
            List<Goods> goodsList = goodsMapper.findAllOnSale();
            for (Goods goods : goodsList) {
                // 设置缓存，有效期24小时
                redisTemplate.opsForValue().set(
                        STOCK_CACHE_KEY + goods.getId(),
                        goods.getStock(),
                        24,
                        TimeUnit.HOURS
                );
            }
            log.info("商品库存缓存初始化完成，共初始化{}个在售商品", goodsList.size());
        } catch (Exception e) {
            log.error("初始化商品库存缓存失败", e);
        }
    }

//    /**
//     * 定时刷新库存缓存（每小时执行一次）
//     * cron表达式说明：0 0 */1 * * ? → 每小时的0分0秒执行
//    */
//     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void refreshStockCache() {
        log.info("开始定时刷新商品库存缓存...");
        // 直接复用初始化方法的逻辑
        initStockCache();
    }
}