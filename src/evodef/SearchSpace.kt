package evodef

import java.io.FileReader
import java.lang.AssertionError
import kotlin.reflect.KClass

/**
 * Created by sml on 16/08/2016.
 *
 * This models a search space where there is a fixed number of dimensions
 * but each dimension may have a different cardinality (i.e. a different number of possible values)
 *
 */
interface SearchSpace {
    // number of dimensions
    fun nDims(): Int

    // number of possible values in the ith dimension
    fun nValues(i: Int): Int

    fun name(i: Int): String

    fun value(d: Int, i: Int): Any {
        return 0.00
    }
}


abstract class AgentSearchSpace<T>(fileName: String) : SearchSpace {

    val searchDimensions: List<String> = if (fileName != "") FileReader(fileName).readLines() else emptyList()
    val searchKeys: List<String> = searchDimensions.map { it.split("=").first() }
    val searchTypes: List<KClass<*>> = searchKeys.map {
        types[it] ?: throw AssertionError("Unknown search variable $it")
    }
    val searchValues: List<List<Any>> = searchDimensions.zip(searchTypes)
        .map { (allV, cl) ->
            allV.split("=")[1]      // get the stuff after the colon, which should be values to be searched
                .split(",")
                .map(String::trim).map {
                    when (cl) {
                        Int::class -> it.toInt()
                        Double::class -> it.toDouble()
                        Boolean::class -> it.toBoolean()
                        String::class -> it
                        else -> throw AssertionError("Currently unsupported class $cl")
                    }
                }
        }
    abstract val types: Map<String, KClass<*>>
    fun convertSettings(settings: IntArray): DoubleArray {
        return settings.zip(searchValues).map { (i, values) ->
            val v = values[i]
            when (v) {
                is Int -> v.toDouble()
                is Double -> values[i]
                is Boolean -> if (v) 1.0 else 0.0
                is String -> i.toDouble() // if a String, then we return the index of this
                else -> settings[i].toDouble()      // if not numeric, default to the category
            } as Double
        }.toDoubleArray()
    }

    fun settingsToMap(settings: DoubleArray): Map<String, Any> {
        return settings.withIndex().map { (i, v) ->
            searchKeys[i] to when (searchTypes[i]) {
                Int::class -> (v + 0.5).toInt()
                Double::class -> v
                Boolean::class -> v > 0.5
                String::class -> searchValues[i][(v + 0.5).toInt()]
                else -> throw AssertionError("Unsupported class ${searchTypes[i]}")
            }
        }.toMap()
    }

    abstract fun getAgent(settings: DoubleArray): T
    override fun nValues(i: Int) = searchValues[i].size
    override fun nDims() = searchValues.size
    override fun name(i: Int) = searchKeys[i]
    override fun value(d: Int, i: Int) = searchValues[d][i]
}

