# 医保结算经办与基金监管模拟平台（CSMIS）

独立的**医保局端模拟平台**：把「医院侧报盘 → 平台即时结算 → 日对账 → 月度申报 → 智能审核扣款 → 预留质保金
→ 月度拨付 → 年终清算」以及「规则配置 → 疑点稽核 → 飞检 → 违规追回」两条链路做成一套可登录、可操作、
有库有数据的完整系统。

与主 HIS 系统（兄弟仓库 [`../cslk`](file:///E:/work_source/his_cslk/cslk)）**完全隔离**：独立仓库、独立数据库
（`mss_insurance`）、独立端口（后端 8090 / 前端 3100），HIS 只通过 HTTP 报盘报文对接本平台。

## 目录结构

```
csmis/
├── source/
│   ├── back_end_mis/          # Spring Boot 后端（com.mis，端口 8090，context-path /mss）
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/mis/  # controller / service / mapper / entity / dto / vo / engine / config
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/        # schema.sql（建表）+ data.sql（基础种子数据）
│   └── front_mis/             # Vue3 经办前端（端口 3100）
│       └── src/               # api / router / stores / layout / components / lib / views
├── docs/                      # 预留
├── shot.bat                   # Edge headless 截图脚本（UI 验收兜底通道）
└── login-1440.png / dashboard-1440.png
```

## 技术栈

| 层  | 选型                                                                                                          |
|----|-------------------------------------------------------------------------------------------------------------|
| 后端 | Spring Boot 3.2.5 / Java 21 / Maven，MyBatis-Plus 3.5.6，MySQL 8，Knife4j 4.4.0（OpenAPI3），jjwt 0.12.5 + BCrypt |
| 前端 | Vue 3.5 + Vite 8.2 + Element Plus 2.14 + Tailwind CSS 4.3 + Pinia 4 + ECharts 6.1 + axios                   |
| 分层 | controller / service(+impl) / mapper / entity / dto / vo，统一返回 `{code, message, data}`，全局异常处理                |

## 快速开始

### 1. 后端

```bash
cd source/back_end_mis
mvn spring-boot:run          # 或 mvn -DskipTests package && java -jar target/mis-backend.jar
```

* Base URL：`http://localhost:8090/mss`，接口文档：`http://localhost:8090/mss/doc.html`
* **首次启动自动建库建表**：连接串带 `createDatabaseIfNotExist=true`，`spring.sql.init.mode=always`
  每次启动都执行 `schema.sql`（全 `CREATE TABLE IF NOT EXISTS`）+ `data.sql`（全 `INSERT IGNORE`），可重复启动。
* 默认连 `192.168.88.132:3306`，账号 `xz_feng`。口令等真值**不入库**，用环境变量覆盖：
  `MIS_DB_URL` / `MIS_DB_USERNAME` / `MIS_DB_PASSWORD` / `JWT_SECRET`
  （本地真值放 `application-local.yml`，已在 `.gitignore` 内）。
* 三个幂等初始化器按「表为空才播种」工作，业务日期以当天为基准生成，保证工作台/报表打开就有数据：
    * `AuthDataInitializer`（`mss_user` 空）→ 3 个登录账号
    * `DataInitializer`（`mss_catalog` 空，固定随机种子 20261013）→ 三大目录、院内对照、结算单与费用明细、待结算业务、审核、备案、对账期
    * `BizSeedInitializer`（逐表判空）→ 参保人扩到 30、2304 结算与报文台账、月度申报/审核/拨付、监管疑点/飞检/追回

### 2. 前端

```bash
cd source/front_mis
npm install        # 仓库同时存在 pnpm-lock.yaml，以 package-lock.json 为准
npm run dev        # http://localhost:3100
```

vite 把 `/mss` 代理到 `http://localhost:8090`（无 rewrite，与后端 context-path 一致），全部走真实接口，无 mock。

### 3. 登录账号

| 账号      | 口令         | 角色                 | 用途          |
|---------|------------|--------------------|-------------|
| `admin` | `admin123` | 系统管理员（`MSS_ADMIN`） | 全量菜单 + 系统管理 |
| `yb001` | `123456`   | 结算经办员              | 经办窗口        |
| `yb002` | `123456`   | 审核稽核员              | 费用审核 / 基金监管 |

> 登录会取员工的岗位（`mss_employee_post`，优先 `is_primary=1`，没有主岗位则取第一条）来确定角色与科室，
> 账号没配岗位 / 角色不可用 / 账号停用或锁定都会登录失败。口令由 `AuthDataInitializer` 运行时 BCrypt 生成，
> 不在 SQL 里。

## 功能模块

菜单由后端下发（`GET /auth/menus`，表 `mss_menu`），前端 22 条路由：

* **经办工作台** — 今日 KPI、待办、最近结算单、按险种/科室统计与近 10 日趋势
* **医保结算** — 门诊医保结算、住院医保结算（待结算列表 → 费用试算 → 确认结算）、结算单管理（明细 / 退费冲正）、
  机构报盘接收（2304/2305 台账）、月度结算管理（申报 → 审核 → 拨付 → 年终清算）
* **参保管理** — 参保人分页与新增/同步（按身份证幂等）
* **费用审核** — 待审核费用分状态页签，通过 / 驳回
* **三大目录与对照** — 医保目录分页与统计、院内项目对照（人工 map/unmap + 一键自动对照）
* **异地就医备案** — 备案增改与撤销
* **清算对账** — 月度账期对账平账
* **统计报表** — ECharts 图表
* **基金监管** — 监管工作台、审核规则配置、疑点稽核（跑规则 / 确认 / 排除）、飞检任务（启动 / 完成 / 转追回）、违规追回（追回 /
  核销）
* **系统管理** — 用户、角色（含菜单授权）、菜单、岗位、字典、科室、员工（含资质）

## 鉴权与权限

`AuthFilter`（`OncePerRequestFilter`，未引入 Spring Security）承担认证：

* 白名单（前缀匹配）：`/auth/login`、`/gateway/`、`/insure/person/query`、Knife4j/Swagger 资源、`/error`
* 其余请求必须带 `Authorization: Bearer <token>`，否则 **HTTP 401** + `Result.error(401,"未登录或登录已过期")`
* token 为 HS256，claims 含 `userId/username/roleCode/deptId/deptName`（id 一律字符串，避免前端精度丢失），
  有效期 24h（`jwt.expiration`）

**授权边界（需要知道的风险）**：过滤器只做认证，**不做接口级授权**。角色 → 菜单/权限码
（`/auth/info` 的 `permissions`）目前只在前端用 `v-perm` 指令控制按钮显隐，后端没有 `@PreAuthorize`
或拦截器校验；`/gateway/` 与 `/insure/person/query` 全开放且不校验 `jinb.medins.app-secret`。
即本平台按「内网演示环境」定位，不能直接暴露公网。

前端 401（业务码或 HTTP 码）会清 storage 并跳 `/login`；业务码非 200 统一 `ElMessage.error`，
可按请求 `silent` 关闭。token 存 `localStorage.token`。

## 数据模型

30 张表，全部 `InnoDB` / `utf8mb4`，前缀统一 `mss_`：

| 域         | 表                                                                                                                                                                              |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 认证与系统（11） | `mss_user`、`mss_role`、`mss_menu`、`mss_role_menu`、`mss_department`、`mss_employee`、`mss_employee_post`、`mss_employee_qualification`、`mss_dict_type`、`mss_dict_data`、`mss_config` |
| 报盘与月度（6）  | `mss_insured_person`、`mss_settlement`、`mss_message`、`mss_monthly`、`mss_monthly_deduct`、`mss_allocation`                                                                        |
| 经办平台（8）   | `mss_catalog`、`mss_item_mapping`、`mss_pending_settle`、`mss_bill`、`mss_fee_item`（`owner_type` 1=待结算 / 2=结算单）、`mss_fee_audit`、`mss_filing`、`mss_recon`                           |
| 基金监管（5）   | `mss_audit_rule`、`mss_suspicion`、`mss_inspection`、`mss_inspection_item`、`mss_recover`                                                                                          |

## 与 HIS 联调

HIS 侧防腐层 `InsuranceChannelService` 提供 mock / remote 两个实现，配置在
`cslk/source/back_end/his-web/src/main/resources/config/domain/his-insurance.yml`，当前为：

```yaml
his:
  insurance:
    enabled: true
    channel:
      mode: remote                                   # mock=不依赖外部；remote=调本平台
      base-url: http://localhost:8090/mss
```

本平台暴露给 HIS 的免登录接口：

| 方法   | 路径                                     | 说明                           |
|------|----------------------------------------|------------------------------|
| POST | `/gateway/2304`                        | 结算信息上传（body 为 HIS 报盘原文 JSON） |
| POST | `/gateway/2305`                        | 结算信息撤销（冲正）                   |
| GET  | `/gateway/dayBill?billDate=yyyy-MM-dd` | 拉取平台侧「对方账」做日对账               |
| POST | `/insure/person/query`                 | 参保查询（身份证 / 医保卡号 / 姓名）        |

* 回执口径：HTTP 恒为 200，业务成败看 `data.infcode`（`0` 成功 / `-1` 失败），与 HIS 原进程内 mock 前置机一致。
* 幂等：同一 `tradeNo` 重复上传回原回执；2305 对已撤销单幂等成功。
* 平台不可用时，HIS 侧 2304/2305 落失败回执（台账可见、可重试）不抛断业务；日对账直接报错，
  避免把「平台不可用」误判成「本地多出差笔」。

## 业务口径

政策参数集中在 `application.yml` 的 `jinb.*`（绑定到 `MisProperties`）：

* **2304 平台侧即时核定**（与院方申报值相互独立）：职工门诊起付 0 / 70%，住院起付 800 / 85%；
  居民门诊起付 0 / 55%，住院起付 1000 / 70%。
* **经办窗口试算**（`SettleEngine`，后端算、前端只展示）：按明细 `payClass` 分解——丙类全额自费，
  甲类不计先行自付，乙类按明细自带的 `selfRatio` 扣先行自付；再扣起付线（职工 300 / 居民 500 / 其他 800）
  按险种比例报销（0.85 / 0.70 / 0.60）；职工个账划扣 `min(个人负担 × 0.4, 1500)`。
* **智能审核**（演示用确定性规则）：结算单号末位命中 `3` 或 `7`（`refuse-tail-digits`）判违规，
  按 `refuse-ratio=1.00` 全额拒付该笔统筹；审核后按净审核金额 `reserve-ratio=0.05` 预留质保金，年终清算返还。
* **月度状态机**：`1 已申报 → 2 已审核 → 3 已拨付 → 4 已清算`，只能顺序推进，跳步或重复操作报错。
* **单号规则**：上传回执 `YBZ+yyyyMMdd+6位序列`；月度拨付 `YBB+yyyyMM+01`；质保金返还 `YBN+yyyyMM+01`。

内置测试参保人（`data.sql`）：`430102199001011234` 张三（职工）、`430102198505056789` 李四（居民）、
`430102197012121234` 王五（职工）、`430121200006069999` 赵六（居民）、`43010219660606001X` 孙七（职工，停保）。
未登记 / 停保仅告警不阻断，保证上传链路可跑通。

## 已知问题与命名说明

读代码时不要被三套前缀绕晕，它们是同一套系统：

* 工程与包名是 **mis**（目录 `back_end_mis`、`com.mis`、artifactId `mis-backend`、环境变量 `MIS_DB_*`）；
* 运行时标识仍是 **mss**（`spring.application.name: mss-back-end`、context-path `/mss`、全部表 `mss_*`）；
* 业务实体与配置前缀是历史名 **jinb**（`JinbSettlement` / `JinbMonthly` 等映射到 `mss_settlement` / `mss_monthly`，
  `MisProperties` 绑定 `jinb.*`）。`logging.level.com.mss` 指向的是已不存在的包，实际不生效。

其他待办：

* `source/back_end_mis/README.md` 内容已过期（仍写「不做任何登录鉴权」、旧目录名 `back_end_jinb` / `front_mss`），以本文件为准。
* 后端只有依赖声明、无 `src/test`；前端无 lint / test 配置。
* `front_mis` 同时存在 `package-lock.json` 与 `pnpm-lock.yaml`，需要清掉一个。

## UI 验收

浏览器不可用时用 Edge headless 兜底截图（默认截 `/login`，1440×900）：

```bash
shot.bat http://localhost:3100/dashboard out.png 1440,900
```

`login-*.png` / `dashboard-1440.png` 是历史验收截图。
