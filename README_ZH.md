# Flip Da Table

简体中文 | [English](README.md)

一个基于 Spring Boot + React 的在线 UNO（及可扩展更多桌游/会话）的示例项目。后端提供认证、会话管理与 UNO 规则运行时；前端提供登录/注册、创建加入对局、实时观战与操作界面。通过 Docker 及 `docker-compose` 可一键部署（PostgreSQL + 后端 + 前端 Nginx）。

## 技术栈概览
- 后端：Spring Boot 3 (Web, Security, Data JPA, Validation, Actuator, WebSocket/SSE 用 Web 模块)、Flyway、JWT (jjwt)、Lombok
- 数据库：PostgreSQL + Flyway 版本化迁移 (`backend/src/main/resources/db/migration`)
- 前端：React 19, React Router v7, Vite, Tailwind CSS (v4) + DaisyUI
- 实时更新：服务器端事件 (Server-Sent Events, SSE) `/api/games/uno/{gameId}/stream`
- 构建与运行：Gradle (Java 17 toolchain), Vite；Docker 镜像 `anguy491/flip-backend` & `anguy491/flip-frontend`

## 项目目录结构（精简展示）
```
backend/
  build.gradle.kts        # Gradle 配置 (Spring Boot 3 + Flyway + JWT 等依赖)
  src/main/java/com/flip/backend/
    api/                  # REST + SSE 控制器 (Auth, Session, Game, Uno)
    config/               # Spring 安全 / CORS / 序列化等配置 (如存在)
    game/                 # 通用游戏引擎抽象 (phase, event, board, player)
      engine/event/       # GameEvent, EventQueue
      engine/phase/       # Phase, RuntimePhase 抽象
      entities/           # Board<P>, Player 基础模型
    uno/                  # UNO 专属实现（实体 + 事件 + 视图）
      entities/           # UnoCard, UnoDeck, UnoPlayer, UnoBoard
      engine/             # UnoGameRegistry, 运行期逻辑
        event/            # UnoPlayCardEvent, UnoDrawCardEvent 等
        phase/            # UnoRuntimePhase (核心状态机)
        view/             # UnoView / UnoBoardView / UnoPlayerView (对前端的投影)
  src/main/resources/
    application.yml       # Spring 配置 (可被环境变量覆盖)
    db/migration/         # Flyway SQL 迁移脚本

frontend/
  package.json            # React/Vite/依赖版本
  src/
    api/                  # fetch 封装 (auth, sessions, uno)
    context/              # 全局 AuthContext
    hooks/                # useUnoGame, usePlayAnimations
    components/           # 基础输入/布局组件
      uno/                # UNO 专用展示与交互组件
    pages/                # Login / Lobby / PlayScreen / 等页面
    assets/               # 图片资源
  vite.config.js          # 构建配置
  tailwind.config.js      # Tailwind v4 配置

nginx/
  nginx.conf              # 反向代理 & 静态文件 & 健康检查

docker-compose.yml        # 一键编排 Postgres + Backend + Frontend
```

## 后端概要
- 主要包：`com.flip.backend.api` (REST/SSE 控制器), `...uno.engine` (UNO 规则 & 运行时), 认证/会话相关代码（用户、角色、Session/Game 管理）
- 认证：`/api/auth/register` & `/api/auth/login`，返回含 JWT token（同时支持 Cookie 方式，前端 `fetch` 使用 `credentials: 'include'`）。
- 会话 / 游戏：`/api/sessions` 系列端点（创建、查询、加入），UNO 游戏在 `/api/games/uno` 下：
  - `GET /api/games/uno/{gameId}/view?viewerId=...` 获取针对特定玩家视角的状态（自己的手牌包含在内，其他玩家只含数量）。
  - `POST /api/games/uno/{gameId}/commands` 发送指令，Body: `{ type, playerId, color?, value? }`，返回是否应用及新的视图。
  - `GET /api/games/uno/{gameId}/stream` SSE 推送通用视图（无私有手牌）。
- 迁移：`V001__init.sql` 起始到后续表（用户角色、sessions、games、state_json 等）。
- 配置：`application.yml` 支持通过环境变量覆盖数据源与 JWT 密钥 (`APP_JWT_SECRET`)。

## 前端概要
- 路由页面：`pages/` (Login, Register, Dashboard, Lobby, PlayScreen, SessionSummary)。
- 业务上下文：`context/AuthContext.jsx` 管理用户登录状态与 token。
- API 封装：`src/api/*.js` 对应 `auth`, `sessions`, `uno`；统一 `fetch` 基础路径 `/api`。
- UNO 逻辑 Hook：`hooks/useUnoGame.js` 管理轮询/流事件与本地状态（含动画 hook）。
- 组件：`components/uno/*` 提供卡牌、事件日志、手牌区等 UI。

## UNO 实时交互流程
1. 前端进入对局页面：先 `GET /view` 拉取首次视图。
2. 并行建立 SSE：`EventSource('/api/games/uno/{gameId}/stream')` 接收通用广播（用于更新公共局面 / 事件日志）。
3. 玩家操作：通过 `POST /commands` 发送，例如出牌、抽牌、选择颜色（WILD）。
4. 后端执行规则引擎，广播最新公共视图，客户端合并更新，并在需要时提示选择颜色。

## Docker 部署
`docker-compose.yml` 中包含：
- postgres: 数据库，持久卷 `pgdata`
- backend: 依赖数据库健康检查后启动，暴露 8080（内部，与前端 Nginx 同网络）
- frontend: 提供 80/443；Nginx 反向代理到后端 (具体 proxy 规则见 `nginx.conf`)，挂载证书 `/etc/letsencrypt`

启动（确保已设置环境变量 `APP_JWT_SECRET`）：
```bash
# Windows PowerShell 示例
$env:APP_JWT_SECRET = 'your-strong-secret'
docker compose up -d --pull always
```

## 设计亮点（后端 UNO 引擎）
核心目标：在通用“游戏引擎”与具体 UNO 规则之间建立清晰分层，最小化耦合，方便未来扩展其他回合制游戏。

1. Phase 抽象
  - `Phase` 定义生命周期入口 `enter()`；`RuntimePhase` 扩展出运行循环 `run()`；UNO 以 `UnoRuntimePhase` 具体化回合推进 / 事件处理 / 胜负判定。
2. Board / Player 通用环形座位模型
  - `Board<P extends Player>` 用双向环形链表保存座位：支持 `reverse()` 方向切换、`step(k)` 多步跳转、`snapshotOrder()` 快照。
  - UNO 通过子类 `UnoBoard` 增加活动颜色 / 顶牌概念但复用遍历与方向反转逻辑（例如 REVERSE 仅调用 `reverse()`）。
3. 事件驱动 (EventQueue + GameEvent)
  - 所有动作被封装为 `GameEvent`（如 `UnoPlayCardEvent`, `UnoDrawCardEvent`），置于 `EventQueue` 统一调度，隔离“判定合法性”与“执行副作用”，方便将来追加日志、回放、审计或撤销机制。
4. 规则与状态解耦
  - `UnoRuntimePhase` 不直接操作手牌集合细节，而是通过事件（play/draw）和 board/deck API；惩罚叠加逻辑（DRAW_TWO / WILD_DRAW_FOUR）集中在 runtime，事件只报告 `penaltyAmount`，实现清晰单一职责。
5. 视图层投影
  - `UnoView` / `UnoBoardView` / `UnoPlayerView` 提供“透视”模型：调用 `buildView(viewerId)` 时仅暴露当前玩家自己的完整手牌，其他玩家只给 `handSize`，为后端再封装 JSON 提供稳定契约，减少控制器逻辑复杂度。
6. 可插入 Turn Listener
  - `UnoRuntimePhase.setTurnListener` 允许在每次回合推进后注入广播逻辑（SSE 推送），而不将传输层概念硬编码进引擎核心。
7. Action Log 环形裁剪
  - 运行时维护固定容量 `actionLog`（50 条），序列号递增，便于前端按 `lastEventSeq` 增量对齐；未来可替换为持久化实现而无需改动前端。
8. 可扩展点
  - 新卡牌效果：仅需扩展 `UnoPlayCardEvent.execute()` 中 switch 或拆分策略类。
  - 新游戏：重用 `Board` / `Phase` / `GameEvent` 基础，新增自己的 `RuntimePhase` 与 `*PlayEvent` 即可。

9. 典型关系 UML（文字化）：
  - Phase <- RuntimePhase <- UnoRuntimePhase
  - Board<P> <- UnoBoard
  - Player <- (UnoPlayer / UnoBot)
  - GameEvent <- (UnoPlayCardEvent / UnoDrawCardEvent ...)
  - UnoRuntimePhase 聚合 UnoBoard + UnoDeck + EventQueue

该层次结构遵循 SRP（单一职责）、OCP（通过新增事件/phase 扩展，而不是修改核心）、LoD（最少知识：控制器仅依赖视图投影而非底层实体）、以及可测试性（事件与 runtime 可单独模拟）。

---
本 README 仅概述核心结构，细节可直接阅读源码。祝使用愉快。
