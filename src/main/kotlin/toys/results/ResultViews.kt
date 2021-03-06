package toys.results

import javafx.application.Application
import javafx.scene.chart.NumberAxis
import tornadofx.*

/**
 * @author qiubaisen
 * @date 2020/7/4
 */


fun main() {
    Application.launch(BenchMarkApp::class.java)
}

class BenchMarkApp : App(LineChartView::class)

class LineChartView : View("Spring MVC Restful interface performance") {
    private val headLines = setOf("uri,concurrency,requests,qps,tpr,t_90")
    private val benchData = loadData("bench2.csv", headLines, 1.5, BenchData::qps)

    override val root = hbox {
        fun createLineChart(title: String, xAxis: String, yAxis: String, op: (BenchData) -> Number) =
                linechart(title, NumberAxis().apply { label = xAxis }, NumberAxis().apply { label = yAxis }) {
                    listOf(Series.RestSeries, Series.NonRestSeries, Series.RestOptSeries, Series.NonRestOptSeries).forEach { ser ->
                        series(ser.name) { benchData[ser]?.forEach { data(it.type.count, op(it)) } }
                    }
                }
        createLineChart("query per second", "mappings count", "qps", BenchData::qps)
        createLineChart("time per request", "mappings count", "tpr", BenchData::tpr)
        createLineChart("tps for top 90% ", "mappings count", "t_90", BenchData::t_90)
    }
}