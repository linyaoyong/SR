# 本机环境与工具运行手册

本文记录 SR 项目在当前机器上的基础环境约定。旧项目 SDS 只在此类环境问题上作为参考。

## 1. Java 与 Maven

所有 Java/Maven 命令前优先设置 JDK 17：

```bash
export JAVA_HOME="<本机JDK17安装目录>"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

预期 Java 主版本为 17。后端测试和构建不要使用 Java 23，旧项目曾因 Mockito/Byte Buddy 兼容性在高版本 JDK 上失败。

建议构建命令：

```bash
cd backend
mvn clean test
mvn clean package -DskipTests
```

## 2. MySQL

默认本机约定：

```text
host: 127.0.0.1
port: 3306
user: root
password: <LOCAL_MYSQL_PASSWORD>
```

执行脚本时建议允许环境变量覆盖：

```bash
MYSQL_PASSWORD=${MYSQL_PASSWORD:?Set MYSQL_PASSWORD in your local environment}
mysql -u root "-p$MYSQL_PASSWORD" < backend/scripts/init.sql
```

SR 推荐数据库：

```text
sr_auth
sr_item
sr_rental
sr_wallet
sr_message
sr_admin
```

## 3. Redis

默认地址：

```text
127.0.0.1:6379
```

常用验证：

```bash
redis-cli ping
redis-cli keys 'sr:*'
```

Redis 在 SR 中用于 refresh token、物品缓存、申请防重复、时间段库存锁、消息未读、WebSocket 在线状态、定时任务锁和事件去重。

## 4. Nacos

默认地址：

```text
http://127.0.0.1:8848
```

用途：

- 服务注册发现。
- Gateway 通过服务名路由。
- OpenFeign 通过服务名调用。
- 管理演示配置，如 QPS 阈值、支付超时、慢调用阈值。

建议为 SR 编写 Nacos 演示配置脚本。脚本放根目录 `scripts/`，不要放进 `backend/`，因为它属于课程演示和本机工具编排，不是后端业务代码：

```text
scripts/nacos-config.sh apply
scripts/nacos-config.sh status
scripts/nacos-config.sh delete
```

脚本应带项目标记，例如：

```text
# share-rental-demo-managed: true
```

避免覆盖 Nacos 中非本项目托管的配置。

## 5. Sentinel Dashboard

默认地址：

```text
http://127.0.0.1:8089
```

建议固定演示资源：

```text
GET /api/items
POST /api/rentals/applications
rental:item-access
rental:wallet-access
```

演示时需要截图：

- 实时监控。
- 资源名。
- Block QPS 或熔断状态。
- 被限流/降级接口的响应。

## 6. JMeter

用户要求：本项目提供的所有 JMeter 脚本均按 GUI 使用方式设计和说明。课程演示文档、README 和视频脚本必须以 GUI 操作为准。

```bash
jmeter
```

GUI 演示推荐流程：

1. 执行 `jmeter` 打开 GUI。
2. File -> Open 选择对应 `.jmx`。
3. 检查 `host`、`port`、线程数、CSV 数据文件路径。
4. 点击 Start。
5. 查看 View Results Tree 和 Aggregate Report。
6. 截图保存到演示材料。

SR 至少准备这些 JMX：

- `docs/tests/item-list-stress.jmx`
- `docs/tests/application-submit-limit.jmx`
- `docs/tests/rental-time-lock-consistency.jmx`
- `docs/tests/wallet-slow-call-circuit-breaker.jmx`
- `docs/tests/item-service-down-degrade.jmx`

输出目录 `docs/tests/output/` 不提交。

## 7. Postman / Apifox

建议文件：

```text
docs/tests/Share Rental API - 双用户完整流程.postman_collection.json
docs/tests/Share Rental API - environment.local.postman_environment.json
```

集合应内置变量：

```text
baseUrl=http://<开发机局域网IP>:8080
userAToken
userBToken
adminToken
itemId
applicationId
proposalId
orderId
conversationId
```

Postman/Apifox 集合尽量完整覆盖 `03-api-contract.md` 中所有对外接口，并为关键内部 Feign 接口准备单独分组，便于演示内部接口被 Gateway 禁止外部访问、服务间调用需内部 token。

每个对外接口至少覆盖：

- 正常请求。
- 参数错误。
- 未登录。
- 无权限。
- 业务异常。
- 限流或降级响应。

## 8. 前端环境

建议版本：

```text
Node.js 20+
npm 10+
```

普通前端：

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

管理员前端：

```bash
cd admin-frontend
npm install
npm run dev -- --host 0.0.0.0
```

后端服务和 Gateway 也需要对局域网开放，便于手机、平板或同一网络内其他设备测试。Spring Boot 配置建议统一包含：

```yaml
server:
  address: 0.0.0.0
```

前端环境变量中的 API 地址不要写死 `localhost`，局域网测试时应使用：

```text
VITE_API_BASE_URL=http://<开发机局域网IP>:8080
VITE_WS_BASE_URL=ws://<开发机局域网IP>:8080/ws/chat
```

前端验收必须包含桌面和移动端截图，检查文字不溢出、按钮可点击、布局不重叠、核心流程可走通。
