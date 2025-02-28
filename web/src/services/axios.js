import axios from "axios";
const qs = window.Qs;
import { message } from "ant-design-vue";
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
          reject(e);
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
          reject(e);
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
          reject(e);
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
        window.location.href = "http://localhost:8084";
      }
    });
  } else {
    resolve(data);
  }
}
export default new Rest();
