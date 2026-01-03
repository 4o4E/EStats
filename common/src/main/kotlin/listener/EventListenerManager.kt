package top.e404.estats.common.listener

import org.springframework.expression.Expression
import org.springframework.util.ClassUtils
import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.config.EventListenerConfig


private val classCache = mutableMapOf<String, Class<*>>()
private fun getClass(name: String): Class<*> {
    return classCache.getOrPut(name) {
        ClassUtils.forName(name, ClassUtils.getDefaultClassLoader())
    }
}

object EventListenerManager {
    private val listeners = mutableListOf<EventListener<*>>()
    fun register(config: EventListenerConfig) {
        val cls = getClass(config.event)

        EStatsCommon.instance.registerEventHandler(cls, config)?.let { listeners.add(it) }
    }

    fun load() {
        for (config in EStatsCommon.instance.config.event) register(config)
    }

    fun stop() = listeners.run {
        forEach {
            EStatsCommon.instance.unregisterEventHandler(it)
        }
        clear()
    }

    data class EventListener<T : Any>(
        val config: EventListenerConfig,
        val cls: Class<T>,
        val condition: Expression?,
        val param: Expression,
    ) : AbstractListener(config.save) {
        fun onEvent(event: Any) {
            EStatsCommon.instance.debug { "监听事件触发: ${cls.simpleName}" }
            try {
                val ev = cls.cast(event)
                val context = EStatsCommon.instance.getCtx().apply {
                    setVariable("event", ev)
                }
                val cond = condition?.getValue(context, Boolean::class.java) ?: true
                if (!cond) return
                val result = param.getValue(context)
                save(result)
            } catch (e: Exception) {
                EStatsCommon.instance.warn("事件监听时出现异常: ${config.event}", e)
            }
        }
    }
}


