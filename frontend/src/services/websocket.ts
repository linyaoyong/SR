// WebSocket 客户端：连接 Gateway 的 /ws/chat?token={jwt}，支持心跳与断线重连。
// 协议对齐 docs/03-api-contract.md 第 11.2 节：
// - 连接地址 ws://<gateway>/ws/chat?token={jwt}
// - 心跳：客户端发送文本 "ping"，服务端回复 "pong"
// - 推送：用户消息为 MessageResponse JSON；系统通知为 content 文本
export interface ChatSocket {
  close: () => void;
}

export interface CreateChatSocketParams {
  token: string;
  onMessage: (payload: unknown) => void;
  onStateChange: (connected: boolean) => void;
}

const HEARTBEAT_MS = 60000;
const INITIAL_RETRY_MS = 1000;
const MAX_RETRY_MS = 10000;

export function createChatSocket(params: CreateChatSocketParams): ChatSocket {
  let socket: WebSocket | null = null;
  let stopped = false;
  let retryMs = INITIAL_RETRY_MS;
  let heartbeat: ReturnType<typeof setInterval> | undefined;
  let reconnectTimer: ReturnType<typeof setTimeout> | undefined;

  const clearTimers = () => {
    if (heartbeat) {
      clearInterval(heartbeat);
      heartbeat = undefined;
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = undefined;
    }
  };

  const connect = () => {
    if (stopped) return;
    // 显式配置时直接用；为空时按当前页面 host 动态拼接，便于局域网设备访问 dev server。
    // 例如手机访问 http://10.0.0.2:5173/ 时会连 ws://10.0.0.2:5173/ws/chat，再由 vite proxy 转到 Gateway。
    const configured = (import.meta.env.VITE_WS_BASE_URL as string | undefined) ?? '';
    const base = configured || `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}`;
    const url = `${base}/ws/chat?token=${encodeURIComponent(params.token)}`;
    try {
      socket = new WebSocket(url);
    } catch {
      scheduleReconnect();
      return;
    }

    socket.onopen = () => {
      retryMs = INITIAL_RETRY_MS;
      params.onStateChange(true);
      heartbeat = setInterval(() => {
        if (socket && socket.readyState === WebSocket.OPEN) {
          socket.send('ping');
        }
      }, HEARTBEAT_MS);
    };

    socket.onmessage = (event) => {
      const data = event.data;
      if (data === 'pong') return;
      try {
        params.onMessage(JSON.parse(data));
      } catch {
        // 非文本 JSON（系统通知直接推送 content），包装为系统通知消息。
        params.onMessage({ messageType: 4, content: data });
      }
    };

    socket.onclose = () => {
      params.onStateChange(false);
      if (heartbeat) {
        clearInterval(heartbeat);
        heartbeat = undefined;
      }
      scheduleReconnect();
    };

    socket.onerror = () => {
      // 主动关闭以触发 onclose 走重连流程。
      socket?.close();
    };
  };

  const scheduleReconnect = () => {
    if (stopped) return;
    if (reconnectTimer) clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(() => {
      reconnectTimer = undefined;
      connect();
    }, retryMs);
    // 指数退避：1s -> 2s -> 4s -> 8s -> 10s（上限 10s）。
    retryMs = Math.min(retryMs * 2, MAX_RETRY_MS);
  };

  connect();

  return {
    close: () => {
      stopped = true;
      clearTimers();
      if (socket) {
        socket.onclose = null;
        socket.onerror = null;
        socket.onmessage = null;
        socket.onopen = null;
        try {
          socket.close();
        } catch {
          // 忽略重复关闭。
        }
        socket = null;
      }
      params.onStateChange(false);
    },
  };
}
