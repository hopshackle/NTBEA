package ntbea;

// import ntuple.params.Param;
import utilities.StatSummary;

public class NTupleSystemReport {

    NTupleSystem nTupleSystem;

    // or could just use static methods ...

    public NTupleSystemReport setModel(NTupleSystem nTupleSystem) {
        this.nTupleSystem = nTupleSystem;
        return this;
    }


//    public void report(NTupleSystem nTupleSystem) {
//
//    }
//
    
    public void report(int[] p) {
        for (NTuple nTuple : nTupleSystem.getTuples()) {
            StatSummary ss = nTuple.getStats(p);
            if (ss != null) {
                System.out.format("\t n: %d,\t mean: %.3f\n", ss.n(), ss.mean());
            } else {
                System.out.println("No data yet");
            }
        }
    }

    public void printSummaryReport() {
        System.out.format("Search space has %d dimensions\n", nTupleSystem.getSearchSpace().nDims());
        for (NTuple nt : nTupleSystem.getTuples()) {
            System.out.println(nt);
        }
        System.out.format("NTuple Model has %d tuples.\n", nTupleSystem.getTuples().size());

    }

    public void printDetailedReport() {
        System.out.format("Search space has %d dimensions\n", nTupleSystem.getSearchSpace().nDims());
        for (NTuple nt : nTupleSystem.getTuples()) {
            System.out.println("nPatterns observed: " + nt.ntMap.size());
            nt.printNonEmpty();
            System.out.println();
        }
        System.out.format("NTuple Model has %d tuples.\n", nTupleSystem.getTuples().size());
    }


}
