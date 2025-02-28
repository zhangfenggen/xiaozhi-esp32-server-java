<template>
  <a-layout>
    <a-layout-content>
      <div class="layout-content-margin">
        <!-- 查询框 -->
        <div class="table-search">
          <a-form
            layout="horizontal"
            :colon="false"
            :labelCol="{ span: 7 }"
            :wrapperCol="{ span: 16 }"
          >
            <a-row class="filter-flex">
              <a-col
                :xxl="6"
                :xl="6"
                :lg="12"
                :md="12"
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
              <a-col :xxl="5" :xl="5" :lg="12" :md="12" :xs="24">
                <a-form-item label="对话日期">
                  <a-range-picker
                    :ranges="{
                      今天: [moment().startOf('day'), moment().endOf('day')],
                      本月: [moment().startOf('month'), moment().endOf('month')],
                    }"
                    :allowClear="false"
                    :style="{ width: 100 }"
                    v-model="timeRange"
                    format="MM-DD"
                    @change="getData()"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 表格数据 -->
        <a-card title="查询表格" :bodyStyle="{ padding: 0 }" :bordered="false">
          <a-table
            rowKey="deviceId"
            :columns="tableColumns"
            :data-source="data"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1200 }"
            size="middle"
          >
            <a-button
              slot="footer"
              :loading="exportLoading"
              :disabled="true"
              @click="exportExcel('message')"
            >
              导出
            </a-button>
            <template slot="operation" slot-scope="text, record">
              <span>
                <a
                  href="javascript:;"
                  @click="edit(record.messageId)"
                  >详情</a
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
import axios from "@/services/axios";
import api from "@/services/api";
import mixin from "@/mixins/index";

export default {
  mixins: [mixin],
  data() {
    return {
      // 查询框
      query: {
        deviceId: "",
      },
      deviceItem: [],
      queryFilter: [
        {
          label: "设备编号",
          value: "",
          index: "deviceId",
        },
        {
          label: "设备名称",
          value: "",
          index: "deviceName",
        },
        {
          label: "消息发送方",
          value: "",
          index: "sender",
        },
      ],
      // 表格数据
      tableColumns: [
        {
          title: "设备编号",
          dataIndex: "deviceId",
          width: 90,
          align: "center",
        },
        {
          title: "设备名称",
          dataIndex: "deviceName",
          width: 100,
          fixed: "left",
          align: "center",
        },
        {
          title: "消息发送方",
          dataIndex: "sender",
          align: "center",
        },
        {
          title: "语音",
          dataIndex: "filePath",
          align: "center",
          ellipsis: true,
        },
        {
          title: "对话时间",
          dataIndex: "createTime",
          scopedSlots: { customRender: "createTime" },
          width: 180,
          align: "center",
          ellipsis: true,
        },
        {
          title: "操作",
          dataIndex: "operation",
          scopedSlots: { customRender: "operation" },
          width: 110,
          align: "center",
          fixed: "right",
        },
      ],
      data: [],
    };
  },
  mounted() {
    this.getData();
  },
  methods: {
    /* 查询参数列表 */
    getData() {
      this.loading = true;
      axios
        .get({
          url: api.message.query,
          data: {
            start: this.pagination.page,
            limit: this.pagination.pageSize,
            ...this.query,
            startTime: this.moment(this.timeRange[0]).format("YYYY-MM-DD HH:mm:ss"),
            endTime: this.moment(this.timeRange[1]).format("YYYY-MM-DD HH:mm:ss"),
          },
        })
        .then((res) => {
          this.loading = false;
          if (res.code === 200) {
            this.data = res.data.list;
            this.pagination.total = res.data.total;
          } else {
            this.$message.error(res.message);
          }
        })
        .catch(() => {
          this.loading = false;
          this.$message.error("服务器维护/重启中,请稍后再试");
        });
    },
  },
};
</script>
