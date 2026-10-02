# 项目边界与协作标准

## 1. 项目定位

邻享租借平台是一个小范围可信物品租借平台，面向本地社区、校园或熟人圈内的短期租借场景。核心闭环是：

```text
注册登录 -> 发布物品 -> 浏览收藏 -> 发起租借申请 -> 多轮协商 -> 创建订单
-> 钱包支付租金/冻结押金 -> 面交或快递履约 -> 归还确认 -> 评价与信用变化
```

本项目的课程目标是用 Spring Cloud Alibaba 做出可运行、可演示、可答辩的微服务系统，而不是生产级租赁平台。

## 2. 最高规格来源

业务规格以 `docs/邻享租借平台拟稿.md` 为准。该文档已确认：

- 普通用户前端和管理员前端独立。
- 后端拆分为 `auth-service`、`item-service`、`rental-service`、`wallet-service`、`message-service`、`admin-service`。
- 必须覆盖 Gateway、Nacos、OpenFeign、Sentinel、Redis、RabbitMQ、MySQL、Postman/Apifox、JMeter。
- 必须准备 API 文档、数据库表结构、错误码、状态码、前端页面设计、测试资产和演示证据。

## 3. 非目标

P0 不做以下内容：

- 真实支付、微信支付、支付宝支付。
- 真实物流查询和物流公司接口。
- 真实实名认证、学号认证、短信验证码。
- 生产级风控、押金仲裁、法务流程。
- Docker/Kubernetes 生产部署。
- 搜索引擎、推荐算法、复杂 RBAC。

这些能力可作为报告中的扩展方向，不能阻塞 P0 演示。

## 4. SDS 复用边界

`/Users/linyaoyong/CodeProjects/SDS` 只作为环境和工程经验参考。允许参考：

- 本机 JDK 17、MySQL、Redis、Nacos、Sentinel、JMeter 的使用方式。
- Gateway 鉴权、`/internal/**` 外部拦截、Feign 内部 token。
- Nacos 配置 apply/status/delete 脚本模式。
- JMeter 压测与 Sentinel 演示组织方式。
- 旧项目复盘中的坑点。

不允许直接继承：

- 校园二手交易的业务模型。
- 旧 API 路径和错误码编号。
- 旧前端 UI 风格和页面组织。
- 旧项目中未验证或已复盘为问题的实现方式。

## 5. 工程标准

- Java 使用 JDK 17。
- 后端使用 Spring Boot 3.x、Spring Cloud 2023.x、Spring Cloud Alibaba 2023.x。
- 后端模块按 Maven parent + common + gateway + service 拆分。
- 数据库按服务拆库，数据库名统一 `sr` 前缀。
- 所有外部接口统一返回 `{ code, message, data }`。
- 前端只访问 Gateway。
- 内部 Feign 接口必须以 `/internal/**` 开头，并使用内部 token。
- 关键状态变更必须记录状态历史或流水。
- 跨服务写操作要有幂等、补偿或最终一致性说明。

## 6. 文档标准

每个重要对象必须在文档中能找到：

- 业务规则：写在规格或领域设计文档中。
- API：写在 `03-api-contract.md`，实现后由 OpenAPI/Knife4j 聚合。
- 数据库：写在 `04-database-design.md`，实现后落到 `backend/scripts/init.sql`。
- 前端页面：写在 `05-frontend-design.md`。
- 测试资产：写在 `06-test-and-demo-plan.md` 和 `07-nacos-sentinel-jmeter-plan.md`。

## 7. 开发顺序建议

1. 固定文档入口、环境和项目边界。
2. 生成数据库设计和 API 契约。
3. 搭建后端 parent/common/gateway 和基础服务。
4. 先闭环认证、物品、租借申请、订单、钱包最小主流程。
5. 再接入消息、管理员审核、评价、异议入口。
6. 同步构建普通前端和管理员前端。
7. 最后补齐 Postman、JMeter、Nacos、Sentinel、演示视频脚本和最终报告。
