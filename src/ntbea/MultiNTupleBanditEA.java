package ntbea;

import evodef.*;
import org.jetbrains.annotations.NotNull;
import utilities.StatSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sml on 09/01/2017.
 */

public class MultiNTupleBanditEA extends NTupleBanditEA {

    public int playerCount;

    public MultiNTupleBanditEA(BanditLandscapeModel model, double kExplore, int nNeighbours, int players) {
        super(model, kExplore, nNeighbours);
        playerCount = players;
    }

    double[] fitness(MultiSolutionEvaluator evaluator, List<int[]> sol) {
        double[] retValue = new double[playerCount];
        for (int i = 0; i < nSamples; i++) {
            double[] fitness = evaluator.evaluate(sol);
            if (fitness.length != playerCount)
                throw new AssertionError("Discrepancy in Player Count - expecting " + playerCount + " results from evaluation");
            for (int p = 0; p < playerCount; p++)
                retValue[p] += fitness[p];
        }
        for (int p = 0; p < playerCount; p++)
            retValue[p] /= nSamples;
        return retValue;
    }

    @NotNull
    @Override
    public double[] runTrial(SolutionEvaluator evaluator, int nEvals) {
        throw new IllegalArgumentException("Only implemented for MultiSolutionEvaluator");
    }

    @NotNull
    @Override
    public double[] runTrial(MultiSolutionEvaluator evaluator, int nEvals) {

        SearchSpace searchSpace = evaluator.searchSpace();
        DefaultMutator mutator = new DefaultMutator(searchSpace);

        nNeighbours = (int) Math.min(nNeighbours, SearchSpaceUtil.size(searchSpace) / 4);
        if (nNeighbours < 5) nNeighbours = 5;
        //     System.out.println("Set neighbours to: " + nNeighbours);

        if (banditLandscapeModel == null) {
            // System.out.println("NTupleBanditEA.runTrial: Creating new landscape model");
            banditLandscapeModel = new NTupleSystem(searchSpace);
        }
        banditLandscapeModel.setEpsilon(epsilon);

        // then each time around the loop try the following
        // create a neighbourhood set of points and pick the best one that combines it's exploitation and evaluation scores

        List<int[]> p = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++)
            p.add(SearchSpaceUtil.randomPoint(searchSpace));

        for (int i = 0; i < nEvals; i++) {

            double[] fitness;

            if (nSamples == 1) {
                fitness = evaluator.evaluate(p);
            } else {
                fitness = fitness(evaluator, p);
            }

            // register all of the evaluated settings with the model
            for (int j = 0; j < playerCount; j++)
                banditLandscapeModel.addPoint(p.get(j), fitness[j]);


            // We then independently find the best neighbour from each of the points
            List<int[]> newP = new ArrayList<>();
            for (int loop = 0; loop < playerCount; loop++) {
                EvaluateChoices evc = new EvaluateChoices(banditLandscapeModel, kExplore);
                int[] startingPoint = p.get(loop);
                while (evc.n() < nNeighbours) {
                    int[] pp = mutator.randMut(startingPoint);
                    evc.add(pp);
                }
                int[] best = evc.picker.getBest();
                newP.add(best);
            }
            p = newP;

        }

        return banditLandscapeModel.getBestOfSampled();
    }

}
