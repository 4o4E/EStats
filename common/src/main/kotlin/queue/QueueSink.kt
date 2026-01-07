package top.e404.estats.common.queue

import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.db.DbManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface QueueSink {
    /**
     * 将一个SQL任务推送到队列中
     * @param database 目标数据库名称
     * @param sql 要执行的SQL语句
     * @param lines SQL参数列表列表
     */
    fun push(
        database: String,
        sql: String,
        lines: List<List<Any?>>
    )

    /**
     * 处理队列中积压的所有任务
     */
    fun flush()

    /**
     * 启动队列处理器
     */
    fun start()

    /**
     * 强制刷盘所有数据
     */
    fun shutdown()

    fun executeBatchInsert(dbName: String, items: List<BufferItem>) {
        DbManager[dbName]!!.useDb {
            for ((sql, lines) in items) {
                EStatsCommon.instance.debug { "执行sql: $sql" }
                it.prepareStatement(sql).use { ps ->
                    for (args in lines) {
                        EStatsCommon.instance.debug { "参数: ${args.joinToString(", ")}" }
                        for ((i, arg) in args.withIndex()) {
                            ps.setObject(i + 1, arg)
                        }
                        ps.addBatch()
                    }
                    ps.executeBatch()
                    ps.close()
                }
            }
        }
    }
}

data class BufferItem(
    val sql: String,
    val lines: List<List<Any?>>
)
private val capacity inline get() = EStatsCommon.instance.config.queue.local.capacity
private val batchThreshold inline get() = EStatsCommon.instance.config.queue.local.batchThreshold
private val timeThreshold inline get() = EStatsCommon.instance.config.queue.local.timeThreshold
private val interval inline get() = EStatsCommon.instance.config.queue.local.interval

object NoQueueSink : QueueSink {
    override fun push(
        database: String,
        sql: String,
        lines: List<List<Any?>>
    ) {
        executeBatchInsert(database, listOf(BufferItem(sql, lines)))
    }

    override fun start() = Unit
    override fun flush() = Unit
    override fun shutdown() = Unit

}

object LocalQueueSink : QueueSink {
    private class DatabaseBuffer() {
        val queue = LinkedBlockingQueue<BufferItem>(capacity)
        val flushLock = ReentrantLock()

        // 上次成功刷盘的时间戳
        @Volatile
        var lastFlushTime: Long = System.currentTimeMillis()
    }

    private val buffers = ConcurrentHashMap<String, DatabaseBuffer>()
    private lateinit var handler: EStatsCommon.TaskHandler

    /**
     * 写入数据
     * 极速返回，不阻塞
     */
    override fun push(database: String, sql: String, lines: List<List<Any?>>) {
        val buffer = buffers.computeIfAbsent(database) {
            DatabaseBuffer()
        }

        // 队列满的处理：直接异步任务插入
        if (!buffer.queue.offer(BufferItem(sql, lines))) {
            if (EStatsCommon.instance.config.queue.local.dropWhenFull) {
                EStatsCommon.instance.warn("队列已满，丢弃数据: $sql\n 参数: ${lines.joinToString("\n") { it.joinToString(", ") }}")
                return
            }
            EStatsCommon.instance.warn("队列已满，直接执行插入数据库操作")
            EStatsCommon.instance.runTaskAsync {
                executeBatchInsert(database, listOf(BufferItem(sql, lines)))
            }
        }
    }

    override fun flush() {
        for ((dbName, buffer) in buffers) {
            tryFlushDatabase(dbName, buffer)
        }
    }

    override fun start() {
        handler = EStatsCommon.instance.runTaskTimerAsync(interval, interval, ::flush)
    }

    private fun tryFlushDatabase(dbName: String, buffer: DatabaseBuffer) {
        EStatsCommon.instance.runTaskAsync {
            // 1. 条件：当前没有批量插入中 (使用 tryLock 非阻塞尝试)
            if (buffer.flushLock.isLocked) return@runTaskAsync
            buffer.flushLock.withLock {
                val currentSize = buffer.queue.size
                if (currentSize == 0) return@withLock

                val timeDiff = System.currentTimeMillis() - buffer.lastFlushTime
                val shouldFlush = currentSize >= batchThreshold || timeDiff >= timeThreshold

                if (!shouldFlush) return@withLock
                EStatsCommon.instance.debug { "保存数据库${dbName}队列" }

                performFlush(dbName, buffer, currentSize)
                // 更新时间戳
                buffer.lastFlushTime = System.currentTimeMillis()
            }
        }
    }

    private fun performFlush(dbName: String, buffer: DatabaseBuffer, limit: Int) {
        val items = ArrayList<BufferItem>(limit)
        // 取出当前队列中的所有数据 (或者限制最大取 1000 条防止 OOM)
        // drainTo 是线程安全的
        buffer.queue.drainTo(items, limit)

        if (items.isEmpty()) return

        // 执行 JDBC Batch
        executeBatchInsert(dbName, items)
    }

    /**
     * 关服时调用：强制刷盘所有数据
     */
    override fun shutdown() {
        if (::handler.isInitialized) handler.cancel()
        for ((dbName, buffer) in buffers) {
            buffer.flushLock.withLock {
                performFlush(dbName, buffer, buffer.queue.size)
            }
        }
    }
}