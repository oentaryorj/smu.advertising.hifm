package larc.recommender.evaluation;

import java.util.Comparator;

public class Case {
    public final boolean mCorrect;
    public final double mScore;
    public static final Comparator<Case> REVERSE_SCORE_COMPARATOR = new ReverseCaseComparator();
    public static final Comparator<Case> SCORE_COMPARATOR = new CaseComparator();
    
    Case(boolean correct, double score) {
        mCorrect = correct;
        mScore = score;
    }
    public double score() {
        return mScore;
    }
    @Override
    public String toString() {
        return mCorrect + " : " + mScore;
    }
    //package privates can't go in interface, so park them here
    private static class CaseComparator implements Comparator<Case> {
    	public int compare(Case obj1, Case obj2) {
    	    return (obj1.score() > obj2.score())? 1 : ((obj1.score() < obj2.score())? -1 : 0);
    	}
    }
    private static class ReverseCaseComparator implements Comparator<Case> {
        public int compare(Case obj1, Case obj2) {
            return (obj1.score() > obj2.score())? -1 : ((obj1.score() < obj2.score())? 1 : 0);
        }
    }
}
