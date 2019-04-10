package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

import justitone.Note;
import justitone.Track;

public class JidiTrack {
    public List<JidiEvent> events;
    
    public JidiTrack(Track track) {
        events = new ArrayList<>();
        
        final int ppm = 480 * 4;
        BigFraction currentPos = BigFraction.ZERO;
        
        boolean noteOn = false;
        
        for (Note n : track.notes) {
            int tick = currentPos.multiply(ppm).intValue();
            
            if (n.offset.equals(BigFraction.ZERO)) {
                if (noteOn) {
                    events.add(new JidiEvent.NoteOff(tick));
                }
            }
            else {
                float freq = n.offset.floatValue() * 440f;
                
                events.add(new JidiEvent.NoteOn(tick));
                events.add(new JidiEvent.Pitch(tick, freq));
            }
        }
    }
}