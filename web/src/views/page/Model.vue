<template>
  <a-layout>
    <a-layout-content>
      <div class="layout-content-margin">
        <!-- 查询框 -->
        <div class="table-search">
          <a-form layout="horizontal" :colon="false" :labelCol="{ span: 6 }" :wrapperCol="{ span: 16 }">
            <a-row class="filter-flex">
              <a-col :xxl="6" :xl="6" :lg="12" :xs="24">
                <a-form-item label="模型类别">
                  <a-select v-model="query.type" @change="getData()">
                    <a-select-option key="" value="">
                      <span>全部</span>
                    </a-select-option>
                    <a-select-option v-for="item in typeOptions" :key="item.value">
                      <span>{{ item.label }}</span>
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xl="6" :lg="12" :xs="24" v-for="item in queryFilter" :key="item.index">
                <a-form-item :label="item.label">
                  <a-input-search v-model="query[item.index]" placeholder="请输入" allow-clear @search="getData()" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 表格数据 -->
        <a-card :bodyStyle="{ padding: 0 }" :bordered="false">
          <a-tabs defaultActiveKey="1" :activeKey="activeTabKey" @change="handleTabChange"
            tabBarStyle="margin: 0 0 0 15px">
            <a-tab-pane key="1" tab="模型列表">
              <a-table :columns="columns" :dataSource="modelItems" :loading="loading" :pagination="pagination"
                rowKey="modelId" :scroll="{ x: 800 }" size="middle">
                <templace slot="modelDesc" slot-scope="text, record">
                  <a-tooltip :title="text" :mouseEnterDelay="0.5" placement="leftTop">
                    <span v-if="text">{{ text }}</span>
                    <span v-else style="padding: 0 50px">&nbsp;&nbsp;&nbsp;</span>
                  </a-tooltip>
                </templace>
                <template slot="operation" slot-scope="text, record">
                  <a-space>
                    <a @click="edit(record)">编辑</a>
                    <a-popconfirm title="确定要删除这个模型配置吗?" @confirm="update(record.modelId)">
                      <a>删除</a>
                    </a-popconfirm>
                  </a-space>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="2" tab="创建模型">
              <a-form layout="horizontal" :form="modelForm" :colon="false" @submit="handleSubmit"
                style="padding: 10px 24px">
                <a-row :gutter="20">
                  <a-col :xl="8" :lg="12" :xs="24">
                    <a-form-item label="模型类别">
                      <a-select v-decorator="[
                        'type',
                        { rules: [{ required: true, message: '请选择模型类别' }] }
                      ]" placeholder="请选择模型类别" @change="handleTypeChange">
                        <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">
                          {{ item.label }}
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                  <a-col :xl="16" :lg="12" :xs="24">
                    <a-form-item label="模型名称">
                      <a-input v-decorator="[
                        'modelName',
                        { rules: [{ required: true, message: '请输入模型名称' }] }
                      ]" autocomplete="off" placeholder="请输入模型名称" />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-form-item label="模型描述">
                  <a-textarea v-decorator="['modelDesc']" placeholder="请输入模型描述" :rows="4" />
                </a-form-item>

                <a-divider>参数配置</a-divider>
                <a-space direction="vertical" style="width: 100%">
                  <a-card v-if="currentType" size="small" :bodyStyle="{ 'background-color': '#fafafa' }"
                    :bordered="false">
                    <a-row :gutter="20">
                      <!-- 根据选择的模型类别动态显示参数配置 -->
                      <template v-for="field in currentTypeFields">
                        <a-col :key="field.name" :xl="field.span || 12" :lg="12" :xs="24">
                          <a-form-item :label="field.label" style="margin-bottom: 24px">
                            <a-input v-decorator="[
                              field.name,
                              { rules: [{ required: field.required, message: `请输入${field.label}` }] }
                            ]" :placeholder="`请输入${field.label}`" :type="field.inputType || 'text'" />
                          </a-form-item>
                        </a-col>
                      </template>
                    </a-row>
                  </a-card>
                  <a-card v-else :bodyStyle="{ 'background-color': '#fafafa' }" :bordered="false">
                    <a-empty description="请先选择模型类别" />
                  </a-card>

                  <a-form-item>
                    <a-button type="primary" html-type="submit">
                      {{ editingModelId ? '更新模型' : '创建模型' }}
                    </a-button>
                    <a-button style="margin-left: 8px" @click="resetForm">
                      取消
                    </a-button>
                  </a-form-item>
                </a-space>
              </a-form>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script>
import axios from '@/services/axios'
import api from '@/services/api'
import mixin from '@/mixins/index'

export default {
  mixins: [mixin],
  data() {
    return {
      // 查询框
      query: {
        type: "",
      },
      queryFilter: [
        {
          label: "模型名称",
          value: "",
          index: "modelName",
        },
      ],
      activeTabKey: '1', // 当前激活的标签页
      modelForm: this.$form.createForm(this, {
        // 监听表单值变化
        onValuesChange: (props, values) => {
          if (values.type && values.type !== this.currentType) {
            this.currentType = values.type;
          }
        }
      }),
      modelItems: [],
      editingModelId: null,
      currentType: '',
      // 模型类别选项
      typeOptions: [
        { label: 'OpenAI', value: 'openai', key: '0' },
        { label: 'Ollama', value: 'ollama', key: '1' },
        { label: 'Qwen', value: 'qwen', key: '2' },
        { label: 'Spark', value: 'spark', key: '3' }
      ],
      // 各类别对应的参数字段定义
      typeFields: {
        openai: [
          { name: 'apiKey', label: 'API Key', required: true, span: 12 },
          { name: 'apiUrl', label: 'API URL', required: false, span: 12 },
        ],
        ollama: [
          { name: 'apiUrl', label: 'API URL', required: false, span: 12 }
        ],
        qwen: [
          { name: 'apiKey', label: 'API Key', required: true, span: 12 }
        ],
        spark: [
          { name: 'appId', label: 'App ID', required: true, span: 8 },
          { name: 'apiKey', label: 'API Key', required: true, span: 8 },
          { name: 'apiSecret', label: 'API Secret', required: true, span: 8 },
          { name: 'apiUrl', label: 'API URL', required: false, span: 12 }
        ]
      },
      columns: [
        {
          title: '模型类别',
          dataIndex: 'type',
          key: 'type',
          width: 120,
          align: 'center',
          customRender: (text) => {
            const type = this.typeOptions.find(item => item.value === text);
            return type ? type.label : text;
          }
        },
        {
          title: '模型名称',
          dataIndex: 'modelName',
          key: 'modelName',
          width: 200,
          align: 'center'
        },
        {
          title: '描述',
          dataIndex: 'modelDesc',
          scopedSlots: { customRender: 'modelDesc' },
          key: 'modelDesc',
          align: 'center',
          ellipsis: true,
        },
        {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 180,
          align: 'center'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          key: 'operation',
          width: 140,
          align: 'center',
          fixed: 'right',
          scopedSlots: { customRender: 'operation' }
        }
      ]
    }
  },
  computed: {
    // 当前选择的模型类别对应的参数字段
    currentTypeFields() {
      return this.typeFields[this.currentType] || [];
    }

  },
  mounted() {
    this.getData()
  },
  methods: {
    // 处理标签页切换
    handleTabChange(key) {
      this.activeTabKey = key;
      this.resetForm();
    },

    // 处理模型类别变化
    handleTypeChange(value) {
      console.log('选择的类别:', value);
      this.currentType = value;

      // 由于使用了v-decorator，不需要手动设置表单值
      // 但需要清除之前的参数值
      const { modelForm } = this;
      const formValues = modelForm.getFieldsValue();

      // 创建一个新的表单值对象，只保留基本信息
      const newValues = {
        type: value,
        modelName: formValues.modelName,
        modelDesc: formValues.modelDesc
      };

      // 清除所有可能的参数字段
      const allParamFields = ['apiKey', 'apiUrl', 'apiSecret', 'appId'];
      allParamFields.forEach(field => {
        newValues[field] = undefined;
      });
      // 重置表单
      this.$nextTick(() => {
        // 设置新的表单值
        modelForm.setFieldsValue(newValues);
      });
    },

    // 获取模型配置列表
    getData() {
      axios
        .get({
          url: api.model.query,
          data: {
            page: this.pagination.page,
            pageSize: this.pagination.pageSize,
            ...this.query
          }
        })
        .then(res => {
          if (res.code === 200) {
            this.modelItems = res.data.list
            this.pagination.total = res.data.total
          } else {
            this.$message.error(res.message)
          }
        })
        .catch(() => {
          this.$message.error('服务器维护/重启中，请稍后再试')
        })
        .finally(() => {
          this.loading = false
        })
    },

    // 提交表单
    handleSubmit(e) {
      e.preventDefault()
      this.modelForm.validateFields((err, values) => {
        if (!err) {
          this.loading = true

          const url = this.editingModelId
            ? api.model.update
            : api.model.add

          axios
            .post({
              url,
              data: {
                modelId: this.editingModelId,
                ...values
              }
            })
            .then(res => {
              if (res.code === 200) {
                this.$message.success(
                  this.editingModelId ? '更新成功' : '创建成功'
                )
                this.resetForm()
                this.getData()
                // 成功后切换到模型列表页
                this.activeTabKey = '1'
              } else {
                this.$message.error(res.message)
              }
            })
            .catch(() => {
              this.$message.error('服务器维护/重启中，请稍后再试')
            })
            .finally(() => {
              this.loading = false
            })
        }
      })
    },

    // 编辑模型
    edit(record) {
      this.editingModelId = record.modelId
      this.currentType = record.type || '';

      // 切换到创建模型标签页
      this.activeTabKey = '2'

      this.$nextTick(() => {
        const { modelForm } = this

        // 设置基本信息和所有可能的参数字段
        const formValues = {
          ...record
        };

        // 设置表单值
        modelForm.setFieldsValue(formValues);
      })
    },

    // 删除模型
    update(modelId) {
      this.loading = true
      axios
        .post({
          url: api.model.update,
          data: {
            modelId: modelId,
            state: 0
          }
        })
        .then(res => {
          if (res.code === 200) {
            this.$message.success('删除模型配置成功')
            this.getData()
          } else {
            this.$message.error(res.message)
          }
        })
        .catch(() => {
          this.$message.error('服务器维护/重启中，请稍后再试')
        })
        .finally(() => {
          this.loading = false
        })
    },

    // 重置表单
    resetForm() {
      this.modelForm.resetFields()
      this.currentType = ''
      this.editingModelId = null
    }
  }
}
</script>