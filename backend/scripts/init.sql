-- =====================================================================
-- 邻享租借平台 数据库初始化脚本
-- 可重复执行：先 DROP 再 CREATE。
-- 文档对齐：docs/04-database-design.md、docs/邻享租借平台拟稿.md
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 1. sr_auth ----------
DROP DATABASE IF EXISTS sr_auth;
CREATE DATABASE sr_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sr_auth;

CREATE TABLE users (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    username                 VARCHAR(64)  NOT NULL,
    password                 VARCHAR(100) NOT NULL,
    avatar_url               VARCHAR(255) DEFAULT NULL,
    description              VARCHAR(255) DEFAULT NULL,
    credit_score             INT          NOT NULL DEFAULT 100,
    role                     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通用户 1=管理员',
    status                   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常 1=已封禁',
    username_audit_status    TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未审核 1=审核通过 2=要求整改',
    avatar_audit_status      TINYINT      NOT NULL DEFAULT 0,
    description_audit_status TINYINT      NOT NULL DEFAULT 0,
    show_rental_history      TINYINT      NOT NULL DEFAULT 0,
    last_login_time         DATETIME     DEFAULT NULL,
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                  TINYINT      NOT NULL DEFAULT 0,
    version                  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username_deleted (username, deleted),
    KEY idx_status (status),
    KEY idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_blacklist (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    target_user_id BIGINT      NOT NULL,
    reason         VARCHAR(255) DEFAULT NULL,
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target_deleted (user_id, target_user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE credit_score_records (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    order_id      BIGINT      DEFAULT NULL,
    change_value  INT         NOT NULL,
    before_score  INT         NOT NULL,
    after_score   INT         NOT NULL,
    reason_type   VARCHAR(32) NOT NULL,
    reason        VARCHAR(255) DEFAULT NULL,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员：admin / 123456（BCrypt 加密）
-- 哈希值由 BCryptPasswordEncoder(strength=10) 生成，可用 AdminPasswordBcryptTest 校验
INSERT INTO users (username, password, role, status, username_audit_status, avatar_audit_status, description_audit_status)
VALUES ('admin', '$2a$10$BWjZ/5bA4mJ0AlMNWAZ2V.lXNgZeCZUAl0ZHWgCl3dbr4Xw.j1RoC', 1, 0, 1, 1, 1);

-- ---------- 2. sr_item ----------
DROP DATABASE IF EXISTS sr_item;
CREATE DATABASE sr_item DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sr_item;

CREATE TABLE categories (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0=启用 1=禁用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE items (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id                BIGINT        NOT NULL,
    title                   VARCHAR(128) NOT NULL,
    description             TEXT,
    category_id             BIGINT        NOT NULL,
    tags                    VARCHAR(255),
    quantity                INT           NOT NULL DEFAULT 1,
    rented_count            INT           NOT NULL DEFAULT 0,
    support_delivery        TINYINT       NOT NULL DEFAULT 0,
    delivery_city           VARCHAR(64),
    support_meetup          TINYINT       NOT NULL DEFAULT 0,
    meetup_location         VARCHAR(255),
    price_type              TINYINT       NOT NULL DEFAULT 0 COMMENT '0=免费 1=按天',
    daily_price             DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    min_rent_days           INT           NOT NULL DEFAULT 1,
    free_rent               TINYINT       NOT NULL DEFAULT 0,
    deposit_enabled         TINYINT       NOT NULL DEFAULT 0,
    deposit_amount          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    credit_deposit_enabled  TINYINT       NOT NULL DEFAULT 0,
    min_credit_score        INT           NOT NULL DEFAULT 0,
    free_deposit_score      INT           NOT NULL DEFAULT 0,
    reduced_deposit_score   INT           NOT NULL DEFAULT 0,
    reduced_deposit_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status                  TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待上架 1=已上架 2=已下架 3=强制下架',
    audit_status            TINYINT       NOT NULL DEFAULT 0,
    audit_reason            VARCHAR(255),
    audit_time              DATETIME,
    audit_admin_id          BIGINT,
    version                 INT           NOT NULL DEFAULT 0,
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_owner_id (owner_id),
    KEY idx_category_status (category_id, status),
    KEY idx_audit_status (audit_status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_images (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    item_id     BIGINT       NOT NULL,
    url         VARCHAR(255) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE favorites (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    item_id     BIGINT   NOT NULL,
    status      TINYINT  NOT NULL DEFAULT 1 COMMENT '1=已收藏 0=已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_item (user_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_snapshots (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    item_id        BIGINT        NOT NULL,
    owner_id       BIGINT        NOT NULL,
    title          VARCHAR(128)  NOT NULL,
    description    TEXT,
    category_name  VARCHAR(64),
    image_urls     TEXT,
    price_type     TINYINT       NOT NULL,
    daily_price     DECIMAL(10,2) NOT NULL,
    deposit_amount  DECIMAL(10,2) NOT NULL,
    support_delivery TINYINT     NOT NULL,
    support_meetup TINYINT       NOT NULL,
    snapshot_json  TEXT,
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化 8 个分类
INSERT INTO categories (name, sort_order, status) VALUES
  ('数码设备', 1, 0),
  ('摄影器材', 2, 0),
  ('户外露营', 3, 0),
  ('生活工具', 4, 0),
  ('图书教材', 5, 0),
  ('服饰道具', 6, 0),
  ('运动器材', 7, 0),
  ('其他',     8, 0);

-- ---------- 3. sr_rental ----------
DROP DATABASE IF EXISTS sr_rental;
CREATE DATABASE sr_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sr_rental;

CREATE TABLE rental_applications (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    item_id                     BIGINT        NOT NULL,
    owner_id                    BIGINT        NOT NULL,
    renter_id                   BIGINT        NOT NULL,
    conversation_id             BIGINT,
    status                      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待协商 1=已确认 2=已转单 3=已取消',
    current_proposal_id         BIGINT,
    owner_confirmed             TINYINT       NOT NULL DEFAULT 0,
    renter_confirmed            TINYINT       NOT NULL DEFAULT 0,
    prepaid_rent_amount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pre_frozen_deposit_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    create_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                     TINYINT       NOT NULL DEFAULT 0,
    version                     INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_item_id (item_id),
    KEY idx_owner_id (owner_id),
    KEY idx_renter_id (renter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental_proposals (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    application_id   BIGINT        NOT NULL,
    version_no       INT           NOT NULL,
    operator_id      BIGINT        NOT NULL,
    quantity         INT           NOT NULL DEFAULT 1,
    delivery_type    TINYINT       NOT NULL COMMENT '0=面交 1=快递',
    rent_start_time  DATETIME      NOT NULL,
    rent_end_time    DATETIME      NOT NULL,
    meetup_time      DATETIME,
    meetup_location  VARCHAR(255),
    receiver_name    VARCHAR(64),
    receiver_phone   VARCHAR(32),
    receiver_address VARCHAR(255),
    rent_amount      DECIMAL(10,2) NOT NULL,
    deposit_amount   DECIMAL(10,2) NOT NULL,
    changed_fields   VARCHAR(255),
    remark           VARCHAR(255),
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application_version (application_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental_orders (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    order_no                VARCHAR(32)   NOT NULL,
    application_id          BIGINT        NOT NULL,
    proposal_id             BIGINT        NOT NULL,
    item_id                 BIGINT        NOT NULL,
    item_snapshot_id        BIGINT,
    owner_id                BIGINT        NOT NULL,
    renter_id               BIGINT        NOT NULL,
    quantity                INT           NOT NULL DEFAULT 1,
    delivery_type           TINYINT       NOT NULL,
    rent_start_time         DATETIME      NOT NULL,
    rent_end_time           DATETIME      NOT NULL,
    daily_price             DECIMAL(10,2) NOT NULL,
    rent_amount             DECIMAL(10,2) NOT NULL,
    deposit_amount          DECIMAL(10,2) NOT NULL,
    paid_rent_amount        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    frozen_deposit_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status                  TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待付款 1=已付款待交付 2=已发货 3=租借中 4=待归还确认 5=已完成 6=已取消 7=异议中 8=已关闭',
    ship_company            VARCHAR(64),
    ship_tracking_no        VARCHAR(64),
    return_company          VARCHAR(64),
    return_tracking_no      VARCHAR(64),
    received_time           DATETIME,
    returned_time           DATETIME,
    completed_time          DATETIME,
    overdue_minutes         INT           NOT NULL DEFAULT 0,
    overdue_fee_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    overdue_settled         TINYINT       NOT NULL DEFAULT 0,
    cancel_reason           VARCHAR(255),
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT       NOT NULL DEFAULT 0,
    version                 INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_owner_status (owner_id, status),
    KEY idx_renter_status (renter_id, status),
    KEY idx_item_time (item_id, rent_start_time, rent_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental_time_locks (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    order_id        BIGINT   NOT NULL,
    item_id         BIGINT   NOT NULL,
    quantity        INT      NOT NULL,
    rent_start_time DATETIME NOT NULL,
    rent_end_time   DATETIME NOT NULL,
    status          TINYINT  NOT NULL DEFAULT 0 COMMENT '0=占用 1=已释放',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_item_time_status (item_id, rent_start_time, rent_end_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_status_histories (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    order_id    BIGINT      NOT NULL,
    old_status  TINYINT,
    new_status  TINYINT     NOT NULL,
    operator_id BIGINT      NOT NULL,
    remark      VARCHAR(255),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reviews (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    order_id    BIGINT      NOT NULL,
    item_id     BIGINT      NOT NULL,
    reviewer_id BIGINT      NOT NULL,
    reviewee_id BIGINT      NOT NULL,
    rating      TINYINT     NOT NULL,
    content     VARCHAR(500),
    image_urls  TEXT,
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0=正常 1=已隐藏',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_reviewer (order_id, reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE disputes (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    order_id                 BIGINT        NOT NULL,
    applicant_id             BIGINT        NOT NULL,
    reason                   VARCHAR(128)  NOT NULL,
    description              VARCHAR(500),
    expected_deposit_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    image_urls               TEXT,
    status                   TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待处理 1=处理中 2=已裁定',
    admin_id                 BIGINT,
    admin_remark             VARCHAR(255),
    create_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 4. sr_wallet ----------
DROP DATABASE IF EXISTS sr_wallet;
CREATE DATABASE sr_wallet DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sr_wallet;

CREATE TABLE wallet_accounts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    balance       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    frozen_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '0=正常 1=已冻结',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wallet_transactions (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    transaction_no       VARCHAR(32)   NOT NULL,
    user_id              BIGINT        NOT NULL,
    counterparty_user_id BIGINT,
    application_id       BIGINT,
    proposal_id         BIGINT,
    order_id            BIGINT,
    type                TINYINT       NOT NULL COMMENT '1=充值 2=支付租金 3=冻结押金 4=释放押金 5=租金收入 6=取消退款 7=逾期支出 8=逾期收入 9=押金扣除',
    direction           TINYINT       NOT NULL COMMENT '1=收入 2=支出 3=冻结 4=解冻',
    amount              DECIMAL(10,2) NOT NULL,
    balance_before      DECIMAL(10,2) NOT NULL,
    balance_after       DECIMAL(10,2) NOT NULL,
    frozen_before       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    frozen_after        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    remark              VARCHAR(255),
    create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    KEY idx_user_id (user_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deposit_freezes (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    application_id    BIGINT,
    proposal_id       BIGINT,
    order_id          BIGINT,
    user_id           BIGINT        NOT NULL,
    amount            DECIMAL(10,2) NOT NULL,
    deducted_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status            TINYINT       NOT NULL DEFAULT 0 COMMENT '0=已冻结 1=已释放 2=已扣除 3=已取消 4=部分扣除后已释放',
    freeze_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    release_time      DATETIME,
    cancel_time       DATETIME,
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_settlements (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    order_id                 BIGINT        NOT NULL,
    renter_id                BIGINT        NOT NULL,
    owner_id                 BIGINT        NOT NULL,
    rent_amount              DECIMAL(10,2) NOT NULL,
    deposit_amount           DECIMAL(10,2) NOT NULL,
    overdue_fee_amount       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deposit_deducted_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    rent_settled             TINYINT       NOT NULL DEFAULT 0,
    deposit_released        TINYINT       NOT NULL DEFAULT 0,
    overdue_fee_settled     TINYINT       NOT NULL DEFAULT 0,
    status                   TINYINT       NOT NULL DEFAULT 0 COMMENT '0=未结算 1=部分结算 2=已结算',
    settle_time              DATETIME,
    create_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 5. sr_message ----------
DROP DATABASE IF EXISTS sr_message;
CREATE DATABASE sr_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sr_message;

CREATE TABLE conversations (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    item_id               BIGINT       NOT NULL,
    user_a_id             BIGINT       NOT NULL,
    user_b_id             BIGINT       NOT NULL,
    last_message_content  VARCHAR(500),
    last_message_time     DATETIME,
    user_a_unread_count   INT          NOT NULL DEFAULT 0,
    user_b_unread_count   INT          NOT NULL DEFAULT 0,
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_users (item_id, user_a_id, user_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE messages (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id         BIGINT       NOT NULL,
    sender_id               BIGINT       NOT NULL,
    receiver_id             BIGINT       NOT NULL,
    message_type            TINYINT      NOT NULL COMMENT '1=文本 2=图片 3=卡片 4=系统通知 5=审核通知',
    card_type               TINYINT      COMMENT '1=申请卡片 2=协商修改卡片 3=订单卡片 4=评价提醒卡片 5=异议卡片',
    content                 TEXT,
    image_urls              TEXT,
    related_application_id  BIGINT,
    related_order_id        BIGINT,
    is_read                 TINYINT      NOT NULL DEFAULT 0,
    read_time               DATETIME,
    create_time             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_conversation_id (conversation_id),
    KEY idx_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 6. sr_admin ----------
DROP DATABASE IF EXISTS sr_admin;
CREATE DATABASE sr_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sr_admin;

CREATE TABLE admin_operation_logs (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    admin_id       BIGINT      NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    target_type    VARCHAR(32) NOT NULL,
    target_id      BIGINT      NOT NULL,
    before_json    TEXT,
    after_json     TEXT,
    remark          VARCHAR(255),
    ip             VARCHAR(64),
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_admin_id (admin_id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_records (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    admin_id    BIGINT      NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id   BIGINT      NOT NULL,
    field_name  VARCHAR(64),
    old_status  TINYINT,
    new_status  TINYINT      NOT NULL,
    reason      VARCHAR(255),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
