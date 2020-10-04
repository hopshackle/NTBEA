package evodef

import evodef.SolutionEvaluator
import evodef.LandscapeModel
import evodef.EvolutionLogger

/**
 * Created by sml on 16/08/2016.
 *
 * Some of the methods in this interface
 *
 */
interface EvoAlg {
    // seed the algorithm with a specified point in the search space
    fun runTrial(evaluator: SolutionEvaluator, nEvals: Int): DoubleArray
    val model: LandscapeModel
}