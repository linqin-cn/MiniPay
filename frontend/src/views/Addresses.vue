<template>
  <section class="address-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">addresses</p>
        <h2>地址管理</h2>
      </div>
      <button class="primary-lite" @click="resetForm">新增地址</button>
    </header>

    <div class="address-layout">
      <main class="address-list">
        <div v-if="loading" class="state-panel">正在加载地址...</div>
        <div v-else-if="addresses.length === 0" class="state-panel empty">
          <strong>还没有收货地址</strong>
          <span>先新增一个地址，确认订单时会自动使用默认地址。</span>
        </div>

        <article v-for="address in addresses" :key="address.id" class="address-card">
          <div class="card-main">
            <div class="name-line">
              <strong>{{ address.receiverName }}</strong>
              <span>{{ address.receiverPhone }}</span>
              <em v-if="address.isDefault">默认</em>
            </div>
            <p>{{ fullAddress(address) }}</p>
          </div>
          <div class="card-actions">
            <button @click="editAddress(address)">编辑</button>
            <button v-if="!address.isDefault" @click="makeDefault(address)">设为默认</button>
            <button class="danger" @click="removeAddress(address)">删除</button>
          </div>
        </article>
      </main>

      <aside class="form-panel">
        <h3>{{ form.id ? '编辑地址' : '新增地址' }}</h3>
        <label>收货人<input v-model.trim="form.receiverName" placeholder="姓名" /></label>
        <label>手机号<input v-model.trim="form.receiverPhone" placeholder="手机号" /></label>
        <div class="region-grid">
          <label>省份<input v-model.trim="form.province" placeholder="省份" /></label>
          <label>城市<input v-model.trim="form.city" placeholder="城市" /></label>
          <label>区县<input v-model.trim="form.district" placeholder="区县" /></label>
        </div>
        <label>详细地址<textarea v-model.trim="form.detailAddress" placeholder="街道、门牌号等" /></label>
        <label class="check-line"><input v-model="form.isDefault" type="checkbox" />设为默认地址</label>
        <button class="submit-btn" :disabled="saving" @click="saveAddress">{{ saving ? '保存中...' : '保存地址' }}</button>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createAddress, deleteAddress, getAddresses, setDefaultAddress, updateAddress } from '@/api'

const loading = ref(false)
const saving = ref(false)
const notice = ref('')
const addresses = ref([])
const form = reactive(emptyForm())

function emptyForm() {
  return {
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false
  }
}

function fullAddress(address) {
  return [address.province, address.city, address.district, address.detailAddress].filter(Boolean).join(' ')
}

function demoAddresses() {
  return [{
    id: 'local-1',
    receiverName: '林同学',
    receiverPhone: '13000000000',
    province: '上海市',
    city: '上海市',
    district: '浦东新区',
    detailAddress: 'MiniPay 路 100 号',
    isDefault: true
  }]
}

async function loadAddresses() {
  loading.value = true
  notice.value = ''
  try {
    const res = await getAddresses()
    addresses.value = res.data?.data || []
    if (!addresses.value.length) {
      addresses.value = demoAddresses()
      notice.value = '当前使用演示地址数据'
    }
  } catch (error) {
    addresses.value = demoAddresses()
    notice.value = '地址服务暂不可用，当前使用演示地址数据'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, emptyForm())
}

function editAddress(address) {
  Object.assign(form, { ...address })
}

function validateForm() {
  if (!form.receiverName || !form.receiverPhone || !form.detailAddress) {
    notice.value = '请填写收货人、手机号和详细地址'
    return false
  }
  return true
}

async function saveAddress() {
  if (!validateForm()) return
  saving.value = true
  const payload = { ...form }
  delete payload.id
  try {
    if (typeof form.id === 'number') {
      await updateAddress(form.id, payload)
      notice.value = '地址已更新'
    } else {
      await createAddress(payload)
      notice.value = '地址已新增'
    }
    resetForm()
    await loadAddresses()
  } catch (error) {
    if (form.id) {
      const index = addresses.value.findIndex(item => item.id === form.id)
      if (index >= 0) addresses.value[index] = { ...form }
    } else {
      addresses.value.unshift({ ...form, id: `local-${Date.now()}` })
    }
    if (form.isDefault) setLocalDefault(form.id)
    notice.value = '后端暂不可用，已在当前页面临时保存'
    resetForm()
  } finally {
    saving.value = false
  }
}

async function makeDefault(address) {
  try {
    if (typeof address.id === 'number') await setDefaultAddress(address.id)
  } catch (error) {}
  setLocalDefault(address.id)
}

function setLocalDefault(id) {
  addresses.value = addresses.value.map(item => ({ ...item, isDefault: item.id === id }))
}

async function removeAddress(address) {
  addresses.value = addresses.value.filter(item => item.id !== address.id)
  if (typeof address.id === 'number') {
    try { await deleteAddress(address.id) } catch (error) {}
  }
}

onMounted(loadAddresses)
</script>

<style scoped>
.address-page { display: grid; gap: 20px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 6px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0; font-weight: 800; }
h2, h3, p { margin: 0; }
h2 { font-size: 28px; line-height: 1.2; color: #111827; }
button { font-family: inherit; }
.address-layout { display: grid; grid-template-columns: minmax(0, 1fr) 372px; gap: 18px; align-items: start; }
.address-list { display: grid; gap: 12px; min-width: 0; }
.address-card, .form-panel, .state-panel { background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; box-shadow: 0 12px 28px rgba(17, 24, 39, .04); }
.address-card { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 16px; padding: 18px; align-items: center; }
.card-main { display: grid; gap: 9px; min-width: 0; }
.name-line { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.name-line strong { color: #111827; font-size: 17px; }
.name-line span, .address-card p { color: #64748b; line-height: 1.55; }
.name-line em { font-style: normal; color: #166534; background: #dcfce7; padding: 3px 8px; border-radius: 999px; font-size: 12px; font-weight: 900; }
.card-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
.card-actions button, .primary-lite { min-height: 36px; border: 0; border-radius: 6px; padding: 0 12px; background: #e7f2eb; color: #14532d; font-weight: 900; cursor: pointer; }
.card-actions .danger { background: #fff1f0; color: #b42318; }
.form-panel { position: sticky; top: 86px; display: grid; gap: 13px; padding: 18px; }
.form-panel h3 { color: #111827; font-size: 18px; }
.form-panel label { display: grid; gap: 7px; color: #475569; font-size: 14px; font-weight: 800; }
.form-panel input, .form-panel textarea { width: 100%; border: 1px solid #d7dde8; border-radius: 6px; padding: 11px 12px; outline: none; background: #fff; color: #111827; transition: border-color .2s, box-shadow .2s; }
.form-panel input:focus, .form-panel textarea:focus { border-color: #14532d; box-shadow: 0 0 0 3px rgba(20, 83, 45, .1); }
.form-panel textarea { min-height: 92px; resize: vertical; }
.region-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.check-line { display: flex !important; grid-template-columns: none; align-items: center; gap: 8px; }
.check-line input { width: 16px; height: 16px; accent-color: #14532d; }
.submit-btn { min-height: 44px; border: 0; border-radius: 6px; background: #14532d; color: #fff; font-weight: 900; cursor: pointer; }
.submit-btn:disabled { opacity: .6; cursor: not-allowed; }
.notice { color: #8a5a00; background: #fff4cc; padding: 10px 12px; border-radius: 6px; line-height: 1.5; }
.state-panel { padding: 28px; color: #64748b; }
.state-panel.empty { display: grid; gap: 8px; }
.state-panel strong { color: #111827; }
@media (max-width: 960px) { .address-layout { grid-template-columns: 1fr; } .form-panel { position: static; } }
@media (max-width: 680px) { .page-head { align-items: flex-start; flex-direction: column; } .address-card { grid-template-columns: 1fr; } .card-actions { justify-content: flex-start; } .region-grid { grid-template-columns: 1fr; } }
</style>
