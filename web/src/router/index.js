import Vue from "vue";
import Router from "vue-router";

Vue.use(Router);

export default new Router({
  mode: "history",
  routes: [
    {
      path: "/",
      redirect: "login"
    },
    {
      path: "/",
      component: resolve => require(["@/views/common/Home"], resolve),
      meta: { title: "首页" },
      children: [
        {
          path: "/dashboard",
          component: resolve => require(["@/views/page/Dashboard"], resolve),
          name: "Dashboard",
          meta: { title: "Dashboard", icon: "dashboard" }
        },
        {
          path: "/device",
          component: resolve => require(["@/views/page/Device"], resolve),
          name: "Device",
          meta: {
            title: "设备管理",
            icon: "robot",
            breadcrumb: [{ breadcrumbName: "设备管理" }]
          }
        },
        {
          path: "/message",
          component: resolve => require(["@/views/page/Message"], resolve),
          name: "message",
          meta: { title: "对话管理", icon: "message" }
        },
        {
          path: "/setting",
          component: resolve => require(["@/views/common/PageView"], resolve),
          name: "Setting",
          redirect: "/setting/account",
          meta: { title: "设置", icon: "setting" },
          children: [
            {
              path: "/setting/account",
              component: resolve =>
                require(["@/views/page/setting/Account"], resolve),
              meta: { title: "个人中心", parent: "设置" }
            },
            {
              path: "/setting/config",
              component: resolve =>
                require(["@/views/page/setting/Config"], resolve),
              meta: { title: "个人设置", parent: "设置" }
            }
          ]
        }
      ]
    },
    {
      path: "/login",
      component: resolve => require(["@/views/page/Login"], resolve)
    },
    {
      path: "/forget",
      component: resolve => require(["@/views/page/Forget"], resolve)
    },
    {
      path: "/register",
      component: resolve => require(["@/views/page/Register"], resolve)
    },
    {
      path: "/404",
      component: resolve => require(["@/views/exception/404.vue"], resolve)
    },
    {
      path: "/403",
      component: resolve => require(["@/views/exception/403.vue"], resolve)
    },
    {
      path: "/*",
      redirect: "/404"
    }
  ]
});
