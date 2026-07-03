<template>
  <div class="container">
    <div class="card" v-if="order">
      <h2>订单确认</h2>
      
      <div class="order-info">
        <div class="info-row">
          <span class="label">订单号</span>
          <span class="value">{{ order.orderId }}</span>
        </div>
        <div class="info-row">
          <span class="label">金额</span>
          <span class="value amount">¥{{ order.amount ? order.amount.toFixed(2) : '0.00' }}</span>
        </div>
        <div class="info-row">
          <span class="label">状态</span>
          <span :class="['value', 'status', order.status ? order.status.toLowerCase() : '']">{{ statusText }}</span>
        </div>
        <div class="info-row" v-if="order.description">
          <span class="label">描述</span>
          <span class="value">{{ order.description }}</span>
        </div>
        <div class="info-row">
          <span class="label">创建时间</span>
          <span class="value">{{ formatTime(order.createdAt) }}</span>
        </div>
      </div>

      <!-- 支付方式选择 -->
      <div v-if="showPayButton" class="pay-section">
        <div class="pay-type-selector">
          <button 
            :class="['pay-type-btn', { active: payType === 'alipay' }]"
            @click="payType = 'alipay'"
          >
            <span class="pay-icon">💙</span>
            支付宝支付
          </button>
          <button 
            :class="['pay-type-btn', { active: payType === 'mock' }]"
            @click="payType = 'mock'"
          >
            <span class="pay-icon">🎮</span>
            模拟支付
          </button>
        </div>
        
        <button @click="handlePay" :disabled="paying" class="pay-btn">
          <span v-if="paying" class="loading"></span>
          {{ paying ? '支付中...' : '立即支付' }}
        </button>
      </div>

      <!-- 支付宝二维码展示 -->
      <div v-if="showQRCode" class="qr-section">
        <h3>支付宝扫码支付</h3>
        <div class="qr-container">
          <img :src="qrCodeImage" alt="支付宝二维码" class="qr-image" />
        </div>
        <p class="qr-tip">请使用支付宝扫描二维码完成支付</p>
        <p class="amount">¥{{ order.amount ? order.amount.toFixed(2) : '0.00' }}</p>
        
        <div class="qr-actions">
          <button @click="queryPayResult" :disabled="querying" class="check-btn">
            {{ querying ? '查询中...' : '我已支付，查询结果' }}
          </button>
          <button @click="refreshQRCode" class="refresh-btn">刷新二维码</button>
          <button @click="cancelPay" class="cancel-btn">取消支付</button>
        </div>
      </div>

      <!-- 模拟支付弹窗 -->
      <div v-if="showMockModal" class="modal-overlay" @click="closeModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>模拟支付</h3>
            <button @click="closeModal" class="close-btn">×</button>
          </div>
          <div class="modal-body">
            <div class="payment-info">
              <p>订单号：{{ order.orderId }}</p>
              <p>支付金额：¥{{ order.amount ? order.amount.toFixed(2) : '0.00' }}</p>
            </div>
            <div class="mock-result">
              <p>点击按钮模拟支付结果（80%成功率）</p>
              <button @click="confirmMockPayment" :disabled="processing" class="confirm-btn">
                {{ processing ? '处理中...' : '确认支付' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 支付结果 -->
      <div v-if="paymentResult" class="result-section">
        <div :class="['result-card', paymentResult.status ? paymentResult.status.toLowerCase() : '']">
          <div class="result-icon">
            <span v-if="paymentResult.status === 'SUCCESS'">✓</span>
            <span v-else>✗</span>
          </div>
          <h3>{{ paymentResult.status === 'SUCCESS' ? '支付成功' : '支付失败' }}</h3>
          <p v-if="paymentResult.status === 'SUCCESS'">
            支付金额：¥{{ order.amount ? order.amount.toFixed(2) : '0.00' }}
          </p>
          <p v-if="paymentResult.paymentId">支付流水号：{{ paymentResult.paymentId }}</p>
          <div class="result-actions">
            <button @click="goBack" class="back-btn">返回首页</button>
          </div>
        </div>
      </div>

      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
    </div>

    <div v-else-if="loading" class="loading-container">
      <div class="loading"></div>
      <p>加载订单信息...</p>
    </div>

    <div v-else class="empty-state">
      <p>订单不存在或已过期</p>
      <button @click="goBack" class="back-btn">返回首页</button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder, createPayment, queryPaymentStatus } from '../api'

export default {
  name: 'PayOrder',
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    const order = ref(null)
    const loading = ref(true)
    const paying = ref(false)
    const processing = ref(false)
    const querying = ref(false)
    const errorMessage = ref('')
    const payType = ref('alipay')
    const showQRCode = ref(false)
    const qrCodeImage = ref('')
    const showMockModal = ref(false)
    const paymentResult = ref(null)

    const showPayButton = computed(() => {
      return order.value && order.value.status === 'PENDING' && !showQRCode.value && !paymentResult.value
    })

    const statusText = computed(() => {
      const statusMap = { 'PENDING': '待支付', 'PAID': '已支付', 'FAILED': '支付失败' }
      return statusMap[order.value?.status] || order.value?.status
    })

    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }

    const loadOrder = async () => {
      const orderId = route.params.orderId
      try {
        const res = await getOrder(orderId)
        if (res.data && res.data.success) {
          order.value = res.data.data
          if (order.value.status !== 'PENDING') {
            paymentResult.value = { status: order.value.status === 'PAID' ? 'SUCCESS' : 'FAILED', paymentId: '' }
          }
        } else {
          errorMessage.value = '订单不存在'
        }
      } catch (error) {
        errorMessage.value = '加载订单失败'
      } finally {
        loading.value = false
      }
    }

    const handlePay = async () => {
      if (payType.value === 'alipay') {
        await createAlipayPayment()
      } else {
        showMockModal.value = true
      }
    }

    const createAlipayPayment = async () => {
      paying.value = true
      errorMessage.value = ''
      try {
        const res = await createPayment({
          orderId: order.value.orderId,
          amount: Math.round(order.value.amount * 100),
          payType: 'alipay'
        })
        if (res.data && res.data.success) {
          qrCodeImage.value = res.data.data.qrCode
          showQRCode.value = true
        } else {
          errorMessage.value = (res.data && res.data.message) || '创建支付失败'
        }
      } catch (error) {
        errorMessage.value = '网络错误，请重试'
        console.error(error)
      } finally {
        paying.value = false
      }
    }

    const queryPayResult = async () => {
      querying.value = true
      errorMessage.value = ''
      try {
        const res = await queryPaymentStatus(order.value.orderId)
        if (res.data && res.data.success) {
          const status = res.data.data.status
          if (status === 'SUCCESS') {
            order.value.status = 'PAID'
            paymentResult.value = { status: 'SUCCESS', paymentId: res.data.data.paymentId }
            showQRCode.value = false
          } else if (status === 'FAILED') {
            order.value.status = 'FAILED'
            paymentResult.value = { status: 'FAILED', paymentId: res.data.data.paymentId }
            showQRCode.value = false
          } else {
            errorMessage.value = '尚未支付成功，请稍后再试'
          }
        }
      } catch (error) {
        errorMessage.value = '查询失败，请重试'
      } finally {
        querying.value = false
      }
    }

    const refreshQRCode = async () => {
      await createAlipayPayment()
    }

    const cancelPay = () => {
      showQRCode.value = false
    }

    const closeModal = () => {
      showMockModal.value = false
    }

    const confirmMockPayment = async () => {
      processing.value = true
      try {
        const res = await createPayment({
          orderId: order.value.orderId,
          amount: Math.round(order.value.amount * 100),
          payType: 'mock'
        })
        if (res.data && res.data.success) {
          paymentResult.value = res.data.data
          showMockModal.value = false
        } else {
          errorMessage.value = (res.data && res.data.message) || '支付失败'
        }
      } catch (error) {
        errorMessage.value = '网络错误'
      } finally {
        processing.value = false
      }
    }

    const goBack = () => {
      router.push('/create')
    }

    onMounted(() => { loadOrder() })

    return {
      order, loading, paying, processing, querying, errorMessage, payType,
      showQRCode, qrCodeImage, showMockModal, paymentResult,
      showPayButton, statusText, formatTime,
      handlePay, queryPayResult, refreshQRCode, cancelPay,
      closeModal, confirmMockPayment, goBack
    }
  }
}
</script>

<style scoped>
.container { max-width: 500px; margin: 0 auto; padding: 20px; }
.card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
h2 { margin: 0 0 20px 0; color: #333; }
.order-info { margin-bottom: 24px; }
.info-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.info-row:last-child { border-bottom: none; }
.label { color: #666; }
.value { color: #333; font-weight: 600; }
.value.amount { color: #e74c3c; font-size: 24px; }
.status.pending { color: #f39c12; }
.status.paid { color: #27ae60; }
.status.failed { color: #e74c3c; }
.pay-section { margin-top: 20px; }
.pay-type-selector { display: flex; gap: 12px; margin-bottom: 16px; }
.pay-type-btn { flex: 1; padding: 16px; border: 2px solid #e0e0e0; border-radius: 8px; background: white; cursor: pointer; transition: all 0.3s; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.pay-type-btn.active { border-color: #667eea; background: #f0f4ff; }
.pay-icon { font-size: 24px; }
.pay-btn { width: 100%; padding: 16px; background: linear-gradient(135deg, #27ae60 0%, #2ecc71 100%); color: white; border: none; border-radius: 8px; font-size: 18px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; text-align: center; }
.pay-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(39, 174, 96, 0.4); }
.pay-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.qr-section { text-align: center; padding: 20px; margin-top: 20px; background: #f9f9f9; border-radius: 12px; }
.qr-section h3 { margin: 0 0 16px 0; color: #333; }
.qr-container { display: flex; justify-content: center; margin: 16px 0; }
.qr-image { width: 250px; height: 250px; border: 2px solid #eee; border-radius: 8px; }
.qr-tip { color: #666; font-size: 14px; margin: 8px 0; }
.amount { font-size: 28px; font-weight: bold; color: #e74c3c; margin: 16px 0; }
.qr-actions { display: flex; gap: 12px; justify-content: center; margin-top: 16px; flex-wrap: wrap; }
.check-btn { padding: 10px 24px; background: #27ae60; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; }
.check-btn:hover:not(:disabled) { background: #219a52; }
.check-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.refresh-btn, .cancel-btn { padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.refresh-btn { background: #667eea; color: white; }
.cancel-btn { background: #ddd; color: #666; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: white; border-radius: 12px; width: 90%; max-width: 400px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { margin: 0; color: #333; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #999; }
.modal-body { padding: 20px; }
.payment-info { background: #f9f9f9; padding: 16px; border-radius: 8px; margin-bottom: 16px; }
.payment-info p { margin: 8px 0; color: #555; }
.mock-result { text-align: center; }
.mock-result p { color: #666; margin-bottom: 16px; }
.confirm-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #27ae60 0%, #2ecc71 100%); color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; }
.result-section { margin-top: 20px; }
.result-card { padding: 24px; border-radius: 12px; text-align: center; }
.result-card.success { background: #d4edda; border: 1px solid #c3e6cb; }
.result-card.failed { background: #f8d7da; border: 1px solid #f5c6cb; }
.result-icon { width: 60px; height: 60px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; font-size: 32px; }
.result-card.success .result-icon { background: #27ae60; color: white; }
.result-card.failed .result-icon { background: #e74c3c; color: white; }
.result-card h3 { margin-bottom: 16px; color: #333; }
.result-card p { margin: 8px 0; color: #555; }
.result-actions { margin-top: 20px; }
.back-btn { padding: 12px 32px; background: #667eea; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.error-message { margin-top: 16px; padding: 12px; background: #f8d7da; border: 1px solid #f5c6cb; border-radius: 8px; color: #721c24; text-align: center; }
.loading-container { text-align: center; padding: 60px; }
.loading { width: 20px; height: 20px; border: 2px solid #fff; border-top-color: transparent; border-radius: 50%; animation: spin 0.8s linear infinite; flex-shrink: 0; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 60px; background: white; border-radius: 12px; }
.empty-state p { margin-bottom: 16px; color: #666; }
</style>
