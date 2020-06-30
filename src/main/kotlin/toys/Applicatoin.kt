package toys

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

/**
 * @author qiubaisen
 * @date 2020/6/28
 */

fun main() {
    runApplication<ToysApplication>()
}

@Component
class TestRunner(@Autowired val config: MeasureConfig) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        println("当前配置是：$config")
    }
}

@SpringBootApplication
@EnableConfigurationProperties(MeasureConfig::class)
class ToysApplication

@ConstructorBinding
@ConfigurationProperties(prefix = "measure")
data class MeasureConfig(
        val paramUrls: Int = 0, val paramUrlPattern: String = "/param/%s",
        val pathVariableUrls: Int = 0, val pathVariableUrlPattern: String = "/path/{path}/%s",
        val enableOptimize: Boolean = false
)

