package top.e404.estats.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import top.e404.eplugin.config.JarConfigDefault
import top.e404.eplugin.config.KtxConfig
import top.e404.estats.PL
import top.e404.estats.common.config.ConfigData

object Config : KtxConfig<ConfigData>(
    plugin = PL,
    path = "config.yml",
    default = JarConfigDefault(PL, "config.yml"),
    serializer = ConfigData.serializer(),
    format = Yaml(configuration = YamlConfiguration(strictMode = false))
)
