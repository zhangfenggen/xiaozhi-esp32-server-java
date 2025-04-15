<script>
export default {
  name: "DeviceEditDialog",
  props:{
    visible:Boolean,
    modelItems:Array,
    sttItems:Array,
    roleItems:Array,
    current:Object
  },
  data(){
    return {
      form: this.$form.createForm(this, {
        deviceId:"",
        deviceName:"",
        modelId:null,
        sttId:null,
        roleId:null
      }),
    }
  },
  methods:{
    handleClose(){
      this.$emit("close");
    },
    handleOk(){
      this.$emit("submit",this.form)
    }
  },
  watch:{
    visible(val){
      if(val){
        this.form = Object.assign({},this.$props.current);
      }
    }
  }
}
</script>

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
        <a-select v-model="form.modelId">
          <a-select-option v-for="i in modelItems" :key="i.modelId" :value="i.modelId" class="model-option">{{ i.modelName }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="语音识别">
        <a-select  v-model="form.sttId">
          <a-select-option v-for="i in sttItems" :key="i.sttId" :value="i.sttId" class="model-option">{{ i.sttName }}</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style lang="scss" scoped>
>>> .ant-input{
  text-align: center !important;
}
</style>

<style>
.model-option{
  text-align: center;
}
</style>