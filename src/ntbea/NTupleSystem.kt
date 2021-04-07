package ntbea

import evodef.SearchSpace
import evodef.SearchSpaceUtil
import evodef.BanditLandscapeModel
// import ntuple.params.Param;
import utilities.Picker
import utilities.StatSummary

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

/**
 * Modified from original NTupleSystem created by simonmarklucas on 13/11/2016.
 *
 *
 * This version created by simonmarklucas on 10/03/2018
 *
 *
 * This one uses an array of int directly as the key and thereby avoids possible address collisions
 */

// todo need to make this more efficient
// todo there is something strange that happens when NTuples are created
// something like making the StatSummary objects seems really slow

open class NTupleSystem(override val searchSpace: SearchSpace) : BanditLandscapeModel {

    protected var epsilon = defaultEpsilon

    var sampledPoints: MutableSet<IntArray>

    var tuples: ArrayList<NTuple>

    var use1Tuple = true
    var use2Tuple = true
    var use3Tuple = false
    var useNTuple = true

    // careful - this can be slow - it iterates over all points in the search space!
    override val bestSolution: DoubleArray
        get() {
            val picker = Picker<IntArray>(Picker.MAX_FIRST)
            var i = 0
            while (i < SearchSpaceUtil.size(searchSpace)) {
                val p = SearchSpaceUtil.nthPoint(searchSpace, i)
                picker.add(getMeanEstimate(p), p)
                i++
            }
            return picker.best.map { it.toDouble() }.toDoubleArray()
        }

    override// System.out.println("Best solution: " + Arrays.toString(picker.getBest()) + "\t: " + picker.getBestScore());
    val bestOfSampled: DoubleArray
        get() {
            val picker = Picker<IntArray>(Picker.MAX_FIRST)
            for (p in sampledPoints) {
                picker.add(getMeanEstimate(p), p)
            }
            return picker.best.map { it.toDouble() }.toDoubleArray()
        }

    init {
        // this.searchSpace = searchSpace;
        tuples = ArrayList()
        sampledPoints = HashSet()
    }

    fun useTuples(useTuples: BooleanArray): NTupleSystem {
        use1Tuple = useTuples[0]
        use2Tuple = useTuples[1] && searchSpace.nDims() > 2
        use3Tuple = useTuples[2] && searchSpace.nDims() > 3
        useNTuple = useTuples[3]
        return this
    }

    fun addTuples(): NTupleSystem {
        // this should only be called AFTER setting up the search space
        tuples = ArrayList()
        if (use1Tuple) add1Tuples()
        if (use2Tuple) add2Tuples()
        if (use3Tuple) add3Tuples()
        if (useNTuple) addNTuple()
        return this
    }

    override fun setEpsilon(epsilon: Double): NTupleSystem {
        this.epsilon = epsilon
        return this
    }

    override fun reset(): BanditLandscapeModel {
        sampledPoints = HashSet()
        for (nTuple in tuples) {
            nTuple.reset()
        }
        return this
    }

    override fun addPoint(p: IntArray, value: Double) {
        for (tuple in tuples) {
            tuple.add(p, value)
        }
        sampledPoints.add(p)
    }

    fun addSummary(p: IntArray, ss: StatSummary) {
        for (tuple in tuples) {
            tuple.add(p, ss)
        }
    }

    override fun getMeanEstimate(x: IntArray): Double {
        // we could get an average ...

        val ssTot = StatSummary()
        for (tuple in tuples) {
            val ss = tuple.getStats(x)
            if (ss != null) {
                if (tuple.tuple.size >= minTupleSize) {
                    val mean = ss.mean()
                    if (!java.lang.Double.isNaN(mean))
                        ssTot.add(mean)
                }
            }
        }
        // BarChart.display(probVec, "Prob Vec: " + Arrays.toString(x) + " : " + pWIn(probVec));

        // return rand.nextDouble();
        // System.out.println("Returning: " + ssTot.mean() + " : " + ssTot.n());

        val ret = ssTot.mean()
        return if (java.lang.Double.isNaN(ret)) {
            0.0
        } else {
            ret
        }
    }

    override fun getExplorationEstimate(x: IntArray): Double {
        // just takes the average of the exploration vector
        val vec = getExplorationVector(x)
        var tot = 0.0
        for (e in vec) tot += e
        return tot / vec.size
    }

    open fun getExplorationVector(x: IntArray): DoubleArray {
        // idea is simple: we just provide a summary over all
        // the samples, comparing each to the maximum in that N-Tuple

        // todo check whether we need the 1+

        val vec = DoubleArray(tuples.size)
        for (i in tuples.indices) {
            val tuple = tuples[i]
            val ss = tuple.getStats(x)
            if (ss != null) {
                vec[i] = Math.sqrt(Math.log((1 + tuple.nSamples()).toDouble()) / (epsilon + ss.n()))
            } else {
                vec[i] = Math.sqrt(Math.log((1 + tuple.nSamples).toDouble()) / epsilon)
            }
        }
        return vec
    }


    // note that there is a smarter way to add different n-tuples, but this way is easiest

    fun add1Tuples(): NTupleSystem {
        for (i in 0 until searchSpace.nDims()) {
            val a = intArrayOf(i)
            tuples.add(NTuple(searchSpace, a))
        }
        return this
    }

    fun add2Tuples(): NTupleSystem {
        for (i in 0 until searchSpace.nDims() - 1) {
            for (j in i + 1 until searchSpace.nDims()) {
                val a = intArrayOf(i, j)
                tuples.add(NTuple(searchSpace, a))
            }
        }
        return this
    }

    fun add3Tuples(): NTupleSystem {
        for (i in 0 until searchSpace.nDims() - 2) {
            for (j in i + 1 until searchSpace.nDims() - 1) {
                for (k in j + 1 until searchSpace.nDims()) {
                    val a = intArrayOf(i, j, k)
                    tuples.add(NTuple(searchSpace, a))
                }
            }
        }
        return this
    }

    fun addNTuple(): NTupleSystem {
        // adds the entire one
        val a = IntArray(searchSpace.nDims())
        for (i in a.indices) {
            a[i] = i
        }
        tuples.add(NTuple(searchSpace, a))
        return this
    }

    fun numberOfSamples(): Int {
        return sampledPoints.size
    }

    override fun addPoint(p: DoubleArray, value: Double) {
        addPoint(Arrays.stream(p).mapToInt { i -> (i + 0.5).toInt() }.toArray(), value)
    }

    override fun getExplorationEstimate(x: DoubleArray): Double {
        return getExplorationEstimate(Arrays.stream(x).mapToInt { i -> (i + 0.5).toInt() }.toArray())
    }

    override fun getMeanEstimate(x: DoubleArray): Double {
        return getMeanEstimate(Arrays.stream(x).mapToInt { i -> (i + 0.5).toInt() }.toArray())
    }

    companion object {

        internal var minTupleSize = 1
        internal var defaultEpsilon = 0.5
    }
}
