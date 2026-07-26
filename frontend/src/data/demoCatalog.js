export const demoProducts = [
  {
    id: 1001,
    title: 'MiniPods Pro 降噪耳机',
    description: '通勤、会议和运动都能使用的无线降噪耳机，支持快速配对和长续航。',
    mainImage: 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=900&q=80',
    categoryName: '数码音频',
    merchantName: 'MiniPay 数码旗舰店',
    status: 'ON_SALE',
    tags: ['降噪', '蓝牙 5.3', '现货']
  },
  {
    id: 1002,
    title: 'FlowKey 机械键盘',
    description: '紧凑配列机械键盘，适合开发、办公和游戏场景。',
    mainImage: 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=900&q=80',
    categoryName: '办公外设',
    merchantName: '键造实验室',
    status: 'ON_SALE',
    tags: ['热插拔', '三模连接', 'PBT 键帽']
  },
  {
    id: 1003,
    title: 'Pulse Watch 智能手表',
    description: '支持运动记录、消息提醒和健康数据查看的日常智能手表。',
    mainImage: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80',
    categoryName: '智能穿戴',
    merchantName: '未来生活馆',
    status: 'ON_SALE',
    tags: ['运动监测', '消息提醒', '防水']
  },
  {
    id: 1004,
    title: 'Orbit Desk 桌面充电站',
    description: '多设备桌面充电站，适合手机、耳机、手表同时充电。',
    mainImage: 'https://images.unsplash.com/photo-1616410011236-7a42121dd981?auto=format&fit=crop&w=900&q=80',
    categoryName: '充电配件',
    merchantName: 'MiniPay 配件仓',
    status: 'ON_SALE',
    tags: ['多口快充', '桌面收纳', 'Type-C']
  }
]

export const demoSkus = {
  1001: [
    { id: 20011, productId: 1001, skuName: '曜石黑 / 标准版', price: 399, originalPrice: 499, stock: 86, attributesJson: '{"颜色":"曜石黑","版本":"标准版"}' },
    { id: 20012, productId: 1001, skuName: '云雾白 / 长续航版', price: 459, originalPrice: 559, stock: 42, attributesJson: '{"颜色":"云雾白","版本":"长续航版"}' }
  ],
  1002: [
    { id: 20021, productId: 1002, skuName: '银灰 / 茶轴', price: 329, originalPrice: 399, stock: 55, attributesJson: '{"颜色":"银灰","轴体":"茶轴"}' },
    { id: 20022, productId: 1002, skuName: '墨绿 / 红轴', price: 349, originalPrice: 429, stock: 31, attributesJson: '{"颜色":"墨绿","轴体":"红轴"}' }
  ],
  1003: [
    { id: 20031, productId: 1003, skuName: '星光银 / 42mm', price: 699, originalPrice: 799, stock: 64, attributesJson: '{"颜色":"星光银","尺寸":"42mm"}' },
    { id: 20032, productId: 1003, skuName: '深空灰 / 46mm', price: 759, originalPrice: 899, stock: 28, attributesJson: '{"颜色":"深空灰","尺寸":"46mm"}' }
  ],
  1004: [
    { id: 20041, productId: 1004, skuName: '白色 / 65W', price: 189, originalPrice: 239, stock: 120, attributesJson: '{"颜色":"白色","功率":"65W"}' },
    { id: 20042, productId: 1004, skuName: '黑色 / 100W', price: 269, originalPrice: 329, stock: 76, attributesJson: '{"颜色":"黑色","功率":"100W"}' }
  ]
}

export function findDemoProduct(id) {
  return demoProducts.find(item => String(item.id) === String(id))
}

export function getDemoSkus(productId) {
  return demoSkus[productId] || []
}

export function money(value) {
  const number = Number(value || 0)
  return number.toFixed(2)
}
