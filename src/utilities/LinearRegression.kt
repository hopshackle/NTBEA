package utilities

import org.apache.commons.math3.linear.*

/*
	 * x is n x k; y is n x 1
	 */
class LinearRegression(private val X: Array<DoubleArray>, private val Y: DoubleArray) {

    /** the weight to learn  */
    /*
	 * [0] element is always the intercept, so total length is k+1
	 */
    val weights: DoubleArray

    init {
        val solver = SingularValueDecomposition(Array2DRowRealMatrix(X, false)).getSolver()
        weights = solver.solve(ArrayRealVector(Y)).toArray()
    }


    val error: Double
        get() {
            var retValue = 0.0
            for (i in Y.indices) {
                val predicted = predict(X[i])
                retValue += Math.pow(Y[i] - predicted, 2.0)
            }
            return retValue / Y.size.toDouble()
        }

    fun predict(x: DoubleArray): Double {
        if (x.size != weights.size) throw AssertionError("Data not same dimension as weights " + x.size + " vs " + weights.size)
        var retValue = 0.0
        for (i in x.indices) {
            retValue += x[i] * weights[i]
        }
        return retValue
    }

    override fun toString(): String {
        return weights.joinToString()
    }
}