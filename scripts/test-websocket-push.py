#!/usr/bin/env python3
"""阶段 7 WebSocket 烟测：B 连接 → 等待 A 发消息 → 验证推送 + Redis 在线状态"""
import sys
import threading
import time
import websocket
import urllib.request
import json

JWT_A = open('/tmp/jwt_a.txt').read().strip()
JWT_B = open('/tmp/jwt_b.txt').read().strip()

received = []

def on_message(ws, message):
    received.append(message)
    print(f"[RECEIVED] {message}", flush=True)

def on_error(ws, error):
    print(f"[ERROR] {error}", flush=True)

def on_close(ws, close_status, close_msg):
    print(f"[CLOSED] status={close_status} msg={close_msg}", flush=True)

def on_open(ws):
    print("[CONNECTED] B WebSocket 已连接", flush=True)
    # 发个 ping 心跳
    ws.send("ping")

def check_redis_online():
    """通过 redis-cli 检查 B (userId=21) 的在线状态"""
    import subprocess
    r = subprocess.run(['redis-cli', 'get', 'sr:message:online:21'],
                       capture_output=True, text=True)
    print(f"[REDIS] sr:message:online:21 = {r.stdout.strip()!r}", flush=True)
    r2 = subprocess.run(['redis-cli', 'exists', 'sr:message:online:21'],
                        capture_output=True, text=True)
    print(f"[REDIS] exists = {r2.stdout.strip()}", flush=True)

def send_message_from_a():
    """A 通过 REST 发消息给 B"""
    time.sleep(3)  # 等 B 连接建立 + Redis 写入
    print("\n[A SENDING] 通过 REST 发消息...", flush=True)
    req = urllib.request.Request(
        'http://127.0.0.1:8080/api/messages/conversations/4/messages',
        method='POST',
        data=json.dumps({
            "messageType": 1,
            "content": "WebSocket 推送测试消息 - 阶段7"
        }).encode(),
        headers={
            'Authorization': f'Bearer {JWT_A}',
            'Content-Type': 'application/json'
        },
    )
    try:
        resp = urllib.request.urlopen(req, timeout=5)
        print(f"[A SENT] HTTP {resp.status}: {resp.read().decode()[:200]}", flush=True)
    except Exception as e:
        print(f"[A SEND ERROR] {e}", flush=True)
    # 给 ws 接收时间
    time.sleep(3)
    # 关闭 ws
    ws.keep_running = False
    try:
        ws.close()
    except Exception:
        pass

if __name__ == '__main__':
    # 用 no_proxy 绕过 Clash
    ws_url = f"ws://127.0.0.1:8080/ws/chat?token={JWT_B}"
    print(f"[CONNECTING] {ws_url[:80]}...", flush=True)

    ws = websocket.WebSocketApp(
        ws_url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close,
    )

    # 启动 A 发消息的线程（包含关闭 ws 逻辑）
    t = threading.Thread(target=send_message_from_a, daemon=True)
    t.start()

    # 阻塞 15 秒等接收（send_message_from_a 内部会主动关闭 ws）
    try:
        ws.run_forever(ping_interval=0)
    except Exception as e:
        print(f"[run_forever error] {e}", flush=True)

    # 连接关闭后检查 Redis 状态（应已删除）
    print("\n[POST-CLOSE] 检查 Redis 在线状态（应已删除）...", flush=True)
    check_redis_online()

    print(f"\n[RESULT] 收到 {len(received)} 条推送", flush=True)
    if received:
        print("[PASS] WebSocket 推送正常", flush=True)
        sys.exit(0)
    else:
        print("[FAIL] 没收到推送", flush=True)
        sys.exit(1)
