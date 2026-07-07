const env = {
  // 按实际部署地址修改
  apiHost: 'http://192.168.2.165:8080',
  apiPrefix: '/api/app',
  wsUrl: 'ws://192.168.2.165:8080/ws',
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
