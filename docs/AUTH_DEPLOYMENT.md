# 登录认证生产发布清单

密码找回与 Google 登录由独立功能开关控制。首轮部署数据库迁移和应用代码时保持两个开关关闭；邮箱密码登录不受影响。

## 1. 数据库预检

在执行 `V007__authentication_upgrades.sql` 前，检查是否存在只因大小写或首尾空格不同而重复的邮箱：

```sql
SELECT lower(btrim(email)) AS normalized_email, array_agg(id ORDER BY id) AS user_ids
FROM users
GROUP BY lower(btrim(email))
HAVING count(*) > 1;
```

查询必须返回空结果。若发现重复账号，应先人工确认需要保留的账号与游戏数据；迁移会主动失败，不会静默合并。

## 2. Resend 与邮件域名

1. 在 Resend 验证 `mail.anguy.dev`，按控制台提供的记录配置 SPF 和 DKIM，并为 `anguy.dev` 配置 DMARC。
2. 确认 `support@anguy.dev` 能正常收信。
3. 生成仅供本服务使用的 Resend API Key。SMTP 使用 `smtp.resend.com:587`、用户名 `resend` 和 STARTTLS。
4. 默认发件人为 `Flip Da Table <no-reply@mail.anguy.dev>`；测试纯文本和 HTML 两种邮件内容以及 30 分钟链接。

## 3. Google Web Client

1. 在 Search Console 验证 `anguy.dev`。
2. 创建 Web application Client ID，Authorized JavaScript origin 填写 `https://game.anguy.dev`。
3. Authorized redirect URI 填写 `https://game.anguy.dev/api/auth/google/callback`。
4. OAuth 主页使用 `https://game.anguy.dev/login`，隐私政策使用 `https://game.anguy.dev/privacy`，发布 External 品牌配置。
5. 不创建或配置 Client Secret；应用只验证 GIS ID token，不保存 Google access/refresh token，也不启用 One Tap。

## 4. 生产环境变量

```dotenv
APP_PUBLIC_URL=https://game.anguy.dev
APP_JWT_SECRET=<至少 32 字节随机秘密>
POSTGRES_PASSWORD=<独立生成的强随机数据库口令>
APP_SUPPORT_EMAIL=support@anguy.dev
APP_MAIL_FROM=Flip Da Table <no-reply@mail.anguy.dev>
RESEND_API_KEY=<Resend API Key>
GOOGLE_CLIENT_ID=<Google Web Client ID>
APP_AUTH_PASSWORD_RESET_ENABLED=false
APP_AUTH_GOOGLE_ENABLED=false
```

秘密只应由部署平台注入，不要写入 Git。`GET /api/auth/capabilities` 只公开功能状态、Google Client ID/login URI 和联系邮箱。

## 5. 分阶段启用与验证

1. 功能关闭时部署并确认邮箱密码注册、登录和游戏流程正常。
2. 将 `APP_AUTH_PASSWORD_RESET_ENABLED=true`，验证未知邮箱与真实邮箱都返回相同 `202` 响应；确认邮件链接不会出现在 Nginx 请求日志，使用一次后失效，修改密码后旧 JWT 被拒绝。
3. 将 `APP_AUTH_GOOGLE_ENABLED=true`，分别验证新账号、Gmail/Workspace 自动绑定、第三方邮箱冲突时的密码绑定、错误/过期交换码与五次密码失败锁定。
4. 检查 `/privacy`、移动端 390×844、键盘焦点，以及 GIS 脚本不可用时邮箱密码登录仍然可用。
5. 监控 `auth.password_reset.*`、`auth.google.*` 指标和不含邮箱、令牌、密码的错误日志。

若异常，只需关闭对应功能开关并重新部署；现有邮箱密码认证继续工作。
