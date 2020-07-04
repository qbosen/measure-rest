package toys.results

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.streams.asSequence


/**
 * @author qiubaisen
 * @date 2020/7/4
 */


data class Type(val restful: Boolean, val optimize: Boolean, val count: Int)
data class Series(val restful: Boolean, val optimize: Boolean)

class BenchData(lineData: String) {
    lateinit var uri: String
    lateinit var type: Type
    var concurrency: Int = 0
    var requests: Int = 0
    var qps: Double = 0.0
    var tpr: Double = 0.0
    var t_90: Int = 0

    init {
        lineData.splitToSequence(',').forEachIndexed { i, v ->
            when (i) {
                0 -> uri = v.also { type = parseUri(v) }
                1 -> concurrency = v.toInt()
                2 -> requests = v.toInt()
                3 -> qps = v.toDouble()
                4 -> tpr = v.toDouble()
                5 -> t_90 = v.toInt()
            }
        }
    }

    companion object {
        private const val optimizeStr = "optimize"
        private val paramPattern = Pattern.compile("/param/(\\d+)\\?param=(.+)")!!
        private val pathPattern = Pattern.compile("/path/(.+)/(\\d+)")!!
        fun parseUri(uri: String): Type {
            return paramPattern.matcher(uri).takeIf(Matcher::matches)?.run { Type(false, group(2) == optimizeStr, group(1).toInt()) }
                    ?: pathPattern.matcher(uri).takeIf(Matcher::matches)?.run { Type(true, group(1) == optimizeStr, group(2).toInt()) }
                    ?: throw RuntimeException("无法解析:$uri")
        }
    }
}

const val headLine = "uri,concurrency,requests,qps,tpr,t_90"
fun cleanData(data: Map<Type, List<BenchData>>): List<BenchData> {
    return data.values.map { it[9] }
}

fun loadData(): Map<Series, List<BenchData>> {
    val groupData = Files.lines(Paths.get("bench.csv")).asSequence().filterNot { headLine == it }
            .map { BenchData(it) }.groupBy(BenchData::type)
    val cleanData = cleanData(groupData)
    return cleanData.asSequence().groupBy { Series(it.type.restful, it.type.optimize) }
}