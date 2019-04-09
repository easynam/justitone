package justitone;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.Fraction;

public class Track {
	List<Note> notes;
	
	public Fraction baseOffset = Fraction.ONE;
	
	public Track() {
		this.notes = new ArrayList<>();
	}
	
	public void addNote(Fraction offset, Fraction length) {
		notes.add(new Note(baseOffset.multiply(offset), length));
	}
	
	public void holdNote(Fraction length) {
		Note n = notes.get(notes.size() - 1);
		
		notes.add(new Note(n.offset, n.length.add(length)));
	}
	
	public void addTuple(Track tuple, Fraction length) {
		Fraction totalLength = tuple.notes.stream().map(n -> n.length).reduce(Fraction::add).orElse(Fraction.ZERO);
		
		if (totalLength.compareTo(Fraction.ZERO) > 0) {
			Fraction multiplier = length.divide(totalLength);
			
			for(Note note : tuple.notes) {
				addNote(note.offset, note.length.multiply(multiplier));
			}
		}
	}
	
	public void changeRoot(Fraction fraction) {
		baseOffset = baseOffset.multiply(fraction);
	}
}
