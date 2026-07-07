/** 图表共享配色方案 */
export const CHART_COLORS = {
  /** 主色-营收 */
  primary: '#0f6fff',
  /** 信息-订单 */
  info: '#14a3ff',
  /** 警告-翻台率 */
  warning: '#f0a11a',
  /** 错误-占用 */
  error: '#163a70',
  /** 完整调色板(用于多系列图表) */
  palette: ['#0f6fff', '#21a67a', '#14a3ff', '#f0a11a', '#163a70', '#4f7cff', '#3ac2ff', '#6d7fa3'],

  /** 对应浅色背景 */
  bgPrimary: '#e9f2ff',
  bgInfo: '#e9f8ff',
  bgWarning: '#fff6df',
  bgError: '#eaf1ff'
} as const;
