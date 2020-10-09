package mathFunctions

import evodef.*
import ntbea.*
import utilities.GlobalStatsCollator
import java.io.FileWriter
import kotlin.AssertionError
import kotlin.math.*



fun main(args: Array<String>) {
    if (args.size < 4 || args.contains("-h")) {
        println(
            """
                Core required command line arguments in order:
                - Type of Function: Hartmann3|Hartmann6|Branin|GoldsteinPrice
                - Type of NTBEA function: STD|FIT|STDFIT|EXP|LIN|INV|SQRT|GAUSSIAN  (STD works best almost always)
                - Number of NTBEA runs, and trials for each in format "runs|trials"
                - Discretization level to use for each parameter dimension
                
                Additional optional arguments:
                kExplore=                   The constant to use in the UCB formula. Large k explores more. Default is 100.
                minWeight=                  Only used for EXP, LIN, SQRT or INV weight functions. Frankly I wouldn't bother.
                fitWeight=                  Weight to apply to the Linear Regression function when using STDFIT - between 0.0 and 1.0. Default is 0.5.
                maxF=                       Maximum number of features to use for FIT or STDFIT in the regression component. Default is 0 (meaning no maximum).
                T=                          For EXP/LIN/SQRT/INV, this is the temperature to use in the weight function. Default is 30.
                T=                          For FIT/STDFIT this is instead the threshold number of visits before a Tuple is incorporated in the regression. Default is 30.
                hood=                       The number of neighbouring points to test at each iteration of NTBEA before picking one to sample with the function.
                logFile=                    A filename to write the results too (one line per NTBEA run).
            """.trimIndent()
        )
    }

    val f = when (args[0]) {
        "Hartmann3" -> Hartmann3
        "Hartmann6" -> Hartmann6
        "Branin" -> Branin
        "GoldsteinPrice" -> GoldsteinPrice
        else -> throw AssertionError("Invalid first argument for function " + args[0])
    }
    val type = args[1]
    val iterations = args[2].split("|")
    val dimensions = args[3].toInt()
    FunctionReport(f).iterations(
        type.toUpperCase(),
        iterations[0].toInt(),
        iterations[1].toInt(),
        Pair(f.dimension, dimensions),
        args
    )
}

class FunctionReport(val f: NTBEAFunction) {
    fun iterations(type: String, runs: Int, iterPerRun: Int, searchDimensions: Pair<Int, Int>, args: Array<String>) {
        // here we want to run ParameterSearch several times (runs), and then
        // for each run we keep the final best sampled point...and its actual value under the function
        // We can then report the basic stats for imprecision and also actual value

        val searchSpace: SearchSpace = if (type == "GAUSSIAN") {
            GaussianSearchSpace(
                searchDimensions.first,
                IntArray(searchDimensions.first * 2) { searchDimensions.second },
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(1.0, 1.0),
                doubleArrayOf(0.1, 0.1),
                doubleArrayOf(0.2, 0.2)
            )
        } else {
            FunctionSearchSpace(searchDimensions.first, searchDimensions.second)
        }
        val kExplore = (args.find { it.startsWith("kExplore=") }?.split("=")?.get(1) ?: "100.0").toDouble()
        val minWeight = (args.find { it.startsWith("minWeight=") }?.split("=")?.get(1) ?: "0.0").toDouble()
        val weight = (args.find { it.startsWith("fitWeight=") }?.split("=")?.get(1) ?: "0.5").toDouble()
        val maxFeatures = (args.find { it.startsWith("maxF=") }?.split("=")?.get(1) ?: "0").toInt()
        val T = (args.find { it.startsWith("T=") }?.split("=")?.get(1) ?: "30").toInt()
        val neighbourhood: Double = args.find { it.startsWith("hood=") }?.split("=")?.get(1)?.toDouble()
            ?: min(50.0, searchDimensions.second.toDouble().pow(searchDimensions.first) * 0.01)
        val fileName: String = args.find { it.startsWith("logFile=") }?.split("=")?.get(1) ?: ""

        val weightFunction: (Int) -> Double = when (type) {
            "EXP" -> { visits: Int -> 1.0 - exp(-visits.toDouble() / T) }
            "LIN" -> { visits: Int -> min(visits.toDouble() / T, 1.0) }
            "INV" -> { visits: Int -> 1.0 - T / (T + visits.toDouble()) }
            "SQRT" -> { visits: Int -> 1.0 - sqrt(T / (T + visits.toDouble())) }
            else -> { visits: Int -> 1.0 - exp(-visits.toDouble() / T) }
            // we default to exponential
        }

        val landscapeModel: LandscapeModel = when (type) {
            "STD", "GAUSSIAN" -> NTupleSystem(searchSpace)
            "FIT" -> NTupleSystemBinaryFit(
                searchSpace,
                threshold = T,
                interpolateByTuple = true,
                maxFeatures = maxFeatures
            )
            "STDFIT" -> NTupleSystemBinaryFit(
                searchSpace,
                threshold = T,
                interpolateByTuple = false,
                interpolation = weight,
                maxFeatures = maxFeatures
            )
            else -> NTupleSystemExp(
                searchSpace,
                T,
                minWeight,
                weightFunction,
                weightExplore = false,
                exploreWithSqrt = false
            )
        }

        val use3Tuples = args.contains("useThreeTuples")
        if (landscapeModel is NTupleSystem) {
            landscapeModel.use3Tuple = use3Tuples && searchSpace.nDims() > 3
            landscapeModel.use2Tuple = searchSpace.nDims() > 2
            landscapeModel.addTuples()
        }

        val searchFramework: EvoAlg = when (landscapeModel) {
            is NTupleSystem -> NTupleBanditEA(landscapeModel, kExplore, neighbourhood.toInt())
            else -> throw AssertionError("Unknown EvoAlg $landscapeModel")
        }

        val evaluator = FunctionEvaluator(f, searchSpace)

        val fullRecord = mutableMapOf<String, Int>()
        val actualValues = mutableMapOf<String, Double>()

        GlobalStatsCollator.clear()
        val fileWriter: FileWriter? = if (fileName != "") FileWriter("$fileName") else null
        repeat(runs) {
            evaluator.reset()
            landscapeModel.reset()
            searchFramework.runTrial(evaluator, iterPerRun)

            val choice = landscapeModel.bestOfSampled.map { it.toInt() }.toIntArray()
            val actualValue = f.functionValue(searchSpace.valueAt(choice))
            val predictedValue = landscapeModel.getMeanEstimate(landscapeModel.bestOfSampled)

            val choiceVals = searchSpace.valueAt(choice)
            val key = choiceVals.joinToString(",") {String.format("%.3f", it)}
            fullRecord[key] = fullRecord.getOrDefault(key , 0) + 1
            actualValues[key] = actualValue
            GlobalStatsCollator.addStatistics("ActualValue", actualValue)
            GlobalStatsCollator.addStatistics("Delta", predictedValue - actualValue)
            val details =
                String.format(
                    "%.3g, %.3g, %.3g, %s\n", predictedValue, actualValue, predictedValue - actualValue,
                    landscapeModel.bestOfSampled.joinToString(separator = "|", transform = { it.toInt().toString() })
                )
            if (fileWriter != null)
                fileWriter.write(details)
            else
                println(details)
            //       println(landscapeModel.toString())
        }

        println(GlobalStatsCollator.summaryString())
        val orderedByCount = fullRecord.toList().sortedBy { it.second }.reversed().take(10)
        println("10 most popular choices : \n" + orderedByCount.joinToString("\n") {
            String.format("\t%s - %d\t%.3f", it.first, it.second, actualValues[it.first])
        })

//        val totalScore = orderedByCount.sumByDouble { actualValues[it.first]!! }
//        println(totalScore)
    }
}


