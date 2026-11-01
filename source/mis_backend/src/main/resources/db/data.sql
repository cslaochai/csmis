-- 幂等初始化参保人（INSERT IGNORE 依赖身份证唯一键）。
-- 演示用测试人员；HIS 患者若不在此处，2304 上传会按报文中的险种兜底接收并给出告警，参保查询则返回未参保。
INSERT IGNORE INTO mis_insured_person
(id_card, insurance_no, name, gender, insurance_type, person_status, account_balance, region) VALUES
('430102199001011234', '430100001', '张三', '男', '2', 1, 3200.50, '长沙市'),
('430102198505056789', '430100002', '李四', '女', '3', 1, 0.00,    '长沙市'),
('430102197012121234', '430100003', '王五', '男', '2', 1, 1500.00, '长沙市'),
('430121200006069999', '430100004', '赵六', '女', '3', 1, 0.00,    '长沙市'),
('43010219660606001X', '430100005', '孙七', '男', '2', 0, 80.00,  '长沙市');

-- =====================================================================
-- 系统基础铺底（仿 HIS 02-铺底基础数据）：科室 / 角色 / 菜单 / 角色菜单 / 员工 / 岗位 / 字典 / 配置
-- 全部 INSERT IGNORE 幂等；用户表密码为 BCrypt，由 AuthDataInitializer 首次启动时播种。
-- =====================================================================

-- 经办科室
INSERT IGNORE INTO sys_department (id, dept_code, dept_name, dept_type, parent_id, sort_order, status) VALUES
(1, 'ZHK',  '综合管理科', 4, 0, 1, 1),
(2, 'JSJBK', '结算经办科', 1, 0, 2, 1),
(3, 'SHJHK', '审核稽核科', 2, 0, 3, 1),
(4, 'JJCWK', '基金财务科', 3, 0, 4, 1);

-- 经办人员
INSERT IGNORE INTO sys_employee (id, emp_code, emp_name, emp_type, gender, hire_date, phone, dept_id, dept_name, title, position, status) VALUES
(1, 'ADMIN001', '系统管理员', 3, 1, '2026-01-01', '13800000001', 1, '综合管理科', NULL, '平台管理员', 1),
(2, 'YB001',    '结算经办员', 1, 2, '2026-01-01', '13800000002', 2, '结算经办科', '初级', '结算经办', 1),
(3, 'YB002',    '审核稽核员', 2, 1, '2026-01-01', '13800000003', 3, '审核稽核科', '中级', '审核稽核', 1);

-- 角色
INSERT IGNORE INTO sys_role (id, role_code, role_name, role_type, data_scope, sort_order, status) VALUES
(1, 'MSS_ADMIN',    '经办管理员', 1, 1, 1, 1),
(2, 'MSS_OPERATOR', '经办员',     1, 3, 2, 1),
(3, 'MSS_AUDITOR',  '审核员',     1, 3, 3, 1);

-- 菜单（目录=1，菜单=2；component 对应 front_mss/src/views 下路径）


-- 系统管理按钮权限码（menu_type=3，v-perm 显隐用；后端鉴权仍是接口白名单口径）


-- ===========================================================
-- 菜单：三级结构
--   顶级（业务域）：2 医保结算经办 · 13 医保基金监管 · 9 系统管理
--   二级（业务分组）：200-206 / 210-215 / 220-223
--   三级（页面）：menu_type=2；按钮 menu_type=3 挂在所属页面下
-- 说明：顶级/二级的 path 只是分组标识（不参与路由跳转），
--       三级页面的 path 与 front_mis 路由一一对应。
-- ===========================================================

-- 顶级：业务域
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES
(2, '医保结算经办', 0, 10, 1, 'settle', NULL, 'settle', 'Briefcase', NULL),
(13, '医保基金监管', 0, 20, 1, 'supervise', NULL, 'supervise', 'Monitor', NULL),
(9, '系统管理', 0, 30, 1, 'system', NULL, 'system', 'Setting', NULL);

-- 二级：业务分组目录
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES
(200, '结算经办', 2, 0, 1, 'settle.do', NULL, 'group.settle.do', 'CreditCard', 'settle:do:list'),
(201, '结算与清算', 2, 1, 1, 'settle.fin', NULL, 'group.settle.fin', 'Money', 'settle:fin:list'),
(202, '参保与账户', 2, 2, 1, 'settle.insured', NULL, 'group.settle.insured', 'User', 'insured:person:list'),
(203, '费用与目录', 2, 3, 1, 'settle.catalog', NULL, 'group.settle.catalog', 'Notebook', 'catalog:list'),
(204, '待遇与备案', 2, 4, 1, 'settle.filing', NULL, 'group.settle.filing', 'Stamp', 'filing:list'),
(205, '进销存', 2, 5, 1, 'settle.stock', NULL, 'group.settle.stock', 'Goods', 'stock:item:list'),
(206, '经办看板', 2, 6, 1, 'settle.board', NULL, 'group.settle.board', 'DataBoard', 'settle:board:view'),
(210, '监管总览', 13, 10, 1, 'supervise.overview', NULL, 'group.supervise.overview', 'DataBoard', 'supervise:dashboard:view'),
(211, '规则与知识', 13, 11, 1, 'supervise.rule', NULL, 'group.supervise.rule', 'SetUp', 'supervise:rule:list'),
(212, '稽核核查', 13, 12, 1, 'supervise.audit', NULL, 'group.supervise.audit', 'Search', 'supervise:suspicion:list'),
(213, '智能监管', 13, 13, 1, 'supervise.ai', NULL, 'group.supervise.ai', 'Aim', 'supervise:fraud:list'),
(214, '专项监管', 13, 14, 1, 'supervise.special', NULL, 'group.supervise.special', 'Histogram', 'supervise:quality:list'),
(215, '信用管理', 13, 15, 1, 'supervise.credit', NULL, 'group.supervise.credit', 'Medal', 'supervise:credit:list'),
(220, '用户与权限', 9, 20, 1, 'system.user', NULL, 'group.system.user', 'User', 'system:user:list'),
(221, '组织与岗位', 9, 21, 1, 'system.org', NULL, 'group.system.org', 'OfficeBuilding', 'system:dept:list'),
(222, '基础配置', 9, 22, 1, 'system.dict', NULL, 'group.system.dict', 'Collection', 'system:dict:list'),
(223, '审计与待办', 9, 23, 1, 'system.audit', NULL, 'group.system.audit', 'Document', 'system:operlog:list');

-- 三级：页面菜单
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES
(1, '经办工作台', 206, 1, 2, '/dashboard', 'dashboard/DashboardView', 'portal.dashboard', 'DataBoard', 'portal:dashboard:view'),
(21, '门诊医保结算', 200, 21, 2, '/settlement/outpatient', 'settlement/OutpatientSettleView', 'settle.outpatient', 'Ticket', 'settle:outpatient:list'),
(22, '住院医保结算', 200, 22, 2, '/settlement/inpatient', 'settlement/InpatientSettleView', 'settle.inpatient', 'FirstAidKit', 'settle:inpatient:list'),
(24, '机构报盘接收', 200, 24, 2, '/settlement/gateway', 'settlement/GatewaySettleView', 'settle.gateway', 'Upload', 'settle:gateway:list'),
(23, '结算单管理', 201, 23, 2, '/settlement/bill', 'settlement/SettleBillView', 'settle.bill', 'Document', 'settle:bill:list'),
(150, '结算清单', 201, 50, 2, '/settle/list', 'settlement/SettleListView', 'settle.list', 'Tickets', 'settle:list:list'),
(25, '月度结算管理', 201, 25, 2, '/settlement/monthly', 'settlement/MonthlySettleView', 'settle.monthly', 'Calendar', 'settle:monthly:list'),
(26, '日拨付管理', 201, 26, 2, '/settlement/daily', 'settlement/DailyAllocView', 'settle.daily', 'Wallet', 'settle:daily:list'),
(7, '清算对账', 201, 7, 2, '/reconciliation', 'reconciliation/ReconciliationView', 'recon', 'DataAnalysis', 'recon:list'),
(31, '参保人管理', 202, 31, 2, '/insured/person', 'insured/InsuredPersonView', 'insured.person', 'UserFilled', 'insured:person:list'),
(32, '转移接续/账户/慢特病', 202, 32, 2, '/insured/service', 'insured/InsureServiceView', 'insured.service', 'Switch', 'insured:service:list'),
(4, '费用审核', 203, 4, 2, '/audit/fee', 'audit/FeeAuditView', 'audit.fee', 'Stamp', 'audit:fee:list'),
(5, '三大目录与对照', 203, 5, 2, '/catalog', 'catalog/CatalogView', 'catalog', 'Notebook', 'catalog:list'),
(151, '目录分表管理', 203, 51, 2, '/settle/catalog', 'settlement/CatalogSplitView', 'settle.catalog', 'Collection', 'settle:catalog:list'),
(10, '定点协议管理', 203, 10, 2, '/agreement', 'agreement/AgreementView', 'agreement', 'Postcard', 'agreement:list'),
(11, '手工报销', 204, 11, 2, '/claim', 'claim/ManualClaimView', 'claim', 'Money', 'claim:list'),
(6, '跨省异地就医备案', 204, 6, 2, '/filing/cross-region', 'filing/CrossRegionFilingView', 'filing.crossRegion', 'Tickets', 'filing:list'),
(33, '药品耗材进销存', 205, 33, 2, '/stock', 'stock/StockView', 'stock.manage', 'Goods', 'stock:item:list'),
(152, '库存盘点', 205, 52, 2, '/stock/check', 'stock/StockCheckView', 'stock.check', 'Files', 'stock:check:list'),
(8, '统计报表', 206, 8, 2, '/stats', 'stats/StatsView', 'stats', 'TrendCharts', 'stats:view'),
(131, '监管工作台', 210, 31, 2, '/supervise/dashboard', 'supervise/SuperviseDashboard', 'supervise.dashboard', 'DataBoard', 'supervise:dashboard:view'),
(140, '举报投诉与奖励', 210, 40, 2, '/supervise/report', 'supervise/ReportView', 'supervise.report', 'ChatDotRound', 'supervise:report:list'),
(132, '规则配置', 211, 32, 2, '/supervise/rule', 'supervise/RuleView', 'supervise.rule', 'SetUp', 'supervise:rule:list'),
(136, '知识库', 211, 36, 2, '/supervise/knowledge', 'supervise/KnowledgeView', 'supervise.knowledge', 'Reading', 'supervise:knowledge:list'),
(133, '疑点稽核', 212, 33, 2, '/supervise/suspicion', 'supervise/SuspicionView', 'supervise.suspicion', 'Search', 'supervise:suspicion:list'),
(134, '飞检任务', 212, 34, 2, '/supervise/inspection', 'supervise/InspectionView', 'supervise.inspection', 'Van', 'supervise:inspection:list'),
(135, '违规追回', 212, 35, 2, '/supervise/recover', 'supervise/RecoverView', 'supervise.recover', 'RefreshLeft', 'supervise:recover:list'),
(139, '事前提醒/事中拦截', 213, 39, 2, '/supervise/precheck', 'supervise/PrecheckView', 'supervise.precheck', 'Bell', 'supervise:precheck:list'),
(138, '大数据反欺诈', 213, 38, 2, '/supervise/fraud', 'supervise/FraudView', 'supervise.fraud', 'Aim', 'supervise:fraud:list'),
(141, 'DRG/DIP支付监管', 213, 41, 2, '/drg', 'drg/DrgView', 'drg.manage', 'Histogram', 'drg:case:list'),
(144, '结算清单质控', 214, 44, 2, '/supervise/quality', 'supervise/QualityView', 'supervise.quality', 'DocumentChecked', 'supervise:quality:list'),
(145, '药品追溯码监管', 214, 45, 2, '/supervise/trace', 'supervise/TraceView', 'supervise.trace', 'Postcard', 'supervise:trace:list'),
(142, '双随机一公开', 214, 42, 2, '/supervise/random', 'supervise/RandomView', 'supervise.random', 'Refresh', 'supervise:random:list'),
(143, '自查自纠', 214, 43, 2, '/supervise/selfcheck', 'supervise/SelfCheckView', 'supervise.selfcheck', 'Finished', 'supervise:selfcheck:list'),
(137, '信用管理', 215, 37, 2, '/supervise/credit', 'supervise/CreditView', 'supervise.credit', 'Medal', 'supervise:credit:list'),
(91, '用户管理', 220, 91, 2, '/system/user', 'system/UserView', 'system.user', 'User', 'system:user:list'),
(92, '角色管理', 220, 92, 2, '/system/role', 'system/RoleView', 'system.role', 'Avatar', 'system:role:list'),
(93, '菜单管理', 220, 93, 2, '/system/menu', 'system/MenuView', 'system.menu', 'Menu', 'system:menu:list'),
(94, '岗位管理', 221, 94, 2, '/system/post', 'system/PostView', 'system.post', 'Postcard', 'system:post:list'),
(96, '科室管理', 221, 96, 2, '/system/dept', 'system/DeptView', 'system.dept', 'OfficeBuilding', 'system:dept:list'),
(97, '员工管理', 221, 97, 2, '/system/employee', 'system/EmployeeView', 'system.employee', 'Avatar', 'org:employee:list'),
(95, '字典管理', 222, 95, 2, '/system/dict', 'system/DictView', 'system.dict', 'Collection', 'system:dict:list'),
(153, '操作日志', 223, 53, 2, '/system/operlog', 'system/OperLogView', 'system.operlog', 'Document', 'system:operlog:list'),
(154, '待办中心', 223, 54, 2, '/system/todo', 'system/TodoView', 'system.todo', 'Bell', 'system:todo:list');

-- 按钮权限：挂在所属页面下（menu_type=3）
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES
(101, '用户新增', 91, 1, 3, NULL, NULL, 'system.user.add', NULL, 'system:user:add'),
(102, '用户编辑', 91, 2, 3, NULL, NULL, 'system.user.edit', NULL, 'system:user:edit'),
(103, '用户删除', 91, 3, 3, NULL, NULL, 'system.user.delete', NULL, 'system:user:delete'),
(104, '角色新增', 92, 4, 3, NULL, NULL, 'system.role.add', NULL, 'system:role:add'),
(105, '角色删除', 92, 5, 3, NULL, NULL, 'system.role.delete', NULL, 'system:role:delete'),
(106, '菜单新增', 93, 6, 3, NULL, NULL, 'system.menu.add', NULL, 'system:menu:add'),
(107, '菜单删除', 93, 7, 3, NULL, NULL, 'system.menu.delete', NULL, 'system:menu:delete'),
(108, '字典新增', 95, 8, 3, NULL, NULL, 'system.dict.add', NULL, 'system:dict:add'),
(109, '字典编辑', 95, 9, 3, NULL, NULL, 'system.dict.edit', NULL, 'system:dict:edit'),
(110, '字典删除', 95, 10, 3, NULL, NULL, 'system.dict.delete', NULL, 'system:dict:delete'),
(111, '员工新增', 97, 11, 3, NULL, NULL, 'org.employee.add', NULL, 'org:employee:add'),
(112, '员工删除', 97, 12, 3, NULL, NULL, 'org.employee.delete', NULL, 'org:employee:delete');

-- ===========================================================
-- 角色菜单授权：三级结构下，页面可见需要其所有祖先（顶级域 + 二级分组）都被授权，
-- 否则菜单树挂不上、页面点不到。
-- 注：sys_role_menu.id 无 AUTO_INCREMENT（主键由 entity 生成，此处为纯 SQL 种子），
--     故 id 必须显式指定，取 1000+ 号段避开业务表 id。
-- ===========================================================

-- 角色 1
INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id) VALUES
(1001, 1, 1), (1002, 1, 2), (1003, 1, 4), (1004, 1, 5), (1005, 1, 6), (1006, 1, 7), (1007, 1, 8), (1008, 1, 9), (1009, 1, 10), (1010, 1, 11), (1011, 1, 13), (1012, 1, 21), (1013, 1, 22), (1014, 1, 23), (1015, 1, 24), (1016, 1, 25), (1017, 1, 26), (1018, 1, 31), (1019, 1, 32), (1020, 1, 33), (1021, 1, 91), (1022, 1, 92), (1023, 1, 93), (1024, 1, 94), (1025, 1, 95), (1026, 1, 96), (1027, 1, 97), (1028, 1, 101), (1029, 1, 102), (1030, 1, 103), (1031, 1, 104), (1032, 1, 105), (1033, 1, 106), (1034, 1, 107), (1035, 1, 108), (1036, 1, 109), (1037, 1, 110), (1038, 1, 111), (1039, 1, 112), (1040, 1, 131), (1041, 1, 132), (1042, 1, 133), (1043, 1, 134), (1044, 1, 135), (1045, 1, 136), (1046, 1, 137), (1047, 1, 138), (1048, 1, 139), (1049, 1, 140), (1050, 1, 141), (1051, 1, 142), (1052, 1, 143), (1053, 1, 144), (1054, 1, 145), (1055, 1, 150), (1056, 1, 151), (1057, 1, 152), (1058, 1, 153), (1059, 1, 154), (1060, 1, 200), (1061, 1, 201), (1062, 1, 202), (1063, 1, 203), (1064, 1, 204), (1065, 1, 205), (1066, 1, 206), (1067, 1, 210), (1068, 1, 211), (1069, 1, 212), (1070, 1, 213), (1071, 1, 214), (1072, 1, 215), (1073, 1, 220), (1074, 1, 221), (1075, 1, 222), (1076, 1, 223);

-- 角色 2
INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id) VALUES
(1077, 2, 1), (1078, 2, 2), (1079, 2, 5), (1080, 2, 6), (1081, 2, 7), (1082, 2, 8), (1083, 2, 10), (1084, 2, 11), (1085, 2, 21), (1086, 2, 22), (1087, 2, 23), (1088, 2, 24), (1089, 2, 25), (1090, 2, 26), (1091, 2, 31), (1092, 2, 32), (1093, 2, 33), (1094, 2, 150), (1095, 2, 151), (1096, 2, 152), (1097, 2, 200), (1098, 2, 201), (1099, 2, 202), (1100, 2, 203), (1101, 2, 204), (1102, 2, 205), (1103, 2, 206);

-- 角色 3
INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id) VALUES
(1104, 3, 2), (1105, 3, 4), (1106, 3, 13), (1107, 3, 23), (1108, 3, 131), (1109, 3, 132), (1110, 3, 133), (1111, 3, 134), (1112, 3, 135), (1113, 3, 136), (1114, 3, 137), (1115, 3, 138), (1116, 3, 139), (1117, 3, 140), (1118, 3, 141), (1119, 3, 142), (1120, 3, 143), (1121, 3, 144), (1122, 3, 145), (1123, 3, 150), (1124, 3, 201), (1125, 3, 203), (1126, 3, 210), (1127, 3, 211), (1128, 3, 212), (1129, 3, 213), (1130, 3, 214), (1131, 3, 215);

-- 角色菜单授权（历史段落，见上）
-- 经办管理员：全部菜单

-- 经办员：业务菜单（不含费用审核/系统管理）

-- 审核员：工作台 + 结算单 + 月度 + 费用审核 + 统计报表


-- 员工岗位（角色 × 科室，每人恰好一条 is_primary=1）
INSERT IGNORE INTO sys_employee_post (id, employee_id, role_id, dept_id, is_primary, effective_date) VALUES (1, 1, 1, 1, 1, '2026-01-01'), (2, 2, 2, 2, 1, '2026-01-01'), (3, 3, 3, 3, 1, '2026-01-01');

-- 字典类型
INSERT IGNORE INTO sys_dict_type (id, dict_type, dict_name, status, dict_source) VALUES
(1, 'mss_menu_type',  '菜单类型',   1, 1),
(2, 'mss_user_status','用户状态',   1, 1),
(3, 'mss_user_type',  '用户类型',   1, 1),
(4, 'mss_role_type',  '角色类型',   1, 1),
(5, 'mss_data_scope', '数据权限范围',1, 1),
(6, 'sys_dept_type',  '科室类型',   1, 1),
(7, 'sys_emp_type',   '员工类型',   1, 1),
(8, 'mss_common_status','通用状态', 1, 1),
(9, 'mss_menu_visible','菜单可见',  1, 1),
(10, 'sys_hospital_title', '职称',     1, 1),
(11, 'hospital_position',  '职位',     1, 1),
(12, 'his_education',      '学历',     1, 1),
(13, 'his_emp_cert_type',  '证书类型', 1, 1),
(14, 'his_emp_cert_org',   '发证机关', 1, 1);

-- 字典数据
INSERT IGNORE INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_sort, is_default, status) VALUES (1, 'mss_menu_type',   '目录', '1', 1, 0, 1), (2, 'mss_menu_type',   '菜单', '2', 2, 1, 1), (3, 'mss_menu_type',   '按钮', '3', 3, 0, 1), (4, 'mss_user_status', '停用', '0', 1, 0, 1), (5, 'mss_user_status', '启用', '1', 2, 1, 1), (6, 'mss_user_status', '锁定', '2', 3, 0, 1), (7, 'mss_user_type',   '系统用户', '1', 1, 1, 1), (8, 'mss_user_type',   '外部用户', '2', 2, 0, 1), (9, 'mss_role_type',   '系统角色', '1', 1, 1, 1), (10, 'mss_role_type',   '自定义角色', '2', 2, 0, 1), (11, 'mss_data_scope',  '全部数据', '1', 1, 1, 1), (12, 'mss_data_scope',  '自定义数据', '2', 2, 0, 1), (13, 'mss_data_scope',  '本部门数据', '3', 3, 0, 1), (14, 'mss_data_scope',  '仅本人数据', '5', 4, 0, 1), (15, 'sys_dept_type',   '前台经办', '1', 1, 1, 1), (16, 'sys_dept_type',   '审核稽核', '2', 2, 0, 1), (17, 'sys_dept_type',   '基金财务', '3', 3, 0, 1), (18, 'sys_dept_type',   '综合管理', '4', 4, 0, 1), (19, 'sys_emp_type',    '经办员', '1', 1, 1, 1), (20, 'sys_emp_type',    '审核员', '2', 2, 0, 1), (21, 'sys_emp_type',    '管理员', '3', 3, 0, 1), (22, 'sys_emp_type',    '其他',   '4', 4, 0, 1), (23, 'mss_common_status','停用', '0', 1, 0, 1), (24, 'mss_common_status','启用', '1', 2, 1, 1), (25, 'mss_menu_visible', '隐藏', '0', 1, 0, 1), (26, 'mss_menu_visible', '显示', '1', 2, 1, 1), (27, 'sys_hospital_title', '主任医师', '1', 1, 0, 1), (28, 'sys_hospital_title', '副主任医师', '2', 2, 0, 1), (29, 'sys_hospital_title', '主治医师', '3', 3, 0, 1), (30, 'sys_hospital_title', '住院医师', '4', 4, 0, 1), (31, 'sys_hospital_title', '主任药师', '5', 5, 0, 1), (32, 'sys_hospital_title', '副主任药师', '6', 6, 0, 1), (33, 'sys_hospital_title', '主管药师', '7', 7, 0, 1), (34, 'sys_hospital_title', '药师', '8', 8, 0, 1), (35, 'sys_hospital_title', '主任护师', '9', 9, 0, 1), (36, 'sys_hospital_title', '副主任护师', '10', 10, 0, 1), (37, 'sys_hospital_title', '主管护师', '11', 11, 0, 1), (38, 'sys_hospital_title', '护师', '12', 12, 0, 1), (39, 'hospital_position', '院长', '1', 1, 0, 1), (40, 'hospital_position', '副院长', '2', 2, 0, 1), (41, 'hospital_position', '科室主任', '3', 3, 0, 1), (42, 'hospital_position', '科室副主任', '4', 4, 0, 1), (43, 'hospital_position', '组长', '5', 5, 0, 1), (44, 'hospital_position', '普通员工', '6', 6, 1, 1), (45, 'his_education', '博士研究生', '1', 1, 0, 1), (46, 'his_education', '硕士研究生', '2', 2, 0, 1), (47, 'his_education', '本科', '3', 3, 1, 1), (48, 'his_education', '大专', '4', 4, 0, 1), (49, 'his_education', '中专', '5', 5, 0, 1), (50, 'his_emp_cert_type', '医师执业证书', '1', 1, 0, 1), (51, 'his_emp_cert_type', '护士执业证书', '2', 2, 0, 1), (52, 'his_emp_cert_type', '药师执业证书', '3', 3, 0, 1), (53, 'his_emp_cert_type', '卫生专业技术资格证书', '4', 4, 0, 1), (54, 'his_emp_cert_type', '其他', '9', 9, 0, 1), (55, 'his_emp_cert_org', '国家卫生健康委员会', '1', 1, 0, 1), (56, 'his_emp_cert_org', '国家药品监督管理局', '2', 2, 0, 1), (57, 'his_emp_cert_org', '省级卫生健康委员会', '3', 3, 0, 1), (58, 'his_emp_cert_org', '市级卫生健康委员会', '4', 4, 0, 1), (59, 'his_emp_cert_org', '其他', '9', 9, 0, 1);

-- 系统参数
INSERT IGNORE INTO sys_config (config_id, config_name, config_key, config_value, config_type, is_system) VALUES (1, '平台名称',   'mss.platform.name', '医保结算经办平台', 0, 1), (2, '定点机构编码', 'mss.medins.code',  'H4301000001',     1, 1), (3, '定点机构名称', 'mss.medins.name',  '长沙市麓康医院',  1, 1);

-- =====================================================================
-- 基金监管铺底：内置监管规则 + 菜单 + 角色授权
-- =====================================================================

-- 内置监管规则（规则引擎按 rule_type 分派扫描逻辑）
INSERT IGNORE INTO mif_audit_rule (id, rule_code, rule_name, rule_type, target, severity, params, description, status) VALUES
(1, 'RULE_DUP_CHARGE',     '重复收费',          'duplicate_charge',         1, 2, '{"minAmount":200}',     '同一结算单同项目多次计费且合计达阈值', 1),
(2, 'RULE_OVER_LIMIT',     '超限价收费',        'over_limit_price',         1, 2, '{"overRatio":0.1}',      '项目单价高于医保限价比例阈值',       1),
(3, 'RULE_SPLIT_HOSP',     '分解住院',          'split_hospitalization',    2, 3, '{"windowDays":15,"minCount":2}', '同一参保人短期内多次住院结算', 1),
(4, 'RULE_BED_HANG',       '挂床住院',          'bed_hanging',              1, 2, '{"minDays":10,"maxAmount":500}', '住院天数长而费用极低',       1),
(5, 'RULE_ABNORMAL_COST',  '高额住院费用',      'abnormal_cost',            2, 3, '{"avgTimes":3}',         '住院费用显著高于同险种均值',         1),
(6, 'RULE_PAYCLASS_C',     '丙类项目入统筹',    'payclass_c',               1, 2, '{"minAmount":200}',     '结算单含丙类项目金额并发生统筹支付',   1);

-- 基金监管菜单（目录=13，子菜单 131-135）


-- 新接入模块菜单（结算清单 / 目录分表 / 库存盘点 / 操作日志 / 待办中心）


-- 基金监管角色授权：经办管理员全量 + 审核员监管全量


-- 新接入模块角色授权：结算清单/目录分表/库存盘点 → 经办管理员 + 经办员；操作日志/待办中心 → 经办管理员 + 审核员
-- 注意：子菜单必须连父目录一起授权，否则菜单树挂不上（父级缺失时子菜单不显示）


-- 知识库铺底（两库之知识库示例条目）
INSERT IGNORE INTO mif_knowledge (id, kb_code, kb_name, kb_type, content, source, status) VALUES
(1, 'KB_DRUG_001', '格列美脲片每日最大剂量', 'drug_knowledge', '2型糖尿病患者格列美脲片每日最大推荐剂量不超过 6mg，超量用药属不合理用药。', '药品说明书', 1),
(2, 'KB_CLINIC_001', '门诊慢特病用药与备案病种一致', 'clinical_guideline', '门诊慢特病患者开具药品及检验检查项目应与备案病种相符，不符项目不予统筹支付。', '临床诊疗规范', 1),
(3, 'KB_POLICY_001', '分解住院判定口径', 'policy', '同一参保人短期内（15天内）多次住院结算且无明显临床指征，判定为分解住院。', '医保基金监管规则', 1);

-- 定点协议铺底
INSERT IGNORE INTO mis_agreement (id, medins_code, medins_name, medins_type, agreement_no, agreement_status, sign_date, expire_date, service_scope, annual_score) VALUES
(1, 'H4301000001', '长沙市麓康医院', 1, 'AG2026-001', 1, '2026-01-01', '2026-12-31', '门诊统筹、住院统筹、慢特病门诊', 92),
(2, 'H4301000002', '康民大药房', 2, 'AG2026-002', 1, '2026-01-01', '2026-12-31', '医保定点零售药店购药', 88),
(3, 'H4301000003', '湘江社区卫生服务中心', 1, 'AG2026-003', 0, NULL, NULL, '门诊统筹、家庭医生签约', NULL);

-- 手工报销铺底
INSERT IGNORE INTO mis_manual_claim (id, claim_no, person_name, id_card, insurance_type, claim_type, visit_date, total_amount, insurance_pay, personal_pay, status, apply_reason) VALUES
(1, 'BX202611010001', '陈女士', '430102198003052241', '3', 1, '2026-10-20', 860.00, 0.00, 860.00, 0, '省外门诊慢特病费用报销'),
(2, 'BX202611010002', '周先生', '430102197511203318', '2', 2, '2026-10-15', 12500.00, 0.00, 12500.00, 1, '异地住院费用报销，已审核待拨付');

-- 信用管理铺底
INSERT IGNORE INTO mif_credit (id, subject_type, subject_code, subject_name, credit_level, score, violation_count, punish_type) VALUES
(1, 1, 'H4301000001', '长沙市麓康医院', 'A', 95, 0, 1),
(2, 1, 'H4301000002', '康民大药房', 'B', 82, 1, 0),
(3, 2, 'YB001', '结算经办员', 'B', 88, 0, 0);

-- 结算清单质控规则铺底
INSERT IGNORE INTO mif_quality_rule (id, rule_code, rule_name, rule_type, severity, deduct_ratio, deduct_type, deduct_amount, params, status, description) VALUES
(1, 'QC-001', '清单必填项完整性',   'required_field',     2, 0.0000, 1, 0.00, NULL, 1, '项目名称/医保编码/数量/单价任一为空即质控不通过'),
(2, 'QC-002', '医保目录对照核查',   'not_mapped',         2, 0.1000, 1, 0.00, NULL, 1, '医保编码未在目录中完成对照的，费用不予结算'),
(3, 'QC-003', '超限价费用核查',     'over_limit_price',   2, 0.2000, 1, 0.00, '{"overRatio":0}', 1, '结算单价高于医保限价的部分按规定扣减'),
(4, 'QC-004', '数量合理性核查',     'invalid_qty',        2, 0.3000, 1, 0.00, '{"maxQty":50}', 1, '数量非正或超过阈值的，疑分解收费'),
(5, 'QC-005', '支付类别填报核查',   'pay_class_missing',  1, 0.0000, 1, 0.00, NULL, 1, '支付类别须正确填报甲/乙/丙'),
(6, 'QC-006', '清单重复上报核查',   'duplicate_report',   3, 0.5000, 1, 0.00, '{"minAmount":0}', 1, '同一结算单内同项目同金额多次出现的，按重复收费扣减'),
(7, 'QC-007', '金额勾稽一致性核查', 'amount_mismatch',    2, 0.0000, 1, 0.00, '{"tolerance":0.01}', 1, '明细金额与单价×数量不一致的'),
(8, 'QC-008', '目录有效性核查',     'catalog_disabled',   3, 1.0000, 1, 0.00, NULL, 1, '目录条目已停用仍结算的，全额扣减'),
(9, 'QC-009', '乙类自付比例核查',   'self_ratio_missing', 1, 0.0000, 1, 0.00, NULL, 1, '乙类项目未维护先行自付比例的'),
(10, 'QC-010', '性别限制项目核查',  'gender_limit',       3, 0.0000, 1, 0.00, NULL, 0, '限定性别项目与参保人性别不符的（按需启用）');

-- 药品追溯码台账铺底
INSERT IGNORE INTO mif_trace_code (id, code, drug_name, spec, manufacturer, batch_no, expire_date, status, verify_count) VALUES
(1, '81AA00010001', '阿莫西林胶囊', '0.25g*24粒', '华北制药股份有限公司', 'B20260801', '2027-08-01', 1, 0),
(2, '81AA00010002', '阿莫西林胶囊', '0.25g*24粒', '华北制药股份有限公司', 'B20260802', '2027-08-02', 1, 0),
(3, '81AA00010003', '阿莫西林胶囊', '0.25g*24粒', '石药集团中诺药业',    'B20260715', '2027-07-15', 1, 0),
(4, '81BB00020001', '头孢克肟分散片', '0.1g*6片', '广州白云山制药股份有限公司', 'B20260910', '2027-09-10', 1, 0),
(5, '81CC00030001', '连花清瘟胶囊', '0.35g*24粒', '石家庄以岭药业股份有限公司', 'B20260320', '2027-03-20', 1, 0),
(6, '81DD00040001', '布洛芬缓释胶囊', '0.3g*20粒', '中美天津史克制药有限公司', 'B20260505', '2027-05-05', 1, 0),
(7, '81EE00050001', '氯化钠注射液', '250ml', '石药集团第一制药有限公司', 'B20250101', '2026-12-31', 1, 0),
(8, '81FF00060001', '奥美拉唑肠溶胶囊', '20mg*14粒', '山东罗欣药业集团股份有限公司', 'B20250801', '2027-08-01', 1, 0);

-- 追溯码核验日志铺底
INSERT IGNORE INTO mif_trace_verify_log (id, code, bill_no, item_name, drug_name, verify_result, problem_desc, verifier, verify_time) VALUES
(1, '81EE00050001', 'YL20261012345', '氯化钠注射液', '氯化钠注射液', 5, '追溯码 81EE00050001 有效期至 2026-12-31，已过期仍结算', '审核员B', '2026-11-01 09:12:00'),
(2, '', 'YL20261023456', '阿莫西林胶囊', NULL, 2, '项目「阿莫西林胶囊」未采集到追溯码，金额 36.00 元', '审核员B', '2026-11-01 09:15:00'),
(3, '81AA00010001', 'YL20261034567', '阿莫西林胶囊', '阿莫西林胶囊', 1, '追溯码 81AA00010001 核验通过，药品「阿莫西林胶囊」信息一致', '审核员B', '2026-11-01 09:18:00');

-- 医保关系转移接续铺底
INSERT IGNORE INTO mis_transfer (id, transfer_no, person_name, id_card, insurance_no, transfer_type, from_region, to_region, apply_date, status, pay_years, fund_balance, handler, handle_opinion) VALUES
(1, 'ZY202611010001', '王秀英', '430103198203154321', 'YB43010300123', 2, '长沙市', '株洲市', '2026-10-20', 4, 12.5, 2350.60, '经办员A', '已受理并完成转出，等待转入地确认'),
(2, 'ZY202611010002', '陈静',   '430104196812056789', 'YB43010400567', 1, '长沙市', '湘潭市', '2026-10-28', 1, 0.00, 0.00, NULL, NULL);

-- 个人账户流水铺底
INSERT IGNORE INTO mis_account_txn (id, txn_no, id_card, person_name, txn_type, amount, balance_after, txn_time, reason, status) VALUES
(1, 'ZH2026100100001', '430103198203154321', '王秀英', 1, 1200.00, 1200.00, '2026-10-01 09:00:00', '职工医保个人账户划入', 1),
(2, 'ZH2026100100002', '430103198203154321', '王秀英', 2, -350.40,  849.60, '2026-10-12 15:20:00', '门诊结算个人账户划扣', 1),
(3, 'ZH2026100100003', '430104196812056789', '陈静',   1, 800.00,  800.00,  '2026-10-01 09:05:00', '职工医保个人账户划入', 1);

-- 门诊慢特病认定铺底
INSERT IGNORE INTO mis_chronic (id, record_no, person_name, id_card, insurance_no, disease_code, disease_name, disease_level, apply_date, doctor, hospital, status, valid_from, valid_to, reviewer, review_opinion) VALUES
(1, 'MB202610010001', '王秀英', '430103198203154321', 'YB43010300123', 'MB-T2DM', '2型糖尿病', 2, '2026-10-15', '张建国', '长沙市麓康医院', 2, '2026-11-01', '2027-10-31', '审核员B', '符合门诊慢特病病种目录及认定标准，同意享受门诊慢特病待遇'),
(2, 'MB202610010002', '李明',   '430105199205183322', 'YB43010500987', 'MB-CKD',  '慢性肾脏病', 2, '2026-10-26', '李秀英', '湘江社区卫生服务中心', 1, NULL, NULL, NULL, NULL),
(3, 'MB202610010003', '周强',   '430106197703224411', 'YB43010600321', 'MB-SP',   '精神分裂症', 1, '2026-10-08', '王伟',   '康民大药房', 3, NULL, NULL, '审核员B', '所提供病历资料不完整，暂不符合门诊特殊病种认定条件，予以退回');

-- 双随机抽查任务铺底
INSERT IGNORE INTO mif_random_task (id, task_no, task_name, check_type, plan_count, plan_checker_count, status, dept_name, remark) VALUES
(1, 'SR20261001001', '2026年第三季度定点医疗机构日常检查', 1, 3, 2, 1, '基金监管科', '按双随机原则随机抽取检查对象与检查人员，结果对外公开'),
(2, 'SR20261001002', '零售药店专项检查（串换项目专项）',   2, 2, 2, 1, '基金监管科', '聚焦定点零售药店串换项目、串换药品行为');

-- 机构自查自纠铺底
INSERT IGNORE INTO mif_self_check (id, task_no, medins_code, medins_name, task_name, check_scope, start_date, deadline, status, submit_content, reviewer, review_opinion) VALUES
(1, 'ZC20261001001', 'H4301000001', '长沙市麓康医院', '2026年10月医保基金使用自查', '医保物价执行、串换项目、慢特病用药合规、基金结算单据自查', '2026-10-25', '2026-11-05', 3, '自查发现3个丙类项目未标注自费提示，已对相关科室通报并完成整改；涉及金额0元。', '医保稽核员', '自查情况属实，整改措施到位，予以办结。'),
(2, 'ZC20261001002', 'H4301000002', '康民大药房',     '零售药店不规范使用医保卡自查', '医保卡使用登记、处方真实性、串换项目自查', '2026-10-25', '2026-11-05', 1, NULL, NULL, NULL);

-- DRG/DIP 病组与病种分值池铺底
INSERT IGNORE INTO mif_drg_group (id, group_code, group_name, pay_type, weight, rate, point_value, point_cost, avg_cost, case_count, status, description) VALUES
(1, 'DRG-101', '肺炎-轻度伴并发症',     1, 1.20, 3200.00, 0,    0,    4200.00, 0, 1, '权重 1.20，费率 3200 元/权重'),
(2, 'DRG-102', '急性心肌梗死',          1, 2.50, 3600.00, 0,    0,    9800.00, 0, 1, '权重 2.50，费率 3600 元/权重'),
(3, 'DRG-103', '阑尾切除术',            1, 0.85, 2800.00, 0,    0,    2600.00, 0, 1, '权重 0.85，费率 2800 元/权重'),
(4, 'DRG-104', '脑梗死（急性期）',      1, 1.90, 3400.00, 0,    0,    7600.00, 0, 1, '权重 1.90，费率 3400 元/权重'),
(5, 'DIP-201', '肺炎-普通型',           2, 0,    0,       1200, 45,   4200.00, 0, 1, '分值 1200，每分值成本 45 元'),
(6, 'DIP-202', '高血压（伴并发症）',    2, 0,    0,       900,  48,   5600.00, 0, 1, '分值 900，每分值成本 48 元'),
(7, 'DIP-203', '糖尿病（普通型）',      2, 0,    0,       800,  46,   4900.00, 0, 1, '分值 800，每分值成本 46 元');

-- 举报投诉铺底
INSERT IGNORE INTO mif_report (id, report_no, reporter_name, reporter_contact, reported_medins_code, reported_medins_name, reported_subject, problem_type, content, status, handle_user, handle_opinion, reward_amount, create_time) VALUES
(1, 'JB202611010001', '张先生', '138****1234', 'H4301000001', '长沙市麓康医院', '门诊内科王某', '违规收费', '2026-10-28 在该院门诊就诊，检验项目收费高于医保限价且未告知患者。', 2, '医保稽核员', '已受理，核查该院检验科收费标准与限价执行情况', 0, '2026-10-29 09:20:00'),
(2, 'JB202611010002', '匿名', NULL, 'H4301000002', '康民大药房', NULL, '欺诈骗保', '该药店为多个参保人代刷医保卡，刷卡时间集中在夜间。', 3, '医保稽核员', '已联合药监部门核查，暂停该药店医保服务协议', 0, '2026-10-25 15:40:00'),
(3, 'JB202611010003', '李女士', '139****5678', 'H4301000003', '湘江社区卫生服务中心', '服务台人员', '服务态度', '办理慢特病备案时告知需等待两个月，实际系统显示已办结。', 4, '医保稽核员', '已办结，责令机构书面致歉并公示', 500, '2026-10-18 11:05:00');

-- 大数据反欺诈模型库铺底（示例模型，params 为模型阈值参数）
INSERT IGNORE INTO mif_fraud_model (id, model_code, model_name, category, risk_level, algorithm, params, status, description) VALUES
(1, 'FRAUD-001', '分解住院识别',   '01', 3, '同一参保人短期内多次入院且单次费用偏低，疑似分解住院', '{"windowDays":30,"maxAdm":3,"minAmount":800}', 1, '识别将一次住院拆分成多次的违规行为'),
(2, 'FRAUD-002', '冒名就医识别',   '02', 3, '就诊频次/时段与历史画像显著偏离，疑似冒名', '{"windowDays":7,"threshold":5}', 1, '基于行为画像识别冒名就医（含同日跨机构结算）'),
(3, 'FRAUD-003', '串换药品识别',   '03', 2, '院内项目与医保目录映射异常，疑似串换高价药', '{"matchRate":0.85}', 1, '识别串换药品/项目行为'),
(4, 'FRAUD-004', '过度医疗识别',   '04', 2, '次均费用/药占比超同组机构均值倍数，疑似过度医疗', '{"multiple":1.8,"cRatio":0.5}', 1, '识别过度检查、过度用药'),
(5, 'FRAUD-005', '空刷医保卡识别', '05', 3, '无对应诊疗记录却发生结算，疑似空刷', '{"minAmount":1}', 1, '识别虚构交易空刷医保卡'),
(6, 'FRAUD-006', '结算数据一致性核查','06', 2, '结算单状态与费用明细冲正状态不一致，存在资金回退风险', NULL, 1, '识别退费未冲正等账务不一致情形');

-- 事前提醒 / 事中拦截规则铺底（category 决定引擎判定逻辑，params 为阈值参数）
INSERT IGNORE INTO mif_precheck_rule (id, rule_code, rule_name, trigger_stage, risk_level, prompt_text, block_flag, category, params, status, description) VALUES
(1, 'PRE-001', '冒名就医预警',     'in_settle', 3, '结算登记身份与参保库登记信息不一致，疑似冒名就医', 1, '冒名', NULL, 1, '结算关口实时比对持卡人身份信息，命中即阻断'),
(2, 'PRE-002', '超量开药提醒',     'before_settle', 2, '同一项目开量超过阈值，请确认用药合理性', 0, '超量开药', '{"maxQty":10}', 1, '开方前实时比对说明书限量'),
(3, 'PRE-003', '重复收费预警',     'in_settle', 2, '同一结算单同项目重复计费，疑似重复收费', 1, '重复收费', '{"minAmount":100}', 1, '结算时比对同一结算单内项目明细'),
(4, 'PRE-004', '超限价提醒',       'in_settle', 1, '项目价格超过医保限价，按限价结算', 0, '超限价', '{"overRatio":0}', 1, '结算时实时比对医保限价'),
(5, 'PRE-005', '参保状态异常提醒', 'before_settle', 3, '参保人未登记或处于停保状态，不应发生医保结算', 1, '参保异常', NULL, 1, '结算前核验参保状态，命中即阻断'),
(6, 'PRE-006', '目录未对照提醒',   'before_settle', 1, '存在未完成医保目录对照的项目', 0, '目录未对照', NULL, 1, '结算前核验院内项目与医保目录对照关系'),
(7, 'PRE-007', '自付占比过高提醒', 'before_settle', 1, '个人自付占比超过阈值，请核对待遇政策', 0, '自付占比过高', '{"selfPayRatio":0.5}', 1, '结算前测算个人自付占比'),
(8, 'PRE-008', '重复结算拦截',     'in_settle', 3, '同一业务单号已存在有效结算单，疑似重复结算', 1, '重复结算', NULL, 1, '结算关口核验业务单号唯一性'),
(9, 'PRE-009', '高频就医拦截',     'in_settle', 2, '参保人窗口期内结算次数超阈值，涉嫌高频就医', 1, '高频就医', '{"windowDays":7,"threshold":5}', 1, '结算关口比对近期结算频次'),
(10, 'PRE-010', '分解住院预警',    'in_settle', 3, '参保人窗口期内多次住院结算，疑似分解住院', 1, '分解住院', '{"windowDays":30,"minCount":2}', 1, '结算关口比对窗口期内住院次数'),
(11, 'PRE-011', '同日住院门诊提醒','in_settle', 2, '参保人同日既有住院又有门诊结算，涉嫌重复就医', 1, '同日住院门诊', NULL, 1, '结算关口比对同日就诊轨迹'),
(12, 'PRE-012', '空刷医保卡提醒',  'in_settle', 3, '存在费用明细但结算金额几乎为零，疑似空刷医保卡', 1, '空刷医保卡', '{"minAmount":1}', 1, '结算关口校验费用与金额匹配性'),
(13, 'PRE-013', '丙类入统筹提醒',  'in_settle', 2, '含丙类（自费）项目且仍发生统筹支付，涉嫌串换项目', 1, '丙类入统筹', '{"minAmount":200}', 1, '结算关口比对自费项目与统筹支付');


-- ==================== 补齐表结构铺底 ====================

-- 平台公共：行政区划
INSERT IGNORE INTO sys_area (id, area_code, area_name, parent_code, area_level, sort_order, status) VALUES
(1, '430000', '湖南省', NULL, 1, 1, 1),
(2, '430100', '长沙市', '430000', 2, 1, 1),
(3, '430200', '株洲市', '430000', 2, 2, 1),
(4, '430300', '湘潭市', '430000', 2, 3, 1),
(5, '430102', '芙蓉区', '430100', 3, 1, 1),
(6, '430103', '天心区', '430100', 3, 2, 1),
(7, '430104', '岳麓区', '430100', 3, 3, 1),
(8, '430111', '雨花区', '430100', 3, 4, 1);

-- 平台公共：接口报文模板（医保接口清单）
INSERT IGNORE INTO sys_api_template (id, api_code, api_name, api_direction, message_type, version, status) VALUES
(1, '2301', '实时结算请求', 1, 'JSON', '1.0', 1),
(2, '2304', '机构结算结果报盘', 2, 'JSON', '1.0', 1),
(3, '2305', '结算回执', 2, 'JSON', '1.0', 1),
(4, '2201', '参保信息查询', 1, 'JSON', '1.0', 1),
(5, '2401', '目录下载', 1, 'JSON', '1.0', 1);

-- 平台公共：待办任务
INSERT IGNORE INTO sys_todo (id, todo_type, todo_title, biz_type, biz_id, biz_no, assignee, assignee_id, assign_dept, priority, status) VALUES
(1, 'mif_suspicion', '疑点稽核待处理：同一参保人短期内多次住院', 'mif_suspicion', 1, 'YC202611010001', '审核员B', 3, '基金监管科', 1, 0),
(2, 'mif_fraud_clue', '反欺诈线索待核查：冒名就医', 'mif_fraud_clue', 1, 'CL20261101000001', '审核员B', 3, '基金监管科', 1, 0),
(3, 'mis_fee_audit', '费用审核待办：丙类项目需确认', 'mis_fee_audit', 1, 'YL20261101000001', '审核员B', 3, '费用审核科', 2, 0),
(4, 'mif_stock_check', '库存盘点待复核', 'mif_stock_check', 1, 'PD202611010001', '审核员B', 3, '药械科', 2, 0);

-- 平台公共：站内通知
INSERT IGNORE INTO sys_notice (id, notice_title, notice_type, notice_content, receiver, receiver_id, read_flag, send_time) VALUES
(1, '事前提醒规则已更新', 1, '规则 PRE-004 超限价提醒参数已调整，请关注。', 'admin', 1, 0, '2026-11-01 08:30:00'),
(2, '结算单待审核', 2, '结算单 YL20261101000001 存在丙类项目，需人工复核。', '审核员B', 3, 0, '2026-11-01 09:10:00'),
(3, '库存低于预警下限', 4, '阿莫西林胶囊当前库存 42，低于预警下限 60，请及时采购入库。', '采购管理员', 2, 0, '2026-11-01 09:40:00');

-- 结算经办：待遇政策配置
INSERT IGNORE INTO mis_benefit_config (id, config_code, config_name, insurance_type, encounter_type, deductible, cap_amount, pay_ratio, account_ratio, status) VALUES
(1, 'BF001', '职工医保门诊统筹', '职工医保', '门诊', 300.00, 8000.00, 0.8500, 0.4000, 1),
(2, 'BF002', '职工医保住院统筹', '职工医保', '住院', 300.00, 45000.00, 0.8500, 0.4000, 1),
(3, 'BF003', '居民医保门诊统筹', '居民医保', '门诊', 500.00, 4000.00, 0.7000, 0.0000, 1),
(4, 'BF004', '居民医保住院统筹', '居民医保', '住院', 500.00, 25000.00, 0.7000, 0.0000, 1);

-- 结算经办：缴费档次
INSERT IGNORE INTO mis_pay_level (id, level_code, level_name, insurance_type, pay_base, pay_amount, account_ratio, benefit_level, effective_start, effective_end, status) VALUES
(1, 'ZG-60',  '职工医保 60% 档次', '职工医保', 6000.00, 660.00, 0.0800, '一档', '2026-01-01', '2026-12-31', 1),
(2, 'ZG-80',  '职工医保 80% 档次', '职工医保', 8000.00, 880.00, 0.0800, '二档', '2026-01-01', '2026-12-31', 1),
(3, 'ZG-100', '职工医保 100% 档次', '职工医保', 10000.00, 1100.00, 0.0800, '三档', '2026-01-01', '2026-12-31', 1),
(4, 'JM-300', '居民医保 300 元档', '居民医保', 0.00, 300.00, 0.0000, '居民档', '2026-01-01', '2026-12-31', 1),
(5, 'JM-400', '居民医保 400 元档', '居民医保', 0.00, 400.00, 0.0000, '居民档', '2026-01-01', '2026-12-31', 1);

-- 结算经办：病种目录
INSERT IGNORE INTO mis_disease_catalog (id, disease_code, disease_name, icd_code, disease_level, pay_ratio, deductible, cap_amount, limit_days, status) VALUES
(1, 'MB-T2DM',  '2型糖尿病',          'E11', 2, 0.8000, 200.00, 3000.00, 365, 1),
(2, 'MB-CKD',   '慢性肾脏病',        'N18', 2, 0.8000, 200.00, 3000.00, 365, 1),
(3, 'MB-CVD',   '冠心病',            'I25', 2, 0.8000, 300.00, 5000.00, 365, 1),
(4, 'MB-STROKE','脑卒中后遗症',      'I69', 2, 0.8000, 300.00, 5000.00, 365, 1),
(5, 'MB-SP',    '精神分裂症',        'F20', 1, 0.8500, 0.00, 0.00, 0, 1),
(6, 'MB-TB',    '肺结核',            'A16', 1, 0.8500, 0.00, 0.00, 0, 1),
(7, 'MB-HD',    '慢性心力衰竭',      'I50', 2, 0.8000, 300.00, 5000.00, 365, 1);

-- 结算经办：参保缴费记录
INSERT IGNORE INTO mis_insure_pay_record (id, person_id, id_card, person_name, group_code, pay_year, pay_month, pay_level_code, pay_base, pay_amount, account_amount, pay_type, pay_status, pay_time) VALUES
(1, 1, '430102199001011234', '张三', 'DW001', '2026', '2026-10', 'ZG-100', 10000.00, 1100.00, 88.00, 1, 1, '2026-10-05 10:00:00'),
(2, 1, '430102199001011234', '张三', 'DW001', '2026', '2026-11', 'ZG-100', 10000.00, 1100.00, 88.00, 1, 1, '2026-11-05 10:00:00'),
(3, 2, '430102198505056789', '李四', 'DW002', '2026', '2026-10', 'JM-400', 0.00, 400.00, 0.00, 1, 1, '2026-10-05 10:05:00'),
(4, 3, '430102197012121234', '王五', 'DW001', '2026', '2026-10', 'ZG-80',  8000.00, 880.00, 70.40, 1, 1, '2026-10-05 10:10:00');

-- 结算经办：药品生产企业
INSERT IGNORE INTO mis_manufacturer (id, manufacturer_code, manufacturer_name, credit_code, license_no, contact, phone, region, status) VALUES
(1, 'QY001', '华北制药股份有限公司', '91130100100000001A', '冀食药监械生产许20160001号', '张经理', '0311-8888888', '石家庄市', 1),
(2, 'QY002', '石药集团中诺药业有限公司', '91130100100000002B', '冀食药监械生产许20160002号', '李经理', '0311-8777777', '石家庄市', 1),
(3, 'QY003', '广州白云山制药股份有限公司', '91440100100000003C', '粤食药监械生产许20160003号', '王经理', '020-8666666', '广州市', 1),
(4, 'QY004', '山东罗欣药业集团股份有限公司', '91371300100000004D', '鲁食药监械生产许20160004号', '刘经理', '0539-8555555', '临沂市', 1);

-- 结算经办：药品目录
INSERT IGNORE INTO mis_drug_catalog (id, drug_code, drug_name, generic_name, dosage_form, strength, manufacturer_code, yb_code, pay_class, self_ratio, ins_ratio, limit_price, unit, status) VALUES
(1, 'YP0001', '阿莫西林胶囊', '阿莫西林', '胶囊', '0.25g*24粒', 'QY001', 'X000000001', '甲', 0.0000, 0.8500, 12.50, '盒', 1),
(2, 'YP0002', '头孢克肟分散片', '头孢克肟', '分散片', '0.1g*6片', 'QY001', 'X000000002', '甲', 0.0000, 0.8500, 18.60, '盒', 1),
(3, 'YP0003', '氯化钠注射液', '氯化钠', '注射液', '250ml', 'QY002', 'X000000003', '甲', 0.0000, 0.8500, 3.20, '瓶', 1),
(4, 'YP0004', '奥美拉唑肠溶胶囊', '奥美拉唑', '肠溶胶囊', '20mg*14粒', 'QY002', 'X000000004', '乙', 0.1000, 0.8000, 26.80, '盒', 1),
(5, 'YP0005', '布洛芬缓释胶囊', '布洛芬', '缓释胶囊', '0.3g*20粒', 'QY004', 'X000000005', '甲', 0.0000, 0.8500, 21.40, '盒', 1),
(6, 'YP0006', '连花清瘟胶囊', '连花清瘟', '胶囊', '0.35g*24粒', 'QY003', 'Z000000001', '乙', 0.1000, 0.8000, 29.90, '盒', 1);

-- 结算经办：结算清单（由经办结算单派生的清单主表）
INSERT IGNORE INTO mis_settle_list (id, list_no, bill_id, bill_no, medins_code, medins_name, patient_name, id_card_mask, insurance_type, encounter_type, item_count, total_amount, insurance_pay, list_version, upload_batch_no, check_status, upload_time) VALUES
(1, 'QL20261101000001', 1, 'YL20261101000001', 'H4301000001', '长沙市麓康医院', '李建国', '430102********3456', '职工医保', '门诊', 5, 268.00, 181.20, '1.0', 'PC20261101001', 0, '2026-11-01 08:20:00'),
(2, 'QL20261101000002', 2, 'YL20261101000002', 'H4301000001', '长沙市麓康医院', '王秀英', '430103********4321', '居民医保', '门诊', 4, 152.50, 0.00, '1.0', 'PC20261101001', 0, '2026-11-01 08:22:00');

-- 基金监管：规则参数
INSERT IGNORE INTO mif_rule_param (id, rule_id, rule_code, param_key, param_value, param_name, value_type, remark) VALUES
(1, 3, 'duplicate_charge', 'minAmount', '100', '重复收费金额阈值（元）', 'decimal', '同项目重复计费金额达到该值即命中'),
(2, 4, 'over_limit_price', 'overRatio', '0.1', '超限价容忍比例', 'decimal', '超出限价 10% 以内不命中'),
(3, 1, 'split_hospitalization', 'windowDays', '15', '分解住院观察窗口（天）', 'int', '15 天内多次住院即命中'),
(4, 2, 'bed_hanging', 'minDays', '10', '挂床住院最小住院天数', 'int', '住院满 10 天且费用极低即命中');

-- 基金监管：规则版本
INSERT IGNORE INTO mif_rule_version (id, rule_id, rule_code, version, rule_snapshot, change_reason, effective_time, operator, status) VALUES
(1, 1, 'duplicate_charge', '1.0', '{"minAmount":100}', '初始版本上线', '2026-01-01 00:00:00', '系统管理员', 0),
(2, 1, 'duplicate_charge', '1.1', '{"minAmount":100,"severity":2}', '提高严重程度至警示级', '2026-06-01 00:00:00', '系统管理员', 1);

-- 基金监管：反欺诈风险特征库
INSERT IGNORE INTO mif_fraud_feature (id, feature_code, feature_name, feature_type, data_source, calc_expr, default_value, weight, description, status) VALUES
(1, 'F001', '近30天跨机构就医次数', '频次', 'mis_settlement', 'count(distinct fix_medins_code) where id_card=? and bill_date>=now()-30d', '0', 0.3000, '同一身份证短期在多家机构结算', 1),
(2, 'F002', '次均费用偏离倍数', '金额', 'mis_bill', 'avg(total_amount) / avg_total_amount_by_insurance_type', '1.0', 0.2500, '费用显著偏离同险种均值', 1),
(3, 'F003', '同一结算单重复项目数', '频次', 'mis_fee_item', 'count(*) group by item_name having count(*)>1', '0', 0.2000, '清单内同项目重复出现', 1),
(4, 'F004', '夜间结算占比', '时序', 'mis_settlement', 'night_settle_count / total_settle_count', '0', 0.1000, '深夜结算比例异常偏高', 1),
(5, 'F005', '参保人与结算人姓名一致性', '关联', 'mis_insured_person', 'name_match(id_card, patient_name)', '1', 0.1500, '结算姓名与参保库登记不一致', 1);

-- 基金监管：病组明细（DRG 入组条件）
INSERT IGNORE INTO mif_drg_group_detail (id, group_code, detail_seq, icd_code, icd_name, weight, age_limit, gender_limit, cost_low, cost_high) VALUES
(1, 'DRG-101', 1, 'J18.9', '肺炎,未特指', 1.20, '不限', '不限', 2000.00, 8000.00),
(2, 'DRG-102', 1, 'I21.0', '急性心肌梗死', 2.50, '不限', '不限', 6000.00, 20000.00),
(3, 'DRG-103', 1, 'K35.9', '急性阑尾炎,未特指', 0.85, '不限', '不限', 1500.00, 5000.00),
(4, 'DRG-104', 1, 'I63.9', '脑梗死,未特指', 1.90, '不限', '不限', 4000.00, 14000.00);

-- 基金监管：信用记分明细
INSERT IGNORE INTO mif_credit_detail (id, subject_type, subject_code, subject_name, event_type, score_change, score_after, source_type, source_no, reason, occur_time, operator) VALUES
(1, 1, 'H4301000001', '长沙市麓康医院', '违规扣分', -6.00, 86.00, 'mif_recover', 'RC20261001001', '超限价收费，经核查属实', '2026-10-20 10:00:00', '稽核员A'),
(2, 1, 'H4301000002', '康民大药房', '违规扣分', -12.00, 76.00, 'mif_suspicion', 'YC20261001002', '串换项目且刷卡时间异常', '2026-10-18 14:30:00', '稽核员A'),
(3, 2, 'YB001', '王建国', '守信加分', 2.00, 90.00, 'mif_audit', 'SH20261001001', '连续12个月费用审核零驳回', '2026-10-25 09:00:00', '系统自动');

-- 基金监管：盘点单
INSERT IGNORE INTO mif_stock_check (id, check_no, medins_code, medins_name, check_date, item_count, diff_count, diff_amount, status, checker, reviewer, review_time) VALUES
(1, 'PD202611010001', 'H4301000001', '长沙市麓康医院', '2026-11-01', 12, 2, 156.80, 2, '药械管理员', NULL, NULL),
(2, 'PD202610280001', 'H4301000001', '长沙市麓康医院', '2026-10-28', 15, 1, 42.00, 3, '药械管理员', '审核员B', '2026-10-29 10:00:00');

-- 基金监管：盘点明细
INSERT IGNORE INTO mif_stock_check_item (id, check_no, item_code, item_name, book_qty, real_qty, diff_qty, diff_amount, diff_reason) VALUES
(1, 'PD202611010001', 'YP0001', '阿莫西林胶囊', 58.00, 60.00, 2.00, 25.00, '出库未登记'),
(2, 'PD202611010001', 'YP0004', '奥美拉唑肠溶胶囊', 30.00, 25.00, -5.00, 134.00, '近效期报损未过账'),
(3, 'PD202610280001', 'YP0002', '头孢克肟分散片', 120.00, 118.00, -2.00, 37.20, '盘点差异待核');

-- 基金监管：行政处罚
INSERT IGNORE INTO mif_penalty (id, penalty_no, medins_code, medins_name, penalty_type, penalty_reason, penalty_amount, penalty_date, decision_doc, status) VALUES
(1, 'CF20261001001', 'H4301000002', '康民大药房', '暂停', '经查实串换项目、虚构门诊费用，情节严重', 20000.00, '2026-10-22', '湘医保罚〔2026〕1号', 2);
