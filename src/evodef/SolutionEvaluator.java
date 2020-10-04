package evodef;

/**
 * Created by simonmarklucas on 06/08/2016.
 *
 * Evaluates solutions and logs fitness improvement
 *
 *
 */
public interface SolutionEvaluator {
    // call reset before running
    public void reset();
    double evaluate(double[] solution);
    double evaluate(int[] solution);
    // has the algorithm found the optimal solution?
    SearchSpace searchSpace();
    int nEvals();
    EvolutionLogger logger();
}
