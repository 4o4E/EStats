package top.e404.estats

import net.bytebuddy.agent.ByteBuddyAgent
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.springframework.expression.ParseException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import top.e404.eplugin.EPlugin
import top.e404.estats.command.Commands
import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.config.EventListenerConfig
import top.e404.estats.common.listener.EventListenerManager
import top.e404.estats.config.Config
import top.e404.estats.config.Lang
import top.e404.estats.util.Util
import java.lang.reflect.Method

@Suppress("UNUSED")
open class EStats : EPlugin() {
    override val bstatsId = 28618
    override var debug: Boolean
        get() = Config.config.debug
        set(value) {
            Config.config.debug = value
        }
    override val langManager by lazy { Lang }

    private object VirtualListener : Listener

    init {
        PL = this
        EStatsCommon.instance = object : EStatsCommon {
            override val config get() = Config.config
            override val dataFolder = getDataFolder()
            override val parser = SpelExpressionParser()

            override fun debug(message: String) = PL.debug(message)
            override fun debug(message: () -> String) = PL.debug(true, message)
            override fun warn(message: String, throwable: Throwable?) = PL.warn(message, throwable)
            override fun runTask(task: () -> Unit) {
                PL.runTask { task() }
            }

            override fun runTaskAsync(task: () -> Unit) {
                PL.runTaskAsync { task() }
            }

            override fun runTaskLater(delayMillis: Long, task: () -> Unit) {
                PL.runTaskLater(delayMillis) { task() }
            }

            override fun getCtx(root: Any?) = StandardEvaluationContext(root).apply {
                setVariable("Util", Util)
                setVariable("Bukkit", Bukkit::class.java)
                setVariable("now", System.currentTimeMillis())
                for ((k, v) in Config.config.variables) {
                    setVariable(k, v)
                }
            }

            override fun getClassLoader(plugin: String): ClassLoader? {
                return Bukkit.getPluginManager().getPlugin(plugin)?.javaClass?.classLoader
            }

            private val methodCache = mutableMapOf<Class<*>, Method>()

            override fun <T : Any> registerEventHandler(
                cls: Class<T>,
                config: EventListenerConfig
            ): EventListenerManager.EventListener<T>? {
                val condition = try {
                    config.condition?.let { parser.parseExpression(it) }
                } catch (e: ParseException) {
                    PL.warn("事件监听器注册失败: ${config.event} 的 condition 表达式解析失败", e)
                    return null
                }
                val param = try {
                    config.param.let { parser.parseExpression(it) }
                } catch (e: ParseException) {
                    PL.warn("事件监听器注册失败: ${config.event} 的 params 表达式解析失败", e)
                    return null
                }

                PL.debug { "监听事件: ${cls.simpleName}" }
                val executor = EventListenerManager.EventListener(
                    config,
                    cls,
                    condition,
                    param
                )
                @Suppress("UNCHECKED_CAST")
                Bukkit.getPluginManager().registerEvent(
                    cls as Class<out Event?>,
                    VirtualListener,
                    EventPriority.MONITOR,
                    { _, event -> executor.onEvent(event) },
                    PL
                )
                return executor
            }

            private fun getHandlerListMethod(clazz: Class<*>) = methodCache.getOrPut(clazz) {
                clazz.getDeclaredMethod("getHandlerList").also {
                    it.setAccessible(true)
                }
            }

            override fun unregisterEventHandler(handler: EventListenerManager.EventListener<*>) {
                getHandlerListMethod(handler.cls).invoke(null).let { hl ->
                    (hl as HandlerList).unregister(VirtualListener)
                }
            }
        }
        ByteBuddyAgent.install()
    }

    override fun onEnable() {
        bstats()
        Lang.load(null)
        Config.load(null)
        Commands.register()

        EStatsCommon.instance.load()
    }

    override fun onDisable() {
        EStatsCommon.instance.stop()

        Bukkit.getScheduler().cancelTasks(this)
    }
}

lateinit var PL: EStats
    private set
