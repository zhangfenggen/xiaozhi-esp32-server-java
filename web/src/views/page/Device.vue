<template>
  <a-layout>
    <a-layout-content>
      <div class="layout-content-margin">
        <!-- 查询框 -->
        <div class="table-search">
          <a-form
            layout="horizontal"
            :colon="false"
            :labelCol="{ span: 6 }"
            :wrapperCol="{ span: 16 }"
          >
            <a-row class="filter-flex">
              <a-col
                :xl="6"
                :lg="12"
                :xs="24"
                v-for="item in queryFilter"
                :key="item.index"
              >
                <a-form-item :label="item.label">
                  <a-input-search
                    v-model="query[item.index]"
                    placeholder="请输入"
                    allow-clear
                    @search="getData()"
                  />
                </a-form-item>
              </a-col>
              <a-col :xxl="6" :xl="6" :lg="12" :xs="24">
                <a-form-item label="状态">
                  <a-select v-model="query.state" @change="getData()">
                    <a-select-option v-for="item in stateItems" :key="item.key">
                      <span>{{ item.label }}</span>
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 表格数据 -->
        <a-card title="查询表格" :bodyStyle="{ padding: 0 }" :bordered="false">
          <a-input-search
            slot="extra"
            enter-button="添加设备"
            autoFocus
            placeholder="请输入设备码"
            @search="addDevice"
          />
          <a-table
            rowKey="deviceId"
            :columns="tableColumns"
            :data-source="data"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1200 }"
            size="middle"
          >
            <a-button slot="footer" :loading="exportLoading" :disabled="true" @click="">
              导出
            </a-button>
            <template
              v-for="col in ['deviceName', 'wifiName', 'wifiPassword']"
              :slot="col"
              slot-scope="text, record"
            >
              <div :key="col">
                <a-input
                  v-if="record.editable"
                  style="margin: -5px 0; text-align: center"
                  :value="text"
                  @change="
                    (e) => inputEdit(e.target.value, record.deviceId, col)
                  "
                  @keyup.enter="(e) => update(record, record.deviceId)"
                  @keyup.esc="(e) => cancel(record.deviceId)"
                />
                <span
                  v-else-if="editingKey === ''"
                  @click="edit(record.deviceId)"
                  style="cursor: pointer"
                >
                  <a-tooltip title="点击编辑" :mouseEnterDelay="0.5">
                    <span v-if="text">{{ text }}</span>
                    <span v-else style="padding: 0 50px">&nbsp;&nbsp;&nbsp;</span>
                  </a-tooltip>
                </span>
                <span v-else>{{ text }}</span>
              </div>
            </template>
            <template slot="operation" slot-scope="text, record">
              <a-space v-if="record.editable">
                <a-popconfirm
                  href="javascript:;"
                  title="确定保存？"
                  @confirm="update(record, record.deviceId)"
                >
                  <a>保存</a>
                </a-popconfirm>
                <a href="javascript:;" @click="cancel(record.deviceId)">取消</a>
              </a-space>
              <span v-else>
                <a
                  href="javascript:;"
                  :disabled="editingKey !== ''"
                  @click="edit(record.deviceId)"
                  >编辑</a
                >
              </span>
            </template>
          </a-table>
        </a-card>
      </div>
    </a-layout-content>
    <a-back-top />
  </a-layout>
</template>

<script>
import axios from '@/services/axios'
import api from '@/services/api'
import mixin from '@/mixins/index'

export default {
  mixins: [mixin],
  data () {
    return {
      // 查询框
      query: {
        state: ""
      },
      queryFilter: [
        {
          label: '设备编号',
          value: '',
          index: 'deviceId'
        },
        {
          label: '设备名称',
          value: '',
          index: 'deviceName'
        }
      ],
      stateItems: [
        {
          label: "全部",
          value: "",
          key: "",
        },
        {
          label: "在线",
          value: "1",
          key: "1",
        },
        {
          label: "离线",
          value: "0",
          key: "0",
        },
      ],
      // 表格数据
      tableColumns: [
        {
          title: '设备编号',
          dataIndex: 'deviceId',
          scopedSlots: { customRender: 'deviceId' },
          width: 200,
          fixed: 'left',
          align: 'center'
        },
        {
          title: '设备名称',
          dataIndex: 'deviceName',
          scopedSlots: { customRender: 'deviceName' },
          width: 100,
          align: 'center'
        },
        {
          title: 'WIFI名称',
          dataIndex: 'wifiName',
          scopedSlots: { customRender: 'wifiName' },
          align: 'center',
          ellipsis: true
        },
        {
          title: 'WIFI密码',
          dataIndex: 'wifiPassword',
          scopedSlots: { customRender: 'wifiPassword' },
          align: 'center'
        },
        {
          title: 'Mac地址',
          dataIndex: 'mac',
          scopedSlots: { customRender: 'mac' },
          align: 'center',
          ellipsis: true
        },
        {
          title: '设备状态',
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' },
          align: 'center'
        },
        {
          title: '创建时间',
          dataIndex: 'createTime',
          scopedSlots: { customRender: 'createTime' },
          align: 'center'
        },
        {
          title: '活跃时间',
          dataIndex: 'updateTime',
          scopedSlots: { customRender: 'updateTime' },
          align: 'center'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          scopedSlots: { customRender: 'operation' },
          width: 110,
          align: 'center',
          fixed: 'right'
        }
      ],
      data: [],
      cacheData: [],
      // 操作单元格是否可编辑
      editingKey: '',
      // 审核链接
      verifyCode: ''
    }
  },
  mounted () {
    this.getData()
  },
  methods: {
    /* 查询参数列表 */
    getData () {
      this.loading = true
      this.editingKey = ''
      axios
        .get({
          url: api.device.query,
          data: {
            start: this.pagination.page,
            limit: this.pagination.pageSize,
            ...this.query
          }
        })
        .then((res) => {
          this.loading = false
          if (res.code === 200) {
            this.data = res.data.list
            this.cacheData = this.data.map((item) => ({ ...item }))
            this.pagination.total = res.data.total
          } else {
            this.$message.error(res.message)
          }
        })
        .catch(() => {
          this.loading = false
          this.$message.error('服务器维护/重启中，请稍后再试')
        })
    },
    // 添加设备
    addDevice(value, event) {
      if (value === '') {
        this.$message.info('请输入设备编号')
        return
      }
      axios
        .post({
          url: api.device.add,
          data: {
            code: value
          }
        })
        .then((res) => {
          if (res.code === 200) {
            this.getData()
          } else {
            this.$message.error(res.message)
          }
        })
        .catch(() => {
          this.$message.error('服务器维护/重启中，请稍后再试')
        })
    }
  }
}
</script>
