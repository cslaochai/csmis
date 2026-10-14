<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="login-aside">
        <div class="aside-decor">
          <span class="orb orb-a"></span>
          <span class="orb orb-b"></span>
          <span class="orb orb-c"></span>
          <BrandMark class="aside-watermark" :size="230" tone="light" />
        </div>

        <div class="aside-body">
          <div class="aside-head">
            <BrandMark :size="80" tone="light" />
            <div>
              <div class="brand-title">医保结算经办平台</div>
              <div class="brand-sub">Medical Insurance Settlement Platform</div>
            </div>
          </div>

          <ul class="aside-list">
            <li v-for="item in highlights" :key="item.title">
              <span class="aside-icon"><el-icon :size="21"><component :is="item.icon" /></el-icon></span>
              <div>
                <div class="aside-item-title">{{ item.title }}</div>
                <div class="aside-item-desc">{{ item.desc }}</div>
              </div>
            </li>
          </ul>
        </div>

        <div class="aside-foot">
          <el-icon :size="14"><Checked /></el-icon>
          <span>数据内网隔离 · 操作留痕可追溯</span>
        </div>
      </aside>

      <section class="login-main">
        <div class="login-card">
          <div class="card-brand">
            <BrandMark :size="56" tone="brand" />
            <div>
              <div class="card-brand-title">医保结算经办平台</div>
              <div class="card-brand-sub">Medical Insurance Settlement</div>
            </div>
          </div>

          <div class="card-head">
            <h2>账号登录</h2>
            <p>请使用经办系统分配的账号进入</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="onSubmit">
              <el-icon v-if="!loading" class="btn-icon"><Promotion /></el-icon>
              <span>登 录</span>
            </el-button>
          </el-form>

          <div class="login-tip">
            <div class="tip-title"><el-icon :size="14"><Key /></el-icon><span>演示账号</span></div>
            <div class="tip-row" v-for="row in demoAccounts" :key="row.account">
              <span class="tip-role"><el-icon :size="14"><component :is="row.icon" /></el-icon>{{ row.role }}</span>
              <span class="tip-account">{{ row.account }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Checked,
  DataAnalysis,
  FirstAidKit,
  Key,
  Lock,
  Odometer,
  Promotion,
  User,
  UserFilled,
  Avatar,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import BrandMark from '@/components/BrandMark.vue'

const router = useRouter()
const auth = useAuthStore()

const highlights = [
  { icon: DataAnalysis, title: '结算清单经办', desc: '登记、审核、上传一体化流转' },
  { icon: Odometer, title: '费用智能审核', desc: '规则引擎拦截违规费用' },
  { icon: FirstAidKit, title: '医保目录管理', desc: '三大目录与对照维护' },
]

const demoAccounts = [
  { role: '经办管理员', account: 'admin / admin123', icon: Avatar },
  { role: '经办员', account: 'yb001 / 123456', icon: UserFilled },
  { role: '审核员', account: 'yb002 / 123456', icon: Checked },
]

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')
    router.replace('/')
  } catch (e) {
    // 登录失败提示由后端 message 决定（登录接口不弹全局提示，这里内联展示）
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #0b3b66 0%, #1269b5 55%, #0e9488 120%);
}
.login-shell {
  display: flex;
  width: 100%;
  max-width: 940px;
  min-height: 520px;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 20px 56px rgba(11, 59, 102, 0.38);
}

.login-aside {
  position: relative;
  flex: 1;
  min-width: 0;
  padding: 44px 40px;
  display: flex;
  flex-direction: column;
  gap: 32px;
  color: #eaf3fb;
  background: linear-gradient(160deg, #0b3b66 0%, #12609f 60%, #0e7f78 100%);
  overflow: hidden;
}
.aside-body {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 30px;
}
.aside-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.orb-a {
  width: 220px;
  height: 220px;
  top: -70px;
  right: -60px;
}
.orb-b {
  width: 140px;
  height: 140px;
  bottom: -50px;
  left: -40px;
  background: rgba(255, 255, 255, 0.06);
}
.orb-c {
  width: 72px;
  height: 72px;
  top: 46%;
  right: 12%;
  background: rgba(14, 148, 136, 0.28);
}

.aside-watermark {
  position: absolute;
  right: -34px;
  bottom: -46px;
  opacity: 0.1;
}
.aside-head {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
}
.brand-title {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
}
.brand-sub {
  margin-top: 4px;
  font-size: 11.5px;
  letter-spacing: 0.4px;
  color: rgba(234, 243, 251, 0.7);
}

.aside-list {
  position: relative;
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.aside-list li {
  display: flex;
  align-items: center;
  gap: 12px;
}
.aside-icon {
  flex: none;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #1269b5;
  box-shadow: 0 2px 8px rgba(4, 26, 45, 0.28);
}
.aside-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}
.aside-item-desc {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(234, 243, 251, 0.66);
}

.aside-foot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(234, 243, 251, 0.75);
}

.login-main {
  flex: none;
  width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 40px;
}
.login-card {
  width: 100%;
}
.card-brand {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 22px;
}
.card-brand-title {
  font-size: 15px;
  font-weight: 700;
  color: #0b3b66;
}
.card-brand-sub {
  margin-top: 2px;
  font-size: 10.5px;
  letter-spacing: 0.3px;
  color: #8aa0b8;
}
.card-head h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0b3b66;
}
.card-head p {
  margin: 6px 0 26px;
  font-size: 12.5px;
  color: #8aa0b8;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 2px;
}
.btn-icon {
  margin-right: 6px;
}

.login-tip {
  margin-top: 22px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f2f7fb;
  border: 1px solid #e2edf6;
  color: #5c7a99;
  font-size: 12px;
}
.tip-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #1269b5;
  margin-bottom: 8px;
}
.tip-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.9;
}
.tip-role {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: none;
}
.tip-account {
  font-family: Menlo, Consolas, monospace;
  color: #45627f;
}

.login-main :deep(.el-input__prefix) {
  color: #1269b5;
}

@media (max-width: 860px) {
  .login-aside {
    display: none;
  }
  .login-shell {
    max-width: 420px;
    min-height: auto;
  }
  .login-main {
    width: 100%;
    padding: 40px 32px;
  }
  .card-brand {
    display: flex;
  }
}
</style>
