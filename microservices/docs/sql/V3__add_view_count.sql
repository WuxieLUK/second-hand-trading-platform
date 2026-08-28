-- ES 搜索行为排序所需：添加商品浏览数字段
-- 执行前请确认 goods 表不存在 view_count 列

ALTER TABLE goods ADD COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数，用于 ES 行为排序';

-- 将已有的收藏数据同步到 ES（需要重启 service-product 触发 @PostConstruct fullSync）
