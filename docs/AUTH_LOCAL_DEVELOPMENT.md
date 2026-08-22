# 本地认证开发

本地 Google 登录、密码重置邮件和 PostgreSQL 使用开发配置，与 Droplet 生产配置分离。

## 1. 环境变量

复制 `backend/.env.example` 为 `backend/.env`，保留或生成至少 32 字节的 `APP_JWT_SECRET`，并填写开发环境 Google Web Client ID。`.env` 已被 Git 忽略。

Google Web Client 需要配置：

- Authorized JavaScript origins: `http://localhost`、`http://localhost:5173`
- Authorized redirect URI: `http://localhost:5173/api/auth/google/callback`

本地页面必须使用 `http://localhost:5173`，不要混用 `127.0.0.1`。

## 2. 启动依赖

```powershell
docker compose --env-file backend/.env -f docker-compose.yml -f docker-compose.dev.yml up -d postgres mailpit
```

PostgreSQL 监听 `127.0.0.1:5432`。Mailpit SMTP 监听 `127.0.0.1:1025`，收件箱位于 `http://localhost:8025`。

## 3. 启动应用

```powershell
cd backend
.\gradlew.bat bootRun
```

另开一个终端：

```powershell
cd frontend
npm run dev -- --host localhost
```

打开 `http://localhost:5173/login`。`GET http://localhost:8080/api/auth/capabilities` 应显示密码重置和 Google 登录均已启用。

## 4. 停止依赖

```powershell
docker compose --env-file backend/.env -f docker-compose.yml -f docker-compose.dev.yml stop postgres mailpit
```

本地 Mailpit 邮件是临时数据；生产环境继续使用 Resend、HTTPS 域名和 `docs/AUTH_DEPLOYMENT.md` 中的分阶段开关流程。

