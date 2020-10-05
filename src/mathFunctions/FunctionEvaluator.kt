package mathFunctions

import evodef.*
import kotlin.AssertionError
import kotlin.random.Random

class FunctionEvaluator(val f: NTBEAFunction, val searchSpace: SearchSpace) : SolutionEvaluator {

    private val rnd = Random(System.currentTimeMillis())

    override fun logger() = EvolutionLogger()

    var nEvals = 0

    override fun searchSpace() = searchSpace

    override fun reset() {
        nEvals = 0
    }

    override fun nEvals() = nEvals
    override fun evaluate(settings: IntArray): Double {
        return evaluate(searchSpace.sampleAt(settings))
    }

    override fun evaluate(settings: DoubleArray): Double {
        nEvals++
        return if (rnd.nextDouble() < f.functionValue(settings)) 1.0 else 0.0
    }
}

fun main(args: Array<String>) {
    val f = when (args[0]) {
        "Hartmann3" -> Hartmann3
        "Hartmann6" -> Hartmann6
        "Branin" -> Branin
        "GoldsteinPrice" -> GoldsteinPrice
        else -> throw AssertionError("Unknown function ${args[0]}")
    }
    FunctionExhaustiveResult(f, FunctionSearchSpace(f.dimension, args[1].toInt())).calculate()
}

class FunctionExhaustiveResult(val f: NTBEAFunction, val searchSpace: FunctionSearchSpace) {

    // We run through every possible setting in the searchSpace, and run maxGames, logging the average result
    val stateSpaceSize = (0 until searchSpace.nDims()).fold(1, { acc, i -> acc * searchSpace.nValues(i) })

    val allRanges = (0 until searchSpace.nDims()).map { 0 until searchSpace.nValues(it) }

    fun addDimension(start: List<List<Int>>, next: IntRange): List<List<Int>> {
        return start.flatMap { list -> next.map { list + it } }
    }

    val allOptions = allRanges.fold(listOf(emptyList<Int>()), { acc, r -> addDimension(acc, r) })

    fun calculate() {
        val optionScores = allOptions.map {
            val params = it.toIntArray()
            //   StatsCollator.clear()
            val perfectScore = f.functionValue(searchSpace.valueAt(params))
            println("${params.joinToString()} has mean score ${String.format("%.3g", perfectScore)}")
            //   println(StatsCollator.summaryString())
            params to perfectScore
        }

        val numberPositive = optionScores.filter { it.second > 0.0 }.count().toDouble()
        val sortedScores = optionScores.sortedBy { it.second }.reversed().take(50)
        println("Best scores are : \n" + sortedScores.joinToString("\n") { String.format("\t%.3g at %s", it.second, it.first.joinToString()) })
        println("A total of ${String.format("%.1f%% are above zero", 100.0 * numberPositive / optionScores.size)}")
    }
}

class FunctionSearchSpace(val dimensions: Int, val valuesPerDimension: Int) : SearchSpace {

    val increment = 1.0 / valuesPerDimension
    override fun nDims(): Int {
        return dimensions
    }

    override fun nValues(i: Int): Int {
        return valuesPerDimension
    }

    override fun name(i: Int): String {
        return ""
    }

    override fun value(i: Int, j: Int): Double {
        return j * increment
    }

    override fun sampleAt(settings: IntArray): DoubleArray {
        return settings.withIndex().map { value(it.index, it.value) }.toDoubleArray()
    }

    override fun valueAt(indices: IntArray): DoubleArray {
        return sampleAt(indices)
    }

}

class GaussianSearchSpace(val dimensions: Int, val valuesPerDimension: IntArray, val minMus: DoubleArray, val maxMus: DoubleArray, val minSigs: DoubleArray, val maxSigs: DoubleArray) : SearchSpace {

    var muSig: Array<Array<Double>>

    init {
        // Build the search space
        muSig = Array(dimensions * 2) { i ->
            val nIncrements = valuesPerDimension[i]
            if (i >= dimensions) {
                val sigIdx = i-dimensions
                val minSig = minSigs[sigIdx]
                val maxSig = maxSigs[sigIdx]
                val sigInc = (maxSig - minSig) / nIncrements

                Array(valuesPerDimension[i]) { j ->
                    minSig + sigInc * j
                }
            } else {
                val minMu = minMus[i]
                val maxMu = maxMus[i]
                val muInc = (maxMu - minMu) / nIncrements
                Array(valuesPerDimension[i]) { j ->
                    minMu + muInc * j
                }
            }
        }
    }

    override fun nDims(): Int {
        return dimensions * 2
    }

    override fun nValues(i: Int): Int {
        return valuesPerDimension[i];
    }

    override fun name(i: Int): String {
        return ""
    }

    override fun value(d: Int, i: Int): Any {
        return muSig[d][i]
    }

    fun sample(idx: IntArray): DoubleArray {

        return DoubleArray(dimensions) { d ->
            val so = d + dimensions
            val mu = muSig[d][idx[d]]
            val sig = muSig[so][idx[so]]

            java.util.Random().nextGaussian() * sig + mu
        }
    }

    override fun sampleAt(settings: IntArray): DoubleArray {
        return sample(settings)
    }

    override fun valueAt(idx: IntArray): DoubleArray {
        return DoubleArray(dimensions) { d ->
            muSig[d][idx[d]]
        }
    }

}