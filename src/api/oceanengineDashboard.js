import request from '@/utils/request'

export function fetchOverview(params) {
  return request({
    url: '/adminApi/qc/oe/dashboard/overview',
    method: 'get',
    params
  })
}

export function fetchSales(params) {
  return request({
    url: '/adminApi/qc/oe/dashboard/sales',
    method: 'get',
    params
  })
}

export function fetchCompanies(params) {
  return request({
    url: '/adminApi/qc/oe/dashboard/companies',
    method: 'get',
    params
  })
}

export function fetchDownCompanies(params) {
  return request({
    url: '/adminApi/qc/oe/dashboard/companies/down',
    method: 'get',
    params
  })
}

export function fetchAdvertisers(params) {
  return request({
    url: '/adminApi/qc/oe/dashboard/advertisers',
    method: 'get',
    params
  })
}

export function fetchCustomerTrend(advCompanyId, params) {
  return request({
    url: `/adminApi/qc/oe/dashboard/customer/${advCompanyId}/trend`,
    method: 'get',
    params
  })
}

export function fetchSaleTrend(saleUserId, params) {
  return request({
    url: `/adminApi/qc/oe/dashboard/sale/${saleUserId}/trend`,
    method: 'get',
    params
  })
}

export function fetchChildUserIds(userId) {
  return request({
    url: '/adminUser/queryChildUserId',
    method: 'get',
    params: { userId }
  })
}
