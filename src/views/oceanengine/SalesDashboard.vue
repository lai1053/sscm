<template>
  <div class="oe-dashboard">
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
          筛选作用于概览、下钻与趋势
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6">
        <el-card v-loading="overviewLoading" shadow="never" class="kpi-card">
          <div class="kpi-title">近 {{ overview.windowDays || effectiveWindow }} 天总消耗</div>
          <div class="kpi-value">{{ formatAmount(overview.totalCost) }}</div>
          <div class="kpi-meta">{{ formatRange(overview) || '数据准备中' }}</div>
          <div :class="`is-${changeType(overview.totalCost, overview.prevTotalCost)}`" class="kpi-change">
            对比上期：{{ formatRate(overview.totalCost, overview.prevTotalCost) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card v-loading="overviewLoading" shadow="never" class="kpi-card">
          <div class="kpi-title">客户健康</div>
          <div class="risk-row">
            <div class="risk-pill success">
              <span class="label">活跃</span>
              <span class="value">{{ overview.activeCompanyCount || 0 }}</span>
            </div>
            <div class="risk-pill warn">
              <span class="label">预警</span>
              <span class="value">{{ overview.warnCompanyCount || 0 }}</span>
            </div>
            <div class="risk-pill danger">
              <span class="label">危险</span>
              <span class="value">{{ overview.dangerCompanyCount || 0 }}</span>
            </div>
          </div>
          <div class="kpi-subtext">{{ channelLabel }} · {{ accountSourceLabel }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card v-loading="overviewLoading" shadow="never" class="kpi-card">
          <div class="kpi-title">客户 / 广告主数</div>
          <div class="kpi-value">{{ overview.totalCompanyCount || 0 }}</div>
          <div class="kpi-meta">公司数</div>
          <div class="kpi-subtext">广告主 {{ overview.totalAdvertiserCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card v-loading="overviewLoading" shadow="never" class="kpi-card">
          <div class="kpi-title">下滑客户</div>
          <div class="kpi-value">{{ overview.downCompanyCount || 0 }}</div>
          <div class="kpi-subtext">近 {{ overview.windowDays || effectiveWindow }} 天消耗下滑超过 10% 的客户数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="drill-card">
      <div slot="header" class="card-header">
        <div>
          <span>销售 → 公司 → 广告主</span>
          <span class="header-ctx">（{{ windowLabel }} · {{ channelLabel }} · {{ accountSourceLabel }}）</span>
        </div>
        <div class="actions">
          <el-button type="text" size="mini" @click="navigateToCustomers">客户列表</el-button>
          <el-button type="text" size="mini" icon="el-icon-refresh" @click="handleFilterChange">刷新</el-button>
        </div>
      </div>
      <el-row :gutter="12">
        <el-col :span="8">
          <div class="table-title">销售</div>
          <el-table
            v-loading="salesLoading"
            :data="sales"
            :row-class-name="saleRowClass"
            border
            size="mini"
            highlight-current-row
            @row-click="handleSaleRowClick">
            <el-table-column label="销售" min-width="150">
              <template slot-scope="scope">
                <div class="main-text">{{ scope.row.saleName || '-' }}</div>
                <div class="sub-text">{{ scope.row.crmRealname || '' }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="companyCount" label="公司" width="70" />
            <el-table-column prop="advertiserCount" label="广告主" width="80" />
            <el-table-column label="ADS" width="90">
              <template slot-scope="scope">{{ formatAmount(scope.row.adsCost) }}</template>
            </el-table-column>
            <el-table-column label="千川" width="90">
              <template slot-scope="scope">{{ formatAmount(scope.row.qcCost) }}</template>
            </el-table-column>
            <el-table-column label="总消耗" width="100">
              <template slot-scope="scope">{{ formatAmount(scope.row.totalCost) }}</template>
            </el-table-column>
          </el-table>
        </el-col>

        <el-col :span="8">
          <div class="table-title">
            公司
            <span v-if="selectedSaleLabel" class="header-ctx">（销售：{{ selectedSaleLabel }}）</span>
          </div>
          <el-table
            v-loading="companyLoading"
            :data="filteredCompanies"
            :row-class-name="companyRowClass"
            border
            size="mini"
            highlight-current-row
            @row-click="handleCompanyRowClick">
            <el-table-column prop="advCompanyName" label="公司" min-width="150" />
            <el-table-column prop="ownerName" label="客户归属" min-width="110">
              <template slot-scope="scope">{{ scope.row.ownerName || '-' }}</template>
            </el-table-column>
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
        </el-col>

        <el-col :span="8">
          <div class="table-title">
            广告主
            <span v-if="selectedCompanyLabel" class="header-ctx">（公司：{{ selectedCompanyLabel }}）</span>
          </div>
          <el-table
            v-loading="advertiserLoading"
            :data="filteredAdvertisers"
            border
            size="mini">
            <el-table-column prop="advertiserName" label="广告主" min-width="150" />
            <el-table-column prop="channel" label="渠道" width="80" />
            <el-table-column prop="accountSource" label="账户" width="80" />
            <el-table-column label="ADS" width="80">
              <template slot-scope="scope">{{ formatAmount(scope.row.adsCost) }}</template>
            </el-table-column>
            <el-table-column label="千川" width="80">
              <template slot-scope="scope">{{ formatAmount(scope.row.qcCost) }}</template>
            </el-table-column>
            <el-table-column label="总消耗" width="90">
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
        </el-col>
      </el-row>
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
  fetchOverview,
  fetchSales,
  fetchCompanies,
  fetchAdvertisers,
  fetchCustomerTrend
} from '@/api/oceanengineDashboard'

export default {
  name: 'OceanengineSalesDashboard',
  data() {
    return {
      filters: {
        windowType: '7',
        customWindow: 30,
        channel: 'ALL',
        accountSource: 'ALL',
        onlySelf: false,
        riskLevel: 'ALL'
      },
      overview: {},
      overviewLoading: false,
      sales: [],
      companies: [],
      advertisers: [],
      salesLoading: false,
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
        : Number(this.filters.windowType || 7)
      return win > 0 ? win : 7
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
    selectedSaleLabel() {
      const target = (this.sales || []).find(item => item.saleUserId === this.selectedSaleUserId)
      if (!target) return ''
      return target.saleName || target.crmRealname || ''
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
    navigateToCustomers() {
      const q = this.buildFilterParams()
      if (this.selectedSaleUserId) q.saleUserId = this.selectedSaleUserId
      this.$router.push({ path: '/oceanengine/customers', query: q })
    },
    handleCustomWindowChange() {
      if (!this.filters.customWindow || this.filters.customWindow < 1) {
        this.filters.customWindow = 7
      }
      this.handleFilterChange()
    },
    handleRiskChange() {
      // 仅本地过滤列表
    },
    handleFilterChange() {
      this.selectedSaleUserId = null
      this.selectedAdvCompanyId = null
      this.companies = []
      this.advertisers = []
      this.loadOverview()
      this.loadSales()
      if (this.trendDialog.visible && this.trendDialog.advCompanyId) {
        this.loadTrend()
      }
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
      return params
    },
    buildCompanyParams() {
      const params = this.buildFilterParams()
      if (this.selectedSaleUserId) {
        params.saleUserId = this.selectedSaleUserId
      }
      return params
    },
    buildAdvertiserParams() {
      const params = this.buildCompanyParams()
      if (this.selectedAdvCompanyId) {
        params.advCompanyId = this.selectedAdvCompanyId
      }
      return params
    },
    async loadOverview() {
      this.overviewLoading = true
      try {
        const res = await fetchOverview(this.buildFilterParams())
        this.overview = res.data || {}
      } catch (e) {
        this.overview = {}
        this.$message.error('概览数据加载失败，请稍后重试')
      } finally {
        this.overviewLoading = false
      }
    },
    loadSales() {
      this.salesLoading = true
      fetchSales(this.buildFilterParams()).then(res => {
        const data = res.data || {}
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        this.sales = list
        const keep = list.find(item => item.saleUserId === this.selectedSaleUserId)
        this.selectedSaleUserId = keep ? keep.saleUserId : (list[0] && list[0].saleUserId) || null
        this.loadCompanies()
      }).catch(() => {
        this.sales = []
        this.selectedSaleUserId = null
      }).finally(() => {
        this.salesLoading = false
      })
    },
    loadCompanies() {
      this.companies = []
      this.advertisers = []
      if (!this.selectedSaleUserId && !this.filters.onlySelf) {
        return
      }
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
    handleSaleRowClick(row) {
      if (!row || !row.saleUserId) return
      if (row.saleUserId === this.selectedSaleUserId) return
      this.selectedSaleUserId = row.saleUserId
      this.loadCompanies()
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
    saleRowClass({ row }) {
      return row.saleUserId === this.selectedSaleUserId ? 'is-selected-row' : ''
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
      const map = {
        ALL: '全部来源',
        AD: 'AD',
        LOCAL: 'LOCAL',
        STAR: 'STAR',
        LUBAN: '鲁班',
        DOMESTIC: 'DOMESTIC',
        QIANCHUAN: '千川'
      }
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
    formatRange(overview) {
      if (!overview || !overview.windowStart || !overview.windowEnd) return ''
      return `${this.formatDate(overview.windowStart)} ~ ${this.formatDate(overview.windowEnd)}`
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
    changeType(current, previous) {
      const change = this.calcChange(current, previous)
      return change.direction
    },
    formatRate(current, previous) {
      const change = this.calcChange(current, previous)
      return change.text
    },
    calcChange(current, previous) {
      const cur = Number(current || 0)
      const pre = Number(previous || 0)
      if (pre <= 0) {
        return { direction: 'flat', text: '-' }
      }
      const rate = ((cur - pre) / pre) * 100
      const text = `${rate >= 0 ? '+' : ''}${rate.toFixed(1)}%`
      return {
        direction: rate > 0 ? 'up' : rate < 0 ? 'down' : 'flat',
        text
      }
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
    }
  }
}
</script>

<style lang="scss" scoped>
.oe-dashboard {
  .filter-card {
    margin-bottom: 16px;
  }
  .filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
  }
  .filter-left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
  .filter-item {
    margin-right: 12px;
    width: 150px;
    &.narrow {
      width: 120px;
    }
  }
  .switch-item {
    width: auto;
  }
  .filter-tip {
    color: #909399;
    font-size: 12px;
  }
  .kpi-row {
    margin-bottom: 16px;
  }
  .kpi-card {
    min-height: 156px;
  }
  .kpi-title {
    font-weight: 600;
    color: #303133;
  }
  .kpi-meta {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
  }
  .kpi-value {
    margin-top: 12px;
    font-size: 26px;
    font-weight: 700;
    color: #303133;
  }
  .kpi-change {
    margin-top: 6px;
    font-size: 12px;
    &.is-up {
      color: #67c23a;
    }
    &.is-down {
      color: #f56c6c;
    }
    &.is-flat {
      color: #909399;
    }
  }
  .kpi-subtext {
    margin-top: 8px;
    color: #909399;
    font-size: 12px;
  }
  .risk-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
  }
  .risk-pill {
    flex: 1;
    margin-right: 8px;
    padding: 10px 12px;
    border-radius: 8px;
    background: #f5f7fa;
    text-align: center;
    &:last-child {
      margin-right: 0;
    }
    .label {
      display: block;
      color: #909399;
      font-size: 12px;
    }
    .value {
      display: block;
      margin-top: 4px;
      font-size: 18px;
      font-weight: 700;
      color: #303133;
    }
    &.success {
      background: #f0f9eb;
      .value {
        color: #67c23a;
      }
    }
    &.warn {
      background: #fdf6ec;
      .value {
        color: #e6a23c;
      }
    }
    &.danger {
      background: #fef0f0;
      .value {
        color: #f56c6c;
      }
    }
  }
  .drill-card {
    margin-bottom: 16px;
  }
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    .header-ctx {
      margin-left: 6px;
      color: #909399;
      font-size: 12px;
      font-weight: 400;
    }
  }
  .table-title {
    margin: 4px 0 8px;
    font-weight: 600;
    color: #303133;
    .header-ctx {
      margin-left: 6px;
      color: #909399;
      font-size: 12px;
    }
  }
  .actions {
    display: flex;
    align-items: center;
  }
  .main-text {
    font-weight: 600;
    color: #303133;
  }
  .sub-text {
    color: #909399;
    font-size: 12px;
  }
  ::v-deep .is-selected-row {
    background: #f5f7fa;
  }
  .trend-title {
    font-weight: 600;
    color: #303133;
    .header-ctx {
      color: #909399;
      font-size: 12px;
      font-weight: 400;
      margin-left: 6px;
    }
  }
  .trend-toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }
  .trend-chart {
    height: 340px;
  }
  .trend-empty {
    margin-top: 12px;
  }
}
</style>
