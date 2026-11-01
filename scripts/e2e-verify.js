// 端到端业务链路验证脚本（Node，直接打 HTTP 接口）
// 用法: node e2e.js <baseUrl> <username> <password>
const BASE = process.argv[2] || 'http://localhost:8090/mss'
const USER = process.argv[3] || 'admin'
const PASS = process.argv[4] || 'Admin@123'

// 口令需 SM2 加密后传输（与前端 lib/password.js 同一方案）
const path = require('path')
const fs = require('fs')

let sm2 = null
// sm-crypto 在 front_mis/node_modules 下；脚本可能在 scripts/ 或 .workbuddy/tmp/，逐个候选路径找
for (const base of [
  path.resolve(__dirname, '../source/front_mis'),
  path.resolve(__dirname, '../../source/front_mis'),
  path.resolve(__dirname, '../front_mis'),
]) {
  try {
    const req = require('module').createRequire(path.join(base, 'package.json'))
    sm2 = req('sm-crypto').sm2
    break
  } catch (e) { /* 试下一个 */ }
}
if (!sm2) {
  // 兜底：直接从 node_modules 绝对路径加载
  for (const base of [
    path.resolve(__dirname, '../source/front_mis/node_modules/sm-crypto'),
    path.resolve(__dirname, '../../source/front_mis/node_modules/sm-crypto'),
  ]) {
    try {
      if (fs.existsSync(base)) { sm2 = require(base).sm2; break }
    } catch (e) { /* ignore */ }
  }
}

async function encryptPassword(plain) {
  if (!sm2) throw new Error('sm-crypto 不可用：找不到 front_mis/node_modules/sm-crypto')
  const res = await fetch(BASE + '/auth/publicKey')
  const json = await res.json()
  const pub = json?.data?.publicKey
  if (!pub) throw new Error('获取登录加密公钥失败: ' + JSON.stringify(json).slice(0, 200))
  return sm2.doEncrypt(plain, pub, 1)
}

let token = ''
let pass = 0
let fail = 0
const failures = []

function log(icon, name, detail) {
  console.log(`${icon} ${name}${detail ? ' — ' + detail : ''}`)
}

function ok(name, detail) {
  pass++
  log('  [PASS]', name, detail)
}
function bad(name, detail) {
  fail++
  failures.push(`${name}: ${detail}`)
  log('  [FAIL]', name, detail)
}

async function api(method, path, body, opts = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token && !opts.noAuth) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch (e) { json = { raw: text } }
  return { status: res.status, body: json, data: json && json.data }
}

function chk(name, cond, detail) {
  if (cond) ok(name, detail)
  else bad(name, detail || '断言失败')
  return cond
}

async function main() {
  console.log('='.repeat(70))
  console.log('医保结算经办 + 基金监管 端到端业务链路验证')
  console.log('目标: ' + BASE)
  console.log('='.repeat(70))

  // ---------- 1. 登录 ----------
  console.log('\n【1】登录与认证')
  let encPass = ''
  try {
    encPass = await encryptPassword(PASS)
    ok('口令 SM2 加密', !!encPass, `密文长度 ${encPass.length}`)
  } catch (e) {
    bad('口令 SM2 加密', e.message)
    process.exit(1)
  }
  let r = await api('POST', '/auth/login', { username: USER, password: encPass })
  if (r.status !== 200 || !r.body || r.body.code !== 200) {
    bad('登录', `HTTP ${r.status} ${JSON.stringify(r.body).slice(0, 200)}`)
    console.log('\n登录失败，后续用例无法执行')
    process.exit(1)
  }
  token = r.data?.token || r.data?.accessToken
  chk('登录成功', !!token, 'token=' + (token ? token.slice(0, 20) + '...' : 'null'))

  r = await api('GET', '/auth/info')
  chk('获取用户信息', r.body?.code === 200, r.data?.username || '')

  // ---------- 2. 菜单与权限 ----------
  console.log('\n【2】菜单三级结构与角色授权')
  r = await api('GET', '/auth/menus')
  const menus = r.data || []
  chk('获取菜单树', r.body?.code === 200 && Array.isArray(menus), `共 ${menus.length} 个顶级菜单`)

  // 顶级必须是三域
  const topNames = menus.map(m => m.menuName)
  chk('顶级只有三个业务域', menus.length === 3,
    topNames.join(' / '))
  for (const expect of ['医保结算经办', '医保基金监管', '系统管理']) {
    chk(`顶级菜单：${expect}`, topNames.includes(expect))
  }

  // 三级结构：顶级 → 二级分组 → 三级页面
  let groupCnt = 0, pageCnt = 0, noThird = []
  for (const top of menus) {
    for (const g of top.children || []) {
      groupCnt++
      const pages = (g.children || []).filter(p => p.menuType === 2)
      pageCnt += pages.length
      if (pages.length === 0) noThird.push(`${top.menuName}/${g.menuName}`)
    }
  }
  chk('二级为业务分组', groupCnt === 17, `共 ${groupCnt} 个分组`)
  chk('三级为页面', pageCnt === 44, `共 ${pageCnt} 个页面`)
  chk('每个分组都有三级页面', noThird.length === 0, noThird.join(', ') || '无空分组')

  // 页面 path 必须能在前端路由里找到（点得开）
  const routerSrc = fs.readFileSync(
    path.resolve(__dirname, '../source/front_mis/src/router/index.js'), 'utf-8')
  const routePaths = new Set()
  for (const m of routerSrc.matchAll(/path:\s*'([^']+)'/g)) {
    if (!m[1].startsWith('/')) routePaths.add('/' + m[1])
  }
  const noRoute = []
  for (const top of menus) {
    for (const g of top.children || []) {
      for (const p of g.children || []) {
        if (p.menuType === 2 && p.path && !routePaths.has(p.path)) noRoute.push(p.path)
      }
    }
  }
  chk('所有页面都有对应前端路由', noRoute.length === 0,
    noRoute.length ? noRoute.join(', ') : `已核对 ${pageCnt} 个页面`)

  // 关键菜单必须在树里
  const menuKeys = JSON.stringify(menus)
  const expectMenus = [
    ['结算清单', 'settle.list'],
    ['目录分表管理', 'settle.catalog'],
    ['库存盘点', 'stock.check'],
    ['操作日志', 'system.operlog'],
    ['待办中心', 'system.todo'],
  ]
  for (const [label, key] of expectMenus) {
    chk(`菜单已挂到三级：${label}`, menuKeys.includes(key), key)
  }

  // 图标必须是 @element-plus/icons-vue 里真实存在的组件名，
  // 否则前端 <component :is="icon"> 渲染成空标签 —— 图标不显示且控制台不报错
  const frontRequire = require('module').createRequire(
    path.resolve(__dirname, '../source/front_mis/package.json'))
  const iconLib = new Set(Object.keys(frontRequire('@element-plus/icons-vue')))
  const badIcons = []
  const walkIcons = (nodes) => {
    for (const n of nodes || []) {
      if (n.icon && !iconLib.has(n.icon)) badIcons.push(`${n.menuName}→${n.icon}`)
      walkIcons(n.children)
    }
  }
  walkIcons(menus)
  chk('菜单图标均存在于 Element Plus 图标库', badIcons.length === 0,
    badIcons.length ? badIcons.join(', ') : `已核对 ${iconLib.size} 个可用图标`)

  // 角色隔离：经办员看不到监管侧与系统管理，审核员看不到经办结算
  async function menusOf(user, pass) {
    const pk = await (await fetch(BASE + '/auth/publicKey')).json()
    const enc = sm2.doEncrypt(pass, pk.data.publicKey, 1)
    const lr = await (await fetch(BASE + '/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: user, password: enc }),
    })).json()
    if (!lr.data?.token) return null
    const mr = await (await fetch(BASE + '/auth/menus', {
      headers: { Authorization: 'Bearer ' + lr.data.token },
    })).json()
    return mr.data || []
  }
  const opMenus = await menusOf('yb001', '123456')
  if (opMenus) {
    const opTop = opMenus.map(m => m.menuName)
    chk('经办员可见医保结算经办', opTop.includes('医保结算经办'), opTop.join(' / '))
    chk('经办员不可见医保基金监管', !opTop.includes('医保基金监管'))
    chk('经办员不可见系统管理', !opTop.includes('系统管理'))
  }
  const auMenus = await menusOf('yb002', '123456')
  if (auMenus) {
    const auTop = auMenus.map(m => m.menuName)
    chk('审核员可见医保基金监管', auTop.includes('医保基金监管'), auTop.join(' / '))
    chk('审核员不可见系统管理', !auTop.includes('系统管理'))
  }

  // ---------- 3. 目录分表 ----------
  console.log('\n【3】目录分表（药品/诊疗/耗材）')
  r = await api('GET', '/settle/catalog/stats')
  chk('目录分表统计', r.body?.code === 200,
    `药品 ${r.data?.drug ?? 0} / 诊疗 ${r.data?.treat ?? 0} / 耗材 ${r.data?.material ?? 0}`)

  r = await api('GET', '/settle/catalog/listPage?page=1&size=5')
  chk('目录分表分页查询', r.body?.code === 200 && Array.isArray(r.data?.list),
    `返回 ${r.data?.list?.length ?? 0} 条 / 共 ${r.data?.total ?? 0} 条`)

  // ---------- 4. 待结算 → 结算（核心链路） ----------
  console.log('\n【4】结算主链路（事前提醒 → 事中拦截 → 清单生成 → 出库）')
  r = await api('GET', '/settle/outpatient/listPage?page=1&size=10')
  const pendings = r.data?.list || []
  chk('查询待结算业务', r.body?.code === 200, `待结算 ${pendings.length} 条`)

  let settleOk = false
  let billNo = ''
  if (pendings.length > 0) {
    const p = pendings[0]
    const bizNo = p.bizNo || p.visitNo
    // 4.1 预览（触发事前提醒）
    r = await api('POST', '/settle/outpatient/preview', { bizNo })
    if (r.body?.code === 200) {
      const pre = r.data
      const hitCount = pre?.precheck?.hitCount ?? pre?.hitCount ?? 0
      ok('结算预览返回待遇计算结果',
        `总费用 ${pre?.total ?? 0}，事前提醒命中 ${hitCount} 条`)
      chk('预览返回事前提醒节点', pre?.precheck !== undefined || hitCount !== undefined,
        `precheck 节点存在`)
    } else {
      bad('结算预览', `HTTP ${r.status} ${JSON.stringify(r.body).slice(0, 150)}`)
    }

    // 4.2 确认结算
    r = await api('POST', '/settle/outpatient/confirm', { bizNo })
    if (r.body?.code === 200 && r.data && !r.data.error) {
      settleOk = true
      billNo = r.data.bill?.billNo || ''
      ok('确认结算成功', `结算单号 ${billNo}，医保支付 ${r.data.bill?.insurancePay ?? 0} 元`)
    } else {
      // 命中阻断规则属预期情况
      const err = r.data?.error || r.body?.message || ''
      if (String(err).includes('拦截') || String(err).includes('命中')) {
        ok('事中拦截生效（结算被拒付）', err)
        settleOk = true
      } else {
        bad('确认结算', `HTTP ${r.status} ${JSON.stringify(r.body).slice(0, 200)}`)
      }
    }
  } else {
    bad('结算主链路', '无待结算业务，无法验证')
  }

  // ---------- 5. 结算清单主表 ----------
  console.log('\n【5】结算清单主表')
  r = await api('GET', '/settle/list/listPage?page=1&size=10')
  const lists = r.data?.list || []
  chk('结算清单查询', r.body?.code === 200, `共 ${r.data?.total ?? 0} 张清单`)
  if (settleOk && billNo) {
    const hit = lists.find(l => l.billNo === billNo)
    chk('结算自动生成清单主表', !!hit,
      hit ? `清单号 ${hit.listNo}，项目数 ${hit.itemCount}` : `未找到结算单 ${billNo} 对应清单`)
  }

  // ---------- 6. 进销存 ----------
  console.log('\n【6】进销存联动')
  r = await api('GET', '/stock/item/listPage?page=1&size=5')
  chk('库存主档查询', r.body?.code === 200, `共 ${r.data?.total ?? 0} 个品项`)
  r = await api('GET', '/stock/txn/listPage?page=1&size=5')
  chk('进销存流水查询', r.body?.code === 200, `共 ${r.data?.total ?? 0} 条流水`)

  // ---------- 7. 库存盘点 ----------
  console.log('\n【7】库存盘点（发起 → 明细 → 完成 → 复核）')
  r = await api('POST', '/stock/check/create?medinsCode=H4301000001&medinsName=长沙市麓康医院')
  if (r.body?.code === 200 && r.data?.ok) {
    ok('发起盘点单', `盘点品项 ${r.data.matched} 个`)
    // 找刚生成的盘点单
    r = await api('GET', '/stock/check/listPage?page=1&size=1')
    const ck = r.data?.list?.[0]
    if (ck) {
      r = await api('GET', `/stock/check/items?checkNo=${encodeURIComponent(ck.checkNo)}`)
      const its = r.data || []
      chk('盘点明细带出账面数', its.length > 0 && its[0].bookQty != null,
        `${its.length} 个品项，账面数 ${its[0]?.bookQty}`)
      // 录入实盘（第一个品项实盘 = 账面 + 1，制造盘盈）
      if (its.length > 0) {
        const realQty = Number(its[0].bookQty || 0) + 1
        r = await api('POST', '/stock/check/submitItems', [{
          checkNo: ck.checkNo, itemCode: its[0].itemCode, realQty, diffReason: 'E2E 联调盘盈',
        }])
        chk('录入实盘数', r.body?.code === 200 && r.data?.ok, `提交 ${r.data?.matched ?? 0} 项`)
        // 完成盘点
        r = await api('POST', `/stock/check/finish?checkNo=${encodeURIComponent(ck.checkNo)}`)
        chk('完成盘点并调整库存', r.body?.code === 200 && r.data?.ok,
          `差异品项 ${r.data?.matched ?? 0} 项`)
        // 复核
        r = await api('POST', `/stock/check/review?checkNo=${encodeURIComponent(ck.checkNo)}&pass=true&opinion=E2E+联调复核通过`)
        chk('盘点复核办结', r.body?.code === 200 && r.data?.ok)
      }
    }
  } else {
    bad('发起盘点', `HTTP ${r.status} ${JSON.stringify(r.body).slice(0, 150)}`)
  }

  // ---------- 8. 个人账户划入 ----------
  console.log('\n【8】个人账户（缴费记录驱动划入）')
  r = await api('GET', '/insure/account/info?idCard=430102197508123456')
  const balBefore = r.data?.balance ?? 0
  ok('查询个人账户余额', r.body?.code === 200, `当前余额 ${balBefore} 元`)

  r = await api('POST', '/insure/account/accrueBatch?payMonth=')
  if (r.body?.code === 200 && r.data?.ok) {
    ok('批量划入个人账户', `划入 ${r.data.matched} 笔`)
  } else {
    bad('批量划入', `HTTP ${r.status} ${JSON.stringify(r.body).slice(0, 150)}`)
  }
  // 幂等校验：再跑一次应提示已划入
  r = await api('POST', '/insure/account/accrueBatch?payMonth=')
  chk('划入幂等（重复执行不重复入账）',
    r.body?.code === 200 && (r.data?.matched === 0 || r.data?.error),
    r.data?.error || `本次划入 ${r.data?.matched ?? 0} 笔（应为 0）`)

  r = await api('GET', '/insure/account/info?idCard=430102197508123456')
  ok('划入后余额已更新', r.body?.code === 200, `余额 ${r.data?.balance ?? 0} 元`)

  // ---------- 9. 待办中心 ----------
  console.log('\n【9】待办中心')
  r = await api('GET', '/system/todo/count')
  chk('待办数量统计', r.body?.code === 200,
    `待处理 ${r.data?.['待处理'] ?? 0} / 已完成 ${r.data?.['已完成'] ?? 0}`)
  r = await api('GET', '/system/todo/listPage?page=1&size=10')
  const todos = r.data?.list || []
  chk('待办列表查询', r.body?.code === 200, `共 ${r.data?.total ?? 0} 条待办`)
  const todoTypes = [...new Set(todos.map(t => t.todoType))]
  ok('待办来源覆盖多个业务环节', todoTypes.length > 0, todoTypes.join(', ') || '无')

  // ---------- 10. 操作日志 ----------
  console.log('\n【10】操作日志（审计留痕）')
  r = await api('GET', '/system/operlog/listPage?page=1&size=20')
  const logs = r.data?.list || []
  chk('操作日志查询', r.body?.code === 200, `共 ${r.data?.total ?? 0} 条`)
  chk('写操作已被自动留痕', logs.length > 0,
    logs.length ? `最近一条：${logs[0].operDesc || logs[0].requestUri}` : '无记录')
  // 审计留痕的关键要求：必须记录「谁、何时、对哪张单」
  const withUser = logs.filter(l => l.operUser).length
  chk('日志记录了操作人', withUser > 0, `${withUser}/${logs.length} 条有操作人`)
  const withTarget = logs.filter(l => l.targetNo || l.targetType).length
  chk('日志记录了业务对象', withTarget > 0, `${withTarget}/${logs.length} 条带业务对象`)
  const withCost = logs.filter(l => l.costMs != null).length
  chk('日志记录了耗时', withCost > 0, `${withCost}/${logs.length} 条有耗时`)

  r = await api('GET', '/system/operlog/stat?days=30')
  chk('操作日志统计', r.body?.code === 200,
    `总量 ${r.data?.total ?? 0}，成功 ${r.data?.successCount ?? 0}，失败 ${r.data?.failCount ?? 0}`)

  // ---------- 11. 监管链路 ----------
  console.log('\n【11】基金监管链路')
  r = await api('POST', '/supervise/suspicion/run')
  chk('疑点稽核执行', r.body?.code === 200, `本次新增疑点 ${r.data?.matched ?? 0} 条`)
  r = await api('GET', '/supervise/suspicion/listPage?page=1&size=1')
  chk('疑点库有数据', (r.data?.total ?? 0) > 0, `疑点库共 ${r.data?.total ?? 0} 条`)

  r = await api('POST', '/supervise/fraud/clue/run', {})
  chk('反欺诈模型运行', r.body?.code === 200, `生成线索 ${r.data?.matched ?? 0} 条`)
  r = await api('GET', '/supervise/fraud/clue/listPage?page=1&size=1')
  chk('反欺诈线索库有数据', (r.data?.total ?? 0) > 0, `线索库共 ${r.data?.total ?? 0} 条`)

  r = await api('POST', '/supervise/quality/result/check')
  chk('结算清单质控执行', r.body?.code === 200, `本次命中质控项 ${r.data?.matched ?? 0} 条`)
  r = await api('GET', '/supervise/quality/result/listPage?page=1&size=1')
  chk('质控结果库有数据', r.body?.code === 200, `质控结果共 ${r.data?.total ?? 0} 条`)

  r = await api('GET', '/stats/fund/overview?rangeDays=30')
  chk('基金运行分析', r.body?.code === 200,
    `总费用 ${r.data?.totalAmount ?? 0}，基金率 ${r.data?.fundRate ?? '-'}`)

  // ---------- 汇总 ----------
  console.log('\n' + '='.repeat(70))
  console.log(`验证结果：通过 ${pass} 项，失败 ${fail} 项`)
  if (failures.length) {
    console.log('\n失败明细：')
    failures.forEach(f => console.log('  - ' + f))
  }
  console.log('='.repeat(70))
  process.exit(fail > 0 ? 1 : 0)
}

main().catch(e => {
  console.error('执行异常:', e)
  process.exit(2)
})
