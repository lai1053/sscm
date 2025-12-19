import request from '@/utils/request'

export function fetchOwnerPage(params) {
  return request({
    url: '/adminApi/qc/oe/owner/page',
    method: 'get',
    params
  })
}

export function saveOwner(data) {
  return request({
    url: '/adminApi/qc/oe/owner/save',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function bindAdvertisers(data) {
  return request({
    url: '/adminApi/qc/oe/owner/bindAdvertisers',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function unbindAdvertisers(data) {
  return request({
    url: '/adminApi/qc/oe/owner/unbindAdvertisers',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function fetchOwnerAdvertisers(ownerId) {
  return request({
    url: `/adminApi/qc/oe/owner/${ownerId}/advertisers`,
    method: 'get'
  })
}

export function fetchAvailableAdvertisers(params) {
  return request({
    url: '/adminApi/qc/oe/owner/availableAdvertisers',
    method: 'get',
    params
  })
}

// 删除客户归属
export function deleteOwner(ownerId) {
  return request({
    url: `/adminApi/qc/oe/owner/delete/${ownerId}`,
    method: 'post'
  })
}
