package evodef

interface LandscapeModel {

    // careful - this can be slow - it iterates over all points in the search space!
    val bestSolution: DoubleArray

    // get best of sampled is the default choice
    val bestOfSampled: DoubleArray

    // reset removes all data from the model
    fun reset(): LandscapeModel

    val searchSpace: SearchSpace

    fun setEpsilon(epsilon: Double): LandscapeModel

    fun addPoint(p: DoubleArray, value: Double)

    // return a Double object
    fun getMeanEstimate(x: DoubleArray): Double

    // if we've seen nothing of this point then the value
    // for the exploration term will be high, but small epsilon
    // prevents overflow
    fun getExplorationEstimate(x: DoubleArray): Double

}

