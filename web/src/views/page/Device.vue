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
                    <a-select-option
                      v-for="item in stateItems"
                      :key="item.value"
                    >
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
            <a-button
              slot="footer"
              :loading="exportLoading"
              :disabled="true"
              @click=""
            >
              导出
            </a-button>
            <!-- deviceName部分保持不变 -->
            <template
              v-for="col in ['deviceName']"
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
                    <span v-else style="padding: 0 50px"
                      >&nbsp;&nbsp;&nbsp;</span
                    >
                  </a-tooltip>
                </span>
                <span v-else>{{ text }}</span>
              </div>
            </template>

            <!-- roleName 下拉框 -->
            <template slot="roleName" slot-scope="text, record">
              <a-select
                v-if="record.editable"
                style="margin: -5px 0; text-align: center; width: 100%"
                :value="record.roleId"
                @change="
                  (value) => handleSelectChange(value, record.deviceId, 'role')
                "
              >
                <a-select-option
                  v-for="item in roleItems"
                  :key="item.roleId"
                  :value="item.roleId"
                >
                  <div style="text-align: center">{{ item.roleName }}</div>
                </a-select-option>
              </a-select>
              <span
                v-else-if="editingKey === ''"
                @click="edit(record.deviceId)"
                style="cursor: pointer"
              >
                <a-tooltip
                  :title="record.roleDesc"
                  :mouseEnterDelay="1"
                  placement="right"
                >
                  <span v-if="text">{{ text }}</span>
                  <span v-else style="padding: 0 50px">&nbsp;&nbsp;&nbsp;</span>
                </a-tooltip>
              </span>
              <span v-else>{{ text }}</span>
            </template>

            <!-- modelName 下拉框 -->
            <template slot="modelName" slot-scope="text, record">
              <a-select
                v-if="record.editable"
                style="margin: -5px 0; text-align: center; width: 100%"
                :value="record.modelId"
                @change="
                  (value) => handleSelectChange(value, record.deviceId, 'model')
                "
              >
                <a-select-option
                  v-for="item in modelItems"
                  :key="item.modelId"
                  :value="item.modelId"
                >
                  <div style="text-align: center">{{ item.modelName }}</div>
                </a-select-option>
              </a-select>
              <span
                v-else-if="editingKey === ''"
                @click="edit(record.deviceId)"
                style="cursor: pointer"
              >
                <a-tooltip :title="record.modelDesc" :mouseEnterDelay="0.5">
                  <span v-if="record.modelId">{{
                    getItemName(
                      modelItems,
                      "modelId",
                      record.modelId,
                      "modelName"
                    )
                  }}</span>
                  <span v-else style="padding: 0 50px">&nbsp;&nbsp;&nbsp;</span>
                </a-tooltip>
              </span>
              <span v-else>{{
                getItemName(modelItems, "modelId", record.modelId, "modelName")
              }}</span>
            </template>

            <!-- sttName 下拉框 -->
            <template slot="sttName" slot-scope="text, record">
              <a-select
                v-if="record.editable"
                style="margin: -5px 0; text-align: center; width: 100%"
                :value="record.sttId"
                @change="
                  (value) => handleSelectChange(value, record.deviceId, 'stt')
                "
              >
                <a-select-option
                  v-for="item in sttItems"
                  :key="item.sttId"
                  :value="item.sttId"
                >
                  <div style="text-align: center">{{ item.sttName }}</div>
                </a-select-option>
              </a-select>
              <span
                v-else-if="editingKey === ''"
                @click="edit(record.deviceId)"
                style="cursor: pointer"
              >
                <a-tooltip :title="record.sttDesc" :mouseEnterDelay="0.5">
                  <span v-if="record.sttId">{{
                    getItemName(sttItems, "sttId", record.sttId, "sttName")
                  }}</span>
                  <span v-else style="padding: 0 50px">Vosk本地识别</span>
                </a-tooltip>
              </span>
              <span v-else>{{
                getItemName(sttItems, "sttId", record.sttId, "sttName")
              }}</span>
            </template>

            <!-- 其他模板保持不变 -->
            <template slot="state" slot-scope="text">
              <a-tag color="green" v-if="text == 1">在线</a-tag>
              <a-tag color="red" v-else>离线</a-tag>
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
              <a-space v-else>
                <a href="javascript:" @click="edit(record.deviceId)">编辑</a>
                <a href="javascript:" @click="editWithDialog(record)">详情</a>
              </a-space>
            </template>
          </a-table>
        </a-card>
      </div>
    </a-layout-content>
    <a-back-top />

    <DeviceEditDialog @submit="update" @close="editVisible = false" :visible="editVisible" :current="currentDevice" :model-items="modelItems" :stt-items="sttItems" :role-items="roleItems"></DeviceEditDialog>
  </a-layout>
</template>

<script>
import axios from "@/services/axios";
import api from "@/services/api";
import mixin from "@/mixins/index";
import { message } from "ant-design-vue";
import DeviceEditDialog from "@/components/DeviceEditDialog.vue";
export default {
  components: {DeviceEditDialog},
  mixins: [mixin],
  data() {
    return {
      // 查询框
      editVisible: false,
      currentDevice:{},
      query: {
        state: "",
      },
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
          title: "设备编号",
          dataIndex: "deviceId",
          scopedSlots: { customRender: "deviceId" },
          width: 160,
          fixed: "left",
          align: "center",
        },
        {
          title: "设备名称",
          dataIndex: "deviceName",
          scopedSlots: { customRender: "deviceName" },
          width: 100,
          align: "center",
        },
        {
          title: "设备角色",
          dataIndex: "roleName",
          scopedSlots: { customRender: "roleName" },
          width: 100,
          align: "center",
        },
        {
          title: "模型",
          dataIndex: "modelName",
          scopedSlots: { customRender: "modelName" },
          width: 150,
          align: "center",
        },
        {
          title: "语音识别",
          dataIndex: "sttName",
          scopedSlots: { customRender: "sttName" },
          width: 150,
          align: "center",
        },
        {
          title: "WIFI名称",
          dataIndex: "wifiName",
          scopedSlots: { customRender: "wifiName" },
          width: 100,
          align: "center",
          ellipsis: true,
        },
        {
          title: "IP地址",
          dataIndex: "ip",
          scopedSlots: { customRender: "ip" },
          width: 180,
          align: "center",
          ellipsis: true,
        },
        {
          title: "设备状态",
          dataIndex: "state",
          scopedSlots: { customRender: "state" },
          width: 100,
          align: "center",
        },
        {
          title: "产品类型",
          dataIndex: "chipModelName",
          width: 100,
          align: "center",
        },
        {
          title: "版本号",
          dataIndex: "version",
          width: 100,
          align: "center",
        },
        {
          title: "活跃时间",
          dataIndex: "lastLogin",
          scopedSlots: { customRender: "lastLogin" },
          width: 180,
          align: "center",
        },
        {
          title: "创建时间",
          dataIndex: "createTime",
          scopedSlots: { customRender: "createTime" },
          width: 180,
          align: "center",
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
      roleItems: [],
      modelItems: [],
      ttsItems: [],
      sttItems: [],
      data: [],
      cacheData: [],
      // 操作单元格是否可编辑
      editingKey: "",
      // 审核链接
      verifyCode: "",
    };
  },
  mounted() {
    this.getData();
    this.getRole();
    this.getConfig();
  },
  methods: {
    /* 查询参数列表 */
    getData() {
      this.loading = true;
      this.editingKey = "";
      axios
        .get({
          url: api.device.query,
          data: {
            start: this.pagination.page,
            limit: this.pagination.pageSize,
            ...this.query,
          },
        })
        .then((res) => {
          if (res.code === 200) {
            this.data = res.data.list.map((item) => {
              item.sttId = item.sttId || -1;
              return item;
            });
            this.cacheData = this.data.map((item) => ({ ...item }));
            this.pagination.total = res.data.total;
          } else {
            this.$message.error(res.message);
          }
        })
        .catch(() => {
          this.$message.error("服务器维护/重启中，请稍后再试");
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 添加设备
    addDevice(value, event) {
      if (value === "") {
        this.$message.info("请输入设备编号");
        return;
      }
      axios
        .post({
          url: api.device.add,
          data: {
            code: value,
          },
        })
        .then((res) => {
          if (res.code === 200) {
            this.getData();
          } else {
            this.$message.error(res.message);
          }
        })
        .catch(() => {
          this.$message.error("服务器维护/重启中，请稍后再试");
        });
    },
    // 选择变更处理函数
    handleSelectChange(value, key, type) {
      // 根据类型确定要使用的数据源和字段名
      let items, idField, nameField;

      if (type === "role") {
        items = this.roleItems;
        idField = "roleId";
        nameField = "roleName";
      } else if (type === "model") {
        items = this.modelItems;
        idField = "modelId";
        nameField = "modelName";
      } else if (type === "stt") {
        items = this.sttItems;
        idField = "sttId";
        nameField = "sttName";
      } else {
        return; // 不支持的类型，直接返回
      }

      // 查找对应的项
      const item = items.find((item) => item[idField] === value);
      const name = item ? item[nameField] : "";

      // 更新数据
      const data = this.editLine(key);
      data.target[idField] = value;
      data.target[nameField] = name;
      this.data = [...this.data]; // 强制更新视图
    },
    // 更新设备消息
    update(val, key) {
      if (key) {
        this.loading = true;
        delete val.editable;
      }
      axios
        .post({
          url: api.device.update,
          data: {
            deviceId: val.deviceId,
            deviceName: val.deviceName,
            modelId: val.modelId,
            sttId: val.sttId,
            ttsId: val.ttsId,
            roleId: val.roleId,
          },
        })
        .then((res) => {
          if (res.code === 200) {
            this.getData()
            this.editVisible = false
            message.success("修改成功");
          } else {
            this.$message.error(res.message);
          }
        })
        .catch(() => {
          message.error("服务器维护/重启中,请稍后再试");
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 获取角色列表
    getRole() {
      axios
        .get({
          url: api.role.query,
          data: {},
        })
        .then((res) => {
          if (res.code === 200) {
            this.roleItems = res.data.list;
          } else {
            this.$message.error(res.message);
          }
        })
        .catch(() => {
          this.$message.error("服务器维护/重启中，请稍后再试");
        });
    },
    // 获取模型列表
    getConfig() {
      axios
        .get({
          url: api.config.query,
          data: {},
        })
        .then((res) => {
          if (res.code === 200) {
            this.sttItems.push({
              sttId: -1,
              sttName: "Vosk本地识别",
              sttDesc: "默认Vosk本地语音识别模型",
            });
            res.data.list.forEach((item) => {
              if (item.configType == "llm") {
                item.modelId = item.configId;
                item.modelName = item.configName;
                item.modelDesc = item.configDesc;
                this.modelItems.push(item);
              } else if (item.configType == "stt") {
                item.sttId = item.configId;
                item.sttName = item.configName;
                item.sttDesc = item.configDesc;
                this.sttItems.push(item);
              }
            });
          } else {
            this.$message.error(res.message);
          }
        })
        .catch(() => {
          this.$message.error("服务器维护/重启中，请稍后再试");
        });
    },

    // 获取项目名称的辅助方法
    getItemName(items, idField, id, nameField) {
      const item = items.find((item) => item[idField] === id);
      return item ? item[nameField] : "";
    },

    editWithDialog(device){
      this.editVisible = true
      this.currentDevice = device
    }
  },
};
</script>
<style scoped>
/* 使下拉框选项居中 */

/* 确保下拉框中的文本居中 */
>>> .ant-select-selection__rendered .ant-select-selection-selected-value {
  text-align: center !important;
  width: 100% !important;
}

/* 查询框中的下拉框保持默认对齐方式 */
>>> .table-search .ant-select-selection-selected-value {
  text-align: left !important;
}
</style>