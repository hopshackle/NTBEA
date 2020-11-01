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

    fun sampleAt(indices: IntArray): DoubleArray {
        return doubleArrayOf(0.0)
    }

    fun valueAt(indices: IntArray): DoubleArray {
        return doubleArrayOf(0.0)
    }
}

private fun setupSearchDimensions(fileName: String): List<String> {
    val allDimensions = if (fileName != "") FileReader(fileName).readLines() else emptyList()
    return allDimensions.filter { it.contains(",") }  // this filters out any dimension with only one entry
}

abstract class AgentSearchSpace<T>(val searchDimensions: List<String>, val types: Map<String, Class<*>>) : SearchSpace {
    constructor(fileName: String, types: Map<String, Class<*>>) : this(setupSearchDimensions(fileName), types)

    //  val searchDimensions: List<String> = setupSearchDimensions(fileName)

    val searchKeys: List<String> = searchDimensions.map { it.split("=").first() }
    val searchTypes: List<Class<*>> = searchKeys.map {
        types[it] ?: throw AssertionError("Unknown search variable $it")
    }
    val searchValues: List<List<Any>> = searchDimensions.zip(searchTypes)
        .map { (allV, cl) ->
            allV.split("=")[1]      // get the stuff after the colon, which should be values to be searched
                .split(",")
                .map(String::trim).map {str ->
                    when (cl) {
                        Int::class, Int::class.javaObjectType, Int::class.javaPrimitiveType -> str.toInt()
                        Double::class, Double::class.javaObjectType, Double::class.javaPrimitiveType -> str.toDouble()
                        Boolean::class.java, Boolean::class.javaObjectType, Boolean::class.javaPrimitiveType -> str.toBoolean()
                        String::class.java, String::class.javaObjectType -> str
                        else -> if (cl.isEnum) {
                            cl.enumConstants.find { it.toString() == str } ?: throw AssertionError("Enum not found : " + str + " in " + cl.enumConstants.joinToString())
                        } else
                            throw AssertionError("Currently unsupported class $cl.")
                    }
                }
        }

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
                Int::class, Int::class.javaObjectType, Int::class.javaPrimitiveType -> (v + 0.5).toInt()
                Double::class, Double::class.javaObjectType, Double::class.javaPrimitiveType -> v
                Boolean::class.java, Boolean::class.javaObjectType, Boolean::class.javaPrimitiveType -> v > 0.5
                else ->  searchValues[i][(v + 0.5).toInt()]
            }
        }.toMap()
    }

    abstract fun getAgent(settings: IntArray): T
    override fun nValues(i: Int) = searchValues[i].size
    override fun nDims() = searchValues.size
    override fun name(i: Int) = searchKeys[i]
    override fun value(d: Int, i: Int) = searchValues[d][i]
}

