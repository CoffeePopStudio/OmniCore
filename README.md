# OnmiCore

> [中文文档](README_ZH.md)

A comprehensive Paper plugin for logging block operations, container access, inventory changes, and more — with **Git-style timeline rollback**, **async rollback with real-time progress**, and a built-in **Web management panel**.

## Features

- **Full logging** — Block place/break, container (chest/furnace/shulker etc.) access, inventory changes (pickup/drop)
- **Git-style timeline rollback** — Roll back the world to any point in time without losing subsequent player operations
- **Async rollback** — Zero server lag during rollback, configurable blocks-per-tick, real-time progress feedback
- **Resumable rollback** — Batch processing with checkpoints; recovering from a server crash mid-rollback is supported
- **Smart dedup** — Placing then immediately breaking at the same spot within 3 seconds is automatically ignored
- **Full NBT preservation** — Complete BlockState + NBT data (zlib-compressed), exact restoration of command blocks, signs, etc.
- **Web panel** — Built-in HTTP server (Javalin) with a modern Vue 3 SPA for querying and management
- **Dual database** — SQLite out of the box, MySQL for larger servers
- **i18n** — Built-in Chinese and English, extensible framework

## Commands

| Command | Description |
|---|---|
| `/oc inspect` | Toggle inspect mode, click a block to view history |
| `/oc query <time> [filters...]` | Query history records |
| `/oc rollback <time> [filters...]` | Preview a rollback |
| `/oc rollback confirm` | Execute the pending rollback |
| `/oc rollback cancel` | Cancel the pending rollback |
| `/oc restore <rollbackID>` | Undo a rollback |
| `/oc web` | Generate a Web panel bind link |
| `/oc purge <days>` | Manually purge old data |
| `/oc status` | View plugin status |
| `/oc reload` | Reload configuration |
| `/oc migrate-world <old> <new>` | Migrate world name in database |

### Query filters

```
/oc query 30m player:Steve world:world radius:50 ~ ~ ~ type:diamond_ore
```

- Time: `30m` (30 min) / `1h` (1 hour) / `7d` (7 days)
- `player:` — Filter by player name
- `world:` — Filter by world
- `radius:` — Filter by radius (requires coordinates)
- `type:` — Filter by block type

## Permissions

| Node | Description |
|---|---|
| `onmicore.use` | Use `/oc` commands |
| `onmicore.web.view` | Web panel: view logs (read-only) |
| `onmicore.web.rollback` | Web panel: execute rollbacks |
| `onmicore.web.admin` | Web panel: admin access |

## Configuration

```yaml
language: zh   # zh / en

database:
  type: SQLite  # SQLite or MySQL
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

## Installation

1. Drop `OnmiCore-{version}.jar` into your `plugins/` folder
2. Start the server — config and SQLite database are auto-generated
3. Use `/oc` commands or open the Web panel

## Build

```bash
./gradlew build
```

Output: `build/libs/OnmiCore-{version}.jar`

## Tech Stack

| Layer | Technology |
|---|---|
| Server | Paper 1.21.11 (Java 21) |
| Build | Gradle + Shadow |
| Database | HikariCP + SQLite / MySQL |
| HTTP Server | Javalin (embedded) |
| Cryptography | bcrypt + AES + JWT |
| Frontend | Vue 3 + Vite + Naive UI |

## Roadmap

- **Phase 1** ✅ Block logging + Container + Inventory + Git rollback + Web panel
- **Phase 2** 🔲 Vault economy + Death drops + Chat/command logging
- **Phase 3** 🔲 Land claim changes (Residence / GriefPrevention / Lands)
