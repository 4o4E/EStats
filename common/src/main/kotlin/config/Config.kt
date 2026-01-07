package top.e404.estats.common.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.e404.estats.common.queue.LocalQueueSink
import top.e404.estats.common.queue.NoQueueSink

@Serializable
data class ConfigData(
    var debug: Boolean,
    val variables: Map<String, String>,
    val databases: Map<String, DatabaseConfig>,
    val queue: QueueConfig,
    val event: List<EventListenerConfig>,
    val func: List<FuncListenerConfig>,
    val schedule: List<ScheduleConfig>,
)

@Serializable
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String,
    val pool: PoolConfig,
    val init: List<String>,
)

@Serializable
data class PoolConfig(
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val idleTimeout: Long,
    val maxLifetime: Long,
    val connectionTimeout: Long,
)

@Serializable
data class QueueConfig(
    val type: QueueType,
    val local: LocalQueueConfig,
) {
    val current by lazy {
        when (type) {
            QueueType.NO -> NoQueueSink
            QueueType.LOCAL -> LocalQueueSink
        }
    }
}

@Serializable
enum class QueueType {
    NO,
    LOCAL,
}

/**
 * 本地队列配置
 * @param capacity 队列容量
 * @param interval 处理间隔，单位毫秒
 */
@Serializable
data class LocalQueueConfig(
    val capacity: Int,
    val interval: Long,
    @SerialName("batch_threshold")
    val batchThreshold: Int,
    @SerialName("time_threshold")
    val timeThreshold: Int,
    @SerialName("drop_when_full")
    val dropWhenFull: Boolean,
)


/**
 * 事件监听器
 * @param event 监听的事件全限定名
 * @param condition 监听器触发条件el表达式
 * @param param 监听器参数el表达式
 * @param database 监听器使用的数据库配置名称
 * @param save 监听器结果保存sql
 */
@Serializable
data class EventListenerConfig(
    val event: String,
    val condition: String? = null,
    val param: String,
    val database: String,
    val save: String,
)

/**
 * 方法监听器
 * @param plugin 监听的方法所属插件id
 * @param method 监听的方法的el表达式
 * @param condition 监听器触发条件el表达式
 * @param param 监听器参数el表达式
 * @param database 监听器使用的数据库配置名称
 * @param save 监听器结果保存sql
 */
@Serializable
data class FuncListenerConfig(
    val plugin: String,
    val method: String,
    val condition: String? = null,
    val param: String,
    val database: String,
    val save: String,
)

/**
 * 方法监听器
 * @param name 任务名称
 * @param cron 任务执行的cron表达式
 * @param sync 是否同步
 * @param condition 监听器触发条件el表达式
 * @param param 监听器参数el表达式
 * @param database 监听器使用的数据库配置名称
 * @param save 监听器结果保存sql
 */
@Serializable
data class ScheduleConfig(
    val name: String,
    val cron: String,
    val sync: Boolean = true,
    val condition: String? = null,
    val param: String,
    val database: String,
    val save: String,
)