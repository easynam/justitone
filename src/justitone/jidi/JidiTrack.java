package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

import justitone.Note;
import justitone.Sequence;

public class JidiTrack {
    public List<JidiEvent> events;
    
    public JidiTrack(Sequence track) {
        events = new ArrayList<>();
        
//        final int ppm = 480 * 4;
//        BigFraction currentPos = BigFraction.ZERO;
//        
//        boolean noteOn = false;
//        
//        for (Note n : track.events) {
//            int tick = currentPos.multiply(ppm).intValue();
//            
//            if (n.offset.equals(BigFraction.ZERO)) {
//                if (noteOn) {
//                    events.add(new JidiEvent.NoteOff(tick));
//                }
//                
//                noteOn = false;
//            }
//            else {
//                noteOn = true;
//                
//                float freq = n.offset.floatValue() * 440f;
//                
//                events.add(new JidiEvent.NoteOn(tick));
//                events.add(new JidiEvent.Pitch(tick, freq));
//            }
//            
//            currentPos = currentPos.add(n.length);
//        }
//        
//        for (JidiEvent e : events) {
//            System.out.println(e);
//        }
    }
}
