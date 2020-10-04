package utilities

import kotlin.math.*

object GlobalStatsCollator : StatsCollator()

open class StatsCollator {

    private var statistics: MutableMap<String, Double> = HashMap()
    private var squares: MutableMap<String, Double> = HashMap()
    private var count: MutableMap<String, Int> = HashMap()
    private var values: MutableMap<String, List<Double>> = HashMap()

    fun clear() {
        statistics = HashMap()
        count = HashMap()
        squares = HashMap()
    }

    fun addStatistics(newStats: Map<String, Number>) {
        newStats.forEach { (k, v) -> addStatistics(k, v.toDouble()) }
    }

    fun addStatistics(key: String, value: Double) {
        val oldV = statistics.getOrDefault(key, 0.00)
        val oldSq = squares.getOrDefault(key, 0.00)
        val oldCount = count.getOrDefault(key, 0)
        statistics[key] = oldV + value
        squares[key] = oldSq + value * value
        count[key] = oldCount + 1
    }

    fun addDetailedStatistics(key: String, value: Double) {
        addStatistics(key, value)
        values[key] = values.getOrDefault(key, emptyList()) + listOf(value)
    }

    fun addStatistics(key: String, value: Int) = addStatistics(key, value.toDouble())
    fun addStatistics(key: String, value: Long) = addStatistics(key, value.toDouble())

    fun summaryString(): String {
        return statistics.entries
                .map { (k, _) ->
                    String.format("%-20s = %.4g, SE = %.2g", k, getStatistics(k),
                            sqrt(((squares[k]!! / count[k]!!) - getStatistics(k).pow(2.0)) / (count[k]!! - 1).toDouble())) +
                            if (values[k] != null) {
                                String.format(", Median = %.4g, IQR = %.4g to %.4g, 95%% Range = %.4g to %.4g\n",
                                getPercentile(k, 0.5), getPercentile(k, 0.25), getPercentile(k, 0.75),
                                getPercentile(k, 0.025), getPercentile(k, 0.975))
                            } else "\n"
                }
                .sorted()
                .joinToString(separator = "")
    }

    fun getStatistics(key: String): Double {
        if (statistics.containsKey(key))
            return statistics[key]!! / count[key]!!
        return -1.0
    }

    /*
    Provide percentile in range [0, 1]
     */
    fun getPercentile(key: String, percentile: Double): Double {
        if (percentile < 0.0 || percentile > 1.0)
            throw AssertionError("Percentile must be between 0.0 and 1.0")
        val index = ((values[key]?.size ?: 0).toDouble() * percentile).toInt()
        return values[key]?.sorted()?.get(index) ?: 0.0
    }
}

