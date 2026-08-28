-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: shop
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '105b01ab-ef88-11f0-83f5-72a04a391a20:1-22799';

--
-- Table structure for table `address`
--

DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `address_name` varchar(50) DEFAULT '' COMMENT '地址名称（如“宿舍地址”“家里地址”）',
  `province` varchar(20) NOT NULL DEFAULT '' COMMENT '省份',
  `city` varchar(20) NOT NULL DEFAULT '' COMMENT '城市',
  `district` varchar(20) NOT NULL DEFAULT '' COMMENT '区县',
  `detail_addr` varchar(255) NOT NULL DEFAULT '' COMMENT '详细地址（如XX宿舍X栋X单元X室）',
  `zip_code` varchar(6) DEFAULT '' COMMENT '邮政编码',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `user_id` int NOT NULL COMMENT '用户ID',
  `is_default` int DEFAULT NULL COMMENT '是否为默认地址',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地址表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address`
--

LOCK TABLES `address` WRITE;
/*!40000 ALTER TABLE `address` DISABLE KEYS */;
INSERT INTO `address` VALUES (16,'万州区郭村镇笔峰村9组84号','无','重庆市','万州区','万州区郭村镇笔峰村9组84号','100000','2026-03-01 20:46:07','2026-03-04 15:35:21',1,0),(17,'万州区郭村镇笔峰村9组84号','1','1','1','万州区郭村镇笔峰村9组84号','100000','2026-03-01 21:39:19','2026-03-04 15:35:26',1,1);
/*!40000 ALTER TABLE `address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint unsigned NOT NULL COMMENT '关联会话ID',
  `sender_id` int NOT NULL COMMENT '发送者ID',
  `receiver_id` int NOT NULL COMMENT '接收者ID',
  `content` varchar(1000) NOT NULL COMMENT '消息内容',
  `msg_type` tinyint DEFAULT '1' COMMENT '消息类型：1-文本 2-图片 3-语音',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读：0-未读 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_message`
--

LOCK TABLES `chat_message` WRITE;
/*!40000 ALTER TABLE `chat_message` DISABLE KEYS */;
INSERT INTO `chat_message` VALUES (1,2,1,1,'2',1,1,'2026-03-04 12:53:45'),(2,1,1,3,'你好',1,1,'2026-03-04 13:04:08'),(3,1,1,3,'1',1,1,'2026-03-05 01:32:49'),(4,1,1,3,'1',1,1,'2026-03-05 04:53:36'),(5,2,1,1,'你好',1,1,'2026-03-05 07:47:12'),(6,2,1,1,'您当前',1,1,'2026-03-05 07:47:22'),(7,1,1,3,'你',1,1,'2026-03-05 07:47:36'),(8,1,1,3,'1',1,1,'2026-03-05 07:48:00'),(9,1,3,1,'不好',1,1,'2026-03-05 07:48:51');
/*!40000 ALTER TABLE `chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_session`
--

DROP TABLE IF EXISTS `chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '会话 ID',
  `from_user_id` int NOT NULL COMMENT '发起方用户 ID',
  `to_user_id` int NOT NULL COMMENT '接收方用户 ID',
  `user_pair_min` int GENERATED ALWAYS AS (least(`from_user_id`,`to_user_id`)) VIRTUAL COMMENT '用户对最小编号',
  `user_pair_max` int GENERATED ALWAYS AS (greatest(`from_user_id`,`to_user_id`)) VIRTUAL COMMENT '用户对最大编号',
  `last_msg` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_msg_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后一条消息时间',
  `from_unread` int DEFAULT '0' COMMENT '发起方未读消息数',
  `to_unread` int DEFAULT '0' COMMENT '接收方未读消息数',
  `status` tinyint DEFAULT '1' COMMENT '会话状态:1-正常 2-已拉黑',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_pair` (`user_pair_min`,`user_pair_max`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_session`
--

LOCK TABLES `chat_session` WRITE;
/*!40000 ALTER TABLE `chat_session` DISABLE KEYS */;
INSERT INTO `chat_session` (`id`, `from_user_id`, `to_user_id`, `last_msg`, `last_msg_time`, `from_unread`, `to_unread`, `status`, `create_time`, `update_time`) VALUES (1,3,1,'不好','2026-03-05 07:48:51',0,0,1,'2026-03-03 15:44:31','2026-03-05 07:55:01'),(2,1,1,'您当前','2026-03-05 07:47:22',0,0,1,'2026-03-03 15:49:58','2026-03-05 07:47:38');
/*!40000 ALTER TABLE `chat_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `like_count` int DEFAULT '0',
  `nickname` varchar(20) DEFAULT NULL,
  `goods_id` int unsigned DEFAULT NULL COMMENT '商品ID',
  `user_url` varchar(500) DEFAULT NULL COMMENT '用户url',
  `parent_id` int DEFAULT NULL COMMENT '父级id',
  `comment_user_id` int NOT NULL COMMENT '用户id',
  PRIMARY KEY (`id`),
  KEY `fk_orders_customers` (`nickname`),
  KEY `goods_id` (`goods_id`),
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (46,'1','2026-02-25 08:06:13','2026-02-25 08:06:13',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',NULL,0),(47,'1x','2026-02-25 08:06:20','2026-02-25 08:06:20',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',46,0),(48,'1','2026-02-25 09:18:41','2026-02-25 09:18:41',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',46,0),(49,'1','2026-02-25 09:18:45','2026-02-25 09:18:45',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',46,0),(50,'1','2026-02-25 09:21:41','2026-02-25 09:21:41',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',NULL,0),(51,'1','2026-02-25 10:12:52','2026-02-25 10:12:52',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',46,0),(52,'1','2026-02-25 10:13:33','2026-02-25 10:13:33',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',NULL,0),(53,'1','2026-02-25 10:13:59','2026-02-25 10:13:59',NULL,'fsy',11,'https://free.picui.cn/free/2026/02/09/6989884c3d644.jpg',46,0);
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment_likes`
--

DROP TABLE IF EXISTS `comment_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_likes` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `user_id` int unsigned NOT NULL COMMENT '点赞人的用户 ID',
  `comment_id` int unsigned NOT NULL COMMENT '被点赞的评论 ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment_likes`
--

LOCK TABLES `comment_likes` WRITE;
/*!40000 ALTER TABLE `comment_likes` DISABLE KEYS */;
/*!40000 ALTER TABLE `comment_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods`
--

DROP TABLE IF EXISTS `goods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `goods_name` varchar(100) NOT NULL DEFAULT '' COMMENT '商品名称',
  `goods_desc` longtext COMMENT '商品描述（二手商品详情、新旧程度等）',
  `goods_pic` longtext COMMENT '商品图片URL（多个用逗号分隔）',
  `category_id` int unsigned NOT NULL DEFAULT '0' COMMENT '商品分类ID（如1-教材 2-电子产品 3-生活用品）',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '商品原价',
  `sell_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '售卖价格',
  `seller_id` int unsigned NOT NULL COMMENT '卖家ID（关联user表id）',
  `goods_status` tinyint NOT NULL DEFAULT '1' COMMENT '商品状态：1-在售 2-已售出 3-下架 4-审核中 5-违规封禁',
  `is_new` tinyint NOT NULL DEFAULT '0' COMMENT '新旧程度：0-二手 1-全新 2-9成新 3-8成新 4-7成新及以下',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `stock` bigint NOT NULL COMMENT '库存数量',
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_goods_status` (`goods_status`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二手商品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods`
--

LOCK TABLES `goods` WRITE;
/*!40000 ALTER TABLE `goods` DISABLE KEYS */;
INSERT INTO `goods` VALUES (11,'歪果手机','歪果牌手机你值得拥有+1！！！','https://free.picui.cn/free/2026/02/20/699865e78616b.jpg',9,111.00,11.00,1,1,0,'2026-02-25 14:53:04','2026-03-04 16:20:31',100),(12,'1','www','https://free.picui.cn/free/2026/02/09/698987f29890a.jpg',9,111.00,11.00,1,1,0,'2026-02-25 18:44:36','2026-03-05 13:03:42',100),(13,'高等数学（同济7版）','二手教材，仅使用过一学期，内页有少量笔记，无缺页','https://free.picui.cn/free/2026/02/09/698987aa42cc5.jpg',9,59.80,25.00,1,1,0,'2026-03-05 05:23:06','2026-03-05 13:27:58',11),(14,'小米13手机','自用小米13，使用8个月，电池健康度95%，无拆无修，配件齐全','https://free.picui.cn/free/2026/02/25/699e960613c0a.jpg',9,4299.00,2800.00,1,1,0,'2026-03-05 05:23:06','2026-03-05 13:28:24',11),(15,'宜家单人床垫','9成新单人床垫，使用1年，无污渍无破损，尺寸1.2*2.0m','https://free.picui.cn/free/2026/01/21/6970e978b56d7.jpg',9,399.00,150.00,1,1,0,'2026-03-05 05:23:06','2026-03-05 13:29:08',22),(16,'机械键盘','青轴机械键盘，使用半年，按键无失灵，赠送键帽一套','https://free.picui.cn/free/2026/03/05/69a9147a09b27.jpg',9,299.00,180.00,1,1,0,'2026-03-05 05:23:06','2026-03-05 13:29:51',100),(17,'Python编程入门书籍','全新未拆封Python入门书，买重了低价出','https://free.picui.cn/free/2026/03/05/69a914a373e2b.jpg',9,45.00,30.00,1,1,0,'2026-03-05 05:23:06','2026-03-05 13:30:32',33);
/*!40000 ALTER TABLE `goods` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods_collect`
--

DROP TABLE IF EXISTS `goods_collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods_collect` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int unsigned NOT NULL COMMENT '用户ID',
  `goods_id` int unsigned NOT NULL COMMENT '商品ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_goods` (`user_id`,`goods_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `goods_collect_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `goods_collect_ibfk_2` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_collect`
--

LOCK TABLES `goods_collect` WRITE;
/*!40000 ALTER TABLE `goods_collect` DISABLE KEYS */;
INSERT INTO `goods_collect` VALUES (3,1,11,'2026-02-26 06:27:28'),(6,1,12,'2026-03-04 08:04:34');
/*!40000 ALTER TABLE `goods_collect` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods_image`
--

DROP TABLE IF EXISTS `goods_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods_image` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `goods_id` int NOT NULL COMMENT '关联商品ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `ext_info` json DEFAULT NULL COMMENT '扩展信息（多态化）：如{"showRule":"carousel","size":"800x600"}',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品图片表（详情图/多态化图片）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_image`
--

LOCK TABLES `goods_image` WRITE;
/*!40000 ALTER TABLE `goods_image` DISABLE KEYS */;
INSERT INTO `goods_image` VALUES (12,11,'https://free.picui.cn/free/2026/02/20/699865e78616b.jpg',NULL,'2026-02-25 06:53:04'),(13,11,'https://free.picui.cn/free/2026/02/09/698987aa42cc5.jpg',NULL,'2026-02-25 06:53:04'),(20,12,'https://free.picui.cn/free/2026/02/25/699ed4ac53d01.jpg',NULL,'2026-02-25 10:57:08'),(21,12,'https://free.picui.cn/free/2025/12/10/69397d93867ac.png',NULL,'2026-02-25 10:57:08');
/*!40000 ALTER TABLE `goods_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods_like`
--

DROP TABLE IF EXISTS `goods_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods_like` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int unsigned NOT NULL COMMENT '用户ID',
  `goods_id` int unsigned NOT NULL COMMENT '商品ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_goods` (`user_id`,`goods_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `goods_like_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `goods_like_ibfk_2` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_like`
--

LOCK TABLES `goods_like` WRITE;
/*!40000 ALTER TABLE `goods_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `goods_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_info`
--

DROP TABLE IF EXISTS `order_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_info` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL DEFAULT '' COMMENT '订单编号（唯一，如时间戳+随机数）',
  `buyer_id` int unsigned NOT NULL COMMENT '买家ID（关联user表id）',
  `seller_id` int unsigned NOT NULL COMMENT '卖家ID（关联user表id）',
  `address_id` int unsigned NOT NULL COMMENT '收货地址ID（关联address表id）',
  `goods_id` int NOT NULL COMMENT '关联商品ID',
  `goods_name` varchar(100) NOT NULL COMMENT '商品名称（冗余）',
  `goods_pic` varchar(500) DEFAULT '' COMMENT '商品图片（冗余）',
  `goods_price` decimal(10,2) NOT NULL COMMENT '商品价格（冗余）',
  `goods_num` tinyint DEFAULT '1' COMMENT '商品数量（二手默认1）',
  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
  `order_status` tinyint NOT NULL DEFAULT '1' COMMENT '订单状态：1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-退款中 7-已退款',
  `pay_type` tinyint DEFAULT '0' COMMENT '支付方式：0-未支付 1-微信 2-支付宝 3-线下转账',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '确认收货时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `refund_status` tinyint DEFAULT '0' COMMENT '退款状态：0-无退款 1-退款中 2-退款成功 3-退款失败',
  `refund_amount` decimal(10,2) DEFAULT '0.00' COMMENT '退款金额',
  `refund_reason` varchar(500) DEFAULT NULL COMMENT '退款原因',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `refund_remark` varchar(500) DEFAULT NULL COMMENT '退款备注（商家/平台）',
  `remark` varchar(500) DEFAULT '' COMMENT '订单备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_order_status` (`order_status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_info`
--

LOCK TABLES `order_info` WRITE;
/*!40000 ALTER TABLE `order_info` DISABLE KEYS */;
INSERT INTO `order_info` VALUES (1,'312312',1,1,1,0,'','https://free.picui.cn/free/2026/01/26/697779ea754e1.jpg',0.00,1,1.00,2,0,'2026-02-24 00:28:15',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-02-23 22:21:30','2026-02-24 00:28:15'),(2,'20260304152831579909',1,1,16,11,'歪果手机','https://free.picui.cn/free/2026/02/20/699865e78616b.jpg',11.00,1,11.00,2,0,'2026-03-04 15:28:37',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-04 15:28:32','2026-03-04 15:28:37'),(3,'20260304153606392366',1,1,17,11,'歪果手机','https://free.picui.cn/free/2026/02/20/699865e78616b.jpg',11.00,4,44.00,2,0,'2026-03-04 15:36:10',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-04 15:36:06','2026-03-04 15:36:10'),(4,'20260304183435816310',1,1,17,12,'1','https://free.picui.cn/free/2026/02/09/698987f29890a.jpg',11.00,3,33.00,2,0,'2026-03-04 18:34:39',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-04 18:34:35','2026-03-04 18:34:39');
/*!40000 ALTER TABLE `order_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `permission_id` int NOT NULL AUTO_INCREMENT,
  `permission_name` varchar(50) NOT NULL,
  `permission_description` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `permission_name` (`permission_name`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
INSERT INTO `permissions` VALUES (1,'/user/info','博客根据用户名查询用户','2025-07-11 16:16:53','2025-07-11 16:17:18'),(2,'/user/updates','博客更新用户信息','2025-07-11 16:16:55','2025-07-11 16:17:14'),(3,'/user/updateAvatar','博客更新用户头像','2025-07-11 16:17:00','2025-07-11 16:17:13'),(4,'/user/updatePwd','博客更新用户密码','2025-07-11 16:16:56','2025-07-11 16:17:11'),(5,'/user/userList','博客用户管理','2025-07-11 16:17:10','2025-07-11 16:17:17'),(6,'/user/{id}','博客删除用户','2025-07-11 16:17:05','2025-07-14 19:42:11'),(7,'/user/allUserList','博客查询所有用户列表','2025-07-11 16:17:08','2025-07-11 16:17:21'),(9,'/roles/add','博客增加角色','2025-07-11 16:20:33','2025-07-11 16:20:33'),(10,'/roles/list','博客获取角色分页列表','2025-07-11 16:20:33','2025-07-11 16:20:33'),(11,'/roles/info/{id}','博客根据id获取角色详细信息','2025-07-11 16:20:33','2025-07-11 16:20:33'),(12,'/roles/update','博客更新角色信息','2025-07-11 16:20:33','2025-07-11 16:20:33'),(13,'/roles/delete/{id}','博客删除角色信息','2025-07-11 16:20:33','2025-07-11 16:20:33'),(14,'/roles/allRolesList','博客查找所有角色','2025-07-11 16:20:33','2025-07-11 16:20:33'),(15,'/roles/userRolesList','博客用户角色表分页查询','2025-07-11 16:20:33','2025-07-11 16:20:33'),(16,'/roles/userRolesDelete/{id}','博客删除用户角色关联表','2025-07-11 16:20:33','2025-07-11 16:20:33'),(17,'/roles/userRolesAdd','博客添加用户角色关联表','2025-07-11 16:21:02','2025-07-11 16:21:02'),(18,'/permission/add','博客增加权限','2025-07-11 16:23:34','2025-07-11 16:23:34'),(19,'/permission/list','博客获取权限分页列表','2025-07-11 16:23:34','2025-07-11 16:23:34'),(20,'/permission/info/{id}','博客根据列表id获取详细权限信息','2025-07-11 16:23:34','2025-07-14 19:42:48'),(21,'/permission/update','博客更新权限信息','2025-07-11 16:23:34','2025-07-11 16:23:34'),(22,'/permission/delete/{id}','博客删除权限信息','2025-07-11 16:23:34','2025-07-11 16:23:34'),(23,'/permission/allPermissionList','博客获取权限列表','2025-07-11 16:23:34','2025-07-11 16:23:34'),(24,'/permission/permissionRolesList','博客用户角色表分页查询','2025-07-11 16:23:34','2025-07-11 16:23:34'),(25,'/permission/permissionRolesAdd','博客添加用户角色关联','2025-07-11 16:24:33','2025-07-11 16:24:33'),(26,'/permission/permissionRolesDelete/{id}','博客删除用户角色关联','2025-07-11 16:24:33','2025-07-11 16:24:33'),(27,'/File/upload','博客图片上传','2025-07-11 16:25:15','2025-07-11 16:25:15'),(28,'/comment/add','博客评论添加','2025-07-11 16:27:49','2025-07-11 16:27:49'),(29,'/comment/list','博客评论分页查询','2025-07-11 16:27:49','2025-07-11 16:27:49'),(30,'/comment/update','博客更新评论','2025-07-11 16:27:49','2025-07-11 16:27:49'),(31,'/comment/delete/{id}','博客删除评论','2025-07-11 16:27:49','2025-07-11 16:27:49'),(50,'/message/add','博客添加留言','2025-07-27 14:36:08','2025-07-27 14:48:43'),(53,'/message/page','博客留言分页查询','2025-07-28 21:58:54','2025-07-28 21:58:54'),(54,'/message/delete/{id}','博客留言删除','2025-07-28 22:31:14','2025-07-28 22:31:14'),(55,'/message/update','博客留言更新','2025-07-28 22:31:36','2025-07-28 22:31:36'),(64,'/comment/like/{id}','评论点赞','2026-01-19 09:09:52','2026-01-19 09:09:52'),(77,'/ai/chat','AI智能客服','2026-01-26 13:58:00','2026-01-26 13:58:00'),(78,'/goods/add','添加商品','2026-02-22 05:29:30','2026-02-22 05:29:30'),(79,'/goods/list','分页查询商品','2026-02-22 05:29:56','2026-02-22 05:29:56'),(80,'/goods/detail','商品详情','2026-02-22 05:30:15','2026-02-22 05:30:15'),(81,'/goods/update','修改商品','2026-02-22 05:30:31','2026-02-22 05:30:31'),(82,'/goods/delete','删除商品','2026-02-22 05:30:46','2026-02-22 05:30:46'),(83,'/goods/updateStatus','更新商品状态','2026-02-22 05:31:10','2026-02-22 05:31:10'),(84,'/shopcategory/add','添加商品分类','2026-02-22 05:43:00','2026-02-22 05:43:00'),(85,'/shopcategory/list','获取全部商品分类','2026-02-22 05:43:35','2026-02-22 05:43:35'),(86,'/shopcategory/detail','根据id查询商品分类信息','2026-02-22 05:43:56','2026-02-22 05:43:56'),(87,'/shopcategory/update','修改商品分类信息','2026-02-22 05:44:26','2026-02-22 05:44:26'),(88,'/shopcategory/delete','删除商品分类','2026-02-22 05:44:44','2026-02-22 05:44:44'),(89,'/address/add','新增地址','2026-02-23 10:37:33','2026-02-23 10:37:33'),(90,'/address/list','用户分页查询地址列表','2026-02-23 10:37:47','2026-02-23 10:37:47'),(91,'/address/allList','分页查询所有用户的地址列表','2026-02-23 10:38:07','2026-02-23 10:38:07'),(92,'/address/detail','查询地址详情','2026-02-23 10:38:23','2026-02-23 10:38:23'),(93,'/address/update','修改地址','2026-02-23 10:38:35','2026-02-23 10:38:35'),(94,'/address/delete','删除地址','2026-02-23 10:38:51','2026-02-23 10:38:51'),(95,'/address/setDefault','设置默认地址','2026-02-23 10:39:07','2026-02-23 10:39:07'),(96,'/address/default','查询用户默认地址','2026-02-23 10:39:43','2026-02-23 10:39:43'),(97,'/order/create','创建订单','2026-02-23 13:57:36','2026-02-23 13:57:36'),(98,'/order/list','查询订单列表','2026-02-23 13:57:53','2026-02-23 13:57:53'),(99,'/order/detail','查询订单详情','2026-02-23 13:58:07','2026-02-23 13:58:07'),(100,'/order/updateStatus','更新订单状态','2026-02-23 13:58:22','2026-02-23 13:58:22'),(101,'/order/cancel','取消订单','2026-02-23 13:58:40','2026-02-23 13:58:40'),(102,'/order/confirmReceive','订单确认收货','2026-02-23 13:58:57','2026-02-23 13:58:57'),(103,'/goodsCollect/add/{goodsId}','添加商品收藏','2026-02-24 13:34:24','2026-02-24 13:34:24'),(104,'/goodsCollect/list/{goodsId}','查询当前用户是否收藏该商品','2026-02-24 13:34:39','2026-02-24 13:34:39'),(105,'/goodsCollect/delete/{goodsId}','取消商品收藏','2026-02-24 13:34:54','2026-02-24 13:34:54'),(107,'/goodsCollect/myList','我的商品收藏列表（分页）','2026-02-24 13:35:34','2026-02-24 13:35:34'),(108,'/goods/like/add/{goodsId}','添加商品点赞','2026-02-24 13:35:54','2026-02-24 13:35:54'),(109,'/goods/like/delete/{goodsId}','取消商品点赞','2026-02-24 13:36:09','2026-02-24 13:36:09'),(110,'/goods/like/list/user/{goodsId}','查询当前用户是否点赞该商品','2026-02-24 13:36:25','2026-02-24 13:36:25'),(112,'/goods/mylist','查询我的商品','2026-02-25 06:05:10','2026-02-25 06:05:10'),(113,'/goods/updateStatusOFF_SHELF','更新商品状态为下架','2026-02-25 13:13:39','2026-02-25 13:13:39'),(114,'/address/myList','分页查询我的地址列表','2026-03-01 12:44:01','2026-03-01 12:44:01'),(115,'/chat/myList','核心接口：获取我的聊天列表（所有会话/聊天对象）','2026-03-03 14:27:27','2026-03-03 14:27:27'),(116,'/chat/msg/{sessionId}','获取会话历史消息（分页）','2026-03-03 14:27:43','2026-03-03 14:27:43'),(117,'/chat/send','发送消息','2026-03-03 14:28:01','2026-03-03 14:28:01'),(118,'/chat/markRead/{sessionId}','标记会话消息为已读','2026-03-03 14:28:16','2026-03-03 14:28:16'),(119,'/chat/createSession/{receiverId}','创建会话（前端一般无需主动调用，发送消息时后端自动创建）','2026-03-03 15:34:15','2026-03-03 15:49:38');
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions_roles`
--

DROP TABLE IF EXISTS `permissions_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions_roles` (
  `permission_id` int NOT NULL,
  `role_id` int NOT NULL,
  `permission_role_id` int NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`permission_role_id`),
  KEY `permissions_roles_ibfk_2` (`role_id`),
  KEY `permissions_roles_ibfk_1` (`permission_id`),
  CONSTRAINT `permissions_roles_ibfk_1` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`permission_id`) ON DELETE CASCADE,
  CONSTRAINT `permissions_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions_roles`
--

LOCK TABLES `permissions_roles` WRITE;
/*!40000 ALTER TABLE `permissions_roles` DISABLE KEYS */;
INSERT INTO `permissions_roles` VALUES (1,1,1,'2025-07-11 16:38:23','2025-07-11 16:37:26'),(2,1,2,'2025-07-11 16:37:29','2025-07-11 16:37:38'),(3,1,3,'2025-07-11 16:37:27','2025-07-11 16:37:39'),(4,1,4,'2025-07-11 16:37:36','2025-07-11 16:37:40'),(5,1,5,'2025-07-11 16:37:41','2025-07-11 16:37:43'),(6,1,6,'2025-07-11 16:37:44','2025-07-11 16:37:45'),(7,1,7,'2025-07-11 16:37:48','2025-07-11 16:37:54'),(9,1,9,'2025-07-11 16:37:58','2025-07-11 16:37:58'),(10,1,10,'2025-07-11 16:37:57','2025-07-11 16:38:05'),(11,1,11,'2025-07-11 16:37:56','2025-07-11 16:38:04'),(12,1,12,'2025-07-11 16:37:55','2025-07-11 16:38:03'),(13,1,13,'2025-07-11 16:38:54','2025-07-11 16:38:02'),(14,1,14,'2025-07-11 16:39:02','2025-07-11 16:38:01'),(15,1,15,'2025-07-11 16:39:01','2025-07-11 16:38:01'),(16,1,16,'2025-07-11 16:39:00','2025-07-11 16:38:53'),(17,1,17,'2025-07-11 16:38:59','2025-07-11 16:37:59'),(18,1,18,'2025-07-11 16:38:59','2025-07-11 16:38:52'),(19,1,19,'2025-07-11 16:38:58','2025-07-11 16:38:51'),(20,1,20,'2025-07-11 16:38:57','2025-07-11 16:38:50'),(21,1,21,'2025-07-11 16:38:48','2025-07-11 16:38:49'),(22,1,22,'2025-07-11 16:38:47','2025-07-11 16:38:46'),(23,1,23,'2025-07-11 16:38:55','2025-07-11 16:38:44'),(24,1,24,'2025-07-11 16:38:43','2025-07-11 16:38:43'),(25,1,25,'2025-07-11 16:38:33','2025-07-11 16:38:35'),(26,1,26,'2025-07-11 16:38:31','2025-07-11 16:38:34'),(27,1,27,'2025-07-11 16:38:36','2025-07-11 16:38:37'),(28,1,28,'2025-07-11 16:38:30','2025-07-11 16:38:41'),(29,1,29,'2025-07-11 16:38:30','2025-07-11 16:38:40'),(30,1,30,'2025-07-11 16:38:29','2025-07-11 16:38:39'),(31,1,31,'2025-07-11 16:38:28','2025-07-11 16:38:38'),(3,2,41,'2025-07-14 19:02:10','2025-07-14 19:02:10'),(4,2,42,'2025-07-14 19:02:27','2025-07-14 19:02:27'),(27,2,44,'2025-07-14 19:03:35','2025-07-14 19:03:35'),(28,2,45,'2025-07-14 19:03:49','2025-07-14 19:03:49'),(29,2,46,'2025-07-14 19:04:26','2025-07-14 19:04:26'),(2,2,47,'2025-07-14 19:06:12','2025-07-14 19:06:12'),(1,2,49,'2025-07-14 20:06:13','2025-07-14 20:06:13'),(50,1,65,'2025-07-27 14:36:20','2025-07-27 14:36:20'),(50,2,66,'2025-07-27 14:36:23','2025-07-27 14:36:23'),(53,1,70,'2025-07-28 21:59:00','2025-07-28 21:59:00'),(55,1,71,'2025-07-28 22:31:47','2025-07-28 22:31:47'),(54,1,72,'2025-07-28 22:31:55','2025-07-28 22:31:55'),(64,2,81,'2026-01-19 09:10:14','2026-01-19 09:10:14'),(64,1,82,'2026-01-19 09:10:18','2026-01-19 09:10:18'),(77,1,100,'2026-01-26 13:58:08','2026-01-26 13:58:08'),(77,2,101,'2026-01-26 13:58:12','2026-01-26 13:58:12'),(78,1,102,'2026-02-22 05:31:33','2026-02-22 05:31:33'),(79,1,106,'2026-02-22 05:39:35','2026-02-22 05:39:35'),(80,1,107,'2026-02-22 05:40:20','2026-02-22 05:40:20'),(81,1,108,'2026-02-22 05:40:34','2026-02-22 05:40:34'),(82,1,109,'2026-02-22 05:40:54','2026-02-22 05:40:54'),(83,1,110,'2026-02-22 05:41:02','2026-02-22 05:41:02'),(84,1,111,'2026-02-22 05:44:57','2026-02-22 05:44:57'),(85,1,112,'2026-02-22 05:45:19','2026-02-22 05:45:19'),(86,1,113,'2026-02-22 05:45:30','2026-02-22 05:45:30'),(87,1,114,'2026-02-22 05:45:49','2026-02-22 05:45:49'),(88,1,115,'2026-02-22 05:46:14','2026-02-22 05:46:14'),(89,1,116,'2026-02-23 10:39:54','2026-02-23 10:39:54'),(90,1,117,'2026-02-23 10:40:08','2026-02-23 10:40:08'),(91,1,118,'2026-02-23 10:40:19','2026-02-23 10:40:19'),(92,1,119,'2026-02-23 10:40:36','2026-02-23 10:40:36'),(93,1,120,'2026-02-23 10:40:47','2026-02-23 10:40:47'),(94,1,121,'2026-02-23 10:40:56','2026-02-23 10:40:56'),(95,1,122,'2026-02-23 10:41:05','2026-02-23 10:41:05'),(96,1,123,'2026-02-23 10:41:13','2026-02-23 10:41:13'),(97,1,124,'2026-02-23 13:59:09','2026-02-23 13:59:09'),(98,1,125,'2026-02-23 13:59:24','2026-02-23 13:59:24'),(99,1,126,'2026-02-23 13:59:37','2026-02-23 13:59:37'),(100,1,127,'2026-02-23 13:59:50','2026-02-23 13:59:50'),(101,1,128,'2026-02-23 13:59:59','2026-02-23 13:59:59'),(102,1,129,'2026-02-23 14:00:24','2026-02-23 14:00:24'),(103,1,130,'2026-02-24 13:37:46','2026-02-24 13:37:46'),(104,1,131,'2026-02-24 13:38:40','2026-02-24 13:38:40'),(105,1,132,'2026-02-24 13:39:01','2026-02-24 13:39:01'),(107,1,134,'2026-02-24 13:39:34','2026-02-24 13:39:34'),(108,1,135,'2026-02-24 13:39:51','2026-02-24 13:39:51'),(109,1,136,'2026-02-24 13:40:04','2026-02-24 13:40:04'),(110,1,137,'2026-02-24 13:40:12','2026-02-24 13:40:12'),(112,1,139,'2026-02-25 06:05:19','2026-02-25 06:05:19'),(113,1,140,'2026-02-25 13:15:14','2026-02-25 13:15:14'),(114,1,141,'2026-03-01 12:44:20','2026-03-01 12:44:20'),(115,1,142,'2026-03-03 14:28:22','2026-03-03 14:28:22'),(116,1,143,'2026-03-03 14:28:32','2026-03-03 14:28:32'),(117,1,144,'2026-03-03 14:28:42','2026-03-03 14:28:42'),(118,1,145,'2026-03-03 14:28:49','2026-03-03 14:28:49'),(119,1,146,'2026-03-03 15:34:21','2026-03-03 15:34:21');
/*!40000 ALTER TABLE `permissions_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resources`
--

DROP TABLE IF EXISTS `resources`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resources` (
  `resource_id` int NOT NULL AUTO_INCREMENT,
  `resource_name` varchar(50) NOT NULL,
  `resource_type` varchar(20) DEFAULT NULL,
  `resource_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`resource_id`),
  UNIQUE KEY `resource_name` (`resource_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resources`
--

LOCK TABLES `resources` WRITE;
/*!40000 ALTER TABLE `resources` DISABLE KEYS */;
/*!40000 ALTER TABLE `resources` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_description` text,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'管理员','系统管理员','2025-07-11 15:05:42','2026-01-17 09:45:25'),(2,'用户','用户','2025-07-11 15:06:16','2025-07-11 15:06:17'),(3,'游客','无权限','2025-07-11 15:06:33','2025-07-11 15:06:35');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shop_category`
--

DROP TABLE IF EXISTS `shop_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_category` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `category_name` varchar(32) NOT NULL COMMENT '分类名称',
  `category_alias` varchar(32) NOT NULL COMMENT '分类别名',
  `create_user` varchar(40) NOT NULL COMMENT '创建人姓名',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `fk_shop_category_user` (`create_user`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shop_category`
--

LOCK TABLES `shop_category` WRITE;
/*!40000 ALTER TABLE `shop_category` DISABLE KEYS */;
INSERT INTO `shop_category` VALUES (9,'软件','rj','1','2026-02-21 21:27:39','2026-02-22 12:58:46');
/*!40000 ALTER TABLE `shop_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_tags` varchar(500) DEFAULT NULL COMMENT '用户标签',
  `username` varchar(20) NOT NULL COMMENT '用户名',
  `password` varchar(32) DEFAULT NULL COMMENT '密码',
  `nickname` varchar(10) DEFAULT '' COMMENT '昵称',
  `email` varchar(128) DEFAULT '' COMMENT '邮箱',
  `user_pic` longtext,
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `phone` varchar(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,NULL,'fsy','202cb962ac59075b964b07152d234b70','元神','s13628419482@qq.com','https://free.picui.cn/free/2026/03/01/69a44b2d5ff46.jpg','2025-11-05 19:46:01','2026-03-01 14:22:02','18581201659'),(3,NULL,'fsy1','202cb962ac59075b964b07152d234b70','','','https://free.picui.cn/free/2026/01/26/697779ea754e1.jpg','2026-01-26 14:03:29','2026-01-26 15:01:35',NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_role_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `role_id` int NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`user_role_id`),
  KEY `role_id` (`role_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (8,1,1,'2025-11-05 19:46:01','2025-11-05 19:46:01'),(10,3,2,'2026-01-26 14:03:29','2026-01-26 14:03:29'),(11,3,1,'2026-03-03 14:53:56','2026-03-03 14:53:56');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-05 18:59:11
