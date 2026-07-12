import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import { getServiceBaseURL } from '@/utils/service';

/** WebSocket 消息结构（与后端 WsMessage 对应） */
export interface WsMessage<T = any> {
  eventType: string;
  data: T;
  topic: string;
  timestamp: string;
}

/** 桌台状态变更消息 */
export interface TableStatusData {
  tableId: number;
  tableCode: string;
  oldStatus: number;
  newStatus: number;
}

type MessageHandler = (msg: WsMessage) => void;

let client: Client | null = null;
const subscribers = new Map<string, Set<MessageHandler>>();
const stompSubscriptions = new Map<string, { unsubscribe: () => void }>();

/** 获取 WebSocket 连接地址 */
function getWsUrl() {
  const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
  const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

  // 复用现有接口基地址规则，保证开发代理和部署环境都连接到同一后端。
  const normalizedBaseURL = baseURL.replace(/\/+$/, '');
  const wsPath = `${normalizedBaseURL}/ws`;

  if (/^https?:\/\//i.test(wsPath)) {
    return wsPath;
  }

  return new URL(wsPath.startsWith('/') ? wsPath : `/${wsPath}`, window.location.origin).toString();
}

/** 初始化 WebSocket 连接 */
export function connectWebSocket() {
  if (client?.active) return;

  client = new Client({
    // 使用 SockJS 作为传输层
    webSocketFactory: () => new SockJS(getWsUrl()) as any,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (msg) => {
      if (import.meta.env.DEV) {
        // 开发环境打印调试信息（过滤心跳）
        if (!msg.includes('PING') && !msg.includes('PONG')) {
          console.log('[WS]', msg);
        }
      }
    }
  });

  client.onConnect = () => {
    console.log('[WS] 连接成功');
    // 重新订阅所有 topic
    stompSubscriptions.clear();
    for (const [topic] of subscribers) {
      doSubscribe(topic);
    }
  };

  client.onStompError = (frame) => {
    console.error('[WS] STOMP 错误:', frame.headers.message);
  };

  client.onWebSocketError = (event) => {
    console.error('[WS] 底层连接异常:', event);
  };

  client.onWebSocketClose = (event) => {
    console.warn('[WS] 连接关闭:', event.code, event.reason || '无原因');
  };

  client.activate();
}

/** 断开 WebSocket 连接 */
export function disconnectWebSocket() {
  if (client?.active) {
    client.deactivate();
    client = null;
  }
}

/** 内部订阅实现 */
function doSubscribe(topic: string) {
  if (!client?.connected) return;
  if (stompSubscriptions.has(topic)) return;

  const subscription = client.subscribe(topic, (message) => {
    try {
      const parsed: WsMessage = JSON.parse(message.body);
      const handlers = subscribers.get(topic);
      if (!handlers || handlers.size === 0) return;
      for (const handler of handlers) {
        handler(parsed);
      }
    } catch (e) {
      console.error('[WS] 消息解析失败:', e);
    }
  });

  stompSubscriptions.set(topic, { unsubscribe: () => subscription.unsubscribe() });
}

/** 订阅指定 topic，返回取消订阅函数 */
export function subscribe(topic: string, handler: MessageHandler): () => void {
  if (!subscribers.has(topic)) {
    subscribers.set(topic, new Set());
  }
  const handlers = subscribers.get(topic)!;
  handlers.add(handler);

  // 如果已连接，立即订阅
  if (client?.connected) {
    doSubscribe(topic);
  }

  // 返回取消订阅函数
  return () => {
    handlers.delete(handler);
    if (handlers.size === 0) {
      subscribers.delete(topic);

      const sub = stompSubscriptions.get(topic);
      if (sub) {
        sub.unsubscribe();
        stompSubscriptions.delete(topic);
      }
    }
  };
}
