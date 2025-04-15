import Vue from 'vue'
import Vuex from 'vuex'
import Cookies from 'js-cookie'
Vue.use(Vuex)

const store = new Vuex.Store({
  state: {
    info: Cookies.getJSON('userInfo'),
    isMobile: true
  },
  getters: {
    USER_INFO: state => {
      return state.info
    },
    MOBILE_TYPE: state => {
      return state.isMobile
    }
  },
  mutations: {
    USER_INFO: (state, info) => {
      state.info = info
    },
    MOBILE_TYPE: (state, isMobile) => {
      state.isMobile = isMobile
    }
  }
})
export default store
