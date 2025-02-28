<template>
  <a-layout>
    <a-layout-content>
      <div class="layout-content-margin">
        <a-card title="欢迎注册" :bordered="false">
          <a-row>
            <!-- 信息 -->
            <a-col :md="24" :lg="12">
              <a-form
                :form="infoForm"
                :colon="false"
                @submit="submit">
                <a-form-item label="姓名">
                  <a-input
                    v-decorator="['name', {rules: [{ required: true, message: '请输入真实姓名' }]}]"
                    autocomplete="off"
                    placeholder="请输入真实姓名，用于支付宝转款验证"
                  />
                </a-form-item>
                <a-form-item label="支付宝">
                  <a-input
                    v-decorator="['alipay', {rules: [{ required: method, message: '请输入支付宝账号'}, { validator: paymentMethod }, { validateTrigger: ['change', 'blur']} ]}]"
                    autocomplete="off"
                    placeholder="用于结算工资，可与微信二选一"
                  />
                </a-form-item>
                <a-form-item label="微信">
                  <a-input
                    v-decorator="['wechat', {rules: [{ required: method, message: '请输入微信账号'}, { validator: paymentMethod }, { validateTrigger: ['change', 'blur']} ]}]"
                    autocomplete="off"
                    placeholder="用于结算工资，可与支付宝二选一"
                  />
                </a-form-item>
                <a-form-item label="QQ">
                  <a-input
                    v-decorator="['qq', {rules: [{ required: true, message: '请输入QQ号' }]}]"
                    autocomplete="off"
                    placeholder="有任何问题我们会与此QQ联系"
                  />
                </a-form-item>
                <a-form-item label="推荐人">
                  <a-input
                    v-decorator="['referrer', {rules: [{ required: false, message: '请输入推荐人' }]}]"
                    autocomplete="off"
                    placeholder="没有可不填"
                  />
                </a-form-item>
                <a-form-item label="注册码">
                  <a-input
                    disabled
                    v-decorator="['registerId', { initialValue: code }, {rules: [{ required: true, message: '请输入注册码' }]}]"
                    autocomplete="off"
                    placeholder="请找管理索要注册码"
                  />
                </a-form-item>
                <a-form-item label=" ">
                  <a-button type="primary" html-type="submit">保存</a-button>
                </a-form-item>
              </a-form>
            </a-col>
          </a-row>
        </a-card>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script>
import axios from '@/services/axios'
import api from '@/services/api'
export default {
  data () {
    return {
      // 结款账号
      method: true,
      code: this.$route.query.registerId,
      infoForm: this.$form.createForm(this)
    }
  },
  mounted () {
    if (this.code) this.verifyCode()
  },
  methods: {
    verifyCode () {
      axios
        .get({
          url: api.verify.confirmCode,
          data: {
            registerId: this.code
          }
        }).then(res => {
          if (res.code !== 200) this.$message.error(res.message)
        }).catch(() => {
          this.$message.error('服务器维护/重启中，请稍后再试')
        })
    },
    /* 结算方式 */
    paymentMethod (rule, value, callback) {
      if (value && value.trim()) {
        this.method = false
        return callback()
      }
      this.method = true
      return callback(new Error('请选择一种收款方式并输入账号'))
    },

    /* 提交按钮 */
    submit (e) {
      e.preventDefault()
      this.infoForm.validateFields((err, values) => {
        if (!err) {
          axios
            .post({
              url: api.device.add,
              data: {
                ...values
              }
            }).then(res => {
              if (res.code === 200) {
                this.$message.success(res.message)
              } else {
                this.$message.error(res.message)
              }
            }).catch(() => {
              this.$message.error('服务器维护/重启中，请稍后再试')
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.ant-layout-content >>> .layout-content-margin {
  margin: 20px;
  height: 100vh;
}
</style>
