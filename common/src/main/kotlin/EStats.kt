package top.e404.estats.common

import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import top.e404.estats.common.config.ConfigData
import top.e404.estats.common.config.EventListenerConfig
import top.e404.estats.common.db.DbManager
import top.e404.estats.common.listener.EventListenerManager
import top.e404.estats.common.listener.FuncListenerManager
import top.e404.estats.common.listener.ScheduleManager
import java.io.File

interface EStatsCommon {
    companion object {
        lateinit var instance: EStatsCommon
    }
    val config: ConfigData
    val dataFolder: File
    val parser: SpelExpressionParser
    fun debug(message: String)
    fun debug(message: () -> String)
    fun warn(message: String, throwable: Throwable? = null)

    interface TaskHandler {
        fun cancel()
    }

    fun runTask(task: () -> Unit): TaskHandler
    fun runTaskAsync(task: () -> Unit): TaskHandler
    fun runTaskTimerAsync(delay: Long, period: Long, task: () -> Unit): TaskHandler
    fun runTaskLater(delayMillis: Long, task: () -> Unit): TaskHandler

    fun getCtx(root: Any? = null): StandardEvaluationContext
    fun getClassLoader(plugin: String): ClassLoader?

    fun <T : Any> registerEventHandler(event: Class<T>, config: EventListenerConfig): EventListenerManager.EventListener<T>?
    fun unregisterEventHandler(handler: EventListenerManager.EventListener<*>)

    fun load() {
        DbManager.loadAll()
        FuncListenerManager.load()
        EventListenerManager.load()
        ScheduleManager.load()
        config.queue.current.start()
    }

    fun stop() {
        config.queue.current.shutdown()
        FuncListenerManager.stop()
        EventListenerManager.stop()
        ScheduleManager.stop()
        DbManager.stopAll()
    }
}