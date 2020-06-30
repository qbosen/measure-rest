package toys.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import toys.MeasureConfig
import kotlin.reflect.jvm.javaMethod


/**
 * @author qiubaisen
 * @date 2020/6/30
 */

// https://stackoverflow.com/questions/5758504/is-it-possible-to-dynamically-set-requestmappings-in-spring-mvc

@RestController
class BasicController {
    fun paramHandler(@RequestParam("param") param: String): String = param

    fun pathVariableHandler(@PathVariable("path") path: String): String = path
}

@Component
class HandlerMappingService(
        @Autowired val controller: BasicController,
        @Autowired val mappings: RequestMappingHandlerMapping,
        @Autowired val config: MeasureConfig
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        for (i in 1..config.paramUrls) {
            addParamHandler(i)
        }
        for (i in 1..config.pathVariableUrls) {
            addPathVariableHandler(i)
        }
    }

    fun addParamHandler(i: Int) {
        mappings.registerMapping(
                RequestMappingInfo.paths(config.paramUrlPattern.format(i))
                        .methods(RequestMethod.GET)
                        .produces(MediaType.APPLICATION_JSON_VALUE).build(),
                controller,
                controller::paramHandler.javaMethod!!)
    }

    fun addPathVariableHandler(i: Int) {
        mappings.registerMapping(
                RequestMappingInfo.paths(config.pathVariableUrlPattern.format(i))
                        .methods(RequestMethod.GET)
                        .produces(MediaType.APPLICATION_JSON_VALUE).build(),
                controller,
                controller::pathVariableHandler.javaMethod!!)
    }
}