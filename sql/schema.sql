-- =====================================================================
--  基于 SpringBoot+Vue 的非遗手工艺品展示与预约体验平台
--  数据库脚本 schema.sql  ——  26 张表 + 初始化数据
--  DBMS : MySQL 8.0    字符集 : utf8mb4
--  作者 : 李明峰 23100025        指导教师 : 吴长伟
--  生成阶段 : P1
-- ---------------------------------------------------------------------
--  执行方式（命令行）：
--    mysql -h 127.0.0.1 -P 3307 -u root -p123456 < schema.sql
--  或在 Navicat 中打开本文件，选择连接后直接运行
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

CREATE DATABASE IF NOT EXISTS `heritage` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `heritage`;

-- ---------------------------------------------------------------------
--  0. 清理旧表（按依赖倒序）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `craftsman_follow`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `browse_history`;
DROP TABLE IF EXISTS `banner`;
DROP TABLE IF EXISTS `article_like`;
DROP TABLE IF EXISTS `article_comment`;
DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `user_coupon`;
DROP TABLE IF EXISTS `coupon`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `booking_config`;
DROP TABLE IF EXISTS `booking_status_log`;
DROP TABLE IF EXISTS `booking`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `cart`;
DROP TABLE IF EXISTS `goods_image`;
DROP TABLE IF EXISTS `goods_spec`;
DROP TABLE IF EXISTS `goods`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `craftsman`;
DROP TABLE IF EXISTS `user`;


-- =====================================================================
--  1. 用户与匠人
-- =====================================================================

-- 1.1 用户表（含管理员、匠人账号）
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
  `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密）',
  `nickname`    VARCHAR(50)           DEFAULT NULL   COMMENT '昵称',
  `avatar`      VARCHAR(255)          DEFAULT NULL   COMMENT '头像 URL',
  `phone`       VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
  `email`       VARCHAR(100)          DEFAULT NULL   COMMENT '邮箱',
  `gender`      TINYINT               DEFAULT 0      COMMENT '性别 0未知 1男 2女',
  `role`        TINYINT      NOT NULL DEFAULT 1      COMMENT '角色 1普通用户 2匠人 3管理员',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 0禁用 1正常',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_status` (`role`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表（含三种角色）';

-- 1.2 匠人扩展信息
CREATE TABLE `craftsman` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        BIGINT       NOT NULL                COMMENT '关联 user.id',
  `real_name`      VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
  `craft_type`     VARCHAR(50)           DEFAULT NULL   COMMENT '技艺门类，如 剪纸/木雕',
  `inheritance`    VARCHAR(255)          DEFAULT NULL   COMMENT '师承',
  `intro`          VARCHAR(500)          DEFAULT NULL   COMMENT '个人简介',
  `resume`         TEXT                                 COMMENT '履历（富文本）',
  `cover`          VARCHAR(255)          DEFAULT NULL   COMMENT '主页封面图',
  `id_card_no`     VARCHAR(30)           DEFAULT NULL   COMMENT '证件号（资质审核用）',
  `cert_image`     VARCHAR(255)          DEFAULT NULL   COMMENT '资质证明图',
  `audit_status`   TINYINT      NOT NULL DEFAULT 0      COMMENT '审核状态 0待审核 1已通过 2已驳回',
  `reject_reason`  VARCHAR(255)          DEFAULT NULL   COMMENT '驳回原因',
  `audit_time`     DATETIME              DEFAULT NULL   COMMENT '审核时间',
  `follower_count` INT                   DEFAULT 0      COMMENT '关注数',
  `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '匠人扩展信息表';


-- =====================================================================
--  2. 商品相关
-- =====================================================================

-- 2.1 非遗分类
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
  `icon`        VARCHAR(255)          DEFAULT NULL   COMMENT '图标 URL',
  `description` VARCHAR(255)          DEFAULT NULL   COMMENT '描述',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序号，越小越前',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 0停用 1启用',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '非遗分类表';

-- 2.2 手工艺品/商品
CREATE TABLE `goods` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `craftsman_id`  BIGINT        NOT NULL                COMMENT '所属匠人 craftsman.id',
  `category_id`   BIGINT        NOT NULL                COMMENT '分类 category.id',
  `name`          VARCHAR(100)  NOT NULL                COMMENT '商品名称',
  `subtitle`      VARCHAR(255)           DEFAULT NULL   COMMENT '副标题/卖点',
  `cover`         VARCHAR(255)           DEFAULT NULL   COMMENT '封面图 URL',
  `description`   VARCHAR(1000)          DEFAULT NULL   COMMENT '简要描述',
  `detail`        LONGTEXT                               COMMENT '详情（富文本）',
  `price`         DECIMAL(10,2) NOT NULL                COMMENT '售价',
  `original_price` DECIMAL(10,2)         DEFAULT NULL   COMMENT '原价（划线价）',
  `stock`         INT           NOT NULL DEFAULT 0      COMMENT '库存',
  `sales`         INT           NOT NULL DEFAULT 0      COMMENT '销量',
  `view_count`    INT           NOT NULL DEFAULT 0      COMMENT '浏览量',
  `is_handmade`   TINYINT       NOT NULL DEFAULT 1      COMMENT '是否纯手工 0否 1是',
  `status`        TINYINT       NOT NULL DEFAULT 2      COMMENT '状态 0下架 1上架 2待审核',
  `audit_status`  TINYINT       NOT NULL DEFAULT 0      COMMENT '审核状态 0待审核 1通过 2驳回',
  `reject_reason` VARCHAR(255)           DEFAULT NULL   COMMENT '驳回原因',
  `deleted`       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`   DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_status` (`category_id`, `status`),
  KEY `idx_craftsman` (`craftsman_id`),
  KEY `idx_status_sales` (`status`, `sales`),
  KEY `idx_price` (`price`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '手工艺品商品表';

-- 2.3 商品规格
CREATE TABLE `goods_spec` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_id`    BIGINT        NOT NULL                COMMENT '商品 goods.id',
  `spec_name`   VARCHAR(50)   NOT NULL                COMMENT '规格名，如 20cm/30cm',
  `price`       DECIMAL(10,2) NOT NULL                COMMENT '规格售价',
  `stock`       INT           NOT NULL DEFAULT 0      COMMENT '规格库存',
  `sort`        INT           NOT NULL DEFAULT 0      COMMENT '排序',
  `deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品规格表';

-- 2.4 商品图集
CREATE TABLE `goods_image` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_id`    BIGINT       NOT NULL                COMMENT '商品 goods.id',
  `url`         VARCHAR(255) NOT NULL                COMMENT '图片 URL',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品图集表';


-- =====================================================================
--  3. 交易相关
-- =====================================================================

-- 3.1 购物车
CREATE TABLE `cart` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户 user.id',
  `goods_id`    BIGINT   NOT NULL                COMMENT '商品 goods.id',
  `spec_id`     BIGINT            DEFAULT NULL   COMMENT '规格 goods_spec.id，可为空',
  `quantity`    INT      NOT NULL DEFAULT 1      COMMENT '数量',
  `checked`     TINYINT  NOT NULL DEFAULT 1      COMMENT '是否勾选 0否 1是',
  `deleted`     TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  UNIQUE KEY `uk_user_goods_spec` (`user_id`, `goods_id`, `spec_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车表';

-- 3.2 收货地址
CREATE TABLE `address` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '用户 user.id',
  `receiver`    VARCHAR(50)  NOT NULL                COMMENT '收货人',
  `phone`       VARCHAR(20)  NOT NULL                COMMENT '联系电话',
  `province`    VARCHAR(50)           DEFAULT NULL   COMMENT '省',
  `city`        VARCHAR(50)           DEFAULT NULL   COMMENT '市',
  `district`    VARCHAR(50)           DEFAULT NULL   COMMENT '区/县',
  `detail`      VARCHAR(255) NOT NULL                COMMENT '详细地址',
  `is_default`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认 0否 1是',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收货地址表';

-- 3.3 订单主表
CREATE TABLE `orders` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`         VARCHAR(32)   NOT NULL                COMMENT '订单号',
  `user_id`          BIGINT        NOT NULL                COMMENT '用户 user.id',
  `craftsman_id`     BIGINT                 DEFAULT NULL   COMMENT '匠人 craftsman.id（便于匠人端分单）',
  `total_amount`     DECIMAL(10,2) NOT NULL                COMMENT '商品总额',
  `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '优惠金额',
  `pay_amount`       DECIMAL(10,2) NOT NULL                COMMENT '实付金额',
  `coupon_id`        BIGINT                 DEFAULT NULL   COMMENT '使用的优惠券 coupon.id',
  `user_coupon_id`   BIGINT                 DEFAULT NULL   COMMENT '用户券 user_coupon.id',
  `status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0待付款 1待发货 2待收货 3已完成 4已退款 5已取消',
  `address_snapshot` VARCHAR(500)           DEFAULT NULL   COMMENT '收货地址快照（JSON）',
  `receiver`         VARCHAR(50)            DEFAULT NULL   COMMENT '收货人快照',
  `receiver_phone`   VARCHAR(20)            DEFAULT NULL   COMMENT '收货电话快照',
  `logistics_company` VARCHAR(50)           DEFAULT NULL   COMMENT '物流公司',
  `logistics_no`     VARCHAR(50)            DEFAULT NULL   COMMENT '物流单号',
  `remark`           VARCHAR(255)           DEFAULT NULL   COMMENT '买家备注',
  `craftsman_remark` VARCHAR(255)           DEFAULT NULL   COMMENT '匠人备注',
  `refund_reason`    VARCHAR(255)           DEFAULT NULL   COMMENT '退款原因',
  `refund_reject_reason` VARCHAR(255)       DEFAULT NULL   COMMENT '退款驳回原因',
  `refund_status`    TINYINT       NOT NULL DEFAULT 0      COMMENT '退款状态 0无 1申请中 2已同意 3已驳回',
  `pay_time`         DATETIME               DEFAULT NULL   COMMENT '支付时间',
  `delivery_time`    DATETIME               DEFAULT NULL   COMMENT '发货时间',
  `finish_time`      DATETIME               DEFAULT NULL   COMMENT '完成时间',
  `deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`      DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_craftsman_status` (`craftsman_id`, `status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

-- 3.4 订单项（含商品快照）
CREATE TABLE `order_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT        NOT NULL                COMMENT '订单 orders.id',
  `order_no`    VARCHAR(32)            DEFAULT NULL   COMMENT '订单号',
  `goods_id`    BIGINT        NOT NULL                COMMENT '商品 goods.id',
  `spec_id`     BIGINT                 DEFAULT NULL   COMMENT '规格 goods_spec.id',
  `goods_name`  VARCHAR(100)  NOT NULL                COMMENT '商品名称快照',
  `goods_image` VARCHAR(255)           DEFAULT NULL   COMMENT '商品图片快照',
  `spec_name`   VARCHAR(50)            DEFAULT NULL   COMMENT '规格名快照',
  `price`       DECIMAL(10,2) NOT NULL                COMMENT '成交单价快照',
  `quantity`    INT           NOT NULL                COMMENT '数量',
  `reviewed`    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已评价 0否 1是',
  `create_time` DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_goods` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单项表（商品信息快照）';


-- =====================================================================
--  4. 体验课程与预约（题眼模块）
-- =====================================================================

-- 4.1 体验课程
CREATE TABLE `course` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `craftsman_id`      BIGINT        NOT NULL                COMMENT '授课匠人 craftsman.id',
  `category_id`       BIGINT                 DEFAULT NULL   COMMENT '关联非遗分类 category.id',
  `title`             VARCHAR(100)  NOT NULL                COMMENT '课程标题',
  `subtitle`          VARCHAR(255)           DEFAULT NULL   COMMENT '副标题',
  `cover`             VARCHAR(255)           DEFAULT NULL   COMMENT '封面图',
  `description`       VARCHAR(1000)          DEFAULT NULL   COMMENT '课程简介',
  `detail`            LONGTEXT                               COMMENT '课程详情（富文本）',
  `address`           VARCHAR(255)           DEFAULT NULL   COMMENT '上课地点',
  `start_time`        DATETIME      NOT NULL                COMMENT '开课时间',
  `end_time`          DATETIME      NOT NULL                COMMENT '结束时间',
  `price`             DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '课程费用',
  `total_quota`       INT           NOT NULL                COMMENT '总名额',
  `remain_quota`      INT           NOT NULL                COMMENT '剩余名额',
  `booking_deadline`  DATETIME               DEFAULT NULL   COMMENT '预约截止时间',
  `cancel_deadline`   DATETIME               DEFAULT NULL   COMMENT '可取消截止时间（开课前的时点）',
  `duration`          INT                    DEFAULT NULL   COMMENT '课时（分钟）',
  `difficulty`        TINYINT                DEFAULT 1      COMMENT '难度 1入门 2进阶 3高级',
  `view_count`        INT           NOT NULL DEFAULT 0      COMMENT '浏览量',
  `booking_count`     INT           NOT NULL DEFAULT 0      COMMENT '累计预约数',
  `status`            TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0未开始 1进行中 2已结束',
  `audit_status`      TINYINT       NOT NULL DEFAULT 0      COMMENT '审核状态 0待审核 1通过 2驳回',
  `reject_reason`     VARCHAR(255)           DEFAULT NULL   COMMENT '驳回原因',
  `deleted`           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`       DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_craftsman` (`craftsman_id`),
  KEY `idx_status_start` (`status`, `start_time`),
  KEY `idx_start_time` (`start_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '体验课程表';

-- 4.2 课程预约（题眼：状态机 + 核销码）
CREATE TABLE `booking` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `booking_no`    VARCHAR(32)  NOT NULL                COMMENT '预约单号',
  `course_id`     BIGINT       NOT NULL                COMMENT '课程 course.id',
  `user_id`       BIGINT       NOT NULL                COMMENT '预约用户 user.id',
  `craftsman_id`  BIGINT                DEFAULT NULL   COMMENT '匠人 craftsman.id（冗余，便于匠人端查询）',
  `contact_name`  VARCHAR(50)  NOT NULL                COMMENT '联系人',
  `contact_phone` VARCHAR(20)  NOT NULL                COMMENT '联系电话',
  `quota`         INT          NOT NULL DEFAULT 1      COMMENT '预约名额数（含陪同）',
  `remark`        VARCHAR(255)          DEFAULT NULL   COMMENT '用户备注',
  `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成',
  `checkin_code`  VARCHAR(16)           DEFAULT NULL   COMMENT '核销码（审核通过后生成）',
  `checkin_time`  DATETIME              DEFAULT NULL   COMMENT '核销签到时间',
  `audit_time`    DATETIME              DEFAULT NULL   COMMENT '审核时间',
  `auditor_id`    BIGINT                DEFAULT NULL   COMMENT '审核人 user.id',
  `reject_reason` VARCHAR(255)          DEFAULT NULL   COMMENT '拒绝原因',
  `cancel_reason` VARCHAR(255)          DEFAULT NULL   COMMENT '取消原因',
  `cancel_by`     TINYINT               DEFAULT NULL   COMMENT '取消发起方 1用户 2匠人 3管理员 4系统',
  `cancel_time`   DATETIME              DEFAULT NULL   COMMENT '取消时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_no` (`booking_no`),
  -- 三层防超卖之第三层：唯一索引防重复预约
  UNIQUE KEY `uk_user_course` (`user_id`, `course_id`),
  UNIQUE KEY `uk_checkin_code` (`checkin_code`),
  KEY `idx_course_status` (`course_id`, `status`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_craftsman_status` (`craftsman_id`, `status`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程预约表（题眼）';

-- 4.2.1 预约状态流转轨迹（P8 新增）
--
-- 为什么不能只靠 booking 表上的 audit_time / cancel_time 反推：
--   那些字段只保留「最后一次」的结果，一条预约被驳回 → 用户改约 → 再通过的轨迹
--   在上面完全看不出来。纠纷处理时「这条预约到底经历了什么」必须有据可查。
--
-- 为什么 operator_role 单独存一列而不是只存 operator_id：
--   系统自动操作（超时自动拒绝、结课推进）没有 operator_id，
--   用 role=4 表示「系统」；用户/匠人/管理员则按 Constants.CANCEL_BY_* 同一套编码。
--
-- 为什么 from_status 允许为 NULL：
--   预约被创建的那一刻没有「前一个状态」，用 NULL 表示，比硬塞一个 0 更诚实
--   （0 本身是一个真实存在的状态「待审核」）。
CREATE TABLE `booking_status_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `booking_id`    BIGINT       NOT NULL                COMMENT '预约 booking.id',
  `from_status`   TINYINT               DEFAULT NULL   COMMENT '原状态，NULL 表示创建',
  `to_status`     TINYINT      NOT NULL                COMMENT '新状态 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成',
  `operator_id`   BIGINT                DEFAULT NULL   COMMENT '操作人 user.id，系统操作为 NULL',
  `operator_role` TINYINT               DEFAULT NULL   COMMENT '操作方 1用户 2匠人 3管理员 4系统',
  `operator_name` VARCHAR(50)           DEFAULT NULL   COMMENT '操作人名称（冗余，操作人注销后仍可读）',
  `remark`        VARCHAR(255)          DEFAULT NULL   COMMENT '说明，如拒绝原因 / 取消原因',
  `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_booking` (`booking_id`, `id`),
  KEY `idx_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约状态流转轨迹';

-- 4.3 预约规则配置（全局，可在管理端修改）
CREATE TABLE `booking_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(50)  NOT NULL                COMMENT '配置键',
  `config_value` VARCHAR(100) NOT NULL                COMMENT '配置值',
  `config_name`  VARCHAR(100)          DEFAULT NULL   COMMENT '配置名称',
  `description`  VARCHAR(255)          DEFAULT NULL   COMMENT '说明',
  `update_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约规则配置表';


-- =====================================================================
--  5. 互动与内容
-- =====================================================================

-- 5.1 统一评价表（商品/课程共用，type 区分）
CREATE TABLE `review` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type`        TINYINT      NOT NULL                COMMENT '类型 1商品评价 2课程评价',
  `target_id`   BIGINT       NOT NULL                COMMENT '评价对象：商品 id 或课程 id',
  `order_id`    BIGINT                DEFAULT NULL   COMMENT '关联订单（商品评价）',
  `booking_id`  BIGINT                DEFAULT NULL   COMMENT '关联预约（课程评价）',
  `user_id`     BIGINT       NOT NULL                COMMENT '评价人 user.id',
  `rating`      TINYINT      NOT NULL DEFAULT 5      COMMENT '评分 1-5',
  `content`     VARCHAR(1000)         DEFAULT NULL   COMMENT '评价内容',
  `images`      VARCHAR(1000)         DEFAULT NULL   COMMENT '图片 URL 列表（JSON，最多 3 张）',
  `reply`       VARCHAR(500)          DEFAULT NULL   COMMENT '匠人回复',
  `reply_time`  DATETIME              DEFAULT NULL   COMMENT '回复时间',
  `like_count`  INT          NOT NULL DEFAULT 0      COMMENT '有用数',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_target` (`type`, `target_id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '统一评价表（商品/课程）';

-- 5.2 优惠券
CREATE TABLE `coupon` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`           VARCHAR(100)  NOT NULL                COMMENT '优惠券名称',
  `type`           TINYINT       NOT NULL                COMMENT '类型 1满减 2折扣',
  `threshold`      DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '使用门槛金额',
  `amount`         DECIMAL(10,2)          DEFAULT NULL   COMMENT '满减金额（type=1）',
  `discount`       DECIMAL(4,2)           DEFAULT NULL   COMMENT '折扣率 0-1（type=2，如 0.85 表示 85 折）',
  `total_count`    INT           NOT NULL                COMMENT '发放总量',
  `received_count` INT           NOT NULL DEFAULT 0      COMMENT '已领取数量',
  `per_limit`      INT           NOT NULL DEFAULT 1      COMMENT '每人限领张数',
  `scope`          TINYINT       NOT NULL DEFAULT 1      COMMENT '适用范围 1全场通用 2仅商品 3仅课程',
  `start_time`     DATETIME               DEFAULT NULL   COMMENT '有效期开始',
  `end_time`       DATETIME               DEFAULT NULL   COMMENT '有效期结束',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态 0停用 1启用',
  `description`    VARCHAR(255)           DEFAULT NULL   COMMENT '使用说明',
  `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`    DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '优惠券表';

-- 5.3 用户领券
CREATE TABLE `user_coupon` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT      NOT NULL                COMMENT '用户 user.id',
  `coupon_id`   BIGINT      NOT NULL                COMMENT '优惠券 coupon.id',
  `status`      TINYINT     NOT NULL DEFAULT 0      COMMENT '状态 0未使用 1已使用 2已过期',
  `order_no`    VARCHAR(32)          DEFAULT NULL   COMMENT '使用的订单号',
  `used_time`   DATETIME             DEFAULT NULL   COMMENT '使用时间',
  `expire_time` DATETIME             DEFAULT NULL   COMMENT '过期时间（冗余自 coupon.end_time）',
  `deleted`     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `update_time` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_coupon` (`coupon_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户优惠券表';

-- 5.4 资讯公告
CREATE TABLE `article` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(150) NOT NULL                COMMENT '标题',
  `category_id`   BIGINT                DEFAULT NULL   COMMENT '分类 category.id',
  `cover`         VARCHAR(255)          DEFAULT NULL   COMMENT '封面图',
  `summary`       VARCHAR(500)          DEFAULT NULL   COMMENT '摘要',
  `content`       LONGTEXT                              COMMENT '正文（富文本）',
  `author`        VARCHAR(50)           DEFAULT NULL   COMMENT '作者',
  `tags`          VARCHAR(255)          DEFAULT NULL   COMMENT '标签，逗号分隔',
  `view_count`    INT          NOT NULL DEFAULT 0      COMMENT '浏览量',
  `like_count`    INT          NOT NULL DEFAULT 0      COMMENT '点赞数',
  `comment_count` INT          NOT NULL DEFAULT 0      COMMENT '评论数',
  `is_top`        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否置顶 0否 1是',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 0草稿/下架 1已发布',
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_top` (`status`, `is_top`),
  KEY `idx_category` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资讯公告表';

-- 5.5 资讯评论（T2 功能：表已建，实现可延后）
CREATE TABLE `article_comment` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `article_id`  BIGINT        NOT NULL                COMMENT '资讯 article.id',
  `user_id`     BIGINT        NOT NULL                COMMENT '评论人 user.id',
  `content`     VARCHAR(1000) NOT NULL                COMMENT '评论内容',
  `parent_id`   BIGINT        NOT NULL DEFAULT 0      COMMENT '父评论 id，一级评论为 0',
  `like_count`  INT           NOT NULL DEFAULT 0      COMMENT '点赞数',
  `deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article` (`article_id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资讯评论表（T2）';

-- 5.6 资讯点赞（T2 功能：表已建，实现可延后）
CREATE TABLE `article_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户 user.id',
  `article_id`  BIGINT   NOT NULL                COMMENT '资讯 article.id',
  `create_time` DATETIME          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`, `article_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资讯点赞表（T2）';

-- 5.7 轮播图
CREATE TABLE `banner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`       VARCHAR(100)          DEFAULT NULL   COMMENT '标题',
  `image`       VARCHAR(255) NOT NULL                COMMENT '图片 URL',
  `link_url`    VARCHAR(255)          DEFAULT NULL   COMMENT '跳转链接',
  `position`    TINYINT      NOT NULL DEFAULT 1      COMMENT '位置 1首页 2课程页',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序，越小越前',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 0停用 1启用',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_position_status_sort` (`position`, `status`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '轮播图表';

-- 5.8 浏览历史
CREATE TABLE `browse_history` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户 user.id',
  `target_type` TINYINT  NOT NULL                COMMENT '1商品 2课程 3资讯',
  `target_id`   BIGINT   NOT NULL                COMMENT '目标 id',
  `browse_time` DATETIME          DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `create_time` DATETIME          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  -- 同一用户对同一目标只保留一条，重复浏览更新时间
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_user_time` (`user_id`, `browse_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '浏览历史表';

-- 5.9 站内消息
CREATE TABLE `message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '接收人 user.id',
  `type`        TINYINT      NOT NULL                COMMENT '1预约审核 2订单发货 3退款 4优惠券 5系统公告 6预约提醒',
  `title`       VARCHAR(100) NOT NULL                COMMENT '消息标题',
  `content`     VARCHAR(500)          DEFAULT NULL   COMMENT '消息内容',
  `related_id`  BIGINT                DEFAULT NULL   COMMENT '关联业务 id',
  `is_read`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读 0未读 1已读',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息表';

-- 5.10 收藏
CREATE TABLE `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户 user.id',
  `target_type` TINYINT  NOT NULL                COMMENT '1商品 2课程 3资讯',
  `target_id`   BIGINT   NOT NULL                COMMENT '目标 id',
  `deleted`     TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收藏表（商品/课程/资讯共用）';

-- 5.11 留言反馈
CREATE TABLE `feedback` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT                DEFAULT NULL   COMMENT '提交人 user.id',
  `type`        TINYINT      NOT NULL DEFAULT 1      COMMENT '类型 1功能建议 2内容纠错 3投诉举报 4其他',
  `content`     VARCHAR(1000) NOT NULL               COMMENT '反馈内容',
  `contact`     VARCHAR(100)          DEFAULT NULL   COMMENT '联系方式',
  `reply`       VARCHAR(1000)         DEFAULT NULL   COMMENT '管理员回复',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0待处理 1处理中 2已完成',
  `handle_time` DATETIME              DEFAULT NULL   COMMENT '处理完成时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '留言反馈表';

-- 5.12 操作日志（AOP 异步写入）
CREATE TABLE `operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id` BIGINT                DEFAULT NULL   COMMENT '操作人 user.id',
  `operator`    VARCHAR(50)           DEFAULT NULL   COMMENT '操作人用户名',
  `role`        TINYINT               DEFAULT NULL   COMMENT '角色 1用户 2匠人 3管理员',
  `operation`   VARCHAR(100)          DEFAULT NULL   COMMENT '操作描述',
  `method`      VARCHAR(200)          DEFAULT NULL   COMMENT '方法签名',
  `params`      VARCHAR(2000)         DEFAULT NULL   COMMENT '请求参数（截断）',
  `ip`          VARCHAR(50)           DEFAULT NULL   COMMENT '操作 IP',
  `cost_ms`     BIGINT                DEFAULT NULL   COMMENT '耗时（毫秒）',
  `success`     TINYINT               DEFAULT 1      COMMENT '是否成功 0否 1是',
  `error_msg`   VARCHAR(500)          DEFAULT NULL   COMMENT '异常信息',
  `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator_time` (`operator`, `create_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- 5.13 匠人关注关系（P5 新增）
--     规划书里「匠人关注」属 T2 可裁剪项，但它是匠人主页的核心互动动作、
--     成本很低（一张关系表 + 一个计数），因此按计划实现。
--     唯一键保证关注关系唯一；取消关注走逻辑删除，再次关注时由业务层「复活」原记录。
CREATE TABLE `craftsman_follow` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT   NOT NULL                COMMENT '用户 user.id',
  `craftsman_id` BIGINT   NOT NULL                COMMENT '匠人 craftsman.id',
  `deleted`      TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除 0关注中 1已取消',
  `create_time`  DATETIME          DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `update_time`  DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_craftsman` (`user_id`, `craftsman_id`),
  KEY `idx_craftsman` (`craftsman_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '匠人关注关系表';


-- =====================================================================
--  6. 初始化数据
-- =====================================================================

-- 6.1 预约规则配置（5 条）
INSERT INTO `booking_config` (`config_key`, `config_value`, `config_name`, `description`) VALUES
('booking_advance_hours', '2',   '最少提前预约时长', '距离开课还需至少多少小时才允许提交预约'),
('cancel_before_hours',   '24',  '开课前可取消时限', '开课前多少小时内不允许用户自行取消预约'),
('max_booking_per_course','1',   '每人每课程限约次数', '同一用户对同一门课程最多可预约的次数'),
('audit_required',        '1',   '是否强制审核',     '1=预约需匠人审核后生效；0=提交即为已通过'),
('audit_timeout_hours',   '24',  '超时未审自动拒绝', '匠人超过该小时数未审核，系统自动拒绝并归还名额');

-- 6.2 用户：1 管理员 + 1 演示用户 + 3 匠人账号
--     统一密码 123456（BCrypt 加密）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `gender`, `role`, `status`) VALUES
(1, 'admin',    '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '平台管理员', '/api/uploads/demo/avatar-admin.svg', '13800000000', 'admin@heritage.com',  1, 3, 1),
(2, 'user01',   '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '非遗爱好者', '/api/uploads/demo/avatar-user1.svg', '13800000001', 'user01@heritage.com', 2, 1, 1),
(3, 'user02',   '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '手作小白',   '/api/uploads/demo/avatar-user2.svg', '13800000002', 'user02@heritage.com', 1, 1, 1),
(11, 'zhangjian','$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '张建·剪纸',  '/api/uploads/demo/avatar-c1.svg', '13900000001', 'zhangjian@heritage.com', 1, 2, 1),
(12, 'limu',     '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '李木·木雕',  '/api/uploads/demo/avatar-c2.svg', '13900000002', 'limu@heritage.com',     1, 2, 1),
(13, 'wangtao',  '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '王陶·陶艺',  '/api/uploads/demo/avatar-c3.svg', '13900000003', 'wangtao@heritage.com',  2, 2, 1);

-- 6.3 匠人资料（3 位，均已通过审核）
INSERT INTO `craftsman` (`id`, `user_id`, `real_name`, `craft_type`, `inheritance`, `intro`, `cover`, `audit_status`, `audit_time`, `follower_count`) VALUES
(1, 11, '张建', '剪纸', '师承国家级非遗剪纸传承人王秀英', '河北蔚县人，从事剪纸艺术 26 年。作品以蔚县刻纸为主，擅长花鸟与民俗题材，多次参加省级非遗展演。', '/api/uploads/demo/craft-cover-1.svg', 1, NOW(), 128),
(2, 12, '李木', '木雕', '师承东阳木雕第五代传人李德胜', '浙江东阳人，专攻东阳木雕浅浮雕技法，代表作为《清明上河图》局部复刻，现致力于传统技法与现代家居结合。', '/api/uploads/demo/craft-cover-2.svg', 1, NOW(), 96),
(3, 13, '王陶', '陶艺', '师承景德镇陶瓷大学非遗研修班', '江西景德镇人，专注柴烧陶艺与青花分水，主张「让陶器回到日常生活」，开发了系列入门体验课程。', '/api/uploads/demo/craft-cover-3.svg', 1, NOW(), 87);

-- 6.4 非遗分类（6 个）
INSERT INTO `category` (`id`, `name`, `icon`, `description`, `sort`, `status`) VALUES
(1, '剪纸', '/api/uploads/demo/cat-paper.svg',  '以纸为材，以剪代笔，蔚县剪纸、扬州剪纸均为代表',  1, 1),
(2, '木雕', '/api/uploads/demo/cat-wood.svg',   '东阳木雕、潮州木雕等，讲究散点透视与层次雕刻',    2, 1),
(3, '陶艺', '/api/uploads/demo/cat-pottery.svg','景德镇青花、宜兴紫砂，泥与火的千年对话',          3, 1),
(4, '刺绣', '/api/uploads/demo/cat-embro.svg',  '苏绣、湘绣、蜀绣、粤绣，四大名绣各擅其美',        4, 1),
(5, '编织', '/api/uploads/demo/cat-weave.svg',  '竹编、草编、柳编，就地取材的民间智慧',            5, 1),
(6, '漆器', '/api/uploads/demo/cat-lacquer.svg','平遥推光漆器、扬州漆器，百里千刀一斤漆',          6, 1);

-- 6.5 商品（6 个分类，每类 3~4 件，共 20 件）
INSERT INTO `goods` (`id`, `craftsman_id`, `category_id`, `name`, `subtitle`, `cover`, `description`, `price`, `original_price`, `stock`, `sales`, `view_count`, `status`, `audit_status`) VALUES
(1,  1, 1, '蔚县手工剪纸·喜上眉梢', '纯手工刻制，含实木相框', '/api/uploads/demo/goods-1.svg',  '采用蔚县刻纸技法，一刀一刻皆手工完成，配原木色相框，适合家居装饰与礼品赠送。', 268.00, 358.00, 50, 36, 1280, 1, 1),
(2,  1, 1, '剪纸书签·四季花鸟（4枚装）', '轻薄便携，可作伴手礼',   '/api/uploads/demo/goods-2.svg',  '四季花鸟主题剪纸书签四枚一套，附赠锦盒，是阅读与收藏的雅致之选。', 88.00,  118.00, 120, 84, 960, 1, 1),
(3,  1, 1, '大幅剪纸·百子图（限量版）',  '巨幅作品，耗时 40 天',   '/api/uploads/demo/goods-3.svg',  '大幅百子图剪纸，历时 40 天手工刻制完成，限量 10 件，附收藏证书。', 2880.00, 3680.00, 8, 3, 620, 1, 1),
(4,  2, 2, '东阳木雕·松鹤笔筒', '浅浮雕工艺，文房雅器', '/api/uploads/demo/goods-4.svg',  '东阳木雕浅浮雕技法制成，松鹤延年题材，既是笔筒也是案头清供。', 1280.00, 1580.00, 20, 12, 880, 1, 1),
(5,  2, 2, '木雕摆件·如意观音', '整木雕琢，无拼接',     '/api/uploads/demo/goods-5.svg',  '选用香樟整木雕琢而成，观音面容慈悲，衣纹流畅，适合供奉与收藏。', 3680.00, 4280.00, 10, 5, 540, 1, 1),
(6,  2, 2, '木雕茶则·禅意荷塘', '茶席必备，手感温润',     '/api/uploads/demo/goods-6.svg',  '小叶紫檀边角料制成，荷塘清趣题材，握感温润，是茶席上的一抹禅意。', 460.00, 580.00, 35, 21, 720, 1, 1),
(7,  2, 2, '木雕挂屏·山水四条屏', '客厅陈设，可定制尺寸', '/api/uploads/demo/goods-7.svg',  '山水四条屏，以东阳木雕多层镂空技法制作，可根据墙面尺寸定制。', 6800.00, 8800.00, 5, 2, 410, 1, 1),
(8,  3, 3, '柴烧茶盏·天青釉', '柴窑烧制，每只唯一', '/api/uploads/demo/goods-8.svg',  '柴窑自然落灰成釉，天青色调，每只茶盏的窑变效果都独一无二。', 380.00, 480.00, 40, 28, 1120, 1, 1),
(9,  3, 3, '青花分水·缠枝莲盖碗', '景德镇手工绘制',   '/api/uploads/demo/goods-9.svg',  '景德镇青花分水技法手工绘制缠枝莲纹，釉面莹润，适合冲泡绿茶与乌龙。', 560.00, 680.00, 25, 15, 890, 1, 1),
(10, 3, 3, '手作陶花瓶·粗陶素坯', '原生态风格，插花利器', '/api/uploads/demo/goods-10.svg', '粗陶素坯不上釉，保留了泥土的原始质感，搭配干花或枯枝别有韵味。', 220.00, 280.00, 60, 42, 760, 1, 1),
(11, 3, 3, '陶艺体验套票·亲子课', '含材料，成品可带走', '/api/uploads/demo/goods-11.svg', '亲子陶艺体验套票，含 2 份材料与烧制费用，成品可带走（约 15 天后取）。', 168.00, 198.00, 100, 63, 1340, 1, 1),
(12, 1, 4, '苏绣团扇·双面绣猫', '双面异色绣，可裱框', '/api/uploads/demo/goods-12.svg', '苏绣双面刺绣团扇，一猫两面神态各异，配红木柄，可作摆件或把玩。', 1580.00, 1980.00, 15, 8, 650, 1, 1),
(13, 1, 4, '刺绣手帕·并蒂莲（2条）', '日常可用，送礼体面', '/api/uploads/demo/goods-13.svg', '纯棉底料手工刺绣并蒂莲纹样，一盒两条，日常使用或赠礼皆宜。', 168.00, 218.00, 80, 55, 580, 1, 1),
(14, 1, 4, '苗绣背扇·蝶恋花', '苗族传统纹样，手工老绣', '/api/uploads/demo/goods-14.svg', '苗族传统蝶恋花纹样背扇，采用老绣片改制，色彩浓烈，极具民族特色。', 1280.00, 1680.00, 6, 2, 320, 1, 1),
(15, 2, 5, '竹编提篮·六角孔', '青皮竹手工编织', '/api/uploads/demo/goods-15.svg', '选用三年生青皮竹，六角孔编法，透气耐用，买菜、收纳、野餐皆可用。', 198.00, 258.00, 45, 33, 470, 1, 1),
(16, 3, 5, '草编收纳筐·三件套', '天然蒲草，可叠放', '/api/uploads/demo/goods-16.svg', '天然蒲草手工编织，三种尺寸可叠放收纳，透气防潮，适合衣柜整理。', 158.00, 208.00, 70, 48, 520, 1, 1),
(17, 2, 5, '柳编花器·落地款', '高大造型，配干花更佳', '/api/uploads/demo/goods-17.svg', '柳条编织落地花器，高约 80cm，搭配干花或芦苇别具北欧风格。', 480.00, 620.00, 12, 6, 280, 1, 1),
(18, 1, 6, '平遥推光漆器·首饰盒', '32 道工序手工推光', '/api/uploads/demo/goods-18.svg', '平遥推光漆器工艺，经 32 道工序手工打磨推光，盒面描金花鸟，内衬绒布。', 880.00, 1080.00, 18, 11, 690, 1, 1),
(19, 2, 6, '漆器茶盘·大漆螺钿', '天然大漆，螺钿镶嵌', '/api/uploads/demo/goods-19.svg', '天然生漆髹涂，螺钿镶嵌山水纹样，耐热耐水，是茶席上的重器。', 1680.00, 2080.00, 9, 4, 430, 1, 1),
(20, 3, 6, '漆艺杯垫·四色一组', '入门级漆器，日常可用', '/api/uploads/demo/goods-20.svg', '四色一组漆艺杯垫，采用天然大漆手工髹涂，隔热防烫，日常可用。', 168.00, 228.00, 90, 67, 610, 1, 1);

-- 6.6 商品规格（部分商品有规格）
INSERT INTO `goods_spec` (`goods_id`, `spec_name`, `price`, `stock`, `sort`) VALUES
(1, '20cm 标准款', 268.00, 30, 1),
(1, '30cm 加大款', 398.00, 20, 2),
(2, '四季花鸟 4 枚',  88.00, 80, 1),
(2, '十二生肖 12 枚', 228.00, 40, 2),
(8, '单只装', 380.00, 25, 1),
(8, '双只礼盒装', 720.00, 15, 2),
(10, '小号 15cm', 220.00, 35, 1),
(10, '大号 25cm', 320.00, 25, 2),
(15, '中号', 198.00, 25, 1),
(15, '大号', 268.00, 20, 2);

-- 6.7 商品图集（每件 3 张，共 60 条）
INSERT INTO `goods_image` (`goods_id`, `url`, `sort`) VALUES
(1,'/api/uploads/demo/goods-1.svg',1),(1,'/api/uploads/demo/goods-1-b.svg',2),(1,'/api/uploads/demo/goods-1-c.svg',3),
(2,'/api/uploads/demo/goods-2.svg',1),(2,'/api/uploads/demo/goods-2-b.svg',2),(2,'/api/uploads/demo/goods-2-c.svg',3),
(3,'/api/uploads/demo/goods-3.svg',1),(3,'/api/uploads/demo/goods-3-b.svg',2),(3,'/api/uploads/demo/goods-3-c.svg',3),
(4,'/api/uploads/demo/goods-4.svg',1),(4,'/api/uploads/demo/goods-4-b.svg',2),(4,'/api/uploads/demo/goods-4-c.svg',3),
(5,'/api/uploads/demo/goods-5.svg',1),(5,'/api/uploads/demo/goods-5-b.svg',2),(5,'/api/uploads/demo/goods-5-c.svg',3),
(6,'/api/uploads/demo/goods-6.svg',1),(6,'/api/uploads/demo/goods-6-b.svg',2),(6,'/api/uploads/demo/goods-6-c.svg',3),
(7,'/api/uploads/demo/goods-7.svg',1),(7,'/api/uploads/demo/goods-7-b.svg',2),(7,'/api/uploads/demo/goods-7-c.svg',3),
(8,'/api/uploads/demo/goods-8.svg',1),(8,'/api/uploads/demo/goods-8-b.svg',2),(8,'/api/uploads/demo/goods-8-c.svg',3),
(9,'/api/uploads/demo/goods-9.svg',1),(9,'/api/uploads/demo/goods-9-b.svg',2),(9,'/api/uploads/demo/goods-9-c.svg',3),
(10,'/api/uploads/demo/goods-10.svg',1),(10,'/api/uploads/demo/goods-10-b.svg',2),(10,'/api/uploads/demo/goods-10-c.svg',3),
(11,'/api/uploads/demo/goods-11.svg',1),(11,'/api/uploads/demo/goods-11-b.svg',2),(11,'/api/uploads/demo/goods-11-c.svg',3),
(12,'/api/uploads/demo/goods-12.svg',1),(12,'/api/uploads/demo/goods-12-b.svg',2),(12,'/api/uploads/demo/goods-12-c.svg',3),
(13,'/api/uploads/demo/goods-13.svg',1),(13,'/api/uploads/demo/goods-13-b.svg',2),(13,'/api/uploads/demo/goods-13-c.svg',3),
(14,'/api/uploads/demo/goods-14.svg',1),(14,'/api/uploads/demo/goods-14-b.svg',2),(14,'/api/uploads/demo/goods-14-c.svg',3),
(15,'/api/uploads/demo/goods-15.svg',1),(15,'/api/uploads/demo/goods-15-b.svg',2),(15,'/api/uploads/demo/goods-15-c.svg',3),
(16,'/api/uploads/demo/goods-16.svg',1),(16,'/api/uploads/demo/goods-16-b.svg',2),(16,'/api/uploads/demo/goods-16-c.svg',3),
(17,'/api/uploads/demo/goods-17.svg',1),(17,'/api/uploads/demo/goods-17-b.svg',2),(17,'/api/uploads/demo/goods-17-c.svg',3),
(18,'/api/uploads/demo/goods-18.svg',1),(18,'/api/uploads/demo/goods-18-b.svg',2),(18,'/api/uploads/demo/goods-18-c.svg',3),
(19,'/api/uploads/demo/goods-19.svg',1),(19,'/api/uploads/demo/goods-19-b.svg',2),(19,'/api/uploads/demo/goods-19-c.svg',3),
(20,'/api/uploads/demo/goods-20.svg',1),(20,'/api/uploads/demo/goods-20-b.svg',2),(20,'/api/uploads/demo/goods-20-c.svg',3);

-- 6.8 体验课程（5 门）
--     课程 1：名额紧张（3 个），用于并发演示
--     课程 2：正常开放
--     课程 3：名额充足
--     课程 4：即将开课（用于「不可取消」规则演示）
--     课程 5：已结束（用于状态与评价演示）
INSERT INTO `course`
(`id`,`craftsman_id`,`category_id`,`title`,`subtitle`,`cover`,`description`,`address`,`start_time`,`end_time`,`price`,`total_quota`,`remain_quota`,`booking_deadline`,`cancel_deadline`,`duration`,`difficulty`,`view_count`,`booking_count`,`status`,`audit_status`) VALUES
(1, 1, 1, '蔚县剪纸入门体验课', '从握刀到成品的 3 小时', '/api/uploads/demo/course-1.svg',
 '带你认识蔚县刻纸的工具与技法，从描样、钉纸、刻制到装裱，3 小时内完成一幅属于自己的剪纸作品，成品当场带走。',
 '河北省张家口市蔚县非遗工坊 2 楼体验区',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY) + INTERVAL 3 HOUR,
 128.00, 10, 3, DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY),
 180, 1, 1680, 7, 0, 1),
(2, 2, 2, '东阳木雕浅浮雕入门', '认识刀具与木材的一次课程', '/api/uploads/demo/course-2.svg',
 '讲解东阳木雕的历史与流派，介绍常用雕刻刀具，并在老师的指导下完成一件浅浮雕小挂件。含全部材料费。',
 '浙江省金华市东阳市木雕小镇研习所',
 DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY) + INTERVAL 4 HOUR,
 268.00, 8, 6, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 240, 2, 1320, 2, 0, 1),
(3, 3, 3, '陶艺拉坯初体验', '亲手做一个自己的碗', '/api/uploads/demo/course-3.svg',
 '从揉泥开始，学习拉坯的基本手法，每人可完成 2 个器型，可选择 1 个烧制后快递到家（约 15 天）。适合零基础。',
 '江西省景德镇市陶溪川文创街区 A 区',
 DATE_ADD(NOW(), INTERVAL 12 DAY), DATE_ADD(NOW(), INTERVAL 12 DAY) + INTERVAL 3 HOUR,
 198.00, 15, 15, DATE_ADD(NOW(), INTERVAL 11 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY),
 180, 1, 2140, 0, 0, 1),
(4, 1, 4, '苏绣基础针法体验', '平针、套针、打籽一次学会', '/api/uploads/demo/course-4.svg',
 '系统讲解苏绣的平针、套针、打籽等基础针法，在一方手帕上完成一朵小花，课程结束后可带走自己的作品。',
 '江苏省苏州市镇湖苏绣小镇体验中心',
 DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 3 HOUR,
 168.00, 12, 9, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
 180, 1, 980, 3, 0, 1),
(5, 3, 5, '竹编六角孔果篮体验', '传统编法一次上手', '/api/uploads/demo/course-5.svg',
 '学习竹编最经典的通六角编法，从起底、编织到收口，完成一个实用的小果篮。材料已做防霉处理。',
 '四川省成都市崇州道明竹艺村',
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 3 HOUR,
 148.00, 10, 0, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY),
 180, 1, 760, 10, 2, 1);

-- 6.9 预约数据（覆盖全部 6 种状态，保证 P5 之后每个页面和统计图表都有真实数据）
--     user01(id=2) / user02(id=3) 分别对不同课程预约
INSERT INTO `booking`
(`id`,`booking_no`,`course_id`,`user_id`,`craftsman_id`,`contact_name`,`contact_phone`,`quota`,`remark`,`status`,`checkin_code`,`checkin_time`,`audit_time`,`auditor_id`,`reject_reason`,`cancel_reason`,`cancel_by`,`cancel_time`,`create_time`) VALUES
-- ① 待审核（0）：user02 预约课程 2
(1, 'BK20260901000001', 2, 3, 2, '刘小白', '13800000002', 1, '第一次接触木雕，请多指教', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
-- ② 已通过（1）：user01 预约课程 1（今日可核销）
(2, 'BK20260901000002', 1, 2, 1, '陈爱好', '13800000001', 1, '带一位朋友一起', 1, 'A1B2C3D4', NULL, DATE_SUB(NOW(), INTERVAL 20 HOUR), 11, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 26 HOUR)),
-- ③ 已拒绝（2）：user02 预约课程 1
(3, 'BK20260901000003', 1, 3, 1, '刘小白', '13800000002', 1, NULL, 2, NULL, NULL, DATE_SUB(NOW(), INTERVAL 18 HOUR), 11, '本次课程名额已满，建议报名下一期', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
-- ④ 已取消（3）：user01 预约课程 4 后取消
(4, 'BK20260901000004', 4, 2, 1, '陈爱好', '13800000001', 1, '时间冲突', 3, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 11, NULL, '临时有事，下次再来', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ⑤ 已核销（4）：user01 预约课程 5 并已签到
(5, 'BK20260901000005', 5, 2, 3, '陈爱好', '13800000001', 1, NULL, 4, 'E5F6A7B8', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 10 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
-- ⑥ 已完成（5）：user02 预约课程 5 并完成
(6, 'BK20260901000006', 5, 3, 3, '刘小白', '13800000002', 1, NULL, 5, 'C9D0E1F2', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 5 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY));

-- 6.10 收货地址（演示数据）
INSERT INTO `address` (`user_id`, `receiver`, `phone`, `province`, `city`, `district`, `detail`, `is_default`) VALUES
(2, '陈爱好', '13800000001', '江苏省', '南京市', '鼓楼区', '中山北路 100 号 XX 大厦 1201 室', 1),
(2, '陈爱好', '13800000001', '江苏省', '南京市', '玄武区', '珠江路 200 号 XX 小区 3 栋 502', 0),
(3, '刘小白', '13800000002', '浙江省', '杭州市', '西湖区', '文一西路 500 号 XX 科技园 B 座', 1);

-- 6.11 优惠券（2 张满减 + 1 张折扣）
-- 说明：coupon.received_count 表示「全平台已领张数」，而 user_coupon 只演示了 3 位演示用户的卡包，
--       两者数量不同是正常的（演示环境只有 6 个账号）；article.like_count 同理。
--       article.comment_count 与 article_comment 的行数严格一致（评论只能由用户产生，必须对得上）。
INSERT INTO `coupon` (`id`,`name`,`type`,`threshold`,`amount`,`discount`,`total_count`,`received_count`,`per_limit`,`scope`,`start_time`,`end_time`,`status`,`description`) VALUES
(1, '满 500 减 80 券',   1, 500.00, 80.00, NULL,  1000, 156, 1, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 1, '全场商品通用，单笔订单满 500 元可用'),
(2, '满 1000 减 200 券', 1, 1000.00, 200.00, NULL, 500,  62,  1, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 1, '全场商品通用，单笔订单满 1000 元可用'),
(3, '全场 8.5 折券',     2, 0.00,  NULL,  0.85,  300,  98,  1, 2, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 45 DAY), 1, '仅限商品订单使用，不与其他优惠叠加');

-- 6.12 用户持券（演示）
INSERT INTO `user_coupon` (`user_id`,`coupon_id`,`status`,`expire_time`) VALUES
(2, 1, 0, DATE_ADD(NOW(), INTERVAL 60 DAY)),
(2, 3, 0, DATE_ADD(NOW(), INTERVAL 45 DAY)),
(3, 2, 0, DATE_ADD(NOW(), INTERVAL 60 DAY));

-- 6.13 资讯公告（6 篇）
INSERT INTO `article` (`id`,`title`,`category_id`,`cover`,`summary`,`content`,`author`,`tags`,`view_count`,`like_count`,`comment_count`,`is_top`,`status`) VALUES
(1, '从「重展示」到「重转化」：非遗电商化的三条路径', 1, '/api/uploads/demo/article-1.svg',
 '非遗不能只停留在博物馆的玻璃柜里，如何让手艺人靠手艺体面地生活，是这门生意最本质的问题。',
 '<p>非遗的困境从来不是「没人知道」，而是「知道了也留不下」。</p><p>本文梳理了三条已被验证的转化路径：一是把手艺做成可日常使用的器物，二是把体验做成可预约的服务，三是把匠人做成可被关注的人。</p>',
 '平台编辑部', '非遗,电商,转化', 1286, 87, 2, 1, 1),
(2, '蔚县剪纸：一张纸上的千年刀工', 1, '/api/uploads/demo/article-2.svg',
 '蔚县剪纸并非「剪」，而是「刻」。一把刻刀、一块蜡板，能刻出多少种可能？',
 '<p>蔚县剪纸又叫蔚县刻纸，以「阴刻为主、阳刻为辅」，色彩点染浓艳，构图饱满。</p><p>2006 年，蔚县剪纸被列入第一批国家级非物质文化遗产名录。</p>',
 '张建', '剪纸,蔚县,非遗故事', 942, 63, 0, 0, 1),
(3, '东阳木雕的散点透视：为什么它看起来是「站着的画」', 2, '/api/uploads/demo/article-3.svg',
 '东阳木雕最特别的地方在于它的观看方式——不是从一个点看，而是随着视线移动逐层展开。',
 '<p>散点透视让木雕在有限的深度里呈现出无限的层次，这是东阳木雕区别于西方雕塑语言的核心。</p>',
 '李木', '木雕,东阳,技法', 768, 45, 0, 0, 1),
(4, '柴烧为什么贵：一次窑变的成本账', 3, '/api/uploads/demo/article-4.svg',
 '一窑柴烧的成品率往往不到五成，剩下的去了哪里？',
 '<p>柴烧需要连续烧制 3 到 5 天，松柴消耗以吨计，且成品率极低，这正是它价格高昂的原因。</p>',
 '王陶', '陶艺,柴烧,工艺', 615, 38, 0, 0, 1),
(5, '平台体验课程预约规则说明（2026 年最新）', 1, '/api/uploads/demo/article-5.svg',
 '为了让线下体验更顺畅，平台对预约、取消与核销规则做了统一说明。',
 '<p>1. 预约需至少提前 2 小时提交；2. 开课前 24 小时内不可自行取消；3. 预约通过后凭核销码到现场签到；4. 匠人超过 24 小时未审核，系统将自动拒绝并归还名额。</p>',
 '平台运营', '规则,预约,公告', 1520, 96, 1, 1, 1),
(6, '非遗手作入门怎么选？从一门体验课开始', 4, '/api/uploads/demo/article-6.svg',
 '不知道自己喜欢什么？先花两小时去上一节课，比看一百个视频都管用。',
 '<p>建议从难度为「入门」的课程开始，价格通常在百元出头，且材料与成品都由课程方准备。</p>',
 '平台编辑部', '入门,体验课,新手指南', 1080, 72, 0, 0, 1);

-- 6.14 资讯评论（T2 演示数据）
INSERT INTO `article_comment` (`article_id`,`user_id`,`content`,`parent_id`,`like_count`) VALUES
(1, 2, '写得很实在，非遗确实需要把「用得上」放在第一位。', 0, 12),
(1, 3, '同意楼上，我买过一把竹编篮子，天天在用。', 0, 5),
(5, 2, '规则说明很清晰，收藏了。', 0, 8);

-- 6.15 资讯点赞（T2 演示数据）
INSERT INTO `article_like` (`user_id`,`article_id`) VALUES
(2, 1), (3, 1), (2, 5), (3, 5), (2, 2);

-- 6.16 轮播图（4 张）
INSERT INTO `banner` (`id`,`title`,`image`,`link_url`,`position`,`sort`,`status`) VALUES
(1, '千年非遗，今日可约', '/api/uploads/demo/banner-1.svg', '/courses',      1, 1, 1),
(2, '走进匠人的工作室',   '/api/uploads/demo/banner-2.svg', '/craftsmans',   1, 2, 1),
(3, '手作好物 · 限时满减', '/api/uploads/demo/banner-3.svg', '/goods',        1, 3, 1),
(4, '资讯：非遗电商化的三条路径', '/api/uploads/demo/banner-4.svg', '/articles/1', 1, 4, 1);

-- 6.17 浏览历史（演示数据，含商品与课程）
INSERT INTO `browse_history` (`user_id`,`target_type`,`target_id`,`browse_time`) VALUES
(2, 1, 1,  DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(2, 1, 8,  DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 2, 1,  DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(2, 2, 5,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 18, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 1, 2,  DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 2, 2,  DATE_SUB(NOW(), INTERVAL 6 HOUR));

-- 6.18 站内消息（覆盖全部消息类型）
INSERT INTO `message` (`user_id`,`type`,`title`,`content`,`related_id`,`is_read`) VALUES
(2, 1, '预约审核通过', '你预约的《蔚县剪纸入门体验课》已通过审核，请凭核销码到现场签到。', 2, 0),
(2, 6, '开课提醒',     '你预约的《蔚县剪纸入门体验课》将在 24 小时内开课，请提前 15 分钟到场。', 2, 0),
(3, 1, '预约已被拒绝', '很抱歉，你预约的《蔚县剪纸入门体验课》未通过审核，原因：本次课程名额已满，建议报名下一期。', 3, 1),
(2, 2, '订单已发货',   '你的订单已于今日发出，物流单号 SF1234567890，请注意查收。', NULL, 1),
(2, 4, '优惠券到账',   '恭喜你获得一张「满 500 减 80 券」，可在下单时使用。', 1, 0),
(3, 5, '系统公告',     '平台体验课程预约规则已更新，请查看最新说明。', 5, 0);

-- 6.19 收藏（演示数据）
INSERT INTO `favorite` (`user_id`,`target_type`,`target_id`) VALUES
(2, 1, 4), (2, 1, 12), (2, 2, 3), (3, 1, 8), (3, 3, 1);

-- 6.20 留言反馈（演示数据）
--
-- create_time 必须显式写：处理时长的口径是 handle_time - create_time。
-- 让它取 CURRENT_TIMESTAMP 默认值、而 handle_time 又写成「几天前」的话，
-- 算出来的平均处理时长是负数 —— 页面会显示「平均 -72 小时完成」，
-- 这种数字一眼假，但排查时很容易先去怀疑统计代码而不是种子数据。
INSERT INTO `feedback` (`user_id`,`type`,`content`,`contact`,`reply`,`status`,`handle_time`,`create_time`) VALUES
(2,  1, '建议在课程详情页增加「剩余名额」的实时提醒。', '13800000001', '已采纳，感谢建议，已在最新版本上线。', 2, DATE_SUB(NOW(), INTERVAL 5 DAY) - INTERVAL 12 HOUR, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3,  2, '匠人主页的技艺门类写成了「棕编」，应为「草编」。', '13800000002', '已修正，感谢指正！', 2, DATE_SUB(NOW(), INTERVAL 9 DAY) + INTERVAL 2 HOUR, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(11, 1, '希望课程能支持导出到日历，怕忘记开课时间。', '13900000001', '已支持导出 .ics 日历文件，可在课程详情页下载。', 2, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(12, 3, '有账号疑似恶意占用体验课名额后不到场。', '13900000002', '已核实并限制该账号预约，后续会加强爽约限制。', 2, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3,  3, '有一件木雕商品的图片加载不出来。', '13800000002', NULL, 1, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2,  4, '希望平台能推出年度会员，享受课程折扣。', '13800000001', NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 6.21 评价（商品 / 课程统一表，type 区分）
--     说明：真实评价由 P4 下单完成后（type=1）与 P5 课程核销后（type=2）产生，
--           这里是给前台商品详情页、评分统计、匠人端评价管理用的演示数据。
--     booking_id 仅课程评价且确有对应预约时填写；商品评价的 order_id 见 6.22，
--     order_item.reviewed 已标记，用于验证「已评价」状态回显。
INSERT INTO `review` (`type`,`target_id`,`booking_id`,`user_id`,`rating`,`content`,`images`,`reply`,`reply_time`,`like_count`) VALUES
-- 商品评价（type = 1）
(1, 1,  NULL, 2, 5, '相框做工扎实，剪纸细节比图片上还精致，挂在客厅很显档次。', '["/api/uploads/demo/goods-1.svg"]', '感谢认可！这幅是纯手工刻制的，注意避免阳光直射哦。', DATE_SUB(NOW(), INTERVAL 12 DAY), 8),
(1, 1,  NULL, 3, 4, '整体很满意，就是相框比想象中稍小一点，建议标注尺寸。', NULL, '好的建议，已在详情页补充尺寸标注，感谢反馈！', DATE_SUB(NOW(), INTERVAL 9 DAY), 3),
(1, 2,  NULL, 2, 5, '书签轻薄，四季花鸟四枚各不相同，送朋友很受欢迎。', '["/api/uploads/demo/goods-2.svg"]', NULL, NULL, 12),
(1, 2,  NULL, 3, 5, '做工细致，锦盒也很有质感，性价比高。', NULL, '谢谢喜欢～', DATE_SUB(NOW(), INTERVAL 5 DAY), 6),
(1, 2,  NULL, 2, 4, '挺好的，就是希望能单卖其中某几只。', NULL, NULL, NULL, 1),
(1, 4,  NULL, 3, 5, '笔筒雕工非常细腻，松鹤的羽毛用了多层镂空，握在手里很有分量。', '["/api/uploads/demo/goods-4.svg"]', '多谢赏识，东阳木雕讲究的就是一层层掏出来。', DATE_SUB(NOW(), INTERVAL 20 DAY), 15),
(1, 4,  NULL, 2, 4, '木质纹理漂亮，放在书桌上很稳，物流包装也到位。', NULL, NULL, NULL, 4),
(1, 6,  NULL, 2, 5, '小叶紫檀的料子很正，茶席上一摆就有禅意。', NULL, '谢谢，这是老料，颜色会越用越润。', DATE_SUB(NOW(), INTERVAL 6 DAY), 7),
(1, 8,  NULL, 2, 5, '柴烧的窑变效果确实每只都不同，我收到的这只釉色偏青，非常耐看。', '["/api/uploads/demo/goods-8.svg"]', '柴窑出窑率低，每只都是独一份，感谢喜欢。', DATE_SUB(NOW(), INTERVAL 15 DAY), 21),
(1, 8,  NULL, 3, 5, '杯型握感好，喝茶时手感温润，已回购第二只。', NULL, NULL, NULL, 9),
(1, 10, NULL, 2, 4, '粗陶质感很原生态，配干花特别好看。', NULL, '建议搭配芦苇或尤加利，效果更好。', DATE_SUB(NOW(), INTERVAL 7 DAY), 5),
(1, 10, NULL, 3, 5, '比想象中大一些，正好合适，插花很有味道。', NULL, NULL, NULL, 2),
(1, 11, NULL, 2, 5, '带孩子去体验的，老师很有耐心，成品烧好后快递到家，仪式感满满。', '["/api/uploads/demo/goods-11.svg"]', '欢迎下次再来玩泥巴～', DATE_SUB(NOW(), INTERVAL 4 DAY), 17),
(1, 13, NULL, 3, 5, '手帕的绣工很整齐，并蒂莲配色素雅，送长辈很合适。', NULL, NULL, NULL, 3),
(1, 16, NULL, 2, 4, '三个尺寸很实用，衣柜一下整齐了，草编的味道也很自然。', NULL, NULL, NULL, 6),
(1, 20, NULL, 3, 5, '漆艺杯垫厚实，隔热效果好，四色搭配很雅致。', '["/api/uploads/demo/goods-20.svg"]', '天然大漆需避光保养，日常用软布擦拭即可。', DATE_SUB(NOW(), INTERVAL 11 DAY), 8),
(1, 20, NULL, 2, 4, '颜色比图片略深，但更有质感，满意。', NULL, NULL, NULL, 1),
-- 课程评价（type = 2）
(2, 1,  NULL, 3, 5, '张老师讲得很细，从折纸到下剪一步步带着做，零基础也能出作品。', '["/api/uploads/demo/course-1.svg"]', '感谢参与，勤加练习一定会有进步！', DATE_SUB(NOW(), INTERVAL 3 DAY), 11),
(2, 2,  NULL, 2, 4, '木雕体验节奏偏快，建议新手提前了解下刀具使用。', NULL, '收到，下一期我们会增加 20 分钟基础讲解。', DATE_SUB(NOW(), INTERVAL 2 DAY), 5),
(2, 5,  5,    2, 5, '拉坯比想象中难，但老师一直在旁边指导，最后成品很惊喜。', '["/api/uploads/demo/course-5.svg"]', NULL, NULL, 9),
(2, 5,  6,    3, 5, '第二次来体验了，柴烧出来的效果很特别，值得推荐。', NULL, '谢谢支持，下次可以试试青花分水。', DATE_SUB(NOW(), INTERVAL 1 DAY), 4);

-- 6.22 订单与订单项（覆盖全部 6 种状态 + 退款申请中，供前台「我的订单」与匠人端「订单履约」演示）
--      说明：order_item 里的商品名 / 图片 / 单价是**下单时的快照**，这里直接写死，
--            不随 goods 表变化 —— 这正是订单项快照字段存在的意义。
INSERT INTO `orders`
(`id`,`order_no`,`user_id`,`craftsman_id`,`total_amount`,`discount_amount`,`pay_amount`,`status`,
 `receiver`,`receiver_phone`,`address_snapshot`,`logistics_company`,`logistics_no`,`remark`,
 `refund_reason`,`refund_status`,`pay_time`,`delivery_time`,`finish_time`,`create_time`) VALUES
-- ① 已完成（3）：user01 买张建的剪纸，且已评价（对应 6.21 中 user01 对商品 1 的评价）
(1,'HO202609061030120002100001',2,1,268.00,0.00,268.00,3,
 '陈爱好','13800000001','{"receiver":"陈爱好","phone":"13800000001","province":"江苏省","city":"南京市","district":"鼓楼区","detail":"中山北路 100 号 XX 大厦 1201 室"}',
 '顺丰速运','SF1029384756','请用礼盒包装，谢谢', NULL,0,
 DATE_SUB(NOW(), INTERVAL 12 DAY),DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_SUB(NOW(), INTERVAL 12 DAY)),
-- ② 待付款（0）：user01 买了柴烧茶盏，还没付款（可演示支付 / 取消）
(2,'HO202609171030120002100002',2,3,380.00,0.00,380.00,0,
 '陈爱好','13800000001','{"receiver":"陈爱好","phone":"13800000001","province":"江苏省","city":"南京市","district":"鼓楼区","detail":"中山北路 100 号 XX 大厦 1201 室"}',
 NULL,NULL,NULL, NULL,0, NULL,NULL,NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- ③ 待发货（1）：user02 买的木雕笔筒，已付款等匠人发货
(3,'HO202609161030120003100003',3,2,1280.00,0.00,1280.00,1,
 '刘小白','13800000002','{"receiver":"刘小白","phone":"13800000002","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文一西路 500 号 XX 科技园 B 座"}',
 NULL,NULL,'工作日上午送达更方便', NULL,0,
 DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- ④ 待收货（2）：user01 的木雕茶则已发货，路上
(4,'HO202609141030120002100004',2,2,460.00,0.00,460.00,2,
 '陈爱好','13800000001','{"receiver":"陈爱好","phone":"13800000001","province":"江苏省","city":"南京市","district":"玄武区","detail":"珠江路 200 号 XX 小区 3 栋 502"}',
 '中通快递','ZT8899001122',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ⑤ 已退款（4）：user02 的陶艺体验套票，退款已通过（refund_status=2）
(5,'HO202609121030120003100005',3,3,168.00,0.00,168.00,4,
 '刘小白','13800000002','{"receiver":"刘小白","phone":"13800000002","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文一西路 500 号 XX 科技园 B 座"}',
 NULL,NULL,NULL,'时间冲突，无法参加体验',2,
 DATE_SUB(NOW(), INTERVAL 5 DAY),NULL,NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ⑥ 已取消（5）：user01 下单后未支付，自行取消
(6,'HO202609131030120002100006',2,1,168.00,0.00,168.00,5,
 '陈爱好','13800000001','{"receiver":"陈爱好","phone":"13800000001","province":"江苏省","city":"南京市","district":"鼓楼区","detail":"中山北路 100 号 XX 大厦 1201 室"}',
 NULL,NULL,NULL, NULL,0, NULL,NULL,NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ⑦ 待发货 + 退款申请中（refund_status=1）：两个商品项，用于演示「一单多商品」与匠人端退款审核
(7,'HO202609171030120003100007',3,1,356.00,0.00,356.00,1,
 '刘小白','13800000002','{"receiver":"刘小白","phone":"13800000002","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文一西路 500 号 XX 科技园 B 座"}',
 NULL,NULL,NULL,'临时出差，收货地址有变动，想先退款',1,
 DATE_SUB(NOW(), INTERVAL 20 HOUR),NULL,NULL, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
-- ⑧ 已完成（3）：user01 买的漆艺杯垫（已评价，对应 6.21 中 user01 对商品 20 的评价）
(8,'HO202609091030120002100008',2,3,336.00,0.00,336.00,3,
 '陈爱好','13800000001','{"receiver":"陈爱好","phone":"13800000001","province":"江苏省","city":"南京市","district":"鼓楼区","detail":"中山北路 100 号 XX 大厦 1201 室"}',
 '圆通速递','YT5566778899',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY));

-- 6.23 订单项（商品快照；reviewed=1 表示该订单项已评价过）
INSERT INTO `order_item`
(`order_id`,`order_no`,`goods_id`,`spec_id`,`goods_name`,`goods_image`,`spec_name`,`price`,`quantity`,`reviewed`) VALUES
(1,'HO202609061030120002100001',1,NULL,'蔚县手工剪纸·喜上眉梢','/api/uploads/demo/goods-1.svg',NULL,268.00,1,1),
(2,'HO202609171030120002100002',8,5,'柴烧茶盏·天青釉','/api/uploads/demo/goods-8.svg','单只装',380.00,1,0),
(3,'HO202609161030120003100003',4,NULL,'东阳木雕·松鹤笔筒','/api/uploads/demo/goods-4.svg',NULL,1280.00,1,0),
(4,'HO202609141030120002100004',6,NULL,'木雕茶则·禅意荷塘','/api/uploads/demo/goods-6.svg',NULL,460.00,1,0),
(5,'HO202609121030120003100005',11,NULL,'陶艺体验套票·亲子课','/api/uploads/demo/goods-11.svg',NULL,168.00,1,0),
(6,'HO202609131030120002100006',13,NULL,'刺绣手帕·并蒂莲（2条）','/api/uploads/demo/goods-13.svg',NULL,168.00,1,0),
(7,'HO202609171030120003100007',1,NULL,'蔚县手工剪纸·喜上眉梢','/api/uploads/demo/goods-1.svg',NULL,268.00,1,0),
(7,'HO202609171030120003100007',2,NULL,'剪纸书签·四季花鸟（4枚装）','/api/uploads/demo/goods-2.svg',NULL,88.00,1,0),
(8,'HO202609091030120002100008',20,NULL,'漆艺杯垫·四色一组','/api/uploads/demo/goods-20.svg',NULL,168.00,2,1);

-- 6.24 匠人关注（P5 新增演示数据）
--     user01(id=2) 关注了张建(1)与王陶(3)；user02(id=3) 关注了李木(2)
--     匠人表的 follower_count 与这里的条数保持一致（演示数据自洽）
INSERT INTO `craftsman_follow` (`user_id`, `craftsman_id`, `deleted`) VALUES
(2, 1, 0),
(2, 3, 0),
(3, 2, 0);

-- =====================================================================
--  6.25 P7 匠人端看板补充演示数据
-- =====================================================================
--  为什么 P7 要补这一批数据：P7 的验收标准里有一条「数据看板所有图表都有真实数据
--  （初始化数据要能撑起来）」。P2 的种子数据只有 5 门课 / 6 条预约 / 8 笔订单，
--  直接打开看板会有三处空洞：
--    ① 近 7 天销售趋势只有零星几个非零点，折线几乎贴着横轴；
--    ② 每位匠人手里都没有「已结束的课程」，核销率算不出来（分子分母同时是 0）；
--    ③ 只有 2 位演示用户，预约热度排行的柱子高矮几乎一样。
--
--  补数据的过程中发现并必须守住三条既有约束，否则脚本直接跑不通、
--  或者数据看着能跑但其实自相矛盾：
--
--  约束一：uk_user_course(user_id, course_id) 唯一键（也是本表最容易被忽略的约束）
--    booking 表上有这个唯一键——它是三层防超卖的第三层，
--    保证「同一用户同一课程」在全表只能存在一条记录，
--    连已取消 / 已拒绝的记录同样占着这个坑（因为唯一键不含 status、也不含 deleted）。
--    只有 2 位演示用户时，每门课最多只能挂 2 条预约，
--    而这批课程要撑起「热度排行」至少得有五六条。
--    → 所以 6.25.1 必须先补 8 位演示用户。不是为了让用户数好看，
--      而是**不给足人，就无法在不违反唯一键的前提下把课程的预约量堆起来**。
--
--  约束二：remain_quota = total_quota − Σ(占用名额)
--    「占用名额」= 状态 0待审核 / 1已通过 / 4已核销 / 5已完成；
--    状态 2已拒绝、3已取消 都已归还名额，不计入。
--    课程 1~5 的 remain_quota 在 6.5 里已经写好了（10/3、8/6、15/15、12/9、10/0），
--    本节补充的预约必须**恰好**把这个差额补齐。
--    每一段前面都写了核算注释，补完两者严丝合缝；
--    看板上的「名额使用率」就是靠这条等式成立的。
--
--  约束三：核销只能出现在已经结束的课程上
--    没开课就先「已核销」在业务上不成立。因此本节只给课程 5 / 9 / 10
--    （结束时间分别在 6 / 3 / 4 天前）写核销记录，
--    其余课程一律老老实实停在「待审核 / 已通过」——
--    这也让「已通过未核销」这个指标有了真实含义，而不是被伪造的核销记录冲淡。

-- 6.25.1 补充演示用户（8 位，id 14~21）
--   统一规则：普通用户角色、启用状态、手机号 13800000003~13800000010。
--   密码沿用与 6.2 完全相同的 BCrypt 哈希（明文同为 123456），
--   这样演示时随便挑哪个账号都能登录。
--   id 从 14 开始是为了避开 11~13（三位匠人的登录账号）。
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `gender`, `role`, `status`) VALUES
(14, 'linxi',   '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '林溪',   '/api/uploads/demo/avatar-user3.svg',  '13800000003', 'linxi@heritage.com',   2, 1, 1),
(15, 'zhouyi',  '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '周屹',   '/api/uploads/demo/avatar-user4.svg',  '13800000004', 'zhouyi@heritage.com',  1, 1, 1),
(16, 'suqing',  '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '苏晴',   '/api/uploads/demo/avatar-user5.svg',  '13800000005', 'suqing@heritage.com',  2, 1, 1),
(17, 'heyan',   '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '何砚',   '/api/uploads/demo/avatar-user6.svg',  '13800000006', 'heyan@heritage.com',   1, 1, 1),
(18, 'duanmu',  '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '段沐',   '/api/uploads/demo/avatar-user7.svg',  '13800000007', 'duanmu@heritage.com',  1, 1, 1),
(19, 'qinyue',  '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '秦悦',   '/api/uploads/demo/avatar-user8.svg',  '13800000008', 'qinyue@heritage.com',  2, 1, 1),
(20, 'luozhen', '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '罗甄',   '/api/uploads/demo/avatar-user9.svg',  '13800000009', 'luozhen@heritage.com', 2, 1, 1),
(21, 'xiamin',  '$2a$10$RPYYRSOClD49Y3mSP4VKm.bs1JD6rI.LIlcHPZicalC2PMl.NacRK', '夏敏',   '/api/uploads/demo/avatar-user10.svg', '13800000010', 'xiamin@heritage.com',  1, 1, 1);

-- 6.25.2 补充课程（5 门，id 6~10）
--   分布设计：3 门待开课（撑起匠人端日历排期），2 门已结束（撑起核销率）。
--   每位匠人都至少分到一门待开课 + 一门已结束，避免某个匠人打开看板是空的：
--     待开课：课程 6（张建·2 天后）、课程 7（李木·3 天后）、课程 8（王陶·8 天后）
--     已结束：课程 9（张建·3 天前）、课程 10（李木·4 天前）
--   王陶的已结束课程沿用 6.5 里的课程 5（6 天前结束）。
INSERT INTO `course`
(`id`,`craftsman_id`,`category_id`,`title`,`subtitle`,`cover`,`description`,`address`,`start_time`,`end_time`,`price`,`total_quota`,`remain_quota`,`booking_deadline`,`cancel_deadline`,`duration`,`difficulty`,`view_count`,`booking_count`,`status`,`audit_status`) VALUES
-- 课程 6：张建 · 待开课（2 天后）；总 12 / 余 9 → 待补占用 3
(6, 1, 1, '剪纸团花进阶课', '从单幅到团花的构图训练', '/api/uploads/demo/course-6.svg',
 '在入门课基础上练习团花构图与多层套色，完成一幅直径 30cm 的团花作品。建议先上过入门体验课。',
 '河北省张家口市蔚县非遗工坊 3 楼研习室',
 DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 4 HOUR,
 228.00, 12, 9, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
 240, 2, 640, 3, 0, 1),
-- 课程 7：李木 · 待开课（3 天后）；总 6 / 余 4 → 待补占用 2
(7, 2, 2, '木雕圆雕入门', '从平面走向立体的第一课', '/api/uploads/demo/course-7.svg',
 '讲解圆雕的观察方法与取料原则，用一块黄杨木完成一件小型圆雕摆件，含全部材料与工具。',
 '浙江省金华市东阳市木雕小镇研习所',
 DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY) + INTERVAL 5 HOUR,
 388.00, 6, 4, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
 300, 3, 520, 2, 0, 1),
-- 课程 8：王陶 · 待开课（8 天后）；总 20 / 余 17 → 待补占用 3
(8, 3, 3, '柴烧茶器鉴赏课', '看懂一只柴烧盏的门道', '/api/uploads/demo/course-8.svg',
 '从窑位、落灰、釉面三个角度认识柴烧茶器，含茶席实操与品鉴环节，实物上手不比划。',
 '江西省景德镇市陶溪川文创街区 A 区',
 DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY) + INTERVAL 3 HOUR,
 168.00, 20, 17, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 180, 1, 880, 3, 0, 1),
-- 课程 9：张建 · 已结束（3 天前）；总 12 / 余 8 → 待补占用 4
(9, 1, 1, '剪纸装裱与收藏讲座', '作品如何保存二十年不变形', '/api/uploads/demo/course-9.svg',
 '讲装裱材料的选择、防潮防虫处理与长期保存要点，可携带自己的作品现场咨询。',
 '河北省张家口市蔚县非遗工坊 2 楼体验区',
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 3 HOUR,
 98.00, 12, 8, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
 180, 1, 460, 4, 2, 1),
-- 课程 10：李木 · 已结束（4 天前）；总 10 / 余 6 → 待补占用 4
(10, 2, 2, '木雕打磨与上蜡实操', '决定成品手感的最后一步', '/api/uploads/demo/course-10.svg',
 '实操课：从 240 目到 2000 目的逐级打磨，以及天然蜡的选择与上蜡手法。自带半成品亦可。',
 '浙江省金华市东阳市木雕小镇研习所',
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 4 HOUR,
 218.00, 10, 6, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
 240, 2, 380, 4, 2, 1);

-- 6.25.3 补充商品（2 件低库存）
--   库存预警列表要能同时演示「告急(low)」与「售罄(out)」两档，
--   而 6.6 的种子商品里库存最低的是 5（刚好卡在阈值上），没有库存为 0 的商品，
--   导致 out 这一档在页面上永远出不来。这里补两件：一件 2 件、一件 0 件。
--   两位匠人各分到一件，保证三家铺子的库存预警待办数都不为 0。
INSERT INTO `goods` (`id`, `craftsman_id`, `category_id`, `name`, `subtitle`, `cover`, `description`, `price`, `original_price`, `stock`, `sales`, `view_count`, `status`, `audit_status`) VALUES
(21, 1, 1, '剪纸灯罩·花开富贵', '透光成影，一盏一景', '/api/uploads/demo/goods-21.svg', '以蔚县刻纸技法制作的灯罩，开灯后花纹投影在墙面，光影随角度变化，余量极少。', 360.00, 460.00, 2, 30, 410, 1, 1),
(22, 3, 3, '柴烧花器·落灰釉', '一窑一色，不可复制', '/api/uploads/demo/goods-22.svg', '柴窑烧制的落灰釉花器，釉面随窑位自然流淌，每一只都不同，本期已售罄。', 680.00, 880.00, 0, 26, 350, 1, 1);

-- 6.25.4 补充预约（33 条，id 7~39）
--   严格按「约束一」配对：每条 (user_id, course_id) 在全表唯一。
--   课程 1~5 的补量核算（差额 = 6.5 的 remain_quota 反推）：
--     课程 1  总10 余3 → 占用 7，已有 1（b2 用户2） → 补 6
--     课程 2  总8  余6 → 占用 2，已有 1（b1 用户3） → 补 1
--     课程 3  总15 余15 → 占用 0，已有 0              → 补 0（这条最容易写错：它是满额待约的课）
--     课程 4  总12 余9 → 占用 3，已有 0（b4 已取消→归还） → 补 3
--     课程 5  总10 余0 → 占用 10，已有 2（b5 已核销、b6 已完成） → 补 8
--   课程 6~10 的补量核算：
--     课程 6 总12 余9 → 补 3    课程 7 总6 余4 → 补 2
--     课程 8 总20 余17 → 补 3   课程 9 总12 余8 → 补 4
--     课程 10 总10 余6 → 补 4
--   合计 33 条，与下面列出的条数一致。
--   已结束课程（5/9/10）的到场与爽约分布，决定了看板上的核销率：
--     课程 5  到场 7（b5+b6+b20~b24）、爽约 3（b17~b19）
--     课程 9  到场 3（b32~b34）、爽约 1（b35）
--     课程 10 到场 4（b36~b39）、爽约 0
--     → 全平台核销率 = 14 ÷ (14+4) = 77.8%
INSERT INTO `booking`
(`id`,`booking_no`,`course_id`,`user_id`,`craftsman_id`,`contact_name`,`contact_phone`,`quota`,`remark`,`status`,`checkin_code`,`checkin_time`,`audit_time`,`auditor_id`,`reject_reason`,`cancel_reason`,`cancel_by`,`cancel_time`,`create_time`) VALUES
-- 课程 1（张建，待开课）：补 6 个名额 → 待审 2 条 + 已通过 4 条
(7,  'BK20260901000007', 1, 14, 1, '林溪', '13800000003', 1, '第一次体验剪纸',        0, NULL,       NULL, NULL,                                 NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(8,  'BK20260901000008', 1, 15, 1, '周屹', '13800000004', 1, NULL,                    0, NULL,       NULL, NULL,                                 NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(9,  'BK20260901000009', 1, 16, 1, '苏晴', '13800000005', 1, NULL,                    1, 'P83A4B5C', NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR),     11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(10, 'BK20260901000010', 1, 17, 1, '何砚', '13800000006', 1, '想带孩子一起',          1, 'P84A5B6C', NULL, DATE_SUB(NOW(), INTERVAL 20 HOUR),    11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 'BK20260901000011', 1, 18, 1, '段沐', '13800000007', 1, NULL,                    1, 'P85A6B7C', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY),      11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(12, 'BK20260901000012', 1, 19, 1, '秦悦', '13800000008', 1, NULL,                    1, 'P86A7B8C', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY),      11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 课程 2（李木，待开课）：补 1 个名额 → 已通过 1 条
(13, 'BK20260901000013', 2, 14, 2, '林溪', '13800000003', 1, NULL,                    1, 'P87A8B9C', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY),      12,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- 课程 3（王陶，待开课）：remain_quota 已经是满额 15，占用必须为 0，故一条都不补
-- 课程 4（张建，待开课）：补 3 个名额 → 待审 1 条 + 已通过 2 条
(14, 'BK20260901000014', 4, 14, 1, '林溪', '13800000003', 1, NULL,                    0, NULL,       NULL, NULL,                                 NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(15, 'BK20260901000015', 4, 15, 1, '周屹', '13800000004', 1, '想学绣一朵梅花',        1, 'P88A9B0C', NULL, DATE_SUB(NOW(), INTERVAL 10 HOUR),    11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(16, 'BK20260901000016', 4, 16, 1, '苏晴', '13800000005', 1, NULL,                    1, 'P89A0B1C', NULL, DATE_SUB(NOW(), INTERVAL 10 HOUR),    11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 课程 5（王陶，已结束 6 天）：补 8 个名额 → 爽约 3 条（仍为已通过）+ 到场 5 条（已完成）
(17, 'BK20260901000017', 5, 14, 3, '林溪', '13800000003', 1, NULL,                    1, 'P95A6B7C', NULL,                              DATE_SUB(NOW(), INTERVAL 8 DAY),  13,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(18, 'BK20260901000018', 5, 15, 3, '周屹', '13800000004', 1, NULL,                    1, 'P96A7B8C', NULL,                              DATE_SUB(NOW(), INTERVAL 8 DAY),  13,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(19, 'BK20260901000019', 5, 16, 3, '苏晴', '13800000005', 1, '临时有事，尽量赶',      1, 'P97A8B9C', NULL,                              DATE_SUB(NOW(), INTERVAL 8 DAY),  13,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(20, 'BK20260901000020', 5, 17, 3, '何砚', '13800000006', 1, NULL,                    5, 'P71A2B3C', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 10 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(21, 'BK20260901000021', 5, 18, 3, '段沐', '13800000007', 1, NULL,                    5, 'P72A3B4C', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 12 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(22, 'BK20260901000022', 5, 19, 3, '秦悦', '13800000008', 1, NULL,                    5, 'P73A4B5C', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 15 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(23, 'BK20260901000023', 5, 20, 3, '罗甄', '13800000009', 1, NULL,                    5, 'P74A5B6C', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 18 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(24, 'BK20260901000024', 5, 21, 3, '夏敏', '13800000010', 1, NULL,                    5, 'P75A6B7C', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 21 MINUTE, DATE_SUB(NOW(), INTERVAL 8 DAY), 13, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
-- 课程 6（张建，待开课 2 天后）：补 3 个名额 → 待审 1 条 + 已通过 2 条
(25, 'BK20260901000025', 6, 14, 1, '林溪', '13800000003', 1, '已上过入门课',          0, NULL,       NULL, NULL,                              NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(26, 'BK20260901000026', 6, 15, 1, '周屹', '13800000004', 1, NULL,                    1, 'P90A1B2C', NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR),     11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(27, 'BK20260901000027', 6, 16, 1, '苏晴', '13800000005', 1, NULL,                    1, 'P91A2B3C', NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR),     11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 课程 7（李木，待开课 3 天后）：补 2 个名额 → 已通过 2 条
(28, 'BK20260901000028', 7, 14, 2, '林溪', '13800000003', 1, NULL,                    1, 'P92A3B4C', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY),      12,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(29, 'BK20260901000029', 7, 15, 2, '周屹', '13800000004', 1, '想自己做个摆件',        1, 'P93A4B5C', NULL, DATE_SUB(NOW(), INTERVAL 10 HOUR),    12,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 课程 8（王陶，待开课 8 天后）：补 3 个名额 → 待审 1 条(占 2 名额) + 已通过 1 条
(30, 'BK20260901000030', 8, 14, 3, '林溪', '13800000003', 2, '想看看实物再买',        0, NULL,       NULL, NULL,                              NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(31, 'BK20260901000031', 8, 15, 3, '周屹', '13800000004', 1, NULL,                    1, 'P94A5B6C', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY),      13,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 课程 9（张建，已结束 3 天）：补 4 个名额 → 到场 3 条（已完成）+ 爽约 1 条（已通过）
(32, 'BK20260901000032', 9, 14, 1, '林溪', '13800000003', 1, NULL,                    5, 'P76A7B8C', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 8 MINUTE,  DATE_SUB(NOW(), INTERVAL 5 DAY), 11, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(33, 'BK20260901000033', 9, 15, 1, '周屹', '13800000004', 1, NULL,                    5, 'P77A8B9C', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 12 MINUTE, DATE_SUB(NOW(), INTERVAL 5 DAY), 11, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(34, 'BK20260901000034', 9, 16, 1, '苏晴', '13800000005', 1, '想带作品去请老师看看',  5, 'P78A9B0C', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 16 MINUTE, DATE_SUB(NOW(), INTERVAL 5 DAY), 11, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(35, 'BK20260901000035', 9, 17, 1, '何砚', '13800000006', 1, NULL,                    1, 'P98A9B0C', NULL,                              DATE_SUB(NOW(), INTERVAL 5 DAY),  11,   NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
-- 课程 10（李木，已结束 4 天）：补 4 个名额 → 到场 4 条（2 条已完成 + 2 条已核销），无人爽约
(36, 'BK20260901000036', 10, 14, 2, '林溪', '13800000003', 1, NULL,                   5, 'P79A0B1C', DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 9 MINUTE,  DATE_SUB(NOW(), INTERVAL 6 DAY), 12, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(37, 'BK20260901000037', 10, 15, 2, '周屹', '13800000004', 1, NULL,                   5, 'P80A1B2C', DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 12 MINUTE, DATE_SUB(NOW(), INTERVAL 6 DAY), 12, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(38, 'BK20260901000038', 10, 18, 2, '段沐', '13800000007', 1, '自带半成品',           4, 'P81A2B3C', DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 15 MINUTE, DATE_SUB(NOW(), INTERVAL 6 DAY), 12, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(39, 'BK20260901000039', 10, 19, 2, '秦悦', '13800000008', 1, NULL,                   4, 'P82A3B4C', DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 18 MINUTE, DATE_SUB(NOW(), INTERVAL 6 DAY), 12, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY));

-- 6.25.5 补充订单（11 笔，id 9~19）
--   9~17 用于铺满近 7 天：原本窗口里只有 3 个非零日，折线几乎贴着横轴。
--     每位匠人在 7 天窗口内各安排 3 笔已支付订单，且保证每一天都有单，
--     折线才会连续成形状而不是断续的点。
--   18~19 用于补「待退款申请」这个待办：
--     原种子只有订单 7（张建）带退款申请，李木与王陶的匠人后台
--     「待退款」永远是 0，验收时看不出这一项是活的。
--   另外订单 15/16/18 刻意留成「待发货」，让三位匠人的待发货待办都不为 0。
INSERT INTO `orders`
(`id`,`order_no`,`user_id`,`craftsman_id`,`total_amount`,`discount_amount`,`pay_amount`,`status`,
 `receiver`,`receiver_phone`,`address_snapshot`,`logistics_company`,`logistics_no`,`remark`,
 `refund_reason`,`refund_status`,`pay_time`,`delivery_time`,`finish_time`,`create_time`) VALUES
(9, 'HO202609121030120002100009',14,1,176.00,0.00,176.00,3,
 '林溪','13800000003','{"receiver":"林溪","phone":"13800000003","province":"江苏省","city":"南京市","district":"鼓楼区","detail":"中山北路 100 号 XX 大厦 1201 室"}',
 '顺丰速运','SF3344556677',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10,'HO202609121030120003100010',15,2,460.00,0.00,460.00,3,
 '周屹','13800000004','{"receiver":"周屹","phone":"13800000004","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文一西路 500 号 XX 科技园 B 座"}',
 '中通快递','ZT3344556677',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(11,'HO202609131030120003100011',16,3,560.00,0.00,560.00,3,
 '苏晴','13800000005','{"receiver":"苏晴","phone":"13800000005","province":"江西省","city":"景德镇市","district":"珠山区","detail":"陶溪川文创街区 B 区 3 栋 201"}',
 '圆通速递','YT3344556677',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(12,'HO202609141030120002100012',17,1,504.00,0.00,504.00,3,
 '何砚','13800000006','{"receiver":"何砚","phone":"13800000006","province":"河北省","city":"石家庄市","district":"桥西区","detail":"中山西路 88 号 XX 苑 6 栋 1201"}',
 '顺丰速运','SF4455667788',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(13,'HO202609141030120002100013',18,2,396.00,0.00,396.00,3,
 '段沐','13800000007','{"receiver":"段沐","phone":"13800000007","province":"四川省","city":"成都市","district":"青羊区","detail":"人民中路二段 12 号 XX 花园 3 栋 502"}',
 '中通快递','ZT4455667788',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(14,'HO202609151030120003100014',19,3,336.00,0.00,336.00,3,
 '秦悦','13800000008','{"receiver":"秦悦","phone":"13800000008","province":"广东省","city":"广州市","district":"越秀区","detail":"东风东路 700 号 XX 大厦 1802"}',
 '圆通速递','YT4455667788',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(15,'HO202609161030120002100015',20,1,1580.00,0.00,1580.00,1,
 '罗甄','13800000009','{"receiver":"罗甄","phone":"13800000009","province":"上海市","city":"上海市","district":"徐汇区","detail":"淮海中路 1010 号 XX 广场 A 座 2501"}',
 NULL,NULL,'送人的，麻烦包装仔细一些', NULL,0,
 DATE_SUB(NOW(), INTERVAL 2 DAY),NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(16,'HO202609171030120003100016',21,2,1680.00,0.00,1680.00,1,
 '夏敏','13800000010','{"receiver":"夏敏","phone":"13800000010","province":"山东省","city":"济南市","district":"历下区","detail":"经十路 9999 号 XX 中心 12 层"}',
 NULL,NULL,NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17,'HO202609181030120002100017',14,3,660.00,0.00,660.00,3,
 '林溪','13800000003','{"receiver":"林溪","phone":"13800000003","province":"江苏省","city":"南京市","district":"玄武区","detail":"珠江路 200 号 XX 小区 3 栋 502"}',
 '圆通速递','YT5566778899',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 8 HOUR),DATE_SUB(NOW(), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 2 HOUR),DATE_SUB(NOW(), INTERVAL 8 HOUR)),
-- 18：王陶的待发货订单 + 退款申请中（让王陶的「待发货」与「待退款」两项都不为 0）
(18,'HO202609181030120003100018',15,3,440.00,0.00,440.00,1,
 '周屹','13800000004','{"receiver":"周屹","phone":"13800000004","province":"浙江省","city":"杭州市","district":"滨江区","detail":"江南大道 3688 号 XX 大厦 9 层"}',
 NULL,NULL,'能否换一只釉色深一些的', '看到实物后觉得与预期釉色差别较大，希望退款',1,
 DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 19：李木的待收货订单 + 退款申请中（货物已在途中，属于「签收前反悔」的典型场景）
(19,'HO202609181030120002100019',16,2,460.00,0.00,460.00,2,
 '苏晴','13800000005','{"receiver":"苏晴","phone":"13800000005","province":"江西省","city":"景德镇市","district":"昌江区","detail":"瓷都大道 55 号 XX 公寓 7 栋 801"}',
 '顺丰速运','SF6677889900',NULL, '物流太慢，不想等了',1,
 DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 6.25.6 补充订单（8 笔，id 20~27）
--   为什么 6.25.5 还不够：上面那批是「平台上每天都有单」，
--   但看板上的趋势图是**按匠人各画一条**的——张建在 9/13、9/15、9/18 没有单，
--   折线就会在这三天扎到 0 再弹起来，七天的窗口里出现三个尖坑，
--   看起来像数据断了而不是「这几天确实没生意」。
--   所以这里按「每位匠人在近 7 天里每天都有单」补齐：
--     张建缺 9/13、9/15、9/18 → 订单 20、21、22
--     李木缺 9/13、9/18       → 订单 23、24
--     王陶缺 9/12、9/14、9/16 → 订单 25、26、27
--   补完之后三位匠人的折线都是完整的 7 个点，且高低起伏不同——
--   如果每天都补成同样的金额，三条线会变成三条水平直线，那比有缺口更假。
--   金额刻意错开（352 ~ 1440），保证折线有形状。
--   状态只用「已完成 / 待收货」两种：这两个状态都不进待办提醒，
--   因此补数据不会把 6.25.5 里那几项待办数字带偏。
INSERT INTO `orders`
(`id`,`order_no`,`user_id`,`craftsman_id`,`total_amount`,`discount_amount`,`pay_amount`,`status`,
 `receiver`,`receiver_phone`,`address_snapshot`,`logistics_company`,`logistics_no`,`remark`,
 `refund_reason`,`refund_status`,`pay_time`,`delivery_time`,`finish_time`,`create_time`) VALUES
(20,'HO202609131030120002100020',20,1,880.00,0.00,880.00,3,
 '罗甄','13800000009','{"receiver":"罗甄","phone":"13800000009","province":"上海市","city":"上海市","district":"徐汇区","detail":"淮海中路 1010 号 XX 广场 A 座 2501"}',
 '顺丰速运','SF7788990011',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(21,'HO202609151030120002100021',16,1,352.00,0.00,352.00,3,
 '苏晴','13800000005','{"receiver":"苏晴","phone":"13800000005","province":"江西省","city":"景德镇市","district":"珠山区","detail":"陶溪川文创街区 B 区 3 栋 201"}',
 '中通快递','ZT7788990011',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(22,'HO202609181030120002100022',17,1,1280.00,0.00,1280.00,2,
 '何砚','13800000006','{"receiver":"何砚","phone":"13800000006","province":"河北省","city":"石家庄市","district":"桥西区","detail":"中山西路 88 号 XX 苑 6 栋 1201"}',
 '顺丰速运','SF8899001122',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 4 HOUR),DATE_SUB(NOW(), INTERVAL 2 HOUR),NULL,DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(23,'HO202609131030120003100023',19,2,396.00,0.00,396.00,3,
 '秦悦','13800000008','{"receiver":"秦悦","phone":"13800000008","province":"广东省","city":"广州市","district":"越秀区","detail":"东风东路 700 号 XX 大厦 1802"}',
 '圆通速递','YT7788990011',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(24,'HO202609181030120003100024',20,2,1440.00,0.00,1440.00,2,
 '罗甄','13800000009','{"receiver":"罗甄","phone":"13800000009","province":"上海市","city":"上海市","district":"徐汇区","detail":"淮海中路 1010 号 XX 广场 A 座 2501"}',
 '中通快递','ZT8899001122',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 3 HOUR),DATE_SUB(NOW(), INTERVAL 1 HOUR),NULL,DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(25,'HO202609121030120003100025',16,3,840.00,0.00,840.00,3,
 '苏晴','13800000005','{"receiver":"苏晴","phone":"13800000005","province":"江西省","city":"景德镇市","district":"珠山区","detail":"陶溪川文创街区 B 区 3 栋 201"}',
 '圆通速递','YT9900112233',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(26,'HO202609141030120003100026',18,3,760.00,0.00,760.00,3,
 '段沐','13800000007','{"receiver":"段沐","phone":"13800000007","province":"四川省","city":"成都市","district":"青羊区","detail":"人民中路二段 12 号 XX 花园 3 栋 502"}',
 '顺丰速运','SF9900112233',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(27,'HO202609161030120003100027',21,3,680.00,0.00,680.00,3,
 '夏敏','13800000010','{"receiver":"夏敏","phone":"13800000010","province":"山东省","city":"济南市","district":"历下区","detail":"经十路 9999 号 XX 中心 12 层"}',
 '圆通速递','YT1122334455',NULL, NULL,0,
 DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 6.25.7 补充订单项（与 6.25.5、6.25.6 的订单一一对应）
--   商品名 / 图片 / 单价都是下单那一刻的快照，刻意写死：
--   之后商品改名改价，历史订单里显示的仍然是当时的样子（这正是快照字段的意义）。
--   商品与订单的 craftsman_id 保持一致，避免出现「A 匠人的订单里躺着 B 匠人的货」。
INSERT INTO `order_item`
(`order_id`,`order_no`,`goods_id`,`spec_id`,`goods_name`,`goods_image`,`spec_name`,`price`,`quantity`,`reviewed`) VALUES
(9, 'HO202609121030120002100009',2, NULL,'剪纸书签·四季花鸟（4枚装）','/api/uploads/demo/goods-2.svg',NULL,88.00,2,0),
(10,'HO202609121030120003100010',6, NULL,'木雕茶则·禅意荷塘','/api/uploads/demo/goods-6.svg',NULL,460.00,1,0),
(11,'HO202609131030120003100011',9, NULL,'青花分水·缠枝莲盖碗','/api/uploads/demo/goods-9.svg',NULL,560.00,1,0),
(12,'HO202609141030120002100012',13,NULL,'刺绣手帕·并蒂莲（2条）','/api/uploads/demo/goods-13.svg',NULL,168.00,3,0),
(13,'HO202609141030120002100013',15,NULL,'竹编提篮·六角孔','/api/uploads/demo/goods-15.svg',NULL,198.00,2,0),
(14,'HO202609151030120003100014',11,NULL,'陶艺体验套票·亲子课','/api/uploads/demo/goods-11.svg',NULL,168.00,2,0),
(15,'HO202609161030120002100015',12,NULL,'苏绣团扇·双面绣猫','/api/uploads/demo/goods-12.svg',NULL,1580.00,1,0),
(16,'HO202609171030120003100016',19,NULL,'漆器茶盘·大漆螺钿','/api/uploads/demo/goods-19.svg',NULL,1680.00,1,0),
(17,'HO202609181030120002100017',10,NULL,'手作陶花瓶·粗陶素坯','/api/uploads/demo/goods-10.svg',NULL,220.00,3,0),
(18,'HO202609181030120003100018',10,NULL,'手作陶花瓶·粗陶素坯','/api/uploads/demo/goods-10.svg',NULL,220.00,2,0),
(19,'HO202609181030120002100019',6, NULL,'木雕茶则·禅意荷塘','/api/uploads/demo/goods-6.svg',NULL,460.00,1,0),
(20,'HO202609131030120002100020',18,NULL,'平遥推光漆器·首饰盒','/api/uploads/demo/goods-18.svg',NULL,880.00,1,0),
(21,'HO202609151030120002100021',2, NULL,'剪纸书签·四季花鸟（4枚装）','/api/uploads/demo/goods-2.svg',NULL,88.00,4,0),
(22,'HO202609181030120002100022',14,NULL,'苗绣背扇·蝶恋花','/api/uploads/demo/goods-14.svg',NULL,1280.00,1,0),
(23,'HO202609131030120003100023',15,NULL,'竹编提篮·六角孔','/api/uploads/demo/goods-15.svg',NULL,198.00,2,0),
(24,'HO202609181030120003100024',17,NULL,'柳编花器·落地款','/api/uploads/demo/goods-17.svg',NULL,480.00,3,0),
(25,'HO202609121030120003100025',20,NULL,'漆艺杯垫·四色一组','/api/uploads/demo/goods-20.svg',NULL,168.00,5,0),
(26,'HO202609141030120003100026',8, NULL,'柴烧茶盏·天青釉','/api/uploads/demo/goods-8.svg',NULL,380.00,2,0),
(27,'HO202609161030120003100027',22,NULL,'柴烧花器·落灰釉','/api/uploads/demo/goods-22.svg',NULL,680.00,1,0);

-- 6.25.8 校准 booking_count
--   课程的累计预约数应当是「占用名额」的累计值。
--   这里只能用 total_quota − remain_quota 反算而不能用 SUM(quota)：
--   booking_count 是累计量（含历史上已归还名额的预约），
--   而 remain_quota 是实时余量，两者相等只是因为本节把数据配平了。
--   用反算值同时起到一次交叉校验的作用——若前面哪一段没配平，这里的结果会对不上 remain_quota。
UPDATE `course` SET `booking_count` = `total_quota` - `remain_quota`
WHERE `id` IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);



-- =====================================================================
--  6.26 P8 管理端演示数据补充
-- =====================================================================
--  为什么 P8 还要再补一轮数据：P8 交付的是「平台级」统计与监管页面，
--  它的每一张图都要覆盖**全部**数据，而不只是一个匠人的数据。
--  P7 的 6.25 节补的是「匠人看板撑得起来」，它做的三件事对 P8 都不够用：
--
--    ① 注册时间：6.25 把 14 位演示用户的 create_time 全留在 NOW()，
--       于是「用户增长趋势」只能画出一根竖线，近 6 月的柱状图只有最后一根有高度。
--       → 6.26.1 按天铺开（用相对时间，任何时候重跑都成立）。
--
--    ② 入驻审核：三位匠人在种子里就直接是「已通过」，管理端「匠人资质审核」
--       打开后一条待办都没有 —— 一个没有待审单据的审核页，无法证明审核流程是通的。
--       → 6.26.2 补 1 位待审核 + 1 位已驳回的入驻申请。
--
--    ③ 访问行为：6.25 的 browse_history 只有 7 条、且全在当天，
--       「用户访问量统计」和「月度活跃用户趋势」两张图没东西可画。
--       → 6.26.3 补 6 个月跨度的浏览记录。
--
--  另外顺手校准了一处 P6 留下的口径问题（见 6.26.5 的说明）。

-- 6.26.1 把注册时间铺开（近 6 个月）
--   规则：管理员最早（180 天前）；三位匠人紧随其后（150/145/140 天前，
--   他们是平台的内容供给方，早于普通用户注册才符合业务顺序）；
--   8 位演示用户散在 120~2 天前；user01 留在今天，保证「今日新增」不为 0。
--   用 DATE_SUB(NOW(), ...) 而不是写死日期：种子在任何一天执行，
--   「今日新增 / 近 7 天新增 / 本月新增」三个数字都仍然是合理的。
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 180 DAY) WHERE `id` = 1;
UPDATE `user` SET `create_time` = NOW() WHERE `id` = 2;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE `id` = 3;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 150 DAY) WHERE `id` = 11;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 145 DAY) WHERE `id` = 12;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 140 DAY) WHERE `id` = 13;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 120 DAY) WHERE `id` = 14;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 96 DAY) WHERE `id` = 15;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 74 DAY) WHERE `id` = 16;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 58 DAY) WHERE `id` = 17;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 33 DAY) WHERE `id` = 18;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 12 DAY) WHERE `id` = 19;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 5 DAY) WHERE `id` = 20;
UPDATE `user` SET `create_time` = DATE_SUB(NOW(), INTERVAL 2 DAY) WHERE `id` = 21;

-- 6.26.2 匠人入驻申请（1 位待审核 + 1 位已驳回）
--   为什么复用罗甄(20) / 夏敏(21) 而不是新增账号：
--     这两人本来就是 6.25 补的演示用户，已经以「学员」身份约过课。
--     让他们同时作为「申请入驻的手艺人」出现，既是真实场景（手艺人也会上课），
--     又能保持演示账号总数不变 —— P7 联调脚本里那条「演示数据完好」的断言
--     就是按 14 个账号写死的，不动人数就不会把它弄红。
--   注意：user.role 改成 2 只是「角色是匠人」，能不能用匠人端还要看
--   craftsman.audit_status —— 这正是 P8 资质审核要管的事。
UPDATE `user` SET `role` = 2 WHERE `id` IN (20, 21);

INSERT INTO `craftsman`
(`id`,`user_id`,`real_name`,`craft_type`,`inheritance`,`intro`,`resume`,`cover`,
 `id_card_no`,`cert_image`,`audit_status`,`reject_reason`,`audit_time`,`follower_count`) VALUES
-- 罗甄：待审核（管理端「入驻审核」的第一条待办）
(4, 20, '罗甄', '蜀绣', '师从蜀绣省级代表性传承人李文秀',
 '蜀绣双面绣研习十二年，擅长绣制花鸟与熊猫题材的团扇、摆件。',
 '<p>2014 年入门蜀绣，2018 年随师父参与省级非遗展演，2021 年起独立接单。</p>',
 '/api/uploads/demo/craftsman-4.svg', '5101**********1234',
 '/api/uploads/demo/cert-4.svg', 0, NULL, NULL, 0),
-- 夏敏：已驳回（示范「驳回必填原因」之后的终态长什么样）
(5, 21, '夏敏', '竹编', NULL,
 '从事竹编器具制作六年，主要做提篮、花器与茶席配件。',
 '<p>2019 年跟随家乡竹编合作社学习，2022 年开始在本地市集售卖竹编器物。</p>',
 '/api/uploads/demo/craftsman-5.svg', '3301**********5678',
 '/api/uploads/demo/cert-5.svg', 2,
 '资质证明材料不清晰：证书照片存在反光遮挡，且未提供可核验的师承证明。请重新上传后再次提交。',
 DATE_SUB(NOW(), INTERVAL 1 DAY), 0);

-- 6.26.3 浏览行为（63 条，跨 6 个月）
--   为什么需要它：「用户访问量统计」与「月度活跃用户趋势」是 P8 的两张图，
--   而**访问**这件事在主表里没有痕迹 —— orders 只能反映下单的人，
--   browse_history 才是平台流量的真实来源。
--   分布刻意不均：有的用户 9 条、有的 5 条；既有近 7 天的活跃用户，
--   也有 5 个月没来过的人。若每条都平均分配，趋势图会变成一条水平线。
--
--   唯一键提醒：uk_user_target(user_id, target_type, target_id) ——
--   同一用户对同一目标只能有一条（重复浏览是 UPDATE browse_time，不是 INSERT）。
--   下面这 63 条两两之间不重复，且都不与 6.x 已有的 7 条重复。
INSERT INTO `browse_history` (`id`,`user_id`,`target_type`,`target_id`,`browse_time`) VALUES
(8, 2, 1, 19, DATE_SUB(NOW(), INTERVAL 157 DAY) - INTERVAL 6 HOUR - INTERVAL 22 MINUTE),
(9, 2, 2, 8, DATE_SUB(NOW(), INTERVAL 102 DAY) - INTERVAL 13 HOUR - INTERVAL 35 MINUTE),
(10, 2, 3, 6, DATE_SUB(NOW(), INTERVAL 47 DAY) - INTERVAL 20 HOUR - INTERVAL 48 MINUTE),
(11, 2, 1, 12, DATE_SUB(NOW(), INTERVAL 29 DAY) - INTERVAL 3 HOUR - INTERVAL 1 MINUTE),
(12, 2, 1, 15, DATE_SUB(NOW(), INTERVAL 6 DAY) - INTERVAL 10 HOUR - INTERVAL 14 MINUTE),
(13, 2, 2, 6, DATE_SUB(NOW(), INTERVAL 152 DAY) - INTERVAL 17 HOUR - INTERVAL 27 MINUTE),
(14, 3, 2, 5, DATE_SUB(NOW(), INTERVAL 100 DAY) - INTERVAL 9 HOUR - INTERVAL 33 MINUTE),
(15, 3, 3, 4, DATE_SUB(NOW(), INTERVAL 45 DAY) - INTERVAL 16 HOUR - INTERVAL 46 MINUTE),
(16, 3, 1, 17, DATE_SUB(NOW(), INTERVAL 27 DAY) - INTERVAL 23 HOUR - INTERVAL 59 MINUTE),
(17, 3, 1, 20, DATE_SUB(NOW(), INTERVAL 4 DAY) - INTERVAL 6 HOUR - INTERVAL 12 MINUTE),
(18, 3, 1, 1, DATE_SUB(NOW(), INTERVAL 180 DAY) - INTERVAL 13 HOUR - INTERVAL 25 MINUTE),
(19, 14, 1, 12, DATE_SUB(NOW(), INTERVAL 133 DAY) - INTERVAL 18 HOUR - INTERVAL 34 MINUTE),
(20, 14, 2, 2, DATE_SUB(NOW(), INTERVAL 78 DAY) - INTERVAL 1 HOUR - INTERVAL 47 MINUTE),
(21, 14, 2, 5, DATE_SUB(NOW(), INTERVAL 14 DAY) - INTERVAL 8 HOUR - INTERVAL 0 MINUTE),
(22, 14, 3, 6, DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 15 HOUR - INTERVAL 13 MINUTE),
(23, 14, 1, 4, DATE_SUB(NOW(), INTERVAL 153 DAY) - INTERVAL 22 HOUR - INTERVAL 26 MINUTE),
(24, 14, 1, 9, DATE_SUB(NOW(), INTERVAL 99 DAY) - INTERVAL 5 HOUR - INTERVAL 39 MINUTE),
(25, 14, 2, 10, DATE_SUB(NOW(), INTERVAL 44 DAY) - INTERVAL 12 HOUR - INTERVAL 52 MINUTE),
(26, 14, 2, 3, DATE_SUB(NOW(), INTERVAL 16 DAY) - INTERVAL 19 HOUR - INTERVAL 5 MINUTE),
(27, 14, 2, 8, DATE_SUB(NOW(), INTERVAL 6 DAY) - INTERVAL 2 HOUR - INTERVAL 18 MINUTE),
(28, 15, 2, 6, DATE_SUB(NOW(), INTERVAL 166 DAY) - INTERVAL 21 HOUR - INTERVAL 45 MINUTE),
(29, 15, 2, 9, DATE_SUB(NOW(), INTERVAL 141 DAY) - INTERVAL 4 HOUR - INTERVAL 58 MINUTE),
(30, 15, 3, 4, DATE_SUB(NOW(), INTERVAL 86 DAY) - INTERVAL 11 HOUR - INTERVAL 11 MINUTE),
(31, 15, 1, 10, DATE_SUB(NOW(), INTERVAL 22 DAY) - INTERVAL 18 HOUR - INTERVAL 24 MINUTE),
(32, 15, 1, 13, DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR - INTERVAL 37 MINUTE),
(33, 15, 1, 17, DATE_SUB(NOW(), INTERVAL 161 DAY) - INTERVAL 8 HOUR - INTERVAL 50 MINUTE),
(34, 15, 2, 4, DATE_SUB(NOW(), INTERVAL 107 DAY) - INTERVAL 15 HOUR - INTERVAL 3 MINUTE),
(35, 16, 2, 3, DATE_SUB(NOW(), INTERVAL 79 DAY) - INTERVAL 0 HOUR - INTERVAL 56 MINUTE),
(36, 16, 3, 2, DATE_SUB(NOW(), INTERVAL 15 DAY) - INTERVAL 7 HOUR - INTERVAL 9 MINUTE),
(37, 16, 1, 14, DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 14 HOUR - INTERVAL 22 MINUTE),
(38, 16, 1, 18, DATE_SUB(NOW(), INTERVAL 154 DAY) - INTERVAL 21 HOUR - INTERVAL 35 MINUTE),
(39, 16, 1, 21, DATE_SUB(NOW(), INTERVAL 100 DAY) - INTERVAL 4 HOUR - INTERVAL 48 MINUTE),
(40, 16, 2, 8, DATE_SUB(NOW(), INTERVAL 45 DAY) - INTERVAL 11 HOUR - INTERVAL 1 MINUTE),
(41, 16, 2, 1, DATE_SUB(NOW(), INTERVAL 17 DAY) - INTERVAL 18 HOUR - INTERVAL 14 MINUTE),
(42, 16, 2, 6, DATE_SUB(NOW(), INTERVAL 7 DAY) - INTERVAL 1 HOUR - INTERVAL 27 MINUTE),
(43, 17, 3, 6, DATE_SUB(NOW(), INTERVAL 172 DAY) - INTERVAL 3 HOUR - INTERVAL 7 MINUTE),
(44, 17, 1, 19, DATE_SUB(NOW(), INTERVAL 147 DAY) - INTERVAL 10 HOUR - INTERVAL 20 MINUTE),
(45, 17, 1, 22, DATE_SUB(NOW(), INTERVAL 33 DAY) - INTERVAL 17 HOUR - INTERVAL 33 MINUTE),
(46, 17, 1, 4, DATE_SUB(NOW(), INTERVAL 28 DAY) - INTERVAL 0 HOUR - INTERVAL 46 MINUTE),
(47, 17, 2, 2, DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 7 HOUR - INTERVAL 59 MINUTE),
(48, 18, 1, 1, DATE_SUB(NOW(), INTERVAL 175 DAY) - INTERVAL 6 HOUR - INTERVAL 18 MINUTE),
(49, 18, 1, 6, DATE_SUB(NOW(), INTERVAL 91 DAY) - INTERVAL 13 HOUR - INTERVAL 31 MINUTE),
(50, 18, 1, 10, DATE_SUB(NOW(), INTERVAL 36 DAY) - INTERVAL 20 HOUR - INTERVAL 44 MINUTE),
(51, 18, 2, 6, DATE_SUB(NOW(), INTERVAL 8 DAY) - INTERVAL 3 HOUR - INTERVAL 57 MINUTE),
(52, 18, 2, 9, DATE_SUB(NOW(), INTERVAL 5 DAY) - INTERVAL 10 HOUR - INTERVAL 10 MINUTE),
(53, 18, 3, 4, DATE_SUB(NOW(), INTERVAL 170 DAY) - INTERVAL 17 HOUR - INTERVAL 23 MINUTE),
(54, 19, 1, 11, DATE_SUB(NOW(), INTERVAL 148 DAY) - INTERVAL 9 HOUR - INTERVAL 29 MINUTE),
(55, 19, 1, 14, DATE_SUB(NOW(), INTERVAL 34 DAY) - INTERVAL 16 HOUR - INTERVAL 42 MINUTE),
(56, 19, 2, 10, DATE_SUB(NOW(), INTERVAL 29 DAY) - INTERVAL 23 HOUR - INTERVAL 55 MINUTE),
(57, 19, 2, 3, DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 6 HOUR - INTERVAL 8 MINUTE),
(58, 19, 3, 2, DATE_SUB(NOW(), INTERVAL 168 DAY) - INTERVAL 13 HOUR - INTERVAL 21 MINUTE),
(59, 19, 1, 8, DATE_SUB(NOW(), INTERVAL 114 DAY) - INTERVAL 20 HOUR - INTERVAL 34 MINUTE),
(60, 19, 2, 8, DATE_SUB(NOW(), INTERVAL 59 DAY) - INTERVAL 3 HOUR - INTERVAL 47 MINUTE),
(61, 20, 1, 19, DATE_SUB(NOW(), INTERVAL 22 DAY) - INTERVAL 12 HOUR - INTERVAL 40 MINUTE),
(62, 20, 2, 4, DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 19 HOUR - INTERVAL 53 MINUTE),
(63, 20, 2, 7, DATE_SUB(NOW(), INTERVAL 161 DAY) - INTERVAL 2 HOUR - INTERVAL 6 MINUTE),
(64, 20, 3, 6, DATE_SUB(NOW(), INTERVAL 107 DAY) - INTERVAL 9 HOUR - INTERVAL 19 MINUTE),
(65, 20, 1, 12, DATE_SUB(NOW(), INTERVAL 52 DAY) - INTERVAL 16 HOUR - INTERVAL 32 MINUTE),
(66, 21, 2, 8, DATE_SUB(NOW(), INTERVAL 25 DAY) - INTERVAL 15 HOUR - INTERVAL 51 MINUTE),
(67, 21, 2, 1, DATE_SUB(NOW(), INTERVAL 6 DAY) - INTERVAL 22 HOUR - INTERVAL 4 MINUTE),
(68, 21, 3, 4, DATE_SUB(NOW(), INTERVAL 164 DAY) - INTERVAL 5 HOUR - INTERVAL 17 MINUTE),
(69, 21, 1, 17, DATE_SUB(NOW(), INTERVAL 110 DAY) - INTERVAL 12 HOUR - INTERVAL 30 MINUTE),
(70, 21, 1, 20, DATE_SUB(NOW(), INTERVAL 55 DAY) - INTERVAL 19 HOUR - INTERVAL 43 MINUTE);

-- 6.26.4 补充收藏（让「收藏量统计」有多个目标类型）
--   原种子只有 5 条收藏、且集中在 2 个用户身上。
--   同样受 uk_user_target 约束：同一用户对同一目标只能收藏一次。
INSERT INTO `favorite` (`user_id`,`target_type`,`target_id`) VALUES
(14, 1, 2),
(14, 1, 12),
(14, 2, 1),
(15, 1, 6),
(15, 2, 4),
(16, 1, 13),
(16, 3, 1),
(17, 1, 18),
(17, 2, 6),
(18, 1, 9),
(19, 2, 5),
(19, 1, 14),
(20, 1, 15),
(21, 3, 2);

-- 6.26.5 补齐优惠券卡包，并校准 coupon.received_count
--
--   【口径修正】P6 报告里写过一句：「coupon.received_count 表示全平台已领张数，
--   user_coupon 只演示了几位用户的卡包，两者数量不同是正常的」。
--   这个说法在**只看列表**时没问题，但 P8 要做「优惠券核销率」，就站不住了：
--     核销率 = 已使用 ÷ 已领取。若分子取自 user_coupon、分母取自 received_count，
--     等于拿「演示卡包」除以「全平台」，算出来的比率没有任何意义。
--   而业务上两者本来就该相等 —— 领券动作在同一个事务里做两件事：
--     INSERT 一条 user_coupon + received_count 加 1。
--   所以 received_count ≠ COUNT(user_coupon) 就是数据自相矛盾，不是「演示正常」。
--   P8 把这批数据校准为 received_count == COUNT(user_coupon)，
--   核销率这才是一个可以对 SQL 复算的数字。
--
--   约束：per_limit = 1 → 同一用户对同一张券只能有一条 user_coupon 记录，
--   因此每张券的行数上限 = 参与演示的用户数（13 位：2,3,11~21）。
--   下面券1 13 行、券2 9 行、券3 11 行，均未突破该上限。
INSERT INTO `user_coupon` (`user_id`,`coupon_id`,`status`,`order_no`,`used_time`,`expire_time`) VALUES
(3, 1, 1, 'HO202609121030120002100009', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY)),
(11, 1, 1, 'HO202609131030120002100020', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY)),
(12, 1, 1, 'HO202609141030120002100012', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY)),
(13, 1, 1, 'HO202609151030120002100021', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY)),
(14, 1, 1, 'HO202609121030120002100009', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY)),
(15, 1, 1, 'HO202609131030120002100020', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY)),
(16, 1, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 55 DAY)),
(17, 1, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 55 DAY)),
(18, 1, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 55 DAY)),
(19, 1, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 55 DAY)),
(20, 1, 2, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(21, 1, 2, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(11, 2, 1, 'HO202609121030120003100010', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY)),
(13, 2, 1, 'HO202609141030120002100013', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY)),
(14, 2, 1, 'HO202609131030120003100023', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY)),
(16, 2, 1, 'HO202609121030120003100025', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY)),
(17, 2, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 65 DAY)),
(18, 2, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 65 DAY)),
(20, 2, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 65 DAY)),
(21, 2, 2, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3, 3, 1, 'HO202609131030120003100011', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 75 DAY)),
(11, 3, 1, 'HO202609151030120003100014', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 75 DAY)),
(12, 3, 1, 'HO202609161030120003100027', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 75 DAY)),
(13, 3, 1, 'HO202609181030120002100017', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 75 DAY)),
(15, 3, 1, 'HO202609141030120003100026', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 75 DAY)),
(14, 3, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 75 DAY)),
(16, 3, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 75 DAY)),
(19, 3, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 75 DAY)),
(20, 3, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 75 DAY)),
(21, 3, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 75 DAY));

-- 校准每张券的已领张数 = 该券的卡包记录数（两者必须相等，理由见上）
UPDATE `coupon` c SET c.received_count =
  (SELECT COUNT(*) FROM `user_coupon` uc WHERE uc.coupon_id = c.id AND uc.deleted = 0);


-- =====================================================================
--  6.27 P8 预约状态流转轨迹（演示数据）
-- =====================================================================
--
-- 写法说明：这里**不用 VALUES 硬写 39 条轨迹**，而是从 booking 表现场推导。
--
-- 理由有两个：
--   ① booking 的 create_time 在 6.25 段里是 NOW() 相对值算出来的，如果轨迹写死
--      绝对时间，重新导入一次数据之后「轨迹时间」就会落在「预约创建时间」之前，
--      演示时一眼就看出是假的；
--   ② 轨迹条数天然跟着预约条数走。以后调整预约种子（比如多补几门课），
--      不必再手工同步一遍轨迹。
--
-- 语句顺序 = 轨迹顺序：每条 INSERT ... SELECT 覆盖全部预约，
-- 因此同一张预约的轨迹 id 先后顺序就是下面语句的先后顺序。
-- 已取消的预约若在取消前已通过审核（audit_time 非空），会先补一条「审核通过」，
-- 这样轨迹才读得通（不会出现「从待审核直接跳到已取消」这种信息缺失）。

-- ① 提交预约（待审核）
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id, NULL, 0, b.user_id, 1, u.username, '提交预约，等待匠人审核', b.create_time
FROM `booking` b JOIN `user` u ON u.id = b.user_id
WHERE b.deleted = 0
  AND NOT (b.audit_time IS NULL AND b.status IN (1, 4, 5));

-- ② 提交预约（该课程关闭了审核，直接通过）
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id, NULL, 1, b.user_id, 1, u.username, '提交预约（该课程已关闭审核，直接通过）', b.create_time
FROM `booking` b JOIN `user` u ON u.id = b.user_id
WHERE b.deleted = 0 AND b.audit_time IS NULL AND b.status IN (1, 4, 5);

-- ③ 审核通过（含「通过之后又被取消」的预约，否则轨迹会缺一环）
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id, 0, 1, b.auditor_id, 2, IFNULL(NULLIF(au.nickname, ''), au.username), '匠人审核通过',
       IFNULL(b.audit_time, b.create_time)
FROM `booking` b
LEFT JOIN `user` au ON au.id = b.auditor_id
WHERE b.deleted = 0 AND b.audit_time IS NOT NULL
  AND (b.status IN (1, 4, 5) OR b.status = 3);

-- ④ 审核拒绝（匠人手拒 / 系统超时自动拒绝按原因文案区分操作方）
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id, 0, 2,
       IF(b.reject_reason LIKE '%系统%', NULL, b.auditor_id),
       IF(b.reject_reason LIKE '%系统%', 4, 2),
       IF(b.reject_reason LIKE '%系统%', '系统', IFNULL(NULLIF(au.nickname, ''), au.username)),
       IFNULL(b.reject_reason, '审核未通过'),
       IFNULL(b.audit_time, b.create_time)
FROM `booking` b
LEFT JOIN `user` au ON au.id = b.auditor_id
WHERE b.deleted = 0 AND b.status = 2;

-- ⑤ 取消（四种发起方各归其位；取消前已通过审核的原状态记 1）
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id,
       IF(b.audit_time IS NULL, 0, 1), 3,
       IF(b.cancel_by = 4, NULL, b.user_id),
       IFNULL(b.cancel_by, 4),
       CASE b.cancel_by
         WHEN 1 THEN IFNULL(NULLIF(u.nickname, ''), u.username)
         WHEN 2 THEN IFNULL(cr.real_name, '匠人')
         WHEN 3 THEN '平台管理员'
         ELSE '系统'
       END,
       IFNULL(NULLIF(b.cancel_reason, ''), '预约已取消'),
       IFNULL(b.cancel_time, b.create_time)
FROM `booking` b
LEFT JOIN `user` u ON u.id = b.user_id
LEFT JOIN `craftsman` cr ON cr.id = b.craftsman_id
WHERE b.deleted = 0 AND b.status = 3;

-- ⑥ 现场核销签到
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id, 1, 4, NULL, 2, IFNULL(cr.real_name, '匠人'), '匠人现场核销签到',
       IFNULL(b.checkin_time, b.audit_time)
FROM `booking` b
LEFT JOIN `craftsman` cr ON cr.id = b.craftsman_id
WHERE b.deleted = 0 AND b.status IN (4, 5);

-- ⑦ 课程结束后自动结课
INSERT INTO `booking_status_log`
  (`booking_id`, `from_status`, `to_status`, `operator_id`, `operator_role`, `operator_name`, `remark`, `create_time`)
SELECT b.id, 4, 5, NULL, 4, '系统', CONCAT('课程「', c.title, '」已结束，自动结课'),
       IFNULL(c.end_time, b.checkin_time)
FROM `booking` b JOIN `course` c ON c.id = b.course_id
WHERE b.deleted = 0 AND b.status = 5;


SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
--  初始化数据统计（用于自验）
-- =====================================================================
SELECT '表数量' AS item, COUNT(*) AS value FROM information_schema.tables WHERE table_schema = 'heritage'
UNION ALL SELECT '用户数',   COUNT(*) FROM `user`
UNION ALL SELECT '匠人数',   COUNT(*) FROM `craftsman`
UNION ALL SELECT '分类数',   COUNT(*) FROM `category`
UNION ALL SELECT '商品数',   COUNT(*) FROM `goods`
UNION ALL SELECT '课程数',   COUNT(*) FROM `course`
UNION ALL SELECT '预约数',   COUNT(*) FROM `booking`
UNION ALL SELECT '预约轨迹数', COUNT(*) FROM `booking_status_log`
UNION ALL SELECT '关注数',   COUNT(*) FROM `craftsman_follow`
UNION ALL SELECT '订单数',   COUNT(*) FROM `orders`
UNION ALL SELECT '订单项数', COUNT(*) FROM `order_item`
UNION ALL SELECT '评价数',   COUNT(*) FROM `review`
UNION ALL SELECT '优惠券数', COUNT(*) FROM `coupon`
UNION ALL SELECT '资讯数',   COUNT(*) FROM `article`
UNION ALL SELECT '轮播图数', COUNT(*) FROM `banner`;
