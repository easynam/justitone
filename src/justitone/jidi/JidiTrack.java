package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

public class JidiTrack {
    public List<JidiEvent> events;
    
    public JidiTrack() {
        this.events = new ArrayList<>();
    }
    
    public void add(JidiEvent e) {
        System.out.println(e);
        
        events.add(e);
    }
}
