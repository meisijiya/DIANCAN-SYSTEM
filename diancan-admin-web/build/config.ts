import json5 from 'json5';
import type { ProxyOptions } from 'vite';

/**
 * 获取构建时间
 *
 * @author Henfon
 * @date 2026/07/08
 * @description 生成前端构建时间字符串，用于版本展示和调试排查
 */
export function getBuildTime() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');

  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

/**
 * 创建 Vite 代理配置
 *
 * @author Henfon
 * @date 2026/07/08
 * @description 根据环境变量生成默认服务和扩展服务的代理规则
 */
export function createViteProxy(
  env: Env.ImportMeta,
  enableProxy: boolean
): Record<string, ProxyOptions> | undefined {
  if (!enableProxy || env.VITE_HTTP_PROXY !== 'Y') {
    return undefined;
  }

  const proxy: Record<string, ProxyOptions> = {};

  registerProxy(proxy, '/proxy-default', env.VITE_SERVICE_BASE_URL, env);

  const otherBaseUrlMap = parseOtherServiceBaseUrl(env.VITE_OTHER_SERVICE_BASE_URL);
  for (const [key, target] of Object.entries(otherBaseUrlMap)) {
    registerProxy(proxy, `/proxy-${key}`, target, env);
  }

  return proxy;
}

/**
 * 注册单个代理规则
 *
 * @param proxy 代理配置集合
 * @param prefix 代理前缀
 * @param target 目标地址
 * @param env 环境变量
 * @author Henfon
 * @date 2026/07/08
 * @description 为指定前缀挂载 Vite 代理，并在需要时输出代理日志
 */
function registerProxy(
  proxy: Record<string, ProxyOptions>,
  prefix: string,
  target: string | undefined,
  env: Env.ImportMeta
) {
  if (!target) {
    return;
  }

  proxy[prefix] = {
    target,
    changeOrigin: true,
    ws: true,
    rewrite: path => path.replace(new RegExp(`^${prefix}`), ''),
    configure: proxyServer => {
      if (env.VITE_PROXY_LOG !== 'Y') {
        return;
      }

      proxyServer.on('proxyReq', (_proxyReq, req) => {
        // 输出代理目标，便于排查接口是否走到正确后端
        console.log(`[vite-proxy] ${req.method} ${req.url} -> ${target}`);
      });
    }
  };
}

/**
 * 解析额外服务地址
 *
 * @param raw 环境变量原始值
 * @author Henfon
 * @date 2026/07/08
 * @description 将 json5 字符串解析成服务地址映射
 */
function parseOtherServiceBaseUrl(raw: string) {
  try {
    return json5.parse(raw) as Record<string, string>;
  } catch {
    console.error('VITE_OTHER_SERVICE_BASE_URL is not a valid json5 string');
    return {};
  }
}
