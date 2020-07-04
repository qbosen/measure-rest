package toys.results

import javafx.application.Application
import javafx.scene.chart.CategoryAxis
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
    val datas = loadData()

    override val root = hbox {
        fun createLineChart(title: String, xAxis: String, yAxis: String, op: (BenchData) -> Number) {
            linechart(title, NumberAxis().apply { label = xAxis }, NumberAxis().apply { label = yAxis }) {
                series("NO REST") {
                    datas[Series(false, false)]?.forEach { data(it.type.count, op(it)) }
                }
                series("NO REST OPT") {
                    datas[Series(false, true)]?.forEach { data(it.type.count, op(it)) }
                }
                series("REST") {
                    datas[Series(true, false)]?.forEach { data(it.type.count, op(it)) }
                }
                series("REST OPT") {
                    datas[Series(true, true)]?.forEach { data(it.type.count, op(it)) }
                }
            }
        }

        createLineChart("query per second" ,"mappings count","qps", BenchData::qps)
        createLineChart("time per request" ,"mappings count","tpr", BenchData::tpr)
        createLineChart("tps for top 90% " ,"mappings count","t_90", BenchData::t_90)
    }


}