export const MODULE_REGISTRY = {
  crm: {
    key: 'crm',
    title: '客户管理',
    type: 1,
    module: 'crm',
    path: '/crm',
    icon: 'wk wk-customer',
    fontSize: '17px'
  },
  taskExamine: {
    key: 'taskExamine',
    title: '任务/审批',
    type: 4,
    module: 'taskExamine',
    path: '/taskExamine',
    icon: 'wk wk-office',
    fontSize: '16px'
  },
  log: {
    key: 'log',
    title: '日志',
    type: 3,
    module: 'log',
    path: '/workLog',
    icon: 'wk wk-log',
    fontSize: '17px'
  },
  book: {
    key: 'book',
    title: '通讯录',
    type: 6,
    module: 'book',
    path: '/addressBook',
    icon: 'wk wk-address-book',
    fontSize: '17px'
  },
  project: {
    key: 'project',
    title: '项目管理',
    type: 2,
    module: 'project',
    path: '/project',
    icon: 'wk wk-project',
    fontSize: '15px'
  },
  bi: {
    key: 'bi',
    title: '商业智能',
    type: 5,
    module: 'bi',
    path: '/bi',
    icon: 'wk wk-business-intelligence',
    fontSize: '18px'
  },
  oceanengine: {
    key: 'oceanengine',
    title: '巨量引擎',
    type: 12,
    module: 'oceanengine',
    path: '/oceanengine',
    icon: 'wk wk-monitor',
    fontSize: '17px'
  },
  calendar: {
    key: 'calendar',
    title: '日历',
    type: 8,
    module: 'calendar',
    path: '/calendar/index',
    icon: 'wk wk-calendar',
    fontSize: '20px'
  },
  hrm: {
    key: 'hrm',
    title: '人力资源',
    type: 11,
    module: 'hrm',
    path: '/hrm',
    icon: 'wk wk-employees',
    fontSize: '18px'
  }
}

export const DEFAULT_HEADER_ORDER = ['crm', 'taskExamine', 'log', 'book', 'project', 'bi', 'oceanengine', 'calendar', 'hrm']

export function buildModuleRegistry(filterFn) {
  const modules = {}
  Object.keys(MODULE_REGISTRY).forEach(key => {
    const config = MODULE_REGISTRY[key]
    if (!config) return
    if (!filterFn || filterFn(key, config)) {
      modules[key] = { ...config }
    }
  })
  return modules
}

export default MODULE_REGISTRY
