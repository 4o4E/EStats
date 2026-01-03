package top.e404.estats.common.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import top.e404.estats.common.EStatsCommon
import java.sql.Connection

object DB {
    @Volatile
    private lateinit var datasource: HikariDataSource

    // 延迟关闭旧数据源
    fun stop() {
        if (::datasource.isInitialized) {
            val ds = datasource
            EStatsCommon.instance.runTaskLater(200) {
                ds.close()
            }
        }
    }

    fun load() {
        val config = EStatsCommon.instance.config.database
        datasource = HikariConfig().apply {
            jdbcUrl = config.url
            driverClassName = config.driver
            username = config.username
            password = config.password
            isAutoCommit = true
            maximumPoolSize = config.pool.maximumPoolSize
            minimumIdle = config.pool.minimumIdle
            idleTimeout = config.pool.idleTimeout
            maxLifetime = config.pool.maxLifetime
            connectionTimeout = config.pool.connectionTimeout
            poolName = "EStatsHikariCP"
        }.let(::HikariDataSource)
        // init
        useDb {
            it.createStatement().use { stmt ->
                for (sql in config.init) {
                    EStatsCommon.instance.debug { "执行初始化sql: $sql" }
                    stmt.execute(sql)
                }
            }
        }
    }

    fun <T> useDb(block: (Connection) -> T) = datasource.connection.use(block)

}