package larc.recommender.evaluation;

import java.util.Comparator;
import java.util.List;

public class IndexComparator implements Comparator<Integer> {
	private final List<Double> array;
	private final boolean ascending;

    public IndexComparator(List<Double> array, boolean ascending) {
        this.array = array;
        this.ascending = ascending;
    }
    public Integer[] createIndices() {
    	Integer[] indices = new Integer[array.size()];
        for (int i = 0; i < array.size(); i++) {
        	indices[i] = i; // Autoboxing
        }
        return indices;
    }
    @Override
    public int compare(Integer index1, Integer index2) {
    	double val1 = array.get(index1);
    	double val2 = array.get(index2);
    	if (ascending) {
    		if (val1 < val2) return -1;
    		if (val1 > val2) return 1;
    	} else {
    		if (val1 > val2) return -1;
    		if (val1 < val2) return 1;
    	} 
    	return 0;
    }
}
