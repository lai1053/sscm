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
          <el-select v-model="filters.channel" placeholder="渠道" size="small" class="filter-item" @change="handleFilterChange">
            <el-option label="全部渠道" value="ALL" />
            <el-option label="巨量ADS" value="ADS" />
            <el-option label="千川" value="QIANCHUAN" />
          </el-select>
          <el-select v-model="filters.accountSource" :disabled="filters.channel !== 'ADS'" placeholder="账户来源" size="small" class="filter-item" @change="handleFilterChange">
            <el-option label="全部来源" value="ALL" />
            <el-option label="AD" value="AD" />
            <el-option label="LOCAL" value="LOCAL" />
            <el-option label="STAR" value="STAR" />
            <el-option label="LUBAN" value="LUBAN" />
            <el-option label="DOMESTIC" value="DOMESTIC" />
          </el-select>
          <el-switch
            v-model="filters.onlySelf"
            active-text="只看自己"
            inactive-text="全部销售"
            class="filter-item switch-item"
            @change="handleFilterChange" />
          <el-select v-model="filters.riskLevel" placeholder="风险筛选" size="small" class="filter-item" @change="handleRiskChange">
            <el-option label="全部风险" value="ALL" />
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="预警" value="WARN" />
            <el-option label="危险" value="DANGER" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="handleFilterChange">刷新</el-button>
        </div>
        <div class="filter-tip">
          {{ windowLabel }} · {{ channelLabel }} · {{ accountSourceLabel }}
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
        :data="filteredCompanies"
        :row-class-name="companyRowClass"
        border
        size="mini"
        highlight-current-row
        @row-click="handleCompanyRowClick">
        <el-table-column prop="advCompanyName" label="公司" min-width="160" />
        <el-table-column prop="ownerName" label="客户归属" min-width="110" />
        <el-table-column label="ADS" width="90">
          <template slot-scope="scope">{{ formatAmount(scope.row.adsCost) }}</template>
        </el-table-column>
        <el-table-column label="千川" width="90">
          <template slot-scope="scope">{{ formatAmount(scope.row.qcCost) }}</template>
        </el-table-column>
        <el-table-column label="总消耗" width="100">
          <template slot-scope="scope">{{ formatAmount(scope.row.totalCost) }}</template>
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
        :data="filteredAdvertisers"
        border
        size="mini">
        <el-table-column prop="advertiserName" label="广告主" min-width="160" />
        <el-table-column prop="channel" label="渠道" width="80" />
        <el-table-column prop="accountSource" label="账户" width="80" />
        <el-table-column label="ADS" width="90">
          <template slot-scope="scope">{{ formatAmount(scope.row.adsCost) }}</template>
        </el-table-column>
        <el-table-column label="千川" width="90">
          <template slot-scope="scope">{{ formatAmount(scope.row.qcCost) }}</template>
        </el-table-column>
        <el-table-column label="总消耗" width="100">
          <template slot-scope="scope">{{ formatAmount(scope.row.totalCost) }}</template>
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
    </el-card>

    <el-dialog
      :visible.sync="trendDialog.visible"
      :close-on-click-modal="false"
      width="900px"
      @close="handleTrendClose">
      <div slot="title" class="trend-title">
        {{ trendDialog.companyName || '客户趋势' }}
        <span class="header-ctx">（{{ channelLabel }} · {{ accountSourceLabel }}）</span>
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
  fetchCustomerTrend
} from '@/api/oceanengineDashboard'

export default {
  name: 'OceanengineCustomersList',
  data() {
    return {
      filters: {
        windowType: '30',
        customWindow: 30,
        channel: 'ALL',
        accountSource: 'ALL',
        onlySelf: false,
        riskLevel: 'ALL'
      },
      companies: [],
      advertisers: [],
      companyLoading: false,
      advertiserLoading: false,
      selectedSaleUserId: null,
      selectedAdvCompanyId: null,
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
    effectiveWindow() {
      const win = this.filters.windowType === 'custom'
        ? Number(this.filters.customWindow || 0)
        : Number(this.filters.windowType || 30)
      return win > 0 ? win : 30
    },
    windowLabel() {
      return `近${this.effectiveWindow}天`
    },
    channelLabel() {
      return {
        ALL: '全部渠道',
        ADS: '巨量ADS',
        QIANCHUAN: '千川'
      }[this.filters.channel] || '全部渠道'
    },
    accountSourceLabel() {
      return this.formatAccountSource(this.filters.accountSource || 'ALL')
    },
    filteredCompanies() {
      return this.applyRiskFilter(this.companies)
    },
    filteredAdvertisers() {
      return this.applyRiskFilter(this.advertisers)
    },
    selectedCompanyLabel() {
      const target = (this.companies || []).find(item => item.advCompanyId === this.selectedAdvCompanyId)
      return target ? (target.advCompanyName || '') : ''
    },
    currentUserId() {
      const info = this.$store.getters.userInfo
      return info && (info.userId || info.user_id)
    }
  },
  created() {
    const q = this.$route.query || {}
    if (q.saleUserId) this.selectedSaleUserId = Number(q.saleUserId)
    if (q.channel) this.filters.channel = String(q.channel)
    if (q.accountSource) this.filters.accountSource = String(q.accountSource)
    if (q.onlySelf) this.filters.onlySelf = String(q.onlySelf) === 'true'
    if (q.window) {
      const w = Number(q.window)
      if (w === 7 || w === 15 || w === 30) this.filters.windowType = String(w)
      else {
        this.filters.windowType = 'custom'
        this.filters.customWindow = w
      }
    }
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
    handleCustomWindowChange() {
      if (!this.filters.customWindow || this.filters.customWindow < 1) {
        this.filters.customWindow = 30
      }
      this.handleFilterChange()
    },
    handleRiskChange() {},
    handleFilterChange() {
      this.selectedAdvCompanyId = null
      this.advertisers = []
      this.loadCompanies()
      if (this.trendDialog.visible && this.trendDialog.advCompanyId) {
        this.loadTrend()
      }
      this.syncQuery()
    },
    buildFilterParams(windowOverride) {
      const params = {
        window: windowOverride || this.effectiveWindow,
        channel: this.filters.channel,
        accountSource: this.filters.accountSource
      }
      if (this.filters.onlySelf && this.currentUserId) {
        params.saleUserId = this.currentUserId
      }
      if (this.selectedSaleUserId) {
        params.saleUserId = this.selectedSaleUserId
      }
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
      const params = this.buildCompanyParams()
      this.companyLoading = true
      fetchCompanies(params).then(res => {
        const data = res.data || {}
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        this.companies = list
        const filtered = this.applyRiskFilter(list)
        const keep = filtered.find(item => item.advCompanyId === this.selectedAdvCompanyId)
        this.selectedAdvCompanyId = keep ? keep.advCompanyId : (filtered[0] && filtered[0].advCompanyId) || null
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
        this.advertisers = list
      }).catch(() => {
        this.advertisers = []
      }).finally(() => {
        this.advertiserLoading = false
      })
    },
    handleCompanyRowClick(row) {
      if (!row || !row.advCompanyId) return
      if (row.advCompanyId === this.selectedAdvCompanyId) {
        this.openTrend(row)
        return
      }
      this.selectedAdvCompanyId = row.advCompanyId
      this.loadAdvertisers()
      this.openTrend(row)
    },
    companyRowClass({ row }) {
      return row.advCompanyId === this.selectedAdvCompanyId ? 'is-selected-row' : ''
    },
    applyRiskFilter(list) {
      const target = (this.filters.riskLevel || 'ALL').toUpperCase()
      if (target === 'ALL') return list || []
      return (list || []).filter(item => (item.riskLevel || '').toUpperCase() === target)
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
      fetchCustomerTrend(this.trendDialog.advCompanyId, {
        window: this.trendWindowValue(),
        channel: this.filters.channel,
        accountSource: this.filters.accountSource
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
      const ads = data.map(item => Number(item.adsCost || 0))
      const qc = data.map(item => Number(item.qcCost || 0))
      const total = data.map(item => Number(item.totalCost || 0))
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['总消耗', 'ADS', '千川'] },
        grid: { top: 30, left: 50, right: 20, bottom: 50 },
        xAxis: { type: 'category', boundaryGap: false, data: dates },
        yAxis: { type: 'value', name: '消耗(元)' },
        series: [
          { name: '总消耗', type: 'line', smooth: true, data: total, showSymbol: false },
          { name: 'ADS', type: 'line', smooth: true, data: ads, showSymbol: false },
          { name: '千川', type: 'line', smooth: true, data: qc, showSymbol: false }
        ]
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
        channel: this.filters.channel,
        accountSource: this.filters.accountSource,
        onlySelf: this.filters.onlySelf
      }
      if (this.selectedSaleUserId) q.saleUserId = this.selectedSaleUserId
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
}
</style>
