# HTTPS配置完整指南

## 前置条件确认
- ✅ DNS已配置: game.anguy.dev → 167.172.77.134
- ✅ 服务器可通过SSH访问
- ✅ 端口80和443在防火墙中开放

## 执行步骤

### 1. 上传更新的代码到服务器
```bash
# 在本地执行 - 提交并推送代码更改
git add .
git commit -m "Add HTTPS support with Let's Encrypt"
git push origin main

# 在服务器上执行 - 拉取最新代码
cd /path/to/your/flip-da-table
git pull origin main
```

### 2. 执行HTTPS设置脚本
```bash
# 复制脚本到服务器并执行
chmod +x setup-https.sh

# ⚠️ 重要：修改脚本中的邮箱地址
nano setup-https.sh
# 将 "your-email@example.com" 替换为你的真实邮箱

# 执行脚本
./setup-https.sh
```

### 3. 手动执行步骤（如果脚本失败）

#### 3.1 安装Certbot
```bash
sudo apt update
sudo apt install -y certbot
```

#### 3.2 停止当前服务
```bash
docker-compose down
```

#### 3.3 获取SSL证书
```bash
sudo certbot certonly --standalone \
  --email zbingkun48@gmail.com \
  --agree-tos \
  --no-eff-email \
  -d game.anguy.dev
```

#### 3.4 设置权限
```bash
sudo chmod -R 755 /etc/letsencrypt
sudo chmod 600 /etc/letsencrypt/live/game.anguy.dev/privkey.pem
```

#### 3.5 重新构建并启动
```bash
docker-compose build frontend
docker-compose up -d
```

### 4. 验证HTTPS配置

#### 4.1 检查证书
```bash
sudo certbot certificates
```

#### 4.2 测试访问
- HTTP重定向: http://game.anguy.dev → https://game.anguy.dev
- HTTPS直接访问: https://game.anguy.dev
- API调用: https://game.anguy.dev/api/...
- WebSocket: wss://game.anguy.dev/ws

#### 4.3 检查容器状态
```bash
docker-compose ps
docker-compose logs frontend
```

### 5. 故障排除

#### 证书获取失败
- 确认DNS解析正确: `nslookup game.anguy.dev`
- 检查端口80是否开放: `sudo ufw status`
- 查看certbot日志: `sudo tail -f /var/log/letsencrypt/letsencrypt.log`

#### 容器启动失败
- 检查证书路径: `ls -la /etc/letsencrypt/live/game.anguy.dev/`
- 查看nginx错误: `docker-compose logs frontend`
- 验证nginx配置: `docker-compose exec frontend nginx -t`

#### WebSocket连接问题
- 确认前端使用wss://而不是ws://
- 检查代理配置中的X-Forwarded-Proto头

### 6. 自动续期设置
```bash
# 添加cron任务自动续期
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet --deploy-hook 'cd /path/to/flip-da-table && docker-compose restart frontend'") | crontab -

# 检查cron任务
crontab -l
```

## 重要注意事项

1. **邮箱地址**: 必须使用真实邮箱，Let's Encrypt会发送续期提醒
2. **防火墙**: 确保端口443开放 `sudo ufw allow 443`
3. **DNS传播**: DNS更改可能需要几分钟到几小时生效
4. **证书有效期**: Let's Encrypt证书90天有效，自动续期设置很重要
5. **备份**: 建议备份 `/etc/letsencrypt` 目录

## 验证清单

- [ ] HTTP自动重定向到HTTPS
- [ ] HTTPS网站正常访问
- [ ] API请求正常工作
- [ ] WebSocket连接正常
- [ ] SSL证书有效（绿锁图标）
- [ ] 自动续期cron任务已设置
