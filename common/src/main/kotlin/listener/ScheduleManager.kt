package top.e404.estats.common.listener

import it.sauronsoftware.cron4j.Scheduler
import org.springframework.expression.Expression
import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.config.ScheduleConfig

object ScheduleManager {
    private val tasks = mutableListOf<ScheduleListener>()
    private val cronScheduler = Scheduler()
    fun register(config: ScheduleConfig) {
        val condition = try {
            config.condition?.let { EStatsCommon.instance.parser.parseExpression(it) }
        } catch (e: Exception) {
            EStatsCommon.instance.warn("定时任务监听器 condition 表达式解析失败: ${config.condition}", e)
            return
        }
        val param = try {
            EStatsCommon.instance.parser.parseExpression(config.param)
        } catch (e: Exception) {
            EStatsCommon.instance.warn("定时任务监听器 params 表达式解析失败: ${config.param}", e)
            return
        }
        EStatsCommon.instance.debug { "注册定时任务: ${config.name}(${config.cron})" }
        val listener = ScheduleListener(config, condition, param)
        tasks.add(listener)
        listener.start()
    }

    fun load() {
        cronScheduler.start()
        for (config in EStatsCommon.instance.config.schedule) register(config)
    }

    fun stop() = tasks.run {
        cronScheduler.stop()
        forEach { it.stop() }
        clear()
    }

    data class ScheduleListener(
        val config: ScheduleConfig,
        val condition: Expression?,
        val param: Expression,
    ) : AbstractListener(config.save) {
        var taskId: String? = null

        fun stop() {
            taskId?.let(cronScheduler::deschedule)
        }

        fun start() {
            val runTask = if (config.sync) EStatsCommon.instance::runTask else EStatsCommon.instance::runTaskAsync
            taskId = cronScheduler.schedule(config.cron) {
                runTask { onScheduled() }
            }
        }

        fun onScheduled() {
            EStatsCommon.instance.debug { "定时任务触发: ${config.name}(${config.cron})" }
            try {
                val context = EStatsCommon.instance.getCtx()
                val cond = condition?.getValue(context, Boolean::class.java) ?: true
                if (!cond) return
                val result = param.getValue(context)
                save(config.database, result)
            } catch (e: Exception) {
                EStatsCommon.instance.warn("定时任务执行异常: ${config.param}", e)
            }
        }
    }
}