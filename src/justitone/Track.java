package justitone;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.Fraction;

public class Track {
	List<Note> notes;
	
	float root = 440;
	Fraction offset = Fraction.ONE;
	
	public Track() {
		this.notes = new ArrayList<>();
	}
	
	public void addNote(Fraction offset, Fraction length) {
		notes.add(new Note(this.offset.floatValue() * offset.floatValue() * root, length));
	}
	
	public void changeRoot(float freq) {
		this.root = freq;
		offset = Fraction.ONE;
	}
	
	public void changeRoot(Fraction fraction) {
		System.out.println("changed ratio!");
		offset = offset.multiply(fraction);
	}
}
