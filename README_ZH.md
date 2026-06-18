# OnmiCore

> [English](README.md)

全面记录 Minecraft 服务器中方块操作、容器存取、背包变化等事件，支持 **Git 式时间轴回滚** 和 **异步回滚 + 实时进度反馈** 的 Paper 插件。

## 核心特性

- **全面记录** — 方块放置/破坏、容器（箱子/熔炉/潜影盒等）物品存取、玩家背包变化（拾取/丢弃）
- **Git 式时间轴回滚** — 回滚到指定时间点的世界状态，不丢失后续其他玩家的操作
- **异步回滚** — 回滚不卡服，每 tick 处理配置数量的变更，实时显示进度百分比
- **断点续滚** — 批处理 + 检查点机制，回滚过程中服务器崩溃可恢复
- **智能去重** — 3 秒内同一玩家同一位置先放后挖自动忽略，数据库无噪音
- **完整 NBT 记录** — 记录完整的 BlockState + NBT 数据（zlib 压缩），回滚时精确还原命令方块、告示牌等
- **Web 面板** — 内嵌 HTTP 服务器，支持现代化 Web UI 查询与管理
- **多数据库** — SQLite 开箱即用，也可配置 MySQL 应对大型服务器
- **多语言** — 内置中文和英文，框架可扩展

## 命令

| 命令 | 说明 |
|---|---|
| `/oc inspect` | 切换检查模式，点方块查看历史 |
| `/oc query <时间> [条件...]` | 查询历史记录 |
| `/oc rollback <时间> [条件...]` | 预览回滚 |
| `/oc rollback confirm` | 确认执行回滚 |
| `/oc rollback cancel` | 取消回滚 |
| `/oc restore <回滚ID>` | 撤销回滚 |
| `/oc web` | 生成 Web 面板绑定链接 |
| `/oc purge <天数>` | 手动清理旧数据 |
| `/oc status` | 查看插件运行状态 |
| `/oc reload` | 重载配置文件 |
| `/oc migrate-world <旧名> <新名>` | 迁移世界名 |

### 查询条件语法

```
/oc query 30m player:Steve world:world radius:50 ~ ~ ~ type:diamond_ore
```

- 时间: `30m` (30分钟) / `1h` (1小时) / `7d` (7天)
- `player:` — 按玩家名过滤
- `world:` — 按世界过滤
- `radius:` — 按半径过滤（需同时指定坐标）
- `type:` — 按方块类型过滤

## 权限

| 权限节点 | 说明 |
|---|---|
| `onmicore.use` | 使用 `/oc` 命令 |
| `onmicore.web.view` | Web 面板查看日志（只读） |
| `onmicore.web.rollback` | Web 面板执行回滚 |
| `onmicore.web.admin` | Web 面板管理 |

## 配置

```yaml
language: zh   # 语言: zh / en

database:
  type: SQLite  # SQLite 或 MySQL
  mysql:
    host: localhost
    port: 3306
    database: onmicore
    username: root
    password: ""
    pool-size: 10

storage-path: plugins/OnmiCore/data

web-panel:
  enabled: true
  port: 9812
  allowed-ips: []

logging:
  block-place: true
  block-break: true
  container-access: true
  inventory-change: true
  dedup-time-ms: 3000

rollback:
  blocks-per-tick: 80
  require-confirmation: true
  checkpoint-interval: 100

retention:
  mode: age      # none / age / size
  max-days: 90
  max-size-mb: 1024
  evict-ratio: 0.05
```

## 安装

1. 将 `OnmiCore-{version}.jar` 放入 `plugins/` 目录
2. 启动服务器，插件会自动生成配置文件和 SQLite 数据库
3. 通过 `/oc` 命令或 Web 面板使用

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/OnmiCore-{version}.jar`

## 技术栈

| 层 | 技术 |
|---|---|
| 服务器 | Paper 1.21.11 (Java 21) |
| 构建 | Gradle + Shadow |
| 数据库 | HikariCP + SQLite / MySQL |
| HTTP 服务器 | Javalin (内嵌) |
| 密码学 | bcrypt + AES + JWT |
| 前端 | Vue 3 + Vite + Naive UI |

## 路线图

- **阶段一** ✅ 方块记录 + 容器存取 + 背包变化 + Git 式回滚 + Web 面板
- **阶段二** 🔲 Vault 经济记录 + 死亡掉落 + 聊天/命令记录
- **阶段三** 🔲 领地变更记录（Residence / GriefPrevention / Lands）
