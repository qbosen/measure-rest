package toys

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

/**
 * @author qiubaisen
 * @date 2020/6/28
 */

fun main(args: Array<String>) {
    runApplication<ToysApplication>(*args) {
        addListeners(ApplicationPidFileWriter())
    }
}

@Component
class ConfigRunner(@Autowired val config: MeasureConfig) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        args?.run {
            getOptionValues("paramUrls")?.last()?.let(String::toInt)?.let { config.paramUrls = it }
            getOptionValues("pathUrls")?.last()?.let(String::toInt)?.let { config.pathVariableUrls = it }
            takeIf { containsOption("optimize") }?.let { config.enableOptimize = true }
        }
        println("当前配置是：$config")
    }
}

@SpringBootApplication
@EnableConfigurationProperties(MeasureConfig::class)
class ToysApplication

@ConstructorBinding
@ConfigurationProperties(prefix = "measure")
data class MeasureConfig(
        var paramUrls: Int = 0, var paramUrlPattern: String = "/param/%s",
        var pathVariableUrls: Int = 0, var pathVariableUrlPattern: String = "/path/{path}/%s",
        var enableOptimize: Boolean = false
)

