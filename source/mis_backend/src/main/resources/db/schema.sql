-- =====================================================================
-- 医保结算经办（医保局端）模拟平台 建表脚本（MySQL，独立库 mis_insurance）
-- 启动幂等执行，全部 IF NOT EXISTS。库本身由 JDBC URL createDatabaseIfNotExist 自动创建。
-- =====================================================================

-- 参保人（医保侧参保库）
CREATE TABLE IF NOT EXISTS mis_insured_person (
    id               BIGINT       NOT NULL COMMENT '主键',
    id_card          VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
    insurance_no     VARCHAR(64)  DEFAULT NULL COMMENT '医保卡号/医保电子凭证号',
    name             VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    gender           VARCHAR(8)   DEFAULT NULL COMMENT '性别',
    insurance_type   VARCHAR(16)  DEFAULT NULL COMMENT '险种（2-城镇职工医保 3-城乡居民医保）',
    person_status    INT          DEFAULT 1 COMMENT '参保状态（1-正常参保 0-停保）',
    status_remark    VARCHAR(255) DEFAULT NULL COMMENT '停保/恢复原因留痕',
    account_balance  DECIMAL(12,2) DEFAULT 0 COMMENT '个人账户余额',
    region           VARCHAR(64)  DEFAULT '长沙市' COMMENT '参保地',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_person_idcard (id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参保人（医保侧参保库）';

-- 平台侧结算单（2304 接收后独立落库，即“对方账”）
CREATE TABLE IF NOT EXISTS mis_settlement (
    id               BIGINT       NOT NULL COMMENT '主键',
    trade_no         VARCHAR(64)  DEFAULT NULL COMMENT 'HIS 侧流水号',
    settlement_no    VARCHAR(64)  DEFAULT NULL COMMENT '医保结算清单号',
    orig_trade_no    VARCHAR(64)  DEFAULT NULL COMMENT '被撤销流水号（2305）',
    fix_medins_code  VARCHAR(32)  DEFAULT NULL COMMENT '定点机构编码',
    bill_no          VARCHAR(64)  DEFAULT NULL COMMENT 'HIS 账单号',
    encounter_type   VARCHAR(16)  DEFAULT NULL COMMENT '就诊类型（门诊/住院）',
    patient_name     VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    id_card          VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
    insurance_no     VARCHAR(64)  DEFAULT NULL COMMENT '医保卡号',
    insurance_type   VARCHAR(16)  DEFAULT NULL COMMENT '险种',
    total_amount     DECIMAL(12,2) DEFAULT 0 COMMENT '总费用',
    insurance_pay    DECIMAL(12,2) DEFAULT 0 COMMENT '院方申报统筹支付',
    personal_pay     DECIMAL(12,2) DEFAULT 0 COMMENT '院方申报个人账户支付',
    self_pay         DECIMAL(12,2) DEFAULT 0 COMMENT '院方申报现金自付',
    pool_confirm     DECIMAL(12,2) DEFAULT 0 COMMENT '平台核定统筹支付',
    account_deduct   DECIMAL(12,2) DEFAULT 0 COMMENT '2301个账实扣（撤销冲正退回个账）',
    status           INT          DEFAULT 1 COMMENT '状态（1-已接收有效 2-已撤销冲正）',
    bill_date        DATE         DEFAULT NULL COMMENT '账期日期',
    receipt_no       VARCHAR(64)  DEFAULT NULL COMMENT '医保端回执编号',
    warn_msg         VARCHAR(500) DEFAULT NULL COMMENT '平台告警（非阻断）',
    raw_payload      TEXT COMMENT '2304 原始报文',
    cancel_time      DATETIME     DEFAULT NULL COMMENT '撤销时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sett_trade (trade_no),
    UNIQUE KEY uk_sett_settlementno (settlement_no),
    KEY idx_sett_billdate (bill_date),
    KEY idx_sett_month (fix_medins_code, bill_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台侧结算单（对方账）';

-- 报文收发台账（2304/2305 全留痕）
CREATE TABLE IF NOT EXISTS mis_message (
    id               BIGINT       NOT NULL COMMENT '主键',
    msg_type         VARCHAR(16)  DEFAULT NULL COMMENT '报文类型（2304/2305）',
    trade_no         VARCHAR(64)  DEFAULT NULL COMMENT 'HIS 侧流水号',
    orig_trade_no    VARCHAR(64)  DEFAULT NULL COMMENT '被撤销流水号',
    success          INT          DEFAULT 1 COMMENT '是否成功（1-是 0-否）',
    receipt_no       VARCHAR(64)  DEFAULT NULL COMMENT '回执编号',
    err_msg          VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    payload          TEXT COMMENT '原始报文',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_msg_trade (trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报文收发台账';

-- 月度结算单（申报→审核→拨付→清算）
CREATE TABLE IF NOT EXISTS mis_monthly (
    id               BIGINT       NOT NULL COMMENT '主键',
    month            VARCHAR(7)   DEFAULT NULL COMMENT '账期（yyyy-MM）',
    fix_medins_code  VARCHAR(32)  DEFAULT NULL COMMENT '定点机构编码',
    declare_count    INT          DEFAULT 0 COMMENT '申报结算单笔数',
    declare_amount   DECIMAL(12,2) DEFAULT 0 COMMENT '申报统筹合计',
    deduct_amount    DECIMAL(12,2) DEFAULT 0 COMMENT '审核扣款/拒付合计',
    reserve_amount   DECIMAL(12,2) DEFAULT 0 COMMENT '质量保证金预留',
    payable_amount   DECIMAL(12,2) DEFAULT 0 COMMENT '本月应拨付',
    allocated_amount DECIMAL(12,2) DEFAULT 0 COMMENT '已拨付',
    status           INT          DEFAULT 1 COMMENT '状态（1-已申报 2-已审核 3-已拨付 4-已清算）',
    declare_time     DATETIME     DEFAULT NULL COMMENT '申报时间',
    audit_time       DATETIME     DEFAULT NULL COMMENT '审核时间',
    allocate_time    DATETIME     DEFAULT NULL COMMENT '拨付时间',
    settle_time      DATETIME     DEFAULT NULL COMMENT '年终清算时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度结算单';

-- 月度扣款/拒付明细
CREATE TABLE IF NOT EXISTS mis_monthly_deduct (
    id               BIGINT       NOT NULL COMMENT '主键',
    month            VARCHAR(7)   DEFAULT NULL COMMENT '账期（yyyy-MM）',
    trade_no         VARCHAR(64)  DEFAULT NULL COMMENT 'HIS 侧流水号',
    settlement_no    VARCHAR(64)  DEFAULT NULL COMMENT '结算清单号',
    deduct_type      INT          DEFAULT 1 COMMENT '扣款类型（1-拒付 2-审核扣款）',
    deduct_amount    DECIMAL(12,2) DEFAULT 0 COMMENT '扣款金额',
    reason           VARCHAR(255) DEFAULT NULL COMMENT '扣款原因',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_deduct_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度扣款/拒付明细';

-- 拨付单（月度拨付 / 质保金返还）
CREATE TABLE IF NOT EXISTS mis_allocation (
    id               BIGINT       NOT NULL COMMENT '主键',
    month            VARCHAR(7)   DEFAULT NULL COMMENT '账期（yyyy-MM）',
    alloc_no         VARCHAR(64)  DEFAULT NULL COMMENT '拨付单号',
    alloc_type       INT          DEFAULT 1 COMMENT '拨付类型（1-月度拨付 2-质保金返还/年清算）',
    amount           DECIMAL(12,2) DEFAULT 0 COMMENT '拨付金额',
    remark           VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_alloc_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保拨付单';

-- =====================================================================
-- 经办业务（医保中心窗口 / 经办平台）表
-- =====================================================================

-- 医保三大目录（西药/中成药/中草药/诊疗项目/医用耗材）
CREATE TABLE IF NOT EXISTS mis_catalog (
    id               BIGINT       NOT NULL COMMENT '主键',
    yb_code          VARCHAR(32)  DEFAULT NULL COMMENT '医保编码',
    name             VARCHAR(128) DEFAULT NULL COMMENT '目录名称',
    category         VARCHAR(32)  DEFAULT NULL COMMENT '类别（西药/中成药/中草药/诊疗项目/医用耗材）',
    spec             VARCHAR(64)  DEFAULT NULL COMMENT '规格',
    unit             VARCHAR(16)  DEFAULT NULL COMMENT '单位',
    pay_class        VARCHAR(8)   DEFAULT NULL COMMENT '支付类别（甲/乙/丙）',
    self_ratio       DECIMAL(5,2) DEFAULT 0 COMMENT '乙类先行自付比例（0-1）',
    insurance_ratio  DECIMAL(5,2) DEFAULT 0 COMMENT '政策报销比例（0-1）',
    limit_price      DECIMAL(12,2) DEFAULT 0 COMMENT '医保限价',
    status           INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_cat_name (name),
    KEY idx_cat_cat (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保三大目录';

-- 院内项目 ↔ 医保目录对照
CREATE TABLE IF NOT EXISTS mis_item_mapping (
    id               BIGINT       NOT NULL COMMENT '主键',
    hosp_item_code   VARCHAR(32)  DEFAULT NULL COMMENT '院内项目编码',
    hosp_item_name   VARCHAR(128) DEFAULT NULL COMMENT '院内项目名称',
    category         VARCHAR(32)  DEFAULT NULL COMMENT '类别',
    yb_code          VARCHAR(32)  DEFAULT NULL COMMENT '对照医保编码',
    yb_name          VARCHAR(128) DEFAULT NULL COMMENT '对照医保名称',
    pay_class        VARCHAR(8)   DEFAULT NULL COMMENT '支付类别（甲/乙/丙）',
    mapped           TINYINT(1)   DEFAULT 0 COMMENT '是否已对照（1-是 0-否）',
    updated_at       DATETIME     DEFAULT NULL COMMENT '对照时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_map_hosp (hosp_item_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院内项目医保对照';

-- 待结算业务（门诊就诊 / 出院登记）
CREATE TABLE IF NOT EXISTS mis_pending_settle (
    id               BIGINT       NOT NULL COMMENT '主键',
    biz_type         INT          DEFAULT 1 COMMENT '业务类型（1-门诊 2-住院）',
    biz_no           VARCHAR(32)  DEFAULT NULL COMMENT '业务号（门诊就诊号/住院号）',
    patient_name     VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    id_card          VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
    id_card_mask     VARCHAR(32)  DEFAULT NULL COMMENT '脱敏身份证号',
    insurance_type   VARCHAR(16)  DEFAULT NULL COMMENT '参保类型（职工医保/居民医保/异地就医）',
    dept             VARCHAR(64)  DEFAULT NULL COMMENT '科室',
    doctor           VARCHAR(64)  DEFAULT NULL COMMENT '医师',
    admit_days       INT          DEFAULT NULL COMMENT '住院天数',
    biz_time         DATETIME     DEFAULT NULL COMMENT '就诊/出院时间',
    status           INT          DEFAULT 1 COMMENT '状态（1-待结算 2-已结算）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_biz (biz_no),
    KEY idx_pending_type (biz_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待结算业务';

-- 经办结算单（窗口即时结算产生）
CREATE TABLE IF NOT EXISTS mis_bill (
    id               BIGINT       NOT NULL COMMENT '主键',
    bill_no          VARCHAR(32)  DEFAULT NULL COMMENT '结算单号',
    pending_id       BIGINT       DEFAULT NULL COMMENT '来源待结算业务id',
    biz_no           VARCHAR(32)  DEFAULT NULL COMMENT '业务号（就诊号/住院号）',
    admit_days       INT          DEFAULT NULL COMMENT '住院天数',
    patient_name     VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    id_card          VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
    id_card_mask     VARCHAR(32)  DEFAULT NULL COMMENT '脱敏身份证号',
    insurance_type   VARCHAR(16)  DEFAULT NULL COMMENT '参保类型',
    settle_type      VARCHAR(8)   DEFAULT NULL COMMENT '结算类型（门诊/住院）',
    dept             VARCHAR(64)  DEFAULT NULL COMMENT '科室',
    doctor           VARCHAR(64)  DEFAULT NULL COMMENT '医师',
    settle_time      DATETIME     DEFAULT NULL COMMENT '结算时间',
    status           VARCHAR(16)  DEFAULT '已结算' COMMENT '状态（已结算/待审核/已退费/异常）',
    total_amount     DECIMAL(12,2) DEFAULT 0 COMMENT '费用总额',
    insurance_pay    DECIMAL(12,2) DEFAULT 0 COMMENT '统筹支付',
    personal_account DECIMAL(12,2) DEFAULT 0 COMMENT '个人账户支付',
    cash_pay         DECIMAL(12,2) DEFAULT 0 COMMENT '现金支付',
    self_pay         DECIMAL(12,2) DEFAULT 0 COMMENT '个人自付合计',
    deductible       DECIMAL(12,2) DEFAULT 0 COMMENT '起付线',
    base_pool        DECIMAL(12,2) DEFAULT 0 COMMENT '统筹计入基数',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no),
    KEY idx_bill_time (settle_time),
    KEY idx_bill_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经办结算单';

-- 费用明细（待结算 / 结算单共用：owner_type 1-待结算 2-结算单）
CREATE TABLE IF NOT EXISTS mis_fee_item (
    id               BIGINT       NOT NULL COMMENT '主键',
    owner_type       INT          DEFAULT 1 COMMENT '归属类型（1-待结算 2-结算单）',
    owner_id         BIGINT       DEFAULT NULL COMMENT '归属单id',
    name             VARCHAR(128) DEFAULT NULL COMMENT '项目名称',
    category         VARCHAR(32)  DEFAULT NULL COMMENT '类别',
    spec             VARCHAR(64)  DEFAULT NULL COMMENT '规格',
    unit             VARCHAR(16)  DEFAULT NULL COMMENT '单位',
    yb_code          VARCHAR(32)  DEFAULT NULL COMMENT '医保编码',
    pay_class        VARCHAR(8)   DEFAULT NULL COMMENT '支付类别（甲/乙/丙）',
    self_ratio       DECIMAL(5,2) DEFAULT 0 COMMENT '乙类先行自付比例',
    insurance_ratio  DECIMAL(5,2) DEFAULT 0 COMMENT '政策报销比例',
    qty              INT          DEFAULT 1 COMMENT '数量',
    price            DECIMAL(12,2) DEFAULT 0 COMMENT '单价',
    amount           DECIMAL(12,2) DEFAULT 0 COMMENT '金额',
    reversed         TINYINT(1)   DEFAULT 0 COMMENT '是否已冲正（1-是 0-否）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_item_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用明细';

-- 费用审核任务
CREATE TABLE IF NOT EXISTS mis_fee_audit (
    id               BIGINT       NOT NULL COMMENT '主键',
    bill_id          BIGINT       DEFAULT NULL COMMENT '结算单id',
    bill_no          VARCHAR(32)  DEFAULT NULL COMMENT '结算单号',
    patient_name     VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    item_name        VARCHAR(128) DEFAULT NULL COMMENT '费用项目',
    category         VARCHAR(32)  DEFAULT NULL COMMENT '类别',
    pay_class        VARCHAR(8)   DEFAULT NULL COMMENT '支付类别（甲/乙/丙）',
    amount           DECIMAL(12,2) DEFAULT 0 COMMENT '金额',
    reason           VARCHAR(255) DEFAULT NULL COMMENT '审核要点',
    status           VARCHAR(16)  DEFAULT '待审核' COMMENT '状态（待审核/已通过/已驳回/已申诉/复审通过/复审维持/已合议）',
    appeal_reason    VARCHAR(255) DEFAULT NULL COMMENT '申诉理由（机构对被驳回费用提起申诉）',
    review_result    VARCHAR(16)  DEFAULT NULL COMMENT '复审结论（复审通过/复审维持）',
    review_opinion   VARCHAR(255) DEFAULT NULL COMMENT '复审意见',
    confer_opinion   VARCHAR(255) DEFAULT NULL COMMENT '合议意见（终裁）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_audit_bill (bill_id),
    KEY idx_audit_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用审核任务';

-- 异地就医备案
CREATE TABLE IF NOT EXISTS mis_filing (
    id               BIGINT       NOT NULL COMMENT '主键',
    name             VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    id_card_mask     VARCHAR(32)  DEFAULT NULL COMMENT '脱敏身份证号',
    insurance_area   VARCHAR(64)  DEFAULT NULL COMMENT '参保地',
    hospital         VARCHAR(128) DEFAULT NULL COMMENT '就医医院',
    reason           VARCHAR(32)  DEFAULT NULL COMMENT '备案原因',
    from_area        VARCHAR(64)  DEFAULT NULL COMMENT '就医地',
    valid_start      DATE         DEFAULT NULL COMMENT '生效日',
    valid_end        DATE         DEFAULT NULL COMMENT '失效日',
    status           VARCHAR(16)  DEFAULT '审核中' COMMENT '状态（已备案/审核中/已失效）',
    apply_time       DATE         DEFAULT NULL COMMENT '申请日期',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_filing_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异地就医备案';

-- 清算对账（按月）
CREATE TABLE IF NOT EXISTS mis_recon (
    id               BIGINT       NOT NULL COMMENT '主键',
    period           VARCHAR(8)   DEFAULT NULL COMMENT '清算账期（yyyy-MM）',
    bill_count       INT          DEFAULT 0 COMMENT '结算笔数',
    settle_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '申报结算额',
    insurance_pay    DECIMAL(14,2) DEFAULT 0 COMMENT '统筹申报支付',
    received_from_fund DECIMAL(14,2) DEFAULT 0 COMMENT '基金实际到账',
    diff             DECIMAL(14,2) DEFAULT 0 COMMENT '差额',
    status           VARCHAR(16)  DEFAULT '待对账' COMMENT '状态（待对账/已对账/存在差异）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_recon_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='清算对账';

-- =====================================================================
-- 系统基础（仿 HIS 01-系统基础：组织·用户·岗位·权限·参数·字典）
-- 独立库 mis_insurance 内使用 mss_ 前缀；删除均走软删（del_flag），
-- 岗位/角色/菜单这类唯一键不含 del_flag 的关联表删除走物理删。
-- =====================================================================

-- 经办科室（平台内部科室，岗位 = 角色 × 科室 中的“科室”）
CREATE TABLE IF NOT EXISTS sys_department (
    id               BIGINT       NOT NULL COMMENT '主键ID',
    dept_code        VARCHAR(32)  NOT NULL COMMENT '科室编码（唯一）',
    dept_name        VARCHAR(100) NOT NULL COMMENT '科室名称',
    dept_type        TINYINT      DEFAULT 1 COMMENT '科室类型（1-前台经办 2-审核稽核 3-基金财务 4-综合管理）',
    parent_id        BIGINT       NOT NULL DEFAULT 0 COMMENT '父科室ID',
    sort_order       INT          DEFAULT 0 COMMENT '排序号',
    dept_leader_id   BIGINT       DEFAULT NULL COMMENT '科室负责人（sys_employee.id）',
    phone            VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    email            VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status           TINYINT      DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    del_flag         TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经办科室';

-- 角色
CREATE TABLE IF NOT EXISTS sys_role (
    id           BIGINT       NOT NULL COMMENT '主键ID',
    role_code    VARCHAR(32)  NOT NULL COMMENT '角色编码（唯一）',
    role_name    VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_type    TINYINT      DEFAULT 1 COMMENT '角色类型（1-系统角色 2-自定义角色）',
    data_scope   TINYINT      DEFAULT 1 COMMENT '数据权限范围（1-全部数据 2-自定义数据 3-本部门数据 5-仅本人数据）',
    sort_order   INT          DEFAULT 0 COMMENT '排序号',
    status       TINYINT      DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    del_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- 菜单
CREATE TABLE IF NOT EXISTS sys_menu (
    id           BIGINT       NOT NULL COMMENT '主键ID',
    menu_name    VARCHAR(100) NOT NULL COMMENT '菜单名称',
    parent_id    BIGINT       DEFAULT 0 COMMENT '父菜单ID',
    sort_order   INT          DEFAULT 0 COMMENT '排序号',
    menu_type    TINYINT      DEFAULT 2 COMMENT '菜单类型（1-目录 2-菜单 3-按钮）',
    path         VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    component    VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    menu_key     VARCHAR(100) DEFAULT NULL COMMENT '菜单标识（唯一）',
    icon         VARCHAR(100) DEFAULT NULL COMMENT '图标',
    permission   VARCHAR(200) DEFAULT NULL COMMENT '权限标识',
    is_frame     TINYINT      DEFAULT 0 COMMENT '是否外链（0-否 1-是）',
    is_cache     TINYINT      DEFAULT 0 COMMENT '是否缓存（0-否 1-是）',
    is_visible   TINYINT      DEFAULT 1 COMMENT '是否可见（0-隐藏 1-显示）',
    status       TINYINT      DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    del_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_key (menu_key),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单';

-- 角色菜单关联（本表无 del_flag，删除走物理删）
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id           BIGINT      NOT NULL COMMENT '主键ID',
    role_id      BIGINT      NOT NULL COMMENT '角色ID',
    menu_id      BIGINT      NOT NULL COMMENT '菜单ID',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id                   BIGINT       NOT NULL COMMENT '主键ID',
    user_name            VARCHAR(64)  NOT NULL COMMENT '用户名（唯一）',
    password             VARCHAR(200) NOT NULL COMMENT '密码（BCrypt）',
    real_name            VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    emp_id               BIGINT       DEFAULT NULL COMMENT '关联员工ID',
    user_type            TINYINT      DEFAULT 1 COMMENT '用户类型（1-系统用户 2-外部用户）',
    avatar               VARCHAR(200) DEFAULT NULL COMMENT '头像地址',
    last_login_time      DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip        VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
    login_count          INT          DEFAULT 0 COMMENT '登录次数',
    password_update_time DATETIME     DEFAULT NULL COMMENT '密码更新时间',
    status               TINYINT      DEFAULT 1 COMMENT '状态（0-停用 1-启用 2-锁定）',
    del_flag             TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark               VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_name),
    KEY idx_emp_id (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 经办人员（员工）
CREATE TABLE IF NOT EXISTS sys_employee (
    id           BIGINT      NOT NULL COMMENT '主键ID',
    emp_code     VARCHAR(32) NOT NULL COMMENT '员工编号（唯一）',
    emp_name     VARCHAR(50) NOT NULL COMMENT '员工姓名',
    emp_type     TINYINT     NOT NULL DEFAULT 1 COMMENT '员工类型（1-经办员 2-审核员 3-管理员 4-其他）',
    gender       TINYINT     DEFAULT NULL COMMENT '性别（1-男 2-女 3-未知）',
    birth_date   DATE        DEFAULT NULL COMMENT '出生日期',
    hire_date    DATE        DEFAULT NULL COMMENT '入职日期',
    id_card      VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    phone        VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    email        VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
    dept_id      BIGINT      DEFAULT NULL COMMENT '科室ID',
    dept_name    VARCHAR(200) DEFAULT NULL COMMENT '科室名称（快照）',
    title        VARCHAR(50) DEFAULT NULL COMMENT '职称',
    position     VARCHAR(50) DEFAULT NULL COMMENT '职位',
    education    VARCHAR(20) DEFAULT NULL COMMENT '学历',
    status       TINYINT     DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    del_flag     TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_code (emp_code),
    KEY idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经办人员';

-- 员工岗位（角色 × 科室；本表无 del_flag，删除走物理删）
CREATE TABLE IF NOT EXISTS sys_employee_post (
    id             BIGINT      NOT NULL COMMENT '主键ID',
    employee_id    BIGINT      NOT NULL COMMENT '员工ID',
    role_id        BIGINT      NOT NULL COMMENT '角色ID',
    dept_id        BIGINT      NOT NULL COMMENT '科室ID',
    is_primary     TINYINT     DEFAULT 0 COMMENT '是否主岗位（0-否 1-是）',
    effective_date DATE        DEFAULT NULL COMMENT '岗位生效日期',
    expire_date    DATE        DEFAULT NULL COMMENT '岗位失效日期',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_role_dept (employee_id, role_id, dept_id),
    KEY idx_employee_id (employee_id),
    KEY idx_dept_id (dept_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工岗位（角色×科室）';

-- 员工资格证书（仿 HIS biz_employee_qualification，经办员工执业/职业证书登记）
CREATE TABLE IF NOT EXISTS sys_employee_qualification (
    id          BIGINT       NOT NULL COMMENT '主键ID',
    employee_id BIGINT       NOT NULL COMMENT '员工ID',
    cert_type   VARCHAR(50)  NOT NULL COMMENT '证书类型（his_emp_cert_type 字典）',
    cert_no     VARCHAR(64)  NOT NULL COMMENT '证书编号',
    issue_org   VARCHAR(100) DEFAULT NULL COMMENT '发证机关（his_emp_cert_org 字典）',
    issue_date  DATE         DEFAULT NULL COMMENT '发证日期',
    valid_until DATE         DEFAULT NULL COMMENT '有效期至（空=长期有效）',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    del_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工资格证书';

-- 系统参数
CREATE TABLE IF NOT EXISTS sys_config (
    config_id    BIGINT      NOT NULL COMMENT '配置ID',
    config_name  VARCHAR(64) NOT NULL COMMENT '配置名称',
    config_key   VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) DEFAULT NULL COMMENT '配置值',
    config_type  TINYINT     NOT NULL DEFAULT 1 COMMENT '类型（0-系统 1-业务）',
    is_system    TINYINT     NOT NULL DEFAULT 1 COMMENT '是否系统内置（0-否 1-是）',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

-- 字典类型
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id           BIGINT       NOT NULL COMMENT '主键ID',
    dict_type    VARCHAR(100) NOT NULL COMMENT '字典类型（唯一）',
    dict_name    VARCHAR(100) NOT NULL COMMENT '字典名称',
    status       TINYINT      DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    dict_source  TINYINT      DEFAULT 1 COMMENT '来源（1-系统级 2-自定义）',
    del_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型';

-- 字典数据
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id           BIGINT       NOT NULL COMMENT '主键ID',
    dict_type    VARCHAR(100) NOT NULL COMMENT '字典类型',
    dict_label   VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value   VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_sort    INT          DEFAULT 0 COMMENT '排序号',
    dict_class   VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
    list_class   VARCHAR(100) DEFAULT NULL COMMENT '表格回显样式',
    is_default   TINYINT      DEFAULT 0 COMMENT '是否默认（0-否 1-是）',
    status       TINYINT      DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    dict_source  TINYINT      DEFAULT 1 COMMENT '来源（1-系统级 2-自定义）',
    del_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常 1-删除）',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据';

-- =====================================================================
-- 基金监管（医保局端监管侧：规则引擎 / 疑点稽核 / 飞检 / 违规追回）
-- =====================================================================

-- 监管规则
CREATE TABLE IF NOT EXISTS mif_audit_rule (
    id            BIGINT       NOT NULL COMMENT '主键',
    rule_code     VARCHAR(32)  NOT NULL COMMENT '规则编码（唯一）',
    rule_name     VARCHAR(64)  NOT NULL COMMENT '规则名称',
    rule_type     VARCHAR(32)  NOT NULL COMMENT '规则类型（duplicate_charge-重复收费 over_limit_price-超限价收费 split_hospitalization-分解住院 bed_hanging-挂床住院 abnormal_cost-高额住院 payclass_c-丙类项目入统筹）',
    target        INT          DEFAULT 1 COMMENT '稽核对象（1-经办结算单 2-机构报盘）',
    severity      INT          DEFAULT 1 COMMENT '违规级别（1-一般 2-较重 3-严重）',
    params        VARCHAR(500) DEFAULT NULL COMMENT '规则参数（JSON）',
    description   VARCHAR(500) DEFAULT NULL COMMENT '规则说明',
    status        INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管规则';

-- 疑点记录（规则引擎扫描结算数据产出）
CREATE TABLE IF NOT EXISTS mif_suspicion (
    id               BIGINT       NOT NULL COMMENT '主键',
    suspicion_no     VARCHAR(32)  NOT NULL COMMENT '疑点单号（唯一）',
    rule_id          BIGINT       DEFAULT NULL COMMENT '命中规则id',
    rule_name        VARCHAR(64)  DEFAULT NULL COMMENT '命中规则名称',
    rule_type        VARCHAR(32)  DEFAULT NULL COMMENT '规则类型',
    biz_source       INT          DEFAULT 1 COMMENT '疑点来源（1-经办结算单 2-机构报盘）',
    biz_id           BIGINT       DEFAULT NULL COMMENT '来源单据id',
    biz_no           VARCHAR(64)  DEFAULT NULL COMMENT '来源单据号',
    patient_name     VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    id_card_mask     VARCHAR(32)  DEFAULT NULL COMMENT '脱敏身份证号',
    insurance_type   VARCHAR(16)  DEFAULT NULL COMMENT '参保类型',
    encounter_type   VARCHAR(16)  DEFAULT NULL COMMENT '就诊类型（门诊/住院）',
    dept             VARCHAR(64)  DEFAULT NULL COMMENT '科室',
    doctor           VARCHAR(64)  DEFAULT NULL COMMENT '医师',
    biz_date         DATE         DEFAULT NULL COMMENT '业务日期',
    involved_amount  DECIMAL(12,2) DEFAULT 0 COMMENT '疑点涉及金额',
    reason           VARCHAR(500) DEFAULT NULL COMMENT '疑点说明',
    status           VARCHAR(16)  DEFAULT '待核实' COMMENT '状态（待核实/已确认/已排除）',
    confirm_amount   DECIMAL(12,2) DEFAULT 0 COMMENT '确认违规金额',
    handle_user      VARCHAR(64)  DEFAULT NULL COMMENT '处置人',
    handle_time      DATETIME     DEFAULT NULL COMMENT '处置时间',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_susp_no (suspicion_no),
    UNIQUE KEY uk_susp_biz_rule (biz_source, biz_id, rule_id),
    KEY idx_susp_rule (rule_id),
    KEY idx_susp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='疑点记录';

-- 飞检任务
CREATE TABLE IF NOT EXISTS mif_inspection (
    id             BIGINT       NOT NULL COMMENT '主键',
    inspection_no  VARCHAR(32)  NOT NULL COMMENT '任务单号（唯一）',
    title          VARCHAR(128) DEFAULT NULL COMMENT '任务名称',
    inspect_type   VARCHAR(16)  DEFAULT NULL COMMENT '检查类型（专项检查/日常巡查/举报核查）',
    target_org     VARCHAR(128) DEFAULT NULL COMMENT '被查机构',
    plan_start     DATE         DEFAULT NULL COMMENT '计划开始日',
    plan_end       DATE         DEFAULT NULL COMMENT '计划结束日',
    leader         VARCHAR(64)  DEFAULT NULL COMMENT '带队人',
    members        VARCHAR(200) DEFAULT NULL COMMENT '检查组成员',
    scope_desc     VARCHAR(500) DEFAULT NULL COMMENT '检查范围',
    status         VARCHAR(16)  DEFAULT '草稿' COMMENT '状态（草稿/待开展/检查中/已完成）',
    result_summary VARCHAR(1000) DEFAULT NULL COMMENT '检查结果小结',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ins_no (inspection_no),
    KEY idx_ins_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞检任务';

-- 飞检发现问题明细
CREATE TABLE IF NOT EXISTS mif_inspection_item (
    id              BIGINT       NOT NULL COMMENT '主键',
    inspection_id   BIGINT       NOT NULL COMMENT '任务id',
    problem_type    VARCHAR(32)  DEFAULT NULL COMMENT '问题类型',
    problem_desc    VARCHAR(500) DEFAULT NULL COMMENT '问题描述',
    evidence        VARCHAR(500) DEFAULT NULL COMMENT '证据说明',
    involved_amount DECIMAL(12,2) DEFAULT 0 COMMENT '涉及金额',
    dispose         VARCHAR(16)  DEFAULT NULL COMMENT '处置方式（整改/追回/移交稽核）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_item_inspection (inspection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞检发现问题明细';

-- 违规追回单
CREATE TABLE IF NOT EXISTS mif_recover (
    id               BIGINT       NOT NULL COMMENT '主键',
    recover_no       VARCHAR(32)  NOT NULL COMMENT '追回单号（唯一）',
    source_type      INT          DEFAULT 1 COMMENT '来源类型（1-疑点稽核 2-飞检）',
    source_no        VARCHAR(64)  DEFAULT NULL COMMENT '来源单号',
    target_org_code  VARCHAR(32)  DEFAULT NULL COMMENT '定点机构编码',
    target_org_name  VARCHAR(128) DEFAULT NULL COMMENT '定点机构名称',
    month            VARCHAR(7)   DEFAULT NULL COMMENT '所属账期（yyyy-MM）',
    recover_amount   DECIMAL(12,2) DEFAULT 0 COMMENT '追回金额',
    reason           VARCHAR(500) DEFAULT NULL COMMENT '追回原因',
    status           VARCHAR(16)  DEFAULT '待追回' COMMENT '状态（待追回/已追回/已核销）',
    recover_time     DATETIME     DEFAULT NULL COMMENT '追回时间',
    handler          VARCHAR(64)  DEFAULT NULL COMMENT '经办人',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_recover_no (recover_no),
    KEY idx_recover_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规追回单';

-- 基金监管知识库（两库之"知识库"：临床指南 / 医学知识 / 药品知识 / 政策依据）
CREATE TABLE IF NOT EXISTS mif_knowledge (
    id           BIGINT       NOT NULL COMMENT '主键',
    kb_code      VARCHAR(64)  DEFAULT NULL COMMENT '知识编码（唯一）',
    kb_name      VARCHAR(200) DEFAULT NULL COMMENT '知识标题',
    kb_type      VARCHAR(32)  DEFAULT NULL COMMENT '知识类型（clinical_guideline-临床指南 medical_knowledge-医学知识 drug_knowledge-药品知识 policy-政策依据）',
    content      TEXT         COMMENT '知识内容 / 规则要点',
    source       VARCHAR(200) DEFAULT NULL COMMENT '来源（临床诊疗规范 / 药品说明书 / 医保政策文件）',
    status       INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_code (kb_code),
    KEY idx_kb_type (kb_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金监管知识库（两库之知识库）';

-- 日拨付（按笔）批次
CREATE TABLE IF NOT EXISTS mis_daily_alloc (
    id           BIGINT       NOT NULL COMMENT '主键',
    biz_date     VARCHAR(10)  DEFAULT NULL COMMENT '业务日期（yyyy-MM-dd）',
    batch_no     VARCHAR(32)  DEFAULT NULL COMMENT '日拨付批次号（YBD + yyyyMMdd）',
    settle_count INT          DEFAULT 0 COMMENT '当日结算笔数',
    total_fund   DECIMAL(12,2) DEFAULT 0 COMMENT '当日医保基金支付总额（申报统筹）',
    pay_amount   DECIMAL(12,2) DEFAULT 0 COMMENT '本次按笔拨付金额（total_fund * 日拨付比例）',
    remit_status INT          DEFAULT 0 COMMENT '回单状态（0-待拨付 1-已回单）',
    receipt_no   VARCHAR(64)  DEFAULT NULL COMMENT '银行电子回单编号',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_batch (batch_no),
    KEY idx_daily_bizdate (biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日拨付（按笔）批次';

-- 定点医药机构协议
CREATE TABLE IF NOT EXISTS mis_agreement (
    id               BIGINT       NOT NULL COMMENT '主键',
    medins_code      VARCHAR(32)  DEFAULT NULL COMMENT '定点机构编码（唯一）',
    medins_name      VARCHAR(128) DEFAULT NULL COMMENT '定点机构名称',
    medins_type      INT          DEFAULT 1 COMMENT '机构类型（1-定点医院 2-定点药店）',
    agreement_no     VARCHAR(64)  DEFAULT NULL COMMENT '协议编号',
    agreement_status INT          DEFAULT 0 COMMENT '协议状态（0-申请中 1-已签订 2-已解除）',
    sign_date        DATE         DEFAULT NULL COMMENT '签订日期',
    expire_date      DATE         DEFAULT NULL COMMENT '到期日期',
    service_scope    VARCHAR(255) DEFAULT NULL COMMENT '服务范围',
    annual_score     INT          DEFAULT NULL COMMENT '年度考核得分',
    remark           VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_agreement_medins (medins_code),
    KEY idx_agreement_status (agreement_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定点医药机构协议';

-- 手工（零星）报销
CREATE TABLE IF NOT EXISTS mis_manual_claim (
    id               BIGINT       NOT NULL COMMENT '主键',
    claim_no         VARCHAR(64)  DEFAULT NULL COMMENT '报销单号',
    person_name      VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    id_card          VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
    insurance_type   VARCHAR(16)  DEFAULT NULL COMMENT '险种（2-城镇职工医保 3-城乡居民医保）',
    claim_type       INT          DEFAULT 1 COMMENT '报销类型（1-门诊 2-住院 3-药店）',
    visit_date       DATE         DEFAULT NULL COMMENT '就诊日期',
    total_amount     DECIMAL(12,2) DEFAULT 0 COMMENT '费用总额',
    insurance_pay    DECIMAL(12,2) DEFAULT 0 COMMENT '统筹支付（审核核定）',
    personal_pay    DECIMAL(12,2) DEFAULT 0 COMMENT '个人自付',
    status           INT          DEFAULT 0 COMMENT '状态（0-待审核 1-已审核 2-已拨付 3-已退回）',
    apply_reason     VARCHAR(255) DEFAULT NULL COMMENT '申请事由',
    audit_opinion    VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
    remark           VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_claim_status (status),
    KEY idx_claim_type (claim_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手工（零星）报销';

-- 医保信用管理
CREATE TABLE IF NOT EXISTS mif_credit (
    id               BIGINT       NOT NULL COMMENT '主键',
    subject_type     INT          DEFAULT 1 COMMENT '主体类型（1-定点机构 2-医药从业人员）',
    subject_code     VARCHAR(64)  DEFAULT NULL COMMENT '主体编码（机构编码 / 人员工号）',
    subject_name     VARCHAR(128) DEFAULT NULL COMMENT '主体名称',
    credit_level     VARCHAR(8)   DEFAULT NULL COMMENT '信用等级（A-优秀 B-良好 C-一般 D-差）',
    score            INT          DEFAULT 0 COMMENT '信用评分',
    violation_count  INT          DEFAULT 0 COMMENT '违规记分次数',
    punish_type      INT          DEFAULT 0 COMMENT '奖惩类型（0-无 1-守信激励 2-失信惩戒）',
    remark           VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_credit_subject (subject_type, subject_code),
    KEY idx_credit_punish (punish_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保信用管理';

-- 大数据反欺诈模型库
CREATE TABLE IF NOT EXISTS mif_fraud_model (
    id            BIGINT       NOT NULL COMMENT '主键',
    model_code    VARCHAR(64)  NOT NULL COMMENT '模型编码',
    model_name    VARCHAR(128) NOT NULL COMMENT '模型名称',
    category      VARCHAR(16)  DEFAULT NULL COMMENT '欺诈类别（01-分解住院 02-虚假就医/冒名 03-串换药品 04-过度医疗 05-空刷医保卡 06-其他）',
    risk_level    INT          DEFAULT 2 COMMENT '风险等级（1-低 2-中 3-高）',
    algorithm     VARCHAR(255) DEFAULT NULL COMMENT '算法/规则描述',
    params        TEXT         COMMENT '模型参数（JSON）',
    status        INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    description   VARCHAR(500) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fraud_model_code (model_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大数据反欺诈模型';

-- 反欺诈线索（模型命中后生成）
CREATE TABLE IF NOT EXISTS mif_fraud_clue (
    id              BIGINT       NOT NULL COMMENT '主键',
    clue_no         VARCHAR(64)  NOT NULL COMMENT '线索编号',
    model_id        BIGINT       DEFAULT NULL COMMENT '来源模型ID',
    model_code      VARCHAR(64)  DEFAULT NULL COMMENT '来源模型编码',
    model_name      VARCHAR(128) DEFAULT NULL COMMENT '来源模型名称',
    medins_code     VARCHAR(64)  DEFAULT NULL COMMENT '涉及机构编码',
    medins_name     VARCHAR(128) DEFAULT NULL COMMENT '涉及机构名称',
    insured_id_card VARCHAR(32)  DEFAULT NULL COMMENT '涉及参保人身份证',
    insured_name    VARCHAR(64)  DEFAULT NULL COMMENT '涉及参保人姓名',
    biz_no          VARCHAR(64)  DEFAULT NULL COMMENT '关联业务单号',
    involve_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '涉及金额',
    risk_level      INT          DEFAULT 2 COMMENT '风险等级（1-低 2-中 3-高）',
    clue_detail     TEXT         COMMENT '线索详情',
    status          INT          DEFAULT 1 COMMENT '状态（1-待核查 2-已确认 3-已排除 4-已移送）',
    handle_user     VARCHAR(64)  DEFAULT NULL COMMENT '核查人',
    handle_time     DATETIME     DEFAULT NULL COMMENT '核查时间',
    handle_opinion  VARCHAR(500) DEFAULT NULL COMMENT '核查/处置意见',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fraud_clue_no (clue_no),
    KEY idx_fraud_clue_status (status),
    KEY idx_fraud_clue_medins (medins_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反欺诈线索';

-- 事前提醒 / 事中审核拦截规则（三道防线之前两道）
CREATE TABLE IF NOT EXISTS mif_precheck_rule (
    id            BIGINT       NOT NULL COMMENT '主键',
    rule_code     VARCHAR(64)  NOT NULL COMMENT '规则编码',
    rule_name     VARCHAR(128) NOT NULL COMMENT '规则名称',
    trigger_stage VARCHAR(16)  DEFAULT 'before_settle' COMMENT '触发阶段（before_settle-事前提醒 in_settle-事中拦截）',
    risk_level    INT          DEFAULT 2 COMMENT '风险等级（1-低 2-中 3-高）',
    prompt_text   VARCHAR(255) DEFAULT NULL COMMENT '提醒/拦截提示语',
    block_flag    INT          DEFAULT 0 COMMENT '是否阻断（1-阻断结算 0-仅提醒）',
    category      VARCHAR(32)  DEFAULT NULL COMMENT '类别（冒名/超量开药/超限价/重复收费等）',
    params        TEXT         COMMENT '规则参数（JSON，如 {"threshold":5,"windowDays":30}）',
    status        INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    description   VARCHAR(500) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_precheck_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事前提醒/事中拦截规则';

-- 事前/事中规则命中结果
CREATE TABLE IF NOT EXISTS mif_precheck_result (
    id            BIGINT       NOT NULL COMMENT '主键',
    rule_id       BIGINT       DEFAULT NULL COMMENT '规则ID',
    rule_code     VARCHAR(64)  DEFAULT NULL COMMENT '规则编码',
    rule_name     VARCHAR(128) DEFAULT NULL COMMENT '规则名称',
    trigger_stage VARCHAR(16)  DEFAULT NULL COMMENT '触发阶段',
    biz_no        VARCHAR(64)  DEFAULT NULL COMMENT '业务单号',
    medins_code   VARCHAR(64)  DEFAULT NULL COMMENT '机构编码',
    medins_name   VARCHAR(128) DEFAULT NULL COMMENT '机构名称',
    insured_id_card VARCHAR(32) DEFAULT NULL COMMENT '参保人身份证',
    insured_name  VARCHAR(64)  DEFAULT NULL COMMENT '参保人姓名',
    prompt_text   VARCHAR(255) DEFAULT NULL COMMENT '提示语',
    block_flag    INT          DEFAULT 0 COMMENT '是否阻断（1-阻断 0-提醒）',
    risk_level    INT          DEFAULT 2 COMMENT '风险等级（1-低 2-中 3-高）',
    handle_status INT          DEFAULT 0 COMMENT '处理状态（0-待处理 1-已处理）',
    handle_opinion VARCHAR(255) DEFAULT NULL COMMENT '处理意见',
    hit_time      DATETIME     DEFAULT NULL COMMENT '命中时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_precheck_result_stage (trigger_stage),
    KEY idx_precheck_result_handle (handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事前/事中命中结果';

-- 举报投诉与奖励
CREATE TABLE IF NOT EXISTS mif_report (
    id                  BIGINT       NOT NULL COMMENT '主键',
    report_no           VARCHAR(64)  NOT NULL COMMENT '举报编号',
    reporter_name       VARCHAR(64)  DEFAULT NULL COMMENT '举报人',
    reporter_contact    VARCHAR(64)  DEFAULT NULL COMMENT '举报人联系方式',
    reported_medins_code VARCHAR(64) DEFAULT NULL COMMENT '被举报机构编码',
    reported_medins_name VARCHAR(128) DEFAULT NULL COMMENT '被举报机构名称',
    reported_subject    VARCHAR(128) DEFAULT NULL COMMENT '被举报对象（人员）',
    problem_type        VARCHAR(32)  DEFAULT NULL COMMENT '问题类型（违规收费/欺诈骗保/服务态度/其他）',
    content             TEXT         COMMENT '举报内容',
    attach_url          VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    status              INT          DEFAULT 1 COMMENT '状态（1-待受理 2-核查中 3-已办结 4-已奖励发放）',
    handle_user         VARCHAR(64)  DEFAULT NULL COMMENT '经办人',
    handle_opinion      VARCHAR(500) DEFAULT NULL COMMENT '核查/处置意见',
    reward_amount       DECIMAL(12,2) DEFAULT 0 COMMENT '奖励金额',
    handle_time         DATETIME     DEFAULT NULL COMMENT '处置时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_no (report_no),
    KEY idx_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报投诉与奖励';

-- =====================================================================
-- DRG/DIP 支付方式监管
-- =====================================================================

-- 病组 / 病种分值池
CREATE TABLE IF NOT EXISTS mif_drg_group (
    id            BIGINT       NOT NULL COMMENT '主键',
    group_code    VARCHAR(64)  NOT NULL COMMENT '病组/病种编码',
    group_name    VARCHAR(128) NOT NULL COMMENT '病组/病种名称',
    pay_type      INT          DEFAULT 1 COMMENT '支付方式（1-DRG按病组付费 2-DIP按病种分值付费）',
    weight        DECIMAL(10,2) DEFAULT 0 COMMENT 'DRG权重',
    rate          DECIMAL(12,2) DEFAULT 0 COMMENT 'DRG费率（元/权重）',
    point_value   DECIMAL(12,2) DEFAULT 0 COMMENT 'DIP分值',
    point_cost    DECIMAL(12,2) DEFAULT 0 COMMENT 'DIP每分值成本（元/分）',
    avg_cost      DECIMAL(12,2) DEFAULT 0 COMMENT '组均次费用（入组匹配依据）',
    case_count    INT          DEFAULT 0 COMMENT '病例数',
    status        INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    description   VARCHAR(500) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_drg_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DRG病组/DIP病种分值池';

-- 病例入组与支付监管
CREATE TABLE IF NOT EXISTS mif_drg_case (
    id               BIGINT       NOT NULL COMMENT '主键',
    case_no          VARCHAR(64)  NOT NULL COMMENT '病例号（结算单号）',
    bill_id          BIGINT       DEFAULT NULL COMMENT '关联结算单ID',
    bill_no          VARCHAR(64)  DEFAULT NULL COMMENT '结算单号',
    patient_name     VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    id_card_mask     VARCHAR(32)  DEFAULT NULL COMMENT '身份证掩码',
    insurance_type   VARCHAR(32)  DEFAULT NULL COMMENT '险种',
    pay_type         INT          DEFAULT 1 COMMENT '支付方式（1-DRG 2-DIP）',
    group_code       VARCHAR(64)  DEFAULT NULL COMMENT '病组/病种编码',
    group_name       VARCHAR(128) DEFAULT NULL COMMENT '病组/病种名称',
    weight           DECIMAL(12,2) DEFAULT 0 COMMENT '权重/分值',
    est_cost         DECIMAL(12,2) DEFAULT 0 COMMENT '预估支付标准',
    actual_cost      DECIMAL(12,2) DEFAULT 0 COMMENT '实际费用',
    deviation_rate   DECIMAL(10,4) DEFAULT 0 COMMENT '偏差率（实际/预估-1）',
    status           INT          DEFAULT 1 COMMENT '状态（1-正常 2-偏差预警 3-费用超支 4-特例单议）',
    deviation_reason VARCHAR(500) DEFAULT NULL COMMENT '偏差原因/监管意见',
    case_date        DATE         DEFAULT NULL COMMENT '结算日期',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_drg_case_no (case_no),
    KEY idx_drg_case_status (status),
    KEY idx_drg_case_group (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DRG/DIP病例入组与支付监管';

-- =====================================================================
-- 双随机一公开 / 机构自查自纠
-- =====================================================================

-- 双随机抽查任务
CREATE TABLE IF NOT EXISTS mif_random_task (
    id                BIGINT       NOT NULL COMMENT '主键',
    task_no           VARCHAR(64)  NOT NULL COMMENT '任务编号',
    task_name         VARCHAR(128) NOT NULL COMMENT '任务名称',
    check_type        INT          DEFAULT 1 COMMENT '检查类型（1-日常检查 2-专项检查 3-监督抽查）',
    plan_count        INT          DEFAULT 0 COMMENT '计划抽取机构数',
    plan_checker_count INT         DEFAULT 0 COMMENT '计划抽取检查人员数',
    draw_time         DATETIME     DEFAULT NULL COMMENT '抽取时间',
    status            INT          DEFAULT 1 COMMENT '状态（1-已发布 2-执行中 3-已完成）',
    dept_name         VARCHAR(64)  DEFAULT NULL COMMENT '负责部门',
    remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_random_task_no (task_no),
    KEY idx_random_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双随机抽查任务';

-- 双随机抽取结果（含公开状态）
CREATE TABLE IF NOT EXISTS mif_random_result (
    id            BIGINT       NOT NULL COMMENT '主键',
    task_id       BIGINT       DEFAULT NULL COMMENT '任务ID',
    target_type   INT          DEFAULT 1 COMMENT '抽取对象类型（1-被检查机构 2-检查人员）',
    target_code   VARCHAR(64)  DEFAULT NULL COMMENT '对象编码',
    target_name   VARCHAR(128) DEFAULT NULL COMMENT '对象名称',
    org_name      VARCHAR(128) DEFAULT NULL COMMENT '所属机构/部门',
    draw_time     DATETIME     DEFAULT NULL COMMENT '抽取时间',
    result        INT          DEFAULT 1 COMMENT '检查结果（1-待检查 2-正常 3-存在问题 4-已整改）',
    check_result  VARCHAR(500) DEFAULT NULL COMMENT '检查情况说明',
    public_flag   INT          DEFAULT 0 COMMENT '是否公开（0-未公开 1-已公开）',
    public_time   DATETIME     DEFAULT NULL COMMENT '公开时间',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_random_result_task (task_id),
    KEY idx_random_result_type (target_type),
    KEY idx_random_result_public (public_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双随机抽取结果与公开';

-- 机构自查自纠任务
CREATE TABLE IF NOT EXISTS mif_self_check (
    id              BIGINT       NOT NULL COMMENT '主键',
    task_no         VARCHAR(64)  NOT NULL COMMENT '任务编号',
    medins_code     VARCHAR(64)  DEFAULT NULL COMMENT '定点机构编码',
    medins_name     VARCHAR(128) NOT NULL COMMENT '定点机构名称',
    task_name       VARCHAR(128) NOT NULL COMMENT '任务名称',
    check_scope     VARCHAR(500) DEFAULT NULL COMMENT '自查范围',
    start_date      DATE         DEFAULT NULL COMMENT '下发日期',
    deadline        DATE         DEFAULT NULL COMMENT '整改截止日期',
    status          INT          DEFAULT 1 COMMENT '状态（1-待自查 2-自查中 3-已上报 4-整改中 5-已办结）',
    submit_content  TEXT         COMMENT '自查上报内容',
    submit_time     DATETIME     DEFAULT NULL COMMENT '上报时间',
    rectify_content TEXT         COMMENT '整改内容',
    rectify_time    DATETIME     DEFAULT NULL COMMENT '整改完成时间',
    reviewer        VARCHAR(64)  DEFAULT NULL COMMENT '复核人',
    review_opinion  VARCHAR(500) DEFAULT NULL COMMENT '复核意见',
    review_time     DATETIME     DEFAULT NULL COMMENT '复核时间',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_self_check_task_no (task_no),
    KEY idx_self_check_status (status),
    KEY idx_self_check_medins (medins_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构自查自纠任务';

-- =====================================================================
-- 医保关系转移接续 / 个人账户管理 / 门诊慢特病认定
-- =====================================================================

-- 医保关系转移接续
CREATE TABLE IF NOT EXISTS mis_transfer (
    id             BIGINT       NOT NULL COMMENT '主键',
    transfer_no    VARCHAR(64)  NOT NULL COMMENT '转移单号',
    person_name    VARCHAR(64)  NOT NULL COMMENT '参保人姓名',
    id_card        VARCHAR(32)  NOT NULL COMMENT '身份证号',
    insurance_no   VARCHAR(64)  DEFAULT NULL COMMENT '医保卡号',
    transfer_type  INT          DEFAULT 1 COMMENT '转移类型（1-省内转移 2-跨省转移 3-转出 4-转入）',
    from_region    VARCHAR(64)  DEFAULT NULL COMMENT '转出统筹区',
    to_region      VARCHAR(64)  DEFAULT NULL COMMENT '转入统筹区',
    apply_date     DATE         DEFAULT NULL COMMENT '申请日期',
    status         INT          DEFAULT 1 COMMENT '状态（1-待受理 2-已受理 3-已转出 4-已转入 5-已完成）',
    pay_years      DECIMAL(6,1) DEFAULT 0 COMMENT '累计缴费年限（核定）',
    fund_balance   DECIMAL(12,2) DEFAULT 0 COMMENT '结转个人账户余额',
    transfer_time  DATETIME     DEFAULT NULL COMMENT '转移完成时间',
    handler        VARCHAR(64)  DEFAULT NULL COMMENT '经办人',
    handle_opinion VARCHAR(500) DEFAULT NULL COMMENT '经办意见',
    remark         VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_no (transfer_no),
    KEY idx_transfer_idcard (id_card),
    KEY idx_transfer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保关系转移接续';

-- 个人账户流水
CREATE TABLE IF NOT EXISTS mis_account_txn (
    id             BIGINT       NOT NULL COMMENT '主键',
    txn_no         VARCHAR(64)  NOT NULL COMMENT '流水号',
    id_card        VARCHAR(32)  NOT NULL COMMENT '身份证号',
    person_name    VARCHAR(64)  DEFAULT NULL COMMENT '参保人姓名',
    txn_type       INT          NOT NULL COMMENT '交易类型（1-划入 2-结算划扣 3-一次性支取 4-转移出 5-转移入 6-清退 7-退保冲回）',
    amount         DECIMAL(12,2) DEFAULT 0 COMMENT '发生金额（支取为负）',
    balance_after  DECIMAL(12,2) DEFAULT 0 COMMENT '交易后余额',
    txn_time       DATETIME     DEFAULT NULL COMMENT '交易时间',
    reason         VARCHAR(255) DEFAULT NULL COMMENT '事由',
    ref_no         VARCHAR(64)  DEFAULT NULL COMMENT '关联单号',
    status         INT          DEFAULT 1 COMMENT '状态（1-正常 2-已冻结）',
    remark         VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_txn_no (txn_no),
    KEY idx_account_txn_idcard (id_card),
    KEY idx_account_txn_type (txn_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人账户流水';

-- 门诊慢特病待遇认定
CREATE TABLE IF NOT EXISTS mis_chronic (
    id             BIGINT       NOT NULL COMMENT '主键',
    record_no      VARCHAR(64)  NOT NULL COMMENT '认定单号',
    person_name    VARCHAR(64)  NOT NULL COMMENT '参保人姓名',
    id_card        VARCHAR(32)  NOT NULL COMMENT '身份证号',
    insurance_no   VARCHAR(64)  DEFAULT NULL COMMENT '医保卡号',
    disease_code   VARCHAR(64)  DEFAULT NULL COMMENT '病种编码',
    disease_name   VARCHAR(128) NOT NULL COMMENT '病种名称',
    disease_level  INT          DEFAULT 2 COMMENT '待遇类别（1-门诊特殊病种 2-门诊慢特病）',
    apply_date     DATE         DEFAULT NULL COMMENT '申请日期',
    doctor         VARCHAR(64)  DEFAULT NULL COMMENT '申请医师',
    hospital       VARCHAR(128) DEFAULT NULL COMMENT '定点医疗机构',
    status         INT          DEFAULT 1 COMMENT '状态（1-待审核 2-已认定 3-不通过 4-已复审）',
    valid_from     DATE         DEFAULT NULL COMMENT '待遇有效期起',
    valid_to       DATE         DEFAULT NULL COMMENT '待遇有效期止',
    reviewer       VARCHAR(64)  DEFAULT NULL COMMENT '审核人',
    review_opinion VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    review_time    DATETIME     DEFAULT NULL COMMENT '审核时间',
    remark         VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_chronic_record_no (record_no),
    KEY idx_chronic_idcard (id_card),
    KEY idx_chronic_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门诊慢特病待遇认定';

-- =====================================================================
-- 结算清单质控 / 药品追溯码监管
-- =====================================================================

-- 结算清单质控规则
CREATE TABLE IF NOT EXISTS mif_quality_rule (
    id            BIGINT       NOT NULL COMMENT '主键',
    rule_code     VARCHAR(64)  NOT NULL COMMENT '规则编码',
    rule_name     VARCHAR(128) NOT NULL COMMENT '规则名称',
    rule_type     VARCHAR(32)  NOT NULL COMMENT '校验类型（required_field/not_mapped/over_limit_price/invalid_qty/pay_class_missing/duplicate_report/amount_mismatch/catalog_disabled/self_ratio_missing/gender_limit）',
    severity      INT          DEFAULT 2 COMMENT '严重程度（1-提示 2-警示 3-严重）',
    deduct_ratio  DECIMAL(6,4) DEFAULT 0 COMMENT '扣款比例（0-1）',
    deduct_type   INT          DEFAULT 1 COMMENT '扣款方式（1-按比例 2-固定金额）',
    deduct_amount DECIMAL(12,2) DEFAULT 0 COMMENT '固定扣款金额',
    params        TEXT         COMMENT '规则参数（JSON）',
    status        INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    description   VARCHAR(500) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_quality_rule_code (rule_code),
    KEY idx_quality_rule_type (rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算清单质控规则';

-- 结算清单质控结果
CREATE TABLE IF NOT EXISTS mif_quality_result (
    id             BIGINT       NOT NULL COMMENT '主键',
    bill_id        BIGINT       DEFAULT NULL COMMENT '结算单ID',
    bill_no        VARCHAR(64)  DEFAULT NULL COMMENT '结算单号',
    item_id        BIGINT       DEFAULT NULL COMMENT '费用明细ID',
    item_name      VARCHAR(128) DEFAULT NULL COMMENT '项目名称',
    yb_code        VARCHAR(64)  DEFAULT NULL COMMENT '医保编码',
    medins_code    VARCHAR(64)  DEFAULT NULL COMMENT '定点机构编码',
    medins_name    VARCHAR(128) DEFAULT NULL COMMENT '定点机构名称',
    rule_id        BIGINT       DEFAULT NULL COMMENT '规则ID',
    rule_code      VARCHAR(64)  DEFAULT NULL COMMENT '规则编码',
    rule_name      VARCHAR(128) DEFAULT NULL COMMENT '规则名称',
    rule_type      VARCHAR(32)  DEFAULT NULL COMMENT '校验类型',
    severity       INT          DEFAULT 2 COMMENT '严重程度（1-提示 2-警示 3-严重）',
    field_name     VARCHAR(64)  DEFAULT NULL COMMENT '质控字段',
    origin_value   VARCHAR(255) DEFAULT NULL COMMENT '原始值',
    quality_desc   VARCHAR(500) DEFAULT NULL COMMENT '质控说明',
    involve_amount DECIMAL(12,2) DEFAULT 0 COMMENT '涉及金额',
    deduct_amount  DECIMAL(12,2) DEFAULT 0 COMMENT '建议扣款金额',
    handle_status  INT          DEFAULT 0 COMMENT '处理状态（0-待处理 1-已确认 2-已整改 3-已忽略）',
    handle_opinion VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    handle_user    VARCHAR(64)  DEFAULT NULL COMMENT '处理人',
    handle_time    DATETIME     DEFAULT NULL COMMENT '处理时间',
    check_time     DATETIME     DEFAULT NULL COMMENT '质控时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_quality_result_bill (bill_id),
    KEY idx_quality_result_rule (rule_code),
    KEY idx_quality_result_status (handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算清单质控结果';

-- 药品追溯码台账
CREATE TABLE IF NOT EXISTS mif_trace_code (
    id            BIGINT       NOT NULL COMMENT '主键',
    code          VARCHAR(64)  NOT NULL COMMENT '追溯码（唯一）',
    drug_name     VARCHAR(128) DEFAULT NULL COMMENT '药品名称',
    spec          VARCHAR(128) DEFAULT NULL COMMENT '规格',
    manufacturer  VARCHAR(128) DEFAULT NULL COMMENT '生产企业',
    batch_no      VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
    expire_date   DATE         DEFAULT NULL COMMENT '有效期',
    status        INT          DEFAULT 1 COMMENT '状态（1-未使用 2-已使用 3-已作废 4-已退回）',
    bill_id       BIGINT       DEFAULT NULL COMMENT '关联结算单ID',
    bill_no       VARCHAR(64)  DEFAULT NULL COMMENT '关联结算单号',
    item_id       BIGINT       DEFAULT NULL COMMENT '关联费用明细ID',
    item_name     VARCHAR(128) DEFAULT NULL COMMENT '关联项目名称',
    scan_user     VARCHAR(64)  DEFAULT NULL COMMENT '扫码人',
    scan_time     DATETIME     DEFAULT NULL COMMENT '扫码核销时间',
    verify_count  INT          DEFAULT 0 COMMENT '核验次数',
    remark        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trace_code (code),
    KEY idx_trace_code_drug (drug_name),
    KEY idx_trace_code_status (status),
    KEY idx_trace_code_bill (bill_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品追溯码台账';

-- 追溯码核验日志
CREATE TABLE IF NOT EXISTS mif_trace_verify_log (
    id            BIGINT       NOT NULL COMMENT '主键',
    code          VARCHAR(64)  DEFAULT NULL COMMENT '追溯码',
    bill_id       BIGINT       DEFAULT NULL COMMENT '关联结算单ID',
    bill_no       VARCHAR(64)  DEFAULT NULL COMMENT '关联结算单号',
    item_name     VARCHAR(128) DEFAULT NULL COMMENT '关联项目名称',
    drug_name     VARCHAR(128) DEFAULT NULL COMMENT '台账药品名称',
    verify_result INT          DEFAULT 1 COMMENT '核验结果（1-通过 2-不存在 3-重复使用 4-与项目不符 5-已过期 6-已作废）',
    problem_desc  VARCHAR(500) DEFAULT NULL COMMENT '问题描述',
    verifier      VARCHAR(64)  DEFAULT NULL COMMENT '核验人',
    verify_time   DATETIME     DEFAULT NULL COMMENT '核验时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_trace_log_result (verify_result),
    KEY idx_trace_log_bill (bill_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追溯码核验日志';

-- =====================================================================
-- 药品耗材进销存
-- =====================================================================

-- 库存主档
CREATE TABLE IF NOT EXISTS mis_stock_item (
    id            BIGINT       NOT NULL COMMENT '主键',
    item_code     VARCHAR(64)  NOT NULL COMMENT '品项编码',
    item_name     VARCHAR(128) NOT NULL COMMENT '品项名称',
    category      VARCHAR(32)  DEFAULT NULL COMMENT '类别（西药/中成药/中草药/医用耗材）',
    spec          VARCHAR(128) DEFAULT NULL COMMENT '规格',
    unit          VARCHAR(32)  DEFAULT NULL COMMENT '单位',
    yb_code       VARCHAR(64)  DEFAULT NULL COMMENT '医保编码',
    manufacturer  VARCHAR(128) DEFAULT NULL COMMENT '生产企业',
    stock_qty     DECIMAL(14,2) DEFAULT 0 COMMENT '当前库存数量',
    stock_amount  DECIMAL(16,2) DEFAULT 0 COMMENT '当前库存金额',
    avg_price     DECIMAL(14,4) DEFAULT 0 COMMENT '移动加权平均单价',
    warn_qty      DECIMAL(14,2) DEFAULT 0 COMMENT '库存预警下限',
    location      VARCHAR(64)  DEFAULT NULL COMMENT '库房/货位',
    status        INT          DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_item_code (item_code),
    KEY idx_stock_item_ybcode (yb_code),
    KEY idx_stock_item_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品耗材库存主档';

-- 进销存流水
CREATE TABLE IF NOT EXISTS mis_stock_txn (
    id            BIGINT       NOT NULL COMMENT '主键',
    txn_no        VARCHAR(64)  NOT NULL COMMENT '流水号',
    item_id       BIGINT       DEFAULT NULL COMMENT '库存主档ID',
    item_code     VARCHAR(64)  DEFAULT NULL COMMENT '品项编码',
    item_name     VARCHAR(128) DEFAULT NULL COMMENT '品项名称',
    txn_type      INT          NOT NULL COMMENT '交易类型（1-采购入库 2-销售出库 3-退药入库 4-报损出库 5-盘点调整）',
    qty           DECIMAL(14,2) DEFAULT 0 COMMENT '数量（入库为正，出库为负）',
    price         DECIMAL(14,4) DEFAULT 0 COMMENT '单价',
    amount        DECIMAL(16,2) DEFAULT 0 COMMENT '金额（入库为正，出库为负）',
    before_qty    DECIMAL(14,2) DEFAULT 0 COMMENT '变动前库存',
    after_qty     DECIMAL(14,2) DEFAULT 0 COMMENT '变动后库存',
    batch_no      VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
    expire_date   DATE         DEFAULT NULL COMMENT '有效期',
    ref_no        VARCHAR(64)  DEFAULT NULL COMMENT '关联单号（结算单号/入库单号）',
    biz_date      DATE         DEFAULT NULL COMMENT '业务日期',
    operator      VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    warn_msg      VARCHAR(500) DEFAULT NULL COMMENT '异常提示',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_txn_no (txn_no),
    KEY idx_stock_txn_item (item_id),
    KEY idx_stock_txn_type (txn_type),
    KEY idx_stock_txn_ref (ref_no),
    KEY idx_stock_txn_date (biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品耗材进销存流水';

-- =====================================================================
-- 补齐表结构（mis_ 结算经办 / mif_ 基金监管 / sys_ 平台公共）
-- =====================================================================

-- 操作日志（审计留痕）
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id              BIGINT       NOT NULL COMMENT '主键',
    oper_type       VARCHAR(32) NOT NULL  COMMENT '操作类型（登录/查询/新增/修改/删除/审核/导出/其他）',
    oper_module     VARCHAR(64) DEFAULT NULL COMMENT '功能模块',
    oper_desc       VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    oper_user       VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    oper_user_id    BIGINT      DEFAULT NULL COMMENT '操作人ID',
    request_uri     VARCHAR(255) DEFAULT NULL COMMENT '请求地址',
    request_method  VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
    request_params  TEXT        DEFAULT NULL COMMENT '请求参数',
    target_type     VARCHAR(32) DEFAULT NULL COMMENT '业务对象类型',
    target_id       BIGINT      DEFAULT NULL COMMENT '业务对象ID',
    target_no       VARCHAR(64) DEFAULT NULL COMMENT '业务对象编号',
    oper_ip         VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    cost_ms         BIGINT      DEFAULT NULL COMMENT '耗时（毫秒）',
    oper_result     TINYINT     DEFAULT 1 COMMENT '结果（1-成功 0-失败）',
    error_msg       VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_oper_log_user (oper_user),
    KEY idx_oper_log_target (target_id),
    KEY idx_oper_log_time (create_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志（审计留痕）';

-- 登录日志
CREATE TABLE IF NOT EXISTS sys_login_log (
    id            BIGINT       NOT NULL COMMENT '主键',
    username      VARCHAR(64) DEFAULT NULL COMMENT '登录账号',
    login_type    VARCHAR(16) DEFAULT 1 COMMENT '类型（1-登录 2-登出 3-失败）',
    login_ip      VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    browser       VARCHAR(64) DEFAULT NULL COMMENT '浏览器',
    os_info       VARCHAR(64) DEFAULT NULL COMMENT '操作系统',
    login_result  TINYINT     DEFAULT 1 COMMENT '结果（1-成功 0-失败）',
    error_msg     VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    login_time    DATETIME    DEFAULT NULL COMMENT '登录时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- 附件（与业务单据关联）
CREATE TABLE IF NOT EXISTS sys_attachment (
    id           BIGINT       NOT NULL COMMENT '主键',
    biz_type     VARCHAR(64) NOT NULL  COMMENT '业务类型（如 mif_report）',
    biz_id       BIGINT      DEFAULT NULL COMMENT '业务主键ID',
    biz_no       VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    file_name    VARCHAR(255) NOT NULL  COMMENT '原始文件名',
    file_path    VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
    file_ext     VARCHAR(32) DEFAULT NULL COMMENT '扩展名',
    file_size    BIGINT      DEFAULT 0 COMMENT '文件大小（字节）',
    file_hash    VARCHAR(128) DEFAULT NULL COMMENT '文件哈希（防篡改/秒传）',
    uploader     VARCHAR(64) DEFAULT NULL COMMENT '上传人',
    upload_time  DATETIME    DEFAULT NULL COMMENT '上传时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_attachment_biz (biz_type)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件（与业务单据关联）';

-- 站内通知
CREATE TABLE IF NOT EXISTS sys_notice (
    id              BIGINT       NOT NULL COMMENT '主键',
    notice_title    VARCHAR(128) NOT NULL  COMMENT '通知标题',
    notice_type     VARCHAR(32) DEFAULT 1 COMMENT '类型（1-系统 2-业务 3-监管 4-预警）',
    notice_content  TEXT        DEFAULT NULL COMMENT '通知内容',
    biz_type        VARCHAR(64) DEFAULT NULL COMMENT '关联业务类型',
    biz_id          BIGINT      DEFAULT NULL COMMENT '关联业务ID',
    receiver        VARCHAR(64) DEFAULT NULL COMMENT '接收人',
    receiver_id     BIGINT      DEFAULT NULL COMMENT '接收人ID',
    read_flag       TINYINT     DEFAULT 0 COMMENT '是否已读（0-未读 1-已读）',
    read_time       DATETIME    DEFAULT NULL COMMENT '阅读时间',
    send_time       DATETIME    DEFAULT NULL COMMENT '发送时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_notice_receiver (receiver)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- 待办任务
CREATE TABLE IF NOT EXISTS sys_todo (
    id              BIGINT       NOT NULL COMMENT '主键',
    todo_type       VARCHAR(64) NOT NULL  COMMENT '待办类型（如 mis_bill / mif_suspicion）',
    todo_title      VARCHAR(128) NOT NULL  COMMENT '待办标题',
    biz_type        VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    biz_id          BIGINT      DEFAULT NULL COMMENT '业务主键ID',
    biz_no          VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    assignee        VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    assignee_id     BIGINT      DEFAULT NULL COMMENT '处理人ID',
    assign_dept     VARCHAR(64) DEFAULT NULL COMMENT '处理部门',
    priority        TINYINT     DEFAULT 2 COMMENT '优先级（1-高 2-中 3-低）',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待处理 1-处理中 2-已完成 3-已取消）',
    finish_time     DATETIME    DEFAULT NULL COMMENT '完成时间',
    finish_opinion  VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_todo_assignee (assignee),
    KEY idx_todo_status (status)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办任务';

-- 流程定义
CREATE TABLE IF NOT EXISTS sys_flow_def (
    id            BIGINT       NOT NULL COMMENT '主键',
    flow_code     VARCHAR(64) NOT NULL  COMMENT '流程编码',
    flow_name     VARCHAR(128) NOT NULL  COMMENT '流程名称',
    flow_type     VARCHAR(32) DEFAULT NULL COMMENT '适用业务类型',
    flow_version  VARCHAR(16) DEFAULT '1.0' COMMENT '版本',
    def_json      TEXT        DEFAULT NULL COMMENT '流程定义（JSON：节点与流转规则）',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义';

-- 流程实例
CREATE TABLE IF NOT EXISTS sys_flow_instance (
    id                BIGINT       NOT NULL COMMENT '主键',
    flow_code         VARCHAR(64) NOT NULL  COMMENT '流程编码',
    flow_instance_no  VARCHAR(64) NOT NULL  COMMENT '实例编号',
    biz_type          VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    biz_id            BIGINT      DEFAULT NULL COMMENT '业务主键ID',
    biz_no            VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    current_node      VARCHAR(64) DEFAULT NULL COMMENT '当前节点编码',
    status            TINYINT     DEFAULT 0 COMMENT '状态（0-进行中 1-已完成 2-已终止）',
    start_time        DATETIME    DEFAULT NULL COMMENT '发起时间',
    end_time          DATETIME    DEFAULT NULL COMMENT '结束时间',
    starter           VARCHAR(64) DEFAULT NULL COMMENT '发起人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_flow_instance_biz (biz_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例';

-- 流程节点记录
CREATE TABLE IF NOT EXISTS sys_flow_node (
    id                BIGINT       NOT NULL COMMENT '主键',
    flow_instance_no  VARCHAR(64) NOT NULL  COMMENT '实例编号',
    node_code         VARCHAR(64) NOT NULL  COMMENT '节点编码',
    node_name         VARCHAR(128) DEFAULT NULL COMMENT '节点名称',
    node_order        INT         DEFAULT 0 COMMENT '节点顺序',
    handler           VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    node_result       VARCHAR(32) DEFAULT NULL COMMENT '处理结果',
    node_opinion      VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    node_time         DATETIME    DEFAULT NULL COMMENT '处理时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_flow_node_instance (flow_instance_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点记录';

-- 定时任务调度
CREATE TABLE IF NOT EXISTS sys_job (
    id                BIGINT       NOT NULL COMMENT '主键',
    job_name          VARCHAR(128) NOT NULL  COMMENT '任务名称',
    job_group         VARCHAR(64) DEFAULT 'default' COMMENT '任务分组',
    invoke_target     VARCHAR(255) NOT NULL  COMMENT '调用目标',
    cron_expr         VARCHAR(64) DEFAULT NULL COMMENT 'Cron 表达式',
    misfire_strategy  VARCHAR(32) DEFAULT 1 COMMENT '错过策略（1-立即执行 2-执行一次 3-放弃）',
    status            TINYINT     DEFAULT 0 COMMENT '状态（0-正常 1-暂停）',
    last_run_time     DATETIME    DEFAULT NULL COMMENT '上次执行时间',
    next_run_time     DATETIME    DEFAULT NULL COMMENT '下次执行时间',
    run_count         BIGINT      DEFAULT 0 COMMENT '累计执行次数',
    fail_count        BIGINT      DEFAULT 0 COMMENT '累计失败次数',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务调度';

-- 打印模板
CREATE TABLE IF NOT EXISTS sys_print_template (
    id                BIGINT       NOT NULL COMMENT '主键',
    template_code     VARCHAR(64) NOT NULL  COMMENT '模板编码',
    template_name     VARCHAR(128) NOT NULL  COMMENT '模板名称',
    biz_type          VARCHAR(64) DEFAULT NULL COMMENT '适用业务类型',
    template_content  TEXT        DEFAULT NULL COMMENT '模板内容（HTML/占位符）',
    paper_size        VARCHAR(32) DEFAULT 'A4' COMMENT '纸张规格',
    status            TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打印模板';

-- 医保接口报文模板
CREATE TABLE IF NOT EXISTS sys_api_template (
    id             BIGINT       NOT NULL COMMENT '主键',
    api_code       VARCHAR(64) NOT NULL  COMMENT '接口编码（如 2301/2304/2305）',
    api_name       VARCHAR(128) NOT NULL  COMMENT '接口名称',
    api_direction  TINYINT     DEFAULT 1 COMMENT '方向（1-请求 2-响应）',
    message_type   VARCHAR(32) DEFAULT NULL COMMENT '报文类型',
    version        VARCHAR(16) DEFAULT '1.0' COMMENT '版本',
    template_json  TEXT        DEFAULT NULL COMMENT '报文样例（JSON）',
    status         TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_api_template_code (api_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保接口报文模板';

-- 接口字段映射
CREATE TABLE IF NOT EXISTS sys_api_field_mapping (
    id             BIGINT       NOT NULL COMMENT '主键',
    api_code       VARCHAR(64) NOT NULL  COMMENT '接口编码',
    local_field    VARCHAR(64) NOT NULL  COMMENT '本地字段',
    api_field      VARCHAR(64) NOT NULL  COMMENT '接口字段',
    field_type     VARCHAR(16) DEFAULT 'string' COMMENT '字段类型（string/int/decimal/date/datetime）',
    convert_rule   VARCHAR(255) DEFAULT NULL COMMENT '转换规则',
    required_flag  TINYINT     DEFAULT 0 COMMENT '是否必填（0-否 1-是）',
    remark         VARCHAR(255) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_api_field_api (api_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口字段映射';

-- 行政区划
CREATE TABLE IF NOT EXISTS sys_area (
    id           BIGINT       NOT NULL COMMENT '主键',
    area_code    VARCHAR(32) NOT NULL  COMMENT '区划代码',
    area_name    VARCHAR(64) NOT NULL  COMMENT '区划名称',
    parent_code  VARCHAR(32) DEFAULT NULL COMMENT '上级区划代码',
    area_level   TINYINT     DEFAULT 1 COMMENT '层级（1-省 2-市 3-区县 4-乡镇街道）',
    sort_order   INT         DEFAULT 0 COMMENT '排序号',
    status       TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_area_code (area_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划';

-- 参保单位
CREATE TABLE IF NOT EXISTS mis_insure_group (
    id                  BIGINT       NOT NULL COMMENT '主键',
    group_code          VARCHAR(64) NOT NULL  COMMENT '单位编码',
    group_name          VARCHAR(128) NOT NULL  COMMENT '单位名称',
    group_type          TINYINT     DEFAULT 1 COMMENT '类型（1-机关事业 2-企业 3-个体户 4-居民）',
    social_credit_code  VARCHAR(64) DEFAULT NULL COMMENT '统一社会信用代码',
    contact_name        VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_phone       VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    region              VARCHAR(64) DEFAULT NULL COMMENT '所属统筹区',
    insured_count       INT         DEFAULT 0 COMMENT '参保人数',
    status              TINYINT     DEFAULT 1 COMMENT '状态（1-正常 2-停保 3-注销）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_insure_group_code (group_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参保单位';

-- 参保缴费记录
CREATE TABLE IF NOT EXISTS mis_insure_pay_record (
    id              BIGINT       NOT NULL COMMENT '主键',
    person_id       BIGINT      DEFAULT NULL COMMENT '参保人ID',
    id_card         VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name     VARCHAR(64) DEFAULT NULL COMMENT '参保人姓名',
    group_code      VARCHAR(64) DEFAULT NULL COMMENT '参保单位编码',
    pay_year        VARCHAR(16) DEFAULT NULL COMMENT '缴费年度（yyyy）',
    pay_month       VARCHAR(8)  DEFAULT NULL COMMENT '缴费月份（yyyy-MM）',
    pay_level_code  VARCHAR(32) DEFAULT NULL COMMENT '缴费档次编码',
    pay_base        DECIMAL(14,2) DEFAULT 0 COMMENT '缴费基数',
    pay_amount      DECIMAL(14,2) DEFAULT 0 COMMENT '缴费金额',
    account_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '划入个人账户金额',
    pay_type        TINYINT     DEFAULT 1 COMMENT '类型（1-单位缴 2-个人缴 3-财政补助 4-补缴）',
    pay_status      TINYINT     DEFAULT 1 COMMENT '状态（1-已到账 2-待到账 3-已退费）',
    pay_time        DATETIME    DEFAULT NULL COMMENT '到账时间',
    source_no       VARCHAR(64) DEFAULT NULL COMMENT '来源单号',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_pay_record_idcard (id_card),
    KEY idx_pay_record_month (pay_month)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参保缴费记录';

-- 参保关系变更历史
CREATE TABLE IF NOT EXISTS mis_insure_change (
    id              BIGINT       NOT NULL COMMENT '主键',
    id_card         VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name     VARCHAR(64) DEFAULT NULL COMMENT '参保人姓名',
    change_type     VARCHAR(32) NOT NULL  COMMENT '变更类型（参保/停保/恢复/终止/转移/信息变更）',
    before_value    VARCHAR(500) DEFAULT NULL COMMENT '变更前值',
    after_value     VARCHAR(500) DEFAULT NULL COMMENT '变更后值',
    change_reason   VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
    effective_date  DATE        DEFAULT NULL COMMENT '生效日期',
    operator        VARCHAR(64) DEFAULT NULL COMMENT '经办人',
    oper_time       DATETIME    DEFAULT NULL COMMENT '经办时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_insure_change_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参保关系变更历史';

-- 长期居住备案证明
CREATE TABLE IF NOT EXISTS mis_insure_residence (
    id              BIGINT       NOT NULL COMMENT '主键',
    id_card         VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name     VARCHAR(64) DEFAULT NULL COMMENT '参保人姓名',
    residence_area  VARCHAR(64) DEFAULT NULL COMMENT '居住地',
    residence_type  VARCHAR(32) DEFAULT NULL COMMENT '类型（1-户籍 2-长期居住 3-工作居住）',
    valid_start     DATE        DEFAULT NULL COMMENT '有效期起',
    valid_end       DATE        DEFAULT NULL COMMENT '有效期止',
    cert_no         VARCHAR(64) DEFAULT NULL COMMENT '证明编号',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待审核 1-已备案 2-已失效 3-已驳回）',
    reviewer        VARCHAR(64) DEFAULT NULL COMMENT '审核人',
    review_opinion  VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_residence_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长期居住备案证明';

-- 退休人员认定
CREATE TABLE IF NOT EXISTS mis_insure_retire (
    id              BIGINT       NOT NULL COMMENT '主键',
    id_card         VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name     VARCHAR(64) DEFAULT NULL COMMENT '姓名',
    retire_date     DATE        DEFAULT NULL COMMENT '退休日期',
    retire_type     VARCHAR(32) DEFAULT NULL COMMENT '类型（1-职工退休 2-居民养老 3-特殊工种）',
    pay_years       DECIMAL(6,1) DEFAULT 0 COMMENT '累计缴费年限',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待审核 1-已认定 2-不通过）',
    reviewer        VARCHAR(64) DEFAULT NULL COMMENT '审核人',
    review_opinion  VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    review_time     DATETIME    DEFAULT NULL COMMENT '审核时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_retire_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退休人员认定';

-- 缴费档次
CREATE TABLE IF NOT EXISTS mis_pay_level (
    id               BIGINT       NOT NULL COMMENT '主键',
    level_code       VARCHAR(32) NOT NULL  COMMENT '档次编码',
    level_name       VARCHAR(64) NOT NULL  COMMENT '档次名称',
    insurance_type   VARCHAR(32) DEFAULT NULL COMMENT '适用险种',
    pay_base         DECIMAL(14,2) DEFAULT 0 COMMENT '缴费基数',
    pay_amount       DECIMAL(14,2) DEFAULT 0 COMMENT '缴费金额',
    account_ratio    DECIMAL(6,4) DEFAULT 0 COMMENT '个人账户划入比例',
    benefit_level    VARCHAR(32) DEFAULT NULL COMMENT '对应待遇档级',
    effective_start  DATE        DEFAULT NULL COMMENT '生效起',
    effective_end    DATE        DEFAULT NULL COMMENT '生效止',
    status           TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_pay_level_code (level_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缴费档次';

-- 缴费基数申报
CREATE TABLE IF NOT EXISTS mis_pay_base (
    id              BIGINT       NOT NULL COMMENT '主键',
    group_code      VARCHAR(64) NOT NULL  COMMENT '单位编码',
    group_name      VARCHAR(128) DEFAULT NULL COMMENT '单位名称',
    pay_year        VARCHAR(16) NOT NULL  COMMENT '申报年度',
    declare_base    DECIMAL(16,2) DEFAULT 0 COMMENT '申报基数合计',
    person_count    INT         DEFAULT 0 COMMENT '申报人数',
    declare_amount  DECIMAL(16,2) DEFAULT 0 COMMENT '应缴金额',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待申报 1-已申报 2-已审核 3-已缴费）',
    declare_time    DATETIME    DEFAULT NULL COMMENT '申报时间',
    reviewer        VARCHAR(64) DEFAULT NULL COMMENT '审核人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_pay_base_group (group_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缴费基数申报';

-- 待遇政策配置
CREATE TABLE IF NOT EXISTS mis_benefit_config (
    id                BIGINT       NOT NULL COMMENT '主键',
    config_code       VARCHAR(64) NOT NULL  COMMENT '配置编码',
    config_name       VARCHAR(128) NOT NULL  COMMENT '配置名称',
    insurance_type    VARCHAR(32) DEFAULT NULL COMMENT '适用险种',
    encounter_type    VARCHAR(32) DEFAULT NULL COMMENT '适用就诊类型（门诊/住院）',
    deductible        DECIMAL(12,2) DEFAULT 0 COMMENT '起付线',
    deductible_level  VARCHAR(32) DEFAULT NULL COMMENT '起付线档级（按年度/住院次数）',
    cap_amount        DECIMAL(14,2) DEFAULT 0 COMMENT '封顶线',
    pay_ratio         DECIMAL(6,4) DEFAULT 0 COMMENT '报销比例',
    account_ratio     DECIMAL(6,4) DEFAULT 0 COMMENT '个人账户划入/支付比例',
    effective_start   DATE        DEFAULT NULL COMMENT '生效起',
    effective_end     DATE        DEFAULT NULL COMMENT '生效止',
    status            TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_benefit_config_code (config_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待遇政策配置';

-- 病种目录
CREATE TABLE IF NOT EXISTS mis_disease_catalog (
    id             BIGINT       NOT NULL COMMENT '主键',
    disease_code   VARCHAR(32) NOT NULL  COMMENT '病种编码',
    disease_name   VARCHAR(128) NOT NULL  COMMENT '病种名称',
    icd_code       VARCHAR(32) DEFAULT NULL COMMENT '对应ICD编码',
    disease_level  TINYINT     DEFAULT 2 COMMENT '类别（1-门诊特殊病种 2-门诊慢特病 3-住院病组）',
    pay_ratio      DECIMAL(6,4) DEFAULT 0 COMMENT '支付比例',
    deductible     DECIMAL(12,2) DEFAULT 0 COMMENT '起付线',
    cap_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '封顶线',
    limit_days     INT         DEFAULT 0 COMMENT '年度支付限额天数',
    status         TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_disease_catalog_code (disease_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病种目录';

-- 特殊/双通道药品目录
CREATE TABLE IF NOT EXISTS mis_special_drug (
    id            BIGINT       NOT NULL COMMENT '主键',
    drug_code     VARCHAR(64) NOT NULL  COMMENT '药品编码',
    drug_name     VARCHAR(128) NOT NULL  COMMENT '药品名称',
    spec          VARCHAR(128) DEFAULT NULL COMMENT '规格',
    dual_channel  TINYINT     DEFAULT 0 COMMENT '是否双通道（0-否 1-是）',
    pay_source    VARCHAR(32) DEFAULT NULL COMMENT '支付渠道（1-统筹 2-个账 3-民政 4-慈善）',
    max_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '年度最高支付限额',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_special_drug_code (drug_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特殊/双通道药品目录';

-- 医疗救助对象
CREATE TABLE IF NOT EXISTS mis_aid_person (
    id            BIGINT       NOT NULL COMMENT '主键',
    id_card       VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name   VARCHAR(64) DEFAULT NULL COMMENT '姓名',
    aid_type      TINYINT     DEFAULT 1 COMMENT '救助类型（1-低保 2-特困 3-低保边缘 4-临时救助）',
    aid_level     VARCHAR(32) DEFAULT NULL COMMENT '救助档次',
    deduct_ratio  DECIMAL(6,4) DEFAULT 0 COMMENT '资助比例',
    start_date    DATE        DEFAULT NULL COMMENT '救助起',
    end_date      DATE        DEFAULT NULL COMMENT '救助止',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-有效 0-失效）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_aid_person_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医疗救助对象';

-- 医疗救助报销
CREATE TABLE IF NOT EXISTS mis_aid_claim (
    id             BIGINT       NOT NULL COMMENT '主键',
    claim_no       VARCHAR(64) NOT NULL  COMMENT '报销单号',
    id_card        VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name    VARCHAR(64) DEFAULT NULL COMMENT '姓名',
    aid_type       TINYINT     DEFAULT 1 COMMENT '救助类型',
    total_amount   DECIMAL(14,2) DEFAULT 0 COMMENT '医疗费用总额',
    insurance_pay  DECIMAL(14,2) DEFAULT 0 COMMENT '医保支付金额',
    aid_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '救助金额',
    self_pay       DECIMAL(14,2) DEFAULT 0 COMMENT '个人自付',
    claim_date     DATE        DEFAULT NULL COMMENT '申请日期',
    status         TINYINT     DEFAULT 0 COMMENT '状态（0-待审核 1-已审核 2-已支付 3-已驳回）',
    reviewer       VARCHAR(64) DEFAULT NULL COMMENT '审核人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_aid_claim_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医疗救助报销';

-- 生育待遇申报
CREATE TABLE IF NOT EXISTS mis_maternity_claim (
    id              BIGINT       NOT NULL COMMENT '主键',
    claim_no        VARCHAR(64) NOT NULL  COMMENT '申报单号',
    id_card         VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name     VARCHAR(64) DEFAULT NULL COMMENT '姓名',
    delivery_date   DATE        DEFAULT NULL COMMENT '分娩日期',
    delivery_type   TINYINT     DEFAULT 1 COMMENT '类型（1-顺产 2-剖宫产 3-难产）',
    baby_count      INT         DEFAULT 1 COMMENT '新生儿数量',
    apply_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '申报金额',
    pay_amount      DECIMAL(14,2) DEFAULT 0 COMMENT '核准支付金额',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待审核 1-已核准 2-已支付 3-已驳回）',
    reviewer        VARCHAR(64) DEFAULT NULL COMMENT '审核人',
    review_opinion  VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_maternity_claim_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生育待遇申报';

-- 生育待遇核准结算
CREATE TABLE IF NOT EXISTS mis_maternity_settle (
    id             BIGINT       NOT NULL COMMENT '主键',
    claim_no       VARCHAR(64) NOT NULL  COMMENT '申报单号',
    pay_date       DATE        DEFAULT NULL COMMENT '支付日期',
    pay_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '支付金额',
    pay_source     VARCHAR(32) DEFAULT NULL COMMENT '资金来源',
    bank_account   VARCHAR(64) DEFAULT NULL COMMENT '银行账号',
    settle_status  TINYINT     DEFAULT 0 COMMENT '结算状态（0-待结算 1-已结算）',
    settle_time    DATETIME    DEFAULT NULL COMMENT '结算时间',
    operator       VARCHAR(64) DEFAULT NULL COMMENT '经办人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_maternity_settle_claim (claim_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生育待遇核准结算';

-- 结算清单（医疗机构上传的清单主表）
CREATE TABLE IF NOT EXISTS mis_settle_list (
    id               BIGINT       NOT NULL COMMENT '主键',
    list_no          VARCHAR(64) NOT NULL  COMMENT '清单编号',
    bill_id          BIGINT      DEFAULT NULL COMMENT '关联结算单ID',
    bill_no          VARCHAR(64) DEFAULT NULL COMMENT '结算单号',
    medins_code      VARCHAR(64) DEFAULT NULL COMMENT '医疗机构编码',
    medins_name      VARCHAR(128) DEFAULT NULL COMMENT '医疗机构名称',
    patient_name     VARCHAR(64) DEFAULT NULL COMMENT '患者姓名',
    id_card_mask     VARCHAR(32) DEFAULT NULL COMMENT '身份证掩码',
    insurance_type   VARCHAR(32) DEFAULT NULL COMMENT '险种',
    encounter_type   VARCHAR(32) DEFAULT NULL COMMENT '就诊类型',
    item_count       INT         DEFAULT 0 COMMENT '清单项目数',
    total_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '费用总额',
    insurance_pay    DECIMAL(14,2) DEFAULT 0 COMMENT '统筹支付',
    list_version     VARCHAR(16) DEFAULT '1.0' COMMENT '清单版本',
    upload_batch_no  VARCHAR(64) DEFAULT NULL COMMENT '上传批次号',
    check_status     TINYINT     DEFAULT 0 COMMENT '质控状态（0-未质控 1-通过 2-有问题）',
    upload_time      DATETIME    DEFAULT NULL COMMENT '上传时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_settle_list_no (list_no),
    KEY idx_settle_list_bill (bill_id),
    KEY idx_settle_list_batch (upload_batch_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算清单（医疗机构上传的清单主表）';

-- 结算批次
CREATE TABLE IF NOT EXISTS mis_settle_batch (
    id             BIGINT       NOT NULL COMMENT '主键',
    batch_no       VARCHAR(64) NOT NULL  COMMENT '批次号',
    batch_type     TINYINT     DEFAULT 1 COMMENT '类型（1-实时结算 2-日批 3-月度）',
    biz_date       DATE        DEFAULT NULL COMMENT '业务日期',
    month          VARCHAR(8)  DEFAULT NULL COMMENT '账期（yyyy-MM）',
    list_count     INT         DEFAULT 0 COMMENT '清单数',
    total_amount   DECIMAL(16,2) DEFAULT 0 COMMENT '费用总额',
    insurance_pay  DECIMAL(16,2) DEFAULT 0 COMMENT '统筹支付',
    status         TINYINT     DEFAULT 0 COMMENT '状态（0-上传中 1-已接收 2-已清算 3-异常）',
    upload_user    VARCHAR(64) DEFAULT NULL COMMENT '上传人',
    upload_time    DATETIME    DEFAULT NULL COMMENT '上传时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_settle_batch_no (batch_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算批次';

-- 结算规则（清算与扣减规则）
CREATE TABLE IF NOT EXISTS mis_settle_rule (
    id               BIGINT       NOT NULL COMMENT '主键',
    rule_code        VARCHAR(64) NOT NULL  COMMENT '规则编码',
    rule_name        VARCHAR(128) NOT NULL  COMMENT '规则名称',
    rule_type        VARCHAR(32) DEFAULT NULL COMMENT '类型（1-扣减 2-拒付 3-加成 4-拒付比例）',
    apply_scope      VARCHAR(255) DEFAULT NULL COMMENT '适用范围',
    rule_value       DECIMAL(14,4) DEFAULT 0 COMMENT '规则值（比例或金额）',
    rule_desc        VARCHAR(500) DEFAULT NULL COMMENT '规则说明',
    effective_start  DATE        DEFAULT NULL COMMENT '生效起',
    effective_end    DATE        DEFAULT NULL COMMENT '生效止',
    status           TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_settle_rule_code (rule_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算规则（清算与扣减规则）';

-- 退费申请
CREATE TABLE IF NOT EXISTS mis_refund_apply (
    id              BIGINT       NOT NULL COMMENT '主键',
    apply_no        VARCHAR(64) NOT NULL  COMMENT '申请单号',
    bill_id         BIGINT      DEFAULT NULL COMMENT '结算单ID',
    bill_no         VARCHAR(64) DEFAULT NULL COMMENT '结算单号',
    patient_name    VARCHAR(64) DEFAULT NULL COMMENT '患者姓名',
    refund_amount   DECIMAL(14,2) DEFAULT 0 COMMENT '申请退费金额',
    refund_reason   VARCHAR(500) DEFAULT NULL COMMENT '退费原因',
    apply_user      VARCHAR(64) DEFAULT NULL COMMENT '申请人',
    apply_time      DATETIME    DEFAULT NULL COMMENT '申请时间',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待审核 1-已通过 2-已驳回 3-已退费）',
    reviewer        VARCHAR(64) DEFAULT NULL COMMENT '审核人',
    review_opinion  VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_refund_apply_bill (bill_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退费申请';

-- 结转余额（跨年度/跨统筹区结转）
CREATE TABLE IF NOT EXISTS mis_carry_balance (
    id            BIGINT       NOT NULL COMMENT '主键',
    id_card       VARCHAR(32) NOT NULL  COMMENT '身份证号',
    person_name   VARCHAR(64) DEFAULT NULL COMMENT '姓名',
    carry_type    VARCHAR(32) DEFAULT NULL COMMENT '类型（1-年度结转 2-跨区转移 3-退休冻结）',
    carry_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '结转金额',
    from_period   VARCHAR(16) DEFAULT NULL COMMENT '来源期间',
    to_period     VARCHAR(16) DEFAULT NULL COMMENT '去向期间',
    status        TINYINT     DEFAULT 0 COMMENT '状态（0-待结转 1-已结转 2-已退回）',
    carry_time    DATETIME    DEFAULT NULL COMMENT '结转时间',
    operator      VARCHAR(64) DEFAULT NULL COMMENT '经办人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_carry_balance_idcard (id_card)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结转余额（跨年度/跨统筹区结转）';

-- 药品目录（独立于诊疗/耗材）
CREATE TABLE IF NOT EXISTS mis_drug_catalog (
    id                 BIGINT       NOT NULL COMMENT '主键',
    drug_code          VARCHAR(64) NOT NULL  COMMENT '药品编码',
    drug_name          VARCHAR(128) NOT NULL  COMMENT '药品名称',
    generic_name       VARCHAR(128) DEFAULT NULL COMMENT '通用名',
    dosage_form        VARCHAR(64) DEFAULT NULL COMMENT '剂型',
    strength           VARCHAR(64) DEFAULT NULL COMMENT '规格',
    manufacturer_code  VARCHAR(64) DEFAULT NULL COMMENT '生产企业编码',
    yb_code            VARCHAR(64) DEFAULT NULL COMMENT '医保编码',
    pay_class          VARCHAR(8)  DEFAULT NULL COMMENT '支付类别（甲/乙/丙）',
    self_ratio         DECIMAL(6,4) DEFAULT 0 COMMENT '乙类先行自付比例',
    ins_ratio          DECIMAL(6,4) DEFAULT 0 COMMENT '政策报销比例',
    limit_price        DECIMAL(12,2) DEFAULT 0 COMMENT '医保限价',
    unit               VARCHAR(32) DEFAULT NULL COMMENT '单位',
    status             TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_drug_catalog_code (yb_code),
    KEY idx_drug_catalog_name (drug_name)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品目录（独立于诊疗/耗材）';

-- 医用耗材目录
CREATE TABLE IF NOT EXISTS mis_material_catalog (
    id             BIGINT       NOT NULL COMMENT '主键',
    material_code  VARCHAR(64) NOT NULL  COMMENT '耗材编码',
    material_name  VARCHAR(128) NOT NULL  COMMENT '耗材名称',
    material_type  VARCHAR(64) DEFAULT NULL COMMENT '耗材分类',
    spec           VARCHAR(128) DEFAULT NULL COMMENT '规格',
    unit           VARCHAR(32) DEFAULT NULL COMMENT '单位',
    yb_code        VARCHAR(64) DEFAULT NULL COMMENT '医保编码',
    pay_class      VARCHAR(8)  DEFAULT NULL COMMENT '支付类别',
    limit_price    DECIMAL(12,2) DEFAULT 0 COMMENT '限价',
    register_cert  VARCHAR(64) DEFAULT NULL COMMENT '注册证号',
    status         TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_material_catalog_code (material_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医用耗材目录';

-- 诊疗项目目录
CREATE TABLE IF NOT EXISTS mis_treat_catalog (
    id               BIGINT       NOT NULL COMMENT '主键',
    treat_code       VARCHAR(64) NOT NULL  COMMENT '项目编码',
    treat_name       VARCHAR(128) NOT NULL  COMMENT '项目名称',
    treat_level      VARCHAR(64) DEFAULT NULL COMMENT '项目等级',
    unit             VARCHAR(32) DEFAULT NULL COMMENT '单位',
    yb_code          VARCHAR(64) DEFAULT NULL COMMENT '医保编码',
    pay_class        VARCHAR(8)  DEFAULT NULL COMMENT '支付类别',
    ins_ratio        DECIMAL(6,4) DEFAULT 0 COMMENT '报销比例',
    limit_price      DECIMAL(12,2) DEFAULT 0 COMMENT '限价',
    child_age_limit  INT         DEFAULT 0 COMMENT '年龄限制',
    status           TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_treat_catalog_code (treat_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊疗项目目录';

-- 药品规格字典
CREATE TABLE IF NOT EXISTS mis_drug_spec (
    id            BIGINT       NOT NULL COMMENT '主键',
    spec_name     VARCHAR(128) NOT NULL  COMMENT '规格名称',
    dosage_form   VARCHAR(64) DEFAULT NULL COMMENT '剂型',
    strength      VARCHAR(64) DEFAULT NULL COMMENT '含量',
    unit          VARCHAR(32) DEFAULT NULL COMMENT '单位',
    convert_rate  DECIMAL(10,4) DEFAULT 1 COMMENT '换算比例',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_drug_spec_name (spec_name)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品规格字典';

-- 药品生产企业
CREATE TABLE IF NOT EXISTS mis_manufacturer (
    id                 BIGINT       NOT NULL COMMENT '主键',
    manufacturer_code  VARCHAR(64) NOT NULL  COMMENT '企业编码',
    manufacturer_name  VARCHAR(128) NOT NULL  COMMENT '企业名称',
    credit_code        VARCHAR(64) DEFAULT NULL COMMENT '统一社会信用代码',
    license_no         VARCHAR(64) DEFAULT NULL COMMENT '药品生产许可证号',
    contact            VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    phone              VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    region             VARCHAR(64) DEFAULT NULL COMMENT '所在地',
    status             TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_manufacturer_code (manufacturer_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品生产企业';

-- 限价与支付标准
CREATE TABLE IF NOT EXISTS mis_limit_price (
    id               BIGINT       NOT NULL COMMENT '主键',
    item_code        VARCHAR(64) NOT NULL  COMMENT '项目/药品编码',
    item_name        VARCHAR(128) DEFAULT NULL COMMENT '名称',
    price_type       TINYINT     DEFAULT 1 COMMENT '类型（1-限价 2-支付标准 3-谈判价 4-零加成）',
    limit_price      DECIMAL(12,2) DEFAULT 0 COMMENT '价格',
    region           VARCHAR(64) DEFAULT NULL COMMENT '适用统筹区',
    effective_start  DATE        DEFAULT NULL COMMENT '生效起',
    effective_end    DATE        DEFAULT NULL COMMENT '生效止',
    status           TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_limit_price_item (item_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限价与支付标准';

-- 医保基金账户
CREATE TABLE IF NOT EXISTS mis_fund_account (
    id            BIGINT       NOT NULL COMMENT '主键',
    account_code  VARCHAR(64) NOT NULL  COMMENT '账户编码',
    account_name  VARCHAR(128) NOT NULL  COMMENT '账户名称',
    account_type  TINYINT     DEFAULT 1 COMMENT '类型（1-统筹基金 2-个人账户 3-风险金 4-保证金）',
    balance       DECIMAL(16,2) DEFAULT 0 COMMENT '账户余额',
    region        VARCHAR(64) DEFAULT NULL COMMENT '所属统筹区',
    currency      VARCHAR(8)  DEFAULT 'CNY' COMMENT '币种',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-正常 2-冻结 3-注销）',
    open_date     DATE        DEFAULT NULL COMMENT '开户日期',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_fund_account_code (account_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保基金账户';

-- 基金收支流水
CREATE TABLE IF NOT EXISTS mis_fund_txn (
    id             BIGINT       NOT NULL COMMENT '主键',
    txn_no         VARCHAR(64) NOT NULL  COMMENT '流水号',
    account_code   VARCHAR(64) DEFAULT NULL COMMENT '账户编码',
    txn_type       TINYINT     DEFAULT 0 COMMENT '类型（1-收入 2-支出 3-拨入 4-拨出 5-调拨）',
    txn_direction  TINYINT     DEFAULT 1 COMMENT '方向（1-收入 2-支出）',
    amount         DECIMAL(16,2) DEFAULT 0 COMMENT '金额',
    balance_after  DECIMAL(16,2) DEFAULT 0 COMMENT '交易后余额',
    ref_type       VARCHAR(32) DEFAULT NULL COMMENT '来源单据类型',
    ref_no         VARCHAR(64) DEFAULT NULL COMMENT '来源单据号',
    summary        VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    txn_date       DATE        DEFAULT NULL COMMENT '业务日期',
    operator       VARCHAR(64) DEFAULT NULL COMMENT '经办人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_fund_txn_account (account_code),
    KEY idx_fund_txn_date (txn_date)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金收支流水';

-- 结算凭证
CREATE TABLE IF NOT EXISTS mis_invoice (
    id            BIGINT       NOT NULL COMMENT '主键',
    invoice_no    VARCHAR(64) NOT NULL  COMMENT '凭证号',
    month         VARCHAR(8)  DEFAULT NULL COMMENT '账期（yyyy-MM）',
    medins_code   VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
    medins_name   VARCHAR(128) DEFAULT NULL COMMENT '机构名称',
    bill_count    INT         DEFAULT 0 COMMENT '结算笔数',
    amount        DECIMAL(16,2) DEFAULT 0 COMMENT '金额',
    invoice_type  TINYINT     DEFAULT 1 COMMENT '类型（1-拨付 2-拒付 3-扣款 4-清算）',
    status        TINYINT     DEFAULT 0 COMMENT '状态（0-待生成 1-已生成 2-已回单）',
    receipt_no    VARCHAR(64) DEFAULT NULL COMMENT '回单号',
    create_time   DATETIME    DEFAULT NULL COMMENT '生成时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_invoice_no (invoice_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算凭证';

-- 定点机构保证金
CREATE TABLE IF NOT EXISTS mis_deposit (
    id                BIGINT       NOT NULL COMMENT '主键',
    medins_code       VARCHAR(64) NOT NULL  COMMENT '机构编码',
    medins_name       VARCHAR(128) DEFAULT NULL COMMENT '机构名称',
    deposit_amount    DECIMAL(16,2) DEFAULT 0 COMMENT '保证金金额',
    frozen_amount     DECIMAL(16,2) DEFAULT 0 COMMENT '已冻结金额',
    available_amount  DECIMAL(16,2) DEFAULT 0 COMMENT '可用余额',
    ratio             DECIMAL(6,4) DEFAULT 0 COMMENT '预留比例',
    status            TINYINT     DEFAULT 1 COMMENT '状态（1-正常 2-冻结 3-退回）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_deposit_medins (medins_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定点机构保证金';

-- 基金计提
CREATE TABLE IF NOT EXISTS mis_accumulate (
    id                 BIGINT       NOT NULL COMMENT '主键',
    accumulate_no      VARCHAR(64) NOT NULL  COMMENT '计提单号',
    month              VARCHAR(8)  DEFAULT NULL COMMENT '账期',
    insure_type        VARCHAR(32) DEFAULT NULL COMMENT '险种',
    base_amount        DECIMAL(16,2) DEFAULT 0 COMMENT '计提基数（缴费收入）',
    accumulate_amount  DECIMAL(16,2) DEFAULT 0 COMMENT '计提金额',
    risk_amount        DECIMAL(16,2) DEFAULT 0 COMMENT '风险金提取',
    accumulate_date    DATE        DEFAULT NULL COMMENT '计提日期',
    status             TINYINT     DEFAULT 0 COMMENT '状态（0-待计提 1-已计提）',
    operator           VARCHAR(64) DEFAULT NULL COMMENT '经办人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_accumulate_month (month)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金计提';

-- 监管规则参数
CREATE TABLE IF NOT EXISTS mif_rule_param (
    id           BIGINT       NOT NULL COMMENT '主键',
    rule_id      BIGINT      DEFAULT NULL COMMENT '规则ID',
    rule_code    VARCHAR(64) DEFAULT NULL COMMENT '规则编码',
    param_key    VARCHAR(64) NOT NULL  COMMENT '参数键',
    param_value  VARCHAR(255) DEFAULT NULL COMMENT '参数值',
    param_name   VARCHAR(128) DEFAULT NULL COMMENT '参数名称',
    value_type   VARCHAR(16) DEFAULT 'string' COMMENT '值类型（string/int/decimal/date）',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '说明',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_rule_param_rule (rule_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管规则参数';

-- 监管规则版本
CREATE TABLE IF NOT EXISTS mif_rule_version (
    id              BIGINT       NOT NULL COMMENT '主键',
    rule_id         BIGINT      DEFAULT NULL COMMENT '规则ID',
    rule_code       VARCHAR(64) NOT NULL  COMMENT '规则编码',
    version         VARCHAR(16) NOT NULL  COMMENT '版本号',
    rule_snapshot   TEXT        DEFAULT NULL COMMENT '规则快照（JSON）',
    change_reason   VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    effective_time  DATETIME    DEFAULT NULL COMMENT '生效时间',
    operator        VARCHAR(64) DEFAULT NULL COMMENT '变更人',
    status          TINYINT     DEFAULT 1 COMMENT '状态（1-生效 0-历史）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_rule_version_code (rule_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管规则版本';

-- 疑点处理记录
CREATE TABLE IF NOT EXISTS mif_suspicion_handle (
    id              BIGINT       NOT NULL COMMENT '主键',
    suspicion_id    BIGINT      NOT NULL  COMMENT '疑点ID',
    suspicion_no    VARCHAR(64) DEFAULT NULL COMMENT '疑点编号',
    handle_action   VARCHAR(32) NOT NULL  COMMENT '处理动作（确认/排除/移送/转办）',
    handle_opinion  VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    handle_user     VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    handle_dept     VARCHAR(64) DEFAULT NULL COMMENT '处理部门',
    handle_time     DATETIME    DEFAULT NULL COMMENT '处理时间',
    attach_url      VARCHAR(500) DEFAULT NULL COMMENT '证据附件',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_suspicion_handle_id (suspicion_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='疑点处理记录';

-- 飞检计划
CREATE TABLE IF NOT EXISTS mif_inspection_plan (
    id            BIGINT       NOT NULL COMMENT '主键',
    plan_no       VARCHAR(64) NOT NULL  COMMENT '计划编号',
    plan_name     VARCHAR(128) NOT NULL  COMMENT '计划名称',
    check_type    TINYINT     DEFAULT 1 COMMENT '检查类型（1-日常 2-专项 3-监督抽查）',
    plan_period   VARCHAR(32) DEFAULT NULL COMMENT '计划周期',
    target_count  INT         DEFAULT 0 COMMENT '计划检查机构数',
    start_date    DATE        DEFAULT NULL COMMENT '开始日期',
    end_date      DATE        DEFAULT NULL COMMENT '结束日期',
    dept_name     VARCHAR(64) DEFAULT NULL COMMENT '牵头部门',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-已发布 2-执行中 3-已完成）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_inspection_plan_no (plan_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞检计划';

-- 飞检组成员
CREATE TABLE IF NOT EXISTS mif_inspection_member (
    id               BIGINT       NOT NULL COMMENT '主键',
    task_id          BIGINT      NOT NULL  COMMENT '飞检任务ID',
    member_name      VARCHAR(64) NOT NULL  COMMENT '成员姓名',
    member_code      VARCHAR(64) DEFAULT NULL COMMENT '成员账号',
    member_role      VARCHAR(32) DEFAULT NULL COMMENT '角色（组长/成员/专家）',
    dept_name        VARCHAR(64) DEFAULT NULL COMMENT '所属部门',
    split_mediation  TINYINT     DEFAULT 0 COMMENT '是否回避',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_inspection_member_task (task_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞检组成员';

-- 反欺诈线索证据
CREATE TABLE IF NOT EXISTS mif_clue_evidence (
    id             BIGINT       NOT NULL COMMENT '主键',
    clue_id        BIGINT      NOT NULL  COMMENT '线索ID',
    clue_no        VARCHAR(64) DEFAULT NULL COMMENT '线索编号',
    evidence_type  VARCHAR(32) DEFAULT NULL COMMENT '证据类型（明细/日志/单据/说明）',
    evidence_desc  VARCHAR(500) DEFAULT NULL COMMENT '证据描述',
    evidence_url   VARCHAR(500) DEFAULT NULL COMMENT '附件地址',
    collect_time   DATETIME    DEFAULT NULL COMMENT '采集时间',
    collector      VARCHAR(64) DEFAULT NULL COMMENT '采集人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_clue_evidence_clue (clue_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反欺诈线索证据';

-- 反欺诈线索处理记录
CREATE TABLE IF NOT EXISTS mif_clue_handle (
    id              BIGINT       NOT NULL COMMENT '主键',
    clue_id         BIGINT      NOT NULL  COMMENT '线索ID',
    clue_no         VARCHAR(64) DEFAULT NULL COMMENT '线索编号',
    handle_action   VARCHAR(32) NOT NULL  COMMENT '处理动作（确认/排除/移送）',
    handle_opinion  VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    handler         VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    handle_dept     VARCHAR(64) DEFAULT NULL COMMENT '处理部门',
    transfer_to     VARCHAR(64) DEFAULT NULL COMMENT '移送对象',
    handle_time     DATETIME    DEFAULT NULL COMMENT '处理时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_clue_handle_clue (clue_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反欺诈线索处理记录';

-- 反欺诈模型运行记录
CREATE TABLE IF NOT EXISTS mif_fraud_model_run (
    id          BIGINT       NOT NULL COMMENT '主键',
    model_id    BIGINT      DEFAULT NULL COMMENT '模型ID',
    model_code  VARCHAR(64) DEFAULT NULL COMMENT '模型编码',
    run_no      VARCHAR(64) NOT NULL  COMMENT '运行批次号',
    run_type    TINYINT     DEFAULT 1 COMMENT '类型（1-定时 2-手动）',
    start_time  DATETIME    DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME    DEFAULT NULL COMMENT '结束时间',
    cost_ms     BIGINT      DEFAULT 0 COMMENT '耗时（毫秒）',
    scan_count  INT         DEFAULT 0 COMMENT '扫描数据量',
    hit_count   INT         DEFAULT 0 COMMENT '命中数',
    operator    VARCHAR(64) DEFAULT NULL COMMENT '执行人',
    run_result  TINYINT     DEFAULT 1 COMMENT '结果（1-成功 0-失败）',
    error_msg   VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_fraud_model_run_model (model_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反欺诈模型运行记录';

-- 反欺诈风险特征库
CREATE TABLE IF NOT EXISTS mif_fraud_feature (
    id             BIGINT       NOT NULL COMMENT '主键',
    feature_code   VARCHAR(64) NOT NULL  COMMENT '特征编码',
    feature_name   VARCHAR(128) NOT NULL  COMMENT '特征名称',
    feature_type   VARCHAR(32) DEFAULT NULL COMMENT '类型（1-频次 2-金额 3-关联 4-时序 5-空间）',
    data_source    VARCHAR(64) DEFAULT NULL COMMENT '数据来源表',
    calc_expr      VARCHAR(500) DEFAULT NULL COMMENT '计算表达式',
    default_value  VARCHAR(128) DEFAULT NULL COMMENT '默认值',
    weight         DECIMAL(6,4) DEFAULT 0 COMMENT '风险权重',
    description    VARCHAR(500) DEFAULT NULL COMMENT '说明',
    status         TINYINT     DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_fraud_feature_code (feature_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反欺诈风险特征库';

-- 特例单议记录
CREATE TABLE IF NOT EXISTS mif_drg_special (
    id              BIGINT       NOT NULL COMMENT '主键',
    case_id         BIGINT      DEFAULT NULL COMMENT '病例ID',
    case_no         VARCHAR(64) DEFAULT NULL COMMENT '病例号',
    apply_reason    VARCHAR(500) DEFAULT NULL COMMENT '申请理由',
    apply_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '申请金额',
    approve_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '核准金额',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-待审 1-已核准 2-已驳回）',
    reviewer        VARCHAR(64) DEFAULT NULL COMMENT '评审人',
    review_opinion  VARCHAR(500) DEFAULT NULL COMMENT '评审意见',
    review_time     DATETIME    DEFAULT NULL COMMENT '评审时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_drg_special_case (case_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特例单议记录';

-- 病组明细（组内病种与入组条件）
CREATE TABLE IF NOT EXISTS mif_drg_group_detail (
    id            BIGINT       NOT NULL COMMENT '主键',
    group_code    VARCHAR(64) NOT NULL  COMMENT '病组编码',
    detail_seq    INT         DEFAULT 0 COMMENT '序号',
    icd_code      VARCHAR(32) DEFAULT NULL COMMENT 'ICD编码',
    icd_name      VARCHAR(128) DEFAULT NULL COMMENT 'ICD名称',
    weight        DECIMAL(10,2) DEFAULT 0 COMMENT '权重',
    age_limit     VARCHAR(64) DEFAULT NULL COMMENT '年龄限制',
    gender_limit  VARCHAR(8)  DEFAULT NULL COMMENT '性别限制',
    cost_low      DECIMAL(14,2) DEFAULT 0 COMMENT '费用下限',
    cost_high     DECIMAL(14,2) DEFAULT 0 COMMENT '费用上限',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_drg_group_detail_code (group_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病组明细（组内病种与入组条件）';

-- 信用记分明细
CREATE TABLE IF NOT EXISTS mif_credit_detail (
    id            BIGINT       NOT NULL COMMENT '主键',
    subject_type  TINYINT     DEFAULT 1 COMMENT '主体类型（1-机构 2-医师 3-参保人）',
    subject_code  VARCHAR(64) NOT NULL  COMMENT '主体编码',
    subject_name  VARCHAR(128) DEFAULT NULL COMMENT '主体名称',
    event_type    VARCHAR(32) NOT NULL  COMMENT '事件类型（违规扣分/守信加分/整改恢复）',
    score_change  DECIMAL(6,2) DEFAULT 0 COMMENT '分值变动（扣分为负）',
    score_after   DECIMAL(6,2) DEFAULT 0 COMMENT '变动后总分',
    source_type   VARCHAR(32) DEFAULT NULL COMMENT '来源类型',
    source_no     VARCHAR(64) DEFAULT NULL COMMENT '来源单号',
    reason        VARCHAR(500) DEFAULT NULL COMMENT '事由',
    occur_time    DATETIME    DEFAULT NULL COMMENT '发生时间',
    operator      VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_credit_detail_subject (subject_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用记分明细';

-- 守信激励记录
CREATE TABLE IF NOT EXISTS mif_credit_reward (
    id               BIGINT       NOT NULL COMMENT '主键',
    subject_type     TINYINT     DEFAULT 1 COMMENT '主体类型',
    subject_code     VARCHAR(64) NOT NULL  COMMENT '主体编码',
    subject_name     VARCHAR(128) DEFAULT NULL COMMENT '主体名称',
    reward_type      VARCHAR(32) DEFAULT NULL COMMENT '激励类型（1-绿色通道 2-减免 3-授牌 4-优先拨付）',
    reward_desc      VARCHAR(500) DEFAULT NULL COMMENT '激励内容',
    reward_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '减免金额',
    effective_start  DATE        DEFAULT NULL COMMENT '生效起',
    effective_end    DATE        DEFAULT NULL COMMENT '生效止',
    status           TINYINT     DEFAULT 1 COMMENT '状态',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_credit_reward_subject (subject_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='守信激励记录';

-- 失信惩戒记录
CREATE TABLE IF NOT EXISTS mif_credit_punish (
    id               BIGINT       NOT NULL COMMENT '主键',
    subject_type     TINYINT     DEFAULT 1 COMMENT '主体类型',
    subject_code     VARCHAR(64) NOT NULL  COMMENT '主体编码',
    subject_name     VARCHAR(128) DEFAULT NULL COMMENT '主体名称',
    punish_type      VARCHAR(32) DEFAULT NULL COMMENT '惩戒类型（1-约谈 2-暂停 3-解除协议 4-限制结算）',
    punish_desc      VARCHAR(500) DEFAULT NULL COMMENT '惩戒内容',
    punish_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '涉及金额',
    effective_start  DATE        DEFAULT NULL COMMENT '生效起',
    effective_end    DATE        DEFAULT NULL COMMENT '生效止',
    status           TINYINT     DEFAULT 1 COMMENT '状态',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_credit_punish_subject (subject_code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失信惩戒记录';

-- 行政处罚
CREATE TABLE IF NOT EXISTS mif_penalty (
    id              BIGINT       NOT NULL COMMENT '主键',
    penalty_no      VARCHAR(64) NOT NULL  COMMENT '处罚决定书号',
    medins_code     VARCHAR(64) DEFAULT NULL COMMENT '被处罚机构编码',
    medins_name     VARCHAR(128) DEFAULT NULL COMMENT '被处罚机构名称',
    penalty_type    VARCHAR(32) DEFAULT NULL COMMENT '处罚种类（1-警告 2-罚款 3-暂停 4-解除）',
    penalty_reason  VARCHAR(500) DEFAULT NULL COMMENT '违法事实与理由',
    penalty_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '罚款金额',
    penalty_date    DATE        DEFAULT NULL COMMENT '决定日期',
    decision_doc    VARCHAR(500) DEFAULT NULL COMMENT '决定书文号',
    status          TINYINT     DEFAULT 0 COMMENT '状态（0-拟稿 1-已决定 2-已送达 3-已执行）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_penalty_no (penalty_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政处罚';

-- 监管报表
CREATE TABLE IF NOT EXISTS mif_supervise_report (
    id            BIGINT       NOT NULL COMMENT '主键',
    report_no     VARCHAR(64) NOT NULL  COMMENT '报表编号',
    report_name   VARCHAR(128) NOT NULL  COMMENT '报表名称',
    report_type   VARCHAR(32) DEFAULT NULL COMMENT '报表类型（1-基金运行 2-违规统计 3-机构评价 4-风险画像）',
    period        VARCHAR(16) DEFAULT NULL COMMENT '统计期间',
    report_data   TEXT        DEFAULT NULL COMMENT '报表数据（JSON）',
    summary       VARCHAR(1000) DEFAULT NULL COMMENT '摘要结论',
    status        TINYINT     DEFAULT 1 COMMENT '状态（1-草稿 2-已发布）',
    publisher     VARCHAR(64) DEFAULT NULL COMMENT '发布人',
    publish_time  DATETIME    DEFAULT NULL COMMENT '发布时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_supervise_report_no (report_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管报表';

-- 追溯码采集批次
CREATE TABLE IF NOT EXISTS mif_trace_batch (
    id           BIGINT       NOT NULL COMMENT '主键',
    batch_no     VARCHAR(64) NOT NULL  COMMENT '批次号',
    medins_code  VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
    medins_name  VARCHAR(128) DEFAULT NULL COMMENT '机构名称',
    code_count   INT         DEFAULT 0 COMMENT '码数量',
    upload_user  VARCHAR(64) DEFAULT NULL COMMENT '上传人',
    upload_time  DATETIME    DEFAULT NULL COMMENT '上传时间',
    status       TINYINT     DEFAULT 1 COMMENT '状态（1-已上传 2-已核验 3-部分异常）',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_trace_batch_no (batch_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追溯码采集批次';

-- 库存盘点单
CREATE TABLE IF NOT EXISTS mif_stock_check (
    id           BIGINT       NOT NULL COMMENT '主键',
    check_no     VARCHAR(64) NOT NULL  COMMENT '盘点单号',
    medins_code  VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
    medins_name  VARCHAR(128) DEFAULT NULL COMMENT '机构名称',
    check_date   DATE        DEFAULT NULL COMMENT '盘点日期',
    item_count   INT         DEFAULT 0 COMMENT '盘点品项数',
    diff_count   INT         DEFAULT 0 COMMENT '差异品项数',
    diff_amount  DECIMAL(16,2) DEFAULT 0 COMMENT '差异金额',
    status       TINYINT     DEFAULT 0 COMMENT '状态（0-待盘点 1-盘点中 2-已完成 3-已复核）',
    checker      VARCHAR(64) DEFAULT NULL COMMENT '盘点人',
    reviewer     VARCHAR(64) DEFAULT NULL COMMENT '复核人',
    review_time  DATETIME    DEFAULT NULL COMMENT '复核时间',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_stock_check_no (check_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点单';

-- 库存盘点明细
CREATE TABLE IF NOT EXISTS mif_stock_check_item (
    id           BIGINT       NOT NULL COMMENT '主键',
    check_no     VARCHAR(64) NOT NULL  COMMENT '盘点单号',
    item_code    VARCHAR(64) NOT NULL  COMMENT '品项编码',
    item_name    VARCHAR(128) DEFAULT NULL COMMENT '品项名称',
    book_qty     DECIMAL(14,2) DEFAULT 0 COMMENT '账面数量',
    real_qty     DECIMAL(14,2) DEFAULT 0 COMMENT '实盘数量',
    diff_qty     DECIMAL(14,2) DEFAULT 0 COMMENT '差异数量',
    diff_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '差异金额',
    diff_reason  VARCHAR(255) DEFAULT NULL COMMENT '差异原因',
    create_by_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by_id  BIGINT       DEFAULT NULL COMMENT '修改人ID',
    update_by     VARCHAR(64)  DEFAULT NULL COMMENT '修改人姓名',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_stock_check_item_no (check_no)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点明细';
