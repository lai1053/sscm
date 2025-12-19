/** 巨量引擎路由 */
import Layout from '@/views/layout/OceanengineLayout'

const layout = function(meta = {}, path = '/oceanengine') {
  return {
    path: path,
    component: Layout,
    meta: {
      requiresAuth: true,
      icon: 'performance',
      ...meta
    }
  }
}

export default [
  {
    ...layout({
      permissions: ['oceanengine'],
      title: '巨量引擎',
      icon: 'performance'
    }),
    redirect: '/oceanengine/sales-dashboard',
    children: [
      {
        path: 'sales-dashboard',
        name: 'OceanEngineSalesDashboard',
        component: () => import('@/views/oceanengine/SalesDashboard'),
        meta: {
          title: '销售仪表盘',
          icon: 'board'
        }
      },
      {
        path: 'customers',
        name: 'OceanEngineCustomerList',
        component: () => import('@/views/oceanengine/CustomersList'),
        meta: {
          title: '巨量客户管理',
          icon: 'customer'
        }
      },
      {
        path: 'customer-owner',
        name: 'OceanEngineCustomerOwner',
        component: () => import('@/views/oceanengine/CustomerOwner'),
        meta: {
          title: '客户归属',
          icon: 'customer',
          permissions: ['manage'],
          adminOnly: true
        }
      }
    ]
  }
]
