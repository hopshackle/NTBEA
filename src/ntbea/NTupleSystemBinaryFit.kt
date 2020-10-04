package ntbea

import evodef.BanditLandscapeModel
import evodef.SearchSpace
import utilities.LinearRegression
import utilities.StatSummary
import java.util.*
import kotlin.Comparator
import kotlin.NoSuchElementException
import kotlin.math.*


class NTupleSystemBinaryFit(override val searchSpace: SearchSpace,
                            val interpolation: Double = 0.5,
                            val interpolateByTuple: Boolean = true,
                            val threshold: Int = 10,
                            val maxFeatures: Int = -1)
    : NTupleSystem(searchSpace) {

    private lateinit var regression: LinearRegression
    var allSampledPoints = arrayOf<IntArray>()
    var sampledResults = doubleArrayOf()
    var features = emptySet<IntArray>()
    var totalTime = 0L
    val intArrayComparator = Comparator<IntArray> { arr1: IntArray, arr2: IntArray ->
        try {
            val firstDiff = arr1.zip(arr2).first { it.first != it.second }
            firstDiff.first - firstDiff.second
        } catch (e: NoSuchElementException) {
            // in this case the longer array is bigger
            if (arr1.size == arr2.size) 0 else arr1.size - arr2.size
        }
    }

    val maxTuples = 1 + if (use2Tuple) {
        searchSpace.nDims() + searchSpace.nDims() * (searchSpace.nDims() - 1)
    } else {
        0
    } + if (use3Tuple) {
        searchSpace.nDims() * (searchSpace.nDims() - 1) * (searchSpace.nDims() - 2)
    } else 0

    // This is where we want to use the best alpha linear fit
    override fun getMeanEstimate(x: IntArray): Double {
        val retValue = if (interpolateByTuple) {
            val featuresOn = convert(x).count { it > 0.0 } - 1
            // -1 is because the constant term will always be set
            val fitWeight = featuresOn / maxTuples.toDouble()
            linearFit(x) * fitWeight + getMeanEstimateBelowThreshold(x) * (1.0 - fitWeight)
        } else {
            interpolation * linearFit(x) + (1.0 - interpolation) * super.getMeanEstimate(x)
        }
        //    println("${x.joinToString()} has mean estimate $retValue")
        return retValue
    }

    private fun matchesFeature(tuple: NTuple, x: IntArray): Boolean {
        // tuple.tuple is the indices that apply
        // easiest thing to do is convert x into feature format and see if it exists
        // Since features is a SortedSet (TreeSet), this is O(logN)
        val xAsFeature = x.withIndex().map{(i, _) -> if (tuple.tuple.contains(i)) x[i] else -99}.toIntArray()
        return features.contains(xAsFeature)
    }

    fun getMeanEstimateBelowThreshold(x: IntArray): Double {
        // rather than using the threshold here, I want to check against the feature set
        val ssTot = StatSummary()
        for (tuple in tuples) {
            val ss = tuple.getStats(x)
            if (ss != null) {
                if (tuple.tuple.size >= minTupleSize && !matchesFeature(tuple, x)) {
                    val mean = ss.mean()
                    if (!java.lang.Double.isNaN(mean))
                        ssTot.add(mean)
                }
            }
        }

        val ret = ssTot.mean()
        return if (java.lang.Double.isNaN(ret)) {
            0.0
        } else {
            ret
        }
    }

    override fun reset(): BanditLandscapeModel {
        super.reset()
        allSampledPoints = arrayOf()
        sampledResults = doubleArrayOf()
        features = emptySet()
        totalTime = 0L
        return this
    }

    override fun addPoint(p: IntArray, value: Double) {
        super.addPoint(p, value)
        sampledResults += value // we need to track this for fitting
        allSampledPoints += p
        // we add a bias term in at the front for the moment
        // TODO: Move somewhere else once we process the sampled points
        // TODO : At this point we should then calculate which features meet the threshold requirement
        // and process all sampled points into this format. However ... first of all we check to see if this rough idea will work at all
        val startTime = System.currentTimeMillis()
        calculatePointsAboveThreshold()
      //  val calcTime = System.currentTimeMillis()
        val pointsToFit: Array<DoubleArray> = convert(allSampledPoints);
     //   val conversionTime = System.currentTimeMillis()
        regression = LinearRegression(pointsToFit, sampledResults)
        val regressionTime = System.currentTimeMillis()
        totalTime += regressionTime - startTime
        //     println("Calculation: ${regressionTime - startTime}ms")


    }

    fun linearFit(x: IntArray): Double {
        return regression.predict(convert(x))
    }

    private fun calculatePointsAboveThreshold() {
        val tuplesAboveThresholdByVisit = tuples.map { Pair(it, it.ntMap) }
                .flatMap { (nTuple, ntMap) ->
                    ntMap.filterValues { ss -> ss.n() >= threshold }
                            .toList()
                            .map { (k, v) -> Triple(nTuple, k, v) }
                            .sortedByDescending { (_, _, ss) -> ss.n() }
                }
        // OK - so here is where I can keep a count of the visits
        // sort by them, and take the top N

        features = (if (maxFeatures > 0)
            tuplesAboveThresholdByVisit.take(maxFeatures) else tuplesAboveThresholdByVisit)
                .map { (nTuple, pattern, _) ->
                    val retValue = IntArray(searchSpace.nDims()) { -99 }
                    for (i in pattern.v.indices) {
                        retValue[nTuple.tuple[i]] = pattern.v[i]
                    }
                    retValue
                }.toSortedSet(intArrayComparator)
    }

    private fun convert(input: IntArray): DoubleArray {
        return doubleArrayOf(1.0) + features.map { f ->
            // f has -99 at irrelevant parameters
            if (f.withIndex().all { (i, v) -> v == -99 || v == input[i] }) 1.0 else 0.0
        }.toDoubleArray()
    }

    private fun convert(input: Array<IntArray>): Array<DoubleArray> {
        return input.map(::convert).toTypedArray()
    }

    override fun toString(): String {
        val top20Weights = regression.weights.withIndex().sortedByDescending { abs(it.value) }.take(20)
        return buildString {
            append(String.format("%d sampled points, %d ms calculation, %d features above threshold", sampledResults.size, totalTime, features.size))
            append("\n")
            for ((i, w) in top20Weights) {
                val featureArray = if (i == 0)
                    IntArray(searchSpace.nDims()) { -99 }
                else
                    (features as SortedSet<IntArray>).elementAt(i - 1)

                append(String.format("\t%+.3f : %s\n", w, featureArray.joinToString("|") { v -> if (v == -99) "*" else v.toString() }))
            }
        }
    }
}