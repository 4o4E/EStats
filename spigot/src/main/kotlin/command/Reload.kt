package top.e404.estats.command

import org.bukkit.command.CommandSender
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

    override fun onCommand(sender: CommandSender, args: Array<out String>) {
        plugin.runTaskAsync {
            Lang.load(sender)
            Config.load(sender)

            EStatsCommon.instance.reload()
            sender.sendMessage(Lang["command.reload_done"])
        }
    }
}
