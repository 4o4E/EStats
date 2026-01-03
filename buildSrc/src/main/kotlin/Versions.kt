object Versions {
    const val GROUP = "top.e404"
    const val VERSION = "1.1.0"

    const val KOTLIN = "2.2.21"
    const val EPLUGIN = "1.4.0-SNAPSHOT"
}

fun eplugin(module: String, version: String = Versions.EPLUGIN) = "top.e404.eplugin:eplugin-$module:$version"
fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
