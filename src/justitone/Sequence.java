package justitone;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

public class Sequence {

    public List<Event> events;

//    public BigFraction baseOffset = BigFraction.ONE;

    public Sequence() {
        events = new ArrayList<>();
    }
    
    public Sequence(Sequence sequence) {
        events = new ArrayList<>();
        events.addAll(sequence.events);
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
    
    
//    public void holdNote(BigFraction length) {
//        Note n = events.get(events.size() - 1);
//        events.remove(events.size() - 1);
//
//        events.add(new Note(n.offset, n.length.add(length)));
//    }

//    public void addTuple(Track tuple, BigFraction length) {
//        BigFraction totalLength = tuple.events.stream().map(n -> n.length).reduce(BigFraction::add)
//                .orElse(BigFraction.ZERO);
//
//        if (totalLength.compareTo(BigFraction.ZERO) > 0) {
//            BigFraction multiplier = length.divide(totalLength);
//
//            for (Note note : tuple.events) {
//                addNote(note.offset, note.length.multiply(multiplier));
//            }
//        }
//    }

//    public void addBar(Track bar, BigFraction length) {
//        for (Note note : bar.events) {
//            addNote(note.offset, note.length.multiply(length));
//        }
//    }

//    public void changeRoot(BigFraction fraction) {
//        baseOffset = baseOffset.multiply(fraction);
//    }
}
