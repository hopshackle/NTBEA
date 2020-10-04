package evodef

/**
 * Created by sml on 19/01/2017.
 * Amended by jng on 19/09/2019
 */

interface BanditLandscapeModel : LandscapeModel {

    // reset removes all data from the model
    override fun reset(): BanditLandscapeModel

    fun addPoint(p: IntArray, value: Double)
    fun getExplorationEstimate(x: IntArray): Double
    fun getMeanEstimate(x: IntArray): Double

    override fun addPoint(p: DoubleArray, value: Double) = addPoint(p.map { (it + 0.5).toInt() }.toIntArray(), value)
    override fun getExplorationEstimate(x: DoubleArray): Double = getExplorationEstimate(x.map { (it + 0.5).toInt() }.toIntArray())
    override fun getMeanEstimate(x: DoubleArray): Double = getMeanEstimate(x.map { (it + 0.5).toInt() }.toIntArray())
}


