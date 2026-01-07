package top.e404.estats.common.listener

import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.Advice.*
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.bytecode.assign.Assigner
import net.bytebuddy.matcher.ElementMatchers
import org.springframework.expression.Expression
import org.springframework.expression.ParseException
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils
import top.e404.estats.common.EStatsCommon
import top.e404.estats.common.config.FuncListenerConfig
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object FuncListenerManager {
    private val regex = """^(?<class>[\w.$]+)#(?<method>\w+)\((?<args>.*)\)$""".toRegex()
    private fun resolveMethod(expr: String, classLoader: ClassLoader): Method? {
        val matcher = regex.find(expr)
        requireNotNull(matcher) { "方法签名`${expr}`格式错误，应为: com.example.service.UserService#updateUser(java.lang.String, int)" }

        val className = matcher.groups["class"]!!.value.trim()
        val methodName = matcher.groups["method"]!!.value.trim()
        val argTypes = matcher.groups["args"]!!.value.split(",").map { it.trim() }

        // 1. 加载目标类
        val targetClass = ClassUtils.forName(className, classLoader)

        // 2. 解析参数类型
        // 简单的逗号分割，如果参数类型本身包含泛型逗号(如 Map<String,String>) 需要更复杂的解析，
        // 但反射查找方法只看 Raw Type，所以写 java.util.Map 即可，不用写泛型。
        val paramTypes = argTypes.map { type ->
            // ClassUtils.forName 极其强大：
            // 支持 "int" -> int.class
            // 支持 "java.lang.String[]" -> String[].class
            ClassUtils.forName(type, classLoader)
        }

        // 3. 查找方法 (精确匹配，支持 protected/private)
        // 使用 Spring 的 ReflectionUtils 或者标准反射
        return ReflectionUtils.findMethod(
            targetClass,
            methodName,
            *paramTypes.toTypedArray()
        )
    }

    // 记录已经修改过的类，防止重复增强导致报错或性能损耗
    private val enhancedMethods = ConcurrentHashMap.newKeySet<Method>()
    private val registeredListeners = ConcurrentHashMap<Method, FuncListener>()

    fun load() {
        for (config in EStatsCommon.instance.config.func) register(config)
    }

    private fun register(config: FuncListenerConfig) {
        try {
            val targetLoader = EStatsCommon.instance.getClassLoader(config.plugin)
                ?: error("未找到目标插件: ${config.plugin}")
            val method = resolveMethod(config.method, targetLoader) ?: error("未找到目标方法: ${config.method}")
            // 避免同一个方法被多次代理，导致 advice 嵌套
            if (enhancedMethods.add(method)) {
                try {
                    ByteBuddy()
                        .redefine(method.declaringClass)
                        .visit(Advice.to(AopAdvice::class.java).on(ElementMatchers.`is`(method)))
                        .make()
                        .load(targetLoader, ClassReloadingStrategy.fromInstalledAgent())
                } catch (e: Exception) {
                    enhancedMethods.remove(method)
                    registeredListeners.remove(method)
                    throw RuntimeException("方法代理失败: ${config.method}", e)
                }
            }

            val condition = try {
                config.condition?.let { EStatsCommon.instance.parser.parseExpression(it) }
            } catch (e: ParseException) {
                EStatsCommon.instance.warn("函数监听器注册失败: ${config.method} 的 condition 表达式解析失败", e)
                return
            }
            val param = try {
                config.param.let { EStatsCommon.instance.parser.parseExpression(it) }
            } catch (e: ParseException) {
                EStatsCommon.instance.warn("函数监听器注册失败: ${config.method} 的 params 表达式解析失败", e)
                return
            }
            registeredListeners[method] = FuncListener(config, method, condition, param)

            EStatsCommon.instance.debug("成功代理方法: $method")
        } catch (e: Exception) {
            EStatsCommon.instance.warn("函数监听器注册失败: ${config.method}", e)
        }
    }

    /**
     * 真正处理拦截数据的逻辑
     */
    fun handle(method: Method, args: Array<Any?>, returnVal: Any?) {
        registeredListeners[method]?.onMethodCall(args, returnVal)
    }

    fun stop() {
        registeredListeners.clear()
    }

    data class FuncListener(
        val config: FuncListenerConfig,
        val method: Method,
        val condition: Expression?,
        val param: Expression,
    ) : AbstractListener(config.save) {
        fun onMethodCall(args: Array<Any?>, returnValue: Any?) {
            EStatsCommon.instance.debug { "监听函数触发: ${method.declaringClass.name}#${method.name}(${config.plugin})" }
            try {
                val context = EStatsCommon.instance.getCtx().apply {
                    setVariable("args", args)
                    setVariable("returnVal", returnValue)
                }
                val cond = condition?.getValue(context, Boolean::class.java) ?: true
                if (!cond) return

                val result = param.getValue(context)
                save(config.database, result)
            } catch (e: Exception) {
                EStatsCommon.instance.warn("函数监听时出现异常: ${config.method}", e)
            }
        }
    }

    @Suppress("UNUSED")
    object AopAdvice {
        /**
         * 方法退出时调用
         *
         * @param method 当前方法的 Method 对象
         * @param args 所有参数
         * @param returnValue 返回值 (typing = DYNAMIC 允许 void 方法返回 null 而不报错)
         * @param throwable 抛出的异常（如果有）
         */
        @OnMethodExit(onThrowable = Throwable::class, suppress = Throwable::class)
        @JvmStatic
        fun onExit(
            @Origin method: Method,
            @AllArguments args: Array<Any?>,
            @Return(typing = Assigner.Typing.DYNAMIC) returnValue: Any?,
            @Thrown throwable: Throwable?
        ) {
            if (throwable == null) {
                handle(method, args, returnValue)
            }
        }
    }
}