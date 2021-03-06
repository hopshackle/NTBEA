package utilities;

import java.util.ArrayList;

/**
 This class is used to model the statistics of several numbers.  For the statistics
 we choose here it is not necessary to store all the numbers - just keeping a running total
 of how many, the sum and the sum of the squares is sufficient (plus max and min, for max and min).
 */

public class StatSummary {

    public String name; // defaults to ""

    private int n;
    private double sum, sumsq;
    private double min, max;
    private double mean, median, sd;

    private double lastAdded;
    private boolean valid;

    private ArrayList<Double> elements;

    public StatSummary() {
        this("");
    }

    public StatSummary(String name) {
        this.name = name;
        reset();
    }

    public final void reset() {
        n = 0;
        sum = 0;
        sumsq = 0;
        // Ensure that the first number to be added will fix up min and max to be that number
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
        valid = false;
        elements = new ArrayList<>();
    }

    public double max() {
        return max;
    }

    public double min() {
        return min;
    }

    public double mean() {
        if (!valid)
            computeStats();
        return mean;
    }

    /**
     * @return the sum of the squares of the differences between the mean and the ith values
     */
    public double sumSquareDiff() {
        return sumsq - n * mean() * mean();
    }

    private void computeStats() {
        if (!valid) {
            mean = sum / n;
            double num = sumsq - (n * mean * mean);
            if (num < 0) {
                // Avoids tiny negative numbers possible through imprecision
                num = 0;
            }
            sd = Math.sqrt(num / (n - 1));
            valid = true;
        }
    }

    public double sd() {
        if (!valid)
            computeStats();
        return sd;
    }

    public int n() {
        return n;
    }

    public double stdErr() {
        return sd() / Math.sqrt(n);
    }

    public void add(StatSummary ss) {
        n += ss.n;
        sum += ss.sum;
        sumsq += ss.sumsq;
        max = Math.max(max, ss.max);
        min = Math.min(min, ss.min);
        lastAdded = ss.lastAdded;
        valid = false;
        elements.addAll(ss.getElements());
    }

    public void add(double d) {
        n++;
        sum += d;
        sumsq += d * d;
        min = Math.min(min, d);
        max = Math.max(max, d);
        lastAdded = d;
        valid = false;
        elements.add(d);
    }

    public void add(Number n) {
        add(n.doubleValue());
    }

    public void add(double... xa) {
        for (double x : xa) {
            add(x);
        }
    }

    public double sum(){
        return sum;
    }

    public double getLastAdded() {
        return lastAdded;
    }

    public void discountLast(double discount) {
        lastAdded *= discount;
    }

    @Override
    public String toString() {
        String s = (name == null) ? "" : (name + "\n");
        s +=    " min   = " + min() + "\n" +
                " max   = " + max() + "\n" +
                " ave   = " + mean() + "\n" +
                " sd    = " + sd() + "\n" +
                " se    = " + stdErr() + "\n" +
                " sum   = " + sum + "\n" +
                " sumsq = " + sumsq + "\n" +
                " n     = " + n + "\n";
        return s;
    }

    public String shortString() {
        return shortString(false);
    }

    public String shortString(boolean scientificNotation) {
        return (name == null) ? "[" : (name + ": [") + min() + ", " + max() + "]"
                + " avg=" + (scientificNotation? String.format("%6.3e", (mean())) : String.format("%.2f", mean()))
                + "; sd=" + (scientificNotation? String.format("%6.3e", (sd())) : String.format("%.2f", sd()))
                + "; se=" + (scientificNotation? String.format("%6.3e", (stdErr())) : String.format("%.2f", stdErr()))
                ;
    }

    public ArrayList<Double> getElements() {
        return elements;
    }

    public StatSummary copy()
    {
        StatSummary ss = new StatSummary();

        ss.name = this.name;
        ss.sum = this.sum;
        ss.sumsq = this.sumsq;
        ss.min = this.min;
        ss.max = this.max;
        ss.mean = this.mean;
        ss.sd = this.sd;
        ss.n = this.n;
        ss.valid = this.valid;
        ss.lastAdded = this.lastAdded;

        return ss;
    }
}