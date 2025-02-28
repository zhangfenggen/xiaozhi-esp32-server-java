<template>
    <div class="login">
      <a-form-model
        id="login"
        ref="loginForm"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit="handleSubmit"
      >
        <a-form-model-item>
          <span>管理登录页面</span>
        </a-form-model-item>
        <a-form-model-item prop="username">
          <a-input
            v-model="loginForm.username"
            placeholder="用户名"
          >
            <a-icon slot="prefix" type="user" style="color: rgba(0,0,0,.25)" />
          </a-input>
        </a-form-model-item>
        <a-form-model-item prop="password">
          <a-input-password
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
          >
            <a-icon slot="prefix" type="lock" style="color: rgba(0,0,0,.25)" />
          </a-input-password>
        </a-form-model-item>
        <a-form-model-item prop="rememberMe">
          <a-checkbox
            v-model="loginForm.rememberMe"
          >
            记住我
          </a-checkbox>
          <a class="login-form-forgot">
            <router-link to="forget">忘记密码</router-link>
          </a>
        <a-button type="primary" html-type="submit" class="login-form-button">登录</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>

<script>
import axios from '@/services/axios'
import api from '@/services/api'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  data () {
    return {
      loginForm: {
        username: '',
        password: '',
        rememberMe: false
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名！', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码！', trigger: 'blur' }]
      }
    }
  },
  mounted () {
    this.getCookie()
  },
  methods: {
    getCookie () {
      const username = Cookies.get('username')
      const password = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: password === undefined ? false : Boolean(password)
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          axios
            .post({
              url: api.user.login,
              data: {
                ...this.loginForm
              }
            }).then(res => {
              if (res.code === 200) {
                Cookies.set('userInfo', JSON.stringify(res.data), { expires: 30 })
                if (this.loginForm.rememberMe) {
                  Cookies.set('username', this.loginForm.username, { expires: 30 })
                  Cookies.set('rememberMe', encrypt(this.loginForm.password), { expires: 30 })
                } else {
                  Cookies.remove('username')
                  Cookies.remove('rememberMe')
                }
                this.$store.commit('USER_INFO', res.data)
                this.$router.push('/dashboard')
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
.login {
  font-family:KaiTi;
  height:100vh;
  background:#242645;
  color:#fff;
}
.login-form {
  position:absolute;
  width:300px;
  height:350px;
  margin:auto;
  padding:40px;
  left:0;
  right:0;
  top:0;
  bottom:0;
  box-shadow:-15px 15px 15px rgba(6, 17, 47, 0.7);
  opacity:0.7;
  background:#35394a;
  background:-webkit-gradient(linear, left bottom, right top, color-stop(0%, #35394a), color-stop(100%, rgb(0, 0, 0)));
  background:-webkit-linear-gradient(230deg, rgba(53, 57, 74, 0) 0%, rgb(0, 0, 0) 100%);
  span {
    color: white;
    font-size: 1.2em;
  }
  ::v_deep .ant-checkbox-wrapper {
    color: white;
  }
  .login-form-forgot {
    float: right;
  }
  .login-form-button {
    width: 100%;
  }
}
</style>
