<template>
  <a-modal v-model="visible" title="设备详情" @ok="handleOk" @cancel="handleClose">
    <a-form :form="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-form-item label="设备名称">
        <a-input v-model="form.deviceName"/>
      </a-form-item>
      <a-form-item label="绑定角色">
        <a-select v-model="form.roleId">
          <a-select-option v-for="i in roleItems" :key="i.roleId" :value="i.roleId" class="model-option">{{ i.roleName }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="对话模型">
        <a-cascader 
          style="width: 100%"
          :options="modelOptions" 
          :value="getCascaderValue()"
          @change="handleModelChange" 
          placeholder="请选择模型"
          expandTrigger="hover"
          :allowClear=false />
      </a-form-item>
      <a-form-item label="语音识别">
        <a-select v-model="form.sttId">
          <a-select-option v-for="i in sttItems" :key="i.sttId" :value="i.sttId" class="model-option">{{ i.sttName }}</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>
export default {
  name: "DeviceEditDialog",
  props:{
    visible: Boolean,
    modelItems: Array,
    sttItems: Array,
    roleItems: Array,
    current: Object,
    agentItems: Array
  },
  data(){
    return {
      form: this.$form.createForm(this, {
        deviceId: "",
        deviceName: "",
        modelId: null,
        modelType: "",
        sttId: null,
        roleId: null
      }),
      modelOptions: [
        {
          value: "llm",
          label: "LLM模型",
          children: []
        },
        {
          value: "agent",
          label: "智能体",
          children: []
        }
      ]
    }
  },
  methods:{
    handleClose(){
      this.$emit("close");
    },
    handleOk(){
      this.$emit("submit", this.form)
    },
    // 处理级联选择器变更
    handleModelChange(value) {
      if (!value || value.length < 2) return;
      
      const modelType = value[0]; // llm 或 agent
      const modelId = Number(value[1]); // 确保modelId是数字类型
      
      this.form.modelId = modelId; // 保存modelId，这是传给后端的值
      this.form.modelType = modelType; // 保存模型类型
      
      // 根据类型设置显示名称和描述
      if (modelType === "llm") {
        const model = this.modelItems.find(item => Number(item.configId) === modelId);
        if (model) {
          this.form.modelName = model.configName || model.modelName;
          this.form.modelDesc = model.configDesc || model.modelDesc;
          this.form.provider = model.provider || '';
        }
      } else if (modelType === "agent") {
        const agent = this.agentItems.find(item => Number(item.configId) === modelId);
        if (agent) {
          this.form.modelName = agent.agentName;
          this.form.modelDesc = agent.agentDesc || '';
          this.form.provider = 'coze';
        }
      }
    },
    // 获取级联选择器的值
    getCascaderValue() {
      if (!this.form.modelId) return [];
      
      // 使用modelType字段确定类型
      if (this.form.modelType === 'agent') {
        return ["agent", this.form.modelId];
      } else {
        return ["llm", this.form.modelId];
      }
    },
    // 更新模型选项
    updateModelOptions() {
      // 清空现有选项
      this.modelOptions[0].children = [];
      this.modelOptions[1].children = [];
      
      // 添加LLM模型选项
      if (this.modelItems && this.modelItems.length > 0) {
        this.modelItems.forEach(item => {
          this.modelOptions[0].children.push({
            value: item.configId || item.modelId,
            label: item.configName || item.modelName,
            isLeaf: true,
            data: item
          });
        });
      }
      
      // 添加智能体选项
      if (this.agentItems && this.agentItems.length > 0) {
        this.agentItems.forEach(item => {
          this.modelOptions[1].children.push({
            value: item.configId || item.modelId,
            label: item.agentName,
            isLeaf: true,
            data: item
          });
        });
      }
    }
  },
  watch:{
    visible(val){
      if(val){
        this.form = Object.assign({}, this.$props.current);
        this.updateModelOptions();
      }
    },
    modelItems() {
      this.updateModelOptions();
    },
    agentItems() {
      this.updateModelOptions();
    }
  }
}
</script>

<style lang="scss" scoped>
>>> .ant-input{
  text-align: center !important;
}

/* 确保级联选择器内容居中 */
>>> .ant-cascader-picker-label {
  text-align: center !important;
  width: 100% !important;
}

</style>