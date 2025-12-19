const PRODUCT_LINE_PARAM_MAP = {
  ALL: { channel: 'ALL', accountSource: 'ALL', label: '全部' },
  AD: { channel: 'OCEANENGINE_ADS', accountSource: 'AD', label: 'AD' },
  LOCAL: { channel: 'OCEANENGINE_ADS', accountSource: 'LOCAL', label: '本地推' },
  QIANCHUAN: { channel: 'OCEANENGINE_QIANCHUAN', accountSource: 'ALL', label: '千川' }
}

export const PRODUCT_LINE_OPTIONS = [
  { value: 'ALL', label: '全部产品线' },
  { value: 'AD', label: 'AD' },
  { value: 'LOCAL', label: '本地推' },
  { value: 'QIANCHUAN', label: '千川' }
]

export function mapProductLineToParams(productLine) {
  const target = PRODUCT_LINE_PARAM_MAP[productLine] || PRODUCT_LINE_PARAM_MAP.ALL
  return { channel: target.channel, accountSource: target.accountSource }
}

export function productLineLabel(productLine) {
  const target = PRODUCT_LINE_PARAM_MAP[productLine] || PRODUCT_LINE_PARAM_MAP.ALL
  return target.label
}

export function displayProductLine(channelCode, accountSource) {
  const channel = (channelCode || '').toUpperCase()
  const source = (accountSource || '').toUpperCase()
  if (channel.includes('QIANCHUAN')) return '千川'
  if (channel.includes('ADS') && source === 'LOCAL') return '本地推'
  if (channel.includes('ADS')) return 'AD'
  return '-'
}

export function adsLabelByProductLine(productLine) {
  if (productLine === 'LOCAL') return '本地推消耗'
  if (productLine === 'AD') return 'AD消耗'
  return 'AD消耗'
}

export function showAdsColumn(productLine) {
  return productLine !== 'QIANCHUAN'
}

export function showQcColumn(productLine) {
  return productLine !== 'AD' && productLine !== 'LOCAL'
}

export function adsDisplayCost(row, productLine) {
  const ad = Number(row && row.adCost) || 0
  const local = Number(row && row.localCost) || 0
  const adsFallback = Number(row && (row.adsCost || row.ads_cost)) || 0
  if (productLine === 'AD') return ad
  if (productLine === 'LOCAL') return local
  return ad || adsFallback
}

export function qcDisplayCost(row) {
  return Number(row && (row.qcCost || row.qianchuanCost)) || 0
}

export function totalByProductLine(row, productLine) {
  if (!row) return 0
  const ad = Number(row.adCost || 0)
  const local = Number(row.localCost || 0)
  const qc = qcDisplayCost(row)
  if (productLine === 'QIANCHUAN') return qc
  if (productLine === 'AD') return ad
  if (productLine === 'LOCAL') return local
  const total = Number(row.totalCost || 0)
  if (total) return total
  return ad + local + qc
}
