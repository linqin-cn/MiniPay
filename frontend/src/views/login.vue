<template>
  <section class="login-page">
    <div class="login-panel">
      <div class="brand-block">
        <p>MiniPay</p>
        <h1>选择身份登录</h1>
        <span>普通用户进入购物流程，商家进入经营后台。</span>
      </div>

      <div class="role-switch">
        <button :class="{ active: loginForm.role === 'BUYER' }" @click="loginForm.role = 'BUYER'">普通用户</button>
        <button :class="{ active: loginForm.role === 'MERCHANT' }" @click="loginForm.role = 'MERCHANT'">商家</button>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label>
          账号
          <input v-model.trim="loginForm.username" placeholder="用户名" />
        </label>
        <label>
          密码
          <input v-model.trim="loginForm.password" type="password" placeholder="密码" />
        </label>
        <button class="submit-btn" :disabled="submitting" type="submit">{{ submitting ? '登录中...' : '登录' }}</button>
      </form>

      <div class="panel-footer">
        <span>还没有账号？</span>
        <button type="button" @click="openRegister">注册</button>
      </div>
    </div>

    <div
      v-if="registerVisible"
      class="modal-mask"
      :style="modalMaskStyle"
      @click.self="closeRegister"
    >
      <div class="register-modal" :style="registerModalStyle">
        <button class="close-btn" :style="closeButtonStyle" type="button" aria-label="关闭" @click="closeRegister">×</button>
        <div class="brand-block">
          <p>MiniPay</p>
          <h1>创建新账号</h1>
          <span>同一账号名可以分别注册普通用户和商家身份。</span>
        </div>

        <div class="role-switch">
          <button :class="{ active: registerForm.role === 'BUYER' }" @click="registerForm.role = 'BUYER'">普通用户</button>
          <button :class="{ active: registerForm.role === 'MERCHANT' }" @click="registerForm.role = 'MERCHANT'">商家</button>
        </div>

        <form class="login-form" @submit.prevent="handleRegister">
          <label>
            账号
            <input v-model.trim="registerForm.username" placeholder="用户名" />
          </label>
          <label>
            密码
            <input v-model.trim="registerForm.password" type="password" placeholder="密码" />
          </label>
          <label>
            昵称
            <input v-model.trim="registerForm.nickname" placeholder="昵称，选填" />
          </label>
          <label>
            手机号
            <input v-model.trim="registerForm.phone" placeholder="手机号，选填" />
          </label>
          <button class="submit-btn" :disabled="submitting" type="submit">{{ submitting ? '处理中...' : '注册并登录' }}</button>
        </form>

        <div class="panel-footer">
          <span>已有账号？</span>
          <button type="button" @click="closeRegister">登录</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { notification } from 'ant-design-vue'
import { login, register } from '@/api'

const router = useRouter()
const submitting = ref(false)
const registerVisible = ref(false)
const modalMaskStyle = {
  position: 'fixed',
  inset: '0',
  zIndex: 1000,
  display: 'grid',
  placeItems: 'center',
  padding: '24px',
  background: 'rgba(15, 23, 42, .38)',
  backdropFilter: 'blur(3px)'
}
const registerModalStyle = {
  position: 'relative',
  width: 'min(420px, 100%)',
  maxHeight: 'calc(100vh - 48px)',
  overflowY: 'auto',
  display: 'grid',
  gap: '18px',
  padding: '28px',
  background: '#fff',
  border: '1px solid #dfe7e2',
  borderRadius: '10px',
  boxShadow: '0 20px 46px rgba(17, 24, 39, .08)'
}
const closeButtonStyle = {
  position: 'absolute',
  top: '14px',
  right: '16px',
  width: '30px',
  height: '30px',
  border: 0,
  borderRadius: '50%',
  background: '#f1f5f9',
  color: '#334155',
  fontSize: '20px',
  lineHeight: 1,
  cursor: 'pointer'
}
const loginForm = reactive({
  username: '',
  password: '',
  role: 'BUYER'
})
const registerForm = reactive({
  username: '',
  password: '',
  role: 'BUYER',
  nickname: '',
  phone: ''
})

function openRegister() {
  registerForm.username = loginForm.username
  registerForm.password = loginForm.password
  registerForm.role = loginForm.role
  registerVisible.value = true
}

function closeRegister() {
  registerVisible.value = false
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    notification.warning({ description: '请输入账号和密码' })
    return
  }
  submitting.value = true
  try {
    await loginAndRedirect(loginForm, '登录成功')
  } catch (error) {
    notification.error({ description: error.response?.data?.message || error.message || '登录失败' })
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    notification.warning({ description: '请输入账号和密码' })
    return
  }
  submitting.value = true
  try {
    const registerRes = await register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname,
      phone: registerForm.phone,
      role: registerForm.role
    })
    if (!registerRes.data?.success) {
      throw new Error(registerRes.data?.message || '注册失败')
    }
    await loginAndRedirect(registerForm, '注册并登录成功')
  } catch (error) {
    notification.error({ description: error.response?.data?.message || error.message || '注册失败' })
  } finally {
    submitting.value = false
  }
}

async function loginAndRedirect(form, successMessage) {
  const res = await login({
    username: form.username,
    password: form.password,
    role: form.role
  })
  const data = res.data?.data
  if (!res.data?.success || !data?.token) {
    throw new Error(res.data?.message || '登录失败')
  }
  localStorage.setItem('token', data.token)
  localStorage.setItem('userId', String(data.id || ''))
  localStorage.setItem('userRole', data.role || form.role)
  localStorage.setItem('userInfo', JSON.stringify(data))
  window.dispatchEvent(new Event('minipay-auth-change'))
  notification.success({ description: successMessage })
  router.push((data.role || form.role) === 'MERCHANT' ? '/merchant' : '/products')
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #f3f6f4; }
.login-panel, .register-modal { width: min(420px, 100%); display: grid; gap: 18px; padding: 28px; background: #fff; border: 1px solid #dfe7e2; border-radius: 10px; box-shadow: 0 20px 46px rgba(17, 24, 39, .08); }
.brand-block { display: grid; gap: 8px; }
.brand-block p { margin: 0; color: #14532d; font-weight: 900; }
.brand-block h1 { margin: 0; color: #111827; font-size: 28px; }
.brand-block span { color: #64748b; line-height: 1.6; }
.role-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding: 4px; background: #f8fafc; border-radius: 8px; }
.role-switch button { height: 38px; border: 0; border-radius: 6px; background: transparent; color: #475569; font-weight: 900; cursor: pointer; }
.role-switch button.active { background: #14532d; color: #fff; }
.login-form { display: grid; gap: 12px; }
.login-form label { display: grid; gap: 7px; color: #475569; font-weight: 800; }
.login-form input { height: 42px; border: 1px solid #d7dde8; border-radius: 6px; padding: 0 12px; outline: none; }
.login-form input:focus { border-color: #14532d; box-shadow: 0 0 0 3px rgba(20, 83, 45, .1); }
.submit-btn { height: 44px; border: 0; border-radius: 6px; background: #14532d; color: #fff; font-weight: 900; cursor: pointer; }
.submit-btn:disabled { opacity: .65; cursor: not-allowed; }
.panel-footer { display: flex; align-items: center; justify-content: flex-end; gap: 8px; color: #64748b; font-size: 14px; }
.panel-footer button { border: 0; background: transparent; color: #14532d; font-weight: 900; cursor: pointer; padding: 0; }
.panel-footer button:hover { text-decoration: underline; }
.modal-mask { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 24px; background: rgba(15, 23, 42, .38); backdrop-filter: blur(3px); }
.register-modal { position: relative; }
.close-btn { position: absolute; top: 14px; right: 16px; width: 30px; height: 30px; border: 0; border-radius: 50%; background: #f1f5f9; color: #334155; font-size: 20px; line-height: 1; cursor: pointer; }
.close-btn:hover { background: #e2e8f0; }
</style>
