# [EStats](https://github.com/4o4E/EStats)

[![Release](https://img.shields.io/github/v/release/4o4E/EStats?label=Release)](https://github.com/4o4E/EStats/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/4o4E/EStats/total?label=Download)](https://github.com/4o4E/EStats/releases)

通过spel表达式获取事件中的数据并存入数据库, 支持多数据源, 支持消息队列

[![bstats](https://bstats.org/signatures/bukkit/EStats.svg)](https://bstats.org/plugin/bukkit/EStats)
[![bstats](https://bstats.org/signatures/velocity/EStatsVelocity.svg)](https://bstats.org/plugin/velocity/EStatsVelocity)

## 内置变量

- [Bukkit] 事件: `event` 当前触发的事件 对应的类型为具体事件类型 spel使用示例: `event.player.name`
- [Bukkit] 设置Meta: `Util.setMeta(m: Metadatable, key: String, value: String): String` spel使用示例: `Util.setMeta(event.player, 'key', 'value')`
- [Bukkit] 获取Meta: `Util.getMeta(m: Metadatable, key: String): String?` spel使用示例: `Util.getMeta(event.player, 'key')`
- [Velocity] 事件: `event` 当前触发的事件 对应的类型为具体事件类型 spel使用示例: `event.player.username`
- [Velocity] Server: `server` 当前的VelocityServer 对应类型为 `com.velocitypowered.api.proxy.ProxyServer` spel使用示例: `server.allPlayers.size()`
- [通用] now: `now` 为当前毫秒时间戳 spel使用示例: `now`

[spigot示例配置](spigot/src/main/resources/config.yml)

[velocity示例配置](velocity/src/main/resources/config.yml)