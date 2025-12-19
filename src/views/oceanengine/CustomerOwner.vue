<template>
  <div class="oe-customer-owner">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="filters.crmCustomerName"
            placeholder="CRM客户名称"
            clearable
            size="small"
            class="filter-item"
            @keyup.enter.native="handleSearch"
            @clear="handleSearch" />
          <el-input
            v-model="filters.ownerName"
            placeholder="归属名称"
            clearable
            size="small"
            class="filter-item"
            @keyup.enter.native="handleSearch"
            @clear="handleSearch" />
          <el-input
            v-model="filters.advertiserId"
            placeholder="广告主ID"
            clearable
            size="small"
            class="filter-item"
            @keyup.enter.native="handleSearch"
            @clear="handleSearch" />
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button size="small" icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </div>
        <div class="filter-actions">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate">新建归属</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div slot="header" class="card-header">
        <div>客户归属列表</div>
        <div class="actions">
          <el-button type="text" size="mini" icon="el-icon-refresh" @click="handleSearch">刷新</el-button>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        border
        size="mini">
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="ownerName" label="归属名称" min-width="140" />
        <el-table-column label="CRM客户" min-width="180">
          <template slot-scope="scope">
            {{ scope.row.crmCustomerName || scope.row.crmCustomerId || '-' }}
          </template>
        </el-table-column>
<!--        <el-table-column prop="channel" label="渠道" width="100">-->
<!--          <template slot-scope="scope">-->
<!--            {{ formatChannel(scope.row.channel) }}-->
<!--          </template>-->
<!--        </el-table-column>-->
        <el-table-column prop="advertiserCount" label="广告主数" width="110">
          <template slot-scope="scope">
            {{ scope.row.advertiserCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="openAdvertiserDrawer(scope.row)">管理广告主</el-button>
            <el-tooltip
              v-if="Number(scope.row.advertiserCount || 0) > 0"
              content="请先解绑广告主"
              placement="top">
              <span>
                <el-button type="text" size="mini" disabled>删除</el-button>
              </span>
            </el-tooltip>
            <el-button v-else type="text" size="mini" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          layout="prev, pager, next, total"
          :current-page.sync="pagination.page"
          :page-size="pagination.limit"
          :total="pagination.total"
          @current-change="handlePageChange" />
      </div>
    </el-card>

    <el-dialog
      :visible.sync="dialog.visible"
      :close-on-click-modal="false"
      width="520px"
      @close="handleDialogClose">
      <div slot="title">{{ dialog.form.id ? '编辑归属' : '新建归属' }}</div>
      <el-form
        ref="ownerForm"
        :model="dialog.form"
        :rules="dialog.rules"
        label-width="110px"
        size="small">
        <el-form-item label="归属名称/备注（可选）" prop="ownerName">
          <el-input v-model="dialog.form.ownerName" placeholder="可选，不填默认使用客户名" />
        </el-form-item>
        <el-form-item label="CRM客户" prop="crmCustomerId">
          <el-input :value="formatCrmCustomerDisplay(dialog.form)" readonly placeholder="请选择CRM客户">
            <el-button slot="append" icon="el-icon-search" @click="openCustomerPicker">选择客户</el-button>
          </el-input>
        </el-form-item>
<!--        <el-form-item label="渠道" prop="channel">-->
<!--          <el-input v-model="dialog.form.channel" disabled />-->
<!--        </el-form-item>-->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="dialog.loading" @click="handleSave">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="customerPicker.visible"
      :close-on-click-modal="false"
      width="860px"
      @close="handleCustomerPickerClose">
      <div slot="title">选择CRM客户</div>
      <div class="filter-bar compact">
        <div class="filter-left">
          <el-input
            v-model="customerPicker.filters.search"
            placeholder="客户名称/关键字"
            clearable
            size="small"
            class="filter-item"
            @keyup.enter.native="handleCustomerPickerSearch"
            @clear="handleCustomerPickerSearch" />
          <el-button size="small" type="primary" icon="el-icon-search" @click="handleCustomerPickerSearch">查询</el-button>
          <el-button size="small" icon="el-icon-refresh" @click="handleCustomerPickerReset">重置</el-button>
        </div>
      </div>
      <el-table
        v-loading="customerPicker.loading"
        :data="customerPicker.list"
        border
        size="mini"
        row-key="customerId">
        <el-table-column prop="customerId" label="客户ID" width="120" />
        <el-table-column prop="customerName" label="客户名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="是否绑定客户" width="120">
          <template slot-scope="scope">
            {{ scope.row && scope.row.canBindOeOwner === false ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template slot-scope="scope">
            <el-tooltip
              v-if="scope.row && scope.row.canBindOeOwner === false"
              content="该客户已绑定归属，不可选择"
              placement="top">
              <span>
                <el-button type="text" size="mini" disabled>选择</el-button>
              </span>
            </el-tooltip>
            <el-button v-else type="text" size="mini" @click="handlePickCustomer(scope.row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          layout="prev, pager, next, total"
          :current-page.sync="customerPicker.pagination.page"
          :page-size="customerPicker.pagination.limit"
          :total="customerPicker.pagination.total"
          @current-change="handleCustomerPickerPageChange" />
      </div>
    </el-dialog>

    <el-drawer
      :visible.sync="drawer.visible"
      :append-to-body="false"
      size="70%"
      :with-header="false"
      @close="handleDrawerClose">
      <div class="drawer-header">
        <div class="title">管理广告主</div>
        <div class="sub-title">{{ formatDrawerSubTitle() }}</div>
      </div>

      <div class="drawer-section">
        <div class="section-header">
          <span>已绑定广告主</span>
          <div class="actions">
            <el-button
              type="danger"
              size="mini"
              :disabled="!drawer.boundSelection.length"
              :loading="drawer.actionLoading"
              @click="handleBatchUnbind">批量解绑</el-button>
            <el-button type="text" size="mini" icon="el-icon-refresh" @click="loadOwnerAdvertisers">刷新</el-button>
          </div>
        </div>
        <el-table
          v-loading="drawer.boundLoading"
          :data="drawer.boundList"
          border
          size="mini"
          row-key="advertiserId"
          @selection-change="handleBoundSelectionChange">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="advertiserId" label="广告主ID" width="120" />
          <el-table-column prop="advertiserName" label="广告主" min-width="160" />
          <el-table-column prop="advCompanyName" label="公司" min-width="160" />
          <el-table-column prop="advertiserStatus" label="状态" width="120" />
          <el-table-column label="操作" width="90" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="handleSingleUnbind(scope.row)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-pagination
            layout="prev, pager, next, total"
            :current-page.sync="drawer.boundPagination.page"
            :page-size="drawer.boundPagination.limit"
            :total="drawer.boundPagination.total"
            @current-change="handleBoundPageChange" />
        </div>
      </div>

      <div class="drawer-section">
        <div class="section-header">
          <span>可绑定广告主</span>
          <div class="actions">
            <el-button
              type="primary"
              size="mini"
              :disabled="!drawer.availableSelection.length"
              :loading="drawer.actionLoading"
              @click="handleBind">绑定</el-button>
          </div>
        </div>
        <div class="filter-bar compact">
          <div class="filter-left">
            <el-input
              v-model="drawer.availableFilters.kw"
              placeholder="广告主名称关键字"
              clearable
              size="small"
              class="filter-item"
              @keyup.enter.native="handleAvailableSearch"
              @clear="handleAvailableSearch" />
            <el-input
              v-model="drawer.availableFilters.advertiserId"
              placeholder="广告主ID"
              clearable
              size="small"
              class="filter-item"
              @keyup.enter.native="handleAvailableSearch"
              @clear="handleAvailableSearch" />
            <el-input-number
              v-model="drawer.availableFilters.limit"
              :min="1"
              :max="500"
              size="small"
              class="filter-item narrow"
              @change="handleAvailableSearch" />
            <el-button size="small" type="primary" icon="el-icon-search" @click="handleAvailableSearch">查询</el-button>
            <el-button size="small" icon="el-icon-refresh" @click="resetAvailableFilters">重置</el-button>
          </div>
        </div>
        <el-table
          v-loading="drawer.availableLoading"
          :data="drawer.availableList"
          border
          size="mini"
          row-key="advertiserId"
          @selection-change="handleAvailableSelectionChange">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="advertiserId" label="广告主ID" width="120" />
          <el-table-column prop="advertiserName" label="广告主" min-width="160" />
          <el-table-column prop="advCompanyName" label="公司" min-width="160" />
          <el-table-column prop="advertiserStatus" label="状态" width="120" />
        </el-table>
        <div class="table-footer">
          <el-pagination
            layout="prev, pager, next, total"
            :current-page.sync="drawer.availablePagination.page"
            :page-size="drawer.availablePagination.limit"
            :total="drawer.availablePagination.total"
            @current-change="handleAvailablePageChange" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  fetchOwnerPage,
  saveOwner,
  deleteOwner,
  bindAdvertisers,
  unbindAdvertisers,
  fetchOwnerAdvertisers,
  fetchAvailableAdvertisers
} from '@/api/customerOwner'
import { crmCustomerIndexAPI } from '@/api/crm/customer'
import crmTypeModel from '@/views/crm/model/crmTypeModel'

export default {
  name: 'OceanengineCustomerOwner',
  data() {
    return {
      filters: {
        crmCustomerName: '',
        ownerName: '',
        advertiserId: ''
      },
      list: [],
      loading: false,
      pagination: {
        page: 1,
        limit: 10,
        total: 0
      },
      dialog: {
        visible: false,
        loading: false,
        form: {
          id: null,
          ownerName: '',
          crmCustomerId: '',
          crmCustomerName: '',
          channel: 'ADS'
        },
        rules: {
          crmCustomerId: [{ required: true, message: '请选择CRM客户', trigger: 'change' }]
        }
      },
      customerPicker: {
        visible: false,
        loading: false,
        list: [],
        filters: {
          search: ''
        },
        pagination: {
          page: 1,
          limit: 10,
          total: 0
        }
      },
      drawer: {
        visible: false,
        ownerId: null,
        ownerName: '',
        crmCustomerId: null,
        crmCustomerName: '',
        boundAllList: [],
        boundList: [],
        boundLoading: false,
        boundSelection: [],
        boundPagination: {
          page: 1,
          limit: 10,
          total: 0
        },
        availableAllList: [],
        availableList: [],
        availableLoading: false,
        availableSelection: [],
        availablePagination: {
          page: 1,
          limit: 10,
          total: 0
        },
        availableFilters: {
          kw: '',
          advertiserId: '',
          limit: 200
        },
        actionLoading: false
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      const params = {
        page: this.pagination.page,
        limit: this.pagination.limit,
        crmCustomerName: this.filters.crmCustomerName || undefined,
        ownerName: this.filters.ownerName || undefined,
        advertiserId: this.filters.advertiserId || undefined
      }
      return fetchOwnerPage(params).then(res => {
        const data = res.data || {}
        this.list = data.list || []
        this.pagination.total = data.totalRow || 0
      }).catch(() => {
        this.list = []
        this.pagination.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.pagination.page = 1
      this.fetchList()
    },
    handleReset() {
      this.filters = { crmCustomerName: '', ownerName: '', advertiserId: '' }
      this.pagination.page = 1
      this.fetchList()
    },
    handlePageChange(page) {
      this.pagination.page = page
      this.fetchList()
    },
    indexMethod(index) {
      return (this.pagination.page - 1) * this.pagination.limit + index + 1
    },
    formatChannel(channel) {
      return channel || 'ADS'
    },
    openCreate() {
      this.resetDialogForm()
      this.dialog.visible = true
    },
    openEdit(row) {
      this.resetDialogForm()
      this.dialog.form.id = row.id
      this.dialog.form.ownerName = row.ownerName || ''
      this.dialog.form.crmCustomerId = row.crmCustomerId || ''
      this.dialog.form.crmCustomerName = row.crmCustomerName || ''
      this.dialog.form.channel = 'ADS'
      this.dialog.visible = true
    },
    handleDelete(row) {
      if (!row || !row.id) return
      this.$confirm(
        `确认删除该客户归属吗？（CRM 客户：${row.crmCustomerName || row.crmCustomerId || '-'}，归属名称：${(row.ownerName || '').trim() || '默认归属'}）`,
        '提示',
        { type: 'warning' }
      ).then(() => {
        this.loading = true
        deleteOwner(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        }).finally(() => {
          this.loading = false
        })
      }).catch(() => {})
    },
    resetDialogForm() {
      this.dialog.form = {
        id: null,
        ownerName: '',
        crmCustomerId: '',
        crmCustomerName: '',
        channel: 'ADS'
      }
      if (this.$refs.ownerForm) {
        this.$refs.ownerForm.clearValidate()
      }
    },
    handleDialogClose() {
      const isEdit = !!this.dialog.form.id
      this.resetDialogForm()
      if (isEdit) {
        this.fetchList()
      }
    },
    handleSave() {
      this.$refs.ownerForm.validate(valid => {
        if (!valid) return
        const crmCustomerId = Number(this.dialog.form.crmCustomerId)
        if (isNaN(crmCustomerId) || crmCustomerId <= 0) {
          this.$message.error('请选择正确的CRM客户')
          return
        }
        const isCreate = !this.dialog.form.id
        this.dialog.loading = true
        const ownerName = (this.dialog.form.ownerName || '').trim()
        const payload = {
          id: this.dialog.form.id || undefined,
          ownerName: ownerName || undefined,
          crmCustomerId: crmCustomerId,
          channel: this.dialog.form.channel || 'ADS'
        }
        saveOwner(payload).then(res => {
          const ownerId = res && res.data
          this.$message.success('保存成功')
          this.dialog.visible = false

          if (isCreate) {
            this.fetchList().then(() => {
              const row = this.list.find(item => String(item.id) === String(ownerId))
              if (row) {
                this.openAdvertiserDrawer(row)
              }
            })
          } else {
            this.fetchList()
          }
        }).finally(() => {
          this.dialog.loading = false
        })
      })
    },
    openCustomerPicker() {
      this.customerPicker.visible = true
      this.customerPicker.pagination.page = 1
      this.loadCustomerPickerList()
    },
    handleCustomerPickerSearch() {
      this.customerPicker.pagination.page = 1
      this.loadCustomerPickerList()
    },
    handleCustomerPickerReset() {
      this.customerPicker.filters.search = ''
      this.customerPicker.pagination.page = 1
      this.loadCustomerPickerList()
    },
    handleCustomerPickerPageChange(page) {
      this.customerPicker.pagination.page = page
      this.loadCustomerPickerList()
    },
    handleCustomerPickerClose() {
      this.customerPicker.visible = false
    },
    loadCustomerPickerList() {
      this.customerPicker.loading = true
      const params = {
        page: this.customerPicker.pagination.page,
        limit: this.customerPicker.pagination.limit,
        search: this.customerPicker.filters.search || '',
        type: crmTypeModel.customer
      }
      crmCustomerIndexAPI(params).then(res => {
        const data = res.data || {}
        this.customerPicker.list = data.list || []
        this.customerPicker.pagination.total = data.totalRow || 0
      }).catch(() => {
        this.customerPicker.list = []
        this.customerPicker.pagination.total = 0
      }).finally(() => {
        this.customerPicker.loading = false
      })
    },
    handlePickCustomer(row) {
      if (row && row.canBindOeOwner === false) {
        this.$message.warning('该客户已绑定归属，不可选择')
        return
      }
      const customerId = this.getCrmCustomerId(row)
      if (!customerId) {
        this.$message.error('未获取到客户ID')
        return
      }
      this.dialog.form.crmCustomerId = customerId
      this.dialog.form.crmCustomerName = this.getCrmCustomerName(row)
      if (this.$refs.ownerForm) {
        this.$refs.ownerForm.clearValidate(['crmCustomerId'])
      }
      this.customerPicker.visible = false
    },
    openAdvertiserDrawer(row) {
      this.drawer.ownerId = row.id
      this.drawer.ownerName = row.ownerName || ''
      this.drawer.crmCustomerId = row.crmCustomerId || null
      this.drawer.crmCustomerName = row.crmCustomerName || ''
      this.drawer.visible = true
      this.drawer.boundSelection = []
      this.drawer.availableSelection = []
      this.drawer.boundPagination.page = 1
      this.drawer.availablePagination.page = 1
      this.loadOwnerAdvertisers()
      this.loadAvailableAdvertisers()
    },
    handleDrawerClose() {
      this.drawer.visible = false
      this.drawer.ownerId = null
      this.drawer.ownerName = ''
      this.drawer.crmCustomerId = null
      this.drawer.crmCustomerName = ''
      this.drawer.boundAllList = []
      this.drawer.boundList = []
      this.drawer.boundPagination = { page: 1, limit: 10, total: 0 }
      this.drawer.availableAllList = []
      this.drawer.availableList = []
      this.drawer.availablePagination = { page: 1, limit: 10, total: 0 }
      this.drawer.boundSelection = []
      this.drawer.availableSelection = []
      this.drawer.availableFilters = { kw: '', advertiserId: '', limit: 200 }
      this.drawer.actionLoading = false
      this.fetchList()
    },
    loadOwnerAdvertisers() {
      if (!this.drawer.ownerId) return
      this.drawer.boundLoading = true
      fetchOwnerAdvertisers(this.drawer.ownerId).then(res => {
        this.drawer.boundAllList = res.data || []
        this.drawer.boundPagination.total = this.drawer.boundAllList.length
        this.drawer.boundPagination.page = 1
        this.applyBoundPagination()
      }).catch(() => {
        this.drawer.boundAllList = []
        this.drawer.boundList = []
        this.drawer.boundPagination.total = 0
      }).finally(() => {
        this.drawer.boundLoading = false
      })
    },
    loadAvailableAdvertisers() {
      this.drawer.availableLoading = true
      const params = {
        kw: this.drawer.availableFilters.kw || undefined,
        advertiserId: this.drawer.availableFilters.advertiserId || undefined,
        limit: this.normalizeLimit(this.drawer.availableFilters.limit)
      }
      fetchAvailableAdvertisers(params).then(res => {
        this.drawer.availableAllList = res.data || []
        this.drawer.availablePagination.total = this.drawer.availableAllList.length
        this.drawer.availablePagination.page = 1
        this.applyAvailablePagination()
      }).catch(() => {
        this.drawer.availableAllList = []
        this.drawer.availableList = []
        this.drawer.availablePagination.total = 0
      }).finally(() => {
        this.drawer.availableLoading = false
      })
    },
    handleAvailableSearch() {
      this.drawer.availableFilters.limit = this.normalizeLimit(this.drawer.availableFilters.limit)
      this.loadAvailableAdvertisers()
    },
    resetAvailableFilters() {
      this.drawer.availableFilters = { kw: '', advertiserId: '', limit: 200 }
      this.loadAvailableAdvertisers()
    },
    handleBoundPageChange(page) {
      this.drawer.boundPagination.page = page
      this.drawer.boundSelection = []
      this.applyBoundPagination()
    },
    handleAvailablePageChange(page) {
      this.drawer.availablePagination.page = page
      this.drawer.availableSelection = []
      this.applyAvailablePagination()
    },
    applyBoundPagination() {
      const page = this.drawer.boundPagination.page || 1
      const limit = this.drawer.boundPagination.limit || 10
      const start = (page - 1) * limit
      const end = start + limit
      this.drawer.boundList = (this.drawer.boundAllList || []).slice(start, end)
    },
    applyAvailablePagination() {
      const page = this.drawer.availablePagination.page || 1
      const limit = this.drawer.availablePagination.limit || 10
      const start = (page - 1) * limit
      const end = start + limit
      this.drawer.availableList = (this.drawer.availableAllList || []).slice(start, end)
    },
    handleBoundSelectionChange(selection) {
      this.drawer.boundSelection = selection || []
    },
    handleAvailableSelectionChange(selection) {
      this.drawer.availableSelection = selection || []
    },
    handleBind() {
      if (!this.drawer.ownerId || !this.drawer.availableSelection.length) return
      const advertiserIds = this.drawer.availableSelection.map(item => item.advertiserId)
      this.executeBind(advertiserIds, false)
    },
    executeBind(advertiserIds, forceMove) {
      this.drawer.actionLoading = true
      bindAdvertisers({
        ownerId: this.drawer.ownerId,
        advertiserIds,
        forceMove: forceMove
      }).then(() => {
        this.$message.success(forceMove ? '迁移成功' : '绑定成功')
        this.drawer.availableSelection = []
        this.loadOwnerAdvertisers()
        this.loadAvailableAdvertisers()
      }).catch(err => {
        const conflictIds = this.parseConflictIds(err)
        if (conflictIds.length && !forceMove) {
          this.$confirm(
            `以下广告主已被其他归属绑定：${conflictIds.join(', ')}，是否强制迁移到当前归属？`,
            '提示',
            {
              confirmButtonText: '强制迁移',
              cancelButtonText: '取消',
              type: 'warning'
            }
          ).then(() => {
            this.executeBind(advertiserIds, true)
          }).catch(() => {})
        }
      }).finally(() => {
        this.drawer.actionLoading = false
      })
    },
    handleBatchUnbind() {
      if (!this.drawer.boundSelection.length) return
      const advertiserIds = this.drawer.boundSelection.map(item => item.advertiserId)
      this.confirmUnbind(advertiserIds)
    },
    handleSingleUnbind(row) {
      if (!row || !row.advertiserId) return
      this.confirmUnbind([row.advertiserId])
    },
    confirmUnbind(advertiserIds) {
      this.$confirm('确定解绑选中的广告主吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.drawer.actionLoading = true
        unbindAdvertisers({
          ownerId: this.drawer.ownerId,
          advertiserIds
        }).then(() => {
          this.$message.success('解绑成功')
          this.drawer.boundSelection = []
          this.loadOwnerAdvertisers()
          this.loadAvailableAdvertisers()
        }).finally(() => {
          this.drawer.actionLoading = false
        })
      }).catch(() => {})
    },
    normalizeLimit(value) {
      const num = Number(value)
      if (isNaN(num) || num <= 0) return 200
      return Math.min(Math.max(num, 1), 500)
    },
    parseConflictIds(err) {
      if (!err) return []
      if (err.data && Array.isArray(err.data.conflictAdvertiserIds)) {
        return err.data.conflictAdvertiserIds
      }
      const msg = err.msg || err.message || ''
      const matches = msg.match(/\d+/g)
      if (!matches) return []
      return Array.from(new Set(matches))
    },
    formatCrmCustomerDisplay(form) {
      if (!form) return ''
      const customerId = form.crmCustomerId ? String(form.crmCustomerId) : ''
      const customerName = (form.crmCustomerName || '').trim()
      if (customerName && customerId) return `${customerName}(ID:${customerId})`
      if (customerName) return customerName
      if (customerId) return `ID:${customerId}`
      return ''
    },
    formatDrawerSubTitle() {
      const ownerName = (this.drawer.ownerName || '').trim() || '默认归属'
      const customerId = this.drawer.crmCustomerId ? String(this.drawer.crmCustomerId) : '-'
      const customerName = (this.drawer.crmCustomerName || '').trim() || '-'
      return `CRM客户：${customerName}（ID:${customerId}） / 归属名称：${ownerName}`
    },
    getCrmCustomerId(row) {
      if (!row) return ''
      return row.customerId || row.crmCustomerId || row.id || ''
    },
    getCrmCustomerName(row) {
      if (!row) return ''
      return row.customerName || row.crmCustomerName || row.name || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.oe-customer-owner {
  .filter-card { margin-bottom: 16px; }
  .filter-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; }
  .filter-left { display: flex; align-items: center; flex-wrap: wrap; }
  .filter-item { margin-right: 12px; width: 160px; }
  .filter-item.narrow { width: 120px; }
  .filter-actions { margin-top: 6px; }
  .list-card { margin-bottom: 16px; }
  .card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
  .table-footer { margin-top: 8px; text-align: right; }
  .drawer-header { padding: 16px; border-bottom: 1px solid #ebeef5; }
  .drawer-header .title { font-size: 16px; font-weight: 600; }
  .drawer-header .sub-title { color: #909399; font-size: 12px; margin-top: 4px; }
  .drawer-section { padding: 16px; }
  .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-weight: 600; }
  .actions { display: flex; align-items: center; gap: 8px; }
  .filter-bar.compact { margin-bottom: 8px; }
}
</style>
