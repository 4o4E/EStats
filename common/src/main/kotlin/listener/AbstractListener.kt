package top.e404.estats.common.listener

import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.db.DB

abstract class AbstractListener(
    val sqlTemplate: String
) {
    companion object {
        val sqlTemplateRegex = Regex("\\$\\{(?<paramName>[^}]+)}")
    }

    val sqlArgs: List<String>
    val sql: String

    init {
        val params = mutableListOf<String>()
        sql = sqlTemplateRegex.replace(sqlTemplate) {
            val paramName = it.groups["paramName"]!!.value
            params.add(paramName)
            "?"
        }
        sqlArgs = params
    }

    protected fun save(result: Any?) {
        val result = result.let {
            it as? List<*> ?: listOf(it)
        }.map {
            it as? Map<*, *> ?: error("事件监听器参数表达式必须返回 Map 类型或 Map 类型的列表")
        }
        // todo 队列
        EStatsCommon.instance.runTaskAsync {
            try {
                DB.useDb { connection ->
                    connection.prepareStatement(sql).use { ps ->
                        for (map in result) {
                            EStatsCommon.instance.debug { "执行sql: $sql\n参数: ${sqlArgs.joinToString(", ") { map[it].toString() }}" }
                            for ((index, arg) in sqlArgs.withIndex()) {
                                ps.setObject(index + 1, map[arg])
                            }
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            } catch (e: Exception) {
                EStatsCommon.instance.warn("事件监听器执行sql失败: $sqlTemplate", e)
            }
        }
    }
}