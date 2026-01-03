package top.e404.estats

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import net.bytebuddy.agent.ByteBuddyAgent
import org.bstats.velocity.Metrics
import org.slf4j.Logger
import org.springframework.expression.ParseException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import top.e404.eplugin.EPluginVelocity
import top.e404.estats.command.Commands
import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.config.EventListenerConfig
import top.e404.estats.common.listener.EventListenerManager
import top.e404.estats.config.Config
import top.e404.estats.config.Lang
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED")
open class EStatsVelocity @Inject constructor(
    server: ProxyServer,
    logger: Logger,
    container: PluginContainer,
    @DataDirectory dataDir: Path,
    metricsFactory: Metrics.Factory
) : EPluginVelocity(server, logger, container, dataDir, metricsFactory) {
    override val bstatsId = 28691
    override var debug: Boolean
        get() = Config.config.debug
        set(value) {
            Config.config.debug = value
        }
    override val langManager by lazy { Lang }

    init {
        PL = this
        EStatsCommon.instance = object : EStatsCommon {
            override val config get() = Config.config
            override val dataFolder = dataDir.toFile()
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
                setVariable("server", server)
                setVariable("now", System.currentTimeMillis())
                for ((k, v) in Config.config.variables) {
                    setVariable(k, v)
                }
            }

            override fun getClassLoader(plugin: String): ClassLoader? {
                return server.pluginManager.getPlugin(plugin)
                    .getOrNull()
                    ?.instance
                    ?.getOrNull()
                    ?.javaClass
                    ?.classLoader
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
                server.eventManager.register(
                    PL,
                    cls as Class<*>,
                    executor::onEvent,
                )
                return executor
            }

            override fun unregisterEventHandler(handler: EventListenerManager.EventListener<*>) {
                server.eventManager.unregister(this, handler::onEvent)
            }
        }
        ByteBuddyAgent.install()
    }

    @Subscribe
    fun onEnable(event: ProxyInitializeEvent) {
        bstats()
        Lang.load(null)
        Config.load(null)
        Commands.register()

        EStatsCommon.instance.load()
    }

    @Subscribe
    fun onDisable(event: ProxyShutdownEvent) {
        EStatsCommon.instance.stop()
        server.scheduler.tasksByPlugin(this).forEach { it.cancel() }
    }
}

lateinit var PL: EStatsVelocity
    private set
