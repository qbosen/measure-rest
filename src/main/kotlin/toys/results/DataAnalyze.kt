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
sealed class Series(val name: String) {
    object RestSeries : Series("Rest")
    object NonRestSeries : Series("Non Rest")
    object RestOptSeries : Series("Rest Opt")
    object NonRestOptSeries : Series("Non Rest Opt")

    companion object {
        fun of(restful: Boolean, optimize: Boolean): Series = when {
            restful && !optimize -> RestSeries
            !restful && !optimize -> NonRestSeries
            restful && optimize -> RestOptSeries
            !restful && optimize -> NonRestOptSeries
            else -> throw RuntimeException("无法解析")
        }
    }
}

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

fun cleanData(data: Map<Type, List<BenchData>>, k: Double, valueOp: (BenchData) -> Double): List<BenchData> {
    // 四分位距法过滤
    fun iqrFilter(list: List<BenchData>, k: Double, valueOp: (BenchData) -> Double): List<BenchData> {
        val sorted = list.sortedBy(valueOp)
        val q1 = sorted[(list.size + 1) / 4].let(valueOp)
        val q3 = sorted[(list.size + 1) * 3 / 4].let(valueOp)
        val minEst = q1 - k * (q3 - q1)
        val maxEst = q3 + k * (q3 - q1)
        return sorted.filter { valueOp(it) in minEst..maxEst }.also { println("过滤了${list.size - it.size}条数据") }
    }

    fun avg(list: List<BenchData>): BenchData = list.asSequence()
            .reduce { acc, v -> acc.apply { qps += v.qps; tpr += v.tpr; t_90 += v.t_90 } }
            .apply { qps /= list.size; tpr /= list.size; t_90 /= list.size }

    return data.values.map { avg(iqrFilter(it, k, valueOp)) }
}

fun loadData(): Map<Series, List<BenchData>> {
    val groupData = Files.lines(Paths.get("bench.csv")).asSequence().filterNot { headLine == it }
            .map { BenchData(it) }.groupBy(BenchData::type)
    // 对每一组按qps进行过滤，中度异常过滤
    val cleanData = cleanData(groupData, 1.5, BenchData::qps)
    return cleanData.asSequence().groupBy { Series.of(it.type.restful, it.type.optimize) }
}