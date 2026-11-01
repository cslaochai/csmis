// 菜单图标校验：data.sql 里的 icon 必须存在于 @element-plus/icons-vue，
// 否则前端 <component :is="icon"> 渲染成空标签（图标不显示且不报错，最难发现）。
//
// 用法（需在 front_mis 目录下执行，或指定 --front 路径）：
//   node scripts/check-menu-icons.js [--front <front_mis 路径>]
const path = require('path')
const fs = require('fs')
const { createRequire } = require('module')

const args = process.argv.slice(2)
let frontDir = path.resolve(__dirname, '../source/front_mis')
const fi = args.indexOf('--front')
if (fi >= 0 && args[fi + 1]) frontDir = path.resolve(args[fi + 1])

const req = createRequire(path.join(frontDir, 'package.json'))
const lib = new Set(Object.keys(req('@element-plus/icons-vue')))

const sqlPath = path.join(frontDir, '..', 'mis_backend', 'src', 'main', 'resources', 'db', 'data.sql')
const sql = fs.readFileSync(sqlPath, 'utf-8')

// 提取 sys_menu 的 INSERT 行：列序 id,menu_name,parent_id,sort_order,menu_type,
// path,component,menu_key,icon,permission
const used = new Map()
for (const m of sql.matchAll(/INSERT IGNORE INTO sys_menu \([^)]*\) VALUES\s*(.*?);/gs)) {
  for (const row of m[1].matchAll(/\(([^()]*)\)/g)) {
    const p = row[1].split(',').map(s => s.trim().replace(/^'|'$/g, ''))
    const name = p[1], icon = p[8]
    if (!icon || icon === 'NULL') continue
    if (!used.has(icon)) used.set(icon, [])
    used.get(icon).push(name)
  }
}

const bad = [...used].filter(([icon]) => !lib.has(icon))
const dup = [...used].filter(([, users]) => users.length > 1)

console.log(`图标库 @element-plus/icons-vue 共 ${lib.size} 个图标`)
console.log(`data.sql 使用 ${used.size} 个不同图标\n`)

if (bad.length) {
  console.log(`❌ 无效图标 ${bad.length} 个（前端会渲染成空白）：`)
  for (const [icon, users] of bad) console.log(`   ${icon.padEnd(16)} → ${users.join(', ')}`)
} else {
  console.log('✅ 所有图标均存在于图标库')
}

if (dup.length) {
  console.log(`\n⚠️  重复使用 ${dup.length} 个（顶级/二级/三级重名不影响显示，但辨识度下降）：`)
  for (const [icon, users] of dup) console.log(`   ${icon.padEnd(16)} ${users.length} 处 → ${users.join(', ')}`)
}

process.exit(bad.length ? 1 : 0)
