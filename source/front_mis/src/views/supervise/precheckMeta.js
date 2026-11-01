// 事前提醒 / 事中审核拦截 元数据（与后端口径一致）
export const TRIGGER_STAGES = { before_settle: '事前提醒', in_settle: '事中拦截' }
export const RISK_LEVELS = { 1: '低', 2: '中', 3: '高' }

/** 可判定的规则类别（决定引擎走哪套判定逻辑） */
export const RULE_CATEGORIES = {
  参保异常: '参保人未登记/已停保',
  冒名: '结算身份与参保库登记不一致',
  超限价: '项目单价高于医保限价',
  重复收费: '同一结算单同项目重复计费',
  超量开药: '同一项目开量超过阈值',
  目录未对照: '项目未做医保目录对照',
  自付占比过高: '个人自付占比超阈值',
  重复结算: '同一业务单号重复结算',
  高频就医: '窗口期内结算次数超阈值',
  分解住院: '窗口期内多次住院',
  同日住院门诊: '同日既有住院又有门诊',
  空刷医保卡: '有项目但费用几乎为零',
  丙类入统筹: '自费项目仍发生统筹支付',
}

export function stageText(s) { return TRIGGER_STAGES[s] || s || '-' }
export function riskText(r) { return RISK_LEVELS[r] || r || '-' }
export function riskType(r) { return { 1: 'info', 2: 'warning', 3: 'danger' }[r] || 'info' }
export function blockType(b) { return b === 1 ? 'danger' : 'warning' }
export function categoryText(c) { return c || '-' }
