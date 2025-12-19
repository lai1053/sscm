<template>
  <div class="oe-customers">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-select v-model="filters.windowType" size="small" class="filter-item" @change="handleFilterChange">
            <el-option label="近7天" value="7" />
            <el-option label="近15天" value="15" />
            <el-option label="近30天" value="30" />
            <el-option label="自定义" value="custom" />
          </el-select>
          <el-input-number
            v-if="filters.windowType === 'custom'"
            v-model="filters.customWindow"
            :min="1"
            :max="120"
            :step="1"
            size="small"
            class="filter-item narrow"
            @change="handleCustomWindowChange" />
          <el-select v-model="filters.productLine" placeholder="产品线" size="small" class="filter-item" @change="handleFilterChange">
            <el-option v-for="item in productLineOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.saleUserId" placeholder="销售筛选" size="small" class="filter-item" @change="handleFilterChange">
            <el-option v-for="item in saleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-input
            v-model="filters.keyword"
            placeholder="公司/广告主关键词"
            clearable
            size="small"
            class="filter-item"
            @keyup.enter.native="handleFilterChange"
            @clear="handleFilterChange" />
          <el-select v-model="filters.riskLevel" placeholder="风险筛选" size="small" class="filter-item" @change="handleRiskChange">
            <el-option label="全部风险" value="ALL" />
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="预警" value="WARN" />
            <el-option label="危险" value="DANGER" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="handleFilterChange">刷新</el-button>
        </div>
        <div class="filter-tip">
          {{ windowLabel }} · {{ productLineLabel }}<span v-if="saleLabel"> · 销售：{{ saleLabel }}</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div slot="header" class="card-header">
        <div>客户列表</div>
        <div class="actions">
          <el-button type="text" size="mini" icon="el-icon-refresh" @click="handleFilterChange">刷新</el-button>
        </div>
      </div>
      <el-table
        v-loading="companyLoading"
        :data="pagedCompanies"
        row-key="_rowKey"
        :row-class-name="companyRowClass"
        border
        size="mini"
        highlight-current-row
        @row-click="handleCompanyRowClick">
        <el-table-column type="index" label="序号" width="60" :index="companyIndex" />
        <el-table-column prop="advCompanyName" label="公司" min-width="160" />
        <el-table-column prop="ownerName" label="客户归属" min-width="110" />
        <el-table-column label="产品线" width="90">
          <template slot-scope="scope">{{ displayProductLine(scope.row.channelCode || scope.row.channel, scope.row.accountSource) }}</template>
        </el-table-column>
        <el-table-column v-if="showAdsColumn(filters.productLine)" :label="adsLabel" width="100">
          <template slot-scope="scope">{{ formatAmount(adsDisplayCost(scope.row, filters.productLine)) }}</template>
        </el-table-column>
        <el-table-column label="本地推" width="100">
          <template slot-scope="scope">{{ formatAmount(scope.row.localCost) }}</template>
        </el-table-column>
        <el-table-column v-if="showQcColumn(filters.productLine)" label="千川消耗" width="100">
          <template slot-scope="scope">{{ formatAmount(qcDisplayCost(scope.row)) }}</template>
        </el-table-column>
        <el-table-column label="总消耗" width="110">
          <template slot-scope="scope">{{ formatAmount(formatTotalByProductLine(scope.row)) }}</template>
        </el-table-column>
        <el-table-column label="最近消耗" width="110">
          <template slot-scope="scope">{{ formatDate(scope.row.lastCostDate) }}</template>
        </el-table-column>
        <el-table-column label="不活跃天数" width="100">
          <template slot-scope="scope">{{ formatInactive(scope.row.inactiveDays) }}</template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template slot-scope="scope">
            <el-tag :type="riskTagType(scope.row.riskLevel)" size="mini">
              {{ riskLabel(scope.row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="趋势" width="80">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click.stop="openTrend(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          layout="prev, pager, next"
          :current-page.sync="pagination.companies.currentPage"
          :page-size="pagination.companies.pageSize"
          :total="filteredCompanies.length"
          @current-change="handleCompanyPageChange" />
      </div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div slot="header" class="card-header">
        <div>
          广告主明细
          <span v-if="selectedCompanyLabel" class="header-ctx">（公司：{{ selectedCompanyLabel }}）</span>
        </div>
        <div class="actions">
          <el-button type="text" size="mini" icon="el-icon-refresh" @click="loadAdvertisers">刷新</el-button>
        </div>
      </div>
      <el-table
        v-loading="advertiserLoading"
        :data="pagedAdvertisers"
        row-key="_rowKey"
        border
        size="mini">
        <el-table-column type="index" label="序号" width="60" :index="advertiserIndex" />
        <el-table-column prop="advertiserName" label="广告主" min-width="160" />
        <el-table-column label="产品线" width="90">
          <template slot-scope="scope">{{ displayProductLine(scope.row.channelCode || scope.row.channel, scope.row.accountSource) }}</template>
        </el-table-column>
        <el-table-column v-if="showAdsColumn(filters.productLine)" :label="adsLabel" width="100">
          <template slot-scope="scope">{{ formatAmount(adsDisplayCost(scope.row, filters.productLine)) }}</template>
        </el-table-column>
        <el-table-column label="本地推" width="100">
          <template slot-scope="scope">{{ formatAmount(scope.row.localCost) }}</template>
        </el-table-column>
        <el-table-column v-if="showQcColumn(filters.productLine)" label="千川消耗" width="100">
          <template slot-scope="scope">{{ formatAmount(qcDisplayCost(scope.row)) }}</template>
        </el-table-column>
        <el-table-column label="总消耗" width="110">
          <template slot-scope="scope">{{ formatAmount(formatTotalByProductLine(scope.row)) }}</template>
        </el-table-column>
        <el-table-column label="最近消耗" width="110">
          <template slot-scope="scope">{{ formatDate(scope.row.lastCostDate) }}</template>
        </el-table-column>
        <el-table-column label="不活跃天数" width="100">
          <template slot-scope="scope">{{ formatInactive(scope.row.inactiveDays) }}</template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template slot-scope="scope">
            <el-tag :type="riskTagType(scope.row.riskLevel)" size="mini">
              {{ riskLabel(scope.row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          layout="prev, pager, next"
          :current-page.sync="pagination.advertisers.currentPage"
          :page-size="pagination.advertisers.pageSize"
          :total="filteredAdvertisers.length"
          @current-change="handleAdvertiserPageChange" />
      </div>
    </el-card>

    <el-dialog
      :visible.sync="trendDialog.visible"
      :close-on-click-modal="false"
      width="900px"
      @close="handleTrendClose">
      <div slot="title" class="trend-title">
        {{ trendDialog.companyName || '客户趋势' }}
        <span class="header-ctx">（{{ productLineLabel }}）</span>
      </div>
      <div class="trend-toolbar">
        <el-radio-group v-model="trendDialog.windowType" size="mini" @change="handleTrendWindowChange">
          <el-radio-button label="7">近7天</el-radio-button>
          <el-radio-button label="15">近15天</el-radio-button>
          <el-radio-button label="30">近30天</el-radio-button>
          <el-radio-button label="custom">自定义</el-radio-button>
        </el-radio-group>
        <el-input-number
          v-if="trendDialog.windowType === 'custom'"
          v-model="trendDialog.customWindow"
          :min="1"
          :max="180"
          size="small"
          class="filter-item narrow"
          @change="handleTrendWindowChange" />
        <el-button size="mini" type="primary" icon="el-icon-refresh" @click="loadTrend">刷新趋势</el-button>
      </div>
      <div v-loading="trendDialog.loading" ref="trendChart" class="trend-chart"/>
      <el-empty
        v-if="!trendDialog.loading && (!trendDialog.data || trendDialog.data.length === 0)"
        :image-size="80"
        description="该时间段内无消耗"
        class="trend-empty" />
    </el-dialog>
  </div>
</template>

<script>
import echarts from 'echarts'
import {
  fetchCompanies,
  fetchAdvertisers,
  fetchCustomerTrend,
  fetchSales,
  fetchChildUserIds
} from '@/api/oceanengineDashboard'
import {
  PRODUCT_LINE_OPTIONS,
  mapProductLineToParams,
  productLineLabel,
  displayProductLine as mapDisplayProductLine,
  adsLabelByProductLine,
  showAdsColumn,
  showQcColumn,
  adsDisplayCost,
  qcDisplayCost,
  totalByProductLine
} from './productLineHelper'

export default {
  name: 'OceanengineCustomersList',
  data() {
    return {
      productLineOptions: PRODUCT_LINE_OPTIONS,
      filters: {
        windowType: '30',
        customWindow: 30,
        productLine: 'ALL',
        saleUserId: null,
        keyword: '',
        riskLevel: 'ALL'
      },
      companies: [],
      advertisers: [],
      companyLoading: false,
      advertiserLoading: false,
      allowedSaleUserIds: [],
      saleOptions: [],
      lastSaleOptionProductLine: null,
      selectedAdvCompanyId: null,
      pagination: {
        companies: { currentPage: 1, pageSize: 10 },
        advertisers: { currentPage: 1, pageSize: 10 }
      },
      trendDialog: {
        visible: false,
        loading: false,
        data: [],
        windowType: '30',
        customWindow: 30,
        advCompanyId: null,
        companyName: ''
      },
      trendChart: null
    }
  },
  computed: {
    isAdminUser() {
      const info = this.$store.getters.userInfo || {}
      return !!(info.isAdmin || info.is_admin || info.admin || info.superAdmin || info.isSuperAdmin || info.username === 'admin')
    },
    effectiveWindow() {
      const win = this.filters.windowType === 'custom'
        ? Number(this.filters.customWindow || 0)
        : Number(this.filters.windowType || 30)
      return win > 0 ? win : 30
    },
    windowLabel() {
      return `近${this.effectiveWindow}天`
    },
    productLineLabel() {
      return productLineLabel(this.filters.productLine)
    },
    adsLabel() {
      return adsLabelByProductLine(this.filters.productLine)
    },
    filteredCompanies() {
      return this.applyRiskFilter(this.applyKeywordFilter(this.companies, 'company'))
    },
    pagedCompanies() {
      return this.getPagedData(this.filteredCompanies, this.pagination.companies)
    },
    filteredAdvertisers() {
      return this.applyRiskFilter(this.applyKeywordFilter(this.advertisers, 'advertiser'))
    },
    pagedAdvertisers() {
      return this.getPagedData(this.filteredAdvertisers, this.pagination.advertisers)
    },
    selectedCompanyLabel() {
      const target = (this.companies || []).find(item => item.advCompanyId === this.selectedAdvCompanyId)
      return target ? (target.advCompanyName || '') : ''
    },
    saleLabel() {
      const found = this.saleOptions.find(item => item.value === this.filters.saleUserId)
      return found ? found.label : ''
    },
    currentUserId() {
      const info = this.$store.getters.userInfo
      return info && (info.userId || info.user_id || info.id)
    }
  },
  async created() {
    this.initFromQuery()
    await this.loadAllowedSaleUsers()
    this.normalizeSaleUser()
    await this.loadSaleOptions()
    this.handleFilterChange()
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    this.teardownChart()
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    initFromQuery() {
      const q = this.$route.query || {}
      if (q.window) {
        const w = Number(q.window)
        if (w === 7 || w === 15 || w === 30) this.filters.windowType = String(w)
        else if (!isNaN(w) && w > 0) {
          this.filters.windowType = 'custom'
          this.filters.customWindow = w
        }
      }
      if (q.productLine) this.filters.productLine = String(q.productLine).toUpperCase()
      if (q.saleUserId) this.filters.saleUserId = Number(q.saleUserId)
      if (q.keyword) this.filters.keyword = String(q.keyword)
      if (q.riskLevel) this.filters.riskLevel = String(q.riskLevel).toUpperCase()
    },
    async loadAllowedSaleUsers() {
      if (this.isAdminUser) {
        this.allowedSaleUserIds = null
        return
      }
      if (!this.currentUserId) {
        this.allowedSaleUserIds = []
        return
      }
      const ids = [Number(this.currentUserId)]
      try {
        const res = await fetchChildUserIds(this.currentUserId)
        const list = res.data || []
        list.forEach(id => ids.push(Number(id)))
      } catch (e) {
        // ignore
      }
      this.allowedSaleUserIds = Array.from(new Set(ids.filter(Boolean)))
    },
    normalizeSaleUser() {
      const allowed = this.allowedSaleUserIds
      const options = this.saleOptions || []
      if (!allowed || allowed.length === 0) {
        if (options.length && (!this.filters.saleUserId || !options.some(o => o.value === this.filters.saleUserId))) {
          this.filters.saleUserId = options[0].value
        } else if (!options.length && this.currentUserId) {
          this.filters.saleUserId = this.currentUserId
        }
        return
      }
      if (!this.filters.saleUserId || !allowed.includes(this.filters.saleUserId)) {
        this.filters.saleUserId = allowed[0]
      }
    },
    async loadSaleOptions() {
      if (this.lastSaleOptionProductLine === this.filters.productLine && this.saleOptions.length) {
        this.normalizeSaleUser()
        return
      }
      try {
        const res = await fetchSales({
          window: this.effectiveWindow,
          ...mapProductLineToParams(this.filters.productLine)
        })
        const data = res.data || {}
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        const map = new Map()
        list.forEach(item => {
          if (!this.allowedSaleUserIds || this.allowedSaleUserIds.includes(item.saleUserId)) {
            map.set(item.saleUserId, item.saleName || item.crmRealname || `ID ${item.saleUserId}`)
          }
        })
        if (this.allowedSaleUserIds) {
          this.allowedSaleUserIds.forEach(id => {
            if (!map.has(id)) map.set(id, `ID ${id}`)
          })
        }
        this.saleOptions = Array.from(map.entries()).map(([value, label]) => ({ value, label }))
      } catch (e) {
        if (this.allowedSaleUserIds) {
          this.saleOptions = this.allowedSaleUserIds.map(id => ({ value: id, label: `ID ${id}` }))
        } else {
          this.saleOptions = []
        }
      }
      this.lastSaleOptionProductLine = this.filters.productLine
      this.normalizeSaleUser()
    },
    handleCustomWindowChange() {
      if (!this.filters.customWindow || this.filters.customWindow < 1) {
        this.filters.customWindow = 30
      }
      this.handleFilterChange()
    },
    handleRiskChange() {
      this.pagination.companies.currentPage = 1
      this.pagination.advertisers.currentPage = 1
      const filtered = this.filteredCompanies
      const keep = filtered.find(item => item.advCompanyId === this.selectedAdvCompanyId)
      this.selectedAdvCompanyId = keep ? keep.advCompanyId : (filtered[0] && filtered[0].advCompanyId) || null
      this.loadAdvertisers()
      this.syncQuery()
    },
    async handleFilterChange() {
      this.pagination.companies.currentPage = 1
      this.pagination.advertisers.currentPage = 1
      this.selectedAdvCompanyId = null
      this.advertisers = []
      await this.loadSaleOptions()
      this.normalizeSaleUser()
      this.loadCompanies()
      if (this.trendDialog.visible && this.trendDialog.advCompanyId) {
        this.loadTrend()
      }
      this.syncQuery()
    },
    buildFilterParams(windowOverride) {
      const base = mapProductLineToParams(this.filters.productLine)
      const params = Object.assign({ window: windowOverride || this.effectiveWindow }, base)
      if (this.filters.saleUserId) params.saleUserId = this.filters.saleUserId
      if (this.filters.keyword) params.keyword = this.filters.keyword
      return params
    },
    buildCompanyParams() {
      return this.buildFilterParams()
    },
    buildAdvertiserParams() {
      const params = this.buildCompanyParams()
      if (this.selectedAdvCompanyId) {
        params.advCompanyId = this.selectedAdvCompanyId
      }
      return params
    },
    loadCompanies() {
      this.companies = []
      this.advertisers = []
      if (!this.filters.saleUserId) {
        this.companyLoading = false
        return
      }
      const params = this.buildCompanyParams()
      this.companyLoading = true
      fetchCompanies(params).then(res => {
        const data = res.data || {}
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        // advCompanyId 在当前页面维度可能存在重复（如不同来源/维度聚合），补充稳定行 key 避免 el-table duplicate keys 警告
        const normalized = (list || []).map((item, index) => {
          const companyId = item && item.advCompanyId ? String(item.advCompanyId) : 'unknown'
          const channel = item && (item.channelCode || item.channel) ? String(item.channelCode || item.channel) : ''
          const source = item && item.accountSource ? String(item.accountSource) : ''
          const baseKey = [companyId, channel, source].filter(Boolean).join('-') || companyId
          return { ...item, _rowKey: `${baseKey}-${index}` }
        })
        this.companies = normalized
        const filtered = this.applyRiskFilter(normalized)
        const keep = filtered.find(item => item.advCompanyId === this.selectedAdvCompanyId)
        this.selectedAdvCompanyId = keep ? keep.advCompanyId : (filtered[0] && filtered[0].advCompanyId) || null
        this.pagination.companies.currentPage = 1
        this.pagination.advertisers.currentPage = 1
        this.loadAdvertisers()
      }).catch(() => {
        this.companies = []
        this.selectedAdvCompanyId = null
        this.advertisers = []
      }).finally(() => {
        this.companyLoading = false
      })
    },
    loadAdvertisers() {
      this.advertisers = []
      if (!this.selectedAdvCompanyId) {
        this.advertiserLoading = false
        return
      }
      const params = this.buildAdvertiserParams()
      this.advertiserLoading = true
      fetchAdvertisers(params).then(res => {
        const data = res.data || {}
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        // advertiserId 在当前页面维度可能存在重复（如不同来源/维度聚合），补充稳定行 key 避免 el-table duplicate keys 警告
        this.advertisers = (list || []).map((item, index) => {
          const advertiserId = item && item.advertiserId ? String(item.advertiserId) : 'unknown'
          const channel = item && (item.channelCode || item.channel) ? String(item.channelCode || item.channel) : ''
          const source = item && item.accountSource ? String(item.accountSource) : ''
          const baseKey = [advertiserId, channel, source].filter(Boolean).join('-') || advertiserId
          return { ...item, _rowKey: `${baseKey}-${index}` }
        })
        this.pagination.advertisers.currentPage = 1
      }).catch(() => {
        this.advertisers = []
      }).finally(() => {
        this.advertiserLoading = false
      })
    },
    handleCompanyRowClick(row) {
      if (!row || !row.advCompanyId) return
      if (row.advCompanyId === this.selectedAdvCompanyId) return
      this.selectedAdvCompanyId = row.advCompanyId
      this.pagination.advertisers.currentPage = 1
      this.loadAdvertisers()
    },
    companyRowClass({ row }) {
      return row.advCompanyId === this.selectedAdvCompanyId ? 'is-selected-row' : ''
    },
    applyRiskFilter(list) {
      const target = (this.filters.riskLevel || 'ALL').toUpperCase()
      if (target === 'ALL') return list || []
      return (list || []).filter(item => (item.riskLevel || '').toUpperCase() === target)
    },
    applyKeywordFilter(list, type) {
      const keyword = String(this.filters.keyword || '').trim()
      if (!keyword) return list || []
      const lower = keyword.toLowerCase()
      const source = list || []
      if (type === 'advertiser') {
        return source.filter(item => {
          const name = (item.advertiserName || '').toLowerCase()
          const company = (item.advCompanyName || '').toLowerCase()
          return name.includes(lower) || company.includes(lower)
        })
      }
      return source.filter(item => {
        const name = (item.advCompanyName || '').toLowerCase()
        const owner = (item.ownerName || '').toLowerCase()
        return name.includes(lower) || owner.includes(lower)
      })
    },
    getPagedData(list, pager) {
      const page = (pager && pager.currentPage) || 1
      const size = (pager && pager.pageSize) || 10
      const start = (page - 1) * size
      return (list || []).slice(start, start + size)
    },
    companyIndex(index) {
      return (this.pagination.companies.currentPage - 1) * this.pagination.companies.pageSize + index + 1
    },
    advertiserIndex(index) {
      return (this.pagination.advertisers.currentPage - 1) * this.pagination.advertisers.pageSize + index + 1
    },
    showAdsColumn(productLine) {
      return showAdsColumn(productLine)
    },
    showQcColumn(productLine) {
      return showQcColumn(productLine)
    },
    adsDisplayCost(row, productLine) {
      return adsDisplayCost(row, productLine || this.filters.productLine)
    },
    qcDisplayCost(row) {
      return qcDisplayCost(row)
    },
    formatTotalByProductLine(row) {
      return totalByProductLine(row, this.filters.productLine)
    },
    displayProductLine(channelCode, accountSource) {
      return mapDisplayProductLine(channelCode, accountSource)
    },
    formatAccountSource(val) {
      const map = { ALL: '全部来源', AD: 'AD', LOCAL: 'LOCAL', STAR: 'STAR', LUBAN: '鲁班', DOMESTIC: 'DOMESTIC', QIANCHUAN: '千川' }
      return map[val] || (val || '-')
    },
    formatAmount(val) {
      if (val === null || val === undefined || val === '') return '-'
      const num = Number(val)
      if (isNaN(num)) return val
      return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    formatDate(val) {
      if (!val) return '-'
      const d = new Date(val)
      if (isNaN(d.getTime())) return '-'
      const y = d.getFullYear()
      const m = `${d.getMonth() + 1}`.padStart(2, '0')
      const day = `${d.getDate()}`.padStart(2, '0')
      return `${y}-${m}-${day}`
    },
    formatInactive(val) {
      if (val === null || val === undefined) return '-'
      const num = Number(val)
      return isNaN(num) ? '-' : `${num} 天未消耗`
    },
    riskLabel(val) {
      const level = (val || '').toUpperCase()
      if (level === 'ACTIVE') return '活跃'
      if (level === 'WARN') return '预警'
      return '危险'
    },
    riskTagType(val) {
      const level = (val || '').toUpperCase()
      if (level === 'ACTIVE') return 'success'
      if (level === 'WARN') return 'warning'
      return 'danger'
    },
    openTrend(row) {
      if (!row || !row.advCompanyId) return
      this.trendDialog.advCompanyId = row.advCompanyId
      this.trendDialog.companyName = row.advCompanyName || '客户趋势'
      this.trendDialog.windowType = this.filters.windowType
      this.trendDialog.customWindow = this.filters.customWindow
      this.trendDialog.visible = true
      this.$nextTick(() => {
        this.loadTrend()
      })
    },
    trendWindowValue() {
      const win = this.trendDialog.windowType === 'custom'
        ? Number(this.trendDialog.customWindow || 0)
        : Number(this.trendDialog.windowType || 30)
      return win > 0 ? win : 30
    },
    handleTrendWindowChange() {
      if (this.trendDialog.windowType === 'custom' && (!this.trendDialog.customWindow || this.trendDialog.customWindow < 1)) {
        this.trendDialog.customWindow = 30
      }
      if (this.trendDialog.visible) {
        this.loadTrend()
      }
    },
    loadTrend() {
      if (!this.trendDialog.advCompanyId) return
      this.trendDialog.loading = true
      const base = mapProductLineToParams(this.filters.productLine)
      fetchCustomerTrend(this.trendDialog.advCompanyId, {
        window: this.trendWindowValue(),
        channel: base.channel,
        accountSource: base.accountSource
      }).then(res => {
        this.trendDialog.data = res.data || []
        this.$nextTick(() => {
          this.renderTrendChart()
        })
      }).catch(() => {
        this.trendDialog.data = []
        this.renderTrendChart()
      }).finally(() => {
        this.trendDialog.loading = false
      })
    },
    renderTrendChart() {
      this.ensureTrendChart()
      if (!this.trendChart) return
      const data = this.trendDialog.data || []
      const dates = data.map(item => this.formatDate(item.statDate))
      const ads = data.map(item => adsDisplayCost(item, this.filters.productLine))
      const qc = data.map(item => qcDisplayCost(item))
      const total = data.map(item => totalByProductLine(item, this.filters.productLine))
      const legend = ['总消耗']
      const series = [
        { name: '总消耗', type: 'line', smooth: true, data: total, showSymbol: false }
      ]
      if (this.showAdsColumn(this.filters.productLine)) {
        legend.push(this.adsLabel)
        series.push({ name: this.adsLabel, type: 'line', smooth: true, data: ads, showSymbol: false })
      }
      if (this.showQcColumn(this.filters.productLine)) {
        legend.push('千川')
        series.push({ name: '千川', type: 'line', smooth: true, data: qc, showSymbol: false })
      }
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: legend },
        grid: { top: 30, left: 50, right: 20, bottom: 50 },
        xAxis: { type: 'category', boundaryGap: false, data: dates },
        yAxis: { type: 'value', name: '消耗(元)' },
        series
      }
      this.trendChart.setOption(option, true)
      this.trendChart.resize()
    },
    ensureTrendChart() {
      if (this.trendChart || !this.$refs.trendChart) return
      this.trendChart = echarts.init(this.$refs.trendChart)
    },
    handleTrendClose() {
      this.trendDialog.visible = false
      this.trendDialog.data = []
      this.trendDialog.advCompanyId = null
      this.teardownChart()
    },
    handleCompanyPageChange(page) {
      this.pagination.companies.currentPage = page
    },
    handleAdvertiserPageChange(page) {
      this.pagination.advertisers.currentPage = page
    },
    teardownChart() {
      if (this.trendChart) {
        this.trendChart.dispose()
        this.trendChart = null
      }
    },
    handleResize() {
      if (this.trendChart) {
        this.trendChart.resize()
      }
    },
    syncQuery() {
      const q = {
        window: this.effectiveWindow,
        productLine: this.filters.productLine
      }
      if (this.filters.saleUserId) q.saleUserId = this.filters.saleUserId
      if (this.filters.keyword) q.keyword = this.filters.keyword
      if (this.filters.riskLevel && this.filters.riskLevel !== 'ALL') q.riskLevel = this.filters.riskLevel
      this.$router.replace({ path: '/oceanengine/customers', query: q })
    }
  }
}
</script>

<style lang="scss" scoped>
.oe-customers {
  .filter-card { margin-bottom: 16px; }
  .filter-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; }
  .filter-left { display: flex; align-items: center; flex-wrap: wrap; }
  .filter-item { margin-right: 12px; width: 150px; }
  .filter-item.narrow { width: 120px; }
  .switch-item { width: auto; }
  .filter-tip { color: #909399; font-size: 12px; }
  .list-card { margin-bottom: 16px; }
  .card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
  .header-ctx { margin-left: 6px; color: #909399; font-size: 12px; font-weight: 400; }
  ::v-deep .is-selected-row { background: #f5f7fa; }
  .trend-title { font-weight: 600; color: #303133; }
  .trend-toolbar { display: flex; align-items: center; margin-bottom: 12px; }
  .trend-chart { height: 340px; }
  .trend-empty { margin-top: 12px; }
  .table-footer { margin-top: 8px; text-align: right; }
}
</style>
