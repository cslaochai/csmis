const { chromium } = require('playwright')
const FRONT = process.argv[2], OUT = process.argv[3], WHICH = process.argv[4]
;(async () => {
  const b = await chromium.launch()
  const p = await b.newPage({ viewport: { width: 1440, height: 1000 } })
  const errs = []
  p.on('pageerror', e => errs.push(e.message))
  await p.goto(FRONT + '/login', { waitUntil: 'networkidle' })
  await p.fill('input[placeholder="用户名"]', 'admin')
  await p.fill('input[placeholder="密码"]', 'admin123')
  await p.click('.login-btn')
  await p.waitForTimeout(4000)

  // 逐层展开：找所有 .el-sub-menu__title，用真实鼠标点击（scrollIntoView 保证可见）
  for (let round = 0; round < 4; round++) {
    const titles = await p.$$('.app-sidebar .el-sub-menu__title')
    for (const t of titles) {
      const opened = await t.evaluate(el => el.parentElement.classList.contains('is-opened'))
      if (opened) continue
      const visible = await t.isVisible().catch(() => false)
      if (!visible) continue
      await t.scrollIntoViewIfNeeded().catch(()=>{})
      await t.click({ timeout: 3000 }).catch(()=>{})
      await p.waitForTimeout(180)
    }
    await p.waitForTimeout(400)
  }
  await p.waitForTimeout(500)
  const txt = await p.$eval('.app-sidebar', e => e.innerText)
  console.log('=== 全展开侧边栏 ===')
  console.log(txt)
  await p.screenshot({ path: OUT })
  console.log('\n截图:', OUT, errs.length ? ('错误:'+errs.join('|')) : '无错误')
  await b.close()
})().catch(e => { console.error('失败:', e.message); process.exit(1) })
