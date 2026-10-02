# 邻享租借平台（SR）

面向社区/校园物品租借场景的课程项目，用于展示微服务划分、前后端集成、接口设计、测试与安全迭代。仓库提供普通用户前端、独立管理端和七个后端服务模块。

**这是用于学习、备份和作品展示的源码。支付、钱包和押金是模拟功能，不接入真实资金。当前版本有已知业务与依赖风险，未通过公网部署验收。**

## 项目内容

- 普通用户：注册登录、物品浏览与发布、租借申请/订单、模拟钱包、消息与个人资料。
- 管理员：用户/物品审核、封禁、争议处理与管理信息。
- 工程结构：Gateway统一入口，认证、物品、租借、钱包、消息、管理员服务分离；跨服务通过Feign及消息事件协作。
- 安全迭代：可信内部凭据、网关身份头清洗、真实Actuator边界回归、管理员歧义路径拒绝、刷新令牌原子消费、钱包更新冲突回滚。

## 技术栈与结构

Java17、Spring Boot3.2.9、Spring Cloud/Alibaba、MyBatis-Plus、MySQL、Redis、RabbitMQ、Nacos；两端使用React18、TypeScript、Vite、Vitest和Playwright。

```text
backend/          common + gateway/auth/item/rental/wallet/message/admin
frontend/         普通用户端
admin-frontend/   管理员端
scripts/          检查及演示辅助脚本
docs/             架构、API、数据库及验收设计
.github/          敏感内容检查、手动完整安全检查
```

入口文档：[架构与服务](docs/02-architecture-and-services.md)、[API契约](docs/03-api-contract.md)、[数据库设计](docs/04-database-design.md)、[文档索引](docs/README.md)。

## 本地运行

准备JDK17、Maven、Node22，以及本机独立的MySQL8、Redis、Nacos、RabbitMQ。应用默认端口：Gateway8080，各业务服务8081–8086，普通端5173、管理端5174。

1. 使用专门的演示数据库实例；`backend/scripts/init.sql`会建表并初始化演示账号，不要对重要数据库直接执行。
2. 在终端或IDE环境设置 `MYSQL_PASSWORD`、`SR_JWT_SECRET`（至少32字节的独立随机值）、`SR_INTERNAL_TOKEN`（另一独立随机值）。`.env.example`只提供变量名，Spring不会自动加载该文件。所有后端必须共享一致内部凭据，不提供给浏览器。
3. IDEA导入 `backend/pom.xml`，以JDK17启动六个业务服务，再启动Gateway；Maven方式在 `backend/` 下运行 `mvn spring-boot:run -pl auth-service -DskipTests`，其余模块名替换为item-service、rental-service、wallet-service、message-service、admin-service、gateway。
4. 两个前端目录分别运行 `npm ci`、`npm run dev`。

数据库初始化示例：在仓库根目录运行 `mysql -h 127.0.0.1 -u root -p < backend/scripts/init.sql`，密码使用交互输入

业务服务监听与Nacos注册地址默认loopback。跨机部署需同时配置 `SR_SERVICE_BIND_ADDRESS`、`SR_SERVICE_ADVERTISED_IP`，并配置网络访问控制；不要只把业务端口直接暴露到公网。

## 验证与公开检查

2026-10-02在隔离环境对本版相同应用源码验证：后端454项、普通端81项、管理端47项、安全脚本6项测试通过；两端构建和3项页面冒烟通过。没有连接真实用户数据库，未完成完整业务E2E。测试数据与测试凭据是虚构fixture。

首次安装审查工具：Python3.13下执行 `python3 scripts/security_setup.py`。完整检查入口：`./scripts/security-check.sh`。它会运行Semgrep、DeepSec本地L1/L2、依赖扫描、当前内容及历史秘密扫描、测试和构建；已知依赖漏洞仍会使完整检查返回非零。

GitHub push/PR只运行已提交内容与全部可达历史的敏感内容检查；完整安全工作流手动触发。提交前的未提交内容仍须检查，不能只用历史模式代替当前文件检查。报告目录被Git忽略，不上传原始扫描片段。

## 已知限制与发布范围

重复退款/库存幂等、物品审核状态机、会话即时撤销、WebSocket到期清理、私聊附件授权、上传格式及像素限制、依赖升级仍需完善，详见[安全说明](SECURITY.md)。