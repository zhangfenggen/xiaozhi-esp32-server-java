import axios from "axios";
const qs = window.Qs;
import { message } from "ant-design-vue";

// 设置axios的基础URL，根据环境变量
axios.defaults.baseURL = process.env.BASE_API;
// 设置携带凭证
axios.defaults.withCredentials = true;

function Rest() {}
Rest.prototype = {
  jsonPost(opts) {
    return new Promise((resolve, reject) => {
      axios({
        method: "post",
        url: opts.url,
        headers: {
          "Content-Type": "application/json;charset=UTF-8"
        },
        transformRequest: [
          function(data, headers) {
            return JSON.stringify(opts.data);
          }
        ]
      })
        .then(res => {
          commonResponse(res.data, resolve);
        })
        .catch(e => {
          rejectResponse(e);
        });
    });
  },
  post(opts) {
    return new Promise((resolve, reject) => {
      axios({
        method: "post",
        url: opts.url,
        headers: {
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        transformRequest: [
          function(data, headers) {
            return qs.stringify(opts.data);
          }
        ]
      })
        .then(res => {
          commonResponse(res.data, resolve);
        })
        .catch(e => {
          rejectResponse(e);
        });
    });
  },
  get(opts) {
    return new Promise((resolve, reject) => {
      axios(opts.url, {
        params: opts.data
      })
        .then(res => {
          commonResponse(res.data, resolve);
        })
        .catch(e => {
          rejectResponse(e);
        });
    });
  }
};
function commonResponse(data, resolve) {
  if (data.code === 403) {
    const key = "error";
    message.error({
      content: "登录过期，请重新登录！",
      type: "error",
      key,
      onClose: () => {
        // 使用环境变量中的BASE_URL，而不是硬编码的URL
        window.location.href = process.env.BASE_URL;
      }
    });
  } else {
    resolve(data);
  }
}

function rejectResponse(e) {
  if (e.response.status === 401 || e.response.status === 403) {
    const key = "error";
    message.error({
      content: "登录过期，请重新登录！",
      type: "error",
      key,
      onClose: () => {
        // 使用环境变量中的BASE_URL，而不是硬编码的URL
        window.location.href = process.env.BASE_URL;
      }
    });
  } else {
    resolve(data);
  }
}
export default new Rest();
