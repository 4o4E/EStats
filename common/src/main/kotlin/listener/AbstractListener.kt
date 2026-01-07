package top.e404.estats.common.listener

import top.e404.estats.common.EStatsCommon

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

    protected fun save(database: String, result: Any?) {
        val result = result.let {
            it as? List<*> ?: listOf(it)
        }.map {
            it as? Map<*, *> ?: error("事件监听器参数表达式必须返回 Map 类型或 Map 类型的列表")
        }
        EStatsCommon.instance.config.queue.current.push(
            database,
            sql,
            result.map { map ->
                sqlArgs.map { argName ->
                    map[argName]
                }
            }
        )
    }
}