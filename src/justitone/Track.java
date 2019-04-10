package justitone;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

public class Track {
    List<Note> notes;
    
    public BigFraction baseOffset = BigFraction.ONE;
    
    public int tempo;
    
    public Track(int tempo) {
        this.notes = new ArrayList<>();
        
        this.tempo = tempo;
    }
    
    public void addNote(BigFraction offset, BigFraction length) {
        notes.add(new Note(baseOffset.multiply(offset), length));
    }
    
    public void holdNote(BigFraction length) {
        Note n = notes.get(notes.size() - 1);
        notes.remove(notes.size() -1);
        
        notes.add(new Note(n.offset, n.length.add(length)));
    }
    
    public void addTuple(Track tuple, BigFraction length) {
        BigFraction totalLength = tuple.notes.stream().map(n -> n.length).reduce(BigFraction::add).orElse(BigFraction.ZERO);
        
        if (totalLength.compareTo(BigFraction.ZERO) > 0) {
            BigFraction multiplier = length.divide(totalLength);
            
            for(Note note : tuple.notes) {
                addNote(note.offset, note.length.multiply(multiplier));
            }
        }
    }
    
    public void changeRoot(BigFraction fraction) {
        baseOffset = baseOffset.multiply(fraction);
    }
}
