const devHost = '172.16.95.229';

const env = {
  // 按实际部署地址修改；当前开发机使用局域网地址，便于模拟器和真机联调共用
  apiHost: `http://${devHost}:8080`,
  apiPrefix: '/api/app',
  wsUrl: `ws://${devHost}:8080/ws`,
  // 小程序端默认关闭 WebSocket：当前后端是 STOMP 端点，未适配小程序原生 ws 协议
  enableSocket: false,
  // 小程序手机号登录接口
  loginPath: '/api/app/auth/phone-login'
};

function getBaseURL(withPrefix = true) {
  return withPrefix ? `${env.apiHost}${env.apiPrefix}` : env.apiHost;
}

module.exports = {
  env,
  getBaseURL
};
