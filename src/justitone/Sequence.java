package justitone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

public class Sequence {
    public List<Event> events;

    public Sequence() {
        events = new ArrayList<>();
    }
    
    public Sequence(Sequence sequence) {
        events = new ArrayList<>();
        events.addAll(sequence.events);
    }

    public Sequence(List<Event> events) {
        this.events = new ArrayList<>();
        this.events.addAll(events);
    }
    
    public Sequence(Event... events) {
        this.events = new ArrayList<>();
        this.events.addAll(Arrays.asList(events));
    }

    public void addEvent(Event e) {
        events.add(e);
    }
    
    public BigFraction length() {
        return events.stream()
                     .map(e -> e.length())
                     .reduce(BigFraction::add)
                     .orElse(BigFraction.ZERO);
    }
    
    public List<Event> contents() {
        return events;
    }
}
