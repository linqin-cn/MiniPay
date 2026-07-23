import axios from 'axios'

const api = axios.create({
  baseURL: '/api'
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const register = (data) => api.post('/users/register', data)
export const login = (data) => api.post('/users/login', data)
export const getCurrentUser = () => api.get('/users/me')
export const getAddresses = () => api.get('/users/addresses')
export const createAddress = (data) => api.post('/users/addresses', data)
export const updateAddress = (id, data) => api.put(`/users/addresses/${id}`, data)
export const deleteAddress = (id) => api.delete(`/users/addresses/${id}`)
export const setDefaultAddress = (id) => api.put(`/users/addresses/${id}/default`)

export const getProducts = (params) => api.get('/products', { params })
export const getProduct = (id) => api.get(`/products/${id}`)
export const getProductSkus = (id) => api.get(`/products/${id}/skus`)
export const createProduct = (data) => api.post('/products', data)
export const updateProduct = (id, data) => api.put(`/products/${id}`, data)
export const onSaleProduct = (id) => api.put(`/products/${id}/on-sale`)
export const offSaleProduct = (id) => api.put(`/products/${id}/off-sale`)

export const getCart = () => api.get('/cart')
export const addCartItem = (data) => api.post('/cart/items', data)
export const updateCartItem = (id, data) => api.put(`/cart/items/${id}`, data)
export const deleteCartItem = (id) => api.delete(`/cart/items/${id}`)
export const updateCartItemSelected = (id, selected) => api.put(`/cart/items/${id}/selected`, { selected })
export const deleteSelectedCartItems = () => api.delete('/cart/selected')

export const getInventory = (skuId) => api.get(`/inventory/skus/${skuId}`)
export const lockInventory = (data) => api.post('/inventory/lock', data)
export const deductInventory = (data) => api.post('/inventory/deduct', data)
export const releaseInventory = (data) => api.post('/inventory/release', data)

export const createOrder = (data) => api.post('/orders', data)
export const createTradeOrder = (data) => api.post('/orders/trade', data)
export const confirmOrder = (data) => api.post('/orders/confirm', data)
export const getOrder = (orderId) => api.get(`/orders/${orderId}`)
export const getTradeOrder = (orderNo) => api.get(`/orders/trade/${orderNo}`)
export const getOrderList = () => api.get('/orders')
export const updateOrderStatus = (orderId, status) => api.put(`/orders/${orderId}/status`, { status })
export const cancelOrder = (orderNo) => api.post(`/orders/${orderNo}/cancel`)
export const markOrderPaid = (orderNo) => api.post(`/orders/${orderNo}/paid`)
export const shipOrder = (orderNo) => api.post(`/orders/${orderNo}/ship`)
export const receiveOrder = (orderNo) => api.post(`/orders/${orderNo}/receive`)

export const createPayment = (data) => api.post('/payments', data)
export const createPaymentOrder = (data) => api.post('/payments/orders', data)
export const getPayment = (orderId) => api.get(`/payments/${orderId}`)
export const getPaymentOrder = (paymentNo) => api.get(`/payments/payment-orders/${paymentNo}`)
export const getPaymentOrderByOrderNo = (orderNo) => api.get(`/payments/orders/${orderNo}`)
export const queryPaymentStatus = (orderId) => api.get(`/payments/${orderId}/status`)
export const mockPaymentCallback = (data) => api.post('/payments/callback/mock', data)
export const closePaymentOrder = (paymentNo) => api.post(`/payments/${paymentNo}/close`)
export const refundPayment = (paymentNo, data) => api.post(`/payments/${paymentNo}/refund`, data)

export const getCoupons = () => api.get('/promotions/coupons')
export const receiveCoupon = (couponId) => api.post(`/promotions/coupons/${couponId}/receive`)
export const calculatePromotion = (data) => api.post('/promotions/calculate', data)

export const createLogistics = (data) => api.post('/logistics', data)
export const getLogisticsByOrderNo = (orderNo) => api.get(`/logistics/orders/${orderNo}`)
export const getLogisticsTrace = (logisticsNo) => api.get(`/logistics/${logisticsNo}/trace`)

export const registerMerchant = (data) => api.post('/merchants/register', data)
export const getMerchant = (id) => api.get(`/merchants/${id}`)
export const createShop = (data) => api.post('/merchants/shops', data)
export const getMerchantOrders = () => api.get('/merchants/orders')
export const merchantShipOrder = (orderNo) => api.post(`/merchants/orders/${orderNo}/ship`)
