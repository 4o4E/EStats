package top.e404.estats.command

import com.velocitypowered.api.command.CommandSource
import top.e404.eplugin.EPluginVelocity.Companion.component
import top.e404.eplugin.command.ECommand
import top.e404.estats.PL
import top.e404.estats.common.EStatsCommon
import top.e404.estats.config.Config
import top.e404.estats.config.Lang

object Reload : ECommand(
    PL,
    "reload",
    "(?i)r|reload",
    false,
    "estats.admin"
) {
    override val usage get() = Lang["command.usage.reload"]

    override fun onCommand(sender: CommandSource, args: Array<out String>) {
        plugin.runTaskAsync {
            EStatsCommon.instance.stop()
            Lang.load(sender)
            Config.load(sender)
            EStatsCommon.instance.load()
            sender.sendMessage(Lang["command.reload_done"].component)
        }
    }
}
