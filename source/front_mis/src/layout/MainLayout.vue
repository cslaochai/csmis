<template>
  <el-container class="h-screen app-shell">
    <!-- 顶部 Header：与 cslk HIS 同风格（深蓝顶栏 + logo + 面包屑 + 用户区） -->
    <el-header class="app-header">
      <div class="header-left">
        <router-link to="/" class="brand" title="返回首页" :style="brandStyle">
          <BrandMark :size="34" tone="light" />
          <span v-if="!sidebarCollapsed" class="brand-name">医保结算经办</span>
        </router-link>
        <button class="collapse-btn" :title="sidebarCollapsed ? '展开菜单' : '收起菜单'" @click="toggleSidebar">
          <el-icon v-if="sidebarCollapsed"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </button>
        <nav class="breadcrumb-nav">
          <el-icon class="crumb-icon"><HomeFilled /></el-icon>
          <span class="crumb-label">医保结算经办</span>
          <el-icon v-if="pageTitle" class="crumb-sep"><ArrowRight /></el-icon>
          <span v-if="pageTitle" class="crumb-current">{{ pageTitle }}</span>
        </nav>
      </div>
      <div class="header-right">
        <el-badge :value="pendingCount" :hidden="pendingCount === 0" type="danger">
          <button class="header-icon-btn" title="待办消息">
            <el-icon><Bell /></el-icon>
          </button>
        </el-badge>
        <div class="header-divider"></div>
        <div class="header-user">
          <span class="header-user-name">{{ auth.realName }}</span>
          <span class="header-user-role">{{ auth.deptName }} · {{ auth.roleName }}</span>
        </div>
        <div class="header-divider"></div>
        <div class="header-clock">
          <el-icon><Calendar /></el-icon>
          <span class="clock-time">{{ currentTime }}</span>
          <span class="clock-week">{{ currentWeekday }}</span>
        </div>
        <div class="header-divider"></div>
        <el-dropdown trigger="click" @command="onCommand">
          <button class="header-icon-btn" title="设置">
            <el-icon><Setting /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">
                <el-icon><Key /></el-icon>修改密码
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 下方：左侧导航 + 主内容 -->
    <el-container class="app-body">
      <el-aside :width="sidebarCollapsed ? '64px' : '264px'" class="app-sidebar">
        <el-scrollbar class="flex-1">
          <el-menu
            :collapse="sidebarCollapsed"
            :collapse-transition="false"
            :default-active="activeMenu"
            router
            background-color="transparent"
            text-color="rgba(255,255,255,0.75)"
            active-text-color="#ffffff"
            class="!border-none app-menu"
          >
            <template v-for="menu in auth.menus" :key="menu.id">
              <el-sub-menu v-if="menu.menuType === 1" :index="String(menu.id)">
                <template #title>
                  <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
                  <span>{{ menu.menuName }}</span>
                </template>
                <el-menu-item
                  v-for="child in menu.children || []"
                  :key="child.id"
                  :index="child.path || '/'"
                >
                  <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
                  <span>{{ child.menuName }}</span>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="menu.path || '/'">
                <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
                <span>{{ menu.menuName }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px" append-to-body>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="onChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { changePasswordApi } from '@/api/auth'
import { encryptPassword } from '@/lib/password'
import { dashboardSummary } from '@/api/insurance'
import BrandMark from '@/components/BrandMark.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '医保结算经办平台')
const avatarChar = computed(() => (auth.realName || '用').slice(0, 1))

// ========== 侧栏折叠（与 HIS 一致：按钮在顶栏，侧栏收成图标） ==========
const sidebarCollapsed = ref(false)
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 顶栏品牌区宽度与左侧菜单栏对齐（抄 HIS）：
// 展开 240px + 顶栏 24px 左内边距 = 侧栏 264px，右边缘对齐并带竖线；
// 折叠时 marginLeft -24px + 宽 64px，右边缘对齐侧栏折叠态 64px。
const brandStyle = computed(() =>
  sidebarCollapsed.value
    ? { width: '64px', marginLeft: '-24px', justifyContent: 'center' }
    : { width: '240px' }
)

// ========== 实时时钟 ==========
const currentTime = ref('')
const currentWeekday = ref('')
const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
let clockTimer = null

const updateClock = () => {
  const now = new Date()
  const y = now.getFullYear()
  const mon = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}-${mon}-${d} ${hh}:${mm}:${ss}`
  currentWeekday.value = dayNames[now.getDay()]
}

// 顶部待办角标：从工作台汇总接口读取
const pendingCount = ref(0)
onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  try {
    const res = await dashboardSummary()
    pendingCount.value = res.data.todo?.total || 0
  } catch (e) {
    // 工作台接口失败不阻塞布局
  }
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await auth.logout()
    router.replace('/login')
  } else if (cmd === 'password') {
    pwdForm.oldPassword = pwdForm.newPassword = pwdForm.confirmPassword = ''
    pwdVisible.value = true
  }
}

// 修改密码
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref()
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === pwdForm.value.newPassword ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur',
    },
  ],
}
async function onChangePassword() {
  await pwdFormRef.value.validate()
  pwdSaving.value = true
  try {
    // 旧/新口令先 SM2 加密再上网（与登录同一套公钥，后端解密后校验）
    const [oldPassword, newPassword] = await Promise.all([
      encryptPassword(pwdForm.value.oldPassword),
      encryptPassword(pwdForm.value.newPassword),
    ])
    await changePasswordApi({ oldPassword, newPassword })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } catch (e) {
    // 后端 message 已全局提示
  } finally {
    pwdSaving.value = false
  }
}
</script>

<style scoped>
.app-shell {
  height: 100vh;
}

/* ---------- 深蓝顶栏（与 HIS 一致） ---------- */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #0b4f8a;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.1), 0 2px 10px rgba(6, 45, 84, 0.16);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.3);
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
  white-space: nowrap;
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.crumb-icon {
  color: rgba(255, 255, 255, 0.7);
}

.crumb-sep {
  color: rgba(255, 255, 255, 0.5);
}

.crumb-current {
  color: #fff;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.header-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  font-size: 18px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.header-icon-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.3);
  margin: 0 4px;
}

.header-user {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.header-user-name {
  font-size: 13px;
  color: #fff;
  font-weight: 500;
}

.header-user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
}

.header-clock {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 13px;
}

.clock-time {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.clock-week {
  color: rgba(255, 255, 255, 0.85);
}

/* ---------- 侧栏（菜单区，移除了原品牌块） ---------- */
.app-body {
  height: calc(100vh - 64px);
}

.app-sidebar {
  background: #0a3d6b;
  transition: width 0.2s ease;
}

.app-menu {
  border-right: none;
}

.app-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.16);
}

/* 子菜单展开面板透出侧栏底色（#0A3D6B），避免 Element Plus 默认白底在深蓝侧栏里露白 */
.app-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.app-main {
  background: #f2f7fb;
  padding: 16px;
}
</style>
