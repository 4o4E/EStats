package top.e404.estats.util

import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.Metadatable
import top.e404.estats.PL

@Suppress("UNUSED")
object Util {
    fun setMeta(m: Metadatable, key: String, value: String): String {
        m.setMetadata(key, FixedMetadataValue(PL, value))
        return value
    }

    fun getMeta(m: Metadatable, key: String): String? {
        return m.getMetadata(key).getOrNull(0)?.asString()
    }
}