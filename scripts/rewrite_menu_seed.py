# -*- coding: utf-8 -*-
"""
重写 data.sql 的菜单段为三级结构：
  顶级（3）：2 医保结算经办 / 13 医保基金监管 / 9 系统管理
  二级分组（17）：200-206 / 210-215 / 220-223
  三级页面（44）：沿用原 id，parent_id 指向所属二级分组
  按钮（12）：parent_id 指向所属页面

同时重写 sys_role_menu 授权段：按「页面 + 其所有祖先」授权。
"""
import re

DATA = r"E:\work_source\his_cslk\csmis\source\mis_backend\src\main\resources\db\data.sql"

# ------------------------------------------------------------ 顶级
TOPS = [
    (2,  "医保结算经办", "settle",    "Settlement", 10),
    (13, "医保基金监管", "supervise", "Monitor",    20),
    (9,  "系统管理",     "system",    "Setting",    30),
]

# ------------------------------------------------------------ 二级分组
# id: (名称, 顶级, path, menu_key, icon, permission)
GROUPS = {
    200: ("结算经办",   2,  "settle.do",         "group.settle.do",         "CreditCard",      "settle:do:list"),
    201: ("结算与清算", 2,  "settle.fin",        "group.settle.fin",        "Money",           "settle:fin:list"),
    202: ("参保与账户", 2,  "settle.insured",    "group.settle.insured",    "User",            "insured:person:list"),
    203: ("费用与目录", 2,  "settle.catalog",    "group.settle.catalog",    "Notebook",        "catalog:list"),
    204: ("待遇与备案", 2,  "settle.filing",     "group.settle.filing",     "OfficeSupplies",  "filing:list"),
    205: ("进销存",     2,  "settle.stock",      "group.settle.stock",      "Boxes",           "stock:item:list"),
    206: ("经办看板",   2,  "settle.board",      "group.settle.board",      "DataBoard",       "settle:board:view"),
    210: ("监管总览",   13, "supervise.overview","group.supervise.overview","DataBoard",       "supervise:dashboard:view"),
    211: ("规则与知识", 13, "supervise.rule",    "group.supervise.rule",    "SetUp",           "supervise:rule:list"),
    212: ("稽核核查",   13, "supervise.audit",   "group.supervise.audit",   "Search",          "supervise:suspicion:list"),
    213: ("智能监管",   13, "supervise.ai",      "group.supervise.ai",      "Aim",             "supervise:fraud:list"),
    214: ("专项监管",   13, "supervise.special", "group.supervise.special", "Histogram",       "supervise:quality:list"),
    215: ("信用管理",   13, "supervise.credit",  "group.supervise.credit",  "Medal",           "supervise:credit:list"),
    220: ("用户与权限", 9,  "system.user",       "group.system.user",       "User",            "system:user:list"),
    221: ("组织与岗位", 9,  "system.org",        "group.system.org",        "OfficeBuilding",  "system:dept:list"),
    222: ("基础配置",   9,  "system.dict",       "group.system.dict",       "Collection",      "system:dict:list"),
    223: ("审计与待办", 9,  "system.audit",      "group.system.audit",      "Document",        "system:operlog:list"),
}

# ------------------------------------------------------------ 三级页面
# id: (名称, 二级分组, path, component, menu_key, icon, permission)
PAGES = [
    (1,   "经办工作台",         206, "/dashboard",               "dashboard/DashboardView",       "portal.dashboard",    "DataBoard",   "portal:dashboard:view"),
    (21,  "门诊医保结算",       200, "/settlement/outpatient",   "settlement/OutpatientSettleView", "settle.outpatient", "Ticket",     "settle:outpatient:list"),
    (22,  "住院医保结算",       200, "/settlement/inpatient",    "settlement/InpatientSettleView",  "settle.inpatient",  "FirstAidKit", "settle:inpatient:list"),
    (24,  "机构报盘接收",       200, "/settlement/gateway",      "settlement/GatewaySettleView",   "settle.gateway",   "Upload",      "settle:gateway:list"),
    (23,  "结算单管理",         201, "/settlement/bill",         "settlement/SettleBillView",      "settle.bill",      "Document",    "settle:bill:list"),
    (150, "结算清单",           201, "/settle/list",             "settlement/SettleListView",      "settle.list",      "Tickets",     "settle:list:list"),
    (25,  "月度结算管理",       201, "/settlement/monthly",      "settlement/MonthlySettleView",   "settle.monthly",   "Calendar",    "settle:monthly:list"),
    (26,  "日拨付管理",         201, "/settlement/daily",        "settlement/DailyAllocView",      "settle.daily",     "Wallet",      "settle:daily:list"),
    (7,   "清算对账",           201, "/reconciliation",          "reconciliation/ReconciliationView", "recon",          "DataAnalysis","recon:list"),
    (31,  "参保人管理",         202, "/insured/person",          "insured/InsuredPersonView",      "insured.person",  "UserFilled",  "insured:person:list"),
    (32,  "转移接续/账户/慢特病", 202, "/insured/service",      "insured/InsureServiceView",      "insured.service", "Switch",      "insured:service:list"),
    (4,   "费用审核",           203, "/audit/fee",               "audit/FeeAuditView",             "audit.fee",       "Stamp",       "audit:fee:list"),
    (5,   "三大目录与对照",     203, "/catalog",                 "catalog/CatalogView",            "catalog",         "Notebook",    "catalog:list"),
    (151, "目录分表管理",       203, "/settle/catalog",          "settlement/CatalogSplitView",    "settle.catalog",  "Collection",  "settle:catalog:list"),
    (10,  "定点协议管理",       203, "/agreement",               "agreement/AgreementView",        "agreement",      "OfficeSupplies","agreement:list"),
    (11,  "手工报销",           204, "/claim",                   "claim/ManualClaimView",          "claim",          "Money",       "claim:list"),
    (6,   "跨省异地就医备案",   204, "/filing/cross-region",     "filing/CrossRegionFilingView",   "filing.crossRegion","OfficeSupplies","filing:list"),
    (33,  "药品耗材进销存",     205, "/stock",                   "stock/StockView",                "stock.manage",   "Boxes",       "stock:item:list"),
    (152, "库存盘点",           205, "/stock/check",             "stock/StockCheckView",           "stock.check",    "Clipboard",   "stock:check:list"),
    (8,   "统计报表",           206, "/stats",                   "stats/StatsView",                "stats",          "TrendCharts", "stats:view"),

    (131, "监管工作台",         210, "/supervise/dashboard",     "supervise/SuperviseDashboard",   "supervise.dashboard","DataBoard","supervise:dashboard:view"),
    (140, "举报投诉与奖励",     210, "/supervise/report",        "supervise/ReportView",           "supervise.report","ChatDotRound","supervise:report:list"),
    (132, "规则配置",           211, "/supervise/rule",          "supervise/RuleView",             "supervise.rule", "SetUp",       "supervise:rule:list"),
    (136, "知识库",             211, "/supervise/knowledge",     "supervise/KnowledgeView",        "supervise.knowledge","Reading", "supervise:knowledge:list"),
    (133, "疑点稽核",           212, "/supervise/suspicion",     "supervise/SuspicionView",        "supervise.suspicion","Search",  "supervise:suspicion:list"),
    (134, "飞检任务",           212, "/supervise/inspection",    "supervise/InspectionView",       "supervise.inspection","Van",    "supervise:inspection:list"),
    (135, "违规追回",           212, "/supervise/recover",       "supervise/RecoverView",          "supervise.recover","RefreshLeft","supervise:recover:list"),
    (139, "事前提醒/事中拦截",  213, "/supervise/precheck",      "supervise/PrecheckView",         "supervise.precheck","Bell",     "supervise:precheck:list"),
    (138, "大数据反欺诈",       213, "/supervise/fraud",         "supervise/FraudView",            "supervise.fraud", "Aim",        "supervise:fraud:list"),
    (141, "DRG/DIP支付监管",    213, "/drg",                     "drg/DrgView",                    "drg.manage",      "Histogram",   "drg:case:list"),
    (144, "结算清单质控",       214, "/supervise/quality",       "supervise/QualityView",          "supervise.quality","DocumentChecked","supervise:quality:list"),
    (145, "药品追溯码监管",     214, "/supervise/trace",         "supervise/TraceView",            "supervise.trace","Postcard",    "supervise:trace:list"),
    (142, "双随机一公开",       214, "/supervise/random",        "supervise/RandomView",           "supervise.random","Refresh",   "supervise:random:list"),
    (143, "自查自纠",           214, "/supervise/selfcheck",     "supervise/SelfCheckView",        "supervise.selfcheck","Finished","supervise:selfcheck:list"),
    (137, "信用管理",           215, "/supervise/credit",        "supervise/CreditView",           "supervise.credit","Medal",      "supervise:credit:list"),

    (91,  "用户管理",           220, "/system/user",             "system/UserView",                "system.user",    "User",        "system:user:list"),
    (92,  "角色管理",           220, "/system/role",             "system/RoleView",                "system.role",    "Avatar",      "system:role:list"),
    (93,  "菜单管理",           220, "/system/menu",             "system/MenuView",                "system.menu",    "Menu",        "system:menu:list"),
    (94,  "岗位管理",           221, "/system/post",             "system/PostView",                "system.post",    "Postcard",    "system:post:list"),
    (96,  "科室管理",           221, "/system/dept",             "system/DeptView",                "system.dept",    "OfficeBuilding","system:dept:list"),
    (97,  "员工管理",           221, "/system/employee",         "system/EmployeeView",            "system.employee","Avatar",      "org:employee:list"),
    (95,  "字典管理",           222, "/system/dict",             "system/DictView",                "system.dict",    "Collection",  "system:dict:list"),
    (153, "操作日志",           223, "/system/operlog",          "system/OperLogView",             "system.operlog", "Document",    "system:operlog:list"),
    (154, "待办中心",           223, "/system/todo",             "system/TodoView",                "system.todo",    "Bell",        "system:todo:list"),
]

# ------------------------------------------------------------ 按钮
# id: (名称, 所属页面, menu_key, permission)
BTNS = [
    (101, "用户新增", 91,  "system.user.add",    "system:user:add"),
    (102, "用户编辑", 91,  "system.user.edit",   "system:user:edit"),
    (103, "用户删除", 91,  "system.user.delete", "system:user:delete"),
    (104, "角色新增", 92,  "system.role.add",    "system:role:add"),
    (105, "角色删除", 92,  "system.role.delete", "system:role:delete"),
    (106, "菜单新增", 93,  "system.menu.add",    "system:menu:add"),
    (107, "菜单删除", 93,  "system.menu.delete", "system:menu:delete"),
    (108, "字典新增", 95,  "system.dict.add",    "system:dict:add"),
    (109, "字典编辑", 95,  "system.dict.edit",   "system:dict:edit"),
    (110, "字典删除", 95,  "system.dict.delete", "system:dict:delete"),
    (111, "员工新增", 97,  "org.employee.add",    "org:employee:add"),
    (112, "员工删除", 97,  "org.employee.delete", "org:employee:delete"),
]

# ------------------------------------------------------------ 角色授权
# 角色: (顶级域 id, 可见页面 id 集合, 可见按钮 id 集合)
# 授权规则：页面可见 ⇒ 其所属二级分组 + 顶级域都必须授权（否则菜单树挂不上）
ROLE_PAGES = {
    1: {"top": 2, "pages": set(), "btns": set()},   # 经办管理员：全量
    2: {"top": 2, "pages": set(), "btns": set()},   # 经办员
    3: {"top": 13, "pages": set(), "btns": set()},  # 审核稽核员
}

# 经办员：结算与清算/参保与账户/费用与目录(不含审核)/待遇与备案/进销存/经办看板
ROLE_PAGES[2]["pages"] = {
    21, 22, 24,                      # 结算经办
    23, 150, 25, 26, 7,              # 结算与清算
    31, 32,                          # 参保与账户
    5, 151, 10,                      # 费用与目录（不含费用审核，那是审核员的活）
    11, 6,                           # 待遇与备案
    33, 152,                         # 进销存
    1, 8,                            # 经办看板
}
# 审核稽核员：经办侧只给「审核」相关（结算单/结算清单/费用审核）+ 监管侧全部
ROLE_PAGES[3]["pages"] = {
    23, 150,                         # 结算与清算：结算单管理、结算清单
    4,                               # 费用与目录：费用审核
    131, 140,                        # 监管总览
    132, 136,                        # 规则与知识
    133, 134, 135,                   # 稽核核查
    139, 138, 141,                   # 智能监管
    144, 145, 142, 143,              # 专项监管
    137,                             # 信用管理
}
# 经办管理员：全部页面
ROLE_PAGES[1]["pages"] = {p[0] for p in PAGES}
ROLE_PAGES[1]["btns"] = {b[0] for b in BTNS}
ROLE_PAGES[2]["btns"] = set()
ROLE_PAGES[3]["btns"] = set()


def build_menu_block():
    L = []
    L.append("-- ===========================================================")
    L.append("-- 菜单：三级结构")
    L.append("--   顶级（业务域）：2 医保结算经办 · 13 医保基金监管 · 9 系统管理")
    L.append("--   二级（业务分组）：200-206 / 210-215 / 220-223")
    L.append("--   三级（页面）：menu_type=2；按钮 menu_type=3 挂在所属页面下")
    L.append("-- 说明：顶级/二级的 path 只是分组标识（不参与路由跳转），")
    L.append("--       三级页面的 path 与 front_mis 路由一一对应。")
    L.append("-- ===========================================================")
    L.append("")

    # 顶级
    L.append("-- 顶级：业务域")
    rows = []
    for mid, name, key, icon, sort in TOPS:
        rows.append(f"({mid}, '{name}', 0, {sort}, 1, '{key}', NULL, '{key}', '{icon}', NULL)")
    L.append("INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES")
    L.append(",\n".join(rows) + ";")
    L.append("")

    # 二级
    L.append("-- 二级：业务分组目录")
    rows = []
    for gid, (name, top, path, key, icon, perm) in sorted(GROUPS.items()):
        rows.append(f"({gid}, '{name}', {top}, {gid % 100}, 1, '{path}', NULL, '{key}', '{icon}', '{perm}')")
    L.append("INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES")
    L.append(",\n".join(rows) + ";")
    L.append("")

    # 三级页面
    L.append("-- 三级：页面菜单")
    rows = []
    for mid, name, gid, path, comp, key, icon, perm in PAGES:
        rows.append(f"({mid}, '{name}', {gid}, {mid % 100}, 2, '{path}', '{comp}', '{key}', '{icon}', '{perm}')")
    L.append("INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES")
    L.append(",\n".join(rows) + ";")
    L.append("")

    # 按钮
    L.append("-- 按钮权限：挂在所属页面下（menu_type=3）")
    rows = []
    for bid, name, pid, key, perm in BTNS:
        rows.append(f"({bid}, '{name}', {pid}, {bid % 100}, 3, NULL, NULL, '{key}', NULL, '{perm}')")
    L.append("INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, menu_key, icon, permission) VALUES")
    L.append(",\n".join(rows) + ";")
    L.append("")
    return "\n".join(L)


def build_role_block():
    """按「页面 + 其祖先链」生成授权（id 必须显式给：关联表主键无 AUTO_INCREMENT）"""
    page2group = {p[0]: p[2] for p in PAGES}
    group2top = {g: v[1] for g, v in GROUPS.items()}
    btn2page = {b[0]: b[2] for b in BTNS}

    L = []
    L.append("-- ===========================================================")
    L.append("-- 角色菜单授权：三级结构下，页面可见需要其所有祖先（顶级域 + 二级分组）都被授权，")
    L.append("-- 否则菜单树挂不上、页面点不到。")
    L.append("-- 注：sys_role_menu.id 无 AUTO_INCREMENT（主键由 entity 生成，此处为纯 SQL 种子），")
    L.append("--     故 id 必须显式指定，取 1000+ 号段避开业务表 id。")
    L.append("-- ===========================================================")
    L.append("")
    all_ids = set()
    seq = 1000
    for rid, cfg in ROLE_PAGES.items():
        ids = set()
        for pid in cfg["pages"]:
            ids.add(pid)
            gid = page2group.get(pid)
            if gid:
                ids.add(gid)
        for bid in cfg["btns"]:
            ids.add(bid)
            pid = btn2page.get(bid)
            if pid:
                ids.add(pid)
        # 顶级域按「页面实际归属」推导（审核员也有经办侧的审核类页面，
        # 不能只给一个顶级，否则该顶级下的分组挂不上）
        for gid in list(ids):
            t = group2top.get(gid)
            if t:
                ids.add(t)
        all_ids |= ids
        L.append(f"-- 角色 {rid}")
        rows = []
        for i in sorted(ids):
            seq += 1
            rows.append(f"({seq}, {rid}, {i})")
        L.append("INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id) VALUES")
        L.append(", ".join(rows) + ";")
        L.append("")
    return "\n".join(L)


def main():
    src = open(DATA, encoding="utf-8").read()

    # 1) 删除全部旧的 sys_menu INSERT 段（一次清空，避免分段替换导致内容残留/重复）
    menu_pat = re.compile(r"INSERT IGNORE INTO sys_menu \([^)]*\) VALUES.*?;", re.S)
    n_menu = len(menu_pat.findall(src))
    src = menu_pat.sub("", src)

    # 2) 删除全部旧的 sys_role_menu INSERT 段
    role_pat = re.compile(r"INSERT IGNORE INTO sys_role_menu \([^)]*\) VALUES.*?;", re.S)
    n_role = len(role_pat.findall(src))
    src = role_pat.sub("", src)

    # 3) 在"角色菜单授权"注释处插入新菜单段与新授权段
    menu_block = build_menu_block()
    role_block = build_role_block()

    anchor = "-- 角色菜单授权"
    if anchor in src:
        src = src.replace(anchor, menu_block + "\n" + role_block + "\n-- 角色菜单授权（历史段落，见上）", 1)
    else:
        # 没有锚点就追加到文件末尾菜单区之前
        src = src.rstrip() + "\n\n" + menu_block + "\n" + role_block + "\n"

    # 4) 清理连续空行（最多 3 行）
    src = re.sub(r"\n{4,}", "\n\n\n", src)

    open(DATA, "w", encoding="utf-8").write(src)
    print(f"data.sql 菜单段已重写为三级结构（清理旧段：菜单 {n_menu} / 授权 {n_role}）")
    print(f"  顶级 {len(TOPS)} / 二级 {len(GROUPS)} / 页面 {len(PAGES)} / 按钮 {len(BTNS)}")


if __name__ == "__main__":
    main()
