<template>
  <div class="wrapper">
    <!-- <a-layout v-if="!!userInfo"> -->
    <a-layout>
      <!-- 占位 -->
      <div :style="`width: ${width}px; overflow: hidden; flex: 0 0 ${width}px; max-width: ${width}px; min-width: ${width}px;`"></div>
      <!-- 侧边栏 -->
      <a-layout-sider
        breakpoint="sm"
        collapsed-width="0"
        @breakpoint="onBreakpoint($event, 'sm')"
        style="display: none"
      >
      </a-layout-sider>
      <a-layout-sider
        theme="light"
        breakpoint="lg"
        :collapsed-width="collapseWidth"
        @breakpoint="onBreakpoint($event, 'lg')"
        class="fixed-sidebar"
        :zeroWidthTriggerStyle="{ top: '100px' }"
      >
        <v-sidebar></v-sidebar>
      </a-layout-sider>
      <a-layout>
        <!-- 页眉 -->
        <a-layout-header style="padding: 0; height: auto; line-height: auto">
          <v-header></v-header>
        </a-layout-header>
        <a-layout>
          <!-- 主要内容 -->
          <a-layout-content>
            <v-breadcrumb></v-breadcrumb>
            <router-view />
          </a-layout-content>
          <!-- 页脚 -->
          <a-layout-footer>
            <v-footer></v-footer>
          </a-layout-footer>
        </a-layout>
      </a-layout>
    </a-layout>
  </div>
</template>

<script>
import vSidebar from './Sidebar.vue'
import vHeader from './Header.vue'
import vFooter from './Footer.vue'
import vBreadcrumb from './Breadcrumb.vue'
export default {
  components: {
    vHeader,
    vSidebar,
    vFooter,
    vBreadcrumb
  },
  data () {
    return {
      // 占位宽度
      width: 0,
      // 折叠宽度
      collapseWidth: 0,
      // 判断是否为手机
      clientWidth: document.body.clientWidth
    }
  },
  mounted () {
    window.onresize = () => {
      return (() => {
        this.clientWidth = document.body.clientWidth
      })()
    }
    // 没有登录过或者已退出登录的情况下直接访问页面会跳转到登录页面
    if (!this.userInfo) this.$router.push('/login')
  },
  computed: {
    isMobile () {
      return this.$store.getters.MOBILE_TYPE
    },
    userInfo () {
      return this.$store.getters.USER_INFO
    }
  },
  watch: {
    clientWidth (newVal, oldVal) {
      this.$store.commit('MOBILE_TYPE', newVal < 480)
    }
  },
  methods: {
    /* 侧边栏切换操作 */
    onCollase (collapsed, type) {
      this.collapseWidth = 80
      this.width = 80
      if (type === 'lg' && !collapsed) {
        this.collapseWidth = 200
        this.width = 200
        this.siderCheck = true
      } else if ((type === 'sm' && collapsed) || this.isMobile) {
        this.collapseWidth = 0
        this.width = 0
      }
    },
    onBreakpoint (broken, type) {
      this.onCollase(broken, type)
    }
  }

}
</script>

<style scoped lang="scss">
.wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
}
.fixed-sidebar {
  box-shadow: 0 2px 8px #f0f1f2;
  height: 100vh;
  z-index: 99;
  position: fixed;
  left: 0;
}
</style>
