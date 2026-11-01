# -*- coding: utf-8 -*-
"""
菜单三级化重构。

现状：顶级混着目录与页面（工作台、医保目录管理、清算对账…直接挂在 parent_id=0），
      基金监管 15 个子菜单挤在一层，经办侧没有统一归属。

目标：顶级只有三个域 —— 医保结算经办 / 医保基金监管 / 系统管理；
      二级为业务分组；三级为页面（menu_type=2）；按钮（menu_type=3）跟随其页面。

策略：
  1) 复用现有顶级 id：2 → 医保结算经办，13 → 医保基金监管，9 → 系统管理
  2) 新建二级分组，分配新 id 段 200+
  3) 现有页面菜单统一降到三级（parent_id 指向所属二级分组）
  4) 按钮菜单的 parent_id 跟随其所属页面（原来挂在顶级目录下）
  5) 同步 sys_role_menu：按"新父链上的祖先"补齐授权，保证菜单树挂得上
"""
import re
import sys

DB = "db"

# ---------------------------------------------------------------- 顶级
TOP = {
    2:   ("医保结算经办", "settle",     "settle",     "Settlement", 10),
    13:  ("医保基金监管", "supervise",  "supervise",  "Monitor",    20),
    9:   ("系统管理",     "system",     "system",     "Setting",    30),
}

# ---------------------------------------------------------------- 二级分组
# id -> (名称, 顶级 id, path, component, icon, 权限码)
GROUPS = {
    200: ("结算经办", 2, "settle.do", "group.settle.do", "CreditCard", "settle:do:list"),
    201: ("结算与清算", 2, "settle.fin", "group.settle.fin", "Money", "settle:fin:list"),
    202: ("参保与账户", 2, "settle.insured", "group.settle.insured", "User", "insured:person:list"),
    203: ("费用与目录", 2, "settle.catalog", "group.settle.catalog", "Notebook", "catalog:list"),
    204: ("待遇与备案", 2, "settle.filing", "group.settle.filing", "OfficeSupplies", "filing:list"),
    205: ("进销存", 2, "settle.stock", "group.settle.stock", "Boxes", "stock:item:list"),
    206: ("经办看板", 2, "settle.board", "group.settle.board", "DataBoard", "settle:board:view"),
    210: ("监管总览", 13, "supervise.overview", "group.supervise.overview", "DataBoard", "supervise:dashboard:view"),
    211: ("规则与知识", 13, "supervise.rule", "group.supervise.rule", "SetUp", "supervise:rule:list"),
    212: ("稽核核查", 13, "supervise.audit", "group.supervise.audit", "Search", "supervise:suspicion:list"),
    213: ("智能监管", 13, "supervise.ai", "group.supervise.ai", "Aim", "supervise:fraud:list"),
    214: ("专项监管", 13, "supervise.special", "group.supervise.special", "Histogram", "supervise:quality:list"),
    215: ("信用管理", 13, "supervise.credit", "group.supervise.credit", "Medal", "supervise:credit:list"),
    220: ("用户与权限", 9, "system.user", "group.system.user", "User", "system:user:list"),
    221: ("组织与岗位", 9, "system.org", "group.system.org", "OfficeBuilding", "system:dept:list"),
    222: ("基础配置", 9, "system.dict", "group.system.dict", "Collection", "system:dict:list"),
    223: ("审计与待办", 9, "system.audit", "group.system.audit", "Document", "system:operlog:list"),
}

# ---------------------------------------------------------------- 三级页面归位
# 页面菜单 id -> 所属二级分组 id
PAGE_TO_GROUP = {
    # --- 经办侧 ---
    1:   206,  # 工作台 → 经办看板
    21:  200,  # 门诊医保结算
    22:  200,  # 住院医保结算
    24:  200,  # 跨省异地结算
    23:  201,  # 结算单管理
    150: 201,  # 结算清单
    25:  201,  # 月度结算
    26:  201,  # 日拨付管理
    7:   201,  # 清算对账
    31:  202,  # 参保人管理
    32:  202,  # 转移接续/个人账户/慢特病
    4:   203,  # 费用审核
    5:   203,  # 医保目录管理
    151: 203,  # 目录分表管理
    10:  203,  # 定点协议管理
    11:  204,  # 手工报销
    6:   204,  # 跨省异地就医备案
    33:  205,  # 药品耗材进销存
    152: 205,  # 库存盘点
    8:   206,  # 统计报表 → 经办看板

    # --- 监管侧 ---
    131: 210,  # 监管工作台
    140: 210,  # 举报投诉与奖励
    132: 211,  # 规则配置
    136: 211,  # 知识库
    133: 212,  # 疑点稽核
    134: 212,  # 飞检任务
    135: 212,  # 违规追回
    139: 213,  # 事前提醒/事中拦截
    138: 213,  # 大数据反欺诈
    141: 213,  # DRG/DIP支付监管
    144: 214,  # 结算清单质控
    145: 214,  # 药品追溯码监管
    142: 214,  # 双随机一公开
    143: 214,  # 自查自纠
    137: 215,  # 信用管理

    # --- 系统侧 ---
    91:  220,  # 用户管理
    92:  220,  # 角色管理
    93:  220,  # 菜单管理
    94:  221,  # 岗位管理
    96:  221,  # 科室管理
    97:  221,  # 员工管理
    95:  222,  # 字典管理
    153: 223,  # 操作日志
    154: 223,  # 待办中心
}

# 按钮菜单 id -> 所属页面 id（按钮必须挂在页面下，否则三级树里点不到）
BTN_TO_PAGE = {
    101: 91, 102: 91, 103: 91,     # 用户 新增/编辑/删除
    104: 92, 105: 92,              # 角色 新增/删除
    106: 93, 107: 93,              # 菜单 新增/删除
    108: 95, 109: 95, 110: 95,     # 字典 新增/编辑/删除
    111: 97, 112: 97,              # 员工 新增/删除
}

# 需要废弃的旧顶级目录（其下菜单已全部下沉）
DROP_TOPS = [3]  # 参保管理


def emit():
    """生成 SQL 脚本"""
    out = []
    out.append("-- ===========================================================")
    out.append("-- 菜单三级化重构：顶级三域 / 二级分组 / 三级页面")
    out.append("-- 顶级：2 医保结算经办 · 13 医保基金监管 · 9 系统管理")
    out.append("-- 二级分组：200-206 / 210-215 / 220-223")
    out.append("-- 三级页面沿用原 id，按 PAGE_TO_GROUP 归位")
    out.append("-- ===========================================================")
    out.append("")

    # 1. 顶级目录改名 + 换 path/menu_key/icon/sort
    out.append("-- 1) 顶级：收敛为三个业务域")
    for mid, (name, path, key, icon, sort) in TOP.items():
        out.append(
            f"UPDATE sys_menu SET menu_name='{name}', parent_id=0, sort_order={sort}, menu_type=1, "
            f"path='{path}', component=NULL, menu_key='{key}', icon='{icon}', permission=NULL "
            f"WHERE id={mid};")
    out.append("")

    # 2. 新建二级分组
    out.append("-- 2) 二级：业务分组目录（menu_type=1）")
    for gid, (name, top, path, key, icon, perm) in GROUPS.items():
        out.append(
            f"INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, "
            f"menu_key, icon, permission, is_visible, status) VALUES "
            f"({gid}, '{name}', {top}, {gid % 100}, 1, '{path}', NULL, '{key}', '{icon}', '{perm}', 1, 1) "
            f"ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), "
            f"sort_order=VALUES(sort_order), menu_type=VALUES(menu_type), path=VALUES(path), "
            f"menu_key=VALUES(menu_key), icon=VALUES(icon), permission=VALUES(permission);")
    out.append("")

    # 3. 三级页面归位
    out.append("-- 3) 三级：页面下沉到所属业务分组（menu_type=2）")
    for pid, gid in sorted(PAGE_TO_GROUP.items()):
        out.append(f"UPDATE sys_menu SET parent_id={gid}, menu_type=2 WHERE id={pid};")
    out.append("")

    # 4. 按钮跟随页面
    out.append("-- 4) 按钮权限：挂到所属页面下（menu_type=3）")
    for bid, pid in sorted(BTN_TO_PAGE.items()):
        out.append(f"UPDATE sys_menu SET parent_id={pid}, menu_type=3 WHERE id={bid};")
    out.append("")

    # 5. 废弃旧顶级目录
    out.append("-- 5) 废弃：旧顶级目录（子菜单已下沉）")
    for did in DROP_TOPS:
        out.append(f"UPDATE sys_menu SET status=0, is_visible=0 WHERE id={did};")
    out.append("")

    # 6. 角色授权：所有页面按钮保留原授权；新二级分组按顶级授权自动补齐
    out.append("-- 6) 角色授权：新二级分组按顶级域的授权补齐（否则菜单树挂不上）")
    # 顶级 -> 需要授权的角色（继承原顶级菜单的授权集合）
    out.append("""
-- 二级分组与顶级的授权映射：给顶级授了权的角色，其下二级分组一并授权
INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM sys_role_menu), rm.role_id, g.gid
FROM sys_role_menu rm
JOIN (SELECT 200 gid, 2 top UNION ALL SELECT 201,2 UNION ALL SELECT 202,2
      UNION ALL SELECT 203,2 UNION ALL SELECT 204,2 UNION ALL SELECT 205,2 UNION ALL SELECT 206,2
      UNION ALL SELECT 210,13 UNION ALL SELECT 211,13 UNION ALL SELECT 212,13
      UNION ALL SELECT 213,13 UNION ALL SELECT 214,13 UNION ALL SELECT 215,13
      UNION ALL SELECT 220,9 UNION ALL SELECT 221,9 UNION ALL SELECT 222,9 UNION ALL SELECT 223,9) g
  ON rm.menu_id = g.top
WHERE NOT EXISTS (SELECT 1 FROM (SELECT role_id, menu_id FROM sys_role_menu) x
                  WHERE x.role_id = rm.role_id AND x.menu_id = g.gid);
""".strip())
    out.append("")
    return "\n".join(out)


if __name__ == "__main__":
    sql = emit()
    path = sys.argv[1] if len(sys.argv) > 1 else "menu_restructure.sql"
    with open(path, "w", encoding="utf-8") as f:
        f.write(sql + "\n")
    print(f"已生成 {path}")
    print(f"顶级 {len(TOP)} 个 / 二级分组 {len(GROUPS)} 个 / 三级页面 {len(PAGE_TO_GROUP)} 个 / 按钮 {len(BTN_TO_PAGE)} 个")
