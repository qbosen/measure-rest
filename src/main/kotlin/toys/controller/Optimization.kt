package toys.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import toys.MeasureConfig
import java.lang.reflect.Method
import java.util.*
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.full.createInstance

/**
 * @author qiubaisen
 * @date 2020/6/30
 */

@Configuration
class CustomWebMvcConfigurationSupport(@Autowired val config: MeasureConfig) : DelegatingWebMvcConfiguration() {
    override fun createRequestMappingHandlerMapping(): RequestMappingHandlerMapping = CustomRequestMappingHandlerMapping(config)
}

private class RegexHandler(val pattern: String, val handler: HandlerMethod?) {
    val regex: Pattern = Pattern.compile(pattern)!!
    fun matches(named: String): Boolean = regex.matcher(named).matches()
}


@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class RegexRequest(val value: String, val method: RequestMethod)

class CustomRequestMappingHandlerMapping(private val config: MeasureConfig) : RequestMappingHandlerMapping() {
    private val unSupportNaming: MutableSet<String> = HashSet()
    private val fullUrlMap: MutableMap<String, HandlerMethod?> = HashMap() // method#url -> handler

    // 可以使用热点数据结构
    private val regexUrlMap: MutableMap<String, RegexHandler> = HashMap() // method#pattern -> handler
    private val infoMap: MutableMap<String, RequestMappingInfo> = HashMap()
    private var mappingLookup: Map<RequestMappingInfo, HandlerMethod>

    init {
        val mappingRegistryField = ReflectionUtils.findField(AbstractHandlerMethodMapping::class.java, "mappingRegistry")!!.apply { isAccessible = true }
        val mappingRegistry = mappingRegistryField.get(this)
        val mappingLookupField = ReflectionUtils.findField(mappingRegistry.javaClass, "mappingLookup")!!.apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        mappingLookup = mappingLookupField.get(mappingRegistry) as Map<RequestMappingInfo, HandlerMethod>
    }

    private fun naming(pattern: String, method: RequestMethod): String = method.name + "#" + pattern

    private fun getHandler(mapping: RequestMappingInfo): HandlerMethod? {
        return mappingLookup[mapping]
    }

    override fun lookupHandlerMethod(lookupPath: String, request: HttpServletRequest): HandlerMethod? {
        if (!config.enableOptimize) return super.lookupHandlerMethod(lookupPath, request)
        val named = naming(lookupPath, RequestMethod.valueOf(request.method))
        return when {
            unSupportNaming.contains(named) -> null
            fullUrlMap.containsKey(named) -> handleMatch(infoMap[named]!!, lookupPath, request).run { fullUrlMap[named] }
            else -> regexUrlMap.values.asSequence().find { it.matches(named) }?.let {
                handleMatch(infoMap[it.pattern]!!, lookupPath, request)
                regexUrlMap[it.pattern]!!.handler
            }
        } ?: super.lookupHandlerMethod(lookupPath, request)
    }

    override fun registerHandlerMethod(handler: Any, method: Method, mapping: RequestMappingInfo) {
        super.registerHandlerMethod(handler, method, mapping)
        enhanceRegister(method, mapping)
    }

    override fun registerMapping(mapping: RequestMappingInfo, handler: Any, method: Method) {
        super.registerMapping(mapping, handler, method)
        enhanceRegister(method, mapping)
    }

    private fun enhanceRegister(method: Method, mapping: RequestMappingInfo) {
        var regexAnnotation: RegexRequest? = method.getAnnotation(RegexRequest::class.java)
        // fixme 测试用的侵入式适配
        //  正常不会用自动mapping的方式去注册，所以可以进行方法上注解的识别，这里手动添加信息用于测试
        if(config.enableOptimize && mapping.patternsCondition.patterns.first().startsWith("/path")){
            val url = mapping.patternsCondition.patterns.first().replace("{path}", "\\w+")
            regexAnnotation = RegexRequest::class.constructors.first().call(url, RequestMethod.GET)
        }
        // fixme 结束
        if (regexAnnotation == null) {
            // 没有特殊设置的处理器 1. 全路径匹配，2. 非优化请求
            val patterns = mapping.patternsCondition.patterns
            val methods = mapping.methodsCondition.methods
            if (patterns.size != 1 || methods.size != 1) {
                patterns.forEach { p -> methods.forEach { unSupportNaming.add(naming(p, it)) } }
            } else {
                // 可能是符合要求的全路径请求
                val naming = naming(patterns.first(), methods.first())
                if (fullUrlMap.containsKey(naming)) {
                    unSupportNaming.add(naming)
                } else {
                    fullUrlMap[naming] = getHandler(mapping)
                    infoMap[naming] = mapping
                }
            }
        } else {
            // 设置了额外信息
            val classMapping = method.declaringClass.getAnnotation(RequestMapping::class.java)
            val classPath = if (classMapping?.value?.size == 1) classMapping.value[0] else ""
            val naming = naming(classPath + regexAnnotation.value, regexAnnotation.method)
            if (regexUrlMap.containsKey(naming)) {
                unSupportNaming.add(naming)
            } else {
                regexUrlMap[naming] = RegexHandler(naming, getHandler(mapping))
                infoMap[naming] = mapping
            }
        }
    }
}

