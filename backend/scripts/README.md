# Backend Scripts

- `init.sql`: 一次性创建 6 个业务数据库与全部表、初始化数据。可重复执行（先 DROP 再 CREATE）。
- 执行命令（本机 MySQL 8）：
  ```bash
  MYSQL_PASSWORD=${MYSQL_PASSWORD:?Set MYSQL_PASSWORD in your local environment}
  mysql -u root -p"$MYSQL_PASSWORD" < backend/scripts/init.sql
  ```
- 默认管理员账号：`admin / 123456`，密码使用 BCrypt 加密。
- BCrypt 哈希可通过 `backend/auth-service` 的 `AdminPasswordBcryptTest` 校验。
