import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '经办工作台' },
      },
      {
        path: 'settlement/outpatient',
        name: 'OutpatientSettle',
        component: () => import('@/views/settlement/OutpatientSettleView.vue'),
        meta: { title: '门诊医保结算' },
      },
      {
        path: 'settlement/inpatient',
        name: 'InpatientSettle',
        component: () => import('@/views/settlement/InpatientSettleView.vue'),
        meta: { title: '住院医保结算' },
      },
      {
        path: 'settlement/bill',
        name: 'SettleBill',
        component: () => import('@/views/settlement/SettleBillView.vue'),
        meta: { title: '结算单管理' },
      },
      {
        path: 'settlement/gateway',
        name: 'GatewaySettle',
        component: () => import('@/views/settlement/GatewaySettleView.vue'),
        meta: { title: '机构报盘接收' },
      },
      {
        path: 'settlement/monthly',
        name: 'MonthlySettle',
        component: () => import('@/views/settlement/MonthlySettleView.vue'),
        meta: { title: '月度结算管理' },
      },
      {
        path: 'settlement/daily',
        name: 'DailyAlloc',
        component: () => import('@/views/settlement/DailyAllocView.vue'),
        meta: { title: '日拨付管理' },
      },
      {
        path: 'settle/list',
        name: 'SettleList',
        component: () => import('@/views/settlement/SettleListView.vue'),
        meta: { title: '结算清单' },
      },
      {
        path: 'settle/catalog',
        name: 'CatalogSplit',
        component: () => import('@/views/settlement/CatalogSplitView.vue'),
        meta: { title: '目录分表管理' },
      },
      {
        path: 'insured/person',
        name: 'InsuredPerson',
        component: () => import('@/views/insured/InsuredPersonView.vue'),
        meta: { title: '参保人管理' },
      },
      {
        path: 'insured/service',
        name: 'InsureService',
        component: () => import('@/views/insured/InsureServiceView.vue'),
        meta: { title: '转移接续/个人账户/慢特病' },
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/stock/StockView.vue'),
        meta: { title: '药品耗材进销存' },
      },
      {
        path: 'stock/check',
        name: 'StockCheck',
        component: () => import('@/views/stock/StockCheckView.vue'),
        meta: { title: '库存盘点' },
      },
      {
        path: 'audit/fee',
        name: 'FeeAudit',
        component: () => import('@/views/audit/FeeAuditView.vue'),
        meta: { title: '费用审核' },
      },
      {
        path: 'catalog',
        name: 'Catalog',
        component: () => import('@/views/catalog/CatalogView.vue'),
        meta: { title: '三大目录与对照' },
      },
      {
        path: 'filing/cross-region',
        name: 'CrossRegionFiling',
        component: () => import('@/views/filing/CrossRegionFilingView.vue'),
        meta: { title: '异地就医备案' },
      },
      {
        path: 'reconciliation',
        name: 'Reconciliation',
        component: () => import('@/views/reconciliation/ReconciliationView.vue'),
        meta: { title: '清算对账' },
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/StatsView.vue'),
        meta: { title: '统计报表' },
      },
      {
        path: 'agreement',
        name: 'Agreement',
        component: () => import('@/views/agreement/AgreementView.vue'),
        meta: { title: '定点协议管理' },
      },
      {
        path: 'claim',
        name: 'ManualClaim',
        component: () => import('@/views/claim/ManualClaimView.vue'),
        meta: { title: '手工报销' },
      },
      // ---------------- 基金监管 ----------------
      {
        path: 'supervise/dashboard',
        name: 'SuperviseDashboard',
        component: () => import('@/views/supervise/SuperviseDashboard.vue'),
        meta: { title: '监管工作台' },
      },
      {
        path: 'supervise/rule',
        name: 'SuperviseRule',
        component: () => import('@/views/supervise/RuleView.vue'),
        meta: { title: '规则配置' },
      },
      {
        path: 'supervise/suspicion',
        name: 'SuperviseSuspicion',
        component: () => import('@/views/supervise/SuspicionView.vue'),
        meta: { title: '疑点稽核' },
      },
      {
        path: 'supervise/inspection',
        name: 'SuperviseInspection',
        component: () => import('@/views/supervise/InspectionView.vue'),
        meta: { title: '飞检任务' },
      },
      {
        path: 'supervise/recover',
        name: 'SuperviseRecover',
        component: () => import('@/views/supervise/RecoverView.vue'),
        meta: { title: '违规追回' },
      },
      {
        path: 'supervise/knowledge',
        name: 'SuperviseKnowledge',
        component: () => import('@/views/supervise/KnowledgeView.vue'),
        meta: { title: '知识库' },
      },
      {
        path: 'supervise/credit',
        name: 'SuperviseCredit',
        component: () => import('@/views/supervise/CreditView.vue'),
        meta: { title: '信用管理' },
      },
      {
        path: 'supervise/fraud',
        name: 'SuperviseFraud',
        component: () => import('@/views/supervise/FraudView.vue'),
        meta: { title: '大数据反欺诈' },
      },
      {
        path: 'supervise/precheck',
        name: 'SupervisePrecheck',
        component: () => import('@/views/supervise/PrecheckView.vue'),
        meta: { title: '事前提醒/事中拦截' },
      },
      {
        path: 'supervise/report',
        name: 'SuperviseReport',
        component: () => import('@/views/supervise/ReportView.vue'),
        meta: { title: '举报投诉与奖励' },
      },
      {
        path: 'drg',
        name: 'Drg',
        component: () => import('@/views/drg/DrgView.vue'),
        meta: { title: 'DRG/DIP支付监管' },
      },
      {
        path: 'supervise/random',
        name: 'SuperviseRandom',
        component: () => import('@/views/supervise/RandomView.vue'),
        meta: { title: '双随机一公开' },
      },
      {
        path: 'supervise/selfcheck',
        name: 'SuperviseSelfCheck',
        component: () => import('@/views/supervise/SelfCheckView.vue'),
        meta: { title: '自查自纠' },
      },
      {
        path: 'supervise/quality',
        name: 'SuperviseQuality',
        component: () => import('@/views/supervise/QualityView.vue'),
        meta: { title: '结算清单质控' },
      },
      {
        path: 'supervise/trace',
        name: 'SuperviseTrace',
        component: () => import('@/views/supervise/TraceView.vue'),
        meta: { title: '药品追溯码监管' },
      },
      // ---------------- 系统管理 ----------------
      {
        path: 'system/operlog',
        name: 'SystemOperLog',
        component: () => import('@/views/system/OperLogView.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'system/todo',
        name: 'SystemTodo',
        component: () => import('@/views/system/TodoView.vue'),
        meta: { title: '待办中心' },
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/RoleView.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/MenuView.vue'),
        meta: { title: '菜单管理' },
      },
      {
        path: 'system/post',
        name: 'SystemPost',
        component: () => import('@/views/system/PostView.vue'),
        meta: { title: '岗位管理' },
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/DictView.vue'),
        meta: { title: '字典管理' },
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/views/system/DeptView.vue'),
        meta: { title: '科室管理' },
      },
      {
        path: 'system/employee',
        name: 'SystemEmployee',
        component: () => import('@/views/system/EmployeeView.vue'),
        meta: { title: '员工管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫：无 token 一律去登录页；已登录访问登录页回首页
// menus 只在 login() 里拉过一次、不持久化，刷新后必须在此补拉，否则侧边栏为空
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const token = auth.token || localStorage.getItem('token')
  if (to.path === '/login') {
    return token ? '/' : true
  }
  if (!token) {
    return { path: '/login', replace: true }
  }
  if (auth.menus.length === 0) {
    try {
      await auth.fetchInfoAndMenus()
    } catch (e) {
      // 401 由 axios 拦截器整页跳登录，此处不阻断导航
    }
  }
  return true
})

router.afterEach((to) => {
  const t = to.meta?.title
  if (t) document.title = `${t} - 医保结算经办平台`
})

export default router
