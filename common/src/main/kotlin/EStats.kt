package top.e404.estats.common

import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import top.e404.estats.common.config.ConfigData
import top.e404.estats.common.config.EventListenerConfig
import top.e404.estats.common.db.DB
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
    fun runTask(task: () -> Unit)
    fun runTaskAsync(task: () -> Unit)
    fun runTaskLater(delayMillis: Long, task: () -> Unit)
    fun getCtx(root: Any? = null): StandardEvaluationContext
    fun getClassLoader(plugin: String): ClassLoader?

    fun <T : Any> registerEventHandler(event: Class<T>, config: EventListenerConfig): EventListenerManager.EventListener<T>?
    fun unregisterEventHandler(handler: EventListenerManager.EventListener<*>)

    fun load() {
        DB.load()
        FuncListenerManager.load()
        EventListenerManager.load()
        ScheduleManager.load()
    }

    fun stop() {
        FuncListenerManager.stop()
        EventListenerManager.stop()
        ScheduleManager.stop()
        DB.stop()
    }

    fun reload() {
        stop()
        load()
    }
}