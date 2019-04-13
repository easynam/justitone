package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.util.Pair;

public class Periods {
    List<Pair<BigFraction, BigFraction>> periods;
    
    public Periods() {
        periods = new ArrayList<>();
    }
    
    public void allocate(BigFraction start, BigFraction end) {
        periods.add(new Pair<>(start, end));
    }
    
    public boolean canAllocate(BigFraction start, BigFraction end) {
        if (periods.size() == 0) return true;
        
        Pair<BigFraction, BigFraction> period = new Pair<>(start, end);
        
        return periods.stream()
                      .allMatch(p -> !overlaps(period, p));
    }
    
    public boolean overlaps(Pair<BigFraction, BigFraction> period1, Pair<BigFraction, BigFraction> period2) {
        if (period1.getFirst().compareTo(period2.getSecond()) >= 0) {
            return false;
        }
        if (period2.getFirst().compareTo(period1.getSecond()) >= 0) {
            return false;
        }
        return true;
    }
}
