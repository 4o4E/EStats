package top.e404.estats.command

import top.e404.estats.PL
import top.e404.estats.config.Lang
import top.e404.eplugin.command.AbstractDebugCommand

/**
 * debug指令
 */
object Debug : AbstractDebugCommand(
    PL,
    "estats.admin"
) {
    override val usage get() = Lang["command.usage.debug"]
}
