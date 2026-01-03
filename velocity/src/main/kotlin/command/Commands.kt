package top.e404.estats.command

import top.e404.estats.PL
import top.e404.eplugin.command.ECommandManager

object Commands : ECommandManager(
    PL,
    "estatsv",
    listOf("esv"),
    Debug,
    Reload,
)
