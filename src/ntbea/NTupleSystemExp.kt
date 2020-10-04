package ntbea

import evodef.SearchSpace
import utilities.StatSummary
import kotlin.math.*

class NTupleSystemExp(override var searchSpace: SearchSpace, val halfN: Int, val minWeightExplore: Double = 0.5,
                      val weightFunction: (Int) -> Double = { visits: Int -> 1.0 - exp(-(visits.toDouble()) / halfN) },
                      val weightExplore: Boolean = false, val exploreWithSqrt: Boolean = false) : NTupleSystem(searchSpace) {
    companion object {
        val emptyStats = StatSummary()
        val meanFunction = { tuple: NTuple, data: IntArray ->
            tuple.getStats(data)?.mean() ?: Double.NaN
        }
    }

    override fun getMeanEstimate(x: IntArray): Double {
        val retValue = getWeighting(x, IntArray(x.size) { it }, 0.0, meanFunction, weightFunction)
        //    println("${x.joinToString()} has mean estimate $retValue")
        return retValue
    }

    override fun getExplorationEstimate(x: IntArray): Double {
        if (!weightExplore) {
            return super.getExplorationEstimate(x)
        }

        return getWeighting(x, IntArray(x.size) { it }, minWeightExplore, { tuple, data ->
            val ss = tuple.getStats(data) ?: emptyStats
            if (exploreWithSqrt)
                sqrt(sqrt(1.0 + tuple.nSamples()) / (epsilon + ss.n()))
            else
                sqrt(ln(1.0 + tuple.nSamples()) / (epsilon + ss.n()))
        }, weightFunction)
    }

    override fun getExplorationVector(x: IntArray): DoubleArray {
        // idea is simple: we just provide a summary over all
        // the samples, comparing each to the maximum in that N-Tuple
        // not used with EXP_FULL
        val vec = tuples.map { tuple ->
            val ss = tuple.getStats(x) ?: emptyStats
            if (exploreWithSqrt)
                sqrt(sqrt(1.0 + tuple.nSamples()) / (epsilon + ss.n()))
            else
                sqrt(ln(1.0 + tuple.nSamples()) / (epsilon + ss.n()))
        }
        return vec.toDoubleArray()
    }

    fun getWeighting(x: IntArray, indices: IntArray, minWeight: Double,
                     valueFunction: (NTuple, IntArray) -> Double,
                     weightFunction: (Int) -> Double): Double {
        // valueFunction returns the simple value of the NTuple
        // weightFunction takes the number of visits to the NTuple, and returns a number in [0, 1] for its weighting
        if (indices.size < minTupleSize) return 0.0

        val allSubTuples = tuples.filter { it.tuple.all(indices::contains) }
        if (allSubTuples.isEmpty())
            throw AssertionError("WTF")
        val largestSize = allSubTuples.map(NTuple::tuple).map { it.size }.maxOrNull()
        val allTuplesAtLevel = allSubTuples.filter { it.tuple.size == largestSize }

        val tupleMeans = allTuplesAtLevel.map { t ->
            val stats = t.getStats(x) ?: emptyStats
            var baseResult = valueFunction(t, x)
            val weight = when {
                indices.size == minTupleSize -> 1.0
                baseResult.isNaN() -> {
                    baseResult = 0.0;
                    0.0
                }
                else -> max(minWeight, weightFunction(stats.n()))
            }
            weight * baseResult + (1.0 - weight) * t.tuple.map { excludedIndex ->
                getWeighting(x, t.tuple.filterNot { it == excludedIndex }.toIntArray(), minWeight, valueFunction, weightFunction)
            }.average()
        }
        return tupleMeans.filterNot(Double::isNaN).average()
    }

//    fun getExponentialWeighting(x: IntArray, indices: IntArray, minWeight: Double, f: (NTuple, IntArray) -> Double): Double {
//        return getWeighting(x, indices, minWeight, f, { visits: Int -> 1.0 - exp(-(visits.toDouble()) / halfN) })
//    }

}