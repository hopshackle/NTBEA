package ntbea;

import evodef.*;
import org.jetbrains.annotations.NotNull;
import utilities.StatSummary;

import java.util.Arrays;

/**
 * Created by sml on 09/01/2017.
 */

public class NTupleBanditEA implements EvoAlg {

    // public NTupleSystem banditLandscapeModel;
    protected BanditLandscapeModel banditLandscapeModel;

    // the exploration rate normally called K or C - called kExplore here for clarity
    public double kExplore = 100.0; // 1.0; // Math.sqrt(0.4);
    // the number of neighbours to explore around the current point each time
    // they are only explored IN THE FITNESS LANDSCAPE MODEL, not by sampling the fitness function
    int nNeighbours = 50;

    static double defaultEpsilon = 0.5;
    double epsilon = defaultEpsilon;

    public int nSamples = 1;

    public void setSamplingRate(int n) {
        nSamples = n;
    }


    public NTupleBanditEA(BanditLandscapeModel model, double kExplore, int nNeighbours) {
        banditLandscapeModel = model;
        this.kExplore = kExplore;
        this.nNeighbours = nNeighbours;
    }

    int reportFrequency = 10000;
    public NTupleBanditEA setReportFrequency(int reportFrequency) {
        this.reportFrequency = reportFrequency;
        return this;
    }

    StatSummary fitness(SolutionEvaluator evaluator, int[] sol) {
        StatSummary ss = new StatSummary();
        for (int i = 0; i < nSamples; i++) {
            double fitness = evaluator.evaluate(sol);
            ss.add(fitness);
        }
        return ss;
    }

    public int[] seed;
    SolutionEvaluator evaluator;
    public boolean logBestYet = false;

    @NotNull
    @Override
    public double[] runTrial(SolutionEvaluator evaluator, int nEvals) {

        this.evaluator = evaluator;
        // set  up some convenient references
        SearchSpace searchSpace = evaluator.searchSpace();
        DefaultMutator mutator = new DefaultMutator(searchSpace);
 //       banditLandscapeModel.setSearchSpace(searchSpace);

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

        int[] p;
        if (seed == null) {
            p = SearchSpaceUtil.randomPoint(searchSpace);
        } else {
            p = seed;
        }

        // banditLandscapeModel.printDetailedReport();



        for(int i=0; i<nEvals; i++) {

            // each time around the loop we make one fitness evaluation of p
            // and add this NEW information to the memory
            //int prevEvals = evaluator.nEvals();

            // the new version enables resampling
            double fitness;

/*            String pString = "";
            for (int i = 0; i < p.length; i++) pString += "[" + p[i] + "] ";
            System.out.println(pString);*/

            if (nSamples == 1) {
                fitness = evaluator.evaluate(p);
            } else {
                fitness = fitness(evaluator, p).mean();
            }

//            System.out.println(String.format("Point evaluated is %s, fitness %.2f", pString, fitness));

            if (i>0 && i % reportFrequency == 0) {
                System.out.format("Iteration: %d\t %.1f\n", evaluator.nEvals(), fitness);
                System.out.println(evaluator.logger().ss);
                System.out.println();
                // System.out.println(p.length);
                // System.out.println(p);
            }

            banditLandscapeModel.addPoint(p, fitness);

            // ss.add(t.elapsed());
//            System.out.println(ss);
//            System.out.println("N Neighbours: " + nNeighbours);
            EvaluateChoices evc = new EvaluateChoices(banditLandscapeModel, kExplore);
            // evc.add(p);

            // and then explore the neighbourhood around p, balancing exploration and exploitation
            // depending on the mutation function, some of the neighbours could be far away
            // or some of them could be duplicates - duplicates a bit wasteful so filter these
            // out - repeat until we have the required number of unique neighbours

            while (evc.n() < nNeighbours) {
                int[] pp = mutator.randMut(p);
                evc.add(pp);
            }

            // evc.report();

            // now set the next point to explore
            p = evc.picker.getBest();
//            logger.keepBest(picker.getBest(), picker.getBestScore());

            //int diffEvals = evaluator.nEvals() - prevEvals;

//            if (logBestYet) {
//                double[] bestYet = banditLandscapeModel.getBestOfSampled();
//                for (int i = 0; i < diffEvals; i++) {
//                    evaluator.logger().logBestYest(bestYet);
//                }
//            }
        }

        return banditLandscapeModel.getBestOfSampled();
    }

    public NTupleBanditEA setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }

    @NotNull
    @Override
    public LandscapeModel getModel() {
        return banditLandscapeModel;
    }
}
